package me.rkfg.ns2gather.server;

import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;

import ru.ppsrk.gwt.client.ClientAuthException;
import ru.ppsrk.gwt.client.LogicException;
import me.rkfg.ns2gather.dto.MessageDTO;
import me.rkfg.ns2gather.dto.MessageType;
import me.rkfg.ns2gather.dto.MessageVisibility;

public class MessageManager implements AutoCloseable {
    ConcurrentLinkedQueue<MessageDTO> messages = new ConcurrentLinkedQueue<>();
    Timer messageCleanupTimer = new Timer("Message cleanup", true);

    public MessageManager() {
        runMessageCleanup();
    }

    public List<MessageDTO> getNewMessages(Long gatherId, Long steamId, Long since) {
        List<MessageDTO> result = new LinkedList<>();
        for (MessageDTO messageDTO : messages) {
            if (messageDTO.getTimestamp() > since) {
                if (messageDTO.getVisibility() == MessageVisibility.BROADCAST
                        && (messageDTO.getGatherId() == gatherId || messageDTO.getGatherId() == null)
                        || messageDTO.getVisibility() == MessageVisibility.PERSONAL && messageDTO.getToSteamId().equals(steamId)) {
                    result.add(messageDTO);
                }
            }
        }
        return result;
    }

    private void runMessageCleanup() {
        messageCleanupTimer.schedule(new TimerTask() {

            @Override
            public void run() {
                for (MessageDTO messageDTO : messages) {
                    if (System.currentTimeMillis() - messageDTO.getTimestamp() > Settings.MESSAGE_CLEANUP_INTERVAL) {
                        messages.remove(messageDTO);
                    }
                }
            }
        }, 10000, 10000);
    }

    @Override
    public void close() throws Exception {
        messageCleanupTimer.cancel();
    }

    public void postMessage(MessageDTO messageDTO) {
        messages.offer(messageDTO);
    }

    public void postMessage(MessageType type, String content, Long gatherId) throws LogicException, ClientAuthException {
        postMessage(new MessageDTO(type, content, gatherId));
    }

}
