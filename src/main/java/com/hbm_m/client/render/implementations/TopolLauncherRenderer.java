package com.hbm_m.client.render.implementations;

import java.util.List;
import java.util.Map;

import com.hbm_m.blockentity.machines.TopolLaunchPadBlockEntity;
import com.hbm_m.client.render.HbmBerBounds;
import com.hbm_m.client.render.missile.MissileRenderData;
import com.hbm_m.client.render.missile.MissileRenderRegistry;
import com.hbm_m.item.missile.MissileItem;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.platform.RenderHooks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import org.joml.Matrix4f;

/**
 * Renders the Topol-M launcher: stationary chassis, rotating launch canister.
 *
 * <p>Both parts use separate OBJ models and textures due to relative motion. In the model,
 * the canister extends longitudinally toward +Z with its origin at the elevation hinge;
 * elevation is achieved by rotating around the X axis up to -90 degrees.
 *
 * <p>Rendered directly with model UVs rather than block atlas: vehicle texture is 1024x1024
 * and belongs in its own texture sheet.
 */
@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
public class TopolLauncherRenderer implements HbmBerBounds<TopolLaunchPadBlockEntity> {

    private static final String CHASSIS_OBJ = "models/machines/topol_chassis.obj";
    private static final String CANISTER_OBJ = "models/machines/topol_canister.obj";

    /**
     * Die Schiene oben am Behaelter liegt als eigene Gruppe in der Fahrzeugdatei, weil sie die
     * Fahrzeugtextur nutzt. Gezeichnet wird sie aber mit dem Behaelter, sonst bleibt sie beim
     * Aufrichten waagerecht in der Luft haengen.
     */
    private static final String RAIL_GROUP = "topol_rail";

    private static final ResourceLocation TEX_CHASSIS = ResourceLocation.fromNamespaceAndPath(
            RefStrings.MODID, "textures/models/machines/topol_chassis.png");
    private static final ResourceLocation TEX_CANISTER = ResourceLocation.fromNamespaceAndPath(
            RefStrings.MODID, "textures/models/machines/topol_canister.png");

    /** Lateral offset of elevation hinge; chassis is slightly asymmetric. */
    private static final float PIVOT_X = (float) TopolLaunchPadBlockEntity.PIVOT_LATERAL;

    /** Position of missile tail inside tube, measured from elevation hinge along canister axis. */
    private static final float EJECT_START = 0.5F;

    /**
     * Target position of tail at end of ejection: half a block past the muzzle, exactly where
     * the missile entity spawns.
     */
    private static final float EJECT_END =
            (float) TopolLaunchPadBlockEntity.CANISTER_LENGTH + 0.5F;

    public TopolLauncherRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(TopolLaunchPadBlockEntity be, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        Map<String, List<float[]>> chassis = RBMKColumnRenderer.getObj(CHASSIS_OBJ);
        Map<String, List<float[]>> canister = RBMKColumnRenderer.getObj(CANISTER_OBJ);
        if (chassis.isEmpty() && canister.isEmpty()) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.0D, 0.5D);

        Direction facing = Direction.NORTH;
        if (be.getBlockState().hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            facing = be.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        }
        switch (facing) {
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
            default -> { }
        }

        VertexConsumer chassisBuffer = buffer.getBuffer(RenderType.entityCutoutNoCull(TEX_CHASSIS));
        Matrix4f chassisMatrix = poseStack.last().pose();
        chassis.forEach((group, triangles) -> {
            if (!RAIL_GROUP.equals(group)) {
                drawTriangles(chassisBuffer, chassisMatrix, triangles, packedLight);
            }
        });

