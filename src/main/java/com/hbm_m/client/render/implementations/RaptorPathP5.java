package com.hbm_m.client.render.implementations;

import static com.hbm_m.client.render.raptorpath.RaptorParts.idleStatic;
import static com.hbm_m.client.render.raptorpath.RaptorParts.mesh;
import static com.hbm_m.client.render.raptorpath.RaptorParts.rotY;
import static com.hbm_m.client.render.raptorpath.RaptorParts.select;
import static com.hbm_m.client.render.raptorpath.RaptorParts.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import com.hbm_m.block.decorations.FloodlightBlock;
import com.hbm_m.block.machines.DummyableMachineBlock;
import com.hbm_m.block.machines.MachineRbmkConsoleBlock;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.decorations.FloodlightBlockEntity;
import com.hbm_m.blockentity.decorations.LanternBehemothBlockEntity;
import com.hbm_m.blockentity.machines.HeatingOvenBlockEntity;
import com.hbm_m.blockentity.machines.MachineCoreEmitterBlockEntity;
import com.hbm_m.blockentity.machines.MachineCoreInjectorBlockEntity;
import com.hbm_m.blockentity.machines.MachineExposureChamberBlockEntity;
import com.hbm_m.blockentity.machines.MachineFENSUBlockEntity;
import com.hbm_m.blockentity.machines.MachineFelBlockEntity;
import com.hbm_m.blockentity.machines.MachineFireboxBaseBlockEntity;
import com.hbm_m.blockentity.machines.MachineFireboxBlockEntity;
import com.hbm_m.blockentity.machines.MachineFurnaceIronBlockEntity;
import com.hbm_m.blockentity.machines.MachineHephaestusBlockEntity;
import com.hbm_m.blockentity.machines.MachineMiningLaserBlockEntity;
import com.hbm_m.blockentity.machines.MachineMixerBlockEntity;
import com.hbm_m.blockentity.machines.MachineOreSlopperBlockEntity;
import com.hbm_m.blockentity.machines.MachineOreSlopperBlockEntity.SlopperAnimation;
import com.hbm_m.blockentity.machines.MachinePrecAssBlockEntity;
import com.hbm_m.blockentity.machines.MachineRbmkConsoleBlockEntity;
import com.hbm_m.blockentity.machines.albion.PABeamlineBlockEntity;
import com.hbm_m.blockentity.machines.dfc.DFCStabilizerBlockEntity;
import com.hbm_m.client.ClientRenderHandler;
import com.hbm_m.client.render.LegacyAnimator;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.client.render.UvShiftConsumer;
import com.hbm_m.client.render.machine.MachineRenderers;
import com.hbm_m.client.render.machine.MachineSpecBuilder;
import com.hbm_m.client.render.raptorpath.ObjAtlasMesh;
import com.hbm_m.client.render.util.BeamPronter;
import com.hbm_m.client.render.util.BeamPronter.EnumBeamType;
import com.hbm_m.client.render.util.BeamPronter.EnumWaveType;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.machine.ItemFELCrystal.EnumWavelengths;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.recipe.PrecAssRecipe;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.Vec3;

/**
 * Render-Migration Paket P5 (Kategorie B): OBJ-Koerper und starre bewegte Teile auf Raptors Pfad; Strahlen,
 * Leuchtteile, Farbteile, Fluessigkeiten, Gegenstaende und Text bleiben als immediate-Hook (Code der alten BERs).
 */
//? if forge {
@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
//?} elif fabric {
/*@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
*///?} elif neoforge {
/*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
*///?}
public final class RaptorPathP5 {

    private RaptorPathP5() {}

