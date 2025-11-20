package me.clin.kOTB.manager;

import org.bukkit.Location;

import java.util.Optional;

public class PositionManager {

    public enum ArenaPosition {
        A("a"),
        B("b"),
        C("c");

        private final String configKey;

        ArenaPosition(String configKey) {
            this.configKey = configKey;
        }

        public String getConfigKey() {
            return configKey;
        }
    }

    private final ConfigManager configManager;

    public PositionManager(ConfigManager configManager) {
        this.configManager = configManager;
    }

    public void setPosition(ArenaPosition position, Location location) {
        configManager.setPositionString(position.getConfigKey(), location);
    }

    public Optional<Location> getPosition(ArenaPosition position) {
        return configManager.getPosition(position.getConfigKey());
    }

    public boolean hasPosition(ArenaPosition position) {
        return getPosition(position).isPresent();
    }

    public boolean hasBothPositions() {
        return hasPosition(ArenaPosition.A) && hasPosition(ArenaPosition.B);
    }

    public boolean hasSpectatorPosition() {
        return hasPosition(ArenaPosition.C);
    }
}
