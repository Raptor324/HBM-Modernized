package com.hbm_m.client.render.implementations;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.machines.MachineMiningDrillBlock;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineMiningDrillBlockEntity;
import com.hbm_m.client.render.LegacyAnimator;
import com.hbm_m.client.render.MeshRenderCache;
import com.hbm_m.client.render.SingleMeshVboRenderer;
import com.hbm_m.client.render.machine.MachineRenderApi;
import com.hbm_m.client.render.machine.MachineRenderers;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.platform.PlatformHooks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import com.hbm_m.platform.LoaderHooks;
import com.mojang.blaze3d.vertex.VertexConsumer;

/**
 * Big mining drill on the {@link MachineRenderers} factory (1:1 port of 1.7.10 RenderExcavator):
 * Crusher1/Crusher2 are counter-phase pendulums, Drillbit spins and lowers.
 * The mine shaft segments ("Shaft") are a variable-length stack drawn by an immediate hook
 * through a per-BE VBO (as in legacy). Geometry comes from injected models (not multipart).
 * Pivots are OBJ coordinates (origin = block center) plus a 0.5 offset compensating the baked shift.
 */
public final class MachineMiningDrillRenderer {

    private static final ResourceLocation DRILLBIT_MODEL_ID = id("mining_drill_bit");
    private static final ResourceLocation SHAFT_MODEL_ID = id("mining_drill_shaft");
    private static final ResourceLocation CRUSHER1_MODEL_ID = id("mining_drill_crusher1");
    private static final ResourceLocation CRUSHER2_MODEL_ID = id("mining_drill_crusher2");

    public static void register() {
        MachineRenderers.machine("miningdrill", ModBlockEntities.MINING_DRILL_BE.get(),
                MachineMiningDrillBlockEntity.class)
            // Статичный корпус бура из multipart-модели; "Shaft" не объявлен —
            // штангу рисует hook (стек shaft-моделей по глубине).
            .part("Main")
            .rotatingPart("Crusher1", -1f, 0f, 0f, 0.5f, 2.0f, 3.3125f, 0f, 0f, 0f,
                    be -> partQuads("drill/crusher1", CRUSHER1_MODEL_ID), be -> "c1",
                    (be, pt) -> (double) Mth.lerp(pt, be.prevCrusherRotation, be.crusherRotation))
            .rotatingPart("Crusher2", 1f, 0f, 0f, 0.5f, 2.0f, 2.6875f, 0f, 0f, 0f,
                    be -> partQuads("drill/crusher2", CRUSHER2_MODEL_ID), be -> "c2",
                    (be, pt) -> (double) Mth.lerp(pt, be.prevCrusherRotation, be.crusherRotation))
            .dynamicPart("Drillbit", MachineMiningDrillRenderer::animateDrillbit,
                    be -> partQuads("drill/bit", DRILLBIT_MODEL_ID), be -> "bit")
            .blockTransform(MachineMiningDrillRenderer::applyBlockTransform)
            .hook(MachineMiningDrillRenderer::renderShaftStack)
            .hook(MachineMiningDrillRenderer::renderCrushChute)
            .register();
    }

