package com.hbm_m.client.render.implementations;

import static com.hbm_m.client.render.raptorpath.RaptorParts.idleStatic;
import static com.hbm_m.client.render.raptorpath.RaptorParts.mesh;
import static com.hbm_m.client.render.raptorpath.RaptorParts.rotY;
import static com.hbm_m.client.render.raptorpath.RaptorParts.select;
import static com.hbm_m.client.render.raptorpath.RaptorParts.when;

import com.hbm_m.block.machines.DummyableMachineBlock;
import com.hbm_m.block.machines.MachinePumpBlock;
import com.hbm_m.block.machines.MachinePyroOvenBlock;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.decorations.LanternBlockEntity;
import com.hbm_m.blockentity.machines.ForceFieldBlockEntity;
import com.hbm_m.blockentity.machines.MachineLargeTurbineBlockEntity;
import com.hbm_m.blockentity.machines.MachineLpw2BlockEntity;
import com.hbm_m.blockentity.machines.MachineMicrowaveBlockEntity;
import com.hbm_m.blockentity.machines.MachineMiningDrillBlockEntity;
import com.hbm_m.blockentity.machines.MachinePUREXBlockEntity;
import com.hbm_m.blockentity.machines.MachinePumpElectricBlockEntity;
import com.hbm_m.blockentity.machines.MachinePumpSteamBlockEntity;
import com.hbm_m.blockentity.machines.MachinePumpjackBlockEntity;
import com.hbm_m.blockentity.machines.MachinePyroOvenBlockEntity;
import com.hbm_m.blockentity.machines.MachineRadGenBlockEntity;
import com.hbm_m.blockentity.machines.MachineReactorResearchBlockEntity;
import com.hbm_m.blockentity.machines.MachineRockMillBlockEntity;
import com.hbm_m.blockentity.machines.fusion.FusionKlystronBlockEntity;
import com.hbm_m.blockentity.machines.fusion.FusionKlystronCreativeBlockEntity;
import com.hbm_m.blockentity.machines.fusion.FusionMhdtBlockEntity;
import com.hbm_m.blockentity.machines.pile.PileControlBlockEntity;
import com.hbm_m.blockentity.machines.pile.PileLoaderBlockEntity;
import com.hbm_m.blockentity.machines.pile.PileVentBlockEntity;
import com.hbm_m.blockentity.machines.rbmk.RBMKAutoloaderBlockEntity;
import com.hbm_m.client.ClientRenderHandler;
import com.hbm_m.client.render.LegacyAnimator;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.client.render.UvShiftConsumer;
import com.hbm_m.client.render.machine.MachineRenderers;
import com.hbm_m.client.render.machine.MachineSpecBuilder;
import com.hbm_m.client.render.machine.PartAnimator;
import com.hbm_m.client.render.raptorpath.ObjAtlasMesh;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.util.Vec3NT;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Render-Migration Paket P3 (Kategorie A, bewegt). Ungleichmaessig skalierte Glieder (LPW2-Federbeine),
 * UV-Laeufe auf Einzeltexturen, Farbteile und Bohrgestaenge wechselnder Laenge bleiben als immediate-Hook.
 */
//? if forge {
@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
//?} elif fabric {
/*@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
*///?} elif neoforge {
/*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
*///?}
public final class RaptorPathP3 {

    private RaptorPathP3() {}

