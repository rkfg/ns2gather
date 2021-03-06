package me.rkfg.ns2gather.client;

import static ru.ppsrk.gwt.client.ClientUtils.*;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import me.rkfg.ns2gather.dto.ChatMessageType;
import me.rkfg.ns2gather.dto.CheckedDTO;
import me.rkfg.ns2gather.dto.GatherState;
import me.rkfg.ns2gather.dto.InitStateDTO;
import me.rkfg.ns2gather.dto.MapDTO;
import me.rkfg.ns2gather.dto.MementoCheckedDTO;
import me.rkfg.ns2gather.dto.MessageDTO;
import me.rkfg.ns2gather.dto.PlayerDTO;
import me.rkfg.ns2gather.dto.ServerDTO;
import ru.ppsrk.gwt.client.AlertRuntimeException;
import ru.ppsrk.gwt.client.ClientUtils.MyAsyncCallback;
import ru.ppsrk.gwt.client.LongPollingClient;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.cell.client.ButtonCell;
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
import com.google.gwt.user.client.Cookies;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.Window.Location;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
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

    private boolean ready = false;
    private SoundManager soundManager = new SoundManager();
    private CookieSettingsManager cookieSettingsManager = new CookieSettingsManager();
    DateTimeFormat format = DateTimeFormat.getFormat("[HH:mm:ss]");
    private final NS2GServiceAsync ns2gService = GWT.create(NS2GService.class);
    private final HTML label_nick = new HTML("Ник");
    private final SplitLayoutPanel splitLayoutPanel_main = new SplitLayoutPanel();
    private final DockLayoutPanel dockLayoutPanel_chat = new DockLayoutPanel(Unit.EM);
    private final ScrollPanel scrollPanel_userChat = new ScrollPanel();
    private final FlowPanel user_chat = new FlowPanel();
    private final TextBox textBox_chatText = new TextBox();
    private final Button button_sendChat = new Button("Отправить");
    private final SplitLayoutPanel splitLayoutPanel_data = new SplitLayoutPanel();
    private final ListDataProvider<PlayerDTO> dataProvider_players = new ListDataProvider<PlayerDTO>();
    private final DataGrid<PlayerDTO> dataGrid_players = new DataGrid<PlayerDTO>();
    private final Column<PlayerDTO, PlayerDTO> textColumn_playerName = new Column<PlayerDTO, PlayerDTO>(new AbstractCell<PlayerDTO>() {

        @Override
        public void render(Context context, PlayerDTO value, SafeHtmlBuilder sb) {
            value.buildLink(sb, true);
        }
    }) {
        @Override
        public PlayerDTO getValue(PlayerDTO object) {
            return object;
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
            return object.getName() + " [" + object.getPlayers().size() + "]";
        }
    };
    private final Column<ServerDTO, Boolean> column_voteServer = new Column<ServerDTO, Boolean>(new CheckboxCell(true, false)) {
        @Override
        public Boolean getValue(ServerDTO object) {
            return object.getChecked();
        }
    };
    private final HorizontalPanel horizontalPanel = new HorizontalPanel();
    private final VoteButton button_vote = new VoteButton() {

        @Override
        protected void vote() {
            ns2gService.vote(collectVotes(), new MyAsyncCallback<Void>() {

                @Override
                public void onSuccess(Void result) {
                    switchState();
                }
            });
        }

        @Override
        protected void unvote() {
            ns2gService.unvote(new MyAsyncCallback<Void>() {

                @Override
                public void onSuccess(Void result) {
                    switchState();
                }

            });
        }
    };
    private final FlexTable flexTable = new FlexTable();
    private final Label label_voted = new Label("0/0");
    private final VolumeButton volumeControl = new VolumeButton(cookieSettingsManager) {

        @Override
        public void volumeChanged(int newVolume) {
            setChatVolume(newVolume);
            if (ready) {
                soundManager.playSound(NS2Sound.CHAT);
            }
        }
    };
    private final HorizontalPanel horizontalPanel_header = new HorizontalPanel();
    private final GatherStatusLabel gatherStatusLabel = new GatherStatusLabel(new ClickHandler() {

        @Override
        public void onClick(ClickEvent event) {
            if (!Arrays.asList(GatherState.COMPLETED, GatherState.SIDEPICK, GatherState.PLAYERS).contains(
                    gatherStatusLabel.getGatherState())) {
                return;
            }
            loadVoteResult();
        }
    });
    private final Button button_enterNewGather = new Button("Зайти в новый сбор");
    private final Button button_logout = new Button("Выход");
    private Set<Long> votedPlayers = new HashSet<Long>();
    private VoteResultPanel voteResultPanel = new VoteResultPanel();
    private ServerPlayersPanel serverPlayersPanel = new ServerPlayersPanel();
    private final HorizontalPanel horizontalPanel_voteButton = new HorizontalPanel();
    private final HTML html_version = new HTML();
    private final Button button_rules = new Button("Правила");
    private final FlexTable flexTable_cornerControls = new FlexTable();
    private final Column<ServerDTO, String> column_playersList = new Column<ServerDTO, String>(new ButtonCell()) {
        @Override
        public String getValue(ServerDTO object) {
            return "?";
        }
    };
    private PlayerDTO myPlayer;
    private final SplitLayoutPanel splitLayoutPanel_chat = new SplitLayoutPanel();
    private final ScrollPanel scrollPanel_systemChat = new ScrollPanel();
    private final FlowPanel system_chat = new FlowPanel();
    private final Button button_setNick = new Button("Задать ник");
    private Timer pingTimer;

    /**
     * This is the entry point method.
     */
    public void onModuleLoad() {
        setupExceptionHandler();
        RootLayoutPanel rootLayoutPanel = RootLayoutPanel.get();

        rootLayoutPanel.add(splitLayoutPanel_main);
        horizontalPanel_header.setSpacing(5);

        splitLayoutPanel_main.addNorth(horizontalPanel_header, 70.0);
        horizontalPanel_header.setSize("100%", "100%");

        horizontalPanel_header.add(gatherStatusLabel);
        horizontalPanel_header.setCellVerticalAlignment(gatherStatusLabel, HasVerticalAlignment.ALIGN_MIDDLE);
        horizontalPanel_header.setCellHorizontalAlignment(gatherStatusLabel, HasHorizontalAlignment.ALIGN_CENTER);
        flexTable_cornerControls.setCellPadding(3);

        horizontalPanel_header.add(flexTable_cornerControls);
        horizontalPanel_header.setCellWidth(flexTable_cornerControls, "1px");
        horizontalPanel_header.setCellHorizontalAlignment(flexTable_cornerControls, HasHorizontalAlignment.ALIGN_RIGHT);
        flexTable_cornerControls.setWidget(0, 0, button_rules);
        button_rules.addClickHandler(new Button_rulesClickHandler());
        horizontalPanel_header.setCellWidth(button_rules, "1px");
        horizontalPanel_header.setCellHorizontalAlignment(button_rules, HasHorizontalAlignment.ALIGN_RIGHT);
        button_setNick.addClickHandler(new Button_setNickClickHandler());

        flexTable_cornerControls.setWidget(0, 1, button_setNick);
        button_setNick.setWidth("150px");
        flexTable_cornerControls.setWidget(0, 2, button_logout);
        button_logout.addClickHandler(new Button_logoutClickHandler());
        horizontalPanel_header.setCellWidth(button_logout, "1px");
        horizontalPanel_header.setCellHorizontalAlignment(button_logout, HasHorizontalAlignment.ALIGN_RIGHT);
        flexTable_cornerControls.getCellFormatter().setHorizontalAlignment(1, 0, HasHorizontalAlignment.ALIGN_RIGHT);
        html_version.setText("Версия");
        flexTable_cornerControls.setWidget(1, 0, html_version);
        html_version.setWordWrap(false);

        html_version.addStyleName("version");
        flexTable_cornerControls.getFlexCellFormatter().setColSpan(1, 0, 3);
        flexTable_cornerControls.getCellFormatter().setHorizontalAlignment(0, 0, HasHorizontalAlignment.ALIGN_CENTER);
        flexTable_cornerControls.getCellFormatter().setHorizontalAlignment(0, 2, HasHorizontalAlignment.ALIGN_RIGHT);

        splitLayoutPanel_main.addSouth(dockLayoutPanel_chat, 300.0);
        horizontalPanel.setSpacing(5);

        dockLayoutPanel_chat.addNorth(horizontalPanel, 3.0);
        horizontalPanel.setSize("100%", "100%");

        horizontalPanel.add(volumeControl);
        horizontalPanel.setCellVerticalAlignment(volumeControl, HasVerticalAlignment.ALIGN_MIDDLE);
        horizontalPanel.setCellWidth(volumeControl, "1px");
        horizontalPanel_voteButton.setSpacing(5);

        horizontalPanel.add(horizontalPanel_voteButton);
        horizontalPanel.setCellHorizontalAlignment(horizontalPanel_voteButton, HasHorizontalAlignment.ALIGN_CENTER);
        horizontalPanel.setCellVerticalAlignment(horizontalPanel_voteButton, HasVerticalAlignment.ALIGN_MIDDLE);
        horizontalPanel_voteButton.add(button_vote);
        horizontalPanel_voteButton.setCellVerticalAlignment(button_vote, HasVerticalAlignment.ALIGN_MIDDLE);
        horizontalPanel.setCellVerticalAlignment(button_vote, HasVerticalAlignment.ALIGN_MIDDLE);
        horizontalPanel.setCellHorizontalAlignment(button_vote, HasHorizontalAlignment.ALIGN_CENTER);
        horizontalPanel_voteButton.add(label_voted);
        horizontalPanel_voteButton.setCellVerticalAlignment(label_voted, HasVerticalAlignment.ALIGN_MIDDLE);
        button_enterNewGather.setVisible(false);
        button_enterNewGather.addClickHandler(new Button_enterNewGatherClickHandler());

        horizontalPanel.add(button_enterNewGather);
        horizontalPanel.setCellWidth(button_enterNewGather, "300px");
        horizontalPanel.setCellVerticalAlignment(button_enterNewGather, HasVerticalAlignment.ALIGN_MIDDLE);
        horizontalPanel.setCellHorizontalAlignment(button_enterNewGather, HasHorizontalAlignment.ALIGN_RIGHT);
        flexTable.setCellPadding(5);

        dockLayoutPanel_chat.addSouth(flexTable, 3.0);
        flexTable.setSize("100%", "100%");
        label_nick.setWordWrap(false);
        flexTable.setWidget(0, 0, label_nick);
        textBox_chatText.setMaxLength(ClientSettings.CHAT_MAX_LENGTH);
        textBox_chatText.addKeyDownHandler(new TextBox_chatTextKeyDownHandler());
        flexTable.setWidget(0, 1, textBox_chatText);
        flexTable.getCellFormatter().setWidth(0, 1, "100%");
        textBox_chatText.setWidth("100%");
        flexTable.setWidget(0, 2, button_sendChat);
        button_sendChat.addClickHandler(new Button_sendChatClickHandler());

        dockLayoutPanel_chat.add(splitLayoutPanel_chat);
        scrollPanel_systemChat.setStyleName("border-bs");

        splitLayoutPanel_chat.addEast(scrollPanel_systemChat, 400.0);

        scrollPanel_systemChat.setWidget(system_chat);
        system_chat.setSize("100%", "100%");
        splitLayoutPanel_chat.add(scrollPanel_userChat);
        scrollPanel_userChat.setStyleName("border-bs");

        scrollPanel_userChat.setWidget(user_chat);
        user_chat.setSize("100%", "100%");

        splitLayoutPanel_main.add(splitLayoutPanel_data);

        splitLayoutPanel_data.addWest(dataGrid_players, 400.0);

        dataGrid_players.addColumn(column_voteComm);
        dataGrid_players.setColumnWidth(column_voteComm, "50px");
        dataGrid_players.addColumn(textColumn_playerName, "Имя");

        splitLayoutPanel_data.addEast(dataGrid_servers, 300.0);

        dataGrid_servers.addColumn(column_voteServer);
        dataGrid_servers.setColumnWidth(column_voteServer, "50px");
        dataGrid_servers.addColumn(textColumn_serverName, "Сервер");

        splitLayoutPanel_data.add(dataGrid_maps);

        dataGrid_maps.addColumn(column_voteMap);
        dataGrid_maps.setColumnWidth(column_voteMap, "50px");
        dataGrid_maps.addColumn(textColumn_mapName, "Карта");
        dataProvider_players.addDataDisplay(dataGrid_players);
        dataProvider_maps.addDataDisplay(dataGrid_maps);
        dataProvider_servers.addDataDisplay(dataGrid_servers);

        dataGrid_servers.addColumn(column_playersList);
        dataGrid_servers.setColumnWidth(column_playersList, "70px");
        init();
        ready = true;
    }

    private void init() {
        column_voteComm.setFieldUpdater(new FieldUpdater<PlayerDTO, Boolean>() {

            @Override
            public void update(int index, PlayerDTO object, Boolean value) {
                checkLimit(value, object, dataProvider_players, ClientSettings.voteRules[0].getVotesLimit());
            }
        });

        column_voteServer.setFieldUpdater(new FieldUpdater<ServerDTO, Boolean>() {

            @Override
            public void update(int index, ServerDTO object, Boolean value) {
                checkLimit(value, object, dataProvider_servers, ClientSettings.voteRules[2].getVotesLimit());
            }
        });

        column_voteMap.setFieldUpdater(new FieldUpdater<MapDTO, Boolean>() {

            @Override
            public void update(int index, MapDTO object, Boolean value) {
                checkLimit(value, object, dataProvider_maps, ClientSettings.voteRules[1].getVotesLimit());
            }
        });

        column_playersList.setFieldUpdater(new FieldUpdater<ServerDTO, String>() {

            @Override
            public void update(int index, ServerDTO object, String value) {
                serverPlayersPanel.init(object);
                serverPlayersPanel.center();
            }
        });

        dataGrid_players.setRowStyles(new RowStyles<PlayerDTO>() {

            @Override
            public String getStyleNames(PlayerDTO row, int rowIndex) {
                String result = "big-datagrid";
                if (votedPlayers.contains(row.getId())) {
                    result += " voted";
                }
                return result;
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

        ns2gService.getPlayer(new AsyncCallback<PlayerDTO>() {

            @Override
            public void onSuccess(PlayerDTO result) {
                initPlayer(result);
            }

            @Override
            public void onFailure(Throwable caught) {
                if (caught instanceof AnonymousAuthException) {
                    initPlayer(new PlayerDTO(0L, "Anonymous", "", System.currentTimeMillis()));
                } else {
                    login();
                }
            }
        });
        voteResultPanel.getButton_mute().addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                soundManager.stopSound(NS2Sound.VOTE_END);
            }
        });
        soundManager.setLoop(NS2Sound.VOTE_END, true);
        restorePanelsSize();
    }

    private void restorePanelsSize() {
        splitLayoutPanel_data.setWidgetSize(dataGrid_players,
                cookieSettingsManager.getDoubleCookie(CookieSettingsManager.PLAYER_PANEL_COOKIE, 400.0));
        splitLayoutPanel_data.setWidgetSize(dataGrid_servers,
                cookieSettingsManager.getDoubleCookie(CookieSettingsManager.SERVER_PANEL_COOKIE, 300.0));
        splitLayoutPanel_main.setWidgetSize(dockLayoutPanel_chat,
                cookieSettingsManager.getDoubleCookie(CookieSettingsManager.CHAT_PANEL_COOKIE, 300.0));
        splitLayoutPanel_main.setWidgetSize(horizontalPanel_header,
                cookieSettingsManager.getDoubleCookie(CookieSettingsManager.HEADER_PANEL_COOKIE, 70.0));
        splitLayoutPanel_chat.setWidgetSize(scrollPanel_systemChat,
                cookieSettingsManager.getDoubleCookie(CookieSettingsManager.CHAT_SYSTEM_PANEL_COOKIE, 400.0));
    }

    protected void postRulesAnnounce() {
        if (myPlayer.getId().equals(0L)) {
            addChatMessage(
                    "Вы вошли в Gather анонимно. Вы не можете голосовать и писать в чат, но можете следить за ходом голосования и набором команд. "
                            + "Ознакомьтесь с <a href=\"rules.html\" target=\"_blank\">Правилами</a>", System.currentTimeMillis(),
                    ChatMessageType.SYSTEM);
        } else {
            addChatMessage(
                    "Вы заявили свою готовность играть Gather. Ознакомьтесь с <a href=\"rules.html\" target=\"_blank\">Правилами</a>",
                    System.currentTimeMillis(), ChatMessageType.SYSTEM);
        }
    }

    protected void loadInitState() {
        ns2gService.getInitState(new MyAsyncCallback<InitStateDTO>() {

            @Override
            public void onSuccess(InitStateDTO result) {
                gatherStatusLabel.setGatherState(result.getGatherState());
                if (isGatherClosed(result.getGatherState())) {
                    button_vote.setEnabled(false);
                }
                updateEnterNewButtonVisibility();
                votedPlayers = result.getVotedIds();
                dataProvider_players.setList(result.getPlayers());
                dataProvider_maps.setList(result.getMaps());
                dataProvider_servers.setList(result.getServers());
                label_voted.setText(result.getVoteStat());
                html_version.setHTML("<a href=\"https://github.com/rkfg/ns2gather/issues\" title=\"Сообщить об ошибке\" target=\"_blank\">"
                        + result.getVersion() + "</a>");
                if (result.getPasswords() != null) {
                    addChatMessage(result.getPasswords(), System.currentTimeMillis(), ChatMessageType.SYSTEM);
                }
                if (result.getVoteResults() != null) {
                    voteResultPanel.fillFields(result.getVoteResults());
                }
                if (!myPlayer.getId().equals(0L)) {
                    ns2gService.ping(new MyAsyncCallback<Void>() {

                        @Override
                        public void onSuccess(Void result) {
                            runPing();
                        }
                    });
                }
                runMessageListener();
            }

            @Override
            public void onFailure(Throwable caught) {
                super.onFailure(caught);
                button_enterNewGather.setVisible(true);
            }

        });
    }

    protected void fakeLogin() {
        openPopupPanel(new FakeLoginBox());
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

            Long lastMessageUpdate = -ClientSettings.MESSAGES_ROLLBACK;

            @Override
            public void success(List<MessageDTO> result) {
                boolean loadPlayers = false;
                boolean voteEnded = false;
                boolean badVote = false;
                Long id;
                soundManager.clearQueue();
                for (MessageDTO message : result) {
                    if (message.getTimestamp() > lastMessageUpdate) {
                        lastMessageUpdate = message.getTimestamp();
                    }
                    switch (message.getType()) {
                    case USER_ENTERS:
                        addChatMessage(message.getContent() + " входит.", message.getTimestamp());
                        loadPlayers = true;
                        soundManager.queue(NS2Sound.USER_ENTERS);
                        break;
                    case USER_LEAVES:
                        id = Long.valueOf(message.getContent());
                        if (id.equals(myPlayer.getId())) {
                            break;
                        }
                        addPlayerMessage(message, " покидает нас.");
                        votedPlayers.remove(id);
                        dataGrid_players.redraw();
                        soundManager.queue(NS2Sound.USER_LEAVES);
                        loadPlayers = true;
                        break;
                    case USER_READY:
                        id = Long.valueOf(message.getContent());
                        votedPlayers.add(id);
                        dataGrid_players.redraw();
                        addPlayerMessage(message, " готов начать игру!");
                        break;
                    case USER_UNREADY:
                        id = Long.valueOf(message.getContent());
                        votedPlayers.remove(id);
                        dataGrid_players.redraw();
                        addPlayerMessage(message, " отменил готовность начать игру.");
                        break;
                    case CHAT_MESSAGE:
                        addChatMessage(message.getContent(), message.getTimestamp(), ChatMessageType.CHAT);
                        soundManager.queue(NS2Sound.CHAT);
                        break;
                    case VOTE_CHANGE:
                        label_voted.setText(message.getContent());
                        break;
                    case VOTE_ENDED:
                        voteEnded = true;
                        if (message.getContent().equals("ok")) {
                            addChatMessage("Голосование завершено!", message.getTimestamp());
                            soundManager.queue(NS2Sound.VOTE_END);
                            button_vote.setEnabled(false);
                        } else {
                            addChatMessage(message.getContent(), message.getTimestamp());
                            badVote = true;
                            button_vote.setState(true);
                            button_vote.switchState();
                            soundManager.queue(NS2Sound.REVOTE);
                        }
                        break;
                    case GATHER_STATUS:
                        gatherStatusLabel.setGatherState(GatherState.values()[Integer.valueOf(message.getContent())]);
                        updateEnterNewButtonVisibility();
                        break;
                    case USER_KICKED:
                        Location.reload();
                        break;
                    case RUN_TIMER:
                        gatherStatusLabel.runTimer(Long.valueOf(message.getContent()));
                        soundManager.queue(NS2Sound.TIMER);
                        break;
                    case STOP_TIMER:
                        gatherStatusLabel.stopTimer();
                        break;
                    case MORE_PLAYERS:
                        addChatMessage("Происходит донабор человека для игры в формате 7x7 (8x8)", message.getTimestamp());
                        soundManager.queue(NS2Sound.MORE);
                        break;
                    case RESET_HIGHLIGHT:
                        resetHighlight();
                        break;
                    case PICKED:
                        voteResultPanel.loadParticipants();
                        break;
                    case SERVER_UPDATE:
                        loadServers();
                        break;
                    case PLAYERS_UPDATE:
                        loadPlayers();
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
                soundManager.playQueued();
            }

            @Override
            public void doRPC(LongPollingClient<List<MessageDTO>>.LongPollingAsyncCallback callback) {
                ns2gService.getNewMessages(lastMessageUpdate, callback);
            }
        };
        client.start();
    }

    private void addPlayerMessage(MessageDTO message, String messageText) {
        String name = getNameById(dataProvider_players.getList(), Long.valueOf(message.getContent()));
        if (name != null) {
            addChatMessage(name + messageText, message.getTimestamp());
        }
    }

    protected String getNameById(List<PlayerDTO> list, Long id) {
        for (PlayerDTO playerDTO : list) {
            if (playerDTO.getId().equals(id)) {
                return playerDTO.getEffectiveName();
            }
        }
        return null;
    }

    protected void loadServers() {
        ns2gService.getServers(new MyAsyncCallback<List<ServerDTO>>() {

            @Override
            public void onSuccess(List<ServerDTO> result) {
                MementoCheckedDTO<ServerDTO> serverMemento = new MementoCheckedDTO<ServerDTO>();
                serverMemento.storeChecks(dataProvider_servers.getList());
                dataProvider_servers.setList(result);
                serverMemento.restoreChecks(dataProvider_servers.getList());
            }
        });
    }

    protected void resetHighlight() {
        votedPlayers = new HashSet<Long>();
        dataGrid_players.redraw();
    }

    protected void loadVoteResult() {
        voteResultPanel.center(gatherStatusLabel.getGatherState());
    }

    protected void addChatMessage(String text, Long timestamp) {
        addChatMessage(text, timestamp, ChatMessageType.SYSTEM);
    }

    protected void loadPlayers() {
        ns2gService.getConnectedPlayers(new MyAsyncCallback<List<PlayerDTO>>() {

            @Override
            public void onSuccess(List<PlayerDTO> result) {
                MementoCheckedDTO<PlayerDTO> memento = new MementoCheckedDTO<PlayerDTO>();
                memento.storeChecks(dataProvider_players.getList());
                dataProvider_players.setList(result);
                memento.restoreChecks(result);
            }
        });
    }

    protected void addChatMessage(String text, long timestamp, ChatMessageType messageType) {
        boolean shouldScroll_user = scrollPanel_userChat.getVerticalScrollPosition() == scrollPanel_userChat
                .getMaximumVerticalScrollPosition();
        boolean shouldScroll_system = scrollPanel_systemChat.getVerticalScrollPosition() == scrollPanel_systemChat
                .getMaximumVerticalScrollPosition();
        HTML msg = buildChatMessage(text, timestamp, messageType);
        switch (messageType) {
        case CHAT:
            user_chat.add(msg);
            trimChat(user_chat);
            break;
        case SYSTEM:
            system_chat.add(msg);
            trimChat(system_chat);
            break;
        }
        if (shouldScroll_user) {
            if (text.contains("<img src=")) {
                new Timer() {

                    @Override
                    public void run() {
                        scrollPanel_userChat.scrollToBottom();
                    }
                }.schedule(ClientSettings.IMAGE_SCROLL_DELAY);
            } else {
                scrollPanel_userChat.scrollToBottom();
            }
        }
        if (shouldScroll_system) {
            scrollPanel_systemChat.scrollToBottom();
        }
    }

    private void trimChat(FlowPanel chat) {
        if (chat.getWidgetCount() > ClientSettings.CHAT_MAX_MESSAGES) {
            chat.remove(0);
        }
    }

    private HTML buildChatMessage(String text, long timestamp, ChatMessageType messageType) {
        return new HTML(format.format(new Date(timestamp)) + " <span class=\"" + getCSSClassByMessageType(messageType) + "\">" + text
                + "</span>");
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
        pingTimer = new Timer() {

            @Override
            public void run() {
                ns2gService.ping(new MyAsyncCallback<Void>() {

                    @Override
                    public void onSuccess(Void result) {

                    }
                });
            }
        };
        pingTimer.scheduleRepeating(ClientSettings.PING_INTERVAL);
    }

    protected void login() {
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
            if (voteRow.size() > ClientSettings.voteRules[i].getVotesLimit()) {
                throw new AlertRuntimeException("Слишком много голосов за " + ClientSettings.voteRules[i].getName() + ", ожидается "
                        + ClientSettings.voteRules[i].getVotesRequired() + ", получено " + voteRow.size());
            }
            result[i++] = (Long[]) voteRow.toArray(new Long[0]);
        }
        return result;
    }

    private void setChatVolume(int volume) {
        for (NS2Sound ns2Sound : Arrays.asList(NS2Sound.CHAT, NS2Sound.USER_ENTERS, NS2Sound.USER_LEAVES)) {
            soundManager.setVolume(ns2Sound, volume / 200.0);
        }
    }

    private class TextBox_chatTextKeyDownHandler implements KeyDownHandler {
        public void onKeyDown(KeyDownEvent event) {
            if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
                button_sendChat.click();
            }
        }
    }

    private class Button_logoutClickHandler implements ClickHandler {
        public void onClick(ClickEvent event) {
            switch (gatherStatusLabel.getGatherState()) {
            case CLOSED:
            case OPEN:
            case ONTIMER:
                if (!Window.confirm("Вы действительно хотите покинуть сбор? Если все остальные участники проголосуют, "
                        + "вы не сможете вернуться к ним и попадёте в новый сбор.")) {
                    return;
                }
                break;
            case PLAYERS:
            case SIDEPICK:
                Window.alert("Голосование уже завершено. Пожалуйста, дождитесь выбора сторон и набора команд. "
                        + "Не закрывайте вкладку, если вы покинете сбор на этой стадии, к вам могут быть применены санкции согласно правилам.");
                return;
            case COMPLETED:
                if (!Window
                        .confirm("Вы действительно хотите покинуть сбор? После входа вы попадёте в новый сбор и больше не сможете посмотреть "
                                + "результаты этого сбора и пароль для подключения. Вы также можете нажать кнопку «Зайти в новый сбор», чтобы попасть в новый сбор без перезахода.")) {
                    return;
                }
                break;
            }
            if (pingTimer != null) {
                pingTimer.cancel();
            }
            ns2gService.logout(new MyAsyncCallback<Void>() {

                @Override
                public void onSuccess(Void result) {
                    Cookies.removeCookie(CookieSettingsManager.REMEMBER_STEAM_ID);
                    Location.reload();
                }
            });
        }
    }

    private class Button_enterNewGatherClickHandler implements ClickHandler {
        public void onClick(ClickEvent event) {
            ns2gService.resetGatherPresence(new MyAsyncCallback<Void>() {

                @Override
                public void onSuccess(Void result) {
                    Location.reload();
                }
            });
        }
    }

    public void updateEnterNewButtonVisibility() {
        button_enterNewGather.setVisible(gatherStatusLabel.getState() == GatherState.COMPLETED);
    }

    private class Button_rulesClickHandler implements ClickHandler {
        public void onClick(ClickEvent event) {
            openWindowRootRelative("rules.html");
        }
    }

    public void runSizeSaver() {
        new Timer() {

            @Override
            public void run() {
                cookieSettingsManager.setDoubleCookie(CookieSettingsManager.PLAYER_PANEL_COOKIE,
                        splitLayoutPanel_data.getWidgetSize(dataGrid_players));
                cookieSettingsManager.setDoubleCookie(CookieSettingsManager.SERVER_PANEL_COOKIE,
                        splitLayoutPanel_data.getWidgetSize(dataGrid_servers));
                cookieSettingsManager.setDoubleCookie(CookieSettingsManager.CHAT_PANEL_COOKIE,
                        splitLayoutPanel_main.getWidgetSize(dockLayoutPanel_chat));
                cookieSettingsManager.setDoubleCookie(CookieSettingsManager.HEADER_PANEL_COOKIE,
                        splitLayoutPanel_main.getWidgetSize(horizontalPanel_header));
                cookieSettingsManager.setDoubleCookie(CookieSettingsManager.CHAT_SYSTEM_PANEL_COOKIE,
                        splitLayoutPanel_chat.getWidgetSize(scrollPanel_systemChat));
            }
        }.scheduleRepeating(ClientSettings.SIZE_SAVE_INTERVAL);
    }

    private void initPlayer(PlayerDTO result) {
        myPlayer = result;
        updateNickURL();
        voteResultPanel.setId(myPlayer.getId());
        // send initial ping to show our presence
        loadInitState();
        postRulesAnnounce();
        runSizeSaver();
    }

    private void updateNickURL() {
        SafeHtmlBuilder sb = new SafeHtmlBuilder();
        myPlayer.buildLink(sb, false);
        label_nick.setHTML(sb.toSafeHtml().asString());
    }

    public static boolean isGatherClosed(GatherState gatherState) {
        return Arrays.asList(GatherState.COMPLETED, GatherState.SIDEPICK, GatherState.PLAYERS).contains(gatherState);
    }

    private class Button_setNickClickHandler implements ClickHandler {
        public void onClick(ClickEvent event) {
            final String newNick = Window.prompt("Укажите ник.", myPlayer.getNick() != null ? myPlayer.getNick() : myPlayer.getName());
            if (newNick != null) {
                ns2gService.changeNick(newNick, new MyAsyncCallback<Void>() {

                    @Override
                    public void onSuccess(Void result) {
                        myPlayer.setNick(newNick);
                        updateNickURL();
                    }
                });
            }
        }
    }
}
