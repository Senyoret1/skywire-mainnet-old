package com.skywire.skycoin.vpn.activities.servers;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.skywire.skycoin.vpn.R;
import com.skywire.skycoin.vpn.controls.BoxRowLayout;
import com.skywire.skycoin.vpn.controls.ConfirmationModalWindow;
import com.skywire.skycoin.vpn.controls.EditServerValueModalWindow;
import com.skywire.skycoin.vpn.controls.ServerInfoModalWindow;
import com.skywire.skycoin.vpn.controls.ServerName;
import com.skywire.skycoin.vpn.controls.SettingsButton;
import com.skywire.skycoin.vpn.controls.options.OptionsItem;
import com.skywire.skycoin.vpn.controls.options.OptionsModalWindow;
import com.skywire.skycoin.vpn.extensible.ListButtonBase;
import com.skywire.skycoin.vpn.helpers.BoxRowTypes;
import com.skywire.skycoin.vpn.helpers.HelperFunctions;
import com.skywire.skycoin.vpn.objects.LocalServerData;
import com.skywire.skycoin.vpn.objects.ServerFlags;
import com.skywire.skycoin.vpn.objects.ServerRatings;
import com.skywire.skycoin.vpn.vpn.VPNServersPersistentData;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

public class ServerListButton extends ListButtonBase<Void> {
    private static DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd hh:mm a");

    private BoxRowLayout mainLayout;
    private ImageView imageFlag;
    private ServerName serverName;
    private TextView textDate;
    private TextView textLocation;
    private TextView textLatency;
    private TextView textCongestion;
    private TextView textHops;
    private TextView textLatencyRating;
    private TextView textCongestionRating;
    private TextView textNote;
    private TextView textPersonalNote;
    private LinearLayout statsArea1;
    private LinearLayout statsArea2;
    private LinearLayout noteArea;
    private LinearLayout personalNoteArea;
    private SettingsButton buttonSettings;

    private VpnServerForList server;
    private ServerLists listType;

    public ServerListButton (Context context) {
        super(context);
    }

    @Override
    protected void Initialize (Context context, AttributeSet attrs) {
        LayoutInflater inflater = (LayoutInflater)context.getSystemService (Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.view_server_list_item, this, true);

        mainLayout = this.findViewById (R.id.mainLayout);
        imageFlag = this.findViewById (R.id.imageFlag);
        serverName = this.findViewById (R.id.serverName);
        textDate = this.findViewById (R.id.textDate);
        textLocation = this.findViewById (R.id.textLocation);
        textLatency = this.findViewById (R.id.textLatency);
        textCongestion = this.findViewById (R.id.textCongestion);
        textHops = this.findViewById (R.id.textHops);
        textLatencyRating = this.findViewById (R.id.textLatencyRating);
        textCongestionRating = this.findViewById (R.id.textCongestionRating);
        textNote = this.findViewById (R.id.textNote);
        textPersonalNote = this.findViewById (R.id.textPersonalNote);
        statsArea1 = this.findViewById (R.id.statsArea1);
        statsArea2 = this.findViewById (R.id.statsArea2);
        noteArea = this.findViewById (R.id.noteArea);
        personalNoteArea = this.findViewById (R.id.personalNoteArea);
        buttonSettings = this.findViewById (R.id.buttonSettings);

        imageFlag.setClipToOutline(true);

        buttonSettings.setClickEventListener(view -> showOptions());
    }

