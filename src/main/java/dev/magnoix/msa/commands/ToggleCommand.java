package dev.magnoix.msa.commands;

import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.magnoix.msa.databases.PluginConfig;
import io.papermc.paper.command.brigadier.CommandSourceStack;

import java.io.File;
import java.util.List;

public class ToggleCommand {
    private final File configFile;
    private final PluginConfig config;
    private List<String> toggleableEnchants;

    public ToggleCommand(PluginConfig config) {
        this.configFile = new File("config.yml");
        this.config = config;
        this.toggleableEnchants = config.getStringList("toggleable-enchants");
    }
    // TODO: Implement data-driven toggleable enchants, with the list of toggleables in a yml config file.
    public LiteralCommandNode<CommandSourceStack> create() {
        return null;
    }

    // Enchant-Specific Helper Methods
    public List<String> getToggleableEnchants() {
        return ;
    }
    public void setToggleableEnchants(List<String> enchants) {
        config.setValues("toggleable-enchants", enchants);
    }
    public void addToggleableEnchant(String enchant) {
        config.addValue("toggleable-enchants", enchant);
    }
}