        poseStack.pushPose();
        // Rotation pivot lies forward of tube end; canister is shifted back accordingly beforehand.
        // This lowers it onto the launch bed during elevation rather than swinging behind vehicle - see HINGE_OFFSET.
        poseStack.translate(PIVOT_X,
                TopolLaunchPadBlockEntity.PIVOT_HEIGHT,
                -TopolLaunchPadBlockEntity.PIVOT_FORWARD + TopolLaunchPadBlockEntity.HINGE_OFFSET);
        float progress = be.getErectorProgress(partialTicks);
        if (progress > 0.0F) {
            poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F * progress));
        }
        poseStack.translate(0.0D, 0.0D, -TopolLaunchPadBlockEntity.HINGE_OFFSET);
        drawMesh(buffer.getBuffer(RenderType.entityCutoutNoCull(TEX_CANISTER)),
                poseStack.last().pose(), canister, packedLight);

        // Die Schiene ist in Fahrzeugkoordinaten modelliert, darum zuerst zurueck auf deren Ursprung.
        poseStack.pushPose();
        poseStack.translate(-TopolLaunchPadBlockEntity.PIVOT_LATERAL,
                -TopolLaunchPadBlockEntity.PIVOT_HEIGHT,
                TopolLaunchPadBlockEntity.PIVOT_FORWARD);
        drawTriangles(buffer.getBuffer(RenderType.entityCutoutNoCull(TEX_CHASSIS)),
                poseStack.last().pose(), chassis.get(RAIL_GROUP), packedLight);
        poseStack.popPose();

        // Ejection: loaded missile travels along canister axis out of the tube. Rendered in canister
        // coordinate system so it follows elevation angle.
        drawEjectingMissile(be, partialTicks, poseStack, buffer, packedLight);

        poseStack.popPose();
        poseStack.popPose();
    }

    /**
     * Renders missile during ejection sequence.
     *
     * <p>Travel distance scales quadratically with time: gas generator acceleration increases
     * speed as ejection progresses. Missile remains inside tube until reaching muzzle at ~25% progress.
     */
    private static void drawEjectingMissile(TopolLaunchPadBlockEntity be, float partialTicks,
                                            PoseStack poseStack, MultiBufferSource buffer, int light) {
        float progress = be.getEjectProgress(partialTicks);
        if (progress < 0.0F) {
            return;
        }

        ItemStack missileStack = be.getMissilePreviewStack();
        if (missileStack.isEmpty() || !(missileStack.getItem() instanceof MissileItem)) {
            return;
        }
        MissileRenderData data = MissileRenderRegistry.get(missileStack);
        if (data == null) {
            return;
        }

        // Accelerated travel without excessive quadratic snapping at end of motion.
        float eased = (float) Math.pow(progress, 1.4D);
        float offset = Mth.lerp(eased, EJECT_START, EJECT_END);

        poseStack.pushPose();
        // Offset to tube centerline: canister origin lies at its bottom edge.
        poseStack.translate(0.0D, TopolLaunchPadBlockEntity.CANISTER_RADIUS, offset);
        // Missile model stands upright on Y axis; in canister space axis points to +Z.
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        data.render(poseStack, light, be.getBlockPos(), buffer, be);
        poseStack.popPose();
    }

    /**
     * Like {@code RBMKColumnRenderer.renderObjGroup}, but without atlas sprite: model UVs pass
     * through unchanged, with only V inverted.
     */
    private static void drawMesh(VertexConsumer vc, Matrix4f matrix,
                                 Map<String, List<float[]>> obj, int light) {
        for (List<float[]> triangles : obj.values()) {
            drawTriangles(vc, matrix, triangles, light);
        }
    }

    private static void drawTriangles(VertexConsumer vc, Matrix4f matrix, List<float[]> triangles, int light) {
        if (triangles == null) {
            return;
        }
        for (float[] tri : triangles) {
            for (int pass = 0; pass < 4; pass++) {   // vierter Durchlauf doppelt den dritten
                int base = Math.min(pass, 2) * 8;
                RenderHooks.vertexFull(vc, matrix,
                        tri[base], tri[base + 1], tri[base + 2],
                        255, 255, 255, 255,
                        tri[base + 3], 1.0F - tri[base + 4],
                        OverlayTexture.NO_OVERLAY, light,
                        tri[base + 5], tri[base + 6], tri[base + 7]);
            }
        }
    }
}
