package com.opencircle.chat;

public interface ChatMessageBroadcaster {

    void broadcast(ChatMessageResponse message);
}
