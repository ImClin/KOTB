package me.clin.kOTB.manager;

import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerFreezeManager {

    private final Set<UUID> frozen = ConcurrentHashMap.newKeySet();

    public void freeze(Player player) {
        if (player != null) {
            frozen.add(player.getUniqueId());
        }
    }

    public void freeze(Collection<Player> players) {
        if (players == null) {
            return;
        }
        players.forEach(this::freeze);
    }

    public void unfreeze(Player player) {
        if (player != null) {
            frozen.remove(player.getUniqueId());
        }
    }

    public void unfreeze(UUID uuid) {
        if (uuid != null) {
            frozen.remove(uuid);
        }
    }

    public void unfreeze(Collection<Player> players) {
        if (players == null) {
            return;
        }
        players.forEach(this::unfreeze);
    }

    public boolean isFrozen(UUID uuid) {
        return uuid != null && frozen.contains(uuid);
    }

    public void clear() {
        frozen.clear();
    }
}
