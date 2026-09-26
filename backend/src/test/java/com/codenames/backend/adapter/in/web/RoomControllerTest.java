package com.codenames.backend.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.codenames.backend.application.RoomApplicationService;
import com.codenames.backend.application.port.RoomEventPublisher;
import com.codenames.backend.model.Player;
import com.codenames.backend.model.PlayerRole;
import com.codenames.backend.model.PlayerTeam;
import com.codenames.backend.model.Room;
import com.codenames.backend.model.RoomStatus;

@WebMvcTest(controllers = RoomController.class)
class RoomControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RoomApplicationService roomService;

    @MockitoBean
    private RoomEventPublisher roomEventPublisher;

    @Test
    void create_returnsRoom() throws Exception {
        Room room = sampleRoom(1L);
        when(roomService.createRoom("jordan")).thenReturn(room);

        mockMvc.perform(post("/room/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"jordan\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void get_returns404StyleBadRequestWhenMissing() throws Exception {
        when(roomService.getRoomById(42L)).thenReturn(null);

        mockMvc.perform(get("/room/42"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Room not found"));
    }

    @Test
    void handleAction_join_publishesUpdate() throws Exception {
        Room room = sampleRoom(7L);
        when(roomService.getRoomById(7L)).thenReturn(room);

        mockMvc.perform(put("/room/7")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"action\":\"join\",\"username\":\"alice\"}"))
                .andExpect(status().isOk());

        verify(roomService).joinRoom(7L, "alice");
        verify(roomEventPublisher).publishRoomUpdate(room);
    }

    @Test
    void handleAction_invalidAction_returnsBadRequest() throws Exception {
        mockMvc.perform(put("/room/7")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"action\":\"dance\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void handleAction_addClue_mapsDto() throws Exception {
        Room room = sampleRoom(3L);
        when(roomService.getRoomById(3L)).thenReturn(room);

        mockMvc.perform(put("/room/3")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "action":"add-clue",
                          "username":"spy",
                          "clue":{"clueName":"OCEAN","attempts":2,"remaining":3,"spyName":"spy"}
                        }
                        """))
                .andExpect(status().isOk());

        verify(roomService).addClue(eq(3L), any(), eq("spy"));
        verify(roomEventPublisher).publishRoomUpdate(room);
    }

    private Room sampleRoom(Long id) {
        Room room = new Room();
        room.setId(id);
        room.setStatus(RoomStatus.PENDING);
        room.setPlayers(new ArrayList<>());
        room.getPlayers().add(new Player("host", PlayerTeam.NONE, PlayerRole.NONE));
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
