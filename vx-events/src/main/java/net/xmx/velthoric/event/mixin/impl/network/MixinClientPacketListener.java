/*
 * This file is part of Velthoric.
 * Licensed under LGPL 3.0.
 */
package net.xmx.velthoric.event.mixin.impl.network;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.xmx.velthoric.event.api.VxClientPlayerNetworkEvent;
/*? if <1.21.1 {*/
/*import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
*//*? }*/
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to trigger network state changes and player respawn clones inside ClientPacketListener.
 *
 * @author xI-Mx-Ix
 */
@Mixin(ClientPacketListener.class)
public abstract class MixinClientPacketListener {

/*? if <1.21.1 {*/
    /*@Shadow @Final private Minecraft minecraft;
    @Shadow @Final private Connection connection;
*//*? }*/

    /**
     * Captures the physical reference to the older local player before a clone is generated.
     */
    @Unique
    private LocalPlayer capturedOldPlayerForClone;

/*? if >=1.21.1 {*/
    /**
     * Intercepts the login packet handle completion to fire the client LoggingIn event.
     *
     * @param packet the inbound login packet
     * @param ci     standard callback info
     */
    @Inject(method = "handleLogin", at = @At("TAIL"))
    private void velthoric$onHandleLogin(ClientboundLoginPacket packet, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        Connection conn = ((AccessorClientCommonPacketListenerImpl) this).velthoric$getConnection();
        VxClientPlayerNetworkEvent.LoggingIn.EVENT.invoker().onClientLoggingIn(
                new VxClientPlayerNetworkEvent.LoggingIn(mc.gameMode, mc.player, conn)
        );
    }
/*? } else {*/
    /*/^*
     * Intercepts the login packet handle completion to fire the client LoggingIn event.
     *
     * @param packet the inbound login packet
     * @param ci     standard callback info
     ^/
    @Inject(method = "handleLogin", at = @At("TAIL"))
    private void velthoric$onHandleLogin(ClientboundLoginPacket packet, CallbackInfo ci) {
        VxClientPlayerNetworkEvent.LoggingIn.EVENT.invoker().onClientLoggingIn(
                new VxClientPlayerNetworkEvent.LoggingIn(this.minecraft.gameMode, this.minecraft.player, this.connection)
        );
    }
*//*? }*/

/*? if >=1.21.1 {*/
    /**
     * Intercepts incoming respawn packet at the head to capture the previous player instance.
     *
     * @param packet the inbound respawn packet
     * @param ci     standard callback info
     */
    @Inject(method = "handleRespawn", at = @At("HEAD"))
    private void velthoric$onHandleRespawnHead(ClientboundRespawnPacket packet, CallbackInfo ci) {
        this.capturedOldPlayerForClone = Minecraft.getInstance().player;
    }
/*? } else {*/
    /*/^*
     * Intercepts incoming respawn packet at the head to capture the previous player instance.
     *
     * @param packet the inbound respawn packet
     * @param ci     standard callback info
     ^/
    @Inject(method = "handleRespawn", at = @At("HEAD"))
    private void velthoric$onHandleRespawnHead(ClientboundRespawnPacket packet, CallbackInfo ci) {
        this.capturedOldPlayerForClone = this.minecraft.player;
    }
*//*? }*/

/*? if >=1.21.1 {*/
    /**
     * Intercepts the tail of respawn handling to compare players and fire a clone event.
     *
     * @param packet the inbound respawn packet
     * @param ci     standard callback info
     */
    @Inject(method = "handleRespawn", at = @At("TAIL"))
    private void velthoric$onHandleRespawnTail(ClientboundRespawnPacket packet, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer newPlayer = mc.player;
        Connection conn = ((AccessorClientCommonPacketListenerImpl) this).velthoric$getConnection();

        if (this.capturedOldPlayerForClone != null && newPlayer != null && this.capturedOldPlayerForClone != newPlayer) {
            VxClientPlayerNetworkEvent.Clone.EVENT.invoker().onClientPlayerClone(
                    new VxClientPlayerNetworkEvent.Clone(mc.gameMode, this.capturedOldPlayerForClone, newPlayer, conn)
            );
        }

        this.capturedOldPlayerForClone = null;
    }
/*? } else {*/
    /*/^*
     * Intercepts the tail of respawn handling to compare players and fire a clone event.
     *
     * @param packet the inbound respawn packet
     * @param ci     standard callback info
     ^/
    @Inject(method = "handleRespawn", at = @At("TAIL"))
    private void velthoric$onHandleRespawnTail(ClientboundRespawnPacket packet, CallbackInfo ci) {
        LocalPlayer newPlayer = this.minecraft.player;
        Connection conn = this.connection;

        if (this.capturedOldPlayerForClone != null && newPlayer != null && this.capturedOldPlayerForClone != newPlayer) {
            VxClientPlayerNetworkEvent.Clone.EVENT.invoker().onClientPlayerClone(
                    new VxClientPlayerNetworkEvent.Clone(this.minecraft.gameMode, this.capturedOldPlayerForClone, newPlayer, conn)
            );
        }

        this.capturedOldPlayerForClone = null;
    }
*//*? }*/
}