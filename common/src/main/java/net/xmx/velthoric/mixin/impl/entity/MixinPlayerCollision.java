/*
 * This file is part of Velthoric.
 * Licensed under LGPL 3.0.
 */
package net.xmx.velthoric.mixin.impl.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import net.xmx.velthoric.core.entity.interaction.VxEntityCollisionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Specific collision mixin for Player entities to override edge-stopping logic
 * when sneaking on physics bodies.
 *
 * @author xI-Mx-Ix
 */
@Mixin(Player.class)
public abstract class MixinPlayerCollision {

    /**
     * Injected logic to prevent players from sneaking off the edge of physics bodies.
     * Note: Players have a separate implementation of this method than generic entities.
     *
     * @param movement Proposed movement vector.
     * @param moverType The mover type defining the trigger source.
     * @param cir Callback returnable for the adjusted movement vector.
     */
    @Inject(method = "maybeBackOffFromEdge", at = @At("HEAD"), cancellable = true)
    private void velthoric_customBackOff(Vec3 movement, MoverType moverType, CallbackInfoReturnable<Vec3> cir) {
        Entity self = (Entity) (Object) this;

        // Checks if the player is actively sneaking, moving downwards, and resting on a custom physics platform.
        if (self.isShiftKeyDown() && movement.y <= 0.0D && VxEntityCollisionManager.isStandingOnPlatform(self)) {
            Vec3 adjusted = VxEntityCollisionManager.handleSneakBackOff(self, movement);
            cir.setReturnValue(adjusted);
        }
    }
}