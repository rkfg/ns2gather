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
                if (!voted) {
                    vote();
                } else {
                    unvote();
                }
            }
        });
    }

    public void switchState() {
        voted = !voted;
        if (voted) {
            setText(UNVOTE);
        } else {
            setText(VOTE);
        }
    }

    public void setState(boolean isVoted) {
        voted = isVoted;
    }

    protected abstract void vote();

    protected abstract void unvote();
}
