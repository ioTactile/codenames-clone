package com.codenames.backend.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.codenames.backend.model.Clue;
import com.codenames.backend.model.Player;
import com.codenames.backend.model.PlayerRole;
import com.codenames.backend.model.PlayerTeam;
import com.codenames.backend.model.Room;
import com.codenames.backend.model.RoomStatus;
import com.codenames.backend.model.Word;
import com.codenames.backend.model.WordColor;
import com.codenames.backend.model.WordState;

/**
 * Pure game rules for Codenames. No Spring, JPA, or I/O.
 * Mutates the given {@link Room} in memory; persistence is the caller's job.
 */
public final class GameEngine {

    private final Random random;

    public GameEngine() {
        this(new Random());
    }

    public GameEngine(Random random) {
        this.random = random;
    }

    public void requireRoomInProgress(Room room) {
        if (room == null || room.getStatus() != RoomStatus.IN_PROGRESS) {
            throw new IllegalArgumentException("Room not found or not in progress");
        }
    }

    public void requireRoomPending(Room room) {
        if (room == null || room.getStatus() != RoomStatus.PENDING) {
            throw new IllegalArgumentException("Room not found or not pending");
        }
    }

    public Player requirePlayer(Room room, String pseudo) {
        return room.getPlayers().stream()
                .filter(p -> p.getName().equals(pseudo))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Player not found"));
    }

    public void switchTeamTurn(Room room) {
        requireRoomInProgress(room);
        if (PlayerTeam.RED.toString().equals(room.getTeamTurn())) {
            room.setTeamTurn(PlayerTeam.BLUE.toString());
        } else {
            room.setTeamTurn(PlayerTeam.RED.toString());
        }
        room.setRoleTurn(PlayerRole.SPYMASTER.toString());
    }

    public void manualTeamTurn(Room room, String pseudo) {
        requireRoomInProgress(room);
        Player player = requirePlayer(room, pseudo);
        if (player.getPlayerRole() != PlayerRole.OPERATIVE
                || !player.getPlayerTeam().toString().equals(room.getTeamTurn())) {
            throw new IllegalArgumentException("Player not found, not operative or not on the right team");
        }
        switchTeamTurn(room);
    }

    public void selectWord(Room room, String wordName, String pseudo) {
        requireRoomInProgress(room);
        Player player = requirePlayer(room, pseudo);
        if (player.getPlayerRole() != PlayerRole.OPERATIVE) {
            throw new IllegalArgumentException("Player not found or not operative");
        }
        Word word = requireWord(room, wordName);
        if (word.getWordState() == WordState.CLICKED) {
            throw new IllegalArgumentException("Word already clicked");
        }
        if (!player.getPlayerTeam().toString().equals(room.getTeamTurn())) {
            throw new IllegalArgumentException("Player is not on the right team");
        }
        if (word.getSelectedBy().contains(player.getName())) {
            word.getSelectedBy().remove(player.getName());
        } else {
            word.getSelectedBy().add(player.getName());
        }
        word.setWordState(word.getSelectedBy().isEmpty() ? WordState.NOT_SELECTED : WordState.SELECTED);
    }

    public void clickWord(Room room, String wordName, String pseudo) {
        requireRoomInProgress(room);
        Player player = requirePlayer(room, pseudo);
        if (player.getPlayerRole() != PlayerRole.OPERATIVE) {
            throw new IllegalArgumentException("Player not found or not operative");
        }
        Word word = requireWord(room, wordName);
        if (word.getWordState() == WordState.CLICKED) {
            throw new IllegalArgumentException("Word already clicked");
        }
        if (!player.getPlayerTeam().toString().equals(room.getTeamTurn())) {
            throw new IllegalArgumentException("Player is not on the right team");
        }
        if (room.getClues().isEmpty()) {
            return;
        }

        word.setWordState(WordState.CLICKED);
        word.setSelectedBy(new ArrayList<>());

        if (word.getWordColor() == WordColor.RED) {
            room.setRedRemainingWords(room.getRedRemainingWords() - 1);
        } else if (word.getWordColor() == WordColor.BLUE) {
            room.setBlueRemainingWords(room.getBlueRemainingWords() - 1);
        } else if (word.getWordColor() == WordColor.WHITE) {
            switchTeamTurn(room);
        } else if (word.getWordColor() == WordColor.BLACK) {
            room.setIsBlackCardSelected(true);
        }

        applyWinConditions(room);

        if (!room.getClues().isEmpty() && room.getStatus() == RoomStatus.IN_PROGRESS) {
            Clue clue = room.getClues().get(room.getClues().size() - 1);
            clue.setRemaining(clue.getRemaining() - 1);
            if (clue.getRemaining() == 0) {
                switchTeamTurn(room);
            }
        }
    }

