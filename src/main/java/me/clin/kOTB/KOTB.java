package me.clin.kOTB;

import me.clin.kOTB.command.TournamentCommand;
import me.clin.kOTB.listener.CombatOverrideListener;
import me.clin.kOTB.listener.FreezeListener;
import me.clin.kOTB.listener.MatchResultListener;
import me.clin.kOTB.listener.YLevelListener;
import me.clin.kOTB.manager.ConfigManager;
import me.clin.kOTB.manager.CountdownManager;
import me.clin.kOTB.manager.PlayerFreezeManager;
import me.clin.kOTB.manager.PositionManager;
import me.clin.kOTB.manager.StatsManager;
import me.clin.kOTB.manager.TournamentManager;
import me.clin.kOTB.placeholder.TournamentPlaceholderExpansion;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class KOTB extends JavaPlugin {

    private ConfigManager configManager;
    private PositionManager positionManager;
    private PlayerFreezeManager freezeManager;
    private CountdownManager countdownManager;
    private TournamentManager tournamentManager;
    private StatsManager statsManager;
    private TournamentPlaceholderExpansion placeholderExpansion;

    @Override
    public void onEnable() {
        this.configManager = new ConfigManager(this);
        this.configManager.load();

        this.positionManager = new PositionManager(configManager);
        this.freezeManager = new PlayerFreezeManager();
        this.countdownManager = new CountdownManager(this);
        this.statsManager = new StatsManager(this);
        this.statsManager.load();
        this.tournamentManager = new TournamentManager(this, positionManager, configManager, freezeManager, countdownManager, statsManager);

        TournamentCommand tournamentCommand = new TournamentCommand(tournamentManager, positionManager, configManager);
        PluginCommand command = getCommand("kotb");
        if (command != null) {
            command.setExecutor(tournamentCommand);
            command.setTabCompleter(tournamentCommand);
        } else {
            getLogger().severe("Kon het /kotb commando niet registreren.");
        }

        getServer().getPluginManager().registerEvents(new FreezeListener(freezeManager), this);
        getServer().getPluginManager().registerEvents(new CombatOverrideListener(tournamentManager), this);
        getServer().getPluginManager().registerEvents(new YLevelListener(configManager, tournamentManager), this);
        getServer().getPluginManager().registerEvents(new MatchResultListener(tournamentManager), this);

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            placeholderExpansion = new TournamentPlaceholderExpansion(this, statsManager);
            placeholderExpansion.register();
            getLogger().info("PlaceholderAPI gedetecteerd: KOTB-placeholders geactiveerd.");
        } else {
            getLogger().info("PlaceholderAPI niet gevonden; placeholders zijn uitgeschakeld.");
        }
    }

    @Override
    public void onDisable() {
        if (tournamentManager != null) {
            tournamentManager.shutdown();
        }
        if (placeholderExpansion != null) {
            placeholderExpansion.unregister();
        }
        if (statsManager != null) {
            statsManager.saveSync();
        }
    }

    public TournamentManager getTournamentManager() {
        return tournamentManager;
    }

    public StatsManager getStatsManager() {
        return statsManager;
    }
}
