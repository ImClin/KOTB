package me.clin.kOTB.listener;

import me.clin.kOTB.manager.TournamentManager;
import me.clin.kOTB.model.Match;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.Optional;
import java.util.UUID;

public class CombatOverrideListener implements Listener {

    private final TournamentManager tournamentManager;

    public CombatOverrideListener(TournamentManager tournamentManager) {
        this.tournamentManager = tournamentManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim) || !(event.getDamager() instanceof Player damager)) {
            return;
        }
        UUID damagerId = damager.getUniqueId();
        Optional<Match> activeMatch = tournamentManager.getActiveMatch(damagerId);
        if (activeMatch.isEmpty()) {
            return;
        }
        Match match = activeMatch.get();
        if (!match.isActive()) {
            return;
        }
        UUID victimId = victim.getUniqueId();
        if (!match.getPlayerA().equals(victimId) && !match.getPlayerB().equals(victimId)) {
            return;
        }
        if (event.isCancelled()) {
            event.setCancelled(false);
        }
        event.setDamage(0.0);
        forceCombatState(damager);
        forceCombatState(victim);
    }

    private void forceCombatState(Player player) {
        player.setInvulnerable(false);
        player.setNoDamageTicks(0);
    }
}
