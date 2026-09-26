package com.codenames.backend.adapter.in.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.codenames.backend.adapter.in.web.dto.ClueRequest;
import com.codenames.backend.adapter.in.web.dto.CreateRoomRequest;
import com.codenames.backend.adapter.in.web.dto.RoomActionRequest;
import com.codenames.backend.application.RoomApplicationService;
import com.codenames.backend.application.port.RoomEventPublisher;
import com.codenames.backend.model.Clue;
import com.codenames.backend.model.Room;

@RestController
@RequestMapping("/room")
public class RoomController {

    private final RoomApplicationService roomService;
    private final RoomEventPublisher roomEventPublisher;

    public RoomController(RoomApplicationService roomService, RoomEventPublisher roomEventPublisher) {
        this.roomService = roomService;
        this.roomEventPublisher = roomEventPublisher;
    }

    @PostMapping("/create")
    public ResponseEntity<Room> create(@RequestBody CreateRoomRequest request) {
        Room room = roomService.createRoom(request.username());
        return ResponseEntity.ok(room);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Room> get(@PathVariable("id") Long id) {
        Room room = roomService.getRoomById(id);
        if (room == null) {
            throw new IllegalArgumentException("Room not found");
        }
        return ResponseEntity.ok(room);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable("id") Long id) {
        roomService.deleteRoomById(id);
        return ResponseEntity.ok("Room deleted");
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> handleAction(
            @PathVariable("id") Long id,
            @RequestBody RoomActionRequest request) {

        if (request.action() == null || request.action().isBlank()) {
            return ResponseEntity.badRequest().body("Invalid action");
        }

        switch (request.action()) {
            case "join" -> roomService.joinRoom(id, request.username());
            case "leave" -> roomService.leaveRoom(id, request.username());
            case "start" -> roomService.startRoom(id);
            case "shuffle-players" -> roomService.shufflePlayers(id);
            case "reset-players" -> roomService.resetPlayers(id);
            case "change-host" -> roomService.changeHost(id, request.username());
            case "select-team" -> roomService.selectTeam(id, request.team(), request.username());
            case "select-role" -> roomService.selectRole(id, request.role(), request.team(), request.username());
            case "change-username" -> roomService.changeUsername(id, request.username(), request.newUsername());
            case "manual-team-turn" -> roomService.manualTeamTurn(id, request.username());
            case "select-word" -> roomService.selectWord(id, request.wordname(), request.username());
            case "click-word" -> roomService.clickWord(id, request.wordname(), request.username());
            case "add-clue" -> roomService.addClue(id, toClue(request.clue()), request.username());
            case "replay" -> roomService.replay(id, request.usernames() != null ? request.usernames() : java.util.List.of());
            default -> {
                return ResponseEntity.badRequest().body("Invalid action");
            }
        }

        Room updatedRoom = roomService.getRoomById(id);
        roomEventPublisher.publishRoomUpdate(updatedRoom);
        return ResponseEntity.ok(request.action() + " : action completed");
    }

    private Clue toClue(ClueRequest clueRequest) {
        if (clueRequest == null) {
            throw new IllegalArgumentException("Clue is required");
        }
        return new Clue(
                clueRequest.clueName(),
                clueRequest.attempts(),
                clueRequest.remaining(),
                clueRequest.spyName());
    }
}