    public void changeData(@NonNull VpnServerForList serverData, ServerLists listType) {
        server = serverData;
        this.listType = listType;

        imageFlag.setImageResource(HelperFunctions.getFlagResourceId(serverData.countryCode));
        serverName.setServer(serverData, listType);

        if (serverData.location != null && !serverData.location.trim().equals("")) {
            String pk = serverData.pk;
            if (pk.length() > 5) {
                pk = pk.substring(0, 5);
            }
            textLocation.setText("(" + pk + ") " + serverData.location);
        } else {
            textLocation.setText(serverData.pk);
        }

        if (serverData.note != null && serverData.note.trim() != "") {
            noteArea.setVisibility(VISIBLE);
            textNote.setText(serverData.note);
        } else {
            noteArea.setVisibility(GONE);
        }
        if (serverData.personalNote != null && serverData.personalNote.trim() != "") {
            personalNoteArea.setVisibility(VISIBLE);
            textPersonalNote.setText(serverData.personalNote);
        } else {
            personalNoteArea.setVisibility(GONE);
        }

        if (listType == ServerLists.Public) {
            statsArea1.setVisibility(VISIBLE);
            statsArea2.setVisibility(VISIBLE);

            textLatency.setText(HelperFunctions.getLatencyValue(serverData.latency, getContext()));
            textCongestion.setText(HelperFunctions.zeroDecimalsFormatter.format(serverData.congestion) + "%");
            textHops.setText(serverData.hops + "");

            textLatencyRating.setText(ServerRatings.getTextForRating(serverData.latencyRating));
            textLatencyRating.setTextColor(getRatingColor(serverData.latencyRating));
            textCongestionRating.setText(ServerRatings.getTextForRating(serverData.congestionRating));
            textCongestionRating.setTextColor(getRatingColor(serverData.congestionRating));

            if (serverData.congestion < 60) {
                textCongestion.setTextColor(ContextCompat.getColor(getContext(), R.color.green));
            } else if (serverData.congestion < 90) {
                textCongestion.setTextColor(ContextCompat.getColor(getContext(), R.color.yellow));
            } else {
                textCongestion.setTextColor(ContextCompat.getColor(getContext(), R.color.red));
            }

            if (serverData.latency < 200) {
                textLatency.setTextColor(ContextCompat.getColor(getContext(), R.color.green));
            } else if (serverData.latency < 350) {
                textLatency.setTextColor(ContextCompat.getColor(getContext(), R.color.yellow));
            } else {
                textLatency.setTextColor(ContextCompat.getColor(getContext(), R.color.red));
            }

            if (serverData.hops < 5) {
                textHops.setTextColor(ContextCompat.getColor(getContext(), R.color.green));
            } else if (serverData.hops < 9) {
                textHops.setTextColor(ContextCompat.getColor(getContext(), R.color.yellow));
            } else {
                textHops.setTextColor(ContextCompat.getColor(getContext(), R.color.red));
            }
        } else {
            statsArea1.setVisibility(GONE);
            statsArea2.setVisibility(GONE);
        }

        if (listType == ServerLists.History) {
            textDate.setVisibility(VISIBLE);
            textDate.setText(dateFormat.format(serverData.lastUsed));
        } else {
            textDate.setVisibility(GONE);
        }
    }

    public void setBoxRowType(BoxRowTypes type) {
        mainLayout.setType(type);
    }

    private int getRatingColor(ServerRatings rating) {
        int colorId = R.color.bronze;

        if (rating == ServerRatings.Gold) {
            colorId = R.color.gold;
        } else if (rating == ServerRatings.Silver) {
            colorId = R.color.silver;
        }

        return ContextCompat.getColor(getContext(), colorId);
    }

