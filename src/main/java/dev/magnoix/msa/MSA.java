package dev.magnoix.msa;

import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.magnoix.msa.events.MiscEvents;
import dev.magnoix.msa.messages.MailManager;
import dev.magnoix.msa.messages.Msg;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public final class MSA extends JavaPlugin {
    private MailManager mailManager;
    @Override
    public void onEnable() {
        // Plugin startup logic
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

    public MailManager getMailManager() {
        return mailManager;
    }
}
