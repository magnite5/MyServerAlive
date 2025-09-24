package dev.magnoix.msa;

import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.magnoix.msa.events.MiscEvents;
import dev.magnoix.msa.messages.MailManager;
import dev.magnoix.msa.messages.Msg;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.logging.Logger;

public final class MSA extends JavaPlugin {
    private MailManager mailManager;
    private BukkitScheduler scheduler;
    @Override
    public void onEnable() {
        // Plugin startup logic
        this.scheduler = getServer().getScheduler();
        this.mailManager = new MailManager(getDataFolder());

        Msg.init(this.mailManager, this.getLogger());
        getServer().getPluginManager().registerEvents(new MiscEvents(), this);

        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
            LiteralCommandNode<CommandSourceStack> messageNode = dev.magnoix.msa.commands.MessageCommand.create();
            LiteralCommandNode<CommandSourceStack> testNode = dev.magnoix.msa.commands.TestCommand.create();

            commands.registrar().register(messageNode);
            commands.registrar().register(testNode);
        });

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    @Override
    public Logger getLogger() {
        return super.getLogger();
    }
    public BukkitScheduler getScheduler() { return scheduler; }
    public MailManager getMailManager() {
        return mailManager;
    }
}
