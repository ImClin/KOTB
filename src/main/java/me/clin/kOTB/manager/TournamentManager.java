package me.clin.kOTB.manager;

import me.clin.kOTB.KOTB;
import me.clin.kOTB.manager.PositionManager.ArenaPosition;
import me.clin.kOTB.model.Match;
import me.clin.kOTB.model.Round;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

public class TournamentManager {

    private final KOTB plugin;
    private final PositionManager positionManager;
    private final ConfigManager configManager;
    private final PlayerFreezeManager freezeManager;
    private final CountdownManager countdownManager;
    private final StatsManager statsManager;

    private final List<UUID> registeredParticipants = new ArrayList<>();
    private final Map<UUID, Match> activeMatches = new HashMap<>();
    private final List<UUID> eliminationOrder = new ArrayList<>();
    private final Map<UUID, Integer> eliminationRounds = new HashMap<>();

    private List<UUID> currentCompetitors = new ArrayList<>();
    private List<UUID> initialBracket = new ArrayList<>();
    private Round currentRound;
    private int roundNumber = 0;
    private boolean running = false;
    private boolean registrationLocked = false;
    private boolean awaitingRoundStart = false;
    private boolean paused = false;
    private Match pendingMatch;
    private BukkitTask preMatchTask;
    private int preMatchSecondsRemaining = 0;

    public TournamentManager(KOTB plugin,
                              PositionManager positionManager,
                              ConfigManager configManager,
                              PlayerFreezeManager freezeManager,
                              CountdownManager countdownManager,
                              StatsManager statsManager) {
        this.plugin = plugin;
        this.positionManager = positionManager;
        this.configManager = configManager;
        this.freezeManager = freezeManager;
        this.countdownManager = countdownManager;
        this.statsManager = statsManager;
    }

    public boolean addParticipant(UUID uuid) {
        if (registrationLocked || running) {
            return false;
        }
        if (registeredParticipants.contains(uuid)) {
            return false;
        }
        registeredParticipants.add(uuid);
        return true;
    }

    public boolean removeParticipant(UUID uuid) {
        if (running) {
            return false;
        }
        return registeredParticipants.remove(uuid);
    }

    public List<String> getParticipantNames() {
        List<String> names = new ArrayList<>();
        for (UUID uuid : registeredParticipants) {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
            String name = offlinePlayer.getName() == null ? uuid.toString() : offlinePlayer.getName();
            names.add(name);
        }
        return names;
    }

    public boolean startTournament(CommandSender sender) {
        if (running) {
            sender.sendMessage("§cEr loopt al een toernooi.");
            return false;
        }
        if (registeredParticipants.size() < 2) {
            sender.sendMessage("§cMinimaal 2 spelers zijn nodig.");
            return false;
        }
        if (!positionManager.hasBothPositions()) {
            sender.sendMessage("§cPosities A en B zijn nog niet ingesteld.");
            return false;
        }
        running = true;
        registrationLocked = true;
        roundNumber = 0;
        currentCompetitors = new ArrayList<>(registeredParticipants);
        initialBracket = new ArrayList<>(registeredParticipants);
        eliminationOrder.clear();
        eliminationRounds.clear();
        Collections.shuffle(currentCompetitors);
        broadcastLegacy("§6§l[Toernooi] §eBracket vergrendeld. Run §f/kotb tournament start §eom te beginnen.");
        startNextRound(false);
        announceUpcomingMatches();
        awaitingRoundStart = true;
        return true;
    }

    private void startNextRound(boolean autoStart) {
        roundNumber++;
        List<UUID> playersForRound = new ArrayList<>(currentCompetitors);
        currentRound = new Round(this, roundNumber, playersForRound);
        if (autoStart) {
            currentRound.start();
        } else {
            awaitingRoundStart = true;
        }
    }

    public void registerActiveMatch(Match match) {
        activeMatches.put(match.getPlayerA(), match);
        activeMatches.put(match.getPlayerB(), match);
    }

    public void unregisterMatch(Match match) {
        activeMatches.remove(match.getPlayerA());
        activeMatches.remove(match.getPlayerB());
    }

