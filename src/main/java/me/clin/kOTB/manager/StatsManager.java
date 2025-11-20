package me.clin.kOTB.manager;

import me.clin.kOTB.KOTB;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

public class StatsManager {

    public record LeaderboardEntry(UUID playerId, int value) {
    }

    private static class PlayerStats {
        private int duelWins;
        private int tournamentWins;

        PlayerStats(int duelWins, int tournamentWins) {
            this.duelWins = duelWins;
            this.tournamentWins = tournamentWins;
        }
    }

    private final KOTB plugin;
    private final Map<UUID, PlayerStats> stats = new HashMap<>();

    private File file;
    private FileConfiguration configuration;

    public StatsManager(KOTB plugin) {
        this.plugin = plugin;
    }

    public void load() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        this.file = new File(plugin.getDataFolder(), "stats.yml");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException ex) {
                plugin.getLogger().log(Level.SEVERE, "Kon stats.yml niet aanmaken", ex);
            }
        }
        this.configuration = YamlConfiguration.loadConfiguration(file);
        stats.clear();
        for (String key : configuration.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                int duel = configuration.getInt(key + ".duelWins", 0);
                int tournament = configuration.getInt(key + ".tournamentWins", 0);
                stats.put(uuid, new PlayerStats(duel, tournament));
            } catch (IllegalArgumentException ex) {
                plugin.getLogger().warning("Ongeldige UUID in stats.yml: " + key);
            }
        }
    }

    public void saveSync() {
        if (configuration == null || file == null) {
            return;
        }
        for (Map.Entry<UUID, PlayerStats> entry : stats.entrySet()) {
            String key = entry.getKey().toString();
            configuration.set(key + ".duelWins", entry.getValue().duelWins);
            configuration.set(key + ".tournamentWins", entry.getValue().tournamentWins);
        }
        try {
            configuration.save(file);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "Kon stats.yml niet opslaan", ex);
        }
    }

    public void saveAsync() {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, this::saveSync);
    }

    public void incrementDuelWin(UUID uuid) {
        if (uuid == null) {
            return;
        }
        PlayerStats playerStats = stats.computeIfAbsent(uuid, unused -> new PlayerStats(0, 0));
        playerStats.duelWins++;
        saveAsync();
    }

    public void incrementTournamentWin(UUID uuid) {
        if (uuid == null) {
            return;
        }
        PlayerStats playerStats = stats.computeIfAbsent(uuid, unused -> new PlayerStats(0, 0));
        playerStats.tournamentWins++;
        saveAsync();
    }

    public int getDuelWins(UUID uuid) {
        PlayerStats playerStats = stats.get(uuid);
        return playerStats == null ? 0 : playerStats.duelWins;
    }

    public int getTournamentWins(UUID uuid) {
        PlayerStats playerStats = stats.get(uuid);
        return playerStats == null ? 0 : playerStats.tournamentWins;
    }

    public Optional<LeaderboardEntry> getMostDuelWins() {
        return stats.entrySet().stream()
            .max(Comparator.comparingInt(entry -> entry.getValue().duelWins))
                .map(entry -> new LeaderboardEntry(entry.getKey(), entry.getValue().duelWins));
    }

    public Optional<LeaderboardEntry> getMostTournamentWins() {
        return stats.entrySet().stream()
            .max(Comparator.comparingInt(entry -> entry.getValue().tournamentWins))
                .map(entry -> new LeaderboardEntry(entry.getKey(), entry.getValue().tournamentWins));
    }

    public Map<UUID, PlayerStats> getUnmodifiableStats() {
        return Collections.unmodifiableMap(stats);
    }

    public String resolveName(UUID uuid) {
        if (uuid == null) {
            return "Onbekend";
        }
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
        return offlinePlayer.getName() != null ? offlinePlayer.getName() : uuid.toString();
    }
}
