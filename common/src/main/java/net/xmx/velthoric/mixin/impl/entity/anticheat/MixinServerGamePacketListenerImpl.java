/*
 * This file is part of Velthoric.
 * Licensed under LGPL 3.0.
 */
package net.xmx.velthoric.mixin.impl.entity.anticheat;

import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.xmx.velthoric.core.entity.VxEntityCollisionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to handle server-side movement validation for players interacting with physics bodies.
 * Aligns the server-side position with the client's packet when in contact with a body,
 * preventing false-positive anti-cheat checks like "moved wrongly".
 *
 * @author xI-Mx-Ix
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class MixinServerGamePacketListenerImpl {

    @Shadow public ServerPlayer player;
    @Shadow private double firstGoodX;
    @Shadow private double firstGoodY;
    @Shadow private double firstGoodZ;
    @Shadow private double lastGoodX;
    @Shadow private double lastGoodY;
    @Shadow private double lastGoodZ;

    /**
     * Intercepts the movement resolution after the player has moved on the server.
     * If the player is in contact with any physics platform, we synchronize the server
     * position to the client's packet target to prevent anti-cheat warnings and rubberbanding.
     */
    @Inject(
        method = "handleMovePlayer",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayer;move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V",
            shift = At.Shift.AFTER
        )
    )
    private void velthoric_alignPlayerOnPhysicsBody(ServerboundMovePlayerPacket packet, CallbackInfo ci) {
        // Only align if the player is actively colliding with or standing on a physics body
        if (VxEntityCollisionManager.isColliding(this.player.level(), this.player.getBoundingBox().inflate(0.1))) {
            double targetX = packet.getX(this.player.getX());
            double targetY = packet.getY(this.player.getY());
            double targetZ = packet.getZ(this.player.getZ());

            this.player.setPos(targetX, targetY, targetZ);

            this.firstGoodX = targetX;
            this.firstGoodY = targetY;
            this.firstGoodZ = targetZ;
            this.lastGoodX = targetX;
            this.lastGoodY = targetY;
            this.lastGoodZ = targetZ;
        }
    }
}