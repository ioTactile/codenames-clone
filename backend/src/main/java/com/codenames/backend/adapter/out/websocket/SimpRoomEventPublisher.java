package com.codenames.backend.adapter.out.websocket;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import com.codenames.backend.application.port.RoomEventPublisher;
import com.codenames.backend.model.Room;

@Component
public class SimpRoomEventPublisher implements RoomEventPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public SimpRoomEventPublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void publishRoomUpdate(Room room) {
        if (room == null || room.getId() == null) {
            return;
        }
        messagingTemplate.convertAndSend("/topic/room/" + room.getId(), room);
    }
}
