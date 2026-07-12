package com.hbm_m.client.render.implementations;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.decorations.SoyuzLauncherBlock;
import com.hbm_m.block.entity.machines.SoyuzLauncherBlockEntity;
import com.hbm_m.client.model.SoyuzLauncherBakedModel;
import com.hbm_m.client.model.SoyuzRocketBakedModel;
import com.hbm_m.client.render.AbstractPartBasedRenderer;
import com.hbm_m.client.render.LegacyAnimator;
import com.hbm_m.client.render.MeshRenderCache;
import com.hbm_m.client.render.SingleMeshVboRenderer;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

/**
 * BER for the Soyuz launch tower: renders the 6 static OBJ parts (Table,
 * TowerBase, Tower, SupportBase, Support, Legs) via the VBO path (see
 * {@link SoyuzLauncherBakedModel} for why baked-quad world rendering is
 * skipped), plus:
 * <ul>
 *   <li>the mounted rocket ({@link SoyuzRocketBakedModel}'s "Rocket" part)
 *       while {@code hasRocket()} is true</li>
 *   <li>the Tower/Support "arm" pivot animation, matching legacy
 *       {@code SoyuzLauncherPronter.prontLauncher(rot)}'s
 *       {@code glTranslate -> glRotate -> glTranslate} dance</li>
 * </ul>
 */
public class SoyuzLauncherRenderer extends AbstractPartBasedRenderer<SoyuzLauncherBlockEntity, SoyuzLauncherBakedModel> {

    private static final String CACHE_PREFIX = "soyuz_launcher";
    private static final ResourceLocation ROCKET_MODEL_ID =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/deco_soyuz_rocket");

    /** Ticks over which the arms fully open/close, matching legacy {@code timer = 20}. */
    private static final int ARM_ANIM_TICKS = 20;
    private static final double ARM_OPEN_DEGREES = 45.0D;

    public SoyuzLauncherRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    protected SoyuzLauncherBakedModel getModelType(BakedModel rawModel) {
        return rawModel instanceof SoyuzLauncherBakedModel m ? m : null;
    }

    @Override
    protected Direction getFacing(SoyuzLauncherBlockEntity be) {
        return be.getBlockState().getValue(SoyuzLauncherBlock.FACING);
    }

    @Override
    protected void renderParts(SoyuzLauncherBlockEntity be, SoyuzLauncherBakedModel model, LegacyAnimator animator,
                                float partialTick, int packedLight, int packedOverlay,
                                PoseStack poseStack, MultiBufferSource bufferSource) {
        double rot = computeArmAngle(be, partialTick);

        for (String partName : SoyuzLauncherBakedModel.ALL_PARTS) {
            BakedModel part = model.getPart(partName);
            if (part == null) continue;

            SingleMeshVboRenderer renderer = MeshRenderCache.getOrCreateRenderer(CACHE_PREFIX, partName, part);
            if (renderer == null) continue;

            if (partName.equals(SoyuzLauncherBakedModel.TOWER) && rot != 0.0D) {
                renderPivoted(renderer, poseStack, packedLight, be, bufferSource, 0, 5.5, 5.5, rot, true);
            } else if (partName.equals(SoyuzLauncherBakedModel.SUPPORT) && rot != 0.0D) {
                renderPivoted(renderer, poseStack, packedLight, be, bufferSource, 0, 5.5, -6.5, rot, false);
            } else {
                renderer.render(poseStack, packedLight, be.getBlockPos(), be, bufferSource);
            }
        }

        if (be.hasRocket()) {
            renderRocket(be, poseStack, packedLight, bufferSource);
        }
    }

    /** Reproduces legacy {@code glTranslate(px,py,pz) -> glRotate(rot,axisX,0,0) -> glTranslate(-px,-py,-pz)}. */
    private void renderPivoted(SingleMeshVboRenderer renderer, PoseStack poseStack, int packedLight,
                                SoyuzLauncherBlockEntity be, MultiBufferSource bufferSource,
                                double px, double py, double pz, double rot, boolean positiveX) {
        poseStack.pushPose();
        poseStack.translate(px, py, pz);
        poseStack.mulPose((positiveX ? Axis.XP : Axis.XN).rotationDegrees((float) rot));
        poseStack.translate(-px, -py, -pz);
        renderer.render(poseStack, packedLight, be.getBlockPos(), be, bufferSource);
        poseStack.popPose();
    }

    /** 0 = closed/gripping rocket, 45 = fully open. Mirrors legacy {@code RenderSoyuzLauncher}'s {@code rot} calc. */
    private double computeArmAngle(SoyuzLauncherBlockEntity be, float partialTick) {
        double rot = be.hasRocket() ? 0.0D : ARM_OPEN_DEGREES;
        if (be.isStarting() && be.getCountdown() < ARM_ANIM_TICKS) {
            rot = (ARM_ANIM_TICKS - be.getCountdown() + partialTick) * ARM_OPEN_DEGREES / ARM_ANIM_TICKS;
        }
        return rot;
    }

    private void renderRocket(SoyuzLauncherBlockEntity be, PoseStack poseStack, int packedLight, MultiBufferSource bufferSource) {
        BakedModel rocketModel = getRocketModel();
        if (!(rocketModel instanceof SoyuzRocketBakedModel model)) return;

        BakedModel part = model.getPart(SoyuzRocketBakedModel.ROCKET);
        if (part == null) return;

        SingleMeshVboRenderer renderer = MeshRenderCache.getOrCreateRenderer("soyuz_launcher_rocket", "Rocket", part);
        if (renderer == null) return;

        poseStack.pushPose();
        poseStack.translate(0.0, 5.0, 0.0);
        renderer.render(poseStack, packedLight, be.getBlockPos(), be, bufferSource);
        poseStack.popPose();
    }

    @Nullable
    private static BakedModel getRocketModel() {
        var modelManager = Minecraft.getInstance().getModelManager();
        BakedModel model = modelManager.getModel(ROCKET_MODEL_ID);
        return (model == null || model == modelManager.getMissingModel()) ? null : model;
    }
}