    private MachineMiningDrillRenderer() {}

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/machines/" + path);
    }

    private static List<net.minecraft.client.renderer.block.model.BakedQuad> partQuads(
            String cacheKey, ResourceLocation modelId) {
        BakedModel model = getModel(modelId);
        if (model == null) return List.of();
        return MeshRenderCache.getOrCompilePartGeometry(cacheKey, model).solidQuads();
    }

    @Nullable
    private static BakedModel getModel(ResourceLocation rid) {
        var modelManager = Minecraft.getInstance().getModelManager();
        BakedModel model = LoaderHooks.getModel(modelManager, rid);
        return (model == null || model == modelManager.getMissingModel()) ? null : model;
    }

    private static void applyBlockTransform(MachineMiningDrillBlockEntity be, LegacyAnimator animator) {
        Direction facing = be.getBlockState().getValue(MachineMiningDrillBlock.FACING);
        float facingYRot = switch (facing) {
            case SOUTH -> 180F;
            case WEST -> 270F;
            case EAST -> 90F;
            default -> 0F; // NORTH
        };
        animator.translate(0.5, 0.0, 0.5);
        animator.rotate(facingYRot, 0, 1, 0);
        animator.translate(-0.5, 0.0, -0.5);
    }

    private static boolean animateDrillbit(MachineMiningDrillBlockEntity be, float partialTick,
                                           long gameTime, PoseStack pose) {
        applyDrillShaftTransform(be, partialTick, pose);
        return true;
    }

    /** Drill rotation around Y plus lowering by ext (shared by Drillbit and Shaft). */
    private static void applyDrillShaftTransform(MachineMiningDrillBlockEntity be, float partialTick, PoseStack pose) {
        float drillAngle = Mth.lerp(partialTick, be.prevDrillRotation, be.drillRotation);
        float ext = Mth.lerp(partialTick, be.prevDrillExtension, be.drillExtension);
        pose.translate(0.5, 0.0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(-drillAngle));
        pose.translate(-0.5, 0.0, -0.5);
        pose.translate(0.0, -ext, 0.0);
    }

    /**
     * Shaft: 2-block segments fill the gap left by the lowered drill --
     * a variable number of instances, hence an immediate hook through a per-BE VBO.
     */
    private static void renderShaftStack(MachineMiningDrillBlockEntity be, float partialTick,
                                         PoseStack poseStack, MultiBufferSource bufferSource,
                                         int packedLight, int packedOverlay, MachineRenderApi api) {
        SingleMeshVboRenderer shaft = getModel(SHAFT_MODEL_ID) == null ? null
                : MeshRenderCache.getOrCreateRenderer("mining_drill_shaft", getModel(SHAFT_MODEL_ID));
        if (shaft == null) return;

        poseStack.pushPose();
        applyDrillShaftTransform(be, partialTick, poseStack);
        float e = Mth.lerp(partialTick, be.prevDrillExtension, be.drillExtension);
        while (e >= -1.5F) {
            shaft.render(poseStack, packedLight, be.getBlockPos(), be, bufferSource);
            poseStack.translate(0.0, 2.0, 0.0);
            e -= 2F;
        }
        poseStack.popPose();
    }

    // --- Falling crushed material (FFA merge loss restore, 1:1 RenderExcavator) ---

    private static final ResourceLocation CHUTE_COBBLE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/chute_cobblestone.png");
    private static final ResourceLocation CHUTE_GRAVEL =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/chute_gravel.png");

    /**
     * Оригинальный RenderExcavator: пока работает жёлоб, из-под дробилок падает поток камня —
     * бегущая текстура на 4 стенках шахты (гравий, если включена дробилка).
     */
    private static void renderCrushChute(MachineMiningDrillBlockEntity be, float partialTick,
                                         PoseStack ps, MultiBufferSource buf,
                                         int packedLight, int packedOverlay, MachineRenderApi api) {
        if (be.chuteTimer <= 0) return;

        // Базовый трансформ FFA-BER: центр блока + поворот по FACING + подъём модели на 3.
        Direction facing = be.getBlockState().getValue(MachineMiningDrillBlock.FACING);
        float facingYRot = switch (facing) {
            case EAST -> 90F;
            case NORTH -> 180F;
            case WEST -> 270F;
            default -> 0F; // SOUTH
        };
        ps.pushPose();
        ps.translate(0.5D, 0D, 0.5D);
        ps.mulPose(Axis.YP.rotationDegrees(facingYRot));
        ps.translate(0, -3, 0);

        double speed = 250D;
        float dropU = (float) (-System.currentTimeMillis() % speed / speed);
        float dropL = dropU + 4;
        org.joml.Matrix4f m = ps.last().pose();
        org.joml.Matrix3f n = ps.last().normal();

        VertexConsumer cob = buf.getBuffer(RenderType.entityCutoutNoCull(CHUTE_COBBLE));
        float wx = 0.125F, wz = 0.125F;
        quad(cob, m, n, packedLight, 0, 0, 1, wx, 3, 2.5F + wz, 0, dropU, -wx, 3, 2.5F + wz, 1, dropU, -wx, 2, 2.5F + wz, 1, dropL, wx, 2, 2.5F + wz, 0, dropL);
        quad(cob, m, n, packedLight, 0, 0, -1, -wx, 3, 2.5F - wz, 1, dropU, wx, 3, 2.5F - wz, 0, dropU, wx, 2, 2.5F - wz, 0, dropL, -wx, 2, 2.5F - wz, 1, dropL);
        quad(cob, m, n, packedLight, -1, 0, 0, -wx, 3, 2.5F + wz, 0, dropU, -wx, 3, 2.5F - wz, 1, dropU, -wx, 2, 2.5F - wz, 1, dropL, -wx, 2, 2.5F + wz, 0, dropL);
        quad(cob, m, n, packedLight, 1, 0, 0, wx, 3, 2.5F - wz, 1, dropU, wx, 3, 2.5F + wz, 0, dropU, wx, 2, 2.5F + wz, 0, dropL, wx, 2, 2.5F - wz, 1, dropL);

        boolean smoosh = be.enableCrusher;
        wx = smoosh ? 0.5F : 0.25F;
        wz = 0.0625F;
        float uU = smoosh ? 4 : 2;
        float uL = 0.5F;
        VertexConsumer fall = buf.getBuffer(RenderType.entityCutoutNoCull(smoosh ? CHUTE_GRAVEL : CHUTE_COBBLE));
        quad(fall, m, n, packedLight, 0, 0, 1, wx, 2, 2.5F + wz, 0, dropU, -wx, 2, 2.5F + wz, uU, dropU, -wx, 1, 2.5F + wz, uU, dropL, wx, 1, 2.5F + wz, 0, dropL);
        quad(fall, m, n, packedLight, 0, 0, -1, -wx, 2, 2.5F - wz, uU, dropU, wx, 2, 2.5F - wz, 0, dropU, wx, 1, 2.5F - wz, 0, dropL, -wx, 1, 2.5F - wz, uL, dropL);
        quad(fall, m, n, packedLight, -1, 0, 0, -wx, 2, 2.5F + wz, 0, dropU, -wx, 2, 2.5F - wz, uL, dropU, -wx, 1, 2.5F - wz, uL, dropL, -wx, 1, 2.5F + wz, 0, dropL);
        quad(fall, m, n, packedLight, 1, 0, 0, wx, 2, 2.5F - wz, uL, dropU, wx, 2, 2.5F + wz, 0, dropU, wx, 1, 2.5F + wz, 0, dropL, wx, 1, 2.5F - wz, uL, dropL);

        ps.popPose();
    }

    private static void quad(VertexConsumer vc, org.joml.Matrix4f m, org.joml.Matrix3f n, int light,
                             float nx, float ny, float nz,
                             float x0, float y0, float z0, float u0, float v0,
                             float x1, float y1, float z1, float u1, float v1,
                             float x2, float y2, float z2, float u2, float v2,
                             float x3, float y3, float z3, float u3, float v3) {
        //? if < 1.21.1 {
        vc.vertex(m, x0, y0, z0).color(1F, 1F, 1F, 1F).uv(u0, v0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(n, nx, ny, nz).endVertex();
        vc.vertex(m, x1, y1, z1).color(1F, 1F, 1F, 1F).uv(u1, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(n, nx, ny, nz).endVertex();
        vc.vertex(m, x2, y2, z2).color(1F, 1F, 1F, 1F).uv(u2, v2).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(n, nx, ny, nz).endVertex();
        vc.vertex(m, x3, y3, z3).color(1F, 1F, 1F, 1F).uv(u3, v3).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(n, nx, ny, nz).endVertex();
        //?} else {
        /*com.hbm_m.platform.RenderHooks.normal(vc.addVertex(m, x0, y0, z0).setColor(1F, 1F, 1F, 1F).setUv(u0, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light), n, nx, ny, nz);
        com.hbm_m.platform.RenderHooks.normal(vc.addVertex(m, x1, y1, z1).setColor(1F, 1F, 1F, 1F).setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light), n, nx, ny, nz);
        com.hbm_m.platform.RenderHooks.normal(vc.addVertex(m, x2, y2, z2).setColor(1F, 1F, 1F, 1F).setUv(u2, v2).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light), n, nx, ny, nz);
        com.hbm_m.platform.RenderHooks.normal(vc.addVertex(m, x3, y3, z3).setColor(1F, 1F, 1F, 1F).setUv(u3, v3).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light), n, nx, ny, nz);
        *///?}
    }
}
