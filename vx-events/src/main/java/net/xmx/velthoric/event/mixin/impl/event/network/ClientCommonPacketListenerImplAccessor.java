/*
 * This file is part of Velthoric.
 * Licensed under LGPL 3.0.
 */
package net.xmx.velthoric.event.mixin.impl.event.network;

/*? if >=1.21.1 {*/
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
/*?} else {*/
/*import net.minecraft.client.multiplayer.ClientPacketListener;
*//*?}*/
import net.minecraft.network.Connection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Accessor mixin to expose the protected 'connection' field.
 * <p>
 * In 1.21.1+, targets {@link ClientCommonPacketListenerImpl} which is the parent
 * class that holds the field. In 1.20.1, targets {@link net.minecraft.client.multiplayer.ClientPacketListener}.
 * </p>
 *
 * @author xI-Mx-Ix
 */
/*? if >=1.21.1 {*/
@Mixin(ClientCommonPacketListenerImpl.class)
/*?} else {*/
/*@Mixin(ClientPacketListener.class)
*//*?}*/
public interface ClientCommonPacketListenerImplAccessor {

    /**
     * Accessor for the 'connection' field.
     *
     * @return The underlying network connection.
     */
    @Accessor("connection")
    Connection velthoric$getConnection();
}