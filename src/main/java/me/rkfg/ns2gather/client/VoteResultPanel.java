package me.rkfg.ns2gather.client;

import static ru.ppsrk.gwt.client.ClientUtils.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import me.rkfg.ns2gather.dto.GatherState;
import me.rkfg.ns2gather.dto.PlayerDTO;
import me.rkfg.ns2gather.dto.ServerDTO;
import me.rkfg.ns2gather.dto.Side;
import me.rkfg.ns2gather.dto.VoteResultDTO;
import me.rkfg.ns2gather.dto.VoteType;
import ru.ppsrk.gwt.client.AlertRuntimeException;
import ru.ppsrk.gwt.client.ClientUtils.MyAsyncCallback;
import ru.ppsrk.gwt.client.ResultPopupPanel.ResultPopupPanelCallback;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.cellview.client.CellList;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SingleSelectionModel;

public class VoteResultPanel extends DialogBox {

    private class ParticipantCell extends AbstractCell<PlayerDTO> {

        String baseClass = "";

        public ParticipantCell(Side side) {
            switch (side) {
            case NONE:
                baseClass = "regular";
                break;
            case ALIENS:
                baseClass = "aliens";
                break;
            case MARINES:
                baseClass = "marines";
                break;
            }
        }

        @Override
        public void render(Context context, PlayerDTO value, SafeHtmlBuilder sb) {
            String playerClass = "";
            if (comms.contains(value.getId())) {
                playerClass = "participant captain";
            } else {
                playerClass = "participant " + baseClass;
            }
            sb.appendHtmlConstant("<span class=\"" + playerClass + "\" title=\"" + SafeHtmlUtils.fromString(value.getName()).asString()
                    + "\">");
            value.buildInfo(sb);
            sb.appendHtmlConstant("</span>");
        }
    };

    private SidePick sidePickPanel = new SidePick();
    private final SimplePanel rootPanel = new SimplePanel();
    private final FlexTable flexTable = new FlexTable();
    private final Label label_commA = new Label("Командир А");
    private final Label label_comm1 = new Label("comm1");
    private final Label label_commB = new Label("Командир Б");
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
    private NS2GServiceAsync ns2gService = NS2GServiceAsync.Util.getInstance();
    private final ListDataProvider<PlayerDTO> dataProvider_marines = new ListDataProvider<PlayerDTO>();
    private final CellList<PlayerDTO> cellList_marines = new CellList<PlayerDTO>(new ParticipantCell(Side.MARINES));
    private final ListDataProvider<PlayerDTO> dataProvider_aliens = new ListDataProvider<PlayerDTO>();
    private final CellList<PlayerDTO> cellList_aliens = new CellList<PlayerDTO>(new ParticipantCell(Side.ALIENS));
    private final ListDataProvider<PlayerDTO> dataProvider_nonDistributed = new ListDataProvider<PlayerDTO>();
    private final SingleSelectionModel<PlayerDTO> selectionModel_nonDistributed = new SingleSelectionModel<PlayerDTO>();
    private final CellList<PlayerDTO> cellList_nonDistributed = new CellList<PlayerDTO>(new ParticipantCell(Side.NONE));
    private final Button button_pick = new Button("Забрать");
    private List<Long> comms = new ArrayList<Long>();
    private final FlexTable flexTable_sides = new FlexTable();
    private final ScrollPanel scrollPanel_marines = new ScrollPanel();
    private final ScrollPanel scrollPanel_aliens = new ScrollPanel();
    private final Label lblMarines = new Label("Marines");
    private final Label lblAliens = new Label("Aliens");
    private final ScrollPanel scrollPanel_nonDistributed = new ScrollPanel();
    private final Label label_5 = new Label("Участники");
    private Long myId = null;
    private final Label label_password = new Label("Пароль");

