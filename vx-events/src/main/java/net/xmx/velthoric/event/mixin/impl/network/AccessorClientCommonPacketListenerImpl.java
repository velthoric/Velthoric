/*
 * This file is part of Velthoric.
 * Licensed under LGPL 3.0.
 */
package net.xmx.velthoric.event.mixin.impl.network;

/*? if >=1.21.1 {*/
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
/*? } else {*/
/*import net.minecraft.client.multiplayer.ClientPacketListener;
*//*? }*/
import net.minecraft.network.Connection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Accessor mixin to expose the protected connection field on packet listeners.
 *
 * @author xI-Mx-Ix
 */
/*? if >=1.21.1 {*/
@Mixin(ClientCommonPacketListenerImpl.class)
/*? } else {*/
/*@Mixin(ClientPacketListener.class)
*//*? }*/
public interface AccessorClientCommonPacketListenerImpl {

    /**
     * Retrieves the network connection from the packet listener.
     *
     * @return the active network connection
     */
    @Accessor("connection")
    Connection velthoric$getConnection();
}