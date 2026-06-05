/*
 * This file is part of Velthoric.
 * Licensed under LGPL 3.0.
 */
package net.xmx.velthoric.event.mixin.impl.server;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.xmx.velthoric.event.api.VxLevelEvent;
import net.xmx.velthoric.event.api.VxServerLifecycleEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to hook into the primary logical server class to handle logical server lifecycles and level registrations.
 *
 * @author xI-Mx-Ix
 */
@Mixin(MinecraftServer.class)
public class MixinMinecraftServer {

    /**
     * Fires server starting event when the server execution begins.
     *
     * @param ci callback info
     */
    @Inject(method = "runServer", at = @At("HEAD"))
    private void velthoric$onRunServerStarting(CallbackInfo ci) {
        VxServerLifecycleEvent.Starting.EVENT.invoker().onServerStarting(
                new VxServerLifecycleEvent.Starting((MinecraftServer) (Object) this)
        );
    }

    /**
     * Fires level load events for each server level upon creation.
     *
     * @param ci callback info
     */
    @Inject(method = "createLevels", at = @At("RETURN"))
    private void velthoric$onCreateLevels(CallbackInfo ci) {
        MinecraftServer server = (MinecraftServer) (Object) this;
        for (ServerLevel level : server.getAllLevels()) {
            if (level != null) {
                VxLevelEvent.Load.EVENT.invoker().onLevelLoad(new VxLevelEvent.Load(level));
            }
        }
    }

    /**
     * Fires server stopping event when the shutdown sequence begins.
     *
     * @param ci callback info
     */
    @Inject(method = "stopServer", at = @At("HEAD"))
    private void velthoric$onStopServerStarting(CallbackInfo ci) {
        VxServerLifecycleEvent.Stopping.EVENT.invoker().onServerStopping(
                new VxServerLifecycleEvent.Stopping((MinecraftServer) (Object) this)
        );
    }

    /**
     * Fires level unload events for each server level during server shutdown.
     *
     * @param ci callback info
     */
    @Inject(method = "stopServer", at = @At("RETURN"))
    private void velthoric$onStopServerFinished(CallbackInfo ci) {
        MinecraftServer server = (MinecraftServer) (Object) this;
        for (ServerLevel level : server.getAllLevels()) {
            if (level != null) {
                VxLevelEvent.Unload.EVENT.invoker().onLevelUnload(new VxLevelEvent.Unload(level));
            }
        }
    }
}