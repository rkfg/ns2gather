package me.rkfg.ns2gather.client;

import me.rkfg.ns2gather.dto.Side;
import ru.ppsrk.gwt.client.ResultPopupPanel;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;

public class SidePick extends ResultPopupPanel<Side> {
    private final FlexTable flexTable = new FlexTable();
    private final Button button_marines = new Button("Marines");
    private final Button button_aliens = new Button("Aliens");
    private Side result = null;

    public SidePick() {
        setHTML("Выберите сторону");
        flexTable.setCellPadding(5);

        setWidget(flexTable);
        flexTable.setSize("100%", "100%");
        button_marines.addClickHandler(new Button_marinesClickHandler());

        flexTable.setWidget(0, 0, button_marines);
        button_aliens.addClickHandler(new Button_aliensClickHandler());

        flexTable.setWidget(0, 1, button_aliens);
        flexTable.getCellFormatter().setHorizontalAlignment(0, 0, HasHorizontalAlignment.ALIGN_CENTER);
        flexTable.getCellFormatter().setHorizontalAlignment(0, 1, HasHorizontalAlignment.ALIGN_CENTER);
    }

    @Override
    public void center() {
        setGlassEnabled(false);
        super.center();
    }

    @Override
    protected Side getResult() {
        return result;
    }

    @Override
    public FocusWidget getFocusWidget() {
        return null;
    }

    private class Button_marinesClickHandler implements ClickHandler {
        public void onClick(ClickEvent event) {
            result = Side.MARINES;
            hide();
        }
    }

    private class Button_aliensClickHandler implements ClickHandler {
        public void onClick(ClickEvent event) {
            result = Side.ALIENS;
            hide();
        }
    }
}
