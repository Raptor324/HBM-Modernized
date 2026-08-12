package com.hbm_m.client.render.implementations;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.machines.MachineMiningDrillBlock;
import com.hbm_m.blockentity.machines.MachineMiningDrillBlockEntity;
import com.hbm_m.client.render.MeshRenderCache;
import com.hbm_m.client.render.SingleMeshVboRenderer;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.platform.PlatformHooks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * BER fuer den Large Mining Drill - 1:1 nach dem 1.7.10-Original ({@code RenderExcavator}) nachgebaut:
 * "Main" bleibt (weil unbewegt) im statischen Composite-Blockmodell, alles Bewegliche wird hier Frame
 * fuer Frame gezeichnet:
 * <ul>
 *   <li>Crusher1/Crusher2 schwingen gegenlaeufig als Pendel um ihre jeweilige Achse ({@code crusherRotation}).</li>
 *   <li>Drillbit rotiert um die Y-Achse ({@code drillRotation}) und senkt sich um {@code drillExtension}
 *   Bloecke ab; der entstehende Zwischenraum wird mit gestapelten "Shaft"-Segmenten (je 2 Einheiten hoch)
 *   aufgefuellt, exakt wie im Original.</li>
 * </ul>
 * Die Pivot-Punkte des Originals sind in OBJ-Rohkoordinaten angegeben (Ursprung = Blockmitte); da unsere
 * JSON-Modelle den {@code translate(0.5,0,0.5)}-Versatz bereits beim Backen einrechnen, muss hier bei
 * jeder Rotation der gleiche Versatz auf den Pivot addiert werden (siehe Konstanten unten).
 */

//? if forge {
@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
//?} elif fabric {
/*@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
*///?} elif neoforge {
/*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
*///?}
public class MachineMiningDrillRenderer implements BlockEntityRenderer<MachineMiningDrillBlockEntity> {

    private static final ResourceLocation DRILLBIT_MODEL_ID = id("mining_drill_bit");
    private static final ResourceLocation SHAFT_MODEL_ID = id("mining_drill_shaft");
    private static final ResourceLocation CRUSHER1_MODEL_ID = id("mining_drill_crusher1");
    private static final ResourceLocation CRUSHER2_MODEL_ID = id("mining_drill_crusher2");

    // Original-Pivots (OBJ-Raum, Ursprung = Blockmitte) + 0.5 Versatz fuer unseren gebackenen Block-Raum.
    private static final double CRUSHER1_PIVOT_Z = 2.8125 + 0.5;
    private static final double CRUSHER2_PIVOT_Z = 2.1875 + 0.5;
    private static final double CRUSHER_PIVOT_Y = 2.0;

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/machines/" + path);
    }

    public MachineMiningDrillRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(MachineMiningDrillBlockEntity be, float partialTick, PoseStack poseStack,
                        MultiBufferSource bufferSource, int packedLight, int packedOverlay) {

        SingleMeshVboRenderer drillbitRenderer = getRenderer("mining_drill_bit", DRILLBIT_MODEL_ID);
        SingleMeshVboRenderer shaftRenderer = getRenderer("mining_drill_shaft", SHAFT_MODEL_ID);
        SingleMeshVboRenderer crusher1Renderer = getRenderer("mining_drill_crusher1", CRUSHER1_MODEL_ID);
        SingleMeshVboRenderer crusher2Renderer = getRenderer("mining_drill_crusher2", CRUSHER2_MODEL_ID);

        Direction facing = be.getBlockState().getValue(MachineMiningDrillBlock.FACING);
        float facingYRot = facingYRotDegrees(facing);

        float crusher = Mth.lerp(partialTick, be.prevCrusherRotation, be.crusherRotation);
        float drillAngle = Mth.lerp(partialTick, be.prevDrillRotation, be.drillRotation);
        float ext = Mth.lerp(partialTick, be.prevDrillExtension, be.drillExtension);

        poseStack.pushPose();
        poseStack.translate(0.5, 0.0, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(facingYRot));
        poseStack.translate(-0.5, 0.0, -0.5);

        if (crusher1Renderer != null) {
            poseStack.pushPose();
            poseStack.translate(0.5, CRUSHER_PIVOT_Y, CRUSHER1_PIVOT_Z);
            poseStack.mulPose(Axis.XP.rotationDegrees(-crusher));
            poseStack.translate(-0.5, -CRUSHER_PIVOT_Y, -CRUSHER1_PIVOT_Z);
            crusher1Renderer.render(poseStack, packedLight, be.getBlockPos(), be, bufferSource);
            poseStack.popPose();
        }

        if (crusher2Renderer != null) {
            poseStack.pushPose();
            poseStack.translate(0.5, CRUSHER_PIVOT_Y, CRUSHER2_PIVOT_Z);
            poseStack.mulPose(Axis.XP.rotationDegrees(crusher));
            poseStack.translate(-0.5, -CRUSHER_PIVOT_Y, -CRUSHER2_PIVOT_Z);
            crusher2Renderer.render(poseStack, packedLight, be.getBlockPos(), be, bufferSource);
            poseStack.popPose();
        }

        poseStack.pushPose();
        poseStack.translate(0.5, 0.0, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(-drillAngle));
        poseStack.translate(-0.5, 0.0, -0.5);
        poseStack.translate(0.0, -ext, 0.0);

        if (drillbitRenderer != null) {
            drillbitRenderer.render(poseStack, packedLight, be.getBlockPos(), be, bufferSource);
        }

        if (shaftRenderer != null) {
            float e = ext;
            while (e >= -1.5F) {
                shaftRenderer.render(poseStack, packedLight, be.getBlockPos(), be, bufferSource);
                poseStack.translate(0.0, 2.0, 0.0);
                e -= 2F;
            }
        }

        poseStack.popPose();
        poseStack.popPose();
    }

    private static float facingYRotDegrees(Direction facing) {
        return switch (facing) {
            case SOUTH -> 180F;
            case WEST -> 270F;
            case EAST -> 90F;
            default -> 0F; // NORTH
        };
    }

    @Nullable
    private static SingleMeshVboRenderer getRenderer(String cacheKey, ResourceLocation modelId) {
        BakedModel model = getModel(modelId);
        if (model == null) return null;
        return MeshRenderCache.getOrCreateRenderer(cacheKey, model);
    }

    @Nullable
    private static BakedModel getModel(ResourceLocation id) {
        var modelManager = Minecraft.getInstance().getModelManager();
        BakedModel model = PlatformHooks.getModel(modelManager, id);
        return (model == null || model == modelManager.getMissingModel()) ? null : model;
    }
}
