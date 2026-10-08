package com.hbm_m.client.render.implementations;

import com.hbm_m.block.machines.DummyableMachineBlock;
import com.hbm_m.blockentity.machines.MachineCrucibleBlockEntity;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.inventory.material.Mats.MaterialStack;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * 1:1 {@code RenderCrucible}: das Tiegelmodell und - solange etwas darin ist - ein vollhell leuchtender Schmelzspiegel,
 * dessen Hoehe dem Gesamtinhalt beider Stapel folgt.
 */
public class CrucibleRenderer implements com.hbm_m.client.render.HbmBerBounds<MachineCrucibleBlockEntity> {

    public static final SimpleObjModel MODEL = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/crucible.obj"));
    public static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/crucible_heat.png");
    public static final ResourceLocation LAVA = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/lava.png");

    public CrucibleRenderer(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(MachineCrucibleBlockEntity crucible, float interp, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        ps.pushPose();
        ps.translate(0.5D, 0D, 0.5D);

        // Original: Metadaten 3/5/2/4 -> 270/0/90/180 Grad
        switch (crucible.getBlockState().getValue(DummyableMachineBlock.FACING)) {
            case SOUTH -> ps.mulPose(Axis.YP.rotationDegrees(270));
            case NORTH -> ps.mulPose(Axis.YP.rotationDegrees(90));
            case WEST -> ps.mulPose(Axis.YP.rotationDegrees(180));
            default -> { }
        }

        MODEL.renderAll(ps, buf.getBuffer(RenderType.entityCutout(TEX)), light);

        if (!crucible.recipeStack.isEmpty() || !crucible.wasteStack.isEmpty()) {
            int totalCap = MachineCrucibleBlockEntity.recipeZCapacity + MachineCrucibleBlockEntity.wasteZCapacity;
            int totalMass = 0;

            for (MaterialStack stack : crucible.recipeStack) totalMass += stack.amount;
            for (MaterialStack stack : crucible.wasteStack) totalMass += stack.amount;

            float level = (float) (((double) totalMass / (double) totalCap) * 0.875D);
            float h = 0.5F + level;

            VertexConsumer vc = buf.getBuffer(RenderType.entityCutoutNoCull(LAVA));
            org.joml.Matrix4f m = ps.last().pose();
            org.joml.Matrix3f n = ps.last().normal();
            int full = 0xF000F0;
            vc.vertex(m, -1, h, -1).color(1F, 1F, 1F, 1F).uv(0, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(full).normal(n, 0, 1, 0).endVertex();
            vc.vertex(m, -1, h, 1).color(1F, 1F, 1F, 1F).uv(0, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(full).normal(n, 0, 1, 0).endVertex();
            vc.vertex(m, 1, h, 1).color(1F, 1F, 1F, 1F).uv(1, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(full).normal(n, 0, 1, 0).endVertex();
            vc.vertex(m, 1, h, -1).color(1F, 1F, 1F, 1F).uv(1, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(full).normal(n, 0, 1, 0).endVertex();
        }

        ps.popPose();
    }

    @Override public boolean shouldRenderOffScreen(MachineCrucibleBlockEntity te) { return true; }
    @Override public int getViewDistance() { return 256; }
}
