package com.hbm_m.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.hbm_m.client.weapon.GunItemRenderer;
import com.hbm_m.client.weapon.ItemRenderWeaponBase;
import com.hbm_m.item.weapon.sedna.ItemGunBaseNT;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

@Mixin(PlayerModel.class)
public abstract class MixinPlayerModel<T extends LivingEntity> extends HumanoidModel<T> {

    @Shadow public ModelPart leftSleeve;
    @Shadow public ModelPart rightSleeve;

    public MixinPlayerModel(ModelPart root) {
        super(root);
    }

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void hbm_m$poseAkimboZombieArms(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        if (entity.isFallFlying() || entity.isVisuallySwimming()) return;

        ItemStack mainStack = entity.getMainHandItem();
        ItemStack offStack = entity.getOffhandItem();

        ItemRenderWeaponBase mainRenderer = !mainStack.isEmpty() ? GunItemRenderer.get(mainStack) : null;
        boolean mainAkimbo = mainRenderer != null && mainRenderer.isAkimbo(entity);

        float pitch = this.head.xRot;
        float yaw = this.head.yRot;

        if (mainAkimbo) {
            // Zombie pose: right arm straight forward parallel
            this.rightArm.xRot = (float) (-Math.PI / 2.0) + pitch;
            this.rightArm.yRot = yaw;
            this.rightArm.zRot = 0.0F;
            this.rightSleeve.copyFrom(this.rightArm);

            // Zombie pose: left arm straight forward parallel
            if (offStack.isEmpty() || offStack.getItem() instanceof ItemGunBaseNT) {
                this.leftArm.xRot = (float) (-Math.PI / 2.0) + pitch;
                this.leftArm.yRot = yaw;
                this.leftArm.zRot = 0.0F;
                this.leftSleeve.copyFrom(this.leftArm);
            }
        } else if (!mainStack.isEmpty() && mainStack.getItem() instanceof ItemGunBaseNT
                && !offStack.isEmpty() && offStack.getItem() instanceof ItemGunBaseNT) {
            // Dual wielding guns
            this.rightArm.xRot = (float) (-Math.PI / 2.0) + pitch;
            this.rightArm.yRot = yaw;
            this.rightArm.zRot = 0.0F;
            this.leftArm.xRot = (float) (-Math.PI / 2.0) + pitch;
            this.leftArm.yRot = yaw;
            this.leftArm.zRot = 0.0F;
            this.rightSleeve.copyFrom(this.rightArm);
            this.leftSleeve.copyFrom(this.leftArm);
        }
    }
}
