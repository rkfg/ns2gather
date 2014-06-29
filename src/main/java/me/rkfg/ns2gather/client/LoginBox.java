package me.rkfg.ns2gather.client;

import ru.ppsrk.gwt.client.ClientUtils.MyAsyncCallback;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window.Location;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.SimplePanel;

public class LoginBox extends DialogBox {
    private final SimplePanel simplePanel = new SimplePanel();
    private final FlexTable flexTable = new FlexTable();
    private final HTML html_loginButton = new HTML("", false);
    private final HTML htmlNewHtml = new HTML(
            "Пожалуйста, ознакомьтесь с <a href=\"rules.html\" target=\"_blank\">правилами</a>, прежде чем войти.", true);
    private final Button button_anonymous = new Button("Войти анонимно");

    public LoginBox(String url) {
        setText("Пожалуйста, войдите");

        setWidget(simplePanel);
        simplePanel.setSize("300px", "200px");

        simplePanel.setWidget(flexTable);
        flexTable.setSize("100%", "100%");

        flexTable.setWidget(0, 0, html_loginButton);
        html_loginButton.setHTML("<a href=\"" + url
                + "\"><img src=\"http://steamcommunity-a.akamaihd.net/public/images/signinthroughsteam/sits_large_border.png\"/></a>");
        flexTable.getCellFormatter().setHorizontalAlignment(0, 0, HasHorizontalAlignment.ALIGN_CENTER);
        flexTable.getCellFormatter().setVerticalAlignment(0, 0, HasVerticalAlignment.ALIGN_MIDDLE);

        flexTable.setWidget(1, 0, htmlNewHtml);
        button_anonymous.addClickHandler(new Button_anonymousClickHandler());

        flexTable.setWidget(2, 0, button_anonymous);
        flexTable.getCellFormatter().setHorizontalAlignment(2, 0, HasHorizontalAlignment.ALIGN_CENTER);
    }

    private class Button_anonymousClickHandler implements ClickHandler {
        public void onClick(ClickEvent event) {
            NS2GServiceAsync.Util.getInstance().loginAnonymously(new MyAsyncCallback<Void>() {

                @Override
                public void onSuccess(Void result) {
                    Location.reload();
                }

            });
        }
    }
}
