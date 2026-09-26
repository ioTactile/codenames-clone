package com.codenames.backend.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.codenames.backend.application.port.WordListProvider;
import com.codenames.backend.domain.GameEngine;
import com.codenames.backend.model.Player;
import com.codenames.backend.model.PlayerRole;
import com.codenames.backend.model.PlayerTeam;
import com.codenames.backend.model.Room;
import com.codenames.backend.model.RoomStatus;
import com.codenames.backend.repository.RoomRepository;

@ExtendWith(MockitoExtension.class)
class RoomApplicationServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private WordListProvider wordListProvider;

    private RoomApplicationService roomService;

    @BeforeEach
    void setUp() {
        roomService = new RoomApplicationService(roomRepository, wordListProvider, new GameEngine(new Random(1)));
    }

    @Test
    void createRoom_savesPendingRoomWithHostAndBoard() {
        when(wordListProvider.frenchWords())
                .thenReturn(IntStream.range(0, 40).mapToObj(i -> "W" + i).toList());
        when(roomRepository.save(any(Room.class))).thenAnswer(invocation -> {
            Room room = invocation.getArgument(0);
            room.setId(1L);
            return room;
        });

        Room created = roomService.createRoom("host");

        assertEquals(1L, created.getId());
        assertEquals(RoomStatus.PENDING, created.getStatus());
        assertEquals(1, created.getPlayers().size());
        assertEquals("host", created.getPlayers().get(0).getName());
        assertEquals(25, created.getWords().size());
        verify(roomRepository).save(any(Room.class));
    }

    @Test
    void joinRoom_addsPlayerWhenPending() {
        Room room = pendingRoom(10L);
        when(roomRepository.findById(10L)).thenReturn(Optional.of(room));
        when(roomRepository.save(any(Room.class))).thenAnswer(inv -> inv.getArgument(0));

        roomService.joinRoom(10L, "alice");

        assertEquals(2, room.getPlayers().size());
        assertEquals("alice", room.getPlayers().get(1).getName());
    }

    @Test
    void joinRoom_rejectsDuplicatePseudo() {
        Room room = pendingRoom(10L);
        when(roomRepository.findById(10L)).thenReturn(Optional.of(room));

        assertThrows(IllegalArgumentException.class, () -> roomService.joinRoom(10L, "host"));
    }

    @Test
    void startRoom_movesToInProgress() {
        Room room = pendingRoom(5L);
        when(roomRepository.findById(5L)).thenReturn(Optional.of(room));
        when(roomRepository.save(any(Room.class))).thenAnswer(inv -> inv.getArgument(0));

        roomService.startRoom(5L);

        ArgumentCaptor<Room> captor = ArgumentCaptor.forClass(Room.class);
        verify(roomRepository).save(captor.capture());
        assertEquals(RoomStatus.IN_PROGRESS, captor.getValue().getStatus());
    }

    @Test
    void deleteRoomById_throwsWhenMissing() {
        when(roomRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(IllegalStateException.class, () -> roomService.deleteRoomById(99L));
    }

    private Room pendingRoom(Long id) {
        Room room = new Room();
        room.setId(id);
        room.setStatus(RoomStatus.PENDING);
        room.setPlayers(new ArrayList<>(List.of(new Player("host", PlayerTeam.NONE, PlayerRole.NONE))));
        room.setWords(new ArrayList<>());
        room.setClues(new ArrayList<>());
        room.setTeamTurn(PlayerTeam.RED.toString());
        room.setRoleTurn(PlayerRole.SPYMASTER.toString());
        room.setRedRemainingWords(9);
        room.setBlueRemainingWords(8);
        room.setIsBlackCardSelected(false);
        return room;
    }
}
