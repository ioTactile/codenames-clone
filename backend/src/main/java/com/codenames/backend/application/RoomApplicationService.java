package com.codenames.backend.application;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.codenames.backend.application.port.WordListProvider;
import com.codenames.backend.domain.GameEngine;
import com.codenames.backend.model.Clue;
import com.codenames.backend.model.Player;
import com.codenames.backend.model.PlayerRole;
import com.codenames.backend.model.PlayerTeam;
import com.codenames.backend.model.Room;
import com.codenames.backend.model.RoomStatus;
import com.codenames.backend.model.Word;
import com.codenames.backend.repository.RoomRepository;

@Service
@Transactional
public class RoomApplicationService {

    private final RoomRepository roomRepository;
    private final WordListProvider wordListProvider;
    private final GameEngine gameEngine;

    public RoomApplicationService(
            RoomRepository roomRepository,
            WordListProvider wordListProvider,
            GameEngine gameEngine) {
        this.roomRepository = roomRepository;
        this.wordListProvider = wordListProvider;
        this.gameEngine = gameEngine;
    }

    public Room createRoom(String pseudo) {
        Room room = gameEngine.initializeRoom();
        room.getPlayers().add(gameEngine.createPlayer(pseudo));
        List<Word> words = gameEngine.dealBoard(wordListProvider.frenchWords());
        room.setWords(words);
        gameEngine.setTeamAndRoleTurn(room, words);
        return roomRepository.save(room);
    }

    public void replay(Long roomId, List<String> pseudos) {
        Room room = requireRoom(roomId);
        room.setPlayers(new ArrayList<>());
        for (String pseudo : pseudos) {
            room.getPlayers().add(gameEngine.createPlayer(pseudo));
        }
        List<Word> words = gameEngine.dealBoard(wordListProvider.frenchWords());
        room.setWords(words);
        gameEngine.setTeamAndRoleTurn(room, words);
        room.setClues(new ArrayList<>());
        room.setIsBlackCardSelected(false);
        room.setStatus(RoomStatus.PENDING);
        room.setUpdatedAt(LocalDateTime.now());
        roomRepository.save(room);
    }