    public Optional<Match> getActiveMatch(UUID uuid) {
        return Optional.ofNullable(activeMatches.get(uuid));
    }

    public void handleMatchFinished(Match match, UUID winnerId) {
        unregisterMatch(match);
        if (!running || currentRound == null) {
            return;
        }
        currentRound.onMatchFinished(winnerId);
    }

    public void eliminateParticipant(UUID uuid, int eliminationRound) {
        if (uuid == null) {
            return;
        }
        registeredParticipants.remove(uuid);
        if (!eliminationOrder.contains(uuid)) {
            eliminationOrder.add(uuid);
        }
        eliminationRounds.put(uuid, eliminationRound);
    }

    public void onRoundComplete(List<UUID> winners) {
        if (!running) {
            return;
        }
        currentCompetitors = new ArrayList<>(winners);
        if (winners.isEmpty()) {
            finishTournament(null);
            return;
        }
        if (winners.size() == 1) {
            finishTournament(winners.get(0));
            return;
        }
        Collections.shuffle(currentCompetitors);
        startNextRound(true);
        announceUpcomingMatches();
    }

    private void finishTournament(UUID winnerId) {
        finishTournament(winnerId, null);
    }

    private void finishTournament(UUID winnerId, String overrideMessage) {
        running = false;
        roundNumber = 0;
        currentRound = null;
        currentCompetitors.clear();
        freezeManager.clear();
        cancelPreMatchCountdown();
        pendingMatch = null;
        paused = false;
        awaitingRoundStart = false;
        registrationLocked = false;
        preMatchSecondsRemaining = 0;
        registeredParticipants.clear();
        for (Match match : new HashSet<>(activeMatches.values())) {
            match.cancel("Toernooi beëindigd.");
        }
        activeMatches.clear();

        if (winnerId == null) {
            String message = overrideMessage != null && !overrideMessage.isBlank()
                    ? overrideMessage
                    : "§6§l[Toernooi] §eToernooi beëindigd.";
            broadcastLegacy(message);
            announcePlacements(null);
            cleanupPlacementTracking();
            return;
        }

        Player winner = Bukkit.getPlayer(winnerId);
        String winnerName = winner != null ? winner.getName() : Bukkit.getOfflinePlayer(winnerId).getName();
        if (winnerName == null) {
            winnerName = winnerId.toString();
        }

        statsManager.incrementTournamentWin(winnerId);

        Component title = LegacyComponentSerializer.legacySection().deserialize("§6§lWINNAAR!");
        Component subtitle = LegacyComponentSerializer.legacySection().deserialize("§f" + winnerName + " heeft het toernooi gewonnen!");
        Title.Times times = Title.Times.times(Duration.ofMillis(0), Duration.ofSeconds(2), Duration.ofSeconds(1));
        for (Player online : Bukkit.getOnlinePlayers()) {
            online.showTitle(Title.title(title, subtitle, times));
            online.sendMessage("§6§l[Toernooi] §e" + winnerName + " heeft het toernooi gewonnen!");
        }

        announcePlacements(winnerId);
        cleanupPlacementTracking();
    }

    public void handleYLevelDrop(Player player) {
        if (player == null) {
            return;
        }
        getActiveMatch(player.getUniqueId()).ifPresent(match -> match.handleYLevelLoss(player.getUniqueId()));
    }

    public boolean forceWin(UUID winnerId) {
        Optional<Match> match = getActiveMatch(winnerId);
        if (match.isEmpty()) {
            return false;
        }
        match.get().declareWinner(winnerId, "Gewonnen via admin commando.");
        return true;
    }

    public boolean forceFinish(String message) {
        if (!running) {
            return false;
        }
        String reason = message != null && !message.isBlank()
                ? message
                : "§6§l[Toernooi] §cHet toernooi is handmatig beëindigd.";
        finishTournament(null, reason);
        return true;
    }