    private static ResourceLocation rl(String p) {
        return ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, p);
    }

    private static ObjAtlasMesh p(SimpleObjModel m, ResourceLocation t, String key, String... parts) {
        return ObjAtlasMesh.parts(m, t, "hbm_m/" + key, parts);
    }

    // DFC (Empfaenger bleibt immediate: UV bis 1.74 = Kachelung, im Atlas nicht darstellbar)
    private static final ObjAtlasMesh DFC_EMITTER = ObjAtlasMesh.all(DFCRenderers.EMITTER, DFCRenderers.EMITTER_TEX, "hbm_m/dfc/emitter");
    private static final ObjAtlasMesh DFC_INJECTOR = ObjAtlasMesh.all(DFCRenderers.INJECTOR, DFCRenderers.INJECTOR_TEX, "hbm_m/dfc/injector");
    private static final ObjAtlasMesh DFC_STABILIZER = ObjAtlasMesh.all(DFCRenderers.INJECTOR, DFCRenderers.STABILIZER_TEX, "hbm_m/dfc/stabilizer");
    // Exposure chamber
    private static final ObjAtlasMesh EC_CHAMBER = p(ExposureChamberRenderer.MODEL, ExposureChamberRenderer.TEX, "exposure_chamber/chamber", "Chamber");
    private static final ObjAtlasMesh EC_MAGNETS = p(ExposureChamberRenderer.MODEL, ExposureChamberRenderer.TEX, "exposure_chamber/magnets", "Magnets");
    // FENSU
    private static final ObjAtlasMesh FS_BASE = p(FENSURenderer.MODEL, FENSURenderer.TEX, "fensu/base", "Base");
    private static final ObjAtlasMesh FS_DISC = p(FENSURenderer.MODEL, FENSURenderer.TEX, "fensu/disc", "Disc");
    // FEL
    private static final ObjAtlasMesh FEL = ObjAtlasMesh.all(FelRenderer.MODEL, FelRenderer.TEX, "hbm_m/fel");
    // Firebox / heating oven
    private static final ObjAtlasMesh FB_MAIN = p(FireboxRenderer.MODEL, FireboxRenderer.TEX, "firebox/main", "Main");
    private static final ObjAtlasMesh FB_DOOR = p(FireboxRenderer.MODEL, FireboxRenderer.TEX, "firebox/door", "Door");
    private static final ObjAtlasMesh FB_EMPTY = p(FireboxRenderer.MODEL, FireboxRenderer.TEX, "firebox/inner_empty", "InnerEmpty");
    private static final ObjAtlasMesh HO_MAIN = p(HeatingOvenRenderer.MODEL, HeatingOvenRenderer.TEX, "heating_oven/main", "Main");
    private static final ObjAtlasMesh HO_DOOR = p(HeatingOvenRenderer.MODEL, HeatingOvenRenderer.TEX, "heating_oven/door", "Door");
    private static final ObjAtlasMesh HO_INNER = p(HeatingOvenRenderer.MODEL, HeatingOvenRenderer.TEX, "heating_oven/inner", "Inner");
    // Floodlight: Kopf je eingestellter Drehung (5-Grad-Raster) eingebacken
    private static final ObjAtlasMesh FL_BASE = p(FloodlightRenderer.FLOODLIGHT, FloodlightRenderer.TEX, "floodlight/base", "Base");
    private static final Map<Integer, ObjAtlasMesh> FL_LIGHTS = new HashMap<>();
    // Iron furnace
    private static final ObjAtlasMesh FI_MAIN = p(FurnaceIronRenderer.MODEL, FurnaceIronRenderer.TEX, "furnace_iron/main", "Main");
    private static final ObjAtlasMesh FI_OFF = p(FurnaceIronRenderer.MODEL, FurnaceIronRenderer.TEX, "furnace_iron/off", "Off");
    // Hephaestus (Kern: UV-Lauf mit Kachelung -> immediate)
    private static final ObjAtlasMesh HP_MAIN = p(HephaestusRenderer.MODEL, HephaestusRenderer.TEX, "hephaestus/main", "Main");
    private static final ObjAtlasMesh HP_ROTOR = p(HephaestusRenderer.MODEL, HephaestusRenderer.TEX, "hephaestus/rotor", "Rotor");
    // Behemoth lantern
    private static final ObjAtlasMesh LB_LANTERN = p(LanternRenderer.LANTERN, LanternBehemothRenderer.TEX, "lantern_behemoth/lantern", "Lantern");
    // Ore slopper
    private static final ObjAtlasMesh OS_BASE = p(MachineOreSlopperRenderer.MODEL, MachineOreSlopperRenderer.TEX, "ore_slopper/base", "Base");
    private static final ObjAtlasMesh OS_SLIDER = p(MachineOreSlopperRenderer.MODEL, MachineOreSlopperRenderer.TEX, "ore_slopper/slider", "Slider");
    private static final ObjAtlasMesh OS_HYDRAULICS = p(MachineOreSlopperRenderer.MODEL, MachineOreSlopperRenderer.TEX, "ore_slopper/hydraulics", "Hydraulics");
    private static final ObjAtlasMesh OS_BUCKET = p(MachineOreSlopperRenderer.MODEL, MachineOreSlopperRenderer.TEX, "ore_slopper/bucket", "Bucket");
    private static final ObjAtlasMesh OS_BLADES_L = p(MachineOreSlopperRenderer.MODEL, MachineOreSlopperRenderer.TEX, "ore_slopper/blades_left", "BladesLeft");
    private static final ObjAtlasMesh OS_BLADES_R = p(MachineOreSlopperRenderer.MODEL, MachineOreSlopperRenderer.TEX, "ore_slopper/blades_right", "BladesRight");
    private static final ObjAtlasMesh OS_FAN = p(MachineOreSlopperRenderer.MODEL, MachineOreSlopperRenderer.TEX, "ore_slopper/fan", "Fan");
    // Precision assembler
    private static final ObjAtlasMesh PA_BASE = p(MachinePrecAssRenderer.MODEL, MachinePrecAssRenderer.TEX, "precass/base", "Base");
    private static final ObjAtlasMesh PA_FRAME = p(MachinePrecAssRenderer.MODEL, MachinePrecAssRenderer.TEX, "precass/frame", "Frame");
    private static final ObjAtlasMesh PA_RING = p(MachinePrecAssRenderer.MODEL, MachinePrecAssRenderer.TEX, "precass/ring", "Ring", "Ring2");
    private static final String[] PA_ARM_PARTS = { "ArmLower1", "ArmUpper1", "Head1", "Spike1" };
    private static final ObjAtlasMesh[] PA_ARM = new ObjAtlasMesh[4];
    // RBMK console (Gehaeuse; Mini-Karte und Text immediate)
    private static final ObjAtlasMesh RC_BODY = ObjAtlasMesh.groups(MachineRbmkConsoleRenderer.MODEL_PATH, rl(MachineRbmkConsoleRenderer.BODY_TEXTURE), "hbm_m/rbmk_console/body", "Cube");
    private static final MachineRbmkConsoleRenderer RC_SCREENS = new MachineRbmkConsoleRenderer(null);
    // Mining laser
    private static final ObjAtlasMesh ML_BASE = p(MiningLaserRenderer.MODEL, MiningLaserRenderer.BASE_TEX, "mining_laser/base", "Base");
    private static final ObjAtlasMesh ML_PIVOT = p(MiningLaserRenderer.MODEL, MiningLaserRenderer.PIVOT_TEX, "mining_laser/pivot", "Pivot");
    private static final ObjAtlasMesh ML_LASER = p(MiningLaserRenderer.MODEL, MiningLaserRenderer.LASER_TEX, "mining_laser/laser", "Laser");
    // Mixer
    private static final ObjAtlasMesh MX_MAIN = p(MixerRenderer.MODEL, MixerRenderer.TEX, "mixer/main", "Main");
    private static final ObjAtlasMesh MX_MIXER = p(MixerRenderer.MODEL, MixerRenderer.TEX, "mixer/mixer", "Mixer");
    // Beamline
    private static final ObjAtlasMesh BL_PLAIN = p(PABeamlineRenderer.MODEL, PABeamlineRenderer.TEX, "beamline/plain", "Beamline");
    private static final ObjAtlasMesh BL_WINDOW = p(PABeamlineRenderer.MODEL, PABeamlineRenderer.TEX, "beamline/window", "BeamlineWindow");

    static {
        for (int i = 0; i < PA_ARM.length; i++) {
            PA_ARM[i] = p(MachinePrecAssRenderer.MODEL, MachinePrecAssRenderer.TEX, "precass/" + PA_ARM_PARTS[i].toLowerCase(java.util.Locale.ROOT), PA_ARM_PARTS[i]);
        }
    }

    public static void register() {
        // RenderCoreEmitter / Injector / Stabilizer: Gehaeuse nach 6-Seiten-Ausrichtung, Strahlen immediate
        mesh(MachineRenderers.machine("hbm_m/core_emitter", ModBlockEntities.CORE_EMITTER_BE.get(), MachineCoreEmitterBlockEntity.class)
                .blockTransform((be, a) -> dfcOrient(a, be)), "body", DFC_EMITTER)
                .hook((te, f, ps, buf, light, overlay, api) -> {
                    int range = te.getBeamLength();
                    if (range <= 0) return;
                    ps.pushPose();
                    ps.translate(0, 0.5, 0);
                    long t = DFCRenderers.time(te);
                    BeamPronter.prontBeamwithDepth(ps, buf, new Vec3(0, 0, range), EnumWaveType.SPIRAL, EnumBeamType.SOLID, 0x404000, 0x404000, 0, 1, 0F, 2, 0.0625F, BeamPronter.forkAlpha());
                    BeamPronter.prontBeamwithDepth(ps, buf, new Vec3(0, 0, range), EnumWaveType.RANDOM, EnumBeamType.SOLID, 0x401500, 0x401500, (int) t % 1000, range * 2, 0.125F, 4, 0.0625F, BeamPronter.forkAlpha());
                    BeamPronter.prontBeamwithDepth(ps, buf, new Vec3(0, 0, range), EnumWaveType.RANDOM, EnumBeamType.SOLID, 0x401500, 0x401500, (int) t % 1000 + 1, range * 2, 0.125F, 4, 0.0625F, BeamPronter.forkAlpha());
                    ps.popPose();
                })
                .viewDistance(64).register();
        mesh(MachineRenderers.machine("hbm_m/core_injector", ModBlockEntities.CORE_INJECTOR_BE.get(), MachineCoreInjectorBlockEntity.class)
                .blockTransform((be, a) -> dfcOrient(a, be)), "body", DFC_INJECTOR)
                .hook((injector, f, ps, buf, light, overlay, api) -> {
                    int range = injector.getBeam();
                    if (range <= 0) return;
                    ps.pushPose();
                    ps.translate(0, 0.5, 0);
                    long t = DFCRenderers.time(injector);
                    FluidTank t0 = injector.getTank(0), t1 = injector.getTank(1);
                    if (t0 != null && t0.getFill() > 0)
                        BeamPronter.prontBeam(ps, buf, new Vec3(0, 0, range), EnumWaveType.RANDOM, EnumBeamType.LINE,
                                FluidType.forFluid(t0.getTankType()).getColor(), 0x808080, (int) t % 1000, range, 0.0625F, 0, 0, BeamPronter.forkAlpha());
                    if (t1 != null && t1.getFill() > 0)
                        BeamPronter.prontBeam(ps, buf, new Vec3(0, 0, range), EnumWaveType.RANDOM, EnumBeamType.LINE,
                                FluidType.forFluid(t1.getTankType()).getColor(), 0x808080, (int) t % 1000 + 1, range, 0.0625F, 0, 0, BeamPronter.forkAlpha());
                    ps.popPose();
                })
                .viewDistance(64).register();
        mesh(MachineRenderers.machine("hbm_m/dfc_stabilizer", ModBlockEntities.DFC_STABILIZER_BE.get(), DFCStabilizerBlockEntity.class)
                .blockTransform((be, a) -> dfcOrient(a, be)), "body", DFC_STABILIZER)
                .hook((stabilizer, f, ps, buf, light, overlay, api) -> {
                    int range = stabilizer.getBeam();
                    if (range <= 0) return;
                    ps.pushPose();
                    ps.translate(0, 0.5, 0);
                    long t = DFCRenderers.time(stabilizer);
                    BeamPronter.prontBeam(ps, buf, new Vec3(0, 0, range), EnumWaveType.SPIRAL, EnumBeamType.LINE, 0xffa200, 0xffd000, (int) t * -25 % 360, range * 3, 0.125F, 0, 0, BeamPronter.forkAlpha());
                    BeamPronter.prontBeam(ps, buf, new Vec3(0, 0, range), EnumWaveType.SPIRAL, EnumBeamType.LINE, 0xffa200, 0xffd000, (int) t * -15 % 360 + 180, range * 3, 0.125F, 0, 0, BeamPronter.forkAlpha());
                    BeamPronter.prontBeam(ps, buf, new Vec3(0, 0, range), EnumWaveType.SPIRAL, EnumBeamType.LINE, 0xffa200, 0xffd000, (int) t * -5 % 360 + 180, range * 3, 0.125F, 0, 0, BeamPronter.forkAlpha());
                    ps.popPose();
                })
                .viewDistance(64).register();

        // RenderExposureChamber: Metadaten 4/3/5/2 -> 180/270/0/90; Magnete drehen; Kern (vollhell) + Blitze immediate
        MachineSpecBuilder<MachineExposureChamberBlockEntity> ec = MachineRenderers.machine("hbm_m/exposure_chamber", ModBlockEntities.EXPOSURE_CHAMBER_BE.get(), MachineExposureChamberBlockEntity.class)
                .blockTransform((be, a) -> {
                    a.translate(0.5D, 0D, 0.5D);
                    switch (be.getBlockState().getValue(DummyableMachineBlock.FACING)) {
                        case WEST -> rotY(a, 180);
                        case SOUTH -> rotY(a, 270);
                        case NORTH -> rotY(a, 90);
                        default -> { }
                    }
                });
        mesh(ec, "chamber", EC_CHAMBER);
        mesh(ec, "magnets", (be, f, t, ps) -> {
            ps.mulPose(Axis.YP.rotationDegrees((float) (be.prevRotation + (be.rotation - be.prevRotation) * f)));
            return true;
        }, EC_MAGNETS);
        ec.hook(RaptorPathP5::exposureHook).viewDistance(256).register();

        // RenderFENSU: Scheibe dreht; Lichter (vollhell, mit der Scheibe) immediate
        MachineSpecBuilder<MachineFENSUBlockEntity> fs = MachineRenderers.machine("hbm_m/fensu", ModBlockEntities.MACHINE_FENSU_BE.get(), MachineFENSUBlockEntity.class)
                .blockTransform((be, a) -> {
                    a.translate(0.5D, 0D, 0.5D);
                    switch (be.getBlockState().getValue(DummyableMachineBlock.FACING)) {
                        case NORTH -> rotY(a, 90);
                        case WEST -> rotY(a, 180);
                        case SOUTH -> rotY(a, 270);
                        default -> { }
                    }
                });
        mesh(fs, "base", FS_BASE);
        mesh(fs, "disc", RaptorPathP5::fensuDisc, FS_DISC);
        fs.hook((be, f, ps, buf, light, overlay, api) -> {
            ps.pushPose();
            fensuDisc(be, f, 0L, ps);
            FENSURenderer.MODEL.renderPart("Lights", ps, buf.getBuffer(RenderType.entityCutoutNoCull(FENSURenderer.TEX)), LightTexture.FULL_BRIGHT);
            ps.popPose();
        }).viewDistance(256).register();

        // RenderFEL: Laserstrahl immediate
        mesh(MachineRenderers.machine("hbm_m/fel", ModBlockEntities.FEL_BE.get(), MachineFelBlockEntity.class)
                .blockTransform((be, a) -> {
                    a.translate(0.5D, 0D, 0.5D);
                    switch (be.getBlockState().getValue(DummyableMachineBlock.FACING)) {
                        case WEST -> rotY(a, 90);
                        case SOUTH -> rotY(a, 180);
                        case EAST -> rotY(a, 270);
                        default -> { }
                    }
                }), "body", FEL)
                .hook(RaptorPathP5::felBeam)
                .viewDistance(256).register();

        // RenderFirebox: Tuer schwenkt; Glut (vollhell) immediate
        MachineSpecBuilder<MachineFireboxBlockEntity> fb = MachineRenderers.machine("hbm_m/firebox", ModBlockEntities.FIREBOX_BE.get(), MachineFireboxBlockEntity.class)
                .blockTransform((be, a) -> ovenRotate(be, a));
        mesh(fb, "main", FB_MAIN);
        idleStatic(fb, "door", (be, f, t, ps) -> {
            float door = be.prevDoorAngle + (be.doorAngle - be.prevDoorAngle) * f;
            ps.translate(1.375, 0, 0.375);
            ps.mulPose(Axis.YN.rotationDegrees(door));
            ps.translate(-1.375, 0, -0.375);
            return true;
        }, be -> be.prevDoorAngle != 0F || be.doorAngle != 0F, FB_DOOR);
        when(fb, "inner_empty", be -> !be.wasOn, FB_EMPTY);
        fb.hook((be, f, ps, buf, light, overlay, api) -> {
            if (be.wasOn) FireboxRenderer.MODEL.renderPart("InnerBurning", ps, buf.getBuffer(RenderType.entityCutoutNoCull(FireboxRenderer.TEX)), LightTexture.FULL_BRIGHT);
        }).viewDistance(256).register();

        // RenderHeatingOven: Tuer schiebt; Glut (vollhell) immediate
        MachineSpecBuilder<HeatingOvenBlockEntity> ho = MachineRenderers.machine("hbm_m/heating_oven", ModBlockEntities.HEATING_OVEN_BE.get(), HeatingOvenBlockEntity.class)
                .blockTransform((be, a) -> ovenRotate(be, a));
        mesh(ho, "main", HO_MAIN);
        idleStatic(ho, "door", (be, f, t, ps) -> {
            float door = be.prevDoorAngle + (be.doorAngle - be.prevDoorAngle) * f;
            ps.translate(0, 0, door * 0.75D / 135D);
            return true;
        }, be -> be.prevDoorAngle != 0F || be.doorAngle != 0F, HO_DOOR);
        when(ho, "inner", be -> !be.wasOn, HO_INNER);
        ho.hook((be, f, ps, buf, light, overlay, api) -> {
            if (be.wasOn) HeatingOvenRenderer.MODEL.renderPart("InnerBurning", ps, buf.getBuffer(RenderType.entityCutoutNoCull(HeatingOvenRenderer.TEX)), LightTexture.FULL_BRIGHT);
        }).viewDistance(256).register();

        // RenderFloodlight: Ausrichtung nach Meta; Kopf nach eingestellter Drehung; Lampen (vollhell/abgedunkelt) immediate
        MachineSpecBuilder<FloodlightBlockEntity> fl = MachineRenderers.machine("hbm_m/floodlight", ModBlockEntities.FLOODLIGHT.get(), FloodlightBlockEntity.class)
                .blockTransform((be, a) -> floodlightOrient(be, a));
        mesh(fl, "base", FL_BASE);
        select(fl, "lights", be -> floodlightHead(floodlightRotation(be)));
        fl.hook((tile, f, ps, buf, light, overlay, api) -> {
            ps.pushPose();
            ps.translate(0, 0.5, 0);
            ps.mulPose(Axis.ZP.rotationDegrees(floodlightRotation(tile)));
            ps.translate(0, -0.5, 0);
            var vc = buf.getBuffer(RenderType.entityCutout(FloodlightRenderer.TEX));
            if (tile.isOn) {
                FloodlightRenderer.FLOODLIGHT.renderPart("Lamps", ps, vc, LightTexture.FULL_BRIGHT);
            } else {
                FloodlightRenderer.FLOODLIGHT.renderPartTinted("Lamps", ps, vc, light, 0.25F, 0.25F, 0.25F);
            }
            ps.popPose();
        }).viewDistance(256).register();

        // RenderFurnaceIron: Glut (vollhell) immediate
        MachineSpecBuilder<MachineFurnaceIronBlockEntity> fi = MachineRenderers.machine("hbm_m/furnace_iron", ModBlockEntities.FURNACE_IRON_BE.get(), MachineFurnaceIronBlockEntity.class)
                .blockTransform((be, a) -> {
                    a.translate(0.5D, 0D, 0.5D);
                    switch (ObjBerHelper.meta(be)) {
                        case 5 -> rotY(a, 90);
                        case 2 -> rotY(a, 180);
                        case 4 -> rotY(a, 270);
                        default -> { }
                    }
                    a.translate(-0.5D, 0, -0.5D);
                });
        mesh(fi, "main", FI_MAIN);
        when(fi, "off", be -> !be.wasOn, FI_OFF);
        fi.hook((be, f, ps, buf, light, overlay, api) -> {
            if (be.wasOn) FurnaceIronRenderer.MODEL.renderPart("On", ps, buf.getBuffer(RenderType.entityCutoutNoCull(FurnaceIronRenderer.TEX)), LightTexture.FULL_BRIGHT);
        }).viewDistance(64).register();

        // RenderHephaestus: drei Rotorarme drehen; Kern (Lava/Bruchstein, UV-Lauf) immediate
        MachineSpecBuilder<MachineHephaestusBlockEntity> hp = MachineRenderers.machine("hbm_m/hephaestus", ModBlockEntities.HEPHAESTUS_BE.get(), MachineHephaestusBlockEntity.class)
                .blockTransform((be, a) -> a.translate(0.5D, 0D, 0.5D));
        mesh(hp, "main", HP_MAIN);
        for (int i = 0; i < 3; i++) {
            final int arm = i;
            mesh(hp, "rotor" + i, (be, f, t, ps) -> {
                ps.mulPose(Axis.YP.rotationDegrees(be.prevRot + (be.rot - be.prevRot) * f));
                for (int k = 0; k < arm; k++) ps.mulPose(Axis.YP.rotationDegrees(120));
                return true;
            }, HP_ROTOR);
        }
        hp.hook((geo, interp, ps, buf, light, overlay, api) -> {
            float movement = geo.prevRot + (geo.rot - geo.prevRot) * interp;
            boolean isOn = geo.bufferedHeat > 0;
            var core = new UvShiftConsumer(buf.getBuffer(RenderType.entityCutout(isOn ? HephaestusRenderer.LAVA : HephaestusRenderer.COBBLE)), 0F, movement / 10F, 0.5F, 0.5F);
            if (isOn) {
                HephaestusRenderer.MODEL.renderPart("Core", ps, core, 0xF000F0);
            } else {
                HephaestusRenderer.MODEL.renderPartTinted("Core", ps, core, light, 0.5F, 0.5F, 0.5F);
            }
        }).viewDistance(256).register();

        // RenderLanternBehemoth: kaputt geneigt; Leuchtteil (Farbe, pulsiert) immediate
        mesh(MachineRenderers.machine("hbm_m/lantern_behemoth", ModBlockEntities.LANTERN_BEHEMOTH_BE.get(), LanternBehemothBlockEntity.class)
                .blockTransform((be, a) -> {
                    a.translate(0.5D, 0, 0.5D);
                    if (be.isBroken) {
                        a.rotate(5, 1, 0, 0);
                        a.rotate(10, 0, 0, 1);
                    }
                }), "lantern", LB_LANTERN)
                .hook((lantern, f, ps, buf, light, overlay, api) -> {
                    float r, g;
                    if (lantern.isBroken) {
                        float mult = (float) (Math.sin(System.currentTimeMillis() / 200D) / 2 + 0.5);
                        r = mult; g = 0;
                    } else {
                        float mult = (float) (Math.sin(System.currentTimeMillis() / 200D) / 2 + 0.5) * 0.5F + 0.5F;
                        r = 0; g = mult;
                    }
                    LanternRenderer.LANTERN.renderPartColor("Light", ps, buf.getBuffer(ClientRenderHandler.CustomRenderTypes.SOLID_COLOR_NOCULL), r, g, 0F, 1F);
                })
                .viewDistance(256).register();

        registerOreSlopper();
        registerPrecAss();

        // RenderRBMKConsole: Gehaeuse; Mini-Karte und Textschirme immediate
        mesh(MachineRenderers.machine("hbm_m/rbmk_console", ModBlockEntities.RBMK_CONSOLE_BE.get(), MachineRbmkConsoleBlockEntity.class)
                .blockTransform((be, a) -> {
                    a.translate(0.5, 0, 0.5);
                    Direction facing = be.getBlockState().getValue(MachineRbmkConsoleBlock.FACING);
                    rotY(a, switch (facing) {
                        case SOUTH -> 270f;
                        case WEST -> 180f;
                        case EAST -> 0f;
                        default -> 90f;
                    });
                    a.translate(0.5, 0, 0);
                }), "body", RC_BODY)
                .hook((be, f, ps, buf, light, overlay, api) -> RC_SCREENS.renderScreens(be, ps, buf, light, overlay))
                .viewDistance(64).register();

        // RenderMiningLaser: Drehkopf und Laserrohr folgen dem Ziel; Strahl immediate
        MachineSpecBuilder<MachineMiningLaserBlockEntity> ml = MachineRenderers.machine("hbm_m/mining_laser", ModBlockEntities.MINING_LASER_BE.get(), MachineMiningLaserBlockEntity.class)
                .blockTransform((be, a) -> a.translate(0.5D, -1D, 0.5D));
        mesh(ml, "base", ML_BASE);
        mesh(ml, "pivot", (be, f, t, ps) -> {
            ps.mulPose(Axis.YP.rotationDegrees((float) laserAngles(be, f)[0]));
            return true;
        }, ML_PIVOT);
        mesh(ml, "laser", (be, f, t, ps) -> {
            double[] yp = laserAngles(be, f);
            ps.mulPose(Axis.YP.rotationDegrees((float) yp[0]));
            ps.translate(0, -1, 0);
            ps.mulPose(Axis.XN.rotationDegrees((float) (yp[1] + 90)));
            ps.translate(0, 1, 0);
            return true;
        }, ML_LASER);
        ml.hook(RaptorPathP5::miningLaserBeam).viewDistance(256).register();

        // RenderMixer: Ruehrwerk dreht; Fluessigkeit (Farbe, Fuellstand-Skalierung) immediate
        MachineSpecBuilder<MachineMixerBlockEntity> mx = MachineRenderers.machine("hbm_m/mixer", ModBlockEntities.MIXER_BE.get(), MachineMixerBlockEntity.class)
                .blockTransform((be, a) -> a.translate(0.5D, 0, 0.5D));
        mesh(mx, "main", MX_MAIN);
        mesh(mx, "mixer", (be, f, t, ps) -> {
            ps.mulPose(Axis.YN.rotationDegrees(be.prevRotation + (be.rotation - be.prevRotation) * f));
            return true;
        }, MX_MIXER);
        mx.hook((mixer, f, ps, buf, light, overlay, api) -> {
            int totalFill = 0;
            int totalMax = 0;
            for (FluidTank tank : mixer.tanks) {
                if (FluidTank.isFluidTypeExplicitlySet(tank.getTankType())) {
                    totalFill += tank.getFill();
                    totalMax += tank.getMaxFill();
                }
            }
            if (totalFill <= 0) return;
            int color = com.hbm_m.api.fluids.HbmFluidRegistry.getTintColor(mixer.tanks[2].getTankType());
            ps.pushPose();
            ps.translate(0, 1, 0);
            ps.scale(1, (float) ((double) totalFill / (double) totalMax * 0.99), 1);
            ps.translate(0, -1, 0);
            MixerRenderer.MODEL.renderPartColor("Fluid", ps, buf.getBuffer(ClientRenderHandler.CustomRenderTypes.TRANSLUCENT_COLOR_NOCULL),
                    ((color >> 16) & 255) / 255F, ((color >> 8) & 255) / 255F, (color & 255) / 255F, 0.75F);
            ps.popPose();
        }).viewDistance(256).register();

        // RenderPABeamline: mit/ohne Fenster; Glas (Farbe, Blitz) immediate
        select(MachineRenderers.machine("hbm_m/pa_beamline", ModBlockEntities.PA_BEAMLINE_BE.get(), PABeamlineBlockEntity.class)
                .blockTransform((be, a) -> {
                    a.translate(0.5D, 0D, 0.5D);
                    switch (ObjBerHelper.meta(be)) {
                        case 4 -> rotY(a, 180);
                        case 3 -> rotY(a, 270);
                        case 2 -> rotY(a, 90);
                        default -> { }
                    }
                }), "body", be -> be.hasWindow() ? BL_WINDOW : BL_PLAIN)
                .hook((beamline, f, ps, buf, light, overlay, api) -> {
                    if (!beamline.hasWindow()) return;
                    float flash = beamline.prevLight + (beamline.light - beamline.prevLight) * f;
                    PABeamlineRenderer.MODEL.renderPartColor("BeamlineGlass", ps, buf.getBuffer(ClientRenderHandler.CustomRenderTypes.SOLID_COLOR_NOCULL),
                            Math.min(1F, 0.9F * flash), Math.min(1F, 0.9F * flash), Math.min(1F, 1.0F * flash), 1F);
                })
                .viewDistance(64).register();
    }

    // ── Erzwaescher ──────────────────────────────────────────────────────────────────────

    private static void registerOreSlopper() {
        MachineSpecBuilder<MachineOreSlopperBlockEntity> os = MachineRenderers.machine("hbm_m/ore_slopper", ModBlockEntities.ORE_SLOPPER_BE.get(), MachineOreSlopperBlockEntity.class)
                .blockTransform((be, a) -> {
                    a.translate(0.5D, 0D, 0.5D);
                    switch (be.getBlockState().getValue(DummyableMachineBlock.FACING)) {
                        case SOUTH -> rotY(a, 180);
                        case EAST -> rotY(a, 270);
                        case WEST -> rotY(a, 90);
                        default -> { }
                    }
                });
        mesh(os, "base", OS_BASE);
        mesh(os, "slider", (be, f, t, ps) -> slopper(be, f, ps, 0), OS_SLIDER);
        mesh(os, "hydraulics", (be, f, t, ps) -> slopper(be, f, ps, 1), OS_HYDRAULICS);
        mesh(os, "bucket", (be, f, t, ps) -> slopper(be, f, ps, 2), OS_BUCKET);
        mesh(os, "blades_left", (be, f, t, ps) -> {
            float blades = be.prevBlades + (be.blades - be.prevBlades) * f;
            ps.translate(0.375, 2.75, 0);
            ps.mulPose(Axis.ZP.rotationDegrees(blades));
            ps.translate(-0.375, -2.75, 0);
            return true;
        }, OS_BLADES_L);
        mesh(os, "blades_right", (be, f, t, ps) -> {
            float blades = be.prevBlades + (be.blades - be.prevBlades) * f;
            ps.translate(-0.375, 2.75, 0);
            ps.mulPose(Axis.ZP.rotationDegrees(-blades));
            ps.translate(0.375, -2.75, 0);
            return true;
        }, OS_BLADES_R);
        mesh(os, "fan", (be, f, t, ps) -> {
            float fan = be.prevFan + (be.fan - be.prevFan) * f;
            ps.translate(0, 1.875, -1);
            ps.mulPose(Axis.XP.rotationDegrees(-fan));
            ps.translate(0, -1.875, 1);
            return true;
        }, OS_FAN);
        os.hook((slopper, f, ps, buf, light, overlay, api) -> {
            if (slopper.animation != SlopperAnimation.LIFTING) return;
            ps.pushPose();
            slopper(slopper, f, ps, 2);
            ps.translate(0.0625D, 4.3125D, 2D);
            ps.mulPose(Axis.YP.rotationDegrees(90));
            ps.mulPose(Axis.XP.rotationDegrees(-90));
            ps.scale(1.75F, 1.75F, 1.75F);
            Minecraft.getInstance().getItemRenderer().renderStatic(new ItemStack(ModItems.BEDROCK_ORE_BASE_LIGHT.get()),
                    ItemDisplayContext.FIXED, light, overlay, ps, buf, slopper.getLevel(), 0);
            ps.popPose();
        }).viewDistance(256).register();
    }

    /** Schlitten (0) -> Hydraulik (1) -> Schaufel (2) aus {@code MachineOreSlopperRenderer}. */
    private static boolean slopper(MachineOreSlopperBlockEntity s, float interp, PoseStack ps, int stage) {
        double slide = s.prevSlider + (s.slider - s.prevSlider) * interp;
        ps.translate(0, 0, slide * -3);
        if (stage == 0) return true;
        double extend = (s.prevBucket + (s.bucket - s.prevBucket) * interp) * 1.5;
        ps.translate(0, -Mth.clamp(extend - 0.25, 0, 1.25), 0);
        if (stage == 1) return true;
        ps.translate(0, -Mth.clamp(extend, 0, 1.25), 0);
        return true;
    }

    // ── Praezisionsassembler ─────────────────────────────────────────────────────────────

    private static void registerPrecAss() {
        MachineSpecBuilder<MachinePrecAssBlockEntity> pa = MachineRenderers.machine("hbm_m/precass", ModBlockEntities.PRECASS_BE.get(), MachinePrecAssBlockEntity.class)
                .blockTransform((be, a) -> {
                    a.translate(0.5, 0, 0.5);
                    rotY(a, 90);
                    switch (be.getBlockState().getValue(DummyableMachineBlock.FACING)) {
                        case WEST -> rotY(a, 90);
                        case SOUTH -> rotY(a, 180);
                        case EAST -> rotY(a, 270);
                        default -> { }
                    }
                });
        mesh(pa, "base", PA_BASE);
        when(pa, "frame", be -> be.frame, PA_FRAME);
        mesh(pa, "ring", (be, f, t, ps) -> {
            ps.mulPose(Axis.YP.rotationDegrees((float) lerp(be.prevRing, be.ring, f)));
            return true;
        }, PA_RING);
        for (int arm = 0; arm < 4; arm++) {
            for (int stage = 0; stage < 4; stage++) {
                final int a = arm, st = stage;
                mesh(pa, "arm" + arm + "_" + stage, (be, f, t, ps) -> precAssArm(be, f, ps, a, st), PA_ARM[stage]);
            }
        }
        pa.hook((assembler, interp, ps, buffers, light, overlay, api) -> {
            PrecAssRecipe recipe = assembler.getSelectedRecipe();
            var me = Minecraft.getInstance().player;
            if (recipe == null || me == null || me.distanceToSqr(assembler.getBlockPos().getX() + 0.5, assembler.getBlockPos().getY() + 1, assembler.getBlockPos().getZ() + 0.5) >= 35 * 35) return;
            ps.pushPose();
            ps.mulPose(Axis.YP.rotationDegrees(90));
            ps.translate(0, 1.0625, 0);
            ItemStack stack = recipe.getResultItemSafe();
            stack.setCount(1);
            if (stack.getItem() instanceof BlockItem) {
                ps.translate(0, 0.0625 + 0.125, 0);
            } else {
                ps.mulPose(Axis.XN.rotationDegrees(90));
                ps.translate(0, -0.25 + 0.25, 0);
            }
            ps.scale(1.25F * 0.5F, 1.25F * 0.5F, 1.25F * 0.5F);
            Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, ps, buffers, assembler.getLevel(), 0);
            ps.popPose();
        }).viewDistance(64).register();
    }

    private static double lerp(double prev, double cur, float t) {
        return prev + (cur - prev) * t;
    }

    /** Arm {@code arm} (0-3, je -90 Grad weiter) bis zum Glied {@code stage} (0 ArmLower, 1 ArmUpper, 2 Head, 3 Spike). */
    private static boolean precAssArm(MachinePrecAssBlockEntity be, float f, PoseStack ps, int arm, int stage) {
        ps.mulPose(Axis.YP.rotationDegrees((float) lerp(be.prevRing, be.ring, f)));
        for (int k = 0; k < arm; k++) ps.mulPose(Axis.YP.rotationDegrees(-90));
        ps.translate(0, 1.625, 0.9375);
        ps.mulPose(Axis.XP.rotationDegrees((float) lerp(be.prevArmAngles[0], be.armAngles[0], f)));
        ps.translate(0, -1.625, -0.9375);
        if (stage == 0) return true;
        ps.translate(0, 2.375, 0.9375);
        ps.mulPose(Axis.XP.rotationDegrees((float) lerp(be.prevArmAngles[1], be.armAngles[1], f)));
        ps.translate(0, -2.375, -0.9375);
        if (stage == 1) return true;
        ps.translate(0, 2.375, 0.4375);
        ps.mulPose(Axis.XP.rotationDegrees((float) lerp(be.prevArmAngles[2], be.armAngles[2], f)));
        ps.translate(0, -2.375, -0.4375);
        if (stage == 2) return true;
        ps.translate(0, lerp(be.prevStrikers[arm], be.strikers[arm], f), 0);
        return true;
    }

    // ── Hilfen ───────────────────────────────────────────────────────────────────────────

    /** {@code DFCRenderers.orient}: 90 Grad, dann je Metadate (0/1 kippen um X). */
    private static void dfcOrient(LegacyAnimator a, net.minecraft.world.level.block.entity.BlockEntity be) {
        a.translate(0.5, 0, 0.5);
        rotY(a, 90);
        switch (ObjBerHelper.meta6(be)) {
            case 0 -> {
                a.translate(0.0D, 0.5D, -0.5D);
                a.rotate(90, 1, 0, 0);
            }
            case 1 -> {
                a.translate(0.0D, 0.5D, 0.5D);
                a.rotate(-90, 1, 0, 0);
            }
            case 2 -> rotY(a, 90);
            case 4 -> rotY(a, 180);
            case 3 -> rotY(a, 270);
            default -> { }
        }
    }

    /** {@code HeatingOvenRenderer.rotate}: Metadaten 3/5/2/4 -> 0/90/180/270, danach -90. */
    private static void ovenRotate(MachineFireboxBaseBlockEntity tile, LegacyAnimator a) {
        a.translate(0.5D, 0D, 0.5D);
        Direction facing = tile.getBlockState().hasProperty(HorizontalDirectionalBlock.FACING)
                ? tile.getBlockState().getValue(HorizontalDirectionalBlock.FACING) : Direction.NORTH;
        switch (facing) {
            case EAST -> rotY(a, 90);
            case NORTH -> rotY(a, 180);
            case WEST -> rotY(a, 270);
            default -> { }
        }
        rotY(a, -90);
    }

    private static boolean fensuDisc(MachineFENSUBlockEntity fensu, float f, long t, PoseStack ps) {
        float rot = fensu.prevRotation + (fensu.rotation - fensu.prevRotation) * f;
        ps.translate(0, 2.5, 0);
        ps.mulPose(Axis.XP.rotationDegrees(rot));
        ps.translate(0, -2.5, 0);
        return true;
    }

    private static void floodlightOrient(FloodlightBlockEntity tile, LegacyAnimator a) {
        a.translate(0.5D, 0.5D, 0.5D);
        int meta = tile.getBlockState().getValue(FloodlightBlock.META);
        switch (meta) {
            case 0, 6 -> a.rotate(180, 1, 0, 0);
            case 2 -> { a.rotate(90, 1, 0, 0); a.rotate(180, 0, 0, 1); }
            case 3 -> a.rotate(90, 1, 0, 0);
            case 4 -> { a.rotate(90, 1, 0, 0); a.rotate(90, 0, 0, 1); }
            case 5 -> { a.rotate(90, 1, 0, 0); a.rotate(270, 0, 0, 1); }
            default -> { }
        }
        a.translate(0, -0.5, 0);
        if (meta != 0 && meta != 1) rotY(a, 90);
    }

    private static float floodlightRotation(FloodlightBlockEntity tile) {
        int meta = tile.getBlockState().getValue(FloodlightBlock.META);
        float rotation = tile.rotation;
        if (meta == 0 || meta == 6) rotation -= 90;
        if (meta == 1 || meta == 7) rotation += 90;
        return rotation;
    }

    /** Kopf ("Lights") mit eingebackener Neigung; je Winkel ein Netz (Winkel im 5-Grad-Raster, also wenige). */
    private static ObjAtlasMesh floodlightHead(float rotation) {
        return FL_LIGHTS.computeIfAbsent(Float.floatToIntBits(rotation), bits -> ObjAtlasMesh.of("hbm_m/floodlight/lights" + rotation, b -> {
            b.pose.translate(0, 0.5, 0);
            b.pose.mulPose(Axis.ZP.rotationDegrees(rotation));
            b.pose.translate(0, -0.5, 0);
            b.obj(FloodlightRenderer.FLOODLIGHT, FloodlightRenderer.TEX, "Lights");
        }));
    }

    /** Yaw/Pitch des Laserkopfs aus {@code MiningLaserRenderer}. */
    private static double[] laserAngles(MachineMiningLaserBlockEntity laser, float interpolation) {
        double tx = (laser.targetX - laser.lastTargetX) * interpolation + laser.lastTargetX;
        double ty = (laser.targetY - laser.lastTargetY) * interpolation + laser.lastTargetY;
        double tz = (laser.targetZ - laser.lastTargetZ) * interpolation + laser.lastTargetZ;
        double vx = tx - laser.getBlockPos().getX();
        double vy = ty - laser.getBlockPos().getY() + 3;
        double vz = tz - laser.getBlockPos().getZ();
        Vec3 nVec = new Vec3(vx, vy, vz).normalize().scale(1.5D);
        Vec3 vec = new Vec3(vx - nVec.x, vy - nVec.y, vz - nVec.z);
        double yaw = Math.toDegrees(Math.atan2(vec.x, vec.z));
        double sqrt = Math.sqrt(vec.x * vec.x + vec.z * vec.z);
        double pitch = Math.toDegrees(Math.atan2(vec.y, sqrt));
        return new double[] { yaw, pitch };
    }

    private static void miningLaserBeam(MachineMiningLaserBlockEntity laser, float interpolation, PoseStack ps, MultiBufferSource buf,
                                        int light, int overlay, com.hbm_m.client.render.machine.MachineRenderApi api) {
        if (!laser.beam || laser.getLevel() == null) return;
        double tx = (laser.targetX - laser.lastTargetX) * interpolation + laser.lastTargetX;
        double ty = (laser.targetY - laser.lastTargetY) * interpolation + laser.lastTargetY;
        double tz = (laser.targetZ - laser.lastTargetZ) * interpolation + laser.lastTargetZ;
        double vx = tx - laser.getBlockPos().getX();
        double vy = ty - laser.getBlockPos().getY() + 3;
        double vz = tz - laser.getBlockPos().getZ();
        Vec3 nVec = new Vec3(vx, vy, vz).normalize().scale(1.5D);
        Vec3 vec = new Vec3(vx - nVec.x, vy - nVec.y, vz - nVec.z);
        double length = vec.length();
        ps.pushPose();
        ps.translate(nVec.x, nVec.y - 1, nVec.z);
        int range = (int) Math.ceil(length * 0.5);
        int time = (int) laser.getLevel().getGameTime() * -25 % 360;
        BeamPronter.prontBeam(ps, buf, vec, EnumWaveType.SPIRAL, EnumBeamType.SOLID, 0xa00000, 0xa00000, time, range * 2, 0.075F, 3, 0.025F, BeamPronter.forkAlpha());
        BeamPronter.prontBeam(ps, buf, vec, EnumWaveType.SPIRAL, EnumBeamType.SOLID, 0xa00000, 0xa00000, time + 120, range * 2, 0.075F, 3, 0.025F);
        BeamPronter.prontBeam(ps, buf, vec, EnumWaveType.SPIRAL, EnumBeamType.SOLID, 0xa00000, 0xa00000, time + 240, range * 2, 0.075F, 3, 0.025F, BeamPronter.forkAlpha());
        ps.popPose();
    }

    private static void felBeam(MachineFelBlockEntity fel, float f, PoseStack ps, MultiBufferSource buf,
                                int light, int overlay, com.hbm_m.client.render.machine.MachineRenderApi api) {
        long time = fel.getLevel() != null ? fel.getLevel().getGameTime() : 0;
        int color;
        if (fel.mode.renderedBeamColor == 0) {
            color = java.awt.Color.HSBtoRGB(time / 50.0F, 0.5F, 0.1F) & 16777215;
        } else {
            color = fel.mode.renderedBeamColor;
        }
        int length = fel.distance - 3;
        if (!(fel.getEnergyStored() > MachineFelBlockEntity.powerReq * Math.pow(2, fel.mode.tier) && fel.isOn && fel.mode != EnumWavelengths.NULL && length > 0)) return;
        ps.pushPose();
        ps.translate(0, 1.5, -1.5);
        BeamPronter.prontBeamwithDepth(ps, buf, new Vec3(0, 0, -length - 1), EnumWaveType.SPIRAL, EnumBeamType.SOLID, color, color, 0, 1, 0F, 2, 0.0625F, BeamPronter.forkAlpha());
        BeamPronter.prontBeamwithDepth(ps, buf, new Vec3(0, 0, -length - 1), EnumWaveType.RANDOM, EnumBeamType.SOLID, color, color, (int) (time % 1000 / 2), (length / 2) + 1, 0.0625F, 2, 0.0625F, BeamPronter.forkAlpha());
        ps.popPose();
    }

    /** Kern (vollhell) und Blitze aus {@code ExposureChamberRenderer} (unveraendert). */
    private static void exposureHook(MachineExposureChamberBlockEntity chamber, float interp, PoseStack ps, MultiBufferSource buf,
                                     int light, int overlay, com.hbm_m.client.render.machine.MachineRenderApi api) {
        if (!chamber.isOn) return;
        double rotation = chamber.prevRotation + (chamber.rotation - chamber.prevRotation) * interp;
        long time = chamber.getLevel() == null ? 0 : chamber.getLevel().getGameTime();

        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees((float) (rotation / 2D)));
        ps.translate(0, Math.sin((time % (Math.PI * 16D) + interp) * 0.125) * 0.0625, 0);
        ExposureChamberRenderer.MODEL.renderPart("Core", ps, buf.getBuffer(RenderType.entityCutoutNoCull(ExposureChamberRenderer.TEX)), 0xF000F0);
        ps.popPose();

        int duration = 8;
        Random rand = new Random(time / duration);
        int chance = 2;
        int color = time % duration >= duration / 2 ? 0x80d0ff : 0xffffff;
        rand.nextInt(chance);
        int start = (int) (System.currentTimeMillis() % 1000) / 50;
        if (rand.nextInt(chance) == 0) {
            ps.pushPose();
            ps.translate(0, 3.675, -7.5);
            BeamPronter.prontBeam(ps, buf, new Vec3(0, 0, 5), EnumWaveType.RANDOM, EnumBeamType.LINE, color, 0xffffff, start, 15, 0.125F, 1, 0, BeamPronter.forkAlpha());
            ps.popPose();
        }
        if (rand.nextInt(chance) == 0) {
            ps.pushPose();
            ps.translate(1.1875, 2.5, -7.5);
            BeamPronter.prontBeam(ps, buf, new Vec3(0, 0, 5), EnumWaveType.RANDOM, EnumBeamType.LINE, color, 0xffffff, start, 15, 0.125F, 1, 0, BeamPronter.forkAlpha());
            ps.popPose();
        }
        if (rand.nextInt(chance) == 0) {
            ps.pushPose();
            ps.translate(-1.1875, 2.5, -7.5);
            BeamPronter.prontBeam(ps, buf, new Vec3(0, 0, 5), EnumWaveType.RANDOM, EnumBeamType.LINE, color, 0xffffff, start, 15, 0.125F, 1, 0, BeamPronter.forkAlpha());
            ps.popPose();
        }

        ps.pushPose();
        ps.translate(0, 1.75, 0);
        BeamPronter.prontBeam(ps, buf, new Vec3(0, 1.5, 0), EnumWaveType.RANDOM, EnumBeamType.LINE, 0x80d0ff, 0xffffff, start, 10, 0.125F, 1, 0, BeamPronter.forkAlpha());
        BeamPronter.prontBeam(ps, buf, new Vec3(0, 1.5, 0), EnumWaveType.RANDOM, EnumBeamType.LINE, 0x8080ff, 0xffffff, (int) (System.currentTimeMillis() + 5 % 1000) / 50, 10, 0.125F, 1, 0, BeamPronter.forkAlpha());
        ps.popPose();

        ps.pushPose();
        ps.translate(0, 2.5, 0);
        BeamPronter.prontBeam(ps, buf, new Vec3(0, 0, -1), EnumWaveType.SPIRAL, EnumBeamType.LINE, 0xffff80, 0xffffff, (int) (System.currentTimeMillis() % 360), 15, 0.125F, 1, 0, BeamPronter.forkAlpha());
        BeamPronter.prontBeam(ps, buf, new Vec3(0, 0, -1), EnumWaveType.SPIRAL, EnumBeamType.LINE, 0xff8080, 0xffffff, (int) (System.currentTimeMillis() % 360) + 180, 15, 0.125F, 1, 0, BeamPronter.forkAlpha());
        ps.popPose();
    }
}
