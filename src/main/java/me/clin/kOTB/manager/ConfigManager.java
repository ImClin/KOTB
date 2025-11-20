package me.clin.kOTB.manager;

import me.clin.kOTB.KOTB;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Locale;
import java.util.Optional;
import java.util.logging.Level;

public class ConfigManager {

    private final KOTB plugin;
    private FileConfiguration config;

    public ConfigManager(KOTB plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.config = plugin.getConfig();
        ensureDefaults();
    }

    private void ensureDefaults() {
        if (!config.isSet("positions.a")) {
            config.set("positions.a", "");
        }
        if (!config.isSet("positions.b")) {
            config.set("positions.b", "");
        }
        if (!config.isSet("positions.c")) {
            config.set("positions.c", "");
        }
        if (!config.isSet("ylevel")) {
            config.set("ylevel", 50);
        }
        plugin.saveConfig();
    }

    public void setPositionString(String key, Location location) {
        if (location == null || location.getWorld() == null) {
            throw new IllegalArgumentException("Locatie heeft geen wereld.");
        }
        String value = String.format(Locale.US, "%s,%.3f,%.3f,%.3f,%.3f,%.3f",
                location.getWorld().getName(),
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getYaw(),
                location.getPitch());
        config.set("positions." + key, value);
        plugin.saveConfig();
    }

    public Optional<Location> getPosition(String key) {
        String value = config.getString("positions." + key);
        if (value == null || value.isEmpty()) {
            return Optional.empty();
        }
        String[] parts = value.split(",");
        if (parts.length != 6) {
            plugin.getLogger().warning("Ongeldige positie in config voor key " + key);
            return Optional.empty();
        }
        World world = Bukkit.getWorld(parts[0]);
        if (world == null) {
            plugin.getLogger().warning("Wereld " + parts[0] + " bestaat niet voor positie " + key);
            return Optional.empty();
        }
        try {
            double x = Double.parseDouble(parts[1]);
            double y = Double.parseDouble(parts[2]);
            double z = Double.parseDouble(parts[3]);
            float yaw = Float.parseFloat(parts[4]);
            float pitch = Float.parseFloat(parts[5]);
            return Optional.of(new Location(world, x, y, z, yaw, pitch));
        } catch (NumberFormatException ex) {
            plugin.getLogger().log(Level.WARNING, "Kon positie niet parsen voor key " + key, ex);
            return Optional.empty();
        }
    }

    public double getYLevel() {
        return config.getDouble("ylevel", 50);
    }

    public void setYLevel(double value) {
        config.set("ylevel", value);
        plugin.saveConfig();
    }
}
