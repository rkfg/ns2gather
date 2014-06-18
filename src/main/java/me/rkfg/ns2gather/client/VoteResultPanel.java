package me.rkfg.ns2gather.client;

import java.util.List;

import me.rkfg.ns2gather.dto.CheckedDTO;
import me.rkfg.ns2gather.dto.MapDTO;
import me.rkfg.ns2gather.dto.PlayerDTO;
import me.rkfg.ns2gather.dto.ServerDTO;
import me.rkfg.ns2gather.dto.VoteResultDTO;
import ru.ppsrk.gwt.shared.SharedUtils;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.user.client.ui.HasVerticalAlignment;

public class VoteResultPanel extends DialogBox {
    private final SimplePanel rootPanel = new SimplePanel();
    private final FlexTable flexTable = new FlexTable();
    private final Label label = new Label("Командир А");
    private final Label label_comm1 = new Label("comm1");
    private final Label lblNewLabel_1 = new Label("Командир Б");
    private final Label label_comm2 = new Label("comm2");
    private final Label label_3 = new Label("Карты");
    private final Label label_maps = new Label("maps");
    private final Label label_1 = new Label("Сервер");
    private final Label label_server = new Label("server");
    private final HTML html_connect = new HTML("<a href=\"steam://connect/\">Подключиться</a>", true);
    private final Button button_close = new Button("Закрыть");
    private final HorizontalPanel horizontalPanel = new HorizontalPanel();
    private final Label label_2 = new Label("Вы сможете снова открыть это окно, щёлкнув по статусу gather'а.");
    private final HorizontalPanel horizontalPanel_buttons = new HorizontalPanel();
    private final Button button_mute = new Button("<img src=\"icons/mute.png\">");
    private ListDataProvider<PlayerDTO> dataProvider_players;
    private ListDataProvider<MapDTO> dataProvider_maps;
    private ListDataProvider<ServerDTO> dataProvider_servers;

    public VoteResultPanel(ListDataProvider<PlayerDTO> dataProvider_players, ListDataProvider<MapDTO> dataProvider_maps,
            ListDataProvider<ServerDTO> dataProvider_servers) {
        setModal(false);
        setText("Результаты голосования");
        flexTable.setCellPadding(5);

        setWidget(rootPanel);
        rootPanel.setSize("500px", "300px");
        rootPanel.setWidget(flexTable);
        flexTable.setSize("100%", "100%");
        label.setWordWrap(false);

        flexTable.setWidget(0, 0, label);
        label_comm1.setWordWrap(false);

        flexTable.setWidget(0, 1, label_comm1);

        flexTable.setWidget(1, 0, lblNewLabel_1);
        label_comm2.setWordWrap(false);

        flexTable.setWidget(1, 1, label_comm2);

        flexTable.setWidget(2, 0, label_3);

        flexTable.setWidget(2, 1, label_maps);

        flexTable.setWidget(3, 0, label_1);

        flexTable.setWidget(3, 1, label_server);

        flexTable.setWidget(4, 0, html_connect);
        flexTable.getFlexCellFormatter().setColSpan(4, 0, 2);
        flexTable.getCellFormatter().setHorizontalAlignment(4, 0, HasHorizontalAlignment.ALIGN_CENTER);

        flexTable.setWidget(5, 0, horizontalPanel);
        horizontalPanel.setSize("100%", "50px");

        horizontalPanel.add(label_2);
        horizontalPanel.setCellVerticalAlignment(label_2, HasVerticalAlignment.ALIGN_MIDDLE);
        flexTable.getCellFormatter().setWidth(5, 0, "");

        flexTable.setWidget(6, 0, horizontalPanel_buttons);
        horizontalPanel_buttons.setWidth("100%");
        horizontalPanel_buttons.add(button_close);
        horizontalPanel_buttons.setCellVerticalAlignment(button_close, HasVerticalAlignment.ALIGN_MIDDLE);
        horizontalPanel_buttons.setCellHorizontalAlignment(button_close, HasHorizontalAlignment.ALIGN_CENTER);

        horizontalPanel_buttons.add(button_mute);
        horizontalPanel_buttons.setCellWidth(button_mute, "48px");
        horizontalPanel_buttons.setCellHorizontalAlignment(button_mute, HasHorizontalAlignment.ALIGN_RIGHT);
        button_close.addClickHandler(new Button_closeClickHandler());
        flexTable.getFlexCellFormatter().setColSpan(5, 0, 2);
        html_connect.setStyleName("gwt-Label");
        flexTable.getFlexCellFormatter().setColSpan(6, 0, 2);
        this.dataProvider_players = dataProvider_players;
        this.dataProvider_maps = dataProvider_maps;
        this.dataProvider_servers = dataProvider_servers;
    }

    public void fillFields(List<VoteResultDTO> result) {
        // comms
        int i = 0;
        label_comm1.setText(formatVote(dataProvider_players, result.get(i)));
        i++;
        label_comm2.setText(formatVote(dataProvider_players, result.get(i)));
        i++;
        label_maps.setText(formatVote(dataProvider_maps, result.get(i)) + ", " + formatVote(dataProvider_maps, result.get(i + 1)));
        i += 2;
        label_server.setText(formatVote(dataProvider_servers, result.get(i)));
        setSteamConnectUrl(result.get(i));
    }

    private void setSteamConnectUrl(VoteResultDTO voteResultDTO) {
        ServerDTO item = SharedUtils.getObjectFromCollectionById(dataProvider_servers.getList(), voteResultDTO.getId());
        html_connect.setHTML("<a href=\"steam://connect/" + item.getIp() + (!item.getPassword().isEmpty() ? "/" + item.getPassword() : "")
                + "\">Подключиться</a>");
    }

    private String getItemNameById(ListDataProvider<? extends CheckedDTO> dataProvider, VoteResultDTO voteResult) {
        CheckedDTO item = SharedUtils.getObjectFromCollectionById(dataProvider.getList(), voteResult.getId());
        if (item != null) {
            return item.getName();
        } else {
            return "id = " + voteResult.getId();
        }
    }

    private String formatVote(ListDataProvider<? extends CheckedDTO> dataProvider, VoteResultDTO voteResult) {
        return getItemNameById(dataProvider, voteResult) + " [" + voteResult.getVoteCount() + "]";
    }

    private class Button_closeClickHandler implements ClickHandler {
        public void onClick(ClickEvent event) {
            hide();
        }
    }

    @Override
    public void center() {
        setAnimationEnabled(true);
        super.center();
    }

    public Button getButton_mute() {
        return button_mute;
    }
}
