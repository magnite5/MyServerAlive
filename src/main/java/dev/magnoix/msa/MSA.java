package dev.magnoix.msa;

import dev.magnoix.msa.messages.Msg;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public final class MSA extends JavaPlugin {

    @Override
    public void onEnable() {
        // Plugin startup logic
        Msg.init(this.getLogger());

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
