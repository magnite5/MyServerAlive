package dev.magnoix.msa;

import dev.magnoix.msa.events.MiscEvents;
import dev.magnoix.msa.messages.Msg;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public final class MSA extends JavaPlugin {

    @Override
    public void onEnable() {
        // Plugin startup logic
        Msg.init(this.getLogger());
        getServer().getPluginManager().registerEvents(new MiscEvents(), this);

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    @Override
    public Logger getLogger() {
        return super.getLogger();
    }
}
