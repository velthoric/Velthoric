/*
 * This file is part of Velthoric.
 * Licensed under LGPL 3.0.
 */
package net.xmx.velthoric.mixin.impl.physicsgun;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.
        //$ if >=26.1 'AvatarRenderer;' else 'PlayerRenderer;'
        PlayerRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.model.PlayerModel;
import net.xmx.velthoric.item.physicsgun.VxPhysicsGunItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin class to intercept the arm pose calculation for the physics gun.
 * <p>
 * This mixin is used to change the arm pose to CROSSBOW_HOLD when the player is holding a physics gun.
 * </p>
 *
 * @author xI-Mx-Ix
 */
@Mixin(
        //$ if >=26.1 'AvatarRenderer.class' else 'PlayerRenderer.class'
        PlayerRenderer.class
)
public class PlayerRendererMixin_PhysicsGunArmPose {

    @Inject(
        method = "getArmPose(Lnet/minecraft/client/player/AbstractClientPlayer;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/client/model/PlayerModel$ArmPose;",
        at = @At("RETURN"),
        cancellable = true
    )
    private static void onGetArmPose(AbstractClientPlayer pPlayer, InteractionHand pHand, CallbackInfoReturnable<PlayerModel.ArmPose> cir) {
        ItemStack itemstack = pPlayer.getItemInHand(pHand);
        if (itemstack.getItem() instanceof VxPhysicsGunItem) {
            cir.setReturnValue(PlayerModel.ArmPose.CROSSBOW_HOLD);
        }
    }
}
