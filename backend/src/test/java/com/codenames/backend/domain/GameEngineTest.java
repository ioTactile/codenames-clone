package com.codenames.backend.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.codenames.backend.model.Clue;
import com.codenames.backend.model.Player;
import com.codenames.backend.model.PlayerRole;
import com.codenames.backend.model.PlayerTeam;
import com.codenames.backend.model.Room;
import com.codenames.backend.model.RoomStatus;
import com.codenames.backend.model.Word;
import com.codenames.backend.model.WordColor;
import com.codenames.backend.model.WordState;

class GameEngineTest {

    private GameEngine gameEngine;

    @BeforeEach
    void setUp() {
        gameEngine = new GameEngine(new Random(42));
    }

    @Test
    void dealBoard_creates25WordsWithCorrectColorCounts() {
        List<String> pool = IntStream.range(0, 50).mapToObj(i -> "WORD" + i).toList();
        List<Word> board = gameEngine.dealBoard(pool);

        assertEquals(25, board.size());
        long red = board.stream().filter(w -> w.getWordColor() == WordColor.RED).count();
        long blue = board.stream().filter(w -> w.getWordColor() == WordColor.BLUE).count();
        long black = board.stream().filter(w -> w.getWordColor() == WordColor.BLACK).count();
        long white = board.stream().filter(w -> w.getWordColor() == WordColor.WHITE).count();

        assertTrue(red == 8 || red == 9);
        assertTrue(blue == 8 || blue == 9);
        assertEquals(17, red + blue);
        assertEquals(1, black);
        assertEquals(7, white);
    }

    @Test
    void dealBoard_throwsWhenNotEnoughWords() {
        assertThrows(IllegalStateException.class, () -> gameEngine.dealBoard(List.of("A", "B")));
    }

    @Test
    void setTeamAndRoleTurn_givesTurnToTeamWithMoreCards() {
        Room room = gameEngine.initializeRoom();
        List<Word> words = List.of(
                word("A", WordColor.RED), word("B", WordColor.RED), word("C", WordColor.BLUE));
        gameEngine.setTeamAndRoleTurn(room, words);

        assertEquals(PlayerTeam.RED.toString(), room.getTeamTurn());
        assertEquals(PlayerRole.SPYMASTER.toString(), room.getRoleTurn());
        assertEquals(2, room.getRedRemainingWords());
        assertEquals(1, room.getBlueRemainingWords());
    }

    @Test
    void clickWord_redCard_decrementsAndCanWin() {
        Room room = inProgressRoom(PlayerTeam.RED);
        room.setRedRemainingWords(1);
        room.setBlueRemainingWords(8);
        room.getWords().add(word("TARGET", WordColor.RED));
        room.getClues().add(new Clue("HINT", 1, 2, "spy"));

        gameEngine.clickWord(room, "TARGET", "operative");

        assertEquals(WordState.CLICKED, room.getWords().get(0).getWordState());
        assertEquals(0, room.getRedRemainingWords());
        assertEquals(RoomStatus.RED_TEAM_WINS, room.getStatus());
    }

    @Test
    void clickWord_blackCard_opposingTeamWins() {
        Room room = inProgressRoom(PlayerTeam.RED);
        room.getWords().add(word("ASSASSIN", WordColor.BLACK));
        room.getClues().add(new Clue("HINT", 1, 2, "spy"));

        gameEngine.clickWord(room, "ASSASSIN", "operative");

        assertTrue(room.getIsBlackCardSelected());
        assertEquals(RoomStatus.BLUE_TEAM_WINS, room.getStatus());
    }

    @Test
    void clickWord_whiteCard_switchesTeamTurn() {
        Room room = inProgressRoom(PlayerTeam.RED);
        room.getWords().add(word("NEUTRAL", WordColor.WHITE));
        room.getClues().add(new Clue("HINT", 1, 3, "spy"));

        gameEngine.clickWord(room, "NEUTRAL", "operative");

        assertEquals(PlayerTeam.BLUE.toString(), room.getTeamTurn());
        assertEquals(PlayerRole.SPYMASTER.toString(), room.getRoleTurn());
        assertEquals(RoomStatus.IN_PROGRESS, room.getStatus());
    }

