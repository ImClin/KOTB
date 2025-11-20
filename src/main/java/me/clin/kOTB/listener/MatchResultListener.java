package me.clin.kOTB.listener;

import me.clin.kOTB.manager.TournamentManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class MatchResultListener implements Listener {

    private final TournamentManager tournamentManager;

    public MatchResultListener(TournamentManager tournamentManager) {
        this.tournamentManager = tournamentManager;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        tournamentManager.getActiveMatch(player.getUniqueId())
                .ifPresent(match -> match.handleDeath(player.getUniqueId()));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        tournamentManager.getActiveMatch(player.getUniqueId())
                .ifPresent(match -> match.handleDisconnect(player.getUniqueId()));
    }
}
