package Requests;

import model.GameEntry;

import java.util.List;

public record ListGamesResult(List<GameEntry> games, String message) {
}
