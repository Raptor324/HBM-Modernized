package com.hbm_m.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.hbm_m.powerarmor.ModArmorFSB;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/**
 * Original {@code ModEventHandlerRenderer.onRenderPlayerPre} (IArmorDisableModel): Ruestungen koennen Teile
 * des Spielermodells ausblenden ({@code ArmorFSB.hides}), z. B. die Hutschicht unter Helmen.
 */
@Mixin(PlayerRenderer.class)
public abstract class MixinPlayerRendererHide {

    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD};

    @Inject(method = "setModelProperties", at = @At("TAIL"))
    private void hbm_m$hideParts(AbstractClientPlayer player, CallbackInfo ci) {
        PlayerModel<AbstractClientPlayer> model = ((PlayerRenderer) (Object) this).getModel();
        if (player.isSpectator()) return;

        for (EquipmentSlot slot : SLOTS) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.isEmpty() || !(stack.getItem() instanceof ModArmorFSB disable)) continue;

            if (disable.disablesPart(player, stack, "head")) model.head.visible = false;
            if (disable.disablesPart(player, stack, "hat")) model.hat.visible = false;
            if (disable.disablesPart(player, stack, "body")) { model.body.visible = false; model.jacket.visible = false; }
            if (disable.disablesPart(player, stack, "left_arm")) { model.leftArm.visible = false; model.leftSleeve.visible = false; }
            if (disable.disablesPart(player, stack, "right_arm")) { model.rightArm.visible = false; model.rightSleeve.visible = false; }
            if (disable.disablesPart(player, stack, "left_leg")) { model.leftLeg.visible = false; model.leftPants.visible = false; }
            if (disable.disablesPart(player, stack, "right_leg")) { model.rightLeg.visible = false; model.rightPants.visible = false; }
        }
    }
}
