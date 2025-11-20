package me.clin.kOTB.manager;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerStateManager {

    private static class PlayerState {
        private final ItemStack[] contents;
        private final ItemStack[] armor;
        private final ItemStack[] extra;
        private final double health;
        private final int foodLevel;
        private final float saturation;
        private final float exp;
        private final int level;
        private final int totalExp;
        private final Collection<PotionEffect> potionEffects;
        private final int fireTicks;
        private final Location location;

        private PlayerState(ItemStack[] contents,
                            ItemStack[] armor,
                            ItemStack[] extra,
                            double health,
                            int foodLevel,
                            float saturation,
                            float exp,
                            int level,
                            int totalExp,
                            Collection<PotionEffect> potionEffects,
                            int fireTicks,
                            Location location) {
            this.contents = contents;
            this.armor = armor;
            this.extra = extra;
            this.health = health;
            this.foodLevel = foodLevel;
            this.saturation = saturation;
            this.exp = exp;
            this.level = level;
            this.totalExp = totalExp;
            this.potionEffects = potionEffects;
            this.fireTicks = fireTicks;
            this.location = location;
        }
    }

    private final Map<UUID, PlayerState> storedStates = new HashMap<>();

    public PlayerStateManager() {
    }

    public void backupAndReset(Player player) {
        if (player == null) {
            return;
        }
        UUID uuid = player.getUniqueId();
        if (!storedStates.containsKey(uuid)) {
            storedStates.put(uuid, captureState(player));
        }
        resetForMatch(player);
    }

    public void restore(Player player) {
        if (player == null) {
            return;
        }
        restore(player.getUniqueId());
    }

    public void restore(UUID uuid) {
        PlayerState state = storedStates.remove(uuid);
        if (state == null) {
            return;
        }
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) {
            storedStates.put(uuid, state);
            return;
        }
        applyState(player, state);
    }

    public void restoreOnJoin(PlayerJoinEvent event) {
        restore(event.getPlayer());
    }

    public void restoreAll() {
        for (UUID uuid : storedStates.keySet().toArray(new UUID[0])) {
            restore(uuid);
        }
        storedStates.clear();
    }

    private PlayerState captureState(Player player) {
        PlayerInventory inventory = player.getInventory();
        ItemStack[] contents = cloneContents(inventory.getContents());
        ItemStack[] armor = cloneContents(inventory.getArmorContents());
        ItemStack[] extra = cloneContents(inventory.getExtraContents());
        Collection<PotionEffect> effects = player.getActivePotionEffects();
        Location location = player.getLocation().clone();
        return new PlayerState(
                contents,
                armor,
                extra,
                player.getHealth(),
                player.getFoodLevel(),
                player.getSaturation(),
                player.getExp(),
                player.getLevel(),
                player.getTotalExperience(),
                effects,
                player.getFireTicks(),
                location
        );
    }

    private void resetForMatch(Player player) {
        PlayerInventory inventory = player.getInventory();
        inventory.clear();
        inventory.setArmorContents(new ItemStack[inventory.getArmorContents().length]);
        inventory.setExtraContents(new ItemStack[inventory.getExtraContents().length]);
        player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
        player.setFireTicks(0);
        player.setFoodLevel(20);
        player.setSaturation(20);
        player.setExhaustion(0);
        player.setLevel(0);
        player.setExp(0);
        player.setTotalExperience(0);
        player.setFallDistance(0);
        player.setHealth(getMaxHealth(player));
    }

    private void applyState(Player player, PlayerState state) {
        PlayerInventory inventory = player.getInventory();
        inventory.setContents(cloneContents(state.contents));
        inventory.setArmorContents(cloneContents(state.armor));
        inventory.setExtraContents(cloneContents(state.extra));
        player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
        state.potionEffects.forEach(player::addPotionEffect);
        player.setFireTicks(state.fireTicks);
        player.setFoodLevel(state.foodLevel);
        player.setSaturation(state.saturation);
        player.setLevel(state.level);
        player.setExp(state.exp);
        player.setTotalExperience(state.totalExp);
        player.setHealth(Math.min(state.health, getMaxHealth(player)));
        if (state.location != null && state.location.getWorld() != null) {
            player.teleport(state.location);
        }
    }

    private Attribute maxHealthAttribute;

    private double getMaxHealth(Player player) {
        if (maxHealthAttribute == null) {
            maxHealthAttribute = resolveMaxHealthAttribute();
        }
        if (maxHealthAttribute != null) {
            var instance = player.getAttribute(maxHealthAttribute);
            if (instance != null) {
                return instance.getValue();
            }
        }
        return Math.max(20.0, player.getHealth());
    }

    private Attribute resolveMaxHealthAttribute() {
        return Registry.ATTRIBUTE.get(NamespacedKey.minecraft("generic.max_health"));
    }

    private ItemStack[] cloneContents(ItemStack[] original) {
        if (original == null) {
            return null;
        }
        return Arrays.stream(original)
                .map(item -> item == null ? null : item.clone())
                .toArray(ItemStack[]::new);
    }
}
