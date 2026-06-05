/*
 * This file is part of Velthoric.
 * Licensed under LGPL 3.0.
 */
package net.xmx.velthoric.event.mixin.impl.network;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.Connection;
import net.xmx.velthoric.event.api.VxClientPlayerNetworkEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to fire client logging out events during world disconnection.
 *
 * @author xI-Mx-Ix
 */
@Mixin(ClientLevel.class)
public class MixinClientLevel {

    /**
     * Intercepts the disconnection process of a client-side level to fire LoggingOut and Disconnect events.
     *
     * @param ci standard callback info
     */
    @Inject(method = "disconnect", at = @At("HEAD"))
    private void velthoric$onDisconnect(CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();

        MultiPlayerGameMode gameMode = minecraft.gameMode;
        LocalPlayer player = minecraft.player;

        Connection connection = minecraft.getConnection() != null ? minecraft.getConnection().getConnection() : null;

        VxClientPlayerNetworkEvent.LoggingOut.EVENT.invoker().onClientLoggingOut(
                new VxClientPlayerNetworkEvent.LoggingOut(gameMode, player, connection)
        );

        VxClientPlayerNetworkEvent.Disconnect.EVENT.invoker().onClientDisconnect(
                new VxClientPlayerNetworkEvent.Disconnect((ClientLevel) (Object) this)
        );
    }
}