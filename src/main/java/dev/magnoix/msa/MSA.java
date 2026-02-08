package dev.magnoix.msa;

import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.magnoix.msa.commands.*;
import dev.magnoix.msa.databases.PluginDatabase;
import dev.magnoix.msa.databases.StatisticsManager;
import dev.magnoix.msa.events.MiscEvents;
import dev.magnoix.msa.events.PlayerEvents;
import dev.magnoix.msa.messages.Msg;
import dev.magnoix.msa.utils.StartupUtils;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

import java.sql.SQLException;

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

    private final String permissionPrefix = "msa";

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
        getServer().getPluginManager().registerEvents(new PlayerEvents(statisticsManager, pluginDatabase.getTitleManager(), getPlugin(MSA.class)), this);

        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
            StatisticCommand statisticCommand = new StatisticCommand(permissionPrefix, statisticsManager);
            LiteralCommandNode<CommandSourceStack> statisticNode = statisticCommand.create();
            LeaderboardCommand leaderboardCommand = new LeaderboardCommand(statisticsManager);
            LiteralCommandNode<CommandSourceStack> leaderboardNode = leaderboardCommand.create();
            TitleCommand titleCommand = new TitleCommand();
            LiteralCommandNode<CommandSourceStack> titleNode = titleCommand.create(permissionPrefix, pluginDatabase.getTitleManager());
            ToggleCommand toggleCommand = new ToggleCommand(this);
            LiteralCommandNode<CommandSourceStack> toggleNode = toggleCommand.create();
            ConversionCommand conversionCommand = new ConversionCommand(statisticsManager, this);
            LiteralCommandNode<CommandSourceStack> conversionNode = conversionCommand.create();
            StatisticAliases statsAliases = new StatisticAliases(statisticsManager);

            commands.registrar().register(toggleNode);
            commands.registrar().register(conversionNode);
            commands.registrar().register(new SpawnCommand().create(this));
            StartupUtils.registerCommandWithAliases(commands, statisticNode, "statistic", "stat", "st");
            StartupUtils.registerCommandWithAliases(commands, leaderboardNode, "lb", "top");
            StartupUtils.registerCommandWithAliases(commands, titleNode, "tt", "ranks", "labels");
            StartupUtils.registerCommandNodes(commands, statsAliases.getAliases(true));
        });
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        try {
            pluginDatabase.closeConnection();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public BukkitScheduler getScheduler() { return scheduler; }
}
