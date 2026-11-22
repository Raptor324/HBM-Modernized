package com.hbm_m.client.render.implementations;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.TurretBaseBlockEntity;
import com.hbm_m.blockentity.machines.TurretStats;
import com.hbm_m.client.render.PartGeometry;
import com.hbm_m.client.render.machine.MachineRenderers;
import com.hbm_m.client.render.machine.MachineSpecBuilder;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.main.MainRegistry;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.Vec3;

/**
 * Nucleus machine renderer for all 13 turret variants (TileEntityTurretBaseNT family).
 * <p>
 * Architecture:
 * <ul>
 *   <li>Registered on Nucleus pipeline ({@link MachineRenderers#machine}) with VBO caching,
 *       MDI instancing, Iris/Oculus compatibility, and frustum culling.</li>
 *   <li>Base plate is placed at {@code be.getHorizontalOffset()} (resolving the half-block diagonal offset bug).</li>
 *   <li>Carriage/Pivot rotates by world azimuth matching model OBJ orientations:
 *       Chekhov family (-yaw), Sentry family (-yaw + 90), Arty/HIMARS (-yaw - 90).</li>
 *   <li>Pitch group tilts around exact 1.7.10 pivot points and axes.</li>
 *   <li>Animated parts follow 1.7.10 mechanics: Sentry dual-barrel alternating recoil (and damaged bent barrel),
 *       Chekhov/Friendly gatling spin, Tauon rotor spin, Howard dual counter-spinning barrels,
 *       Arty barrel recoil, and HIMARS crane extension.</li>
 * </ul>
 */
public final class MachineTurretRenderer {

    private static final Map<String, List<BakedQuad>> QUAD_CACHE = new ConcurrentHashMap<>();

    private MachineTurretRenderer() {}

    public static void register() {
        registerTurret("turret_sentry", ModBlockEntities.TURRET_SENTRY_BE.get(), TurretStats.SENTRY);
        registerTurret("turret_chekhov", ModBlockEntities.TURRET_CHEKHOV_BE.get(), TurretStats.CHEKHOV);
        registerTurret("turret_friendly", ModBlockEntities.TURRET_FRIENDLY_BE.get(), TurretStats.FRIENDLY);
        registerTurret("turret_jeremy", ModBlockEntities.TURRET_JEREMY_BE.get(), TurretStats.JEREMY);
        registerTurret("turret_tauon", ModBlockEntities.TURRET_TAUON_BE.get(), TurretStats.TAUON);
        registerTurret("turret_richard", ModBlockEntities.TURRET_RICHARD_BE.get(), TurretStats.RICHARD);
        registerTurret("turret_howard", ModBlockEntities.TURRET_HOWARD_BE.get(), TurretStats.HOWARD);
        registerTurret("turret_maxwell", ModBlockEntities.TURRET_MAXWELL_BE.get(), TurretStats.MAXWELL);
        registerTurret("turret_fritz", ModBlockEntities.TURRET_FRITZ_BE.get(), TurretStats.FRITZ);
        registerTurret("turret_arty", ModBlockEntities.TURRET_ARTY_BE.get(), TurretStats.ARTY);
        registerTurret("turret_himars", ModBlockEntities.TURRET_HIMARS_BE.get(), TurretStats.HIMARS);
        registerTurret("turret_sentry_damaged", ModBlockEntities.TURRET_SENTRY_DAMAGED_BE.get(), TurretStats.SENTRY_DAMAGED);
        registerTurret("turret_howard_damaged", ModBlockEntities.TURRET_HOWARD_DAMAGED_BE.get(), TurretStats.HOWARD_DAMAGED);
    }

    private static void registerTurret(String id, BlockEntityType<TurretBaseBlockEntity> type, TurretStats stats) {
        MachineSpecBuilder<TurretBaseBlockEntity> builder =
                MachineRenderers.machine(id, type, TurretBaseBlockEntity.class)
                        .blockTransform((be, anim) -> {
                            Vec3 off = be.getHorizontalOffset();
                            anim.translate((float) off.x, (float) off.y, (float) off.z);
                        })
                        .dynamicPart("Base", be -> resolvePartQuads(stats.basePartKey), be -> stats.basePartKey)
                        .dynamicPart(stats.yawPartKey,
                                (be, pt, gt, pose) -> animateYaw(stats, be, pt, pose),
                                be -> resolvePartQuads(stats.yawPartKey),
                                be -> stats.yawPartKey);

        for (String partKey : stats.pitchPartKeys) {
            final String key = partKey;
            builder.dynamicPart(key,
                    (be, pt, gt, pose) -> animatePitchPart(stats, key, be, pt, pose),
                    be -> resolvePartQuads(key),
                    be -> key);
        }

        builder.animationEpoch(MachineTurretRenderer::animationEpoch);
        builder.viewDistance(256);
        builder.register();
    }

    private static boolean animateYaw(TurretStats stats, TurretBaseBlockEntity be, float pt, PoseStack pose) {
        float yaw = Mth.rotLerp(pt, be.prevYaw, be.yaw);
        float rotY = switch (stats) {
            case SENTRY, SENTRY_DAMAGED -> -yaw + 90.0F;
            case ARTY, HIMARS -> -yaw - 90.0F;
            default -> -yaw;
        };
        pose.mulPose(Axis.YP.rotationDegrees(rotY));
        return true;
    }

