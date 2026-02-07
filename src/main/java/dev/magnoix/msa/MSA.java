package dev.magnoix.msa;

import com.mojang.brigadier.tree.LiteralCommandNode;
import de.slikey.effectlib.EffectManager;
import dev.magnoix.msa.commands.*;
import dev.magnoix.msa.databases.PluginConfig;
import dev.magnoix.msa.databases.PluginDatabase;
import dev.magnoix.msa.databases.StatisticsManager;
import dev.magnoix.msa.events.MiscEvents;
import dev.magnoix.msa.events.PlayerEvents;
import dev.magnoix.msa.messages.Msg;
import dev.magnoix.msa.utils.StartupUtils;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.logging.Logger;

public final class MSA extends JavaPlugin {

    /*
    TODO:
        - NetWorth GUI (formerly /cv)
        - /nw /kills /deaths shortcut commands
        - /rules command
        - /unequip
        - logging big jumps in stats
        - implement config-based permission prefix
        - PAPI Support
     */

    private PluginDatabase pluginDatabase;
    private BukkitScheduler scheduler;
    private EffectManager effectManager;
    private PluginConfig pluginConfig;

    @Override
    public void onEnable() {
        Msg.init(this);
        this.pluginConfig = new PluginConfig(this.getDataFolder(), "config.yml");
        getLogger().info("(main) Loading config...");

        try {
            if (!getDataFolder().exists()) getDataFolder().mkdirs();
            pluginDatabase = new PluginDatabase(this, pluginConfig, getDataFolder().getAbsolutePath() + "/msa.db");
        } catch (SQLException e) {
            e.printStackTrace();
            getLogger().severe("Failed to connect to database. Disabling plugin. " + e.getMessage());
            Bukkit.getPluginManager().disablePlugin(this);
        }

        this.scheduler = getServer().getScheduler();

        StatisticsManager statisticsManager = pluginDatabase.getStatisticsManager();
        statisticsManager.updateStatisticTypes(pluginConfig);

        effectManager = new EffectManager(this);
        getServer().getPluginManager().registerEvents(new MiscEvents(), this);
        getServer().getPluginManager().registerEvents(new PlayerEvents(statisticsManager, pluginDatabase.getTitleManager(), getPlugin(MSA.class)), this);

        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
            LiteralCommandNode<CommandSourceStack> testNode = dev.magnoix.msa.commands.TestCommand.create();
            LiteralCommandNode<CommandSourceStack> particleTestNode = ParticleTestCommand.create(effectManager);

            StatisticCommand statisticCommand = new StatisticCommand(statisticsManager);
            LiteralCommandNode<CommandSourceStack> statisticNode = statisticCommand.create();
            LeaderboardCommand leaderboardCommand = new LeaderboardCommand(statisticsManager);
            LiteralCommandNode<CommandSourceStack> leaderboardNode = leaderboardCommand.create();
            TitleCommand titleCommand = new TitleCommand();
            LiteralCommandNode<CommandSourceStack> titleNode = titleCommand.create(pluginDatabase.getTitleManager());
            ToggleCommand toggleCommand = new ToggleCommand(this.pluginConfig);
            LiteralCommandNode<CommandSourceStack> toggleNode = toggleCommand.create();
            ConversionCommand conversionCommand = new ConversionCommand(statisticsManager, this);
            LiteralCommandNode<CommandSourceStack> conversionNode = conversionCommand.create();
            StatisticAliases statsAliases = new StatisticAliases(statisticsManager);

            commands.registrar().register(testNode);
            commands.registrar().register(particleTestNode);
            commands.registrar().register(toggleNode);
            commands.registrar().register(conversionNode);
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
        if (effectManager != null) {
            effectManager.dispose();
        }
    }

    @Override
    public @NotNull Logger getLogger() {
        return super.getLogger();
    }
    public @NotNull PluginConfig getPluginConfig() { return pluginConfig; }

    public BukkitScheduler getScheduler() { return scheduler; }
    public EffectManager getEffectManager() { return effectManager; }
}
