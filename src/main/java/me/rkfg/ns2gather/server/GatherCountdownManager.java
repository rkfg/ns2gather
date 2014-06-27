package me.rkfg.ns2gather.server;

import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

public class GatherCountdownManager {
    public class GatherCountdown {
        private Timer timer = new Timer("Countdown", true);

        public void schedule(final TimerTask task, long delay, final Long gatherId) {
            timer.schedule(new TimerTask() {

                @Override
                public void run() {
                    // remove myself
                    countdowns.remove(gatherId);
                    task.run();
                }
            }, delay);
        }

        public void cancel() {
            timer.cancel();
        }

    }

    HashMap<Long, GatherCountdown> countdowns = new HashMap<>();

    public void scheduleGatherCountdownTask(Long gatherId, TimerTask task, long delay) {
        GatherCountdown countdown = countdowns.get(gatherId);
        if (countdown != null) {
            countdown.cancel();
            countdowns.remove(gatherId);
        }
        countdown = new GatherCountdown();
        countdown.schedule(task, delay, gatherId);
        countdowns.put(gatherId, countdown);
    }

    public void cancelGatherCountdownTasks(Long gatherId) {
        GatherCountdown countdown = countdowns.get(gatherId);
        if (countdown != null) {
            countdown.cancel();
        }
    }
}