    @Transactional(readOnly = true)
    public Room getRoomById(Long roomId) {
        return roomRepository.findById(roomId).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<Room> getAllRooms() {
        return roomRepository.findAll();
    }

    public void deleteRoomById(Long roomId) {
        Optional<Room> roomOptional = roomRepository.findById(roomId);
        if (roomOptional.isEmpty()) {
            throw new IllegalStateException("Room with id " + roomId + " does not exist");
        }
        roomRepository.deleteById(roomId);
    }

    public void joinRoom(Long roomId, String pseudo) {
        Room room = requireRoom(roomId);
        gameEngine.requireRoomPending(room);
        if (room.getPlayers().stream().anyMatch(player -> player.getName().equals(pseudo))) {
            throw new IllegalArgumentException("Pseudo already used");
        }
        room.getPlayers().add(new Player(pseudo, PlayerTeam.NONE, PlayerRole.NONE));
        roomRepository.save(room);
    }

    public void joinRoomAsSpectator(Long roomId) {
        Room room = requireRoom(roomId);
        if (room.getStatus() != RoomStatus.IN_PROGRESS) {
            throw new IllegalArgumentException("Room not found or not in progress");
        }
        String pseudo = "Spectateur " + (int) (room.getPlayers().stream()
                .filter(player -> player.getPlayerRole() == PlayerRole.SPECTATOR).count() + 1);
        room.getPlayers().add(new Player(pseudo, PlayerTeam.NONE, PlayerRole.SPECTATOR));
        roomRepository.save(room);
    }

    public void leaveRoom(Long roomId, String pseudo) {
        Room room = requireRoom(roomId);
        gameEngine.requireRoomPending(room);
        room.getPlayers().removeIf(player -> player.getName().equals(pseudo));
        roomRepository.save(room);
    }

    public void startRoom(Long roomId) {
        Room room = requireRoom(roomId);
        gameEngine.requireRoomPending(room);
        room.setStatus(RoomStatus.IN_PROGRESS);
        room.setUpdatedAt(LocalDateTime.now());
        roomRepository.save(room);
    }

    public void selectTeam(Long roomId, String team, String pseudo) {
        Room room = requireRoom(roomId);
        gameEngine.requireRoomPending(room);
        Player player = gameEngine.requirePlayer(room, pseudo);
        if (gameEngine.mapStringToPlayerTeam(team) == PlayerTeam.NONE) {
            player.setPlayerRole(PlayerRole.NONE);
        } else {
            player.setPlayerRole(PlayerRole.OPERATIVE);
        }
        player.setPlayerTeam(gameEngine.mapStringToPlayerTeam(team));
        roomRepository.save(room);
    }

    public void selectRole(Long roomId, String role, String team, String pseudo) {
        Room room = requireRoom(roomId);
        gameEngine.requireRoomPending(room);
        Player player = gameEngine.requirePlayer(room, pseudo);
        if (PlayerRole.SPYMASTER.toString().equals(role)) {
            if (room.getPlayers().stream()
                    .anyMatch(p -> p.getPlayerRole() == PlayerRole.SPYMASTER
                            && p.getPlayerTeam().toString().equals(team))) {
                throw new IllegalArgumentException("There is already a spymaster in the right team");
            }
        }
        player.setPlayerRole(gameEngine.mapStringToPlayerRole(role));
        player.setPlayerTeam(gameEngine.mapStringToPlayerTeam(team));
        roomRepository.save(room);
    }

    public void changeUsername(Long roomId, String pseudo, String newPseudo) {
        Room room = requireRoom(roomId);
        Player player = gameEngine.requirePlayer(room, pseudo);
        if (room.getPlayers().stream().anyMatch(p -> p.getName().equals(newPseudo))) {
            throw new IllegalArgumentException("Pseudo already used");
        }
        player.setName(newPseudo);
        roomRepository.save(room);
    }

    public void shufflePlayers(Long roomId) {
        Room room = requireRoom(roomId);
        gameEngine.requireRoomPending(room);
        List<Player> players = room.getPlayers();
        Collections.shuffle(players);
        List<Player> redPlayers = new ArrayList<>();
        List<Player> bluePlayers = new ArrayList<>();
        for (int i = 0; i < players.size(); i++) {
            if (i % 2 == 0) {
                redPlayers.add(players.get(i));
            } else {
                bluePlayers.add(players.get(i));
            }
        }
        for (Player player : redPlayers) {
            player.setPlayerTeam(PlayerTeam.RED);
            player.setPlayerRole(PlayerRole.OPERATIVE);
        }
        for (Player player : bluePlayers) {
            player.setPlayerTeam(PlayerTeam.BLUE);
            player.setPlayerRole(PlayerRole.OPERATIVE);
        }
        room.getPlayers().clear();
        room.getPlayers().addAll(bluePlayers);
        room.getPlayers().addAll(redPlayers);
        roomRepository.save(room);
    }

    public void resetPlayers(Long roomId) {
        Room room = requireRoom(roomId);
        gameEngine.requireRoomPending(room);
        for (Player player : room.getPlayers()) {
            player.setPlayerTeam(PlayerTeam.NONE);
            player.setPlayerRole(PlayerRole.NONE);
        }
        roomRepository.save(room);
    }

    public void changeHost(Long roomId, String pseudo) {
        Room room = requireRoom(roomId);
        Player player = gameEngine.requirePlayer(room, pseudo);
        List<Player> players = room.getPlayers();
        players.remove(player);
        players.add(0, player);
        room.setPlayers(players);
        roomRepository.save(room);
    }

    public void manualTeamTurn(Long roomId, String pseudo) {
        Room room = requireRoom(roomId);
        gameEngine.manualTeamTurn(room, pseudo);
        roomRepository.save(room);
    }

    public void selectWord(Long roomId, String wordName, String pseudo) {
        Room room = requireRoom(roomId);
        gameEngine.selectWord(room, wordName, pseudo);
        roomRepository.save(room);
    }

    public void clickWord(Long roomId, String wordName, String pseudo) {
        Room room = requireRoom(roomId);
        gameEngine.clickWord(room, wordName, pseudo);
        roomRepository.save(room);
    }

    public void addClue(Long roomId, Clue clue, String pseudo) {
        Room room = requireRoom(roomId);
        gameEngine.addClue(room, clue, pseudo);
        roomRepository.save(room);
    }

    private Room requireRoom(Long roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));
    }
}
