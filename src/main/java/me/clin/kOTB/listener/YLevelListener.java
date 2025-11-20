package me.clin.kOTB.listener;

import me.clin.kOTB.manager.ConfigManager;
import me.clin.kOTB.manager.TournamentManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class YLevelListener implements Listener {

    private final ConfigManager configManager;
    private final TournamentManager tournamentManager;

    public YLevelListener(ConfigManager configManager, TournamentManager tournamentManager) {
        this.configManager = configManager;
        this.tournamentManager = tournamentManager;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo() == null) {
            return;
        }
        Player player = event.getPlayer();
        double threshold = configManager.getYLevel();
        if (event.getTo().getY() < threshold) {
            tournamentManager.handleYLevelDrop(player);
        }
    }
}
