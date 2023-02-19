package com.github.pablowinck;

import io.quarkus.logging.Log;

import javax.enterprise.context.ApplicationScoped;
import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint("/chat/{username}")
@ApplicationScoped
public class ChatSocket {

    Map<String, Session> sessions = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session, @PathParam("username") String username) {
        if (username == null || username.isBlank() || sessions.containsKey(username)) {
            try {
                session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "Invalid username"));
            } catch (Exception e) {
                Log.error("Unable to close session: " + e);
            }
            return;
        }
        Log.info("Opening session for user: " + username);
        // username@front-key ... verify if exists front-key and if it is the same remove it
        sessions.keySet().stream().filter(k -> k.endsWith("@" + username.split("@")[1])).forEach(k -> sessions.remove(k));
        sessions.put(username, session);
    }

    @OnClose
    public void onClose(Session session, @PathParam("username") String username) {
        sessions.remove(username);
    }

    @OnError
    public void onError(Session session, @PathParam("username") String username, Throwable throwable) {
        sessions.remove(username);
        broadcast(new Message(username, "left on error: " + throwable));
    }

    @OnMessage
    public void onMessage(String message, @PathParam("username") String username) {
        if (message.equalsIgnoreCase("_ready_")) {
            broadcast(new Message(username, "joined"));
        } else {
            broadcast(new Message(username.split("@")[0], message));
        }
    }

    private void broadcast(Message message) {
        Log.info("Broadcasting message: " + message);
        sessions.values().forEach(s -> {
            s.getAsyncRemote().sendObject(message.toString(), result -> {
                if (result.getException() != null) {
                    System.out.println("Unable to send message: " + result.getException());
                }
            });
        });
    }
}
