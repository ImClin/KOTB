package me.clin.kOTB.model;

import me.clin.kOTB.manager.CountdownManager;
import me.clin.kOTB.manager.PlayerFreezeManager;
import me.clin.kOTB.manager.PositionManager;
import me.clin.kOTB.manager.PositionManager.ArenaPosition;
import me.clin.kOTB.manager.TournamentManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class Match {

    private enum MatchState {
        CREATED,
        COUNTDOWN,
        ACTIVE,
        COMPLETE
    }

    private final TournamentManager tournamentManager;
    private final UUID playerA;
    private final UUID playerB;
    private final int roundNumber;
    private MatchState state = MatchState.CREATED;
    private BukkitRunnable countdownTask;

    public Match(TournamentManager tournamentManager, UUID playerA, UUID playerB, int roundNumber) {
        this.tournamentManager = tournamentManager;
        this.playerA = playerA;
        this.playerB = playerB;
        this.roundNumber = roundNumber;
    }

    public void start() {
        Optional<Player> optionalA = getOnline(playerA);
        Optional<Player> optionalB = getOnline(playerB);
        if (optionalA.isEmpty() && optionalB.isEmpty()) {
            tournamentManager.broadcast("§6§l[Toernooi] §cBeide spelers waren offline, match geskipt.");
            tournamentManager.handleMatchFinished(this, playerA);
            return;
        }
        if (optionalA.isEmpty()) {
            declareWinner(playerB, "Tegenstander was offline.", "Je was offline tijdens de start.");
            return;
        }
        if (optionalB.isEmpty()) {
            declareWinner(playerA, "Tegenstander was offline.", "Je was offline tijdens de start.");
            return;
        }

        Player first = optionalA.get();
        Player second = optionalB.get();

        PositionManager positionManager = tournamentManager.getPositionManager();
        Location locationA = positionManager.getPosition(ArenaPosition.A).orElse(null);
        Location locationB = positionManager.getPosition(ArenaPosition.B).orElse(null);
        if (locationA == null || locationB == null) {
            tournamentManager.broadcast("§cPosities ontbreken, match kan niet starten.");
            tournamentManager.handleMatchFinished(this, playerA);
            return;
        }

        first.teleport(locationA);
        second.teleport(locationB);
        resetDamageWindow(first);
        resetDamageWindow(second);

        List<Player> players = Arrays.asList(first, second);
        PlayerFreezeManager freezeManager = tournamentManager.getFreezeManager();
        tournamentManager.registerActiveMatch(this);
        freezeManager.freeze(players);
        first.sendMessage("§6§l[Toernooi] §eJe duel begint zo!");
        second.sendMessage("§6§l[Toernooi] §eJe duel begint zo!");

        CountdownManager countdownManager = tournamentManager.getCountdownManager();
        state = MatchState.COUNTDOWN;
        countdownTask = countdownManager.runMatchCountdown(players, () -> {
            freezeManager.unfreeze(players);
            state = MatchState.ACTIVE;
            for (Player player : players) {
                if (player != null && player.isOnline()) {
                    player.sendMessage("§6§l[Toernooi] §aHet duel is begonnen!");
                    resetDamageWindow(player);
                }
            }
            countdownTask = null;
        });
    }

    public void handleYLevelLoss(UUID loserId) {
        if (state == MatchState.COMPLETE) {
            return;
        }
        forfeit(loserId,
                "Tegenstander is als eerste van de brug gevallen.",
                "Je bent als eerste van de brug gevallen.");
    }

    public void handleDisconnect(UUID playerId) {
        if (state == MatchState.COMPLETE) {
            return;
        }
        forfeit(playerId,
                "Tegenstander heeft de server verlaten.",
                "Je hebt de server verlaten.");
    }

    public void handleDeath(UUID playerId) {
        if (state == MatchState.COMPLETE) {
            return;
        }
        forfeit(playerId,
                "Tegenstander is uitgeschakeld.",
                "Je bent uitgeschakeld.");
    }

    public void declareWinner(UUID winnerId, String winnerReason, String loserReason) {
        if (state == MatchState.COMPLETE) {
            return;
        }
        state = MatchState.COMPLETE;
        cancelCountdown();
        cleanupPlayers();
        Optional<Player> winner = getOnline(winnerId);
        String winnerMessage = winnerReason == null ? "" : " " + winnerReason;
        winner.ifPresent(player -> player.sendMessage("§6§l[Toernooi] §aJe hebt gewonnen!" + winnerMessage));
        UUID loserId = getOpponent(winnerId);
        if (loserId != null) {
            String loserMessage = loserReason == null ? "" : " " + loserReason;
            getOnline(loserId).ifPresent(player -> player.sendMessage("§6§l[Toernooi] §cJe hebt verloren." + loserMessage));
            tournamentManager.eliminateParticipant(loserId, roundNumber);
        }
        if (winnerId != null) {
            tournamentManager.getStatsManager().incrementDuelWin(winnerId);
        }
        tournamentManager.handleMatchFinished(this, winnerId);
    }

    public void declareWinner(UUID winnerId, String reason) {
        declareWinner(winnerId, reason, reason);
    }

    public void cancel(String reason) {
        if (state == MatchState.COMPLETE) {
            return;
        }
        state = MatchState.COMPLETE;
        cancelCountdown();
        cleanupPlayers();
        getOnline(playerA).ifPresent(player -> player.sendMessage("§6§l[Toernooi] §cMatch geannuleerd: " + reason));
        getOnline(playerB).ifPresent(player -> player.sendMessage("§6§l[Toernooi] §cMatch geannuleerd: " + reason));
        tournamentManager.unregisterMatch(this);
    }

    private void cleanupPlayers() {
        PlayerFreezeManager freezeManager = tournamentManager.getFreezeManager();
        getOnline(playerA).ifPresent(freezeManager::unfreeze);
        getOnline(playerB).ifPresent(freezeManager::unfreeze);
        freezeManager.unfreeze(playerA);
        freezeManager.unfreeze(playerB);
        tournamentManager.getSpectatorLocation().ifPresent(location -> {
            getOnline(playerA).ifPresent(player -> player.teleport(location));
            getOnline(playerB).ifPresent(player -> player.teleport(location));
        });
    }

    private void cancelCountdown() {
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
    }

    private Optional<Player> getOnline(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null || !player.isOnline()) {
            return Optional.empty();
        }
        return Optional.of(player);
    }

    private boolean contains(UUID uuid) {
        return playerA.equals(uuid) || playerB.equals(uuid);
    }

    private void forfeit(UUID loserId, String winnerReason, String loserReason) {
        if (!contains(loserId)) {
            return;
        }
        UUID opponent = getOpponent(loserId);
        if (opponent == null) {
            return;
        }
        declareWinner(opponent, winnerReason, loserReason);
    }

    public UUID getOpponent(UUID uuid) {
        if (playerA.equals(uuid)) {
            return playerB;
        }
        if (playerB.equals(uuid)) {
            return playerA;
        }
        return null;
    }

    public UUID getPlayerA() {
        return playerA;
    }

    public UUID getPlayerB() {
        return playerB;
    }

    public boolean isActive() {
        return state == MatchState.ACTIVE;
    }

    private void resetDamageWindow(Player player) {
        player.setInvulnerable(false);
        player.setNoDamageTicks(0);
    }
}