    public void addClue(Room room, Clue clue, String pseudo) {
        requireRoomInProgress(room);
        Player player = requirePlayer(room, pseudo);
        if (player.getPlayerRole() != PlayerRole.SPYMASTER) {
            throw new IllegalArgumentException("Player not found or not spymaster");
        }
        if (!player.getPlayerTeam().toString().equals(room.getTeamTurn())) {
            throw new IllegalArgumentException("Player is not spymaster or not on the right team");
        }
        if (room.getWords().stream().anyMatch(word -> word.getWordName().equals(clue.getClueName()))) {
            throw new IllegalArgumentException("Clue name is a word from the board");
        }
        room.getClues().add(new Clue(clue.getClueName(), clue.getAttempts(), clue.getRemaining(), player.getName()));
        room.setRoleTurn(PlayerRole.OPERATIVE.toString());
    }

    public void applyWinConditions(Room room) {
        if (room.getRedRemainingWords() == 0) {
            room.setStatus(RoomStatus.RED_TEAM_WINS);
            room.setUpdatedAt(LocalDateTime.now());
        } else if (room.getBlueRemainingWords() == 0) {
            room.setStatus(RoomStatus.BLUE_TEAM_WINS);
            room.setUpdatedAt(LocalDateTime.now());
        } else if (Boolean.TRUE.equals(room.getIsBlackCardSelected())) {
            if (PlayerTeam.RED.toString().equals(room.getTeamTurn())) {
                room.setStatus(RoomStatus.BLUE_TEAM_WINS);
            } else {
                room.setStatus(RoomStatus.RED_TEAM_WINS);
            }
            room.setUpdatedAt(LocalDateTime.now());
        }
    }

    public List<Word> dealBoard(List<String> wordPool) {
        if (wordPool == null || wordPool.size() < 25) {
            throw new IllegalStateException("Not enough words to deal a board");
        }
        List<String> frenchWords = new ArrayList<>(wordPool);
        Collections.shuffle(frenchWords, random);
        frenchWords = new ArrayList<>(frenchWords.subList(0, 25));

        List<Word> wordsToReturn = new ArrayList<>();
        int redWordsLength = random.nextBoolean() ? 9 : 8;
        int blueWordsLength = redWordsLength == 9 ? 8 : 9;

        addColoredWords(wordsToReturn, frenchWords, redWordsLength, WordColor.RED);
        addColoredWords(wordsToReturn, frenchWords, blueWordsLength, WordColor.BLUE);
        addColoredWords(wordsToReturn, frenchWords, 1, WordColor.BLACK);
        addColoredWords(wordsToReturn, frenchWords, 7, WordColor.WHITE);

        Collections.shuffle(wordsToReturn, random);
        return wordsToReturn;
    }

    public void setTeamAndRoleTurn(Room room, List<Word> words) {
        int redCount = countWordsByColor(words, WordColor.RED);
        int blueCount = countWordsByColor(words, WordColor.BLUE);

        if (redCount > blueCount) {
            room.setTeamTurn(PlayerTeam.RED.toString());
        } else {
            room.setTeamTurn(PlayerTeam.BLUE.toString());
        }
        room.setRoleTurn(PlayerRole.SPYMASTER.toString());
        room.setRedRemainingWords(redCount);
        room.setBlueRemainingWords(blueCount);
    }

    public Room initializeRoom() {
        Room room = new Room();
        room.setPlayers(new ArrayList<>());
        room.setClues(new ArrayList<>());
        room.setIsBlackCardSelected(false);
        room.setStatus(RoomStatus.PENDING);
        room.setCreatedAt(LocalDateTime.now());
        room.setUpdatedAt(LocalDateTime.now());
        return room;
    }

    public Player createPlayer(String pseudo) {
        return new Player(pseudo, PlayerTeam.NONE, PlayerRole.NONE);
    }

    public PlayerTeam mapStringToPlayerTeam(String team) {
        if ("RED".equals(team)) {
            return PlayerTeam.RED;
        }
        if ("BLUE".equals(team)) {
            return PlayerTeam.BLUE;
        }
        return PlayerTeam.NONE;
    }

    public PlayerRole mapStringToPlayerRole(String role) {
        if ("SPYMASTER".equals(role)) {
            return PlayerRole.SPYMASTER;
        }
        if ("OPERATIVE".equals(role)) {
            return PlayerRole.OPERATIVE;
        }
        return PlayerRole.NONE;
    }

    private Word requireWord(Room room, String wordName) {
        return room.getWords().stream()
                .filter(w -> w.getWordName().equals(wordName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Word not found"));
    }

    private void addColoredWords(List<Word> target, List<String> pool, int count, WordColor color) {
        for (int i = 0; i < count; i++) {
            String wordName = pool.remove(random.nextInt(pool.size()));
            target.add(new Word(wordName, new ArrayList<>(), WordState.NOT_SELECTED, color));
        }
    }

    private int countWordsByColor(List<Word> words, WordColor color) {
        return (int) words.stream().filter(word -> word.getWordColor() == color).count();
    }
}
