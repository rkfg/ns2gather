package me.rkfg.ns2gather.client;

import ru.ppsrk.gwt.client.ClientUtils.MyAsyncCallback;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window.Location;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.SimplePanel;

public class FakeLoginBox extends DialogBox {
    private final SimplePanel simplePanel = new SimplePanel();
    private final FlexTable flexTable = new FlexTable();
    private final HTML html_loginButton = new HTML("", false);

    public FakeLoginBox() {
        setText("Пожалуйста, войдите");

        setWidget(simplePanel);
        simplePanel.setSize("300px", "200px");

        simplePanel.setWidget(flexTable);
        flexTable.setSize("100%", "100%");
        html_loginButton.addClickHandler(new Html_loginButtonClickHandler());

        flexTable.setWidget(0, 0, html_loginButton);
        html_loginButton
                .setHTML("<img src=\"http://steamcommunity-a.akamaihd.net/public/images/signinthroughsteam/sits_large_border.png\"/>");
        flexTable.getCellFormatter().setHorizontalAlignment(0, 0, HasHorizontalAlignment.ALIGN_CENTER);
        flexTable.getCellFormatter().setVerticalAlignment(0, 0, HasVerticalAlignment.ALIGN_MIDDLE);
    }

    private class Html_loginButtonClickHandler implements ClickHandler {
        public void onClick(ClickEvent event) {
            NS2GServiceAsync.Util.getInstance().fakeLogin(new MyAsyncCallback<Void>() {

                @Override
                public void onSuccess(Void result) {
                    Location.reload();
                }
            });
        }
    }
}