    public boolean beginMatchSequence(CommandSender sender) {
        if (!running) {
            sender.sendMessage("§cEr is geen toernooi actief.");
            return false;
        }
        if (!activeMatches.isEmpty()) {
            sender.sendMessage("§cEr is al een duel bezig.");
            return false;
        }
        if (paused && pendingMatch != null && preMatchSecondsRemaining > 0) {
            paused = false;
            sender.sendMessage("§aCountdown hervat.");
            runPreMatchCountdown();
            return true;
        }
        if (awaitingRoundStart && currentRound != null) {
            awaitingRoundStart = false;
            sender.sendMessage("§aRonde " + roundNumber + " start. Eerste duel volgt over 10 seconden.");
            currentRound.start();
            return true;
        }
        if (pendingMatch != null && preMatchTask == null) {
            sender.sendMessage("§aCountdown voor het volgende duel wordt hervat.");
            runPreMatchCountdown();
            return true;
        }
        sender.sendMessage("§eGeen countdown actief of duel al gestart.");
        return false;
    }

    public boolean pauseCountdown(CommandSender sender) {
        if (!running) {
            sender.sendMessage("§cEr is geen toernooi actief.");
            return false;
        }
        if (!activeMatches.isEmpty()) {
            sender.sendMessage("§cKan niet pauzeren tijdens een duel.");
            return false;
        }
        if (preMatchTask == null) {
            sender.sendMessage("§eEr is geen countdown om te pauzeren.");
            return false;
        }
        paused = true;
        cancelPreMatchCountdown();
        sender.sendMessage("§aCountdown gepauzeerd.");
        return true;
    }

    public void prepareMatchStart(Match match) {
        pendingMatch = match;
        preMatchSecondsRemaining = 10;
        paused = false;
        cancelPreMatchCountdown();
        announceMatchCountdown(match);
        runPreMatchCountdown();
    }

    private void announceMatchCountdown(Match match) {
        String nameA = resolvePlayerName(match.getPlayerA());
        String nameB = resolvePlayerName(match.getPlayerB());
        broadcastLegacy("§6§l[Toernooi] §f" + nameA + " §cvs§f " + nameB + " §7start in §c10 seconden.");
    }

    private void runPreMatchCountdown() {
        cancelPreMatchCountdown();
        if (pendingMatch == null) {
            return;
        }
        preMatchTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (paused) {
                    cancelPreMatchCountdown();
                    return;
                }
                if (pendingMatch == null) {
                    cancelPreMatchCountdown();
                    return;
                }
                if (preMatchSecondsRemaining <= 0) {
                    Match match = pendingMatch;
                    pendingMatch = null;
                    cancelPreMatchCountdown();
                    match.start();
                    return;
                }
                sendPreMatchTitle(preMatchSecondsRemaining);
                preMatchSecondsRemaining--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void sendPreMatchTitle(int seconds) {
        Component title = LegacyComponentSerializer.legacySection().deserialize("§c§lVolgende duel");
        Component subtitle = LegacyComponentSerializer.legacySection().deserialize("§fStart in §c" + seconds + "s");
        Title.Times times = Title.Times.times(Duration.ofMillis(0), Duration.ofMillis(1000), Duration.ofMillis(250));
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.showTitle(Title.title(title, subtitle, times));
        }
    }

    public boolean isRunning() {
        return running;
    }

    public List<UUID> getRegisteredParticipants() {
        return Collections.unmodifiableList(registeredParticipants);
    }

    public KOTB getPlugin() {
        return plugin;
    }

    public PositionManager getPositionManager() {
        return positionManager;
    }

    public PlayerFreezeManager getFreezeManager() {
        return freezeManager;
    }