    @Test
    void clickWord_withoutClue_isNoOp() {
        Room room = inProgressRoom(PlayerTeam.RED);
        room.getWords().add(word("TARGET", WordColor.RED));

        gameEngine.clickWord(room, "TARGET", "operative");

        assertEquals(WordState.NOT_SELECTED, room.getWords().get(0).getWordState());
        assertEquals(9, room.getRedRemainingWords());
    }

    @Test
    void clickWord_wrongTeam_throws() {
        Room room = inProgressRoom(PlayerTeam.BLUE);
        room.getPlayers().clear();
        room.getPlayers().add(new Player("operative", PlayerTeam.RED, PlayerRole.OPERATIVE));
        room.getWords().add(word("TARGET", WordColor.RED));
        room.getClues().add(new Clue("HINT", 1, 2, "spy"));

        assertThrows(IllegalArgumentException.class,
                () -> gameEngine.clickWord(room, "TARGET", "operative"));
    }

    @Test
    void selectWord_togglesSelection() {
        Room room = inProgressRoom(PlayerTeam.RED);
        room.getWords().add(word("TARGET", WordColor.RED));

        gameEngine.selectWord(room, "TARGET", "operative");
        assertEquals(WordState.SELECTED, room.getWords().get(0).getWordState());
        assertTrue(room.getWords().get(0).getSelectedBy().contains("operative"));

        gameEngine.selectWord(room, "TARGET", "operative");
        assertEquals(WordState.NOT_SELECTED, room.getWords().get(0).getWordState());
        assertTrue(room.getWords().get(0).getSelectedBy().isEmpty());
    }

    @Test
    void addClue_switchesRoleToOperative() {
        Room room = inProgressRoom(PlayerTeam.RED);
        room.setRoleTurn(PlayerRole.SPYMASTER.toString());
        room.getPlayers().add(new Player("spy", PlayerTeam.RED, PlayerRole.SPYMASTER));
        room.getWords().add(word("BOARD", WordColor.RED));

        gameEngine.addClue(room, new Clue("OCEAN", 2, 3, "spy"), "spy");

        assertEquals(1, room.getClues().size());
        assertEquals(PlayerRole.OPERATIVE.toString(), room.getRoleTurn());
    }

    @Test
    void addClue_rejectsBoardWord() {
        Room room = inProgressRoom(PlayerTeam.RED);
        room.setRoleTurn(PlayerRole.SPYMASTER.toString());
        room.getPlayers().add(new Player("spy", PlayerTeam.RED, PlayerRole.SPYMASTER));
        room.getWords().add(word("OCEAN", WordColor.RED));

        assertThrows(IllegalArgumentException.class,
                () -> gameEngine.addClue(room, new Clue("OCEAN", 2, 3, "spy"), "spy"));
    }

    @Test
    void manualTeamTurn_switchesTurn() {
        Room room = inProgressRoom(PlayerTeam.RED);
        gameEngine.manualTeamTurn(room, "operative");
        assertEquals(PlayerTeam.BLUE.toString(), room.getTeamTurn());
        assertEquals(PlayerRole.SPYMASTER.toString(), room.getRoleTurn());
    }

    @Test
    void switchTeamTurn_doesNotPersistSeparately_mutatesSameInstance() {
        Room room = inProgressRoom(PlayerTeam.RED);
        gameEngine.switchTeamTurn(room);
        assertEquals(PlayerTeam.BLUE.toString(), room.getTeamTurn());
    }

    private Room inProgressRoom(PlayerTeam teamTurn) {
        Room room = gameEngine.initializeRoom();
        room.setStatus(RoomStatus.IN_PROGRESS);
        room.setTeamTurn(teamTurn.toString());
        room.setRoleTurn(PlayerRole.OPERATIVE.toString());
        room.setRedRemainingWords(9);
        room.setBlueRemainingWords(8);
        room.setWords(new ArrayList<>());
        room.setClues(new ArrayList<>());
        room.getPlayers().add(new Player("operative", teamTurn, PlayerRole.OPERATIVE));
        room.setCreatedAt(LocalDateTime.now());
        room.setUpdatedAt(LocalDateTime.now());
        return room;
    }

    private Word word(String name, WordColor color) {
        return new Word(name, new ArrayList<>(), WordState.NOT_SELECTED, color);
    }
}
