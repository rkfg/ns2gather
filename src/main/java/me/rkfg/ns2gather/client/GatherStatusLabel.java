package me.rkfg.ns2gather.client;

import me.rkfg.ns2gather.dto.GatherState;

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.HTML;

public class GatherStatusLabel extends HTML {
    GatherState gatherState = GatherState.OPEN;
    Long timeLeft;
    Timer timer = new Timer() {

        @Override
        public void run() {
            setStyleName("gstatus gopen");
            setText("СБОР [ДО КОНЦА ГОЛОСОВАНИЯ: " + timeLeft-- + " с]");
            if (timeLeft == 0) {
                cancel();
                updateState();
            }
        }
    };

    public GatherStatusLabel(ClickHandler clickHandler) {
        updateState();
        if (clickHandler != null) {
            addClickHandler(clickHandler);
        }
    }

    private void updateState() {
        if (timer.isRunning()) {
            return;
        }
        switch (gatherState) {
        case OPEN:
            setStyleName("gstatus gopen");
            setText("СБОР");
            break;
        case CLOSED:
            setStyleName("gstatus gclosed");
            setText("СОБРАН");
            break;
        case COMPLETED:
            setStyleName("gstatus gcompleted");
            setHTML("ПРОВЕДЁН");
            break;
        default:
            break;
        }
    }

    public void setGatherState(GatherState gatherState) {
        this.gatherState = gatherState;
        updateState();
    }

    public GatherState getGatherState() {
        return gatherState;
    }

    public void runTimer(Long startTime) {
        this.timeLeft = startTime;
        timer.scheduleRepeating(1000);
    }

    public void stopTimer() {
        timer.cancel();
        updateState();
    }
}
