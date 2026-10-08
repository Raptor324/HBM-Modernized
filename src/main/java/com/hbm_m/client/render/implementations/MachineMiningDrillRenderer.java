package com.hbm_m.client.render.implementations;

import com.hbm_m.block.machines.DummyableMachineBlock;
import com.hbm_m.blockentity.machines.MachineMiningDrillBlockEntity;
import com.hbm_m.client.render.SimpleObjModel;
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
 * 1:1 {@code RenderExcavator}: Geruest, die zwei gegenlaeufigen Brecherwalzen, der drehende Bohrkopf mit dem Schaft
 * bis zur Ausfahrtiefe und - solange die Rutsche arbeitet - der fallende Steinstrom (Kies, wenn der Brecher laeuft).
 */
public class MachineMiningDrillRenderer implements com.hbm_m.client.render.HbmBerBounds<MachineMiningDrillBlockEntity> {

    public static final SimpleObjModel MODEL = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/mining_drill.obj"));
    public static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/mining_drill.png");
    private static final ResourceLocation COBBLE = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/chute_cobblestone.png");
    private static final ResourceLocation GRAVEL = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/chute_gravel.png");

    public MachineMiningDrillRenderer(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(MachineMiningDrillBlockEntity drill, float interp, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        ps.pushPose();
        ps.translate(0.5D, 0D, 0.5D);

        // Original: Metadaten 3/5/2/4 -> 0/90/180/270 Grad
        switch (drill.getBlockState().getValue(DummyableMachineBlock.FACING)) {
            case EAST -> ps.mulPose(Axis.YP.rotationDegrees(90));
            case NORTH -> ps.mulPose(Axis.YP.rotationDegrees(180));
            case WEST -> ps.mulPose(Axis.YP.rotationDegrees(270));
            default -> { }
        }

        ps.translate(0, -3, 0);

        VertexConsumer vc = buf.getBuffer(RenderType.entityCutout(TEX));
        MODEL.renderPart("Main", ps, vc, light);

        float crusher = drill.prevCrusherRotation + (drill.crusherRotation - drill.prevCrusherRotation) * interp;
        ps.pushPose();
        ps.translate(0.0F, 2.0F, 2.8125F);
        ps.mulPose(Axis.XP.rotationDegrees(-crusher));
        ps.translate(0.0F, -2.0F, -2.8125F);
        MODEL.renderPart("Crusher1", ps, vc, light);
        ps.popPose();

        ps.pushPose();
        ps.translate(0.0F, 2.0F, 2.1875F);
        ps.mulPose(Axis.XP.rotationDegrees(crusher));
        ps.translate(0.0F, -2.0F, -2.1875F);
        MODEL.renderPart("Crusher2", ps, vc, light);
        ps.popPose();

        ps.pushPose();
        ps.mulPose(Axis.YN.rotationDegrees(drill.prevDrillRotation + (drill.drillRotation - drill.prevDrillRotation) * interp));
        float ext = drill.prevDrillExtension + (drill.drillExtension - drill.prevDrillExtension) * interp;
        ps.translate(0.0F, -ext, 0.0F);
        MODEL.renderPart("Drillbit", ps, vc, light);

        while (ext >= -1.5) {
            MODEL.renderPart("Shaft", ps, vc, light);
            ps.translate(0.0D, 2.0D, 0.0D);
            ext -= 2;
        }
        ps.popPose();

        if (drill.chuteTimer > 0) {
            double speed = 250D;
            float dropU = (float) (-System.currentTimeMillis() % speed / speed);
            float dropL = dropU + 4;
            org.joml.Matrix4f m = ps.last().pose();
            org.joml.Matrix3f n = ps.last().normal();

            VertexConsumer cob = buf.getBuffer(RenderType.entityCutoutNoCull(COBBLE));
            float wx = 0.125F, wz = 0.125F;
            quad(cob, m, n, light, 0, 0, 1, wx, 3, 2.5F + wz, 0, dropU, -wx, 3, 2.5F + wz, 1, dropU, -wx, 2, 2.5F + wz, 1, dropL, wx, 2, 2.5F + wz, 0, dropL);
            quad(cob, m, n, light, 0, 0, -1, -wx, 3, 2.5F - wz, 1, dropU, wx, 3, 2.5F - wz, 0, dropU, wx, 2, 2.5F - wz, 0, dropL, -wx, 2, 2.5F - wz, 1, dropL);
            quad(cob, m, n, light, -1, 0, 0, -wx, 3, 2.5F + wz, 0, dropU, -wx, 3, 2.5F - wz, 1, dropU, -wx, 2, 2.5F - wz, 1, dropL, -wx, 2, 2.5F + wz, 0, dropL);
            quad(cob, m, n, light, 1, 0, 0, wx, 3, 2.5F - wz, 1, dropU, wx, 3, 2.5F + wz, 0, dropU, wx, 2, 2.5F + wz, 0, dropL, wx, 2, 2.5F - wz, 1, dropL);

            boolean smoosh = drill.enableCrusher;
            wx = smoosh ? 0.5F : 0.25F;
            wz = 0.0625F;
            float uU = smoosh ? 4 : 2;
            float uL = 0.5F;
            VertexConsumer fall = buf.getBuffer(RenderType.entityCutoutNoCull(smoosh ? GRAVEL : COBBLE));
            quad(fall, m, n, light, 0, 0, 1, wx, 2, 2.5F + wz, 0, dropU, -wx, 2, 2.5F + wz, uU, dropU, -wx, 1, 2.5F + wz, uU, dropL, wx, 1, 2.5F + wz, 0, dropL);
            quad(fall, m, n, light, 0, 0, -1, -wx, 2, 2.5F - wz, uU, dropU, wx, 2, 2.5F - wz, 0, dropU, wx, 1, 2.5F - wz, 0, dropL, -wx, 1, 2.5F - wz, uU, dropL);
            quad(fall, m, n, light, -1, 0, 0, -wx, 2, 2.5F + wz, 0, dropU, -wx, 2, 2.5F - wz, uL, dropU, -wx, 1, 2.5F - wz, uL, dropL, -wx, 1, 2.5F + wz, 0, dropL);
            quad(fall, m, n, light, 1, 0, 0, wx, 2, 2.5F - wz, uL, dropU, wx, 2, 2.5F + wz, 0, dropU, wx, 1, 2.5F + wz, 0, dropL, wx, 1, 2.5F - wz, uL, dropL);
        }

        ps.popPose();
    }

    private static void quad(VertexConsumer vc, org.joml.Matrix4f m, org.joml.Matrix3f n, int light, float nx, float ny, float nz,
                             float x0, float y0, float z0, float u0, float v0,
                             float x1, float y1, float z1, float u1, float v1,
                             float x2, float y2, float z2, float u2, float v2,
                             float x3, float y3, float z3, float u3, float v3) {
        vc.vertex(m, x0, y0, z0).color(1F, 1F, 1F, 1F).uv(u0, v0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(n, nx, ny, nz).endVertex();
        vc.vertex(m, x1, y1, z1).color(1F, 1F, 1F, 1F).uv(u1, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(n, nx, ny, nz).endVertex();
        vc.vertex(m, x2, y2, z2).color(1F, 1F, 1F, 1F).uv(u2, v2).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(n, nx, ny, nz).endVertex();
        vc.vertex(m, x3, y3, z3).color(1F, 1F, 1F, 1F).uv(u3, v3).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(n, nx, ny, nz).endVertex();
    }

    @Override public boolean shouldRenderOffScreen(MachineMiningDrillBlockEntity te) { return true; }
    @Override public int getViewDistance() { return 256; }
}
