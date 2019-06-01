package dms

import (
	"bytes"
	"context"
	"encoding/binary"
	"errors"
	"io"
	"math"
	"net"
	"sync"

	"github.com/skycoin/skywire/internal/ioutil"
	"github.com/skycoin/skywire/pkg/transport"

	"github.com/skycoin/skywire/pkg/cipher"
)

// Errors related to REQUESTs.
var (
	ErrRequestRejected    = errors.New("request rejected")
	ErrRequestCheckFailed = errors.New("request check failed")
)

// Transport represents a connection from dms.Client to remote dms.Client (via dms.Server intermediary).
// It implements transport.Transport
type Transport struct {
	net.Conn // link with server.

	id     uint16
	local  cipher.PubKey
	remote cipher.PubKey // remote PK

	ackWaiter ackWaiter
	readBuf   bytes.Buffer
	readMx    sync.Mutex    // This is for protecting 'readBuf'.
	readCh    chan Frame    // TODO(evanlinjin): find proper way of closing readCh.
	doneCh    chan struct{} // stop writing
	doneOnce  sync.Once
}

// NewTransport creates a new dms_tp.
func NewTransport(conn net.Conn, local, remote cipher.PubKey, id uint16) *Transport {
	return &Transport{
		Conn:   conn,
		id:     id,
		local:  local,
		remote: remote,
		readCh: make(chan Frame, readChSize),
		doneCh: make(chan struct{}),
	}
}

func (c *Transport) close() (closed bool) {
	c.doneOnce.Do(func() {
		close(c.doneCh)
		closed = true
	})
	return closed
}

func (c *Transport) awaitResponse(ctx context.Context) error {
	select {
	case f := <-c.readCh:
		if f.Type() == AcceptType {
			return nil
		}
		return errors.New("invalid remote response")
	case <-c.doneCh:
		return ErrRequestRejected
	case <-ctx.Done():
		return ctx.Err()
	}
}

// Handshake performs a tp handshake (before tp is considered valid).
func (c *Transport) Handshake(ctx context.Context) error {
	// if channel ID is even, client is initiator.
	if isInitiatorID(c.id) {

		pks := combinePKs(c.local, c.remote)
		f := MakeFrame(RequestType, c.id, pks)
		if err := writeFrame(c.Conn, f); err != nil {
			c.close()
			return err
		}
		if err := c.awaitResponse(ctx); err != nil {
			c.close()
			return err
		}
	} else {
		f := MakeFrame(AcceptType, c.id, combinePKs(c.remote, c.local))
		if err := writeFrame(c.Conn, f); err != nil {
			c.close()
			return err
		}
	}
	return nil
}

// IsDone returns whether dms_tp is closed.
func (c *Transport) IsDone() bool {
	select {
	case <-c.doneCh:
		return true
	default:
		return false
	}
}

// InjectRead blocks until frame is read.
// Returns false when read fails (e.g. when tp is closed).
func (c *Transport) InjectRead(f Frame) bool {
	ok := c.injectRead(f)
	if !ok {
		c.close()
		close(c.readCh)
	}
	return ok
}

func (c *Transport) injectRead(f Frame) bool {
	push := func(f Frame) bool {
		select {
		case <-c.doneCh:
			return false
		case c.readCh <- f:
			return true
		default:
			return false
		}
	}

	switch f.Type() {
	case CloseType:
		return false

	case AckType:
		p := f.Pay()
		if len(p) != 2 {
			return false
		}
		c.ackWaiter.done(AckSeq(binary.BigEndian.Uint16(p)))
		return true

	case FwdType:
		p := f.Pay()
		if len(p) < 2 {
			return false
		}
		if ok := push(f); !ok {
			return false
		}
		go func() {
			if err := writeFrame(c.Conn, MakeFrame(AckType, c.id, p[:2])); err != nil {
				c.close()
			}
		}()
		return true

	default:
		return push(f)
	}
}

// Read implements io.Reader
func (c *Transport) Read(p []byte) (n int, err error) {
	c.readMx.Lock()
	defer c.readMx.Unlock()

	if c.readBuf.Len() != 0 {
		return c.readBuf.Read(p)
	}

	select {
	case <-c.doneCh:
		return 0, io.ErrClosedPipe
	case f, ok := <-c.readCh:
		if !ok {
			return 0, io.ErrClosedPipe
		}
		if f.Type() == FwdType {
			return ioutil.BufRead(&c.readBuf, f.Pay()[2:], p)
		}
		return 0, errors.New("unexpected frame")
	}
}

// Write implements io.Writer
func (c *Transport) Write(p []byte) (int, error) {
	select {
	case <-c.doneCh:
		return 0, io.ErrClosedPipe
	default:
		ctx, cancel := context.WithTimeout(context.Background(), readTimeout)
		defer cancel()

		err := c.ackWaiter.wait(ctx, c.doneCh, func(seq AckSeq) error {
			if err := writeFwdFrame(c.Conn, c.id, seq, p); err != nil {
				c.close()
				return err
			}
			return nil
		})
		if err != nil {
			return 0, err
		}
		return len(p), nil
	}
}

// Close closes the dms_tp.
func (c *Transport) Close() error {
	if c.close() {
		_ = writeFrame(c.Conn, MakeFrame(CloseType, c.id, []byte{0})) //nolint:errcheck
		return nil
	}
	return io.ErrClosedPipe
}

// Edges returns the local/remote edges of the transport (dms_client to dms_client).
func (c *Transport) Edges() [2]cipher.PubKey {
	return transport.SortPubKeys(c.local, c.remote)
}

// Type returns the transport type.
func (c *Transport) Type() string {
	return Type
}

//AckSeq is part of the acknowledgement-waiting logic.
type AckSeq uint16

// Encode encodes the AckSeq to a 2-byte slice.
func (s AckSeq) Encode() []byte {
	b := make([]byte, 2)
	binary.BigEndian.PutUint16(b, uint16(s))
	return b
}

type ackWaiter struct {
	nextSeq AckSeq
	waiters [math.MaxUint16]chan struct{}
	mx      sync.RWMutex
}

func (w *ackWaiter) wait(ctx context.Context, done <-chan struct{}, action func(seq AckSeq) error) error {
	ackCh := make(chan struct{})
	defer close(ackCh)

	w.mx.Lock()
	seq := w.nextSeq
	w.nextSeq++
	w.waiters[seq] = ackCh
	w.mx.Unlock()

	if err := action(seq); err != nil {
		return err
	}

	select {
	case <-ackCh:
		return nil
	case <-done:
		return io.ErrClosedPipe
	case <-ctx.Done():
		return ctx.Err()
	}
}

func (w *ackWaiter) done(seq AckSeq) {
	w.mx.RLock()
	ackCh := w.waiters[seq]
	w.mx.RUnlock()

	select {
	case ackCh <- struct{}{}:
	default:
	}
}