    private void showOptions() {
        ArrayList<OptionsItem.SelectableOption> options = new ArrayList();
        ArrayList<Integer> optionCodes = new ArrayList();

        OptionsItem.SelectableOption option = new OptionsItem.SelectableOption();
        option.icon = "\ue88e";
        option.translatableLabelId = R.string.tmp_server_options_view_info;
        options.add(option);
        optionCodes.add(10);
        option = new OptionsItem.SelectableOption();
        option.icon = "\ue3c9";
        option.translatableLabelId = R.string.tmp_edit_value_name_title;
        options.add(option);
        optionCodes.add(101);
        option = new OptionsItem.SelectableOption();
        option.icon = "\ue8d2";
        option.translatableLabelId = R.string.tmp_edit_value_note_title;
        options.add(option);
        optionCodes.add(102);

        if (server.flag != ServerFlags.Favorite) {
            option = new OptionsItem.SelectableOption();
            option.icon = "\ue838";
            option.translatableLabelId = R.string.tmp_server_options_make_favorite;
            options.add(option);
            optionCodes.add(1);
        }

        if (server.flag == ServerFlags.Favorite) {
            option = new OptionsItem.SelectableOption();
            option.icon = "\ue83a";
            option.translatableLabelId = R.string.tmp_server_options_remove_from_favorites;
            options.add(option);
            optionCodes.add(-1);
        }

        if (server.flag != ServerFlags.Blocked) {
            option = new OptionsItem.SelectableOption();
            option.icon = "\ue925";
            option.translatableLabelId = R.string.tmp_server_options_block;
            options.add(option);
            optionCodes.add(2);
        }

        if (server.flag == ServerFlags.Blocked) {
            option = new OptionsItem.SelectableOption();
            option.icon = "\ue8dc";
            option.translatableLabelId = R.string.tmp_server_options_unblock;
            options.add(option);
            optionCodes.add(-2);
        }

        if (server.inHistory) {
            option = new OptionsItem.SelectableOption();
            option.icon = "\ue872";
            option.translatableLabelId = R.string.tmp_server_options_remove_from_history;
            options.add(option);
            optionCodes.add(-3);
        }

        OptionsModalWindow modal = new OptionsModalWindow(getContext(), options, (int selectedOption) -> {
            LocalServerData savedVersion_ = VPNServersPersistentData.getInstance().getSavedVersion(server.pk);
            if (savedVersion_ == null) {
                savedVersion_ = VPNServersPersistentData.getInstance().processFromList(server);
            }

            final LocalServerData savedVersion = savedVersion_;

            if (optionCodes.get(selectedOption) > 100) {
                EditServerValueModalWindow valueModal = new EditServerValueModalWindow(
                    getContext(),
                    optionCodes.get(selectedOption) == 101,
                    server
                );
                valueModal.show();
            } else if (optionCodes.get(selectedOption) == 10) {
                ServerInfoModalWindow infoModal = new ServerInfoModalWindow(getContext(), server, listType);
                infoModal.show();
            } else if (optionCodes.get(selectedOption) == 1) {
                if (server.flag != ServerFlags.Blocked) {
                    VPNServersPersistentData.getInstance().changeFlag(savedVersion, ServerFlags.Favorite);
                    HelperFunctions.showToast(getContext().getString(R.string.tmp_server_options_make_favorite_done), true);
                    return;
                }

                ConfirmationModalWindow confirmationModal = new ConfirmationModalWindow(
                    getContext(),
                    R.string.tmp_server_options_make_favorite_from_blocked_confirmation,
                    R.string.tmp_confirmation_yes,
                    R.string.tmp_confirmation_no,
                    () -> {
                        VPNServersPersistentData.getInstance().changeFlag(savedVersion, ServerFlags.Favorite);
                        HelperFunctions.showToast(getContext().getString(R.string.tmp_server_options_make_favorite_done), true);
                    }
                );
                confirmationModal.show();
            } else if (optionCodes.get(selectedOption) == -1) {
                VPNServersPersistentData.getInstance().changeFlag(savedVersion, ServerFlags.None);
                HelperFunctions.showToast(getContext().getString(R.string.tmp_server_options_remove_from_favorites_done), true);
            } else if (optionCodes.get(selectedOption) == 2) {
                if (VPNServersPersistentData.getInstance().getCurrentServer() != null &&
                    VPNServersPersistentData.getInstance().getCurrentServer().pk.toLowerCase().equals(server.pk.toLowerCase())
                ) {
                    HelperFunctions.showToast(getContext().getString(R.string.tmp_server_options_block_error), true);
                    return;
                }

                if (server.flag != ServerFlags.Favorite) {
                    VPNServersPersistentData.getInstance().changeFlag(savedVersion, ServerFlags.Blocked);
                    HelperFunctions.showToast(getContext().getString(R.string.tmp_server_options_block_done), true);
                    return;
                }

                ConfirmationModalWindow confirmationModal = new ConfirmationModalWindow(
                    getContext(),
                    R.string.tmp_server_options_block_favorite_confirmation,
                    R.string.tmp_confirmation_yes,
                    R.string.tmp_confirmation_no,
                    () -> {
                        VPNServersPersistentData.getInstance().changeFlag(savedVersion, ServerFlags.Blocked);
                        HelperFunctions.showToast(getContext().getString(R.string.tmp_server_options_block_done), true);
                    }
                );
                confirmationModal.show();
            } else if (optionCodes.get(selectedOption) == -2) {
                VPNServersPersistentData.getInstance().changeFlag(savedVersion, ServerFlags.None);
                HelperFunctions.showToast(getContext().getString(R.string.tmp_server_options_unblock_done), true);
            } else if (optionCodes.get(selectedOption) == -3) {
                ConfirmationModalWindow confirmationModal = new ConfirmationModalWindow(
                    getContext(),
                    R.string.tmp_server_options_remove_from_history_confirmation,
                    R.string.tmp_confirmation_yes,
                    R.string.tmp_confirmation_no,
                    () -> {
                        VPNServersPersistentData.getInstance().removeFromHistory(savedVersion.pk);
                        HelperFunctions.showToast(getContext().getString(R.string.tmp_server_options_remove_from_history_done), true);
                    }
                );
                confirmationModal.show();
            }
        });
        modal.show();
    }
}
