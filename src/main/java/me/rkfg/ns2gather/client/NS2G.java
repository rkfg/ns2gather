package me.rkfg.ns2gather.client;

import static ru.ppsrk.gwt.client.ClientUtils.*;

import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import me.rkfg.ns2gather.dto.ChatMessageType;
import me.rkfg.ns2gather.dto.CheckedDTO;
import me.rkfg.ns2gather.dto.MapDTO;
import me.rkfg.ns2gather.dto.MessageDTO;
import me.rkfg.ns2gather.dto.PlayerDTO;
import me.rkfg.ns2gather.dto.RuleDTO;
import me.rkfg.ns2gather.dto.ServerDTO;
import me.rkfg.ns2gather.dto.VoteResultDTO;
import ru.ppsrk.gwt.client.AlertRuntimeException;
import ru.ppsrk.gwt.client.ClientUtils.MyAsyncCallback;
import ru.ppsrk.gwt.client.LongPollingClient;

import com.google.gwt.cell.client.CheckboxCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.cellview.client.RowStyles;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.SplitLayoutPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.view.client.ListDataProvider;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class NS2G implements EntryPoint {

    public static final long GATHER_ID = 0;
    private SoundManager soundManager = new SoundManager();
    public static RuleDTO[] voteRules = { new RuleDTO(1, 1, 2, "командира"), new RuleDTO(1, 2, 2, "карту"), new RuleDTO(1, 1, 1, "сервер") };
    DateTimeFormat format = DateTimeFormat.getFormat("[HH:mm:ss]");
    private final NS2GServiceAsync ns2gService = GWT.create(NS2GService.class);
    private final Label label_nick = new Label("Ник");
    private final SplitLayoutPanel splitLayoutPanel = new SplitLayoutPanel();
    private final DockLayoutPanel dockLayoutPanel = new DockLayoutPanel(Unit.EM);
    private final ScrollPanel scrollPanel = new ScrollPanel();
    private final HTML html_chat = new HTML("Чат<p/>", true);
    private final TextBox textBox_chatText = new TextBox();
    private final Button button_sendChat = new Button("Отправить");
    private final SplitLayoutPanel splitLayoutPanel_1 = new SplitLayoutPanel();
    private final ListDataProvider<PlayerDTO> dataProvider_players = new ListDataProvider<PlayerDTO>();
    private final DataGrid<PlayerDTO> dataGrid_players = new DataGrid<PlayerDTO>();
    private final TextColumn<PlayerDTO> textColumn_playerName = new TextColumn<PlayerDTO>() {
        @Override
        public String getValue(PlayerDTO object) {
            return object.getName();
        }
    };
    private final Column<PlayerDTO, Boolean> column_voteComm = new Column<PlayerDTO, Boolean>(new CheckboxCell(true, false)) {
        @Override
        public Boolean getValue(PlayerDTO object) {
            return object.getChecked();
        }
    };
    private final ListDataProvider<MapDTO> dataProvider_maps = new ListDataProvider<MapDTO>();
    private final DataGrid<MapDTO> dataGrid_maps = new DataGrid<MapDTO>();
    private final TextColumn<MapDTO> textColumn_mapName = new TextColumn<MapDTO>() {
        @Override
        public String getValue(MapDTO object) {
            return object.getName();
        }
    };
    private final Column<MapDTO, Boolean> column_voteMap = new Column<MapDTO, Boolean>(new CheckboxCell(true, false)) {
        @Override
        public Boolean getValue(MapDTO object) {
            return object.getChecked();
        }
    };
    private final ListDataProvider<ServerDTO> dataProvider_servers = new ListDataProvider<ServerDTO>();
    private final DataGrid<ServerDTO> dataGrid_servers = new DataGrid<ServerDTO>();
    private final TextColumn<ServerDTO> textColumn_serverName = new TextColumn<ServerDTO>() {
        @Override
        public String getValue(ServerDTO object) {
            return object.getName();
        }
    };
    private final Column<ServerDTO, Boolean> column_voteServer = new Column<ServerDTO, Boolean>(new CheckboxCell(true, false)) {
        @Override
        public Boolean getValue(ServerDTO object) {
            return object.getChecked();
        }
    };
    private final HorizontalPanel horizontalPanel = new HorizontalPanel();
    private final Button button_vote = new Button("Голосовать и подтвердить готовность");
    private final FlexTable flexTable = new FlexTable();
    private final Label lblNewLabel = new Label("Проголосовали:");
    private final Label label_voted = new Label("0/0");

    /**
     * This is the entry point method.
     */
    public void onModuleLoad() {
        setupExceptionHandler();
        RootLayoutPanel rootLayoutPanel = RootLayoutPanel.get();

        rootLayoutPanel.add(splitLayoutPanel);

        splitLayoutPanel.addSouth(dockLayoutPanel, 300.0);

        dockLayoutPanel.addNorth(horizontalPanel, 3.0);
        horizontalPanel.setSize("100%", "100%");
        button_vote.addClickHandler(new Button_voteClickHandler());

        horizontalPanel.add(button_vote);
        horizontalPanel.setCellVerticalAlignment(button_vote, HasVerticalAlignment.ALIGN_MIDDLE);
        horizontalPanel.setCellHorizontalAlignment(button_vote, HasHorizontalAlignment.ALIGN_CENTER);
        flexTable.setCellPadding(5);

        dockLayoutPanel.addSouth(flexTable, 6.0);
        flexTable.setSize("100%", "100%");
        label_nick.setWordWrap(false);
        flexTable.setWidget(0, 0, label_nick);
        label_nick.setWidth("10px");
        textBox_chatText.addKeyDownHandler(new TextBox_chatTextKeyDownHandler());
        flexTable.setWidget(0, 1, textBox_chatText);
        flexTable.getCellFormatter().setWidth(0, 1, "100%");
        textBox_chatText.setWidth("100%");
        flexTable.setWidget(0, 2, button_sendChat);

        flexTable.setWidget(1, 0, lblNewLabel);

        flexTable.setWidget(1, 1, label_voted);
        flexTable.getCellFormatter().setHorizontalAlignment(1, 1, HasHorizontalAlignment.ALIGN_LEFT);
        button_sendChat.addClickHandler(new Button_sendChatClickHandler());
        scrollPanel.setStyleName("border-bs");

        dockLayoutPanel.add(scrollPanel);

        scrollPanel.setWidget(html_chat);
        html_chat.setSize("100%", "100%");

        splitLayoutPanel.add(splitLayoutPanel_1);

        splitLayoutPanel_1.addWest(dataGrid_players, 500.0);

        dataGrid_players.addColumn(column_voteComm);
        dataGrid_players.setColumnWidth(column_voteComm, "50px");
        column_voteComm.setFieldUpdater(new FieldUpdater<PlayerDTO, Boolean>() {

            @Override
            public void update(int index, PlayerDTO object, Boolean value) {
                checkLimit(value, object, dataProvider_players, voteRules[0].getVotesLimit());
            }
        });

        dataGrid_players.addColumn(textColumn_playerName, "Имя");

        splitLayoutPanel_1.addEast(dataGrid_servers, 500.0);

        dataGrid_servers.addColumn(column_voteServer);
        dataGrid_servers.setColumnWidth(column_voteServer, "50px");
        column_voteServer.setFieldUpdater(new FieldUpdater<ServerDTO, Boolean>() {

            @Override
            public void update(int index, ServerDTO object, Boolean value) {
                checkLimit(value, object, dataProvider_servers, voteRules[2].getVotesLimit());
            }
        });

        dataGrid_servers.addColumn(textColumn_serverName, "Сервер");

        splitLayoutPanel_1.add(dataGrid_maps);

        dataGrid_maps.addColumn(column_voteMap);
        dataGrid_maps.setColumnWidth(column_voteMap, "50px");
        column_voteMap.setFieldUpdater(new FieldUpdater<MapDTO, Boolean>() {

            @Override
            public void update(int index, MapDTO object, Boolean value) {
                checkLimit(value, object, dataProvider_maps, voteRules[1].getVotesLimit());
            }
        });

        dataGrid_maps.addColumn(textColumn_mapName, "Карта");
        dataProvider_players.addDataDisplay(dataGrid_players);
        dataProvider_maps.addDataDisplay(dataGrid_maps);
        dataProvider_servers.addDataDisplay(dataGrid_servers);
        dataGrid_players.setRowStyles(new RowStyles<PlayerDTO>() {

            @Override
            public String getStyleNames(PlayerDTO row, int rowIndex) {
                return "big-datagrid";
            }
        });
        dataGrid_maps.setRowStyles(new RowStyles<MapDTO>() {

            @Override
            public String getStyleNames(MapDTO row, int rowIndex) {
                return "big-datagrid";
            }
        });
        dataGrid_servers.setRowStyles(new RowStyles<ServerDTO>() {

            @Override
            public String getStyleNames(ServerDTO row, int rowIndex) {
                return "big-datagrid";
            }
        });
        ns2gService.getSteamId(new AsyncCallback<Long>() {

            @Override
            public void onSuccess(Long result) {
                runPing();
                loadMaps();
                loadServers();
                runMessageListener();
            }

            @Override
            public void onFailure(Throwable caught) {
                login();
            }
        });
        ns2gService.getUserName(new AsyncCallback<String>() {

            @Override
            public void onSuccess(String result) {
                label_nick.setText(result + ": ");
                loadPlayers();
            }

            @Override
            public void onFailure(Throwable caught) {

            }
        });
        setVolumes();
    }

    private void setVolumes() {
        soundManager.setVolume(NS2Sound.CHAT, 0.3);
        soundManager.setVolume(NS2Sound.USER_ENTERS, 0.3);
        soundManager.setVolume(NS2Sound.USER_LEAVES, 0.3);
    }

    protected void checkLimit(Boolean value, CheckedDTO object, final ListDataProvider<? extends CheckedDTO> dataProvider, int limit) {
        if (value) {
            int cnt = 0;
            for (CheckedDTO checkedDTO : dataProvider.getList()) {
                if (checkedDTO.getChecked()) {
                    cnt++;
                }
            }
            object.setChecked(cnt < limit);
            dataProvider.refresh();
        } else {
            object.setChecked(false);
        }
    }

    private void runMessageListener() {
        LongPollingClient<List<MessageDTO>> client = new LongPollingClient<List<MessageDTO>>(1000) {

            Long lastMessageUpdate = 0L;

            @Override
            public void success(List<MessageDTO> result) {
                boolean loadPlayers = false;
                boolean voteEnded = false;
                boolean badVote = false;
                NS2Sound soundToPlay = null;
                for (MessageDTO message : result) {
                    if (message.getTimestamp() > lastMessageUpdate) {
                        lastMessageUpdate = message.getTimestamp();
                    }
                    switch (message.getType()) {
                    case USER_ENTERS:
                        addChatMessage(message.getContent() + " входит.", message.getTimestamp());
                        loadPlayers = true;
                        soundToPlay = NS2Sound.USER_ENTERS;
                        break;
                    case USER_LEAVES:
                        addChatMessage(message.getContent() + " покидает нас.", message.getTimestamp());
                        soundToPlay = NS2Sound.USER_LEAVES;
                        loadPlayers = true;
                        break;
                    case USER_READY:
                        addChatMessage(message.getContent() + " готов начать игру!", message.getTimestamp());
                        break;
                    case USER_UNREADY:
                        addChatMessage(message.getContent() + " отменил готовность начать игру.", message.getTimestamp());
                        break;
                    case GAME_START:
                        addChatMessage("Игра начинается!", message.getTimestamp());
                        break;
                    case CHAT_MESSAGE:
                        addChatMessage(message.getContent(), message.getTimestamp(), ChatMessageType.CHAT);
                        soundToPlay = NS2Sound.CHAT;
                        break;
                    case VOTE_CHANGE:
                        label_voted.setText(message.getContent());
                        break;
                    case VOTE_ENDED:
                        voteEnded = true;
                        if (message.getContent().equals("ok")) {
                            addChatMessage("Голосование завершено!", message.getTimestamp());
                        } else {
                            addChatMessage(message.getContent(), message.getTimestamp());
                            badVote = true;
                        }
                        soundToPlay = NS2Sound.VOTE_END;
                        break;
                    default:
                        break;
                    }
                }
                if (loadPlayers) {
                    loadPlayers();
                }
                if (voteEnded) {
                    if (!badVote) {
                        loadVoteResult();
                    }
                }
                if (soundToPlay != null) {
                    soundManager.playSound(soundToPlay);
                }
            }

            @Override
            public void doRPC(LongPollingClient<List<MessageDTO>>.LongPollingAsyncCallback callback) {
                ns2gService.getNewMessages(lastMessageUpdate, callback);
            }
        };
        client.start();
    }

    protected void loadVoteResult() {
        ns2gService.getVoteResults(GATHER_ID, new MyAsyncCallback<List<VoteResultDTO>>() {

            @Override
            public void onSuccess(List<VoteResultDTO> result) {
                new VoteResultPanel(result, dataProvider_players, dataProvider_maps, dataProvider_servers).center();
            }
        });
    }

    protected void addChatMessage(String text, Long timestamp) {
        addChatMessage(text, timestamp, ChatMessageType.SYSTEM);
    }

    protected void loadPlayers() {
        ns2gService.getConnectedPlayers(new MyAsyncCallback<List<PlayerDTO>>() {

            @Override
            public void onSuccess(List<PlayerDTO> result) {
                dataProvider_players.setList(result);
            }
        });
    }

    protected void addChatMessage(String text, long timestamp, ChatMessageType messageType) {
        html_chat.setHTML(html_chat.getHTML() + "<br/>" + format.format(new Date(timestamp)) + " <span class=\""
                + getCSSClassByMessageType(messageType) + "\">" + new SafeHtmlBuilder().appendEscaped(text).toSafeHtml().asString()
                + "</span>");
        scrollPanel.scrollToBottom();
    }

    private String getCSSClassByMessageType(ChatMessageType messageType) {
        switch (messageType) {
        case SYSTEM:
            return "msg_system";
        case CHAT:
            return "msg_chat";
        default:
            return "";
        }
    }

    protected void runPing() {
        new Timer() {

            @Override
            public void run() {
                ns2gService.ping(new MyAsyncCallback<Void>() {

                    @Override
                    public void onSuccess(Void result) {

                    }
                });
            }
        }.scheduleRepeating(5000);
    }

    private void loadServers() {
        ns2gService.getServers(new MyAsyncCallback<List<ServerDTO>>() {

            @Override
            public void onSuccess(List<ServerDTO> result) {
                dataProvider_servers.setList(result);
            }
        });
    }

    private void loadMaps() {
        ns2gService.getMaps(new MyAsyncCallback<List<MapDTO>>() {

            @Override
            public void onSuccess(List<MapDTO> result) {
                dataProvider_maps.setList(result);
            }
        });
    }

    private void login() {
        ns2gService.login(new MyAsyncCallback<String>() {

            @Override
            public void onSuccess(String result) {
                if (result != null) {
                    openPopupPanel(new LoginBox(result));
                }
            }
        });
    }

    private class Button_sendChatClickHandler implements ClickHandler {
        public void onClick(ClickEvent event) {
            if (!textBox_chatText.getValue().isEmpty()) {
                ns2gService.sendChatMessage(textBox_chatText.getValue(), new MyAsyncCallback<Void>() {

                    @Override
                    public void onSuccess(Void result) {
                    }
                });
                textBox_chatText.setText("");
            }
        }
    }

    private class Button_voteClickHandler implements ClickHandler {
        public void onClick(ClickEvent event) {
            ns2gService.vote(collectVotes(), new MyAsyncCallback<Void>() {

                @Override
                public void onSuccess(Void result) {

                }
            });
        }
    }

    public Long[][] collectVotes() {
        Long[][] result = new Long[3][];
        int i = 0;
        for (List<? extends CheckedDTO> checkedDTOs : Arrays.asList(dataProvider_players.getList(), dataProvider_maps.getList(),
                dataProvider_servers.getList())) {
            LinkedList<Long> voteRow = new LinkedList<Long>();
            for (CheckedDTO checkedDTO : checkedDTOs) {
                if (checkedDTO.getChecked()) {
                    voteRow.add(checkedDTO.getId());
                }
            }
            if (voteRow.size() > voteRules[i].getVotesLimit()) {
                throw new AlertRuntimeException("Слишком много голосов за " + voteRules[i].getName() + ", ожидается "
                        + voteRules[i].getVotesRequired() + ", получено " + voteRow.size());
            }
            result[i++] = (Long[]) voteRow.toArray(new Long[0]);
        }
        return result;
    }

    private class TextBox_chatTextKeyDownHandler implements KeyDownHandler {
        public void onKeyDown(KeyDownEvent event) {
            if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
                button_sendChat.click();
            }
        }
    }
}
