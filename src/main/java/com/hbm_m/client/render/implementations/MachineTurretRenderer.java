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

    /** OBJ der Himars fuer die typabhaengig texturierten Rohre/Deckel (ResourceManager.turret_himars). */
    private static final com.google.common.base.Supplier<com.hbm_m.client.render.SimpleObjModel> HIMARS_OBJ = com.google.common.base.Suppliers.memoize(
            () -> new com.hbm_m.client.render.SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/turrets/turret_himars.obj")));

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

        // The OBJ is modelled around its own origin (Base spans -0.5..+0.5 in X/Z), so the block
        // model carries a "translation": [0.5, 0, 0.5] to seat it in the block. The moving parts
        // are separate part models WITHOUT that transform, so the renderer has to supply it here.
        //
        // This used to be translate(+0.5) -> rotate -> translate(-0.5), which is a pivot change
        // with zero net translation: the moving parts stayed half a block off the base plate in
        // both X and Z, and swung around a point beside their own axis instead of spinning in
        // place. Translating once and then rotating puts the model axis on the block centre and
        // turns it about itself.
        //
        // Original RenderTurretX: glTranslated(x + pos.xCoord, y, z + pos.zCoord) mit
        // pos = getHorizontalOffset() - die Mitte der 2x2/4x4-Grundplatte relativ zum Kern
        // (Sentry und Einzelblock-Altbestand: 0.5/0.5, also wie bisher die Blockmitte).
        net.minecraft.world.phys.Vec3 center = be.getHorizontalOffset();
        poseStack.translate(center.x, 0.0, center.z);

        // Mehrblock-Tuerme: der Sockel ("Base") wird nicht vom Blockmodell gezeichnet (das sitzt am Kern),
        // sondern hier, wie im Original ohne Drehung am Turm-Zentrum.
        // RenderTurretBase.renderConnectors (vor dem Sockel, ebenfalls ohne Drehung)
        if (be.getBlockState().getBlock() instanceof com.hbm_m.block.machines.TurretMultiblockBlock && !be.isLegacySingle()) {
            if (stats == TurretStats.FRITZ) {
                renderConnectors(be, center, poseStack, bufferSource, packedLight, true, true, be.getTank().getTankType());
            } else if (stats == TurretStats.CHEKHOV || stats == TurretStats.FRIENDLY || stats == TurretStats.JEREMY
                    || stats == TurretStats.RICHARD || stats == TurretStats.HOWARD || stats == TurretStats.MAXWELL) {
                renderConnectors(be, center, poseStack, bufferSource, packedLight, true, false, null);
            }
        }

        String baseKey = baseKeyFor(stats);
        if (baseKey != null && be.getBlockState().getBlock() instanceof com.hbm_m.block.machines.TurretMultiblockBlock) {
            renderPart(baseKey, be, poseStack, bufferSource, packedLight);
        }

        poseStack.mulPose(Axis.YP.rotationDegrees(-yaw - 90.0F));
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
            // RenderTurretTauon: Rotor dreht um die Laufachse (0/1.375/0)
            if (partKey.equals("tauon_rotor")) {
                float rot = be.lastTauSpin + (be.tauSpin - be.lastTauSpin) * partialTick;
                poseStack.pushPose();
                poseStack.translate(0.0, 1.375, 0.0);
                poseStack.mulPose(Axis.XP.rotationDegrees(-rot));
                poseStack.translate(0.0, -1.375, 0.0);
                renderPart(partKey, be, poseStack, bufferSource, packedLight);
                poseStack.popPose();
                continue;
            }
            // RenderTurretSentry (beschaedigt): rechter Lauf haengt um 25 Grad abgeknickt
            if (partKey.equals("sentry_damaged_barrelr")) {
                poseStack.pushPose();
                poseStack.translate(0.0, 1.5, 0.5);
                poseStack.mulPose(Axis.XP.rotationDegrees(25));
                poseStack.translate(0.0, -1.5, -0.5);
                renderPart(partKey, be, poseStack, bufferSource, packedLight);
                poseStack.popPose();
                continue;
            }
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

        // RenderTurretHIMARS: geladene Rohre/Deckel fahren mit dem Kran; Textur je Raketentyp (nicht im Blockatlas)
        if (stats == TurretStats.HIMARS && be.himarsTypeLoaded >= 0 && be.himarsTypeLoaded < com.hbm_m.item.weapon.ItemAmmoHIMARS.itemTypes.length) {
            com.hbm_m.item.weapon.ItemAmmoHIMARS.HIMARSRocket type = com.hbm_m.item.weapon.ItemAmmoHIMARS.itemTypes[be.himarsTypeLoaded];
            if (type != null) {
                poseStack.pushPose();
                poseStack.translate(0.0, 0.0, recoilOffsetFor("himars_crane", be, partialTick));
                com.hbm_m.client.weapon.GunGL.begin(poseStack, bufferSource, packedLight);
                com.hbm_m.client.weapon.GunGL.enableCull();
                com.hbm_m.client.weapon.GunGL.bindTexture(type.texture);
                if (type.modelType == 0) {
                    com.hbm_m.client.weapon.GunGL.renderPart(HIMARS_OBJ.get(), "TubeStandard");
                    for (int i = 0; i < be.himarsAmmo; i++) {
                        com.hbm_m.client.weapon.GunGL.renderPart(HIMARS_OBJ.get(), "CapStandard" + (5 - i + 1));
                    }
                }
                if (type.modelType == 1) {
                    com.hbm_m.client.weapon.GunGL.renderPart(HIMARS_OBJ.get(), "TubeSingle");
                    if (be.himarsAmmo > 0) com.hbm_m.client.weapon.GunGL.renderPart(HIMARS_OBJ.get(), "CapSingle");
                }
                com.hbm_m.client.weapon.GunGL.end();
                poseStack.popPose();
            }
        }

        // RenderTurretTauon: Strahl (beam > 0) entlang des Laufs bis zum Ziel, volle Helligkeit
        if (stats == TurretStats.TAUON && be.beam > 0 && be.getLevel() != null) {
            poseStack.pushPose();
            poseStack.translate(0.0, 1.5D, 0.0);
            com.hbm_m.client.render.util.BeamPronter.prontBeam(poseStack, bufferSource, new net.minecraft.world.phys.Vec3(be.lastDist, 0, 0),
                    com.hbm_m.client.render.util.BeamPronter.EnumWaveType.RANDOM, com.hbm_m.client.render.util.BeamPronter.EnumBeamType.LINE,
                    0xffa200, 0xffd000, (int) be.getLevel().getGameTime() / 5 % 360, (int) be.lastDist + 1, 0.1F, 0, 0);
            poseStack.popPose();
        }

        // RenderTurretMaxwell: acht blaue Spiralstrahlen ab Laufende (getBarrelLength 2.125, Hoehe 2), volle Helligkeit
        if (stats == TurretStats.MAXWELL && be.beam > 0 && be.getLevel() != null) {
            double barrel = 2.125D;
            double length = be.lastDist - barrel;
            poseStack.pushPose();
            poseStack.translate(barrel, 2D, 0);
            for (int i = 0; i < 8; i++) {
                com.hbm_m.client.render.util.BeamPronter.prontBeam(poseStack, bufferSource, new net.minecraft.world.phys.Vec3(length, 0, 0),
                        com.hbm_m.client.render.util.BeamPronter.EnumWaveType.SPIRAL, com.hbm_m.client.render.util.BeamPronter.EnumBeamType.SOLID,
                        0x2020ff, 0x2020ff, (int) ((be.getLevel().getGameTime() + partialTick) * -50 + i * 45) % 360, (int) (be.lastDist + 1), 0.375F, 2, 0.05F);
            }
            poseStack.popPose();
        }

        poseStack.popPose();
    }

    /**
     * 1:1 {@code RenderTurretBase#renderConnectors}: an den acht Zellen rund um die 2x2-Grundplatte wird das
     * {@code turret_chekhov "Connectors"}-Teil (Textur connector) gezeichnet, wenn dort ein Kabel bzw. (Fritz) ein
     * Rohr fuer die Tanksorte andocken kann. Bezugspunkt ist die Turm-Mitte {@code (int)(xCoord + pos.xCoord)}.
     */
    private void renderConnectors(TurretBaseBlockEntity be, net.minecraft.world.phys.Vec3 center, PoseStack poseStack,
                                  MultiBufferSource bufferSource, int packedLight, boolean power, boolean fluid,
                                  @Nullable net.minecraft.world.level.material.Fluid type) {
        net.minecraft.world.level.Level level = be.getLevel();
        if (level == null) return;
        net.minecraft.core.BlockPos bp = be.getBlockPos();
        int x = (int) (bp.getX() + center.x);
        int y = bp.getY();
        int z = (int) (bp.getZ() + center.z);
        net.minecraft.core.Direction negX = net.minecraft.core.Direction.WEST, posX = net.minecraft.core.Direction.EAST;
        net.minecraft.core.Direction negZ = net.minecraft.core.Direction.NORTH, posZ = net.minecraft.core.Direction.SOUTH;

        checkPlug(be, level, x - 2, y, z, power, fluid, type, 0, 0, 0, negX, poseStack, bufferSource, packedLight);
        checkPlug(be, level, x - 2, y, z - 1, power, fluid, type, 0, -1, 0, negX, poseStack, bufferSource, packedLight);

        checkPlug(be, level, x - 1, y, z + 1, power, fluid, type, 0, -1, 90, posZ, poseStack, bufferSource, packedLight);
        checkPlug(be, level, x, y, z + 1, power, fluid, type, 0, 0, 90, posZ, poseStack, bufferSource, packedLight);

        checkPlug(be, level, x + 1, y, z, power, fluid, type, 0, -1, 180, posX, poseStack, bufferSource, packedLight);
        checkPlug(be, level, x + 1, y, z - 1, power, fluid, type, 0, 0, 180, posX, poseStack, bufferSource, packedLight);

        checkPlug(be, level, x, y, z - 2, power, fluid, type, 0, -1, 270, negZ, poseStack, bufferSource, packedLight);
        checkPlug(be, level, x - 1, y, z - 2, power, fluid, type, 0, 0, 270, negZ, poseStack, bufferSource, packedLight);
    }

    private void checkPlug(TurretBaseBlockEntity be, net.minecraft.world.level.Level level, int x, int y, int z,
                           boolean power, boolean fluid, @Nullable net.minecraft.world.level.material.Fluid type,
                           int ox, int oz, int rot, net.minecraft.core.Direction dir,
                           PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(x, y, z);
        if ((power && canConnect(level, pos, dir)) || (fluid && type != null && canConnectFluid(level, pos, dir, type))) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(rot));
            poseStack.translate(ox, 0, oz);
            renderPart("chekhov_connectors", be, poseStack, bufferSource, packedLight);
            poseStack.popPose();
        }
    }

    /** {@code Library.canConnect}: Energie-Verbinder, der zur Maschine hin ({@code dir.getOpposite()}) verbinden will. */
    private static boolean canConnect(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos, net.minecraft.core.Direction dir) {
        if (pos.getY() > level.getMaxBuildHeight() - 1 || pos.getY() < level.getMinBuildHeight()) return false;
        return level.getBlockEntity(pos) instanceof com.hbm_m.interfaces.IEnergyConnector c && c.canConnectEnergy(dir.getOpposite());
    }

    /** {@code Library.canConnectFluid}: Block oder Tile muss die Seite zur Maschine fuer diese Sorte annehmen. */
    private static boolean canConnectFluid(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos, net.minecraft.core.Direction dir,
                                           net.minecraft.world.level.material.Fluid type) {
        if (pos.getY() > level.getMaxBuildHeight() - 1 || pos.getY() < level.getMinBuildHeight()) return false;
        if (level.getBlockState(pos).getBlock() instanceof com.hbm_m.api.fluids.IFluidConnectorBlock con
                && con.canConnect(type, level, pos, dir.getOpposite())) return true;
        return level.getBlockEntity(pos) instanceof com.hbm_m.api.fluids.IFluidConnectorMK2 con && con.canConnect(type, dir.getOpposite());
    }

    /**
     * Sockel je Turm wie in den Original-Renderern: {@code turret_chekhov "Base"} mit turret_base_tex
     * (Friendly: base_friendly, Howard beschaedigt: base_rusted), Arty/HIMARS {@code turret_arty "Base"}.
     * Sentry hat keine Mehrblockstruktur - dort bleibt der Sockel im Blockmodell.
     */
    @Nullable
    private static String baseKeyFor(TurretStats stats) {
        if (stats == TurretStats.ARTY || stats == TurretStats.HIMARS) return "arty_base";
        if (stats == TurretStats.FRIENDLY) return "chekhov_base_friendly";
        if (stats == TurretStats.HOWARD_DAMAGED) return "chekhov_base_rusted";
        if (stats == TurretStats.SENTRY || stats == TurretStats.SENTRY_DAMAGED) return null;
        return "chekhov_base";
    }

    /** Rueckstoss-Versatz fuer die Sentry-Doppellaeufe (siehe {@link TurretBaseBlockEntity#barrelLeftOffset}). */
    private static float recoilOffsetFor(String partKey, TurretBaseBlockEntity be, float partialTick) {
        if (partKey.equals("sentry_barrell") || partKey.equals("sentry_damaged_barrell")) {
            return Mth.lerp(partialTick, be.prevBarrelLeftOffset, be.barrelLeftOffset);
        }
        if (partKey.equals("sentry_barrelr")) {
            return Mth.lerp(partialTick, be.prevBarrelRightOffset, be.barrelRightOffset);
        }
        if (partKey.equals("arty_barrel")) {
            return Mth.lerp(partialTick, be.prevBarrelRecoilOffset, be.barrelRecoilOffset);
        }
        if (partKey.equals("himars_crane")) {
            // RenderTurretHIMARS: Kran faehrt um crane * -5 Bloecke entlang des Werfers zurueck
            return Mth.lerp(partialTick, be.prevHimarsCraneProgress, be.himarsCraneProgress) * -5.0F;
        }
        return 0.0F;
    }

    /** Kontinuierliche Gatling-Trommel-Rotation (Chekhov/Friendly, siehe {@link TurretBaseBlockEntity#barrelSpinAngle}). */
    private static float spinAngleFor(String partKey, TurretBaseBlockEntity be, float partialTick) {
        if (partKey.equals("chekhov_barrels")) {
            return Mth.lerp(partialTick, be.prevBarrelSpinAngle, be.barrelSpinAngle);
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
