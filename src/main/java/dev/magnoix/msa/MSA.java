package dev.magnoix.msa;

import dev.magnoix.msa.commands.*;
import dev.magnoix.msa.databases.PluginDatabase;
import dev.magnoix.msa.databases.StatisticsManager;
import dev.magnoix.msa.events.MiscEvents;
import dev.magnoix.msa.events.PlayerEvents;
import dev.magnoix.msa.messages.Msg;
import dev.magnoix.msa.utils.CommandUtils;
import dev.magnoix.msa.utils.StartupUtils;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

import java.sql.SQLException;
import java.util.logging.Level;
import java.util.HashMap;
import java.util.List;

public final class MSA extends JavaPlugin {

    /*
    TODO:
        - /rules command
        - /unequip
        - logging big jumps in stats
        - implement config-based permission prefix
        - PAPI Support
     */

    private PluginDatabase pluginDatabase;
    private BukkitScheduler scheduler;

    public static final String permissionPrefix = "msa";

    @Override
    public void onEnable() {
        Msg.init(this);

        try {
            if (!getDataFolder().exists()) getDataFolder().mkdirs();
            pluginDatabase = new PluginDatabase(this, getDataFolder().getAbsolutePath() + "/msa.db");
        } catch (SQLException e) {
            e.printStackTrace();
            getLogger().severe("Failed to connect to database. Disabling plugin. " + e.getMessage());
            Bukkit.getPluginManager().disablePlugin(this);
        }

        this.saveDefaultConfig();
        FileConfiguration config = getConfig();
        config.options().copyDefaults(true);
        saveConfig();

        this.scheduler = getServer().getScheduler();

        StatisticsManager statisticsManager = pluginDatabase.getStatisticsManager();
        statisticsManager.updateStatisticTypes(this);

        getServer().getPluginManager().registerEvents(new MiscEvents(), this);
        getServer().getPluginManager().registerEvents(
            new PlayerEvents(
                statisticsManager,
                pluginDatabase.getTitleManager(),
                getPlugin(MSA.class),
                config.getBoolean("statistics.write-on-quit"),
                config.getBoolean("statistics.write-on-join")),
            this);

        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
            StatisticAliases statsAliases = new StatisticAliases(statisticsManager);

            HashMap<CommandUtils.Command, List<String>> commandAliases = new HashMap<>();
            commandAliases.put(new StatisticCommand(permissionPrefix, statisticsManager), List.of("stats", "stat", "st"));
            commandAliases.put(new LeaderboardCommand(statisticsManager), List.of("lb", "top"));
            commandAliases.put(new TitleCommand(), List.of("tt", "ranks", "labels"));
            commandAliases.put(new ToggleCommand(this), null);
            commandAliases.put(new ConversionCommand(pluginDatabase.getStatisticsManager()), null);

            StartupUtils.registerCommandNodesWithAliases(this, commands, commandAliases);
            StartupUtils.registerCommandNodes(commands, statsAliases.getAliases(true));
        });

        StartupUtils.scheduleStatisticsFlush(this, statisticsManager, config.getBoolean("statistics.log-writes"));
        StartupUtils.scheduleLeaderboardRebuild(this, statisticsManager, config.getBoolean("leaderboards.log-rebuilds"));
    }

    @Override
    public void onDisable() {
        try {
            pluginDatabase.getStatisticsManager().flushCache();
            pluginDatabase.closeConnection();
        } catch (SQLException e) {
            Msg.log(Level.SEVERE, "Error flushing / closing connection: " + e.getMessage());
        }
    }

    public PluginDatabase getPluginDatabase() { return pluginDatabase; }

    public BukkitScheduler getScheduler() { return scheduler; }
}
