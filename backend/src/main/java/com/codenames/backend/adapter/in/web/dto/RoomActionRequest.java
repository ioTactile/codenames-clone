package com.codenames.backend.adapter.in.web.dto;

import java.util.List;

public record RoomActionRequest(
        String action,
        String username,
        String newUsername,
        String wordname,
        String role,
        String team,
        ClueRequest clue,
        List<String> usernames) {
}