    private static boolean animatePitchPart(TurretStats stats, String partKey, TurretBaseBlockEntity be, float pt, PoseStack pose) {
        // 1. Yaw group
        animateYaw(stats, be, pt, pose);

        // 2. Pitch group around pivot
        float pitch = Mth.rotLerp(pt, be.prevPitch, be.pitch);
        double pivotY = stats.pivotY;
        double pivotZ = stats.pivotZ;

        pose.translate(0.0, pivotY, pivotZ);
        if (stats.pitchAxis == TurretStats.PitchAxis.X) {
            if (stats == TurretStats.SENTRY || stats == TurretStats.SENTRY_DAMAGED) {
                pose.mulPose(Axis.XP.rotationDegrees(-pitch));
            } else {
                pose.mulPose(Axis.XP.rotationDegrees(pitch));
            }
        } else {
            pose.mulPose(Axis.ZP.rotationDegrees(pitch));
        }
        pose.translate(0.0, -pivotY, -pivotZ);

        // 3. Part-specific animations (1:1 with 1.7.10)
        switch (partKey) {
            case "sentry_barrell", "sentry_damaged_barrell" -> {
                float recoilL = Mth.lerp(pt, be.prevBarrelLeftOffset, be.barrelLeftOffset);
                pose.translate(0.0, 0.0, recoilL * -0.5f);
            }
            case "sentry_barrelr" -> {
                float recoilR = Mth.lerp(pt, be.prevBarrelRightOffset, be.barrelRightOffset);
                pose.translate(0.0, 0.0, recoilR * -0.5f);
            }
            case "sentry_damaged_barrelr" -> {
                // In 1.7.10: bent at pivot (0, 1.5, 0.5) by 25 deg around X
                pose.translate(0.0, 1.5, 0.5);
                pose.mulPose(Axis.XP.rotationDegrees(25.0f));
                pose.translate(0.0, -1.5, -0.5);
            }
            case "chekhov_barrels" -> {
                float rot = Mth.lerp(pt, be.prevBarrelSpinAngle, be.barrelSpinAngle);
                pose.translate(0.0, 1.5, 0.0);
                pose.mulPose(Axis.XP.rotationDegrees(-rot));
                pose.translate(0.0, -1.5, 0.0);
            }
            case "tauon_rotor" -> {
                float rot = (be.lastTauSpin != 0.0F || be.tauSpin != 0.0F)
                        ? Mth.rotLerp(pt, be.lastTauSpin, be.tauSpin)
                        : Mth.lerp(pt, be.prevBarrelSpinAngle, be.barrelSpinAngle);
                pose.translate(0.0, 1.375, 0.0);
                pose.mulPose(Axis.XP.rotationDegrees(-rot));
                pose.translate(0.0, -1.375, 0.0);
            }
            case "howard_barrelstop", "howard_damaged_barrelstop" -> {
                float rot = Mth.lerp(pt, be.prevBarrelSpinAngle, be.barrelSpinAngle);
                pose.translate(0.0, 2.5, 0.0);
                pose.mulPose(Axis.XP.rotationDegrees(-rot));
                pose.translate(0.0, -2.5, 0.0);
            }
            case "howard_barrelsbottom" -> {
                float rot = Mth.lerp(pt, be.prevBarrelSpinAngle, be.barrelSpinAngle);
                pose.translate(0.0, 2.0, 0.0);
                pose.mulPose(Axis.XP.rotationDegrees(rot));
                pose.translate(0.0, -2.0, 0.0);
            }
            case "howard_damaged_barrelsbottom" -> {
                // Static in 1.7.10 (broken lower barrel)
            }
            case "arty_barrel" -> {
                float recoil = Mth.lerp(pt, be.prevBarrelRecoilOffset, be.barrelRecoilOffset);
                pose.translate(0.0, 0.0, recoil * 2.5f);
            }
            case "himars_crane" -> {
                float crane = Mth.lerp(pt, be.prevHimarsCraneProgress, be.himarsCraneProgress);
                pose.translate(0.0, 0.0, crane * -5.0f);
            }
        }

        return true;
    }

    private static long animationEpoch(TurretBaseBlockEntity be) {
        long h = 17;
        h = h * 31 + Float.floatToIntBits(be.yaw);
        h = h * 31 + Float.floatToIntBits(be.prevYaw);
        h = h * 31 + Float.floatToIntBits(be.pitch);
        h = h * 31 + Float.floatToIntBits(be.prevPitch);
        h = h * 31 + Float.floatToIntBits(be.barrelSpinAngle);
        h = h * 31 + Float.floatToIntBits(be.prevBarrelSpinAngle);
        h = h * 31 + Float.floatToIntBits(be.tauSpin);
        h = h * 31 + Float.floatToIntBits(be.lastTauSpin);
        h = h * 31 + Float.floatToIntBits(be.barrelLeftOffset);
        h = h * 31 + Float.floatToIntBits(be.prevBarrelLeftOffset);
        h = h * 31 + Float.floatToIntBits(be.barrelRightOffset);
        h = h * 31 + Float.floatToIntBits(be.prevBarrelRightOffset);
        h = h * 31 + Float.floatToIntBits(be.barrelRecoilOffset);
        h = h * 31 + Float.floatToIntBits(be.prevBarrelRecoilOffset);
        h = h * 31 + Float.floatToIntBits(be.himarsCraneProgress);
        h = h * 31 + Float.floatToIntBits(be.prevHimarsCraneProgress);
        return h;
    }

    private static List<BakedQuad> resolvePartQuads(String partKey) {
        return QUAD_CACHE.computeIfAbsent(partKey, key -> {
            BakedModel model = com.hbm_m.platform.LoaderHooks.getModel(
                    Minecraft.getInstance().getModelManager(),
                    ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/turret_parts/" + key)
            );
            if (model == null) {
                MainRegistry.LOGGER.warn("Missing turret part model: {}", key);
                return List.of();
            }
            return PartGeometry.collectSolidQuads(model, key);
        });
    }
}