    public VoteResultPanel() {
        setModal(false);
        setText("Результаты голосования");
        flexTable.setCellPadding(5);

        setWidget(rootPanel);
        rootPanel.setWidget(flexTable);
        flexTable.setSize("100%", "100%");
        label_commA.setWordWrap(false);

        flexTable.setWidget(0, 0, label_commA);
        label_comm1.setWordWrap(false);

        flexTable.setWidget(0, 1, label_comm1);

        flexTable.setWidget(1, 0, label_commB);
        label_comm2.setWordWrap(false);

        flexTable.setWidget(1, 1, label_comm2);

        flexTable.setWidget(2, 0, label_3);

        flexTable.setWidget(2, 1, label_maps);

        flexTable.setWidget(3, 0, label_1);

        flexTable.setWidget(3, 1, label_server);

        flexTable.setWidget(4, 0, html_connect);
        flexTable.getCellFormatter().setHorizontalAlignment(4, 0, HasHorizontalAlignment.ALIGN_CENTER);

        label_password.addStyleName("password");
        flexTable.setWidget(5, 0, label_password);

        flexTable.setWidget(6, 0, label_4);

        flexTable.setWidget(7, 0, flexTable_sides);
        flexTable_sides.setSize("100%", "100%");

        flexTable_sides.setWidget(0, 0, lblMarines);

        flexTable_sides.setWidget(0, 1, lblAliens);
        scrollPanel_marines.setStyleName("border-bs");
        flexTable_sides.setWidget(1, 0, scrollPanel_marines);
        flexTable_sides.getCellFormatter().setHeight(1, 0, "150px");
        flexTable_sides.getCellFormatter().setWidth(1, 0, "50%");
        scrollPanel_marines.setSize("100%", "100%");
        scrollPanel_marines.setWidget(cellList_marines);
        cellList_marines.setSize("100%", "100%");
        dataProvider_marines.addDataDisplay(cellList_marines);
        scrollPanel_aliens.setStyleName("border-bs");

        flexTable_sides.setWidget(1, 1, scrollPanel_aliens);
        flexTable_sides.getCellFormatter().setHeight(1, 1, "150px");
        scrollPanel_aliens.setSize("100%", "100%");
        flexTable_sides.getCellFormatter().setWidth(1, 1, "50%");
        scrollPanel_aliens.setWidget(cellList_aliens);
        cellList_aliens.setSize("100%", "100%");
        dataProvider_aliens.addDataDisplay(cellList_aliens);
        flexTable_sides.getCellFormatter().setHorizontalAlignment(0, 0, HasHorizontalAlignment.ALIGN_CENTER);
        flexTable_sides.getCellFormatter().setHorizontalAlignment(0, 1, HasHorizontalAlignment.ALIGN_CENTER);

        flexTable_sides.setWidget(2, 0, label_5);
        scrollPanel_nonDistributed.setStyleName("border-bs");

        flexTable_sides.setWidget(3, 0, scrollPanel_nonDistributed);
        flexTable_sides.getCellFormatter().setWidth(3, 0, "100%");
        flexTable_sides.getCellFormatter().setHeight(3, 0, "200px");
        scrollPanel_nonDistributed.setSize("100%", "100%");
        dataProvider_nonDistributed.addDataDisplay(cellList_nonDistributed);
        scrollPanel_nonDistributed.setWidget(cellList_nonDistributed);
        cellList_nonDistributed.setSelectionModel(selectionModel_nonDistributed);
        flexTable_sides.getFlexCellFormatter().setColSpan(3, 0, 2);
        flexTable_sides.getFlexCellFormatter().setColSpan(2, 0, 2);
        flexTable_sides.getCellFormatter().setHorizontalAlignment(2, 0, HasHorizontalAlignment.ALIGN_CENTER);
        button_pick.addClickHandler(new Button_pickClickHandler());
        button_pick.setEnabled(false);

        flexTable.setWidget(8, 0, button_pick);

        flexTable.setWidget(9, 0, horizontalPanel);
        horizontalPanel.setSize("100%", "50px");

        horizontalPanel.add(label_2);
        horizontalPanel.setCellVerticalAlignment(label_2, HasVerticalAlignment.ALIGN_MIDDLE);
        flexTable.getCellFormatter().setWidth(9, 0, "");

        flexTable.setWidget(10, 0, horizontalPanel_buttons);
        horizontalPanel_buttons.setWidth("100%");
        horizontalPanel_buttons.add(button_close);
        horizontalPanel_buttons.setCellVerticalAlignment(button_close, HasVerticalAlignment.ALIGN_MIDDLE);
        horizontalPanel_buttons.setCellHorizontalAlignment(button_close, HasHorizontalAlignment.ALIGN_CENTER);

        horizontalPanel_buttons.add(button_mute);
        horizontalPanel_buttons.setCellWidth(button_mute, "48px");
        horizontalPanel_buttons.setCellHorizontalAlignment(button_mute, HasHorizontalAlignment.ALIGN_RIGHT);
        button_close.addClickHandler(new Button_closeClickHandler());
        flexTable.getFlexCellFormatter().setColSpan(9, 0, 2);
        html_connect.setStyleName("gwt-Label");
        flexTable.getFlexCellFormatter().setColSpan(10, 0, 2);
        flexTable.getFlexCellFormatter().setColSpan(6, 0, 2);
        flexTable.getCellFormatter().setHorizontalAlignment(6, 0, HasHorizontalAlignment.ALIGN_CENTER);
        flexTable.getFlexCellFormatter().setColSpan(8, 0, 2);
        flexTable.getCellFormatter().setHorizontalAlignment(8, 0, HasHorizontalAlignment.ALIGN_CENTER);
        flexTable.getFlexCellFormatter().setColSpan(7, 0, 2);
        flexTable.getFlexCellFormatter().setColSpan(4, 0, 2);
        flexTable.getFlexCellFormatter().setColSpan(5, 0, 2);
        flexTable.getCellFormatter().setHorizontalAlignment(5, 0, HasHorizontalAlignment.ALIGN_CENTER);
    }

