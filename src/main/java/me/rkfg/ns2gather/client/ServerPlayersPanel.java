package me.rkfg.ns2gather.client;

import java.util.Comparator;
import java.util.Date;

import me.rkfg.ns2gather.dto.ServerDTO;
import me.rkfg.ns2gather.dto.SteamPlayerDTO;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.cellview.client.ColumnSortEvent.ListHandler;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.view.client.ListDataProvider;

public class ServerPlayersPanel extends DialogBox {
    private final SimplePanel simplePanel = new SimplePanel();
    private final FlexTable flexTable = new FlexTable();
    private final ListDataProvider<SteamPlayerDTO> dataProvider_players = new ListDataProvider<SteamPlayerDTO>();
    private final DataGrid<SteamPlayerDTO> dataGrid_players = new DataGrid<SteamPlayerDTO>();
    private final Label label_caption = new Label("Игроки сервера");
    private final Button button_close = new Button("Закрыть");
    DateTimeFormat format = DateTimeFormat.getFormat("HH:mm:ss");
    long baseTime = format.parse("00:00:00").getTime();
    ListHandler<SteamPlayerDTO> sortHandler = new ListHandler<SteamPlayerDTO>(null);
    private final TextColumn<SteamPlayerDTO> textColumn_name = new TextColumn<SteamPlayerDTO>() {
        @Override
        public String getValue(SteamPlayerDTO object) {
            return object.getName();
        }
    };
    private final TextColumn<SteamPlayerDTO> textColumn_score = new TextColumn<SteamPlayerDTO>() {
        @Override
        public String getValue(SteamPlayerDTO object) {
            return String.valueOf(object.getScore());
        }
    };
    private final TextColumn<SteamPlayerDTO> textColumn_time = new TextColumn<SteamPlayerDTO>() {
        @Override
        public String getValue(SteamPlayerDTO object) {
            Date time = new Date((long) (object.getConnectTime() * 1000) + baseTime);
            return format.format(time);
        }
    };

    public ServerPlayersPanel() {
        setModal(false);
        setText("Данные о сервере");
        flexTable.setCellPadding(5);

        setWidget(simplePanel);
        simplePanel.setSize("400px", "300px");
        simplePanel.setWidget(flexTable);
        flexTable.setSize("100%", "100%");

        flexTable.setWidget(0, 0, label_caption);

        flexTable.setWidget(1, 0, dataGrid_players);
        flexTable.getCellFormatter().setHeight(1, 0, "100%");
        flexTable.getCellFormatter().setWidth(1, 0, "100%");
        dataGrid_players.setSize("100%", "100%");
        flexTable.getCellFormatter().setHorizontalAlignment(0, 0, HasHorizontalAlignment.ALIGN_CENTER);
        button_close.addClickHandler(new ButtonClickHandler());

        flexTable.setWidget(2, 0, button_close);
        flexTable.getCellFormatter().setHorizontalAlignment(2, 0, HasHorizontalAlignment.ALIGN_CENTER);
        dataProvider_players.addDataDisplay(dataGrid_players);
        textColumn_name.setSortable(true);

        dataGrid_players.addColumn(textColumn_name, "Имя");
        textColumn_score.setSortable(true);

        dataGrid_players.addColumn(textColumn_score, "Счёт");
        textColumn_time.setSortable(true);

        dataGrid_players.addColumn(textColumn_time, "Время");
        dataGrid_players.addColumnSortHandler(sortHandler);
        sortHandler.setComparator(textColumn_name, new Comparator<SteamPlayerDTO>() {

            @Override
            public int compare(SteamPlayerDTO o1, SteamPlayerDTO o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        sortHandler.setComparator(textColumn_score, new Comparator<SteamPlayerDTO>() {

            @Override
            public int compare(SteamPlayerDTO o1, SteamPlayerDTO o2) {
                return o1.getScore() - o2.getScore();
            }
        });
        sortHandler.setComparator(textColumn_time, new Comparator<SteamPlayerDTO>() {

            @Override
            public int compare(SteamPlayerDTO o1, SteamPlayerDTO o2) {
                return Math.round(o1.getConnectTime() - o2.getConnectTime());
            }
        });
    }

    public void init(ServerDTO server) {
        label_caption.setText("Игроки сервера " + server.getName());
        dataProvider_players.setList(server.getPlayers());
        sortHandler.setList(dataProvider_players.getList());
    }

    private class ButtonClickHandler implements ClickHandler {
        public void onClick(ClickEvent event) {
            hide();
        }
    }
}
