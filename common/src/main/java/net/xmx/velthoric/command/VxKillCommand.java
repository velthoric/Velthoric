/*
 * This file is part of Velthoric.
 * Licensed under LGPL 3.0.
 */
package net.xmx.velthoric.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.xmx.velthoric.command.argument.VxBodyArgument;
import net.xmx.velthoric.core.behavior.impl.VxKillBehavior;
import net.xmx.velthoric.core.body.VxBody;
import net.xmx.velthoric.core.body.VxRemovalReason;

import java.util.List;

/**
 * A command to remove physics bodies from the world.
 *
 * @author xI-Mx-Ix
 */
public class VxKillCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("vxkill")
                .requires(
                        //? if >= 26.1 {
                        /*Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)
                        *///? } else {
                        source -> source.hasPermission(2)
                        //? }
                )
                .then(Commands.argument("selector", VxBodyArgument.instance())
                        .executes(context -> {
                            List<VxBody> bodiesToRemove = VxBodyArgument.getBodies(context, "selector");
                            int removedCount = 0;

                            for (VxBody body : bodiesToRemove) {
                                if (!body.getPhysicsWorld().getBodyManager().getBehaviorManager().hasBehavior(body, VxKillBehavior.ID)) {
                                    continue;
                                }
                                body.getPhysicsWorld().getBodyManager().removeBody(body.getPhysicsId(), VxRemovalReason.DISCARD);
                                removedCount++;
                            }

                            int finalRemovedCount = removedCount;
                            context.getSource().sendSuccess(() -> Component.literal("Removed " + finalRemovedCount + " physics bodies."), true);
                            return removedCount;
                        })
                )
        );
    }
}