    private static ResourceLocation rl(String p) {
        return ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, p);
    }

    private static ObjAtlasMesh p(SimpleObjModel m, ResourceLocation t, String key, String... parts) {
        return ObjAtlasMesh.parts(m, t, "hbm_m/" + key, parts);
    }

    // ForceField (RBMK-OBJ-Pfad, Blockatlas)
    private static final ObjAtlasMesh FF_BASE = ObjAtlasMesh.of("hbm_m/forcefield/base",
            b -> b.allGroups("models/forcefield/forcefield_base.obj", rl("block/machine/forcefield_base")));
    /** Kopf mit eingebackenem translate(0, 0.5 + 0.5, 0) (Y-Versatz und Y-Drehung vertauschen). */
    private static final ObjAtlasMesh FF_TOP = ObjAtlasMesh.of("hbm_m/forcefield/top", b -> {
        b.pose.translate(0D, 1D, 0D);
        b.allGroups("models/forcefield/forcefield_top.obj", rl("block/machine/forcefield_top"));
    });
    // Fusion: Klystron / MHD-Turbine (nur die bewegten Teile, Rest ist Blockmodell)
    private static final ObjAtlasMesh KLYSTRON_ROTOR = ObjAtlasMesh.groups("models/block/machines/klystron.obj", rl("block/machine/klystron"), "hbm_m/klystron/rotor", "Rotor");
    private static final ObjAtlasMesh KLYSTRON_CREATIVE_ROTOR = ObjAtlasMesh.groups("models/block/machines/klystron.obj", rl("block/machine/klystron_creative"), "hbm_m/klystron_creative/rotor", "Rotor");
    private static final ObjAtlasMesh MHDT_COILS = ObjAtlasMesh.groups("models/block/machines/mhdt.obj", rl("block/machine/mhdt"), "hbm_m/mhdt/coils", "Coils");
    // Lantern
    private static final ObjAtlasMesh LANTERN = p(LanternRenderer.LANTERN, LanternRenderer.TEX, "lantern/lantern", "Lantern");
    // Large turbine
    private static final ObjAtlasMesh LT_BODY = p(LargeTurbineRenderer.MODEL, LargeTurbineRenderer.TEX, "large_turbine/body", "Body");
    private static final ObjAtlasMesh LT_BLADES = p(LargeTurbineRenderer.MODEL, LargeTurbineRenderer.BLADES, "large_turbine/blades", "Blades");
    // Mining drill
    private static final ObjAtlasMesh MD_MAIN = p(MachineMiningDrillRenderer.MODEL, MachineMiningDrillRenderer.TEX, "mining_drill/main", "Main");
    private static final ObjAtlasMesh MD_CRUSHER1 = p(MachineMiningDrillRenderer.MODEL, MachineMiningDrillRenderer.TEX, "mining_drill/crusher1", "Crusher1");
    private static final ObjAtlasMesh MD_CRUSHER2 = p(MachineMiningDrillRenderer.MODEL, MachineMiningDrillRenderer.TEX, "mining_drill/crusher2", "Crusher2");
    private static final ObjAtlasMesh MD_DRILLBIT = p(MachineMiningDrillRenderer.MODEL, MachineMiningDrillRenderer.TEX, "mining_drill/drillbit", "Drillbit");
    // Microwave
    private static final ObjAtlasMesh MW_BODY = p(MicrowaveRenderer.MODEL, MicrowaveRenderer.TEX, "microwave/body", "mainbody_Cube.001", "window_Cube.002");
    private static final ObjAtlasMesh MW_PLATE = p(MicrowaveRenderer.MODEL, MicrowaveRenderer.TEX, "microwave/plate", "plate_Cylinder");
    // Pile
    private static final ObjAtlasMesh PC_BASE = p(PileDeviceRenderers.CONTROL, PileDeviceRenderers.CONTROL_TEX, "pile_control/base", "Base");
    private static final ObjAtlasMesh PC_ROD = p(PileDeviceRenderers.CONTROL, PileDeviceRenderers.CONTROL_TEX, "pile_control/rod", "Rod");
    private static final ObjAtlasMesh PL_LOADER = p(PileDeviceRenderers.LOADER, PileDeviceRenderers.LOADER_TEX, "pile_loader/loader", "Loader");
    private static final ObjAtlasMesh PL_LEVER = p(PileDeviceRenderers.LOADER, PileDeviceRenderers.LOADER_TEX, "pile_loader/lever", "Lever");
    private static final ObjAtlasMesh PL_SLIDER = p(PileDeviceRenderers.LOADER, PileDeviceRenderers.LOADER_TEX, "pile_loader/slider", "Slider");
    private static final ObjAtlasMesh PL_ROD = p(PileDeviceRenderers.LOADER, PileDeviceRenderers.LOADER_TEX, "pile_loader/rod", "Rod");
    private static final ObjAtlasMesh PV_PIPE = p(PileDeviceRenderers.VENT, PileDeviceRenderers.VENT_TEX, "pile_vent/pipe", "Pipe");
    private static final ObjAtlasMesh PV_FAN = p(PileDeviceRenderers.VENT, PileDeviceRenderers.VENT_TEX, "pile_vent/fan", "Fan");
    // Pump (Dampf/Strom: gleiche Geometrie, andere Textur)
    private static final ObjAtlasMesh[] PUMP_BASE = pumpSet("Base");
    private static final ObjAtlasMesh[] PUMP_ROTOR = pumpSet("Rotor");
    private static final ObjAtlasMesh[] PUMP_ARMS = pumpSet("Arms");
    private static final ObjAtlasMesh[] PUMP_PISTON = pumpSet("Piston");
    // Pumpjack
    private static final ObjAtlasMesh PJ_BASE = p(PumpjackRenderer.MODEL, PumpjackRenderer.TEX, "pumpjack/base", "Base");
    private static final ObjAtlasMesh PJ_ROTOR = p(PumpjackRenderer.MODEL, PumpjackRenderer.TEX, "pumpjack/rotor", "Rotor");
    private static final ObjAtlasMesh PJ_HEAD = p(PumpjackRenderer.MODEL, PumpjackRenderer.TEX, "pumpjack/head", "Head");
    private static final ObjAtlasMesh PJ_CARRIAGE = p(PumpjackRenderer.MODEL, PumpjackRenderer.TEX, "pumpjack/carriage", "Carriage");
    // PUREX
    private static final ObjAtlasMesh PX_BASE = p(PurexRenderer.MODEL, PurexRenderer.TEX, "purex/base", "Base");
    private static final ObjAtlasMesh PX_FRAME = p(PurexRenderer.MODEL, PurexRenderer.TEX, "purex/frame", "Frame");
    private static final ObjAtlasMesh PX_FAN = p(PurexRenderer.MODEL, PurexRenderer.TEX, "purex/fan", "Fan");
    private static final ObjAtlasMesh PX_PUMP = p(PurexRenderer.MODEL, PurexRenderer.TEX, "purex/pump", "Pump");
    // Pyro oven
    private static final ObjAtlasMesh PY_OVEN = p(PyroOvenRenderer.MODEL, PyroOvenRenderer.TEX, "pyrooven/oven", "Oven");
    private static final ObjAtlasMesh PY_SLIDER = p(PyroOvenRenderer.MODEL, PyroOvenRenderer.TEX, "pyrooven/slider", "Slider");
    private static final ObjAtlasMesh PY_FAN = p(PyroOvenRenderer.MODEL, PyroOvenRenderer.TEX, "pyrooven/fan", "Fan");
    // RBMK autoloader
    private static final ObjAtlasMesh AL_BASE = ObjAtlasMesh.groups("models/rbmk/models/autoloader.obj", rl("block/rbmk/model_rbmk_autoloader"), "hbm_m/rbmk_autoloader/base", "Base");
    private static final ObjAtlasMesh AL_PISTON = ObjAtlasMesh.groups("models/rbmk/models/autoloader.obj", rl("block/rbmk/model_rbmk_autoloader"), "hbm_m/rbmk_autoloader/piston", "Piston");
    // RadGen
    private static final ObjAtlasMesh RG_BASE = p(RadGenRenderer.MODEL, RadGenRenderer.TEX, "radgen/base", "Base");
    private static final ObjAtlasMesh RG_ROTOR = p(RadGenRenderer.MODEL, RadGenRenderer.TEX, "radgen/rotor", "Rotor");
    private static final ObjAtlasMesh RG_GLASS = p(RadGenRenderer.MODEL, RadGenRenderer.TEX, "radgen/glass", "Glass");
    // Research reactor
    private static final ObjAtlasMesh RR_BASE = ObjAtlasMesh.all(ReactorResearchRenderer.BASE, ReactorResearchRenderer.BASE_TEX, "hbm_m/reactor_research/base");
    private static final ObjAtlasMesh RR_RODS = ObjAtlasMesh.all(ReactorResearchRenderer.RODS, ReactorResearchRenderer.RODS_TEX, "hbm_m/reactor_research/rods");
    // Rock mill
    private static final ObjAtlasMesh RM_BASE = p(RockMillRenderer.MODEL, RockMillRenderer.TEX, "rockmill/base", "Base");
    private static final ObjAtlasMesh RM_FRAME = p(RockMillRenderer.MODEL, RockMillRenderer.TEX, "rockmill/frame", "Frame");
    private static final ObjAtlasMesh RM_WHEEL = p(RockMillRenderer.MODEL, RockMillRenderer.TEX, "rockmill/wheel", "Wheel");

    // LPW2: starre Glieder
    private static final String[] LPW2_RIGID = { "Frame", "Center", "Rotor", "TurbineFront", "TurbineBack", "Piston", "Engine",
            "ShroudH", "ShroudV", "Flap", "WireLeft", "WireRight", "Cover", "Server1", "Server2", "Server3", "Server4", "Monitor" };
    private static final java.util.Map<String, ObjAtlasMesh> LPW2 = new java.util.HashMap<>();

    static {
        for (String part : LPW2_RIGID) LPW2.put(part, p(Lpw2Renderer.MODEL, Lpw2Renderer.TEX, "lpw2/" + part.toLowerCase(java.util.Locale.ROOT), part));
    }

    private static ObjAtlasMesh[] pumpSet(String part) {
        return new ObjAtlasMesh[] {
                p(PumpRenderer.MODEL, PumpRenderer.STEAM_TEX, "pump_steam/" + part.toLowerCase(java.util.Locale.ROOT), part),
                p(PumpRenderer.MODEL, PumpRenderer.ELECTRIC_TEX, "pump_electric/" + part.toLowerCase(java.util.Locale.ROOT), part) };
    }

    public static void register() {
        // RenderForceField: Sockel; Kopf dreht nur bei stehendem Feld; Feldkugel (Linien) immediate
        MachineSpecBuilder<ForceFieldBlockEntity> ff = MachineRenderers.machine("hbm_m/forcefield", ModBlockEntities.FORCE_FIELD_BE.get(), ForceFieldBlockEntity.class)
                .blockTransform((be, a) -> {
                    a.translate(0.5D, 0D, 0.5D);
                    rotY(a, 180F);
                });
        mesh(ff, "base", FF_BASE);
        idleStatic(ff, "top", (be, f, t, ps) -> {
            double rot = (System.currentTimeMillis() / 10D) % 360D;
            ps.mulPose(Axis.YP.rotationDegrees((float) -rot));
            return true;
        }, RaptorPathP3::fieldUp, FF_TOP);
        ff.hook((be, f, ps, buf, light, overlay, api) -> {
            if (!fieldUp(be)) return;
            ps.pushPose();
            ps.translate(0D, 0.5D, 0D);
            int segments = (int) (16 + be.getRadius() * 0.125F);
            ForceFieldRenderer.drawSphere(ps, buf, segments, segments * 2, be.getRadius(), be.getColor());
            ps.popPose();
        }).viewDistance(256).register();

        // RenderFusionKlystron (+ Kreativ): nur der Rotor, Blockmodell zeichnet den Rest
        mesh(MachineRenderers.machine("hbm_m/fusion_klystron", ModBlockEntities.FUSION_KLYSTRON_BE.get(), FusionKlystronBlockEntity.class)
                .blockTransform((be, a) -> {
                    a.translate(0.5D, 0D, 0.5D);
                    applyFacing(be.getBlockState(), a, 90F);
                    a.translate(-1D, 0D, 0D);
                }), "rotor", (be, f, t, ps) -> klystronRotor(ps, Mth.lerp(f, be.prevFan, be.fan)), KLYSTRON_ROTOR)
                .viewDistance(128).register();
        mesh(MachineRenderers.machine("hbm_m/fusion_klystron_creative", ModBlockEntities.FUSION_KLYSTRON_CREATIVE_BE.get(), FusionKlystronCreativeBlockEntity.class)
                .blockTransform((be, a) -> {
                    a.translate(0.5D, 0D, 0.5D);
                    applyFacing(be.getBlockState(), a, 90F);
                    a.translate(-1D, 0D, 0D);
                }), "rotor", (be, f, t, ps) -> klystronRotor(ps, Mth.lerp(f, be.prevFan, be.fan)), KLYSTRON_CREATIVE_ROTOR)
                .viewDistance(128).register();

        // RenderFusionMhdt: Spulen drehen
        mesh(MachineRenderers.machine("hbm_m/fusion_mhdt", ModBlockEntities.FUSION_MHDT_BE.get(), FusionMhdtBlockEntity.class)
                .blockTransform((be, a) -> {
                    a.translate(0.5D, 0D, 0.5D);
                    applyFacing(be.getBlockState(), a, 90F);
                }), "coils", (be, f, t, ps) -> {
                    float rot = Mth.lerp(f, be.prevRotor, be.rotor) % 15F;
                    ps.translate(0D, 1.5D, 0D);
                    ps.mulPose(Axis.XP.rotationDegrees(rot));
                    ps.translate(0D, -1.5D, 0D);
                    return true;
                }, MHDT_COILS)
                .viewDistance(128).register();

        // RenderLantern: Laterne; Leuchtteil (Farbe, flackert) immediate
        mesh(MachineRenderers.machine("hbm_m/lantern", ModBlockEntities.LANTERN.get(), LanternBlockEntity.class)
                .blockTransform((be, a) -> a.translate(0.5D, 0, 0.5D)), "lantern", LANTERN)
                .hook((be, f, ps, buf, light, overlay, api) -> {
                    float mult = (float) (Math.sin(System.currentTimeMillis() / 200D) / 2 + 0.5) * 0.1F + 0.9F;
                    LanternRenderer.LANTERN.renderPartColor("Light", ps, buf.getBuffer(ClientRenderHandler.CustomRenderTypes.SOLID_COLOR_NOCULL),
                            1F * mult, 1F * mult, 0.7F * mult, 1F);
                })
                .viewDistance(256).register();

        // RenderLargeTurbine: 90 + Metadaten 2/4/3/5 -> 90/180/270/0, Versatz; Schaufeln drehen
        MachineSpecBuilder<MachineLargeTurbineBlockEntity> lt = MachineRenderers.machine("hbm_m/large_turbine", ModBlockEntities.LARGE_TURBINE_BE.get(), MachineLargeTurbineBlockEntity.class)
                .blockTransform((be, a) -> {
                    a.translate(0.5D, 0D, 0.5D);
                    rotY(a, 90);
                    switch (be.getBlockState().getValue(DummyableMachineBlock.FACING)) {
                        case NORTH -> rotY(a, 90);
                        case WEST -> rotY(a, 180);
                        case SOUTH -> rotY(a, 270);
                        default -> { }
                    }
                    a.translate(0, 0, -1);
                });
        mesh(lt, "body", LT_BODY);
        mesh(lt, "blades", (be, f, t, ps) -> {
            ps.translate(0, 1, 0);
            ps.mulPose(Axis.ZP.rotationDegrees(be.lastRotor + (be.rotor - be.lastRotor) * f));
            ps.translate(0, -1, 0);
            return true;
        }, LT_BLADES);
        lt.viewDistance(256).register();

        registerLpw2();

        // RenderMiningDrill: Brecher, Bohrkopf; Gestaenge (Anzahl nach Tiefe) und Schuettung immediate
        MachineSpecBuilder<MachineMiningDrillBlockEntity> md = MachineRenderers.machine("hbm_m/mining_drill", ModBlockEntities.MINING_DRILL_BE.get(), MachineMiningDrillBlockEntity.class)
                .blockTransform((be, a) -> {
                    a.translate(0.5D, 0D, 0.5D);
                    switch (be.getBlockState().getValue(DummyableMachineBlock.FACING)) {
                        case EAST -> rotY(a, 90);
                        case NORTH -> rotY(a, 180);
                        case WEST -> rotY(a, 270);
                        default -> { }
                    }
                    a.translate(0, -3, 0);
                });
        mesh(md, "main", MD_MAIN);
        mesh(md, "crusher1", (be, f, t, ps) -> {
            float crusher = be.prevCrusherRotation + (be.crusherRotation - be.prevCrusherRotation) * f;
            ps.translate(0.0F, 2.0F, 2.8125F);
            ps.mulPose(Axis.XP.rotationDegrees(-crusher));
            ps.translate(0.0F, -2.0F, -2.8125F);
            return true;
        }, MD_CRUSHER1);
        mesh(md, "crusher2", (be, f, t, ps) -> {
            float crusher = be.prevCrusherRotation + (be.crusherRotation - be.prevCrusherRotation) * f;
            ps.translate(0.0F, 2.0F, 2.1875F);
            ps.mulPose(Axis.XP.rotationDegrees(crusher));
            ps.translate(0.0F, -2.0F, -2.1875F);
            return true;
        }, MD_CRUSHER2);
        mesh(md, "drillbit", (be, f, t, ps) -> {
            ps.mulPose(Axis.YN.rotationDegrees(be.prevDrillRotation + (be.drillRotation - be.prevDrillRotation) * f));
            ps.translate(0.0F, -(be.prevDrillExtension + (be.drillExtension - be.prevDrillExtension) * f), 0.0F);
            return true;
        }, MD_DRILLBIT);
        md.hook(RaptorPathP3::miningDrillHook).viewDistance(256).register();

        // RenderMicrowave: Teller dreht nur waehrend des Garens
        MachineSpecBuilder<MachineMicrowaveBlockEntity> mw = MachineRenderers.machine("hbm_m/microwave", ModBlockEntities.MICROWAVE_BE.get(), MachineMicrowaveBlockEntity.class)
                .blockTransform((be, a) -> {
                    a.translate(0.5D, -0.785D, 0.5D);
                    switch (ObjBerHelper.meta(be)) {
                        case 4 -> rotY(a, 90);
                        case 3 -> rotY(a, 180);
                        case 5 -> rotY(a, 270);
                        default -> { }
                    }
                    a.translate(-0.5D, 0.0D, 0.65D);
                });
        mesh(mw, "body", MW_BODY);
        idleStatic(mw, "plate", (be, f, t, ps) -> {
            double rot = (System.currentTimeMillis() * be.speed / 10D) % 360;
            ps.translate(0.575D, 0.0D, -0.45D);
            ps.mulPose(Axis.YP.rotationDegrees((float) rot));
            ps.translate(-0.575D, 0.0D, 0.45D);
            return true;
        }, be -> be.time > 0, MW_PLATE);
        mw.viewDistance(64).register();

        // RenderPileControl / Loader / Vent
        MachineSpecBuilder<PileControlBlockEntity> pc = MachineRenderers.machine("hbm_m/pile_control", ModBlockEntities.PILE_CONTROL_BE.get(), PileControlBlockEntity.class)
                .blockTransform((be, a) -> pileOrient(a, be));
        mesh(pc, "base", PC_BASE);
        idleStatic(pc, "rod", (be, f, t, ps) -> {
            double level = be.lastRenderLevel + (be.renderLevel - be.lastRenderLevel) * f;
            ps.translate(0, level * 0.75, 0);
            return true;
        }, be -> be.lastRenderLevel != 0D || be.renderLevel != 0D, PC_ROD);
        pc.viewDistance(64).register();

        MachineSpecBuilder<PileLoaderBlockEntity> pl = MachineRenderers.machine("hbm_m/pile_loader", ModBlockEntities.PILE_LOADER_BE.get(), PileLoaderBlockEntity.class)
                .blockTransform((be, a) -> pileOrient(a, be));
        mesh(pl, "loader", PL_LOADER);
        PartAnimator<PileLoaderBlockEntity> lever = (be, f, t, ps) -> {
            double position = be.lastRenderLevel + (be.renderLevel - be.lastRenderLevel) * f;
            ps.translate(-0.1875, 0.5, 0);
            ps.mulPose(Axis.ZP.rotationDegrees((float) (position * 90)));
            ps.translate(0.1875, -0.5, 0);
            return true;
        };
        PartAnimator<PileLoaderBlockEntity> slide = (be, f, t, ps) -> {
            double position = be.lastRenderLevel + (be.renderLevel - be.lastRenderLevel) * f;
            ps.translate(position * -0.5, 0, 0);
            return true;
        };
        java.util.function.Predicate<PileLoaderBlockEntity> loaderMoving = be -> be.lastRenderLevel != 0D || be.renderLevel != 0D;
        idleStatic(pl, "lever", lever, loaderMoving, PL_LEVER);
        idleStatic(pl, "slider", slide, loaderMoving, PL_SLIDER);
        when(pl, "rod", slide, be -> loaderMoving.test(be) && !be.getStack().isEmpty(), PL_ROD);
        when(pl, "rod_idle", be -> !loaderMoving.test(be) && !be.getStack().isEmpty(), PL_ROD);
        pl.viewDistance(64).register();

        MachineSpecBuilder<PileVentBlockEntity> pv = MachineRenderers.machine("hbm_m/pile_vent", ModBlockEntities.PILE_VENT_BE.get(), PileVentBlockEntity.class)
                .blockTransform((be, a) -> pileOrient(a, be));
        mesh(pv, "pipe", PV_PIPE);
        mesh(pv, "fan", (be, f, t, ps) -> {
            float rot = be.getLastFan() + (be.getFan() - be.getLastFan()) * f;
            ps.mulPose(Axis.YP.rotationDegrees(rot));
            return true;
        }, PV_FAN);
        pv.viewDistance(64).register();

        // RenderPump (Dampf / Strom)
        registerPump("hbm_m/pump_steam", ModBlockEntities.MACHINE_PUMP_STEAM_BE.get(), MachinePumpSteamBlockEntity.class);
        registerPump("hbm_m/pump_electric", ModBlockEntities.MACHINE_PUMP_ELECTRIC_BE.get(), MachinePumpElectricBlockEntity.class);

        // RenderPumpjack: Kurbel, Kopf, Schlitten; Gestaenge/Seile (Farbflaechen) immediate
        MachineSpecBuilder<MachinePumpjackBlockEntity> pj = MachineRenderers.machine("hbm_m/pumpjack", ModBlockEntities.PUMPJACK_BE.get(), MachinePumpjackBlockEntity.class)
                .blockTransform((be, a) -> {
                    a.translate(0.5, 0, 0.5);
                    switch (ObjBerHelper.meta(be)) {
                        case 2 -> rotY(a, 90);
                        case 4 -> rotY(a, 180);
                        case 3 -> rotY(a, 270);
                        default -> { }
                    }
                });
        mesh(pj, "base", PJ_BASE);
        mesh(pj, "rotor", (be, f, t, ps) -> {
            float rotation = be.prevRot + (be.rot - be.prevRot) * f;
            ps.translate(0, 1.5, -5.5);
            ps.mulPose(Axis.XP.rotationDegrees(rotation - 90));
            ps.translate(0, -1.5, 5.5);
            return true;
        }, PJ_ROTOR);
        mesh(pj, "head", (be, f, t, ps) -> {
            float rotation = be.prevRot + (be.rot - be.prevRot) * f;
            ps.translate(0, 3.5, -3.5);
            ps.mulPose(Axis.XP.rotationDegrees((float) (Math.toDegrees(Math.sin(Math.toRadians(rotation))) * 0.25)));
            ps.translate(0, -3.5, 3.5);
            return true;
        }, PJ_HEAD);
        mesh(pj, "carriage", (be, f, t, ps) -> {
            float rotation = be.prevRot + (be.rot - be.prevRot) * f;
            ps.translate(0, -Math.sin(Math.toRadians(rotation)), 0);
            return true;
        }, PJ_CARRIAGE);
        pj.hook((be, f, ps, buf, light, overlay, api) -> pumpjackLines(be.prevRot + (be.rot - be.prevRot) * f, ps, buf))
                .viewDistance(256).register();

        // RenderPUREX: 90 + Metadaten 2/4/3/5 -> 0/90/180/270
        MachineSpecBuilder<MachinePUREXBlockEntity> px = MachineRenderers.machine("hbm_m/purex", ModBlockEntities.PUREX_BE.get(), MachinePUREXBlockEntity.class)
                .blockTransform((be, a) -> {
                    a.translate(0.5D, 0D, 0.5D);
                    rotY(a, 90);
                    switch (be.getBlockState().getValue(DummyableMachineBlock.FACING)) {
                        case WEST -> rotY(a, 90);
                        case SOUTH -> rotY(a, 180);
                        case EAST -> rotY(a, 270);
                        default -> { }
                    }
                });
        mesh(px, "base", PX_BASE);
        when(px, "frame", be -> be.frame, PX_FRAME);
        mesh(px, "fan", (be, f, t, ps) -> {
            float anim = be.prevAnim + (be.anim - be.prevAnim) * f;
            ps.translate(1.5, 1.25, 0);
            ps.mulPose(Axis.ZP.rotationDegrees(anim * 45));
            ps.translate(-1.5, -1.25, 0);
            return true;
        }, PX_FAN);
        mesh(px, "pump", (be, f, t, ps) -> {
            float anim = be.prevAnim + (be.anim - be.prevAnim) * f;
            ps.translate(sps(anim * 0.25) * 0.5, 0, 0);
            return true;
        }, PX_PUMP);
        px.viewDistance(256).register();

        // RenderPyroOven: Metadaten 2/4/3/5 -> 180/270/0/90
        MachineSpecBuilder<MachinePyroOvenBlockEntity> py = MachineRenderers.machine("hbm_m/pyrooven", ModBlockEntities.PYROOVEN_BE.get(), MachinePyroOvenBlockEntity.class)
                .blockTransform((be, a) -> {
                    a.translate(0.5D, 0D, 0.5D);
                    switch (be.getBlockState().getValue(MachinePyroOvenBlock.FACING)) {
                        case NORTH -> rotY(a, 180);
                        case WEST -> rotY(a, 270);
                        case EAST -> rotY(a, 90);
                        default -> { }
                    }
                });
        mesh(py, "oven", PY_OVEN);
        mesh(py, "slider", (be, f, t, ps) -> {
            float anim = be.prevAnim + (be.anim - be.prevAnim) * f;
            ps.translate(Math.sin(Math.PI / 2D * Math.cos(anim * 0.125)) / 2 - 0.5, 0, 0);
            return true;
        }, PY_SLIDER);
        mesh(py, "fan", (be, f, t, ps) -> {
            float anim = be.prevAnim + (be.anim - be.prevAnim) * f;
            ps.translate(1.5, 0, 1.5);
            ps.mulPose(Axis.YP.rotationDegrees((float) (anim * 45D % 360D)));
            ps.translate(-1.5, 0, -1.5);
            return true;
        }, PY_FAN);
        py.viewDistance(256).register();

        // RenderRBMKAutoloader: Kolben faehrt (p = 0 oben)
        MachineSpecBuilder<RBMKAutoloaderBlockEntity> al = MachineRenderers.machine("hbm_m/rbmk_autoloader", ModBlockEntities.RBMK_AUTOLOADER_BE.get(), RBMKAutoloaderBlockEntity.class)
                .blockTransform((be, a) -> a.translate(0.5, 0, 0.5));
        mesh(al, "base", AL_BASE);
        mesh(al, "piston", (be, f, t, ps) -> {
            double pp = be.lastPiston + (be.piston - be.lastPiston) * f;
            ps.translate(0, 4.0 - pp * 4.0, 0);
            return true;
        }, AL_PISTON);
        al.viewDistance(64).register();

        // RenderRadGen: Rotor nur im Betrieb; Lampe und Glasschimmer (Farbe/Transparenz) immediate
        MachineSpecBuilder<MachineRadGenBlockEntity> rg = MachineRenderers.machine("hbm_m/radgen", ModBlockEntities.RADGEN_BE.get(), MachineRadGenBlockEntity.class)
                .blockTransform((be, a) -> {
                    a.translate(0.5D, 0D, 0.5D);
                    switch (be.getBlockState().getValue(DummyableMachineBlock.FACING)) {
                        case NORTH -> rotY(a, 90);
                        case WEST -> rotY(a, 180);
                        case SOUTH -> rotY(a, 270);
                        default -> { }
                    }
                });
        mesh(rg, "base", RG_BASE);
        idleStatic(rg, "rotor", (be, f, t, ps) -> {
            ps.translate(0, 1.5, 0);
            ps.mulPose(Axis.XP.rotationDegrees((System.currentTimeMillis() % 3600) * -0.1F));
            ps.translate(0, -1.5, 0);
            return true;
        }, MachineRadGenBlockEntity::isOn, RG_ROTOR);
        mesh(rg, "glass", RG_GLASS);
        rg.hook((be, f, ps, buf, light, overlay, api) -> {
            if (be.isOn()) RadGenRenderer.MODEL.renderPartColor("Light", ps, buf.getBuffer(ClientRenderHandler.CustomRenderTypes.SOLID_COLOR_NOCULL), 0F, 1F, 0F, 1F);
            else RadGenRenderer.MODEL.renderPartColor("Light", ps, buf.getBuffer(ClientRenderHandler.CustomRenderTypes.SOLID_COLOR_NOCULL), 0F, 0.1F, 0F, 1F);
            RadGenRenderer.MODEL.renderPartColor("Glass", ps, buf.getBuffer(ClientRenderHandler.CustomRenderTypes.TRANSLUCENT_COLOR_NOCULL), 0.5F, 0.75F, 1F, 0.3F);
        }).viewDistance(256).register();

        // RenderReactorResearch: Stabpaket faehrt; Tscherenkow-Leuchten immediate
        MachineSpecBuilder<MachineReactorResearchBlockEntity> rr = MachineRenderers.machine("hbm_m/reactor_research", ModBlockEntities.REACTOR_RESEARCH_BE.get(), MachineReactorResearchBlockEntity.class)
                .blockTransform((be, a) -> {
                    a.translate(0.5D, 0D, 0.5D);
                    rotY(a, 180);
                });
        mesh(rr, "base", RR_BASE);
        idleStatic(rr, "rods", (be, f, t, ps) -> {
            double level = be.lastLevel + (be.level - be.lastLevel) * f;
            ps.translate(0.0D, level, 0.0D);
            return true;
        }, be -> be.lastLevel != 0 || be.level != 0, RR_RODS);
        rr.hook((be, f, ps, buf, light, overlay, api) -> researchCherenkov(be, ps, buf)).viewDistance(256).register();

        // RenderRockMill: 90 + Metadaten; Mahlrad dreht
        MachineSpecBuilder<MachineRockMillBlockEntity> rm = MachineRenderers.machine("hbm_m/rockmill", ModBlockEntities.ROCKMILL_BE.get(), MachineRockMillBlockEntity.class)
                .blockTransform((be, a) -> {
                    a.translate(0.5D, 0D, 0.5D);
                    rotY(a, 90);
                    switch (be.getBlockState().getValue(DummyableMachineBlock.FACING)) {
                        case WEST -> rotY(a, 90);
                        case SOUTH -> rotY(a, 180);
                        case EAST -> rotY(a, 270);
                        default -> { }
                    }
                });
        mesh(rm, "base", RM_BASE);
        when(rm, "frame", be -> be.frame, RM_FRAME);
        mesh(rm, "wheel", (be, f, t, ps) -> {
            float rot = be.prevRotation + (be.rotation - be.prevRotation) * f;
            ps.mulPose(Axis.YN.rotationDegrees(rot));
            return true;
        }, RM_WHEEL);
        rm.viewDistance(256).register();
    }

    // ── LPW2 ─────────────────────────────────────────────────────────────────────────────

    /** Animationsgroessen aus {@code Lpw2Renderer.render} (gleiche Formeln). */
    private static final class Lpw2 {
        double sway, h, v, piston, rotor, turbine, cover, sx, sy, scroll;

        static Lpw2 of(MachineLpw2BlockEntity te, float interp) {
            Lpw2 p = new Lpw2();
            long time = te.getLevel() == null ? 0 : te.getLevel().getGameTime() % 1000000;
            double t = te.animationTime(time, interp);
            double swayTimer = (t / 3D) % (Math.PI * 4);
            p.sway = (Math.sin(swayTimer) + Math.sin(swayTimer * 2) + Math.sin(swayTimer * 4) + 2.23255D) * 0.5;
            double bellTimer = (t / 5D) % (Math.PI * 4);
            p.h = (Math.sin(bellTimer + Math.PI) + Math.sin(bellTimer * 1.5D)) / 1.90596D;
            p.v = (Math.sin(bellTimer) + Math.sin(bellTimer * 1.5D)) / 1.90596D;
            double pistonTimer = (t / 5D) % (Math.PI * 2);
            p.piston = sps(pistonTimer);
            double rotorTimer = (t / 5D) % (Math.PI * 16);
            p.rotor = (sps(rotorTimer) + rotorTimer / 2D - 1) / 25.1327412287D;
            p.turbine = (t % 100) / 100D;
            double coverTimer = (t / 5D) % (Math.PI * 4);
            p.cover = (Math.sin(coverTimer) + Math.sin(coverTimer * 2) + Math.sin(coverTimer * 4)) * 0.5;
            double serverTimer = (t / 2D) % (Math.PI * 4);
            p.sx = (Math.sin(serverTimer + Math.PI) + Math.sin(serverTimer * 1.5D)) / 1.90596D;
            p.sy = (Math.sin(serverTimer) + Math.sin(serverTimer * 1.5D)) / 1.90596D;
            double errorTimer = ((time + interp) / 3D);
            p.scroll = (sps(errorTimer) + errorTimer / 2D) % 1;
            return p;
        }
    }

    private static final double SERVER_SWAY = 0.0625D * 0.25D;

    private static void registerLpw2() {
        MachineSpecBuilder<MachineLpw2BlockEntity> b = MachineRenderers.machine("hbm_m/lpw2", ModBlockEntities.LPW2_BE.get(), MachineLpw2BlockEntity.class)
                .blockTransform((be, a) -> {
                    a.translate(0.5D, 0D, 0.5D);
                    switch (be.getBlockState().getValue(DummyableMachineBlock.FACING)) {
                        case NORTH -> rotY(a, 90);
                        case WEST -> rotY(a, 180);
                        case SOUTH -> rotY(a, 270);
                        default -> { }
                    }
                });
        mesh(b, "frame", LPW2.get("Frame"));
        // renderMainAssembly
        mesh(b, "center", (be, f, t, ps) -> { mainSway(ps, Lpw2.of(be, f)); return true; }, LPW2.get("Center"));
        mesh(b, "rotor", (be, f, t, ps) -> {
            Lpw2 p = Lpw2.of(be, f);
            mainSway(ps, p);
            ps.translate(0, 3.5, 0);
            ps.mulPose(Axis.ZN.rotationDegrees((float) (p.rotor * 360)));
            ps.translate(0, -3.5, 0);
            return true;
        }, LPW2.get("Rotor"));
        mesh(b, "turbine_front", (be, f, t, ps) -> {
            Lpw2 p = Lpw2.of(be, f);
            mainSway(ps, p);
            ps.translate(0, 3.5, 0);
            ps.mulPose(Axis.ZP.rotationDegrees((float) (p.turbine * 360)));
            ps.translate(0, -3.5, 0);
            return true;
        }, LPW2.get("TurbineFront"));
        mesh(b, "turbine_back", (be, f, t, ps) -> {
            Lpw2 p = Lpw2.of(be, f);
            mainSway(ps, p);
            ps.translate(0, 3.5, 0);
            ps.mulPose(Axis.ZN.rotationDegrees((float) (p.turbine * 360)));
            ps.translate(0, -3.5, 0);
            return true;
        }, LPW2.get("TurbineBack"));
        mesh(b, "piston", (be, f, t, ps) -> {
            Lpw2 p = Lpw2.of(be, f);
            mainSway(ps, p);
            ps.translate(0, 0, p.piston * 0.375D + 0.375D);
            return true;
        }, LPW2.get("Piston"));
        mesh(b, "engine", (be, f, t, ps) -> {
            Lpw2 p = Lpw2.of(be, f);
            mainSway(ps, p);
            ps.translate(0, 3.5, 2.75);
            double magnitude = 2D;
            ps.mulPose(Axis.YP.rotationDegrees((float) (p.v * magnitude)));
            ps.mulPose(Axis.XP.rotationDegrees((float) (p.h * magnitude)));
            ps.translate(0, -3.5, -2.75);
            return true;
        }, LPW2.get("Engine"));
        // renderShroud (starre Teile; Federbeine skaliert -> Hook)
        mesh(b, "shroud_h", (be, f, t, ps) -> { ps.translate(0, -Lpw2.of(be, f).h * 0.125D, 0); return true; }, LPW2.get("ShroudH"));
        double[][] flapsH = { { 90 + 22.5D, 1 }, { 90 - 22.5D, 1 }, { 270 + 22.5D, -1 }, { 270 - 22.5D, -1 } };
        for (int i = 0; i < flapsH.length; i++) {
            final double pos = flapsH[i][0], sign = flapsH[i][1];
            mesh(b, "flap_h" + i, (be, f, t, ps) -> {
                Lpw2 p = Lpw2.of(be, f);
                ps.translate(0, -p.h * 0.125D, 0);
                flap(ps, pos, 5D * sign * p.v + 10D);
                return true;
            }, LPW2.get("Flap"));
        }
        mesh(b, "shroud_v", (be, f, t, ps) -> { ps.translate(Lpw2.of(be, f).v * 0.125D, 0, 0); return true; }, LPW2.get("ShroudV"));
        double[][] flapsV = { { 22.5D, 1 }, { -22.5D, 1 }, { 180 + 22.5D, -1 }, { 180 - 22.5D, -1 } };
        for (int i = 0; i < flapsV.length; i++) {
            final double pos = flapsV[i][0], sign = flapsV[i][1];
            mesh(b, "flap_v" + i, (be, f, t, ps) -> {
                Lpw2 p = Lpw2.of(be, f);
                ps.translate(p.v * 0.125D, 0, 0);
                flap(ps, pos, 5D * sign * p.h + 10D);
                return true;
            }, LPW2.get("Flap"));
        }
        mesh(b, "wire_left", (be, f, t, ps) -> {
            ps.translate(-2.9375, 0, 2.375);
            ps.mulPose(Axis.YP.rotationDegrees((float) (Lpw2.of(be, f).sway * 10)));
            ps.translate(2.9375, 0, -2.375);
            return true;
        }, LPW2.get("WireLeft"));
        mesh(b, "wire_right", (be, f, t, ps) -> {
            ps.translate(2.9375, 0, 2.375);
            ps.mulPose(Axis.YP.rotationDegrees((float) (Lpw2.of(be, f).sway * -10)));
            ps.translate(-2.9375, 0, -2.375);
            return true;
        }, LPW2.get("WireRight"));
        mesh(b, "cover", (be, f, t, ps) -> { ps.translate(0, 0, -Lpw2.of(be, f).cover * 0.125); return true; }, LPW2.get("Cover"));
        mesh(b, "server1", (be, f, t, ps) -> { Lpw2 p = Lpw2.of(be, f); ps.translate(p.sx * SERVER_SWAY, 0, p.sy * SERVER_SWAY); return true; }, LPW2.get("Server1"));
        mesh(b, "server2", (be, f, t, ps) -> { Lpw2 p = Lpw2.of(be, f); ps.translate(-p.sy * SERVER_SWAY, 0, p.sx * SERVER_SWAY); return true; }, LPW2.get("Server2"));
        mesh(b, "server3", (be, f, t, ps) -> { Lpw2 p = Lpw2.of(be, f); ps.translate(p.sy * SERVER_SWAY, 0, -p.sx * SERVER_SWAY); return true; }, LPW2.get("Server3"));
        mesh(b, "server4", (be, f, t, ps) -> { Lpw2 p = Lpw2.of(be, f); ps.translate(-p.sx * SERVER_SWAY, 0, -p.sy * SERVER_SWAY); return true; }, LPW2.get("Server4"));
        mesh(b, "monitor", (be, f, t, ps) -> { Lpw2 p = Lpw2.of(be, f); ps.translate(p.sy * SERVER_SWAY, 0, p.sx * SERVER_SWAY); return true; }, LPW2.get("Monitor"));
        b.hook((be, f, ps, buf, light, overlay, api) -> lpw2Hook(be, f, ps, buf, light));
        b.viewDistance(256).register();
    }

    private static void mainSway(PoseStack ps, Lpw2 p) {
        ps.translate(0, 0, -p.sway * 0.125);
    }

    /** {@code Lpw2Renderer.renderFlap}. */
    private static void flap(PoseStack ps, double position, double rotation) {
        ps.translate(0, 3.5D, 0);
        ps.mulPose(Axis.ZP.rotationDegrees((float) position));
        ps.translate(0, -3.5D, 0);
        ps.translate(0, 6.96875D, 8.5D);
        ps.mulPose(Axis.XP.rotationDegrees((float) rotation));
        ps.translate(0, -6.96875D, -8.5D);
    }

    /** Ungleichmaessig skalierte Federbeine und der Fehlerbildschirm (UV-Lauf) wie im alten BER. */
    private static void lpw2Hook(MachineLpw2BlockEntity te, float interp, PoseStack ps, MultiBufferSource buf, int light) {
        Lpw2 p = Lpw2.of(te, interp);
        VertexConsumer vc = buf.getBuffer(RenderType.entityCutout(Lpw2Renderer.TEX));
        SimpleObjModel m = Lpw2Renderer.MODEL;

        ps.pushPose();
        ps.translate(0, 0, 3.5);
        ps.scale(1, 1, (float) ((3 + p.cover * 0.125) / 3));
        ps.translate(0, 0, -3.5);
        m.renderPart("SuspensionCoverFront", ps, vc, light);
        ps.popPose();

        ps.pushPose();
        ps.translate(0, 0, -5.5);
        ps.scale(1, 1, (float) ((1.5 - p.cover * 0.125) / 1.5));
        ps.translate(0, 0, 5.5);
        m.renderPart("SuspensionCoverBack", ps, vc, light);
        ps.popPose();

        ps.pushPose();
        ps.translate(0, 0, -9);
        ps.scale(1, 1, (float) ((1.25 - p.sway * 0.125) / 1.25));
        ps.translate(0, 0, 9);
        m.renderPart("SuspensionBackOuter", ps, vc, light);
        ps.popPose();

        ps.pushPose();
        ps.translate(0, 0, -9.5);
        ps.scale(1, 1, (float) ((1.75 - p.sway * 0.125) / 1.75));
        ps.translate(0, 0, 9.5);
        m.renderPart("SuspensionBackCenter", ps, vc, light);
        ps.popPose();

        // renderShroud: Federbeine
        double magnitude = 0.125D;
        double length = 0.6875D;

        ps.pushPose();
        ps.translate(-2.625D, 0, 0);
        ps.scale((float) ((length + p.v * magnitude) / length), 1, 1);
        ps.translate(2.625D, 0, 0);
        m.renderPart("SuspensionLeft", ps, vc, light);
        ps.popPose();

        ps.pushPose();
        ps.translate(2.625D, 0, 0);
        ps.scale((float) ((length - p.v * magnitude) / length), 1, 1);
        ps.translate(-2.625D, 0, 0);
        m.renderPart("SuspensionRight", ps, vc, light);
        ps.popPose();

        ps.pushPose();
        ps.translate(0, 6.125D, 0);
        ps.scale(1, (float) ((length + p.h * magnitude) / length), 1);
        ps.translate(0, -6.125D, 0);
        m.renderPart("SuspensionTop", ps, vc, light);
        ps.popPose();

        ps.pushPose();
        ps.translate(0, 0.875D, 0);
        ps.scale(1, (float) ((length - p.h * magnitude) / length), 1);
        ps.translate(0, -0.875D, 0);
        m.renderPart("SuspensionBottom", ps, vc, light);
        ps.popPose();

        ps.pushPose();
        ps.translate(p.sy * SERVER_SWAY, 0, p.sx * SERVER_SWAY);
        m.renderPart("Screen", ps, new UvShiftConsumer(buf.getBuffer(RenderType.entityCutout(Lpw2Renderer.ERROR_TEX)), 0F, (float) p.scroll), light);
        ps.popPose();
    }

    // ── weitere Hilfen ───────────────────────────────────────────────────────────────────

    private static boolean fieldUp(ForceFieldBlockEntity be) {
        return be.isFieldOn() && be.getEnergyStored() > 0;
    }

    /** {@code FusionTorusRenderer.applyFacing(state, pose, baseYaw)} auf den Blocktransform. */
    private static void applyFacing(BlockState state, LegacyAnimator a, float baseYaw) {
        if (!state.hasProperty(HorizontalDirectionalBlock.FACING)) {
            if (baseYaw != 0F) rotY(a, baseYaw);
            return;
        }
        Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
        float rot = switch (facing) {
            case SOUTH -> 180F;
            case EAST -> 270F;
            case WEST -> 90F;
            default -> 0F;
        } + baseYaw;
        if (rot != 0F) rotY(a, rot);
    }

    private static boolean klystronRotor(PoseStack ps, float rot) {
        ps.translate(0D, 2.5D, 0D);
        ps.mulPose(Axis.XP.rotationDegrees(rot));
        ps.translate(0D, -2.5D, 0D);
        return true;
    }

    private static void pileOrient(LegacyAnimator a, net.minecraft.world.level.block.entity.BlockEntity be) {
        a.translate(0.5, 0, 0.5);
        switch (ObjBerHelper.facing(be)) {
            case NORTH -> rotY(a, 90);
            case SOUTH -> rotY(a, 270);
            case WEST -> rotY(a, 180);
            default -> { }
        }
    }

    private static <T extends net.minecraft.world.level.block.entity.BlockEntity> void registerPump(
            String id, net.minecraft.world.level.block.entity.BlockEntityType<T> type, Class<T> cls) {
        MachineSpecBuilder<T> b = MachineRenderers.machine(id, type, cls)
                .blockTransform((be, a) -> {
                    a.translate(0.5D, 0D, 0.5D);
                    switch (be.getBlockState().getValue(DummyableMachineBlock.FACING)) {
                        case SOUTH -> rotY(a, 90);
                        case EAST -> rotY(a, 180);
                        case NORTH -> rotY(a, 270);
                        default -> { }
                    }
                });
        select(b, "base", be -> PUMP_BASE[pumpElectric(be)]);
        select(b, "rotor", (be, f, t, ps) -> {
            double rot = pumpAngle(be, f);
            ps.translate(0, 2.25, 0);
            ps.mulPose(Axis.ZP.rotationDegrees((float) (rot - 90)));
            ps.translate(0, -2.25, 0);
            return true;
        }, be -> PUMP_ROTOR[pumpElectric(be)]);
        select(b, "arms", (be, f, t, ps) -> {
            double rot = pumpAngle(be, f);
            double sin = Math.sin(rot * Math.PI / 180D) * 0.5D - 0.5D;
            double cos = Math.cos(rot * Math.PI / 180D) * 0.5D;
            double ang = Math.acos(cos / 2D);
            double cath = Math.sqrt(1 + (cos * cos) / 2);
            ps.translate(0, 1 - cath + sin, 0);
            ps.translate(0, 4.75, 0);
            ps.mulPose(Axis.ZN.rotationDegrees((float) (ang * 180D / Math.PI - 90D)));
            ps.translate(0, -4.75, 0);
            return true;
        }, be -> PUMP_ARMS[pumpElectric(be)]);
        select(b, "piston", (be, f, t, ps) -> {
            double rot = pumpAngle(be, f);
            double sin = Math.sin(rot * Math.PI / 180D) * 0.5D - 0.5D;
            double cos = Math.cos(rot * Math.PI / 180D) * 0.5D;
            double cath = Math.sqrt(1 + (cos * cos) / 2);
            ps.translate(0, 1 - cath + sin, 0);
            return true;
        }, be -> PUMP_PISTON[pumpElectric(be)]);
        b.viewDistance(256).register();
    }

    /** {@code PumpRenderer.render}: Rotorwinkel aus dem jeweiligen BE. */
    private static float pumpAngle(net.minecraft.world.level.block.entity.BlockEntity tile, float interp) {
        float rotor = 0, lastRotor = 0;
        if (tile instanceof com.hbm_m.blockentity.machines.PumpBlockEntity pp) { rotor = pp.rotor; lastRotor = pp.lastRotor; }
        if (tile instanceof MachinePumpElectricBlockEntity pp) { rotor = pp.rotor; lastRotor = pp.lastRotor; }
        return lastRotor + (rotor - lastRotor) * interp;
    }

    private static int pumpElectric(net.minecraft.world.level.block.entity.BlockEntity tile) {
        return tile.getBlockState().getBlock() instanceof MachinePumpBlock pump && pump.isElectric() ? 1 : 0;
    }

    /** {@code BobMathUtil.sps}. */
    private static double sps(double x) {
        return Math.sin(Math.PI / 2D * Math.cos(x));
    }

    /** Gestaenge und Schuettung aus {@code MachineMiningDrillRenderer} (Gestaenge: Anzahl nach Bohrtiefe). */
    private static void miningDrillHook(MachineMiningDrillBlockEntity drill, float interp, PoseStack ps, MultiBufferSource buf,
                                        int light, int overlay, com.hbm_m.client.render.machine.MachineRenderApi api) {
        VertexConsumer vc = buf.getBuffer(RenderType.entityCutout(MachineMiningDrillRenderer.TEX));
        ps.pushPose();
        ps.mulPose(Axis.YN.rotationDegrees(drill.prevDrillRotation + (drill.drillRotation - drill.prevDrillRotation) * interp));
        float ext = drill.prevDrillExtension + (drill.drillExtension - drill.prevDrillExtension) * interp;
        ps.translate(0.0F, -ext, 0.0F);
        while (ext >= -1.5) {
            MachineMiningDrillRenderer.MODEL.renderPart("Shaft", ps, vc, light);
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

            VertexConsumer cob = buf.getBuffer(RenderType.entityCutoutNoCull(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/chute_cobblestone.png")));
            float wx = 0.125F, wz = 0.125F;
            MachineMiningDrillRenderer.quad(cob, m, n, light, 0, 0, 1, wx, 3, 2.5F + wz, 0, dropU, -wx, 3, 2.5F + wz, 1, dropU, -wx, 2, 2.5F + wz, 1, dropL, wx, 2, 2.5F + wz, 0, dropL);
            MachineMiningDrillRenderer.quad(cob, m, n, light, 0, 0, -1, -wx, 3, 2.5F - wz, 1, dropU, wx, 3, 2.5F - wz, 0, dropU, wx, 2, 2.5F - wz, 0, dropL, -wx, 2, 2.5F - wz, 1, dropL);
            MachineMiningDrillRenderer.quad(cob, m, n, light, -1, 0, 0, -wx, 3, 2.5F + wz, 0, dropU, -wx, 3, 2.5F - wz, 1, dropU, -wx, 2, 2.5F - wz, 1, dropL, -wx, 2, 2.5F + wz, 0, dropL);
            MachineMiningDrillRenderer.quad(cob, m, n, light, 1, 0, 0, wx, 3, 2.5F - wz, 1, dropU, wx, 3, 2.5F + wz, 0, dropU, wx, 2, 2.5F + wz, 0, dropL, wx, 2, 2.5F - wz, 1, dropL);

            boolean smoosh = drill.enableCrusher;
            wx = smoosh ? 0.5F : 0.25F;
            wz = 0.0625F;
            float uU = smoosh ? 4 : 2;
            float uL = 0.5F;
            VertexConsumer fall = buf.getBuffer(RenderType.entityCutoutNoCull(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID,
                    smoosh ? "textures/models/chute_gravel.png" : "textures/models/chute_cobblestone.png")));
            MachineMiningDrillRenderer.quad(fall, m, n, light, 0, 0, 1, wx, 2, 2.5F + wz, 0, dropU, -wx, 2, 2.5F + wz, uU, dropU, -wx, 1, 2.5F + wz, uU, dropL, wx, 1, 2.5F + wz, 0, dropL);
            MachineMiningDrillRenderer.quad(fall, m, n, light, 0, 0, -1, -wx, 2, 2.5F - wz, uU, dropU, wx, 2, 2.5F - wz, 0, dropU, wx, 1, 2.5F - wz, 0, dropL, -wx, 1, 2.5F - wz, uU, dropL);
            MachineMiningDrillRenderer.quad(fall, m, n, light, -1, 0, 0, -wx, 2, 2.5F + wz, 0, dropU, -wx, 2, 2.5F - wz, uL, dropU, -wx, 1, 2.5F - wz, uL, dropL, -wx, 1, 2.5F + wz, 0, dropL);
            MachineMiningDrillRenderer.quad(fall, m, n, light, 1, 0, 0, wx, 2, 2.5F - wz, uL, dropU, wx, 2, 2.5F + wz, 0, dropU, wx, 1, 2.5F + wz, 0, dropL, wx, 1, 2.5F - wz, uL, dropL);
        }
    }

    /** Gestaenge/Seile der Pferdekopfpumpe aus {@code PumpjackRenderer} (Farbflaechen, unveraendert). */
    private static void pumpjackLines(float rotation, PoseStack ps, MultiBufferSource buf) {
        Vec3NT backPos = Vec3NT.createVectorHelper(0, 0, -2);
        backPos.rotateAroundX(-(float) Math.sin(Math.toRadians(rotation)) * 0.25F);
        Vec3NT rot = Vec3NT.createVectorHelper(0, 0.5, 0);
        rot.rotateAroundX(-(float) Math.toRadians(rotation - 90));

        VertexConsumer lines = buf.getBuffer(ClientRenderHandler.CustomRenderTypes.SOLID_COLOR_NOCULL);
        org.joml.Matrix4f m = ps.last().pose();

        float c = 0.5F;
        for (int i = -1; i <= 1; i += 2) {
            PumpjackRenderer.quad(lines, m, c, c, c,
                    0.53125 * i, 1.5 + rot.yCoord, -5.5 + rot.zCoord - 0.0625D,
                    0.53125 * i, 1.5 + rot.yCoord, -5.5 + rot.zCoord + 0.0625D,
                    0.53125 * i, 3.5 + backPos.yCoord, -3.5 + backPos.zCoord + 0.0625D,
                    0.53125 * i, 3.5 + backPos.yCoord, -3.5 + backPos.zCoord - 0.0625D);
        }

        c = 0.2F;
        double pd = 0.03125D;
        double width = 0.25D;
        double height = -Math.sin(Math.toRadians(rotation));

        for (int i = -1; i <= 1; i += 2) {
            float pRot = -(float) (Math.sin(Math.toRadians(rotation)) * 0.25);
            Vec3NT frontPos = Vec3NT.createVectorHelper(0, 0, 1);
            frontPos.rotateAroundX(pRot);
            double dist = 0.03125D;
            Vec3NT frontRad = Vec3NT.createVectorHelper(0, 0, 2.5 + dist);
            double cutlet = 360D / 32D;
            frontRad.rotateAroundX(pRot);
            frontRad.rotateAroundX(-(float) Math.toRadians(cutlet * -3));

            for (int j = 0; j < 4; j++) {
                double sumY = frontPos.yCoord + frontRad.yCoord;
                double sumZ = frontPos.zCoord + frontRad.zCoord;
                if (frontRad.yCoord < 0) sumZ = 3.5 + dist * 0.5;
                double ax = (width - pd) * i, ay = 3.5 + sumY, az = -3.5 + sumZ;
                double bx = (width + pd) * i;
                frontRad.rotateAroundX(-(float) Math.toRadians(cutlet));
                sumY = frontPos.yCoord + frontRad.yCoord;
                sumZ = frontPos.zCoord + frontRad.zCoord;
                if (frontRad.yCoord < 0) sumZ = 3.5 + dist * 0.5;
                PumpjackRenderer.quad(lines, m, c, c, c,
                        ax, ay, az,
                        bx, ay, az,
                        (width + pd) * i, 3.5 + sumY, -3.5 + sumZ,
                        (width - pd) * i, 3.5 + sumY, -3.5 + sumZ);
            }

            double sumY = frontPos.yCoord + frontRad.yCoord;
            double sumZ = frontPos.zCoord + frontRad.zCoord;
            if (frontRad.yCoord < 0) sumZ = 3.5 + dist * 0.5;
            PumpjackRenderer.quad(lines, m, c, c, c,
                    (width + pd) * i, 3.5 + sumY, -3.5 + sumZ,
                    (width - pd) * i, 3.5 + sumY, -3.5 + sumZ,
                    (width - pd) * i, 2 + height, 0,
                    (width + pd) * i, 2 + height, 0);
        }

        double pp = 0.03125D;
        PumpjackRenderer.quad(lines, m, c, c, c,
                pp, height + 1.5, pp,
                -pp, height + 1.5, -pp,
                -pp, 0.75, -pp,
                pp, 0.75, pp);
        PumpjackRenderer.quad(lines, m, c, c, c,
                -pp, height + 1.5, pp,
                pp, height + 1.5, -pp,
                pp, 0.75, -pp,
                -pp, 0.75, pp);
    }

    /** Tscherenkow-Leuchten aus {@code ReactorResearchRenderer} (unveraendert). */
    private static void researchCherenkov(MachineReactorResearchBlockEntity reactor, PoseStack ps, MultiBufferSource buf) {
        if (!(reactor.totalFlux > 10 && reactor.isSubmerged())) return;
        VertexConsumer vc = buf.getBuffer(ClientRenderHandler.CustomRenderTypes.RBMK_CHERENKOV);
        org.joml.Matrix4f m = ps.last().pose();
        for (double dd = 0.285; dd < 0.7; dd += 0.025) {
            float d = (float) dd;
            float a = 0.025F + (float) (Math.random() * 0.015F) + (0.125F * reactor.totalFlux / 1000F);
            float top = 1.375F;
            float bottom = 1.375F;
            ReactorResearchRenderer.quad(vc, m, a, d, bottom - d, -d, d, top + d, -d, d, top + d, d, d, bottom - d, d);
            ReactorResearchRenderer.quad(vc, m, a, -d, bottom - d, -d, -d, top + d, -d, -d, top + d, d, -d, bottom - d, d);
            ReactorResearchRenderer.quad(vc, m, a, -d, bottom - d, d, -d, top + d, d, d, top + d, d, d, bottom - d, d);
            ReactorResearchRenderer.quad(vc, m, a, -d, bottom - d, -d, -d, top + d, -d, d, top + d, -d, d, bottom - d, -d);
            ReactorResearchRenderer.quad(vc, m, a, -d, top + d, -d, -d, top + d, d, d, top + d, d, d, top + d, -d);
            ReactorResearchRenderer.quad(vc, m, a, -d, bottom - d, -d, -d, bottom - d, d, d, bottom - d, d, d, bottom - d, -d);
        }
    }
}
