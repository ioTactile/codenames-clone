package com.codenames.backend.application.port;

import com.codenames.backend.model.Room;

public interface RoomEventPublisher {
    void publishRoomUpdate(Room room);
}