    public void setId(Long id) {
        myId = id;
    }

    public void fillFields(List<VoteResultDTO> result) {
        List<Label> labels = Arrays.asList(label_comm1, label_comm2, label_maps, label_server);
        int i = 0;
        for (Label label : labels) {
            VoteResultDTO voteResultDTO = result.get(i++);
            String name;
            if (voteResultDTO.getTarget() instanceof PlayerDTO) {
                name = ((PlayerDTO) voteResultDTO.getTarget()).getEffectiveName();
            } else {
                name = voteResultDTO.getTarget().getName();
            }
            label.setText(name + " [" + voteResultDTO.getVoteCount() + "]");
        }
        setSteamConnectUrl(result.get(i - 1));
        setPassword(result.get(i - 1));
        comms.clear();
        comms.add(result.get(0).getTarget().getId());
        comms.add(result.get(1).getTarget().getId());
    }

    private void setPassword(VoteResultDTO voteResultDTO) {
        ServerDTO serverDTO = (ServerDTO) voteResultDTO.getTarget();
        String password = serverDTO.getPassword();
        if (password != null && !password.isEmpty()) {
            label_password.setText("Пароль: " + password);
        } else {
            label_password.setText("Пароля нет");
        }
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

    public void center(final GatherState gatherState) {
        setAnimationEnabled(true);
        ns2gService.getVoteResults(new MyAsyncCallback<List<VoteResultDTO>>() {

            @Override
            public void onSuccess(List<VoteResultDTO> result) {
                fillFields(result);
                loadParticipants();
                VoteResultPanel.super.center();
                if (gatherState == GatherState.SIDEPICK && comms.get(0).equals(myId)) {
                    openPopupPanel(sidePickPanel, new ResultPopupPanelCallback<Side>() {

                        @Override
                        public void done(Side result) {
                            ns2gService.pickSide(result, new MyAsyncCallback<Void>() {

                                @Override
                                public void onSuccess(Void result) {

                                }
                            });
                        }
                    });
                }
            }
        });
    }

    public void loadParticipants() {
        ns2gService.getGatherParticipantsList(new MyAsyncCallback<List<PlayerDTO>>() {

            @Override
            public void onSuccess(List<PlayerDTO> result) {
                dataProvider_aliens.getList().clear();
                dataProvider_marines.getList().clear();
                dataProvider_nonDistributed.getList().clear();
                for (PlayerDTO participant : result) {
                    switch (participant.getSide()) {
                    case ALIENS:
                        dataProvider_aliens.getList().add(participant);
                        break;
                    case MARINES:
                        dataProvider_marines.getList().add(participant);
                        Long pId = participant.getId();
                        if (pId.equals(comms.get(0)) || pId.equals(comms.get(1))) {
                            setMarineSide(pId);
                        }
                        break;
                    case NONE:
                        dataProvider_nonDistributed.getList().add(participant);
                        break;
                    }
                }
                button_pick.setEnabled(dataProvider_aliens.getList().size() == dataProvider_marines.getList().size()
                        && myId.equals(comms.get(1)) || dataProvider_aliens.getList().size() != dataProvider_marines.getList().size()
                        && myId.equals(comms.get(0)));
            }
        });
    }

    public Button getButton_mute() {
        return button_mute;
    }

    public void enablePickButton(boolean enabled) {
        button_pick.setEnabled(enabled);
    }

    public void setMarineSide(Long commId) {
        if (commId.equals(comms.get(0))) {
            label_commA.setText("Командир Marines");
            label_commB.setText("Командир Aliens");
        } else {
            label_commB.setText("Командир Marines");
            label_commA.setText("Командир Aliens");
        }
    }

    private class Button_pickClickHandler implements ClickHandler {
        public void onClick(ClickEvent event) {
            PlayerDTO selected = trySelectionModelValue(selectionModel_nonDistributed, "Выберите участника.", PlayerDTO.class);
            ns2gService.pickPlayer(selected.getId(), new MyAsyncCallback<Void>() {

                @Override
                public void onSuccess(Void result) {

                }
            });
        }
    }
}
