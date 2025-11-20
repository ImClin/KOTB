package me.clin.kOTB.placeholder;

import me.clin.kOTB.KOTB;
import me.clin.kOTB.manager.StatsManager;
import me.clin.kOTB.manager.StatsManager.LeaderboardEntry;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Optional;

public class TournamentPlaceholderExpansion extends PlaceholderExpansion {

    private final KOTB plugin;
    private final StatsManager statsManager;

    public TournamentPlaceholderExpansion(KOTB plugin, StatsManager statsManager) {
        this.plugin = plugin;
        this.statsManager = statsManager;
    }

    @Override
    public String getIdentifier() {
        return "kotb";
    }

    @Override
    public String getAuthor() {
        return String.join(", ", plugin.getDescription().getAuthors());
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, String params) {
        if (params == null) {
            return "";
        }
        String key = params.toLowerCase(Locale.ROOT);
        return switch (key) {
            case "meeste_duels" -> formatLeaderboard(statsManager.getMostDuelWins(), "duelwinsten");
            case "meeste_toernooien" -> formatLeaderboard(statsManager.getMostTournamentWins(), "toernooiwinsten");
            case "mijn_duels" -> player == null ? "0" : String.valueOf(statsManager.getDuelWins(player.getUniqueId()));
            case "mijn_toernooien" -> player == null ? "0" : String.valueOf(statsManager.getTournamentWins(player.getUniqueId()));
            default -> "";
        };
    }

    private String formatLeaderboard(Optional<LeaderboardEntry> entry, String suffix) {
        return entry
                .map(e -> statsManager.resolveName(e.playerId()) + " - " + e.value() + " " + suffix)
                .orElse("Nog geen gegevens");
    }
}
