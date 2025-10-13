package dev.magnoix.msa;

import com.mojang.brigadier.tree.LiteralCommandNode;
import de.slikey.effectlib.EffectManager;
import dev.magnoix.msa.data.PlayerStatistics;
import dev.magnoix.msa.events.MiscEvents;
import dev.magnoix.msa.messages.Msg;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.logging.Logger;

public final class MSA extends JavaPlugin {
    private BukkitScheduler scheduler;
    private EffectManager effectManager;

    @Override
    public void onEnable() {
        // Plugin startup logic
        this.scheduler = getServer().getScheduler();
        Msg.init(this.getLogger());

        effectManager = new EffectManager(this);
        getServer().getPluginManager().registerEvents(new MiscEvents(), this);

        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
            LiteralCommandNode<CommandSourceStack> testNode = dev.magnoix.msa.commands.TestCommand.create();
            LiteralCommandNode<CommandSourceStack> particleTestNode = dev.magnoix.msa.ParticleTest.create(effectManager);

            commands.registrar().register(testNode);
            commands.registrar().register(particleTestNode);
        });

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        effectManager.dispose();
    }

    @Override
    public Logger getLogger() {
        return super.getLogger();
    }

    public BukkitScheduler getScheduler() { return scheduler; }
    public EffectManager getEffectManager() { return effectManager; }
}
