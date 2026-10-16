package com.hbm_m.client.render.implementations;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.machines.TurretBaseBlockEntity;
import com.hbm_m.blockentity.machines.TurretStats;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Generischer BER fuer alle 11 Turret-Varianten (siehe {@link TurretStats}): rotiert die
 * "Carriage"/"Pivot"-Gruppe um die Yaw-Achse und die daran haengende Pitch-Gruppe
 * (Kanone/Body/Laeufe) um ihre Achse, jeweils zur clientseitig interpolierten Zielrichtung
 * aus der BlockEntity.
 * <p>
 * WICHTIG: rendert ausschliesslich ueber {@link MeshRenderCache}/{@link SingleMeshVboRenderer}
 * (das VBO-System, das auch Radar/Crystallizer/etc. nutzen). Ein direkter
 * {@code bufferSource.getBuffer(...).putBulkData(...)}-Ansatz ("Legacy"-Pfad) wurde zuerst probiert,
 * blieb aber mit Embeddium/Sodium unsichtbar - {@code ShaderCompatibilityDetector.useVboGeometry()}
 * liefert in diesem Mod IMMER {@code true}, der Legacy-Pfad ist toter Code und offenbar nicht
 * Embeddium-kompatibel. Barrel-Spin/Recoil ist bewusst NICHT animiert - siehe {@link TurretStats}.
 */

//? if forge {
@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
//?} elif fabric {
/*@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
*///?} elif neoforge {
/*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
*///?}
public class MachineTurretRenderer implements com.hbm_m.client.render.HbmBerBounds<TurretBaseBlockEntity> {

    private static final Map<String, ResourceLocation> MODEL_IDS = new HashMap<>();

    public MachineTurretRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(TurretBaseBlockEntity be, float partialTick, PoseStack poseStack,
                        MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        TurretStats stats = be.getStats();

        float yaw = Mth.rotLerp(partialTick, be.prevYaw, be.yaw);
        float pitch = Mth.rotLerp(partialTick, be.prevPitch, be.pitch);

        poseStack.pushPose();

        // Yaw-Gruppe (Carriage/Pivot) - dreht sich um Y, Modell-Konvention: -yaw - 90 (siehe Original RenderTurretX)
        poseStack.translate(0.5, 0.0, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(-yaw - 90.0F));
        poseStack.translate(-0.5, 0.0, -0.5);
        renderPart(stats.yawPartKey, be, poseStack, bufferSource, packedLight);

        // Pitch-Gruppe - dreht sich um den konfigurierten Drehpunkt/Achse
        double pivotY = stats.pivotY;
        double pivotZ = stats.pivotZ;
        poseStack.translate(0.0, pivotY, pivotZ);
        if (stats.pitchAxis == TurretStats.PitchAxis.X) {
            poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
        } else {
            poseStack.mulPose(Axis.ZP.rotationDegrees(pitch));
        }
        poseStack.translate(0.0, -pivotY, -pivotZ);

        for (String partKey : stats.pitchPartKeys) {
            float recoil = recoilOffsetFor(partKey, be, partialTick);
            float spinAngle = spinAngleFor(partKey, be, partialTick);
            if (recoil != 0.0F || spinAngle != 0.0F) {
                poseStack.pushPose();
                if (recoil != 0.0F) poseStack.translate(0.0, 0.0, recoil);
                if (spinAngle != 0.0F) poseStack.mulPose(Axis.ZP.rotationDegrees(spinAngle));
                renderPart(partKey, be, poseStack, bufferSource, packedLight);
                poseStack.popPose();
            } else {
                renderPart(partKey, be, poseStack, bufferSource, packedLight);
            }
        }

        poseStack.popPose();
    }

    /** Rueckstoss-Versatz fuer die Sentry-Doppellaeufe (siehe {@link TurretBaseBlockEntity#barrelLeftOffset}). */
    private static float recoilOffsetFor(String partKey, TurretBaseBlockEntity be, float partialTick) {
        if (partKey.equals("sentry_barrell")) {
            return Mth.lerp(partialTick, be.prevBarrelLeftOffset, be.barrelLeftOffset);
        }
        if (partKey.equals("sentry_barrelr")) {
            return Mth.lerp(partialTick, be.prevBarrelRightOffset, be.barrelRightOffset);
        }
        if (partKey.equals("arty_barrel")) {
            return Mth.lerp(partialTick, be.prevBarrelRecoilOffset, be.barrelRecoilOffset);
        }
        return 0.0F;
    }

    /** Kontinuierliche Gatling-Trommel-Rotation (Chekhov/Friendly, siehe {@link TurretBaseBlockEntity#barrelSpinAngle}). */
    private static float spinAngleFor(String partKey, TurretBaseBlockEntity be, float partialTick) {
        if (partKey.equals("chekhov_barrels")) {
            return Mth.lerp(partialTick, be.prevBarrelSpinAngle, be.barrelSpinAngle);
        }
        if (partKey.equals("himars_crane")) {
            float progress = Mth.lerp(partialTick, be.prevHimarsCraneProgress, be.himarsCraneProgress);
            return progress * -30.0F;
        }
        return 0.0F;
    }

    private void renderPart(String partKey, TurretBaseBlockEntity be, PoseStack poseStack,
                             MultiBufferSource bufferSource, int packedLight) {
        BakedModel model = getModel(partKey);
        if (model == null) return;

        SingleMeshVboRenderer renderer = MeshRenderCache.getOrCreateRenderer(partKey, model);
        if (renderer == null) return;

        renderer.render(poseStack, packedLight, be.getBlockPos(), be, bufferSource);
    }

    @Nullable
    private static BakedModel getModel(String partKey) {
        ResourceLocation id = MODEL_IDS.computeIfAbsent(partKey,
                key -> ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/turret_parts/" + key));
        var modelManager = Minecraft.getInstance().getModelManager();
        BakedModel model = PlatformHooks.getModel(modelManager, id);
        return (model == null || model == modelManager.getMissingModel()) ? null : model;
    }
}
