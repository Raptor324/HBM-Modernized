package com.hbm_m.client.render.implementations;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.machines.MachineArcFurnaceBlock;
import com.hbm_m.blockentity.machines.MachineArcFurnaceBlockEntity;
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

/**
 * BER fuer den Arc Furnace - schaltet die Elektroden zwischen "kalt" (Idle) und "heiss" (waehrend
 * der Verarbeitung, {@link MachineArcFurnaceBlockEntity#isActive()}) um, analog der Hot/Cold-Gruppen
 * aus dem Original-OBJ (siehe {@code arc_furnace.obj}: Electrode1-3 vs. Electrode1-3Hot + ContentsHot).
 * Keine Rotation/Interpolation noetig - reiner Zustandswechsel, kein kontinuierliches Drehen.
 * Die statischen Teile (Furnace/Lid/Ring1-3/Cable1-3) kommen weiterhin ueber das normale gebackene
 * Blockmodell (siehe {@code arc_furnace.json}, dort sind alle Elektroden-Gruppen ausgeblendet).
 */

//? if forge {
@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
//?} elif fabric {
/*@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
*///?} elif neoforge {
/*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
*///?}
public class MachineArcFurnaceRenderer implements com.hbm_m.client.render.HbmBerBounds<MachineArcFurnaceBlockEntity> {

    private static final ResourceLocation COLD_MODEL_ID = id("arc_furnace_electrodes_cold");
    private static final ResourceLocation HOT_MODEL_ID = id("arc_furnace_electrodes_hot");

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/machines/" + path);
    }

    public MachineArcFurnaceRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public boolean shouldRenderOffScreen(MachineArcFurnaceBlockEntity blockEntity) {
        return true;
    }

    @Override
    public void render(MachineArcFurnaceBlockEntity be, float partialTick, PoseStack poseStack,
                        MultiBufferSource bufferSource, int packedLight, int packedOverlay) {

        ResourceLocation modelId = be.isActive() ? HOT_MODEL_ID : COLD_MODEL_ID;
        String cacheKey = be.isActive() ? "arc_furnace_electrodes_hot" : "arc_furnace_electrodes_cold";
        SingleMeshVboRenderer renderer = getRenderer(cacheKey, modelId);
        if (renderer == null) return;

        Direction facing = be.getBlockState().getValue(MachineArcFurnaceBlock.FACING);
        float facingYRot = facingYRotDegrees(facing);

        poseStack.pushPose();
        poseStack.translate(0.5, 0.0, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(facingYRot));
        poseStack.translate(-0.5, 0.0, -0.5);

        renderer.render(poseStack, packedLight, be.getBlockPos(), be, bufferSource);

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
