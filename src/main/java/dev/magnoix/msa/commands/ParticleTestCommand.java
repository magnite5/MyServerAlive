package dev.magnoix.msa.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.effect.LineEffect;
import dev.magnoix.msa.messages.Msg;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

import java.util.logging.Level;

public class ParticleTestCommand {
    public static LiteralCommandNode<CommandSourceStack> create(EffectManager effectManager) {
        return Commands.literal("particletest")
            .then(Commands.argument("x", IntegerArgumentType.integer())
                .then(Commands.argument("y", IntegerArgumentType.integer())
                    .then(Commands.argument("z", IntegerArgumentType.integer())
                        .then(Commands.argument("r", IntegerArgumentType.integer())
                            .then(Commands.argument("g", IntegerArgumentType.integer())
                                .then(Commands.argument("b", IntegerArgumentType.integer())
                                    .executes(ctx -> {
                                        if (!(ctx.getSource().getExecutor() instanceof Player player)) {
                                            Msg.log(Level.SEVERE, "Tried to run particletest command, but the executor was not a player.");
                                            return 0;
                                        }
                                        int x = ctx.getArgument("x", int.class);
                                        int y = ctx.getArgument("y", int.class);
                                        int z = ctx.getArgument("z", int.class);

                                        int r = Math.clamp(ctx.getArgument("r", int.class), 0, 255);
                                        int g = Math.clamp(ctx.getArgument("g", int.class), 0, 255);
                                        int b = Math.clamp(ctx.getArgument("b", int.class), 0, 255);

                                        Location endLocation = new Location(player.getWorld(), x, y, z);

                                        LineEffect line = new LineEffect(effectManager);
                                        line.setEntity(player);
                                        line.setTarget(endLocation);
                                        line.particle = Particle.DUST;
                                        line.color = Color.fromRGB(r, g, b);
                                        line.particleSize = 0.5f;
                                        line.particles = 50;
                                        line.iterations = 100;
                                        line.period = 1; // ticks
                                        line.start();

                                        return 1;
                                    })))))))
            .build();
    }
}
