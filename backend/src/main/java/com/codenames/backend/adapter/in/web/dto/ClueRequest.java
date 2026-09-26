package com.codenames.backend.adapter.in.web.dto;

public record ClueRequest(
        String clueName,
        int attempts,
        int remaining,
        String spyName) {
}
