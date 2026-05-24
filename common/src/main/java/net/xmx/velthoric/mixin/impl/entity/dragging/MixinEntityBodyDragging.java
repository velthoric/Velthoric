/*
 * This file is part of Velthoric.
 * Licensed under LGPL 3.0.
 */
package net.xmx.velthoric.mixin.impl.entity.dragging;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.xmx.velthoric.core.entity.interaction.VxEntityCollisionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to handle tick-based physical "dragging" of entities resting on moving physics bodies.
 * This mixin alters physical game variables at the logic tick rate.
 *
 * @author xI-Mx-Ix
 */
@Mixin(Entity.class)
public abstract class MixinEntityBodyDragging {

    @Shadow public abstract float getYRot();
    @Shadow public abstract void setYRot(float yRot);

    /**
     * Tracks the body ground slot index from the previous logic tick.
     * Used to detect when an entity leaves a body to apply momentum transfer.
     */
    @Unique
    private int velthoric_lastGroundSlot = -1;

    /**
     * Counts consecutive ticks where the entity has lost ground contact with a body.
     * Acts as a grace period to prevent false-positive momentum transfers from
     * brief collision detection gaps during fast body movement.
     */
    @Unique
    private int velthoric_groundLostTicks = 0;

    /**
     * Injects at the end of the base tick to apply the body displacement dragging logic.
     * Keeps all entities (players and mobs) visually and logically synchronized with the body.
     *
     * @param ci Callback info instance.
     */
    @Inject(method = "baseTick", at = @At("TAIL"))
    private void velthoric_dragEntityLogic(CallbackInfo ci) {
        Entity self = (Entity) (Object) this;

        // Entities that are riding a generic mount should not be manually dragged by the ground
        if (self.isPassenger()) {
            return;
        }

        boolean isClientSide = self.level().isClientSide();

        // On the server side, do not apply dragging to players.
        // Server-side players manage their own movements and send packets.
        if (!isClientSide && self instanceof Player) {
            return;
        }

        int groundIdx = VxEntityCollisionManager.getGroundSlotIdx(self);
        if (groundIdx >= 0) {
            Vec3 displacement = VxEntityCollisionManager.getBodyDisplacement(self, groundIdx);
            float yawDelta = VxEntityCollisionManager.getBodyYawDelta(self, groundIdx);

            // Directly synchronize the entity's position with the body's displacement.
            // Block collisions are handled independently by the entity's own movement tick.
            if (displacement.lengthSqr() > 1.0E-10) {
                self.setPos(
                        self.getX() + displacement.x,
                        self.getY() + displacement.y,
                        self.getZ() + displacement.z
                );
            }

            // On the client, the local player's rotation is updated smoothly per frame inside MixinGameRenderer.
            // All other entities (mobs on client, and all entities on server) receive tick-based rotation updates.
            boolean isLocalPlayer = isClientSide && (self instanceof net.minecraft.client.player.LocalPlayer);

            if (!isLocalPlayer && Math.abs(yawDelta) > 1.0E-5) {
                this.setYRot(this.getYRot() + yawDelta);

                // Add to the previous rotation delta to prevent jittering when linearly interpolated
                self.yRotO += yawDelta;

                // Sync the body and head rotations to prevent visual twisting and snappy realignments
                if (self instanceof LivingEntity living) {
                    living.yBodyRot += yawDelta;
                    living.yBodyRotO += yawDelta;
                    living.yHeadRot += yawDelta;
                    living.yHeadRotO += yawDelta;
                }
            }
            this.velthoric_lastGroundSlot = groundIdx;
            this.velthoric_groundLostTicks = 0;
        } else {
            if (this.velthoric_lastGroundSlot >= 0) {
                this.velthoric_groundLostTicks++;

                // Only transfer momentum after 3 ticks of sustained air-time.
                // Brief collision detection gaps during fast body movement are ignored.
                if (this.velthoric_groundLostTicks > 3) {
                    Vec3 bodyVel = VxEntityCollisionManager.getBodyVelocity(self, this.velthoric_lastGroundSlot);
                    if (bodyVel.lengthSqr() > 1.0E-10) {
                        Vec3 currentDelta = self.getDeltaMovement();
                        self.setDeltaMovement(currentDelta.x + bodyVel.x, currentDelta.y + bodyVel.y, currentDelta.z + bodyVel.z);
                    }
                    this.velthoric_lastGroundSlot = -1;
                    this.velthoric_groundLostTicks = 0;
                }
            }
        }
    }
}