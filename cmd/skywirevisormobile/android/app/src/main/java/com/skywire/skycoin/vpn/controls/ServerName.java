package com.skywire.skycoin.vpn.controls;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.core.content.res.ResourcesCompat;

import com.skywire.skycoin.vpn.R;
import com.skywire.skycoin.vpn.activities.servers.ServerLists;
import com.skywire.skycoin.vpn.activities.servers.VpnServerForList;
import com.skywire.skycoin.vpn.helpers.MaterialFontSpan;
import com.skywire.skycoin.vpn.objects.ServerFlags;
import com.skywire.skycoin.vpn.vpn.VPNServersPersistentData;

public class ServerName extends FrameLayout {
    private static MaterialFontSpan materialFontSpan = new MaterialFontSpan();
    private static RelativeSizeSpan relativeSizeSpan = new RelativeSizeSpan(0.75f);

    private TextView text;

    private String defaultName = "";

    public ServerName(Context context) {
        super(context);
        Initialize(context, null);
    }
    public ServerName(Context context, AttributeSet attrs) {
        super(context, attrs);
        Initialize(context, attrs);
    }
    public ServerName(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        Initialize(context, attrs);
    }

    private void Initialize(Context context, AttributeSet attrs) {
        LayoutInflater inflater = (LayoutInflater)context.getSystemService (Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.view_server_name, this, true);

        text = this.findViewById (R.id.text);

        if (attrs != null) {
            TypedArray attributes = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.ServerName,
                0, 0
            );

            String defaultName = attributes.getString(R.styleable.ServerName_default_name);
            if (defaultName != null) {
                this.defaultName = defaultName;
                text.setText(defaultName);
            }

            float textSize = attributes.getDimensionPixelSize(R.styleable.ServerName_text_size, -1);
            if (textSize != -1) {
                text.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
            }

            attributes.recycle();
        }
    }

    public void setServer(VpnServerForList server, ServerLists listType) {
        int initialicons = 0;
        boolean isCurrentServer = VPNServersPersistentData.getInstance().getCurrentServer() != null &&
            server.pk.toLowerCase().equals(VPNServersPersistentData.getInstance().getCurrentServer().pk.toLowerCase());

        SpannableStringBuilder finalText = new SpannableStringBuilder("");

        if (isCurrentServer) {
            finalText.append("\ue876 ");
            initialicons += 1;
        }
        if (server.flag == ServerFlags.Blocked && listType != ServerLists.Blocked) {
            finalText.append("\ue14c ");
            finalText.setSpan(new ForegroundColorSpan(
                ResourcesCompat.getColor(getResources(),R.color.red, null)),
                initialicons * 2,
                (initialicons * 2) + 2,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
            initialicons += 1;
        }
        if (server.flag == ServerFlags.Favorite && listType != ServerLists.Favorites) {
            finalText.append("\ue838 ");
            finalText.setSpan(new ForegroundColorSpan(
                ResourcesCompat.getColor(getResources(),R.color.yellow, null)),
                initialicons * 2,
                (initialicons * 2) + 2,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
            initialicons += 1;
        }
        if (server.inHistory && listType != ServerLists.History && !isCurrentServer) {
            finalText.append("\ue889 ");
            initialicons += 1;
        }

        if (initialicons != 0) {
            finalText.setSpan(materialFontSpan, 0, initialicons * 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            finalText.setSpan(relativeSizeSpan, 0, initialicons * 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        if ((server.name == null || server.name.trim().equals("")) && (server.customName == null || server.customName.trim().equals(""))) {
            finalText.append(defaultName);
        } else if (server.name != null && !server.name.trim().equals("") && (server.customName == null || server.customName.trim().equals(""))) {
            finalText.append(server.name);
        } else if (server.customName != null && !server.customName.trim().equals("") && (server.name == null || server.name.trim().equals(""))) {
            finalText.append(server.customName);
        } else {
            finalText.append(server.customName + " - " + server.name);
        }

        text.setText(finalText);
    }
}
