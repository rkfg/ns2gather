package me.rkfg.ns2gather.client;

import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;

public class LoginBox extends DialogBox {
    private final SimplePanel simplePanel = new SimplePanel();
    private final FlexTable flexTable = new FlexTable();
    private final HTML html_loginButton = new HTML("", false);

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
    }
}
