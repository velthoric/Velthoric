/*
 * This file is part of Velthoric.
 * Licensed under LGPL 3.0.
 */
package net.xmx.velthoric.mixin.impl.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.phys.AABB;
import net.xmx.velthoric.core.entity.interaction.VxEntityBridgeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin for the CollisionGetter interface to integrate Velthoric physics bodies
 * into the standard Minecraft collision check logic.
 *
 * @author xI-Mx-Ix
 */
@Mixin(CollisionGetter.class)
public interface MixinCollisionGetter {

    /**
     * Injects into the noCollision method to verify if a specific area is truly free.
     * Even if no Minecraft blocks or entities are present, a Velthoric physics body might occupy the space.
     *
     * @param entity The entity checking for collision.
     * @param collisionBox The bounding box to check.
     * @param cir The callback returnable containing the boolean result (true if no collision).
     */
    @Inject(method = "noCollision(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;)Z", at = @At("RETURN"), cancellable = true)
    private void velthoric_noCollision(Entity entity, AABB collisionBox, CallbackInfoReturnable<Boolean> cir) {
        // Only perform additional checks if Minecraft already thinks there is no collision
        if (cir.getReturnValue() && entity != null) {
            // Check against Velthoric physics bodies via the bridge manager
            if (VxEntityBridgeManager.isColliding(entity, collisionBox)) {
                // If a physics body is hit, the space is not free
                cir.setReturnValue(false);
            }
        }
    }
}