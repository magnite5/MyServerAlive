package dev.magnoix.msa;

import com.mojang.brigadier.tree.LiteralCommandNode;
import de.slikey.effectlib.EffectManager;
import dev.magnoix.msa.commands.LeaderboardCommand;
import dev.magnoix.msa.commands.ParticleTestCommand;
import dev.magnoix.msa.commands.StatisticCommand;
import dev.magnoix.msa.commands.TitleCommand;
import dev.magnoix.msa.databases.PluginDatabase;
import dev.magnoix.msa.events.MiscEvents;
import dev.magnoix.msa.events.PlayerEvents;
import dev.magnoix.msa.messages.Msg;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.logging.Logger;

public final class MSA extends JavaPlugin {
    private PluginDatabase pluginDatabase;
    private BukkitScheduler scheduler;
    private EffectManager effectManager;

    @Override
    public void onEnable() {
        try {
            if (!getDataFolder().exists()) getDataFolder().mkdirs();
            pluginDatabase = new PluginDatabase(getDataFolder().getAbsolutePath() + "/msa.db");
        } catch (SQLException e) {
            e.printStackTrace();
            getLogger().severe("Failed to connect to database. Disabling plugin. " + e.getMessage());
            Bukkit.getPluginManager().disablePlugin(this);
        }

        this.scheduler = getServer().getScheduler();
        Msg.init(this);

        effectManager = new EffectManager(this);
        getServer().getPluginManager().registerEvents(new MiscEvents(), this);
        getServer().getPluginManager().registerEvents(new PlayerEvents(pluginDatabase.getStatisticsManager(), pluginDatabase.getTitleManager(), getPlugin(MSA.class)), this);

        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
            LiteralCommandNode<CommandSourceStack> testNode = dev.magnoix.msa.commands.TestCommand.create();
            LiteralCommandNode<CommandSourceStack> particleTestNode = ParticleTestCommand.create(effectManager);

            StatisticCommand statisticCommand = new StatisticCommand(pluginDatabase.getStatisticsManager());
            LiteralCommandNode<CommandSourceStack> statisticNode = statisticCommand.create();
            LeaderboardCommand leaderboardCommand = new LeaderboardCommand(pluginDatabase.getStatisticsManager());
            LiteralCommandNode<CommandSourceStack> leaderboardNode = leaderboardCommand.create();
            TitleCommand titleCommand = new TitleCommand();
            LiteralCommandNode<CommandSourceStack> titleNode = titleCommand.create(pluginDatabase.getTitleManager());

            commands.registrar().register(testNode);
            commands.registrar().register(particleTestNode);
            commands.registrar().register(statisticNode);
            commands.registrar().register(leaderboardNode);
            commands.registrar().register(titleNode);
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

    public BukkitScheduler getScheduler() { return scheduler; }
    public EffectManager getEffectManager() { return effectManager; }
}
