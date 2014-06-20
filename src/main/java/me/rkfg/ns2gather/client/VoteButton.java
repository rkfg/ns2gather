package me.rkfg.ns2gather.client;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;

public abstract class VoteButton extends Button {
    private static final String VOTE = "Голосовать";
    private static final String UNVOTE = "Отменить голос";
    boolean voted = false;

    public VoteButton() {
        setText(VOTE);
        addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                voted = !voted;
                if (voted) {
                    vote();
                    setText(UNVOTE);
                } else {
                    unvote();
                    setText(VOTE);
                }
            }
        });
    }

    protected abstract void vote();

    protected abstract void unvote();
}