    public CountdownManager getCountdownManager() {
        return countdownManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public StatsManager getStatsManager() {
        return statsManager;
    }

    public boolean canJoin() {
        return !registrationLocked && !running;
    }

    public Optional<Location> getSpectatorLocation() {
        return positionManager.getPosition(ArenaPosition.C);
    }

    public void shutdown() {
        running = false;
        registrationLocked = false;
        awaitingRoundStart = false;
        paused = false;
        pendingMatch = null;
        cancelPreMatchCountdown();
        if (currentRound != null) {
            currentRound = null;
        }
        for (Match match : new HashSet<>(activeMatches.values())) {
            match.cancel("Plugin uitgeschakeld.");
        }
        activeMatches.clear();
        freezeManager.clear();
        currentCompetitors.clear();
        cleanupPlacementTracking();
    }

    public boolean isParticipant(UUID uuid) {
        return registeredParticipants.contains(uuid);
    }

    public void broadcast(String message) {
        broadcastLegacy(message);
    }

    private void broadcastLegacy(String message) {
        Component component = LegacyComponentSerializer.legacySection().deserialize(message);
        plugin.getServer().getConsoleSender().sendMessage(component);
        for (Player online : Bukkit.getOnlinePlayers()) {
            online.sendMessage(component);
        }
    }

    private void announceUpcomingMatches() {
        if (currentRound == null) {
            return;
        }
        List<Match> matches = currentRound.getMatches();
        if (matches.isEmpty()) {
            return;
        }
        broadcastLegacy("§6§l[Toernooi] §eAankomende duels:" );
        for (Match match : matches) {
            String nameA = resolvePlayerName(match.getPlayerA());
            String nameB = resolvePlayerName(match.getPlayerB());
            broadcastLegacy("§7- §f" + nameA + " §cvs§f " + nameB);
        }
    }

    private String resolvePlayerName(UUID uuid) {
        if (uuid == null) {
            return "Onbekend";
        }
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
        return offlinePlayer.getName() != null ? offlinePlayer.getName() : uuid.toString();
    }

    private void cancelPreMatchCountdown() {
        if (preMatchTask != null) {
            preMatchTask.cancel();
            preMatchTask = null;
        }
    }

    private void announcePlacements(UUID winnerId) {
        if (initialBracket.isEmpty() && eliminationOrder.isEmpty()) {
            return;
        }
        int remaining = !initialBracket.isEmpty() ? initialBracket.size() : eliminationOrder.size() + (winnerId != null ? 1 : 0);
        if (remaining <= 0) {
            return;
        }
        Map<Integer, List<UUID>> grouped = new TreeMap<>();
        for (UUID uuid : eliminationOrder) {
            int round = eliminationRounds.getOrDefault(uuid, 0);
            grouped.computeIfAbsent(round, key -> new ArrayList<>()).add(uuid);
        }
        broadcastLegacy("§6§l[Toernooi] §eEindstand:");
        for (int round : grouped.keySet()) {
            List<UUID> eliminated = grouped.get(round);
            if (eliminated == null || eliminated.isEmpty()) {
                continue;
            }
            String label = formatPlacementRange(remaining, eliminated.size());
            broadcastLegacy("§7" + label + ": §f" + joinNames(eliminated));
            remaining -= eliminated.size();
        }

        List<UUID> survivors = new ArrayList<>();
        Set<UUID> accounted = new HashSet<>(eliminationOrder);
        if (winnerId != null) {
            accounted.add(winnerId);
        }
        for (UUID uuid : initialBracket) {
            if (uuid == null || accounted.contains(uuid)) {
                continue;
            }
            survivors.add(uuid);
        }
        if (!survivors.isEmpty() && remaining > (winnerId != null ? 1 : 0)) {
            String label = formatPlacementRange(remaining, survivors.size());
            broadcastLegacy("§7" + label + ": §f" + joinNames(survivors));
            remaining -= survivors.size();
        }

        if (winnerId != null && remaining >= 1) {
            String label = formatPlacementRange(remaining, 1);
            broadcastLegacy("§7" + label + ": §6" + resolvePlayerName(winnerId));
        }
    }

    private void cleanupPlacementTracking() {
        eliminationOrder.clear();
        eliminationRounds.clear();
        initialBracket.clear();
    }

    private String formatPlacementRange(int start, int count) {
        if (count <= 0) {
            return String.valueOf(start);
        }
        int end = Math.max(1, start - count + 1);
        if (start == end) {
            return String.valueOf(start);
        }
        return start + "-" + end;
    }

    private String joinNames(List<UUID> uuids) {
        return uuids.stream()
                .map(this::resolvePlayerName)
                .collect(Collectors.joining("§7, §f"));
    }
}
