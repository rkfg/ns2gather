package me.rkfg.ns2gather.client;

import java.util.Arrays;
import java.util.List;

import me.rkfg.ns2gather.dto.ServerDTO;
import me.rkfg.ns2gather.dto.VoteResultDTO;
import me.rkfg.ns2gather.dto.VoteType;
import ru.ppsrk.gwt.client.AlertRuntimeException;
import ru.ppsrk.gwt.client.ClientUtils.MyAsyncCallback;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;

public class VoteResultPanel extends DialogBox {
    private final SimplePanel rootPanel = new SimplePanel();
    private final FlexTable flexTable = new FlexTable();
    private final Label label = new Label("Командир А");
    private final Label label_comm1 = new Label("comm1");
    private final Label lblNewLabel_1 = new Label("Командир Б");
    private final Label label_comm2 = new Label("comm2");
    private final Label label_3 = new Label("Карта");
    private final Label label_maps = new Label("maps");
    private final Label label_1 = new Label("Сервер");
    private final Label label_server = new Label("server");
    private final HTML html_connect = new HTML("<a href=\"steam://connect/\">Подключиться</a>", true);
    private final Button button_close = new Button("Закрыть");
    private final HorizontalPanel horizontalPanel = new HorizontalPanel();
    private final Label label_2 = new Label("Вы сможете снова открыть это окно, щёлкнув по статусу gather'а.");
    private final HorizontalPanel horizontalPanel_buttons = new HorizontalPanel();
    private final Button button_mute = new Button("<img src=\"icons/mute.png\">");
    private final Label label_4 = new Label("Список участников:");
    private final Label label_playersList = new Label("");
    private NS2GServiceAsync ns2gService = NS2GServiceAsync.Util.getInstance();

    public VoteResultPanel() {
        setModal(false);
        setText("Результаты голосования");
        flexTable.setCellPadding(5);

        setWidget(rootPanel);
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

        flexTable.setWidget(5, 0, label_4);

        flexTable.setWidget(6, 0, label_playersList);

        flexTable.setWidget(7, 0, horizontalPanel);
        horizontalPanel.setSize("100%", "50px");

        horizontalPanel.add(label_2);
        horizontalPanel.setCellVerticalAlignment(label_2, HasVerticalAlignment.ALIGN_MIDDLE);
        flexTable.getCellFormatter().setWidth(7, 0, "");

        flexTable.setWidget(8, 0, horizontalPanel_buttons);
        horizontalPanel_buttons.setWidth("100%");
        horizontalPanel_buttons.add(button_close);
        horizontalPanel_buttons.setCellVerticalAlignment(button_close, HasVerticalAlignment.ALIGN_MIDDLE);
        horizontalPanel_buttons.setCellHorizontalAlignment(button_close, HasHorizontalAlignment.ALIGN_CENTER);

        horizontalPanel_buttons.add(button_mute);
        horizontalPanel_buttons.setCellWidth(button_mute, "48px");
        horizontalPanel_buttons.setCellHorizontalAlignment(button_mute, HasHorizontalAlignment.ALIGN_RIGHT);
        button_close.addClickHandler(new Button_closeClickHandler());
        flexTable.getFlexCellFormatter().setColSpan(7, 0, 2);
        html_connect.setStyleName("gwt-Label");
        flexTable.getFlexCellFormatter().setColSpan(8, 0, 2);
        flexTable.getFlexCellFormatter().setColSpan(5, 0, 2);
        flexTable.getCellFormatter().setHorizontalAlignment(5, 0, HasHorizontalAlignment.ALIGN_CENTER);
        flexTable.getFlexCellFormatter().setColSpan(6, 0, 2);
    }

    public void fillFields(List<VoteResultDTO> result) {
        List<Label> labels = Arrays.asList(label_comm1, label_comm2, label_maps, label_server);
        int i = 0;
        for (Label label : labels) {
            VoteResultDTO voteResultDTO = result.get(i++);
            label.setText(voteResultDTO.getTarget().getName() + " [" + voteResultDTO.getVoteCount() + "]");
        }
        setSteamConnectUrl(result.get(i - 1));
    }

    private void setSteamConnectUrl(VoteResultDTO voteResultDTO) {
        if (voteResultDTO.getType() != VoteType.SERVER) {
            throw new AlertRuntimeException("Неверный тип результата голосования, ожидался голос за сервер, получено "
                    + voteResultDTO.getType());
        }
        ServerDTO item = (ServerDTO) voteResultDTO.getTarget();
        html_connect.setHTML("<a href=\"steam://connect/" + item.getIp() + (!item.getPassword().isEmpty() ? "/" + item.getPassword() : "")
                + "\">Подключиться</a>");
    }

    private class Button_closeClickHandler implements ClickHandler {
        public void onClick(ClickEvent event) {
            button_mute.click();
            hide();
        }
    }

    @Override
    public void center() {
        setAnimationEnabled(true);
        ns2gService.getVoteResults(new MyAsyncCallback<List<VoteResultDTO>>() {

            @Override
            public void onSuccess(List<VoteResultDTO> result) {
                fillFields(result);
                VoteResultPanel.super.center();
            }
        });
        ns2gService.getGatherPlayersList(new MyAsyncCallback<String>() {

            @Override
            public void onSuccess(String result) {
                label_playersList.setText(result);
            }
        });
    }

    public Button getButton_mute() {
        return button_mute;
    }
}
