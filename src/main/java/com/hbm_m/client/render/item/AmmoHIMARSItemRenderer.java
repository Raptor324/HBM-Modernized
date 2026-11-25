package com.hbm_m.client.render.item;

import com.hbm_m.client.weapon.WeaponResources;
import com.hbm_m.item.weapon.ItemAmmoHIMARS;
import com.hbm_m.item.weapon.ItemAmmoHIMARS.HIMARSRocket;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * 1:1 {@code ItemRenderLibrary} ({@code ModItems.ammo_himars}):
 * renders the 3D rocket + launch tube parts from {@code turret_himars.obj} with the per-type
 * projectile texture, spinning in GUI and scaled to block proportions in hand/ground/frame.
 */
public class AmmoHIMARSItemRenderer extends BlockEntityWithoutLevelRenderer {

    private static AmmoHIMARSItemRenderer instance;

    public static AmmoHIMARSItemRenderer instance() {
        if (instance == null) {
            instance = new AmmoHIMARSItemRenderer();
        }
        return instance;
    }

    private AmmoHIMARSItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),
                Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext ctx, PoseStack ps,
                             MultiBufferSource buf, int light, int overlay) {
        if (!(stack.getItem() instanceof ItemAmmoHIMARS ammo)) {
            return;
        }
        int typeIndex = ammo.type;
        if (typeIndex < 0 || typeIndex >= ItemAmmoHIMARS.itemTypes.length) {
            return;
        }
        HIMARSRocket type = ItemAmmoHIMARS.itemTypes[typeIndex];
        if (type == null) {
            return;
        }

        ps.pushPose();
        if (ctx == ItemDisplayContext.GUI) {
            OrigInventoryTransform.apply(ps);
            ps.translate(0, -2, 0);
            float scale = 2.75F;
            ps.scale(scale, scale, scale);
            ps.mulPose(Axis.YP.rotationDegrees((float) (System.currentTimeMillis() % 3600 / 10.0D)));
        } else {
            RBMKColumnItemRenderer.applyDisplay(stack, ctx, ps);
            ps.translate(0.5, 0.25, 0.5);
            float k = 2.75F / 16F;
            ps.scale(k, k, k);
            ps.mulPose(Axis.YP.rotationDegrees(90));
        }

        ps.translate(0, 1.5, 0);
        ps.mulPose(Axis.YP.rotationDegrees(-45));
        ps.mulPose(Axis.XP.rotationDegrees(90));

        VertexConsumer vc = buf.getBuffer(RenderType.entityCutoutNoCull(type.texture));
        if (type.modelType == 0) {
            ps.translate(0.75, 0, 0);
            WeaponResources.turret_himars.renderPart("RocketStandard", ps, vc, light);
            ps.translate(-1.5, 0, 0);
            ps.translate(0, -3.375D, 0);
            WeaponResources.turret_himars.renderPart("TubeStandard", ps, vc, light);
        } else {
            ps.translate(0.75, 0, 0);
            WeaponResources.turret_himars.renderPart("RocketSingle", ps, vc, light);
            ps.translate(-1.5, 0, 0);
            ps.translate(0, -3.375D, 0);
            WeaponResources.turret_himars.renderPart("TubeSingle", ps, vc, light);
        }

        ps.popPose();
    }
}
