package requests;

import model.GameEntry;

import java.util.Collection;

public record ListGamesResult(Collection<GameEntry> games, String message) {
}
