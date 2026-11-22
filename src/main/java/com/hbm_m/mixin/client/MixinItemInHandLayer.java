package com.hbm_m.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.hbm_m.client.weapon.GunItemRenderer;
import com.hbm_m.client.weapon.ItemRenderWeaponBase;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

@Mixin(ItemInHandLayer.class)
public abstract class MixinItemInHandLayer<T extends LivingEntity, M extends EntityModel<T> & ArmedModel> extends RenderLayer<T, M> {

    public MixinItemInHandLayer(RenderLayerParent<T, M> renderer) {
        super(renderer);
    }

    @Shadow
    protected abstract void renderArmWithItem(LivingEntity livingEntity, ItemStack itemStack, ItemDisplayContext displayContext, HumanoidArm arm, PoseStack poseStack, MultiBufferSource buffer, int packedLight);

    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V", at = @At("TAIL"))
    private void hbm_m$renderAkimboLeftHand(PoseStack poseStack, MultiBufferSource buffer, int packedLight, T livingEntity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        ItemStack mainStack = livingEntity.getMainHandItem();
        ItemStack offStack = livingEntity.getOffhandItem();
        if (offStack.isEmpty() && !mainStack.isEmpty()) {
            ItemRenderWeaponBase renderer = GunItemRenderer.get(mainStack);
            if (renderer != null && renderer.isAkimbo(livingEntity)) {
                HumanoidArm mainArm = livingEntity.getMainArm();
                HumanoidArm offArm = (mainArm == HumanoidArm.RIGHT) ? HumanoidArm.LEFT : HumanoidArm.RIGHT;
                ItemDisplayContext offCtx = (offArm == HumanoidArm.LEFT) ? ItemDisplayContext.THIRD_PERSON_LEFT_HAND : ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
                this.renderArmWithItem(livingEntity, mainStack, offCtx, offArm, poseStack, buffer, packedLight);
            }
        }
    }
}
