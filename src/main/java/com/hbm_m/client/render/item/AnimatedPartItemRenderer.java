package com.hbm_m.client.render.item;

import com.hbm_m.client.render.implementations.SawmillRenderer;
import com.hbm_m.client.render.implementations.StirlingRenderer;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * 1:1 {@code ItemRenderLibrary} gear_large / sawblade: inventory
 * T(0,-7,0) S(6) Ry(-45) Rx(30) T(0,1.375,0) Rz(time) T(0,-1.375,0), followed by T(0,0,-0.875) and the Stirling
 * or sawmill part ("Cog" / "Blade"). Outside inventory, same part without rotation in block view.
 */
public class AnimatedPartItemRenderer extends BlockEntityWithoutLevelRenderer {

    private static AnimatedPartItemRenderer instance;

    public static AnimatedPartItemRenderer instance() {
        if (instance == null) instance = new AnimatedPartItemRenderer();
        return instance;
    }

    private static final ResourceLocation STIRLING = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/block/machine/stirling.png");
    private static final ResourceLocation STIRLING_STEEL = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/block/machine/stirling_steel.png");

    private AnimatedPartItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext ctx, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        boolean saw = id.equals("sawblade");
        ps.pushPose();
        if (ctx == ItemDisplayContext.GUI) {
            OrigInventoryTransform.apply(ps);
            ps.translate(0, -7, 0);
            ps.scale(6F, 6F, 6F);
            ps.mulPose(Axis.YP.rotationDegrees(-45));
            ps.mulPose(Axis.XP.rotationDegrees(30));
            ps.translate(0, 1.375, 0);
            ps.mulPose(Axis.ZP.rotationDegrees((float) (System.currentTimeMillis() % 3600 * (saw ? 0.2F : 0.1F))));
            ps.translate(0, -1.375, 0);
        } else {
            RBMKColumnItemRenderer.applyDisplay(stack, ctx, ps);
            // Block center, part (diameter ~1.4) scaled to block size
            ps.translate(0.5, 0.5, 0.5);
            ps.scale(0.6F, 0.6F, 0.6F);
            ps.mulPose(Axis.YP.rotationDegrees(90));
            ps.translate(0, -1.375, 0);
        }
        ps.translate(0, 0, -0.875);
        if (saw) {
            SawmillRenderer.MODEL.renderPart("Blade", ps, buf.getBuffer(RenderType.entityCutout(SawmillRenderer.TEX)), light);
        } else {
            ResourceLocation tex = id.contains("steel") ? STIRLING_STEEL : STIRLING;
            StirlingRenderer.MODEL.renderPart("Cog", ps, buf.getBuffer(RenderType.entityCutout(tex)), light);
        }
        ps.popPose();
    }
}
