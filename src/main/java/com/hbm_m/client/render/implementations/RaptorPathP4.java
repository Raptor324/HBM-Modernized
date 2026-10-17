package com.hbm_m.client.render.implementations;

import static com.hbm_m.client.render.raptorpath.RaptorParts.idleStatic;
import static com.hbm_m.client.render.raptorpath.RaptorParts.mesh;
import static com.hbm_m.client.render.raptorpath.RaptorParts.rotY;
import static com.hbm_m.client.render.raptorpath.RaptorParts.select;
import static com.hbm_m.client.render.raptorpath.RaptorParts.when;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

import com.hbm_m.block.machines.DummyableMachineBlock;
import com.hbm_m.block.machines.MachineCombustionEngineBlock;
import com.hbm_m.block.machines.MachineThresherBlock;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.CargoElevatorBlockEntity;
import com.hbm_m.blockentity.machines.ChargerBlockEntity;
import com.hbm_m.blockentity.machines.MachineArcFurnaceBlockEntity;
import com.hbm_m.blockentity.machines.MachineChungusBlockEntity;
import com.hbm_m.blockentity.machines.MachineCombinationOvenBlockEntity;
import com.hbm_m.blockentity.machines.MachineCombustionEngineBlockEntity;
import com.hbm_m.blockentity.machines.MachineCrucibleBlockEntity;
import com.hbm_m.blockentity.machines.MachineCyclotronBlockEntity;
import com.hbm_m.blockentity.machines.MachineRotaryFurnaceBlockEntity;
import com.hbm_m.blockentity.machines.MachineSatLinkBlockEntity;
import com.hbm_m.blockentity.machines.MachineSawmillBlockEntity;
import com.hbm_m.blockentity.machines.MachineSteamEngineBlockEntity;
import com.hbm_m.blockentity.machines.MachineStirlingBlockEntity;
import com.hbm_m.blockentity.machines.MachineThresherBlockEntity;
import com.hbm_m.blockentity.machines.MachineTurbofanBlockEntity;
import com.hbm_m.client.ClientRenderHandler;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.client.render.machine.MachineRenderers;
import com.hbm_m.client.render.machine.MachineSpecBuilder;
import com.hbm_m.client.render.machine.PartAnimator;
import com.hbm_m.client.render.raptorpath.ObjAtlasMesh;
import com.hbm_m.inventory.fluid.FluidContainerDefs;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;

/**
 * Render-Migration Paket P4 (Rest Kategorie A bewegt + erste B-Maschinen). Leuchtteile (Lichtwert 240/240),
 * eingefaerbte Teile, Flammen/Lava und Text bleiben als immediate-Hook.
 */
//? if forge {
@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
//?} elif fabric {
/*@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
*///?} elif neoforge {
/*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
*///?}
public final class RaptorPathP4 {

    private RaptorPathP4() {}

    private static ResourceLocation rl(String p) {
        return ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, p);
    }

    private static ObjAtlasMesh p(SimpleObjModel m, ResourceLocation t, String key, String... parts) {
        return ObjAtlasMesh.parts(m, t, "hbm_m/" + key, parts);
    }

    // Rotary furnace
    private static final ObjAtlasMesh RF_FURNACE = p(RotaryFurnaceRenderer.MODEL, RotaryFurnaceRenderer.TEX, "rotary_furnace/furnace", "Furnace");
    private static final ObjAtlasMesh RF_PISTON = p(RotaryFurnaceRenderer.MODEL, RotaryFurnaceRenderer.TEX, "rotary_furnace/piston", "Piston");
    // SatLink (Modell/Textur im alten BER privat)
    private static final SimpleObjModel SATLINK = new SimpleObjModel(rl("models/block/machines/satlink.obj"));
    private static final ResourceLocation SATLINK_TEX = rl("textures/block/machine/satlink.png");
    private static final ObjAtlasMesh SL_BASE = p(SATLINK, SATLINK_TEX, "satlink/base", "Base");
    private static final ObjAtlasMesh SL_ROTOR = p(SATLINK, SATLINK_TEX, "satlink/rotor", "Rotor");
    private static final ObjAtlasMesh SL_DISH = p(SATLINK, SATLINK_TEX, "satlink/dish", "Dish");
    // Sawmill
    private static final ObjAtlasMesh SM_MAIN = p(SawmillRenderer.MODEL, SawmillRenderer.TEX, "sawmill/main", "Main");
    private static final ObjAtlasMesh SM_BLADE = p(SawmillRenderer.MODEL, SawmillRenderer.TEX, "sawmill/blade", "Blade");
    private static final ObjAtlasMesh SM_GEAR_L = p(SawmillRenderer.MODEL, SawmillRenderer.TEX, "sawmill/gear_left", "GearLeft");
    private static final ObjAtlasMesh SM_GEAR_R = p(SawmillRenderer.MODEL, SawmillRenderer.TEX, "sawmill/gear_right", "GearRight");
    // Steam engine
    private static final ObjAtlasMesh SE_BASE = p(SteamEngineRenderer.MODEL, SteamEngineRenderer.TEX, "steam_engine/base", "Base");
    private static final ObjAtlasMesh SE_FLYWHEEL = p(SteamEngineRenderer.MODEL, SteamEngineRenderer.TEX, "steam_engine/flywheel", "Flywheel");
    private static final ObjAtlasMesh SE_SHAFT = p(SteamEngineRenderer.MODEL, SteamEngineRenderer.TEX, "steam_engine/shaft", "Shaft");
    private static final ObjAtlasMesh SE_TRANSMISSION = p(SteamEngineRenderer.MODEL, SteamEngineRenderer.TEX, "steam_engine/transmission", "Transmission");
    private static final ObjAtlasMesh SE_PISTON = p(SteamEngineRenderer.MODEL, SteamEngineRenderer.TEX, "steam_engine/piston", "Piston");
    // Stirling (normal/Stahl/Kreativ; Texturen im alten BER privat)
    private static final ResourceLocation[] STIRLING_TEX = {
            rl("textures/block/machine/stirling.png"), rl("textures/block/machine/stirling_steel.png"), rl("textures/block/machine/stirling_creative.png") };
    private static final ObjAtlasMesh[] ST_BASE = stirling("Base");
    private static final ObjAtlasMesh[] ST_COG = stirling("Cog");
    private static final ObjAtlasMesh[] ST_COG_SMALL = stirling("CogSmall");
    private static final ObjAtlasMesh[] ST_PISTON = stirling("Piston");
    // Thresher
    private static final ObjAtlasMesh TH_BASE = p(ThresherRenderer.MODEL, ThresherRenderer.TEX, "thresher/base", "Base");
    private static final ObjAtlasMesh TH_ENGINE = p(ThresherRenderer.MODEL, ThresherRenderer.TEX, "thresher/engine", "Engine");
    private static final ObjAtlasMesh TH_ARM_UPPER = p(ThresherRenderer.MODEL, ThresherRenderer.TEX, "thresher/arm_upper", "ArmUpper");
    private static final ObjAtlasMesh TH_ARM_LOWER = p(ThresherRenderer.MODEL, ThresherRenderer.TEX, "thresher/arm_lower", "ArmLower");
    private static final ObjAtlasMesh TH_FRONT = p(ThresherRenderer.MODEL, ThresherRenderer.TEX, "thresher/front", "Front");
    private static final ObjAtlasMesh TH_WHEEL = p(ThresherRenderer.MODEL, ThresherRenderer.TEX, "thresher/wheel", "Wheel");
    // Turbofan
    private static final ObjAtlasMesh TF_BODY = p(TurbofanRenderer.MODEL, TurbofanRenderer.TEX, "turbofan/body", "Body");
    private static final ObjAtlasMesh TF_BLADES = p(TurbofanRenderer.MODEL, TurbofanRenderer.TEX, "turbofan/blades", "Blades");
    private static final ObjAtlasMesh TF_BACK = p(TurbofanRenderer.MODEL, TurbofanRenderer.BACK_TEX, "turbofan/back", "Afterburner");
    private static final ObjAtlasMesh TF_AFTERBURNER = p(TurbofanRenderer.MODEL, TurbofanRenderer.AFTERBURNER_TEX, "turbofan/afterburner", "Afterburner");
    // Cargo elevator (eigener OBJ-Leser des alten BER = RBMK-Leser, Blockatlas)
    private static final String ELEVATOR_OBJ = "models/machines/elevator.obj";
    private static final ResourceLocation ELEVATOR_SPRITE = rl("block/machine/elevator");
    private static final ObjAtlasMesh EL_BASE = ObjAtlasMesh.groups(ELEVATOR_OBJ, ELEVATOR_SPRITE, "hbm_m/elevator/base", "Base");
    private static final ObjAtlasMesh EL_PLATFORM = ObjAtlasMesh.groups(ELEVATOR_OBJ, ELEVATOR_SPRITE, "hbm_m/elevator/platform", "Platform");
    private static final ObjAtlasMesh EL_PISTON = ObjAtlasMesh.groups(ELEVATOR_OBJ, ELEVATOR_SPRITE, "hbm_m/elevator/piston", "Piston");
    private static final Map<Integer, ObjAtlasMesh> EL_GUIDES = new HashMap<>();
    // Arc furnace
    private static final ObjAtlasMesh AF_FURNACE = p(ArcFurnaceRenderer.MODEL, ArcFurnaceRenderer.TEX, "arc_furnace/furnace", "Furnace");
    private static final ObjAtlasMesh AF_COLD = p(ArcFurnaceRenderer.MODEL, ArcFurnaceRenderer.TEX, "arc_furnace/contents_cold", "ContentsCold");
    private static final ObjAtlasMesh AF_LID = p(ArcFurnaceRenderer.MODEL, ArcFurnaceRenderer.TEX, "arc_furnace/lid", "Lid");
    private static final ObjAtlasMesh[] AF_RING = new ObjAtlasMesh[3];
    private static final ObjAtlasMesh[] AF_ELECTRODE = new ObjAtlasMesh[3];
    private static final ObjAtlasMesh[] AF_CABLE = new ObjAtlasMesh[3];
    // Charger
    private static final ObjAtlasMesh CH_BASE = p(ChargerRenderer.MODEL, ChargerRenderer.TEX, "charger/base", "Base");
    private static final ObjAtlasMesh CH_LEFT = p(ChargerRenderer.MODEL, ChargerRenderer.TEX, "charger/left", "Left");
    private static final ObjAtlasMesh CH_RIGHT = p(ChargerRenderer.MODEL, ChargerRenderer.TEX, "charger/right", "Right");
    private static final ObjAtlasMesh CH_LEFT_REST = chargerRest("Left");
    private static final ObjAtlasMesh CH_RIGHT_REST = chargerRest("Right");
    // Chungus
    private static final ObjAtlasMesh CG_BODY = p(ChungusRenderer.MODEL, ChungusRenderer.TEX, "chungus/body", "Body");
    private static final ObjAtlasMesh CG_BLADES = p(ChungusRenderer.MODEL, ChungusRenderer.TEX, "chungus/blades", "Blades");
    private static final ObjAtlasMesh[] CG_LEVER = new ObjAtlasMesh[4];
    // Combination oven, combustion engine, crucible
    private static final ObjAtlasMesh CO_BODY = ObjAtlasMesh.all(CombinationOvenRenderer.MODEL, CombinationOvenRenderer.TEX, "hbm_m/combination_oven");
    private static final ObjAtlasMesh CE_ENGINE = p(CombustionEngineRenderer.MODEL, CombustionEngineRenderer.TEX, "combustion_engine/engine", "Engine");
    private static final ObjAtlasMesh CE_CANISTER = p(CombustionEngineRenderer.MODEL, CombustionEngineRenderer.TEX, "combustion_engine/canister", "Canister");
    private static final ObjAtlasMesh CE_HATCH = p(CombustionEngineRenderer.MODEL, CombustionEngineRenderer.TEX, "combustion_engine/hatch", "Hatch");
    private static final ObjAtlasMesh CR_BODY = ObjAtlasMesh.all(CrucibleRenderer.MODEL, CrucibleRenderer.TEX, "hbm_m/crucible");
    // Cyclotron: Koerper + 4 Steckplaetze (leer/gefuellt mit eigener Textur)
    private static final ObjAtlasMesh CY_BODY = p(CyclotronRenderer.MODEL, CyclotronRenderer.tex("cyclotron"), "cyclotron/body", "Body");
    private static final ObjAtlasMesh[][] CY_PLUG = new ObjAtlasMesh[4][2];

    static {
        for (int i = 0; i < 3; i++) {
            AF_RING[i] = p(ArcFurnaceRenderer.MODEL, ArcFurnaceRenderer.TEX, "arc_furnace/ring" + (i + 1), "Ring" + (i + 1));
            AF_ELECTRODE[i] = p(ArcFurnaceRenderer.MODEL, ArcFurnaceRenderer.TEX, "arc_furnace/electrode" + (i + 1), "Electrode" + (i + 1));
            AF_CABLE[i] = p(ArcFurnaceRenderer.MODEL, ArcFurnaceRenderer.TEX, "arc_furnace/cable" + (i + 1), "Cable" + (i + 1));
        }
        for (int i = 0; i < CG_LEVER.length; i++) {
            final int steam = i + 2;
            CG_LEVER[i] = ObjAtlasMesh.of("hbm_m/chungus/lever" + steam, b -> {
                b.pose.translate(0, 0, 4.5);
                b.pose.mulPose(Axis.XP.rotationDegrees(15 - (steam - 2) * 10));
                b.pose.translate(0, 0, -4.5);
                b.obj(ChungusRenderer.MODEL, ChungusRenderer.TEX, "Lever");
            });
        }
        for (int i = 0; i < 4; i++) {
            for (int filled = 0; filled < 2; filled++) {
                String name = "cyclotron_" + CyclotronRenderer.PLUGS[i] + (filled == 1 ? "_filled" : "");
                CY_PLUG[i][filled] = p(CyclotronRenderer.MODEL, CyclotronRenderer.tex(name), "cyclotron/b" + (i + 1) + (filled == 1 ? "_filled" : ""), "B" + (i + 1));
            }
        }
    }

    private static ObjAtlasMesh[] stirling(String part) {
        ObjAtlasMesh[] out = new ObjAtlasMesh[3];
        for (int i = 0; i < 3; i++) out[i] = p(StirlingRenderer.MODEL, STIRLING_TEX[i], "stirling/" + part.toLowerCase(java.util.Locale.ROOT) + i, part);
        return out;
    }

    /** Ladearm in Ruhelage (extend = swivel = 0): feste 10-Grad-Neigung eingebacken. */
    private static ObjAtlasMesh chargerRest(String part) {
        return ObjAtlasMesh.of("hbm_m/charger/" + part.toLowerCase(java.util.Locale.ROOT) + "_rest", b -> {
            b.pose.translate(-0.34375D, 0.25D, 0);
            b.pose.mulPose(Axis.ZP.rotationDegrees(10));
            b.pose.translate(0.34375D, -0.25D, 0);
            b.obj(ChargerRenderer.MODEL, ChargerRenderer.TEX, part);
        });
    }

    private static ObjAtlasMesh elevatorGuides(int height) {
        return EL_GUIDES.computeIfAbsent(height, h -> ObjAtlasMesh.of("hbm_m/elevator/guides" + h, b -> {
            for (int i = 0; i <= h; i++) {
                b.group(ELEVATOR_OBJ, "Guides", ELEVATOR_SPRITE);
                b.pose.translate(0, 1, 0);
            }
        }));
    }

    public static void register() {
        // RenderRotaryFurnace: Metadaten 2/4/3/5 -> 90/180/270/0; Kolben
        MachineSpecBuilder<MachineRotaryFurnaceBlockEntity> rf = MachineRenderers.machine("hbm_m/rotary_furnace", ModBlockEntities.ROTARY_FURNACE_BE.get(), MachineRotaryFurnaceBlockEntity.class)
                .blockTransform((be, a) -> {
                    a.translate(0.5D, 0D, 0.5D);
                    switch (be.getBlockState().getValue(DummyableMachineBlock.FACING)) {
                        case NORTH -> rotY(a, 90);
                        case WEST -> rotY(a, 180);
                        case SOUTH -> rotY(a, 270);
                        default -> { }
                    }
                });
        mesh(rf, "furnace", RF_FURNACE);
        mesh(rf, "piston", (be, f, t, ps) -> {
            float anim = be.lastAnim + (be.anim - be.lastAnim) * f;
            ps.translate(0, sps((anim * 0.75) * 0.125) * 0.5 - 0.5, 0);
            return true;
        }, RF_PISTON);
        rf.viewDistance(256).register();

        // RenderSatLink: 180 + Versatz (dir + rot) * 0.5; Rotor dreht, Schuessel kippt
        MachineSpecBuilder<MachineSatLinkBlockEntity> sl = MachineRenderers.machine("hbm_m/satlink", ModBlockEntities.SATLINK_BE.get(), MachineSatLinkBlockEntity.class)
                .blockTransform((be, a) -> {
                    a.translate(0.5D, 0, 0.5D);
                    rotY(a, 180);
                    Direction dir = be.getBlockState().getValue(DummyableMachineBlock.FACING);
                    Direction rot = dir.getCounterClockWise();
                    a.translate((dir.getStepX() + rot.getStepX()) * 0.5, 0, (dir.getStepZ() + rot.getStepZ()) * 0.5);
                });
        mesh(sl, "base", SL_BASE);
        mesh(sl, "rotor", (be, f, t, ps) -> {
            ps.mulPose(Axis.YP.rotationDegrees(be.prevRot + (be.rot - be.prevRot) * f));
            return true;
        }, SL_ROTOR);
        mesh(sl, "dish", (be, f, t, ps) -> {
            ps.mulPose(Axis.YP.rotationDegrees(be.prevRot + (be.rot - be.prevRot) * f));
            ps.translate(0, 7.375, 0);
            ps.mulPose(Axis.ZP.rotationDegrees(be.prevLift + (be.lift - be.prevLift) * f));
            ps.translate(0, -7.375, 0);
            return true;
        }, SL_DISH);
        sl.viewDistance(256).register();

        // RenderSawmill: Metadaten 3/5/2/4 -> 0/90/180/270; Blatt nur mit Saegeblatt
        MachineSpecBuilder<MachineSawmillBlockEntity> sm = MachineRenderers.machine("hbm_m/sawmill", ModBlockEntities.SAWMILL_BE.get(), MachineSawmillBlockEntity.class)
                .blockTransform((be, a) -> {
                    a.translate(0.5D, 0D, 0.5D);
                    switch (be.getBlockState().getValue(DummyableMachineBlock.FACING)) {
                        case EAST -> rotY(a, 90);
                        case NORTH -> rotY(a, 180);
                        case WEST -> rotY(a, 270);
                        default -> { }
                    }
                });
        mesh(sm, "main", SM_MAIN);
        when(sm, "blade", (be, f, t, ps) -> {
            float rot = be.lastSpin + (be.spin - be.lastSpin) * f;
            ps.translate(0, 1.375, 0);
            ps.mulPose(Axis.ZP.rotationDegrees(-rot * 2));
            ps.translate(0, -1.375, 0);
            return true;
        }, be -> be.hasBlade, SM_BLADE);
        mesh(sm, "gear_left", (be, f, t, ps) -> {
            float rot = be.lastSpin + (be.spin - be.lastSpin) * f;
            ps.translate(0.5625, 1.375, 0);
            ps.mulPose(Axis.ZP.rotationDegrees(rot));
            ps.translate(-0.5625, -1.375, 0);
            return true;
        }, SM_GEAR_L);
        mesh(sm, "gear_right", (be, f, t, ps) -> {
            float rot = be.lastSpin + (be.spin - be.lastSpin) * f;
            ps.translate(-0.5625, 1.375, 0);
            ps.mulPose(Axis.ZP.rotationDegrees(-rot));
            ps.translate(0.5625, -1.375, 0);
            return true;
        }, SM_GEAR_R);
        sm.viewDistance(256).register();

        // RenderSteamEngine: Metadaten 3/5/2/4 -> 90/180/270/0, Versatz 2; Kurbeltrieb
        MachineSpecBuilder<MachineSteamEngineBlockEntity> se = MachineRenderers.machine("hbm_m/steam_engine", ModBlockEntities.STEAM_ENGINE_BE.get(), MachineSteamEngineBlockEntity.class)
                .blockTransform((be, a) -> {
                    a.translate(0.5D, 0D, 0.5D);
                    switch (be.getBlockState().getValue(DummyableMachineBlock.FACING)) {
                        case SOUTH -> rotY(a, 90);
                        case EAST -> rotY(a, 180);
                        case NORTH -> rotY(a, 270);
                        default -> { }
                    }
                    a.translate(2, 0, 0);
                });
        mesh(se, "base", SE_BASE);
        mesh(se, "flywheel", (be, f, t, ps) -> {
            double rot = steamRot(be, f);
            ps.translate(2, 1.375, 0);
            ps.mulPose(Axis.ZN.rotationDegrees((float) rot));
            ps.translate(-2, -1.375, 0);
            return true;
        }, SE_FLYWHEEL);
        mesh(se, "shaft", (be, f, t, ps) -> {
            double rot = steamRot(be, f);
            ps.translate(0, 1.375, -0.5);
            ps.mulPose(Axis.XP.rotationDegrees((float) (rot * 2D)));
            ps.translate(0, -1.375, 0.5);
            return true;
        }, SE_SHAFT);
        mesh(se, "transmission", (be, f, t, ps) -> {
            double rot = steamRot(be, f);
            double sin = Math.sin(rot * Math.PI / 180D) * 0.25D - 0.25D;
            double cos = Math.cos(rot * Math.PI / 180D) * 0.25D;
            double ang = Math.acos(cos / 1.875D);
            ps.translate(sin, cos, 0);
            ps.translate(2.25, 1.375, 0);
            ps.mulPose(Axis.ZN.rotationDegrees((float) (ang * 180D / Math.PI - 90D)));
            ps.translate(-2.25, -1.375, 0);
            return true;
        }, SE_TRANSMISSION);
        mesh(se, "piston", (be, f, t, ps) -> {
            double rot = steamRot(be, f);
            double sin = Math.sin(rot * Math.PI / 180D) * 0.25D - 0.25D;
            double cos = Math.cos(rot * Math.PI / 180D) * 0.25D;
            double cath = Math.sqrt(3.515625D - (cos * cos) / 2);
            ps.translate(1.875 - cath + sin, 0, 0);
            return true;
        }, SE_PISTON);
        se.viewDistance(256).register();

        // RenderStirling: Metadaten 3/5/2/4 -> 0/90/180/270; Bauart bestimmt die Textur
        MachineSpecBuilder<MachineStirlingBlockEntity> st = MachineRenderers.machine("hbm_m/stirling", ModBlockEntities.STIRLING_BE.get(), MachineStirlingBlockEntity.class)
                .blockTransform((be, a) -> {
                    a.translate(0.5D, 0D, 0.5D);
                    switch (ObjBerHelper.meta(be)) {
                        case 5 -> rotY(a, 90);
                        case 2 -> rotY(a, 180);
                        case 4 -> rotY(a, 270);
                        default -> { }
                    }
                });
        select(st, "base", be -> ST_BASE[stirlingType(be)]);
        select(st, "cog", (be, f, t, ps) -> {
            float rot = be.lastSpin + (be.spin - be.lastSpin) * f;
            ps.translate(0, 1.375, 0);
            ps.mulPose(Axis.ZP.rotationDegrees(-rot));
            ps.translate(0, -1.375, 0);
            return true;
        }, be -> be.hasCog() ? ST_COG[stirlingType(be)] : null);
        select(st, "cog_small", (be, f, t, ps) -> {
            float rot = be.lastSpin + (be.spin - be.lastSpin) * f;
            ps.translate(0, 1.375, 0.25);
            ps.mulPose(Axis.XP.rotationDegrees(rot * 2 + 3));
            ps.translate(0, -1.375, -0.25);
            return true;
        }, be -> ST_COG_SMALL[stirlingType(be)]);
        select(st, "piston", (be, f, t, ps) -> {
            float rot = be.lastSpin + (be.spin - be.lastSpin) * f;
            ps.translate(Math.sin(rot * Math.PI / 90D) * 0.25 + 0.125, 0, 0);
            return true;
        }, be -> ST_PISTON[stirlingType(be)]);
        st.viewDistance(256).register();

        // RenderThresher: Kette Engine | ArmUpper -> ArmLower -> Front -> Wheel
        MachineSpecBuilder<MachineThresherBlockEntity> th = MachineRenderers.machine("hbm_m/thresher", ModBlockEntities.THRESHER_BE.get(), MachineThresherBlockEntity.class)
                .blockTransform((be, a) -> {
                    a.translate(0.5D, 0D, 0.5D);
                    switch (be.getBlockState().getValue(MachineThresherBlock.FACING)) {
                        case NORTH -> rotY(a, 180);
                        case WEST -> rotY(a, 270);
                        case EAST -> rotY(a, 90);
                        default -> { }
                    }
                });
        mesh(th, "base", TH_BASE);
        mesh(th, "engine", (be, f, t, ps) -> thresher(be, f, ps, 0), TH_ENGINE);
        mesh(th, "arm_upper", (be, f, t, ps) -> thresher(be, f, ps, 1), TH_ARM_UPPER);
        mesh(th, "arm_lower", (be, f, t, ps) -> thresher(be, f, ps, 2), TH_ARM_LOWER);
        mesh(th, "front", (be, f, t, ps) -> thresher(be, f, ps, 3), TH_FRONT);
        mesh(th, "wheel", (be, f, t, ps) -> thresher(be, f, ps, 4), TH_WHEEL);
        th.viewDistance(256).register();

        // RenderTurbofan: Metadaten 2/4/3/5 -> 90/180/270/0; Nachbrenner-Textur nach Zustand
        MachineSpecBuilder<MachineTurbofanBlockEntity> tf = MachineRenderers.machine("hbm_m/turbofan", ModBlockEntities.TURBOFAN_BE.get(), MachineTurbofanBlockEntity.class)
                .blockTransform((be, a) -> {
                    a.translate(0.5D, 0D, 0.5D);
                    switch (be.getBlockState().getValue(DummyableMachineBlock.FACING)) {
                        case NORTH -> rotY(a, 90);
                        case WEST -> rotY(a, 180);
                        case SOUTH -> rotY(a, 270);
                        default -> { }
                    }
                });
        mesh(tf, "body", TF_BODY);
        mesh(tf, "blades", (be, f, t, ps) -> {
            float spin = be.lastSpin + (be.spin - be.lastSpin) * f;
            ps.translate(0, 1.5, 0);
            ps.mulPose(Axis.ZN.rotationDegrees(spin));
            ps.translate(0, -1.5, 0);
            return true;
        }, TF_BLADES);
        select(tf, "afterburner", be -> be.afterburner == 0 ? TF_BACK : TF_AFTERBURNER);
        tf.viewDistance(256).register();

        // RenderCargoElevator: nur Kernblock; Fuehrungen je Hoehe, Buehne faehrt, Kolbenstapel (Anzahl nach Hub) immediate
        MachineSpecBuilder<CargoElevatorBlockEntity> el = MachineRenderers.machine("hbm_m/cargo_elevator", ModBlockEntities.CARGO_ELEVATOR_BE.get(), CargoElevatorBlockEntity.class)
                .blockTransform((be, a) -> a.translate(0.5, 0, 0.5));
        when(el, "base", CargoElevatorBlockEntity::isCore, EL_BASE);
        select(el, "guides", be -> be.isCore() ? elevatorGuides(Math.max(0, be.height)) : null);
        when(el, "platform", (be, f, t, ps) -> {
            ps.translate(0, be.getRenderExtension(f), 0);
            return true;
        }, CargoElevatorBlockEntity::isCore, EL_PLATFORM);
        el.hook((be, f, ps, buf, light, overlay, api) -> {
            if (!be.isCore()) return;
            double extension = be.getRenderExtension(f);
            ps.pushPose();
            ps.translate(0, extension, 0);
            int pistons = (int) Math.floor(extension) + 1;
            for (int i = 0; i < pistons; i++) {
                EL_PISTON.renderImmediate(ps, buf, light);
                ps.translate(0, -1, 0);
            }
            ps.popPose();
        }).viewDistance(96).register();

        registerArcFurnace();

        // RenderCharger: 90 + FACING; Ladearme fahren aus und schwenken; Lampe und Schlitten (vollhell) immediate
        MachineSpecBuilder<ChargerBlockEntity> ch = MachineRenderers.machine("hbm_m/charger", ModBlockEntities.CHARGER_BE.get(), ChargerBlockEntity.class)
                .blockTransform((be, a) -> {
                    a.translate(0.5, 0, 0.5);
                    rotY(a, 90);
                    switch (ObjBerHelper.facing(be)) {
                        case WEST -> rotY(a, 90);
                        case SOUTH -> rotY(a, 180);
                        case EAST -> rotY(a, 270);
                        default -> { }
                    }
                });
        mesh(ch, "base", CH_BASE);
        Predicate<ChargerBlockEntity> charging = be -> be.getLastUsingTicks() != 0 || be.getUsingTicks() != 0;
        when(ch, "left", (be, f, t, ps) -> chargerArm(be, f, ps, 1), charging, CH_LEFT);
        when(ch, "right", (be, f, t, ps) -> chargerArm(be, f, ps, -1), charging, CH_RIGHT);
        when(ch, "left_rest", charging.negate(), CH_LEFT_REST);
        when(ch, "right_rest", charging.negate(), CH_RIGHT_REST);
        ch.hook((be, f, ps, buf, light, overlay, api) -> {
            double extend = Math.min(1, chargerTime(be, f) * 2);
            ps.pushPose();
            ChargerRenderer.MODEL.renderPartColor("Light", ps, buf.getBuffer(ClientRenderHandler.CustomRenderTypes.SOLID_COLOR_NOCULL), 1F, 0.75F, 0F, 1F);
            ps.translate(-0.34375D, 0.25D, 0);
            ps.mulPose(Axis.ZP.rotationDegrees(10));
            ps.translate(0.34375D, -0.25D, 0);
            ps.translate(0, -0.25 * extend, 0);
            ChargerRenderer.MODEL.renderPart("Slide", ps, buf.getBuffer(RenderType.entityCutoutNoCull(ChargerRenderer.TEX)), LightTexture.FULL_BRIGHT);
            ps.popPose();
        }).viewDistance(64).register();

        // RenderChungus: 90 + FACING, Versatz; Hebel nach Dampfsorte; Schaufeln; zerstoert (nur Space) immediate
        MachineSpecBuilder<MachineChungusBlockEntity> cg = MachineRenderers.machine("hbm_m/chungus", ModBlockEntities.MACHINE_CHUNGUS_BE.get(), MachineChungusBlockEntity.class)
                .blockTransform((be, a) -> {
                    a.translate(0.5D, 0, 0.5D);
                    rotY(a, 90);
                    Direction facing = be.getBlockState().hasProperty(HorizontalDirectionalBlock.FACING)
                            ? be.getBlockState().getValue(HorizontalDirectionalBlock.FACING) : Direction.NORTH;
                    switch (facing) {
                        case NORTH -> rotY(a, 90);
                        case WEST -> rotY(a, 180);
                        case SOUTH -> rotY(a, 270);
                        default -> { }
                    }
                    a.translate(0, 0, -3);
                });
        when(cg, "body", be -> !be.damaged, CG_BODY);
        select(cg, "lever", be -> be.damaged ? null
                : CG_LEVER[net.minecraft.util.Mth.clamp(ChungusRenderer.steamId(be.getSteamTank().getTankType()) - 2, 0, CG_LEVER.length - 1)]);
        when(cg, "blades", (be, f, t, ps) -> {
            ps.translate(0, 2.5, 0);
            ps.mulPose(Axis.ZN.rotationDegrees(be.getAnim(f)));
            ps.translate(0, -2.5, 0);
            return true;
        }, be -> !be.damaged, CG_BLADES);
        cg.hook((be, f, ps, buf, light, overlay, api) -> {
            if (!be.damaged) return;
            ChungusRenderer.MODEL_DESTROYED.renderAll(ps, buf.getBuffer(RenderType.entityCutout(ChungusRenderer.TEX_DESTROYED)), light);
        }).viewDistance(256).register();

        // RenderCombinationOven: Flamme (Billboard, vollhell) immediate
        mesh(MachineRenderers.machine("hbm_m/combination_oven", ModBlockEntities.COMBINATION_OVEN_BE.get(), MachineCombinationOvenBlockEntity.class)
                .blockTransform((be, a) -> a.translate(0.5D, 0D, 0.5D)), "body", CO_BODY)
                .hook((be, f, ps, buf, light, overlay, api) -> combinationFire(be, ps, buf))
                .viewDistance(256).register();

        // RenderCombustionEngine: Kanister nach Fluid eingefaerbt (immediate), Klappe schwenkt
        MachineSpecBuilder<MachineCombustionEngineBlockEntity> ce = MachineRenderers.machine("hbm_m/combustion_engine", ModBlockEntities.COMBUSTION_ENGINE_BE.get(), MachineCombustionEngineBlockEntity.class)
                .blockTransform((be, a) -> {
                    a.translate(0.5D, 0D, 0.5D);
                    switch (be.getBlockState().getValue(MachineCombustionEngineBlock.FACING)) {
                        case SOUTH -> rotY(a, 270);
                        case NORTH -> rotY(a, 90);
                        case WEST -> rotY(a, 180);
                        default -> { }
                    }
                    a.translate(-0.5, 0, 3);
                });
        mesh(ce, "engine", CE_ENGINE);
        when(ce, "canister", be -> FluidContainerDefs.getCanister(be.tank.getTankType()) == null, CE_CANISTER);
        idleStatic(ce, "hatch", (be, f, t, ps) -> {
            ps.translate(1, 0, -2.6875);
            ps.mulPose(Axis.YN.rotationDegrees(be.prevDoorAngle + (be.doorAngle - be.prevDoorAngle) * f));
            ps.translate(-1, 0, 2.6875);
            return true;
        }, be -> be.prevDoorAngle != 0F || be.doorAngle != 0F, CE_HATCH);
        ce.hook((be, f, ps, buf, light, overlay, api) -> {
            FluidContainerDefs.CDCanister canister = FluidContainerDefs.getCanister(be.tank.getTankType());
            if (canister == null) return;
            int color = canister.color();
            float r = ((color & 0xff0000) >> 16) / 256F;
            float g = ((color & 0x00ff00) >> 8) / 256F;
            float b = ((color & 0x0000ff) >> 0) / 256F;
            CombustionEngineRenderer.MODEL.renderPartTinted("Canister", ps, buf.getBuffer(RenderType.entityCutout(CombustionEngineRenderer.TEX)), light, r, g, b);
        }).viewDistance(256).register();

        // RenderCrucible: Metadaten 3/5/2/4 -> 270/0/90/180; Schmelze (vollhell) immediate
        mesh(MachineRenderers.machine("hbm_m/crucible", ModBlockEntities.CRUCIBLE_BE.get(), MachineCrucibleBlockEntity.class)
                .blockTransform((be, a) -> {
                    a.translate(0.5D, 0D, 0.5D);
                    switch (be.getBlockState().getValue(DummyableMachineBlock.FACING)) {
                        case SOUTH -> rotY(a, 270);
                        case NORTH -> rotY(a, 90);
                        case WEST -> rotY(a, 180);
                        default -> { }
                    }
                }), "body", CR_BODY)
                .hook((be, f, ps, buf, light, overlay, api) -> crucibleMelt(be, ps, buf))
                .viewDistance(256).register();

        // RenderCyclotron: Steckplaetze leer/gefuellt (eigene Texturen), Schriftring immediate
        MachineSpecBuilder<MachineCyclotronBlockEntity> cy = MachineRenderers.machine("hbm_m/cyclotron", ModBlockEntities.CYCLOTRON_BE.get(), MachineCyclotronBlockEntity.class)
                .blockTransform((be, a) -> a.translate(0.5D, 0D, 0.5D));
        mesh(cy, "body", CY_BODY);
        for (int i = 0; i < 4; i++) {
            final int plug = i;
            select(cy, "b" + (i + 1), be -> CY_PLUG[plug][be.getPlug(plug) ? 1 : 0]);
        }
        cy.hook((be, f, ps, buf, light, overlay, api) -> cyclotronText(be, ps, buf)).viewDistance(64).register();
    }

    // ── Lichtbogenofen ──────────────────────────────────────────────────────────────────

    private static void registerArcFurnace() {
        MachineSpecBuilder<MachineArcFurnaceBlockEntity> af = MachineRenderers.machine("hbm_m/arc_furnace", ModBlockEntities.ARC_FURNACE_BE.get(), MachineArcFurnaceBlockEntity.class)
                .blockTransform((be, a) -> {
                    a.translate(0.5D, 0D, 0.5D);
                    switch (be.getBlockState().getValue(DummyableMachineBlock.FACING)) {
                        case NORTH -> rotY(a, 90);
                        case WEST -> rotY(a, 180);
                        case SOUTH -> rotY(a, 270);
                        default -> { }
                    }
                });
        mesh(af, "furnace", AF_FURNACE);
        when(af, "contents_cold", be -> be.liquids.isEmpty() && be.hasMaterial, AF_COLD);
        // Deckelgruppe: in Ruhe (zu, kein Prozess) statisch in Modelllage, sonst bewegt
        Predicate<MachineArcFurnaceBlockEntity> moving = be -> be.prevLid != 0F || be.lid != 0F || be.isProgressing;
        PartAnimator<MachineArcFurnaceBlockEntity> lid = RaptorPathP4::arcLid;
        idleStatic(af, "lid", lid, moving, AF_LID);
        for (int i = 0; i < 3; i++) {
            final int e = i;
            final double offset = new double[] { 0.5, 0, -0.5 }[i];
            Predicate<MachineArcFurnaceBlockEntity> ring = be -> be.electrodes[e] != MachineArcFurnaceBlockEntity.ELECTRODE_NONE;
            Predicate<MachineArcFurnaceBlockEntity> fresh = be -> be.electrodes[e] == MachineArcFurnaceBlockEntity.ELECTRODE_FRESH;
            when(af, "ring" + (i + 1), lid, ring.and(moving), AF_RING[i]);
            when(af, "ring" + (i + 1) + "_idle", ring.and(moving.negate()), AF_RING[i]);
            when(af, "electrode" + (i + 1), lid, fresh.and(moving), AF_ELECTRODE[i]);
            when(af, "electrode" + (i + 1) + "_idle", fresh.and(moving.negate()), AF_ELECTRODE[i]);
            PartAnimator<MachineArcFurnaceBlockEntity> cable = (be, f, t, ps) -> {
                arcLid(be, f, t, ps);
                long time = be.getLevel() != null ? be.getLevel().getGameTime() : 0;
                ps.translate(0, 5.5, offset);
                if (be.isProgressing) ps.mulPose(Axis.XP.rotationDegrees((float) (Math.sin((time + f) / 2) * 30)));
                ps.translate(0, -5.5, -offset);
                return true;
            };
            when(af, "cable" + (i + 1), cable, ring.and(moving), AF_CABLE[i]);
            when(af, "cable" + (i + 1) + "_idle", ring.and(moving.negate()), AF_CABLE[i]);
        }
        af.hook((arc, interp, ps, buf, light, overlay, api) -> {
            VertexConsumer nocull = buf.getBuffer(RenderType.entityCutoutNoCull(ArcFurnaceRenderer.TEX));
            int full = 0xF000F0;
            if (!arc.liquids.isEmpty()) {
                ps.pushPose();
                ps.translate(0, -1.75 + MachineArcFurnaceBlockEntity.getStackAmount(arc.liquids) * 1.75 / MachineArcFurnaceBlockEntity.maxLiquid, 0);
                ArcFurnaceRenderer.MODEL.renderPart("ContentsHot", ps, nocull, full);
                ps.popPose();
            }
            ps.pushPose();
            arcLid(arc, interp, 0L, ps);
            for (int i = 0; i < 3; i++) {
                byte e = arc.electrodes[i];
                if (e == MachineArcFurnaceBlockEntity.ELECTRODE_USED) ArcFurnaceRenderer.MODEL.renderPart("Electrode" + (i + 1) + "Hot", ps, nocull, full);
                if (e == MachineArcFurnaceBlockEntity.ELECTRODE_DEPLETED) ArcFurnaceRenderer.MODEL.renderPart("Electrode" + (i + 1) + "Short", ps, nocull, full);
            }
            ps.popPose();
        }).viewDistance(256).register();
    }

    private static boolean arcLid(MachineArcFurnaceBlockEntity arc, float interp, long unused, PoseStack ps) {
        float lift = arc.prevLid + (arc.lid - arc.prevLid) * interp;
        long time = arc.getLevel() != null ? arc.getLevel().getGameTime() : 0;
        ps.translate(0, 2 * lift, 0);
        if (arc.isProgressing) ps.translate(0, 0, Math.sin(time + interp) * 0.005);
        return true;
    }

    // ── Hilfen ───────────────────────────────────────────────────────────────────────────

    private static double sps(double x) {
        return Math.sin(Math.PI / 2D * Math.cos(x));
    }

    private static double steamRot(MachineSteamEngineBlockEntity be, float f) {
        return be.lastRotor + (be.rotor - be.lastRotor) * f;
    }

    private static int stirlingType(MachineStirlingBlockEntity be) {
        int type = be.getGeatMeta();
        return type == 0 ? 0 : type == 2 ? 2 : 1;
    }

    /** ThresherRenderer.renderCommon bis zum Glied {@code stage} (0 Engine, 1 ArmUpper, 2 ArmLower, 3 Front, 4 Wheel). */
    private static boolean thresher(MachineThresherBlockEntity th, float interp, PoseStack ps, int stage) {
        double angle = 82.5 - (th.prevAngle + (th.angle - th.prevAngle) * interp);
        double spin = th.lastSpin + (th.spin - th.lastSpin) * interp;
        if (stage == 0) {
            double engine = th.isOn && th.getLevel() != null
                    ? Math.sin(th.getLevel().getGameTime() * 2 % (Math.PI * 2) + interp) : 0;
            ps.translate(0, engine * 0.01, 0);
            return true;
        }
        ps.translate(0, 0.5, -1);
        ps.mulPose(Axis.XP.rotationDegrees((float) angle));
        ps.translate(0, -0.5, 1);
        if (stage == 1) return true;
        ps.translate(0, 0.5, -5);
        ps.mulPose(Axis.XP.rotationDegrees((float) (angle * -2)));
        ps.translate(0, -0.5, 5);
        ps.translate(-0.01, 0, 0);
        if (stage == 2) return true;
        ps.translate(0.01, 0, 0);
        ps.translate(0, 0.5, -9);
        ps.mulPose(Axis.XP.rotationDegrees((float) angle));
        ps.translate(0, -0.5, 9);
        ps.translate(0.01, 0, 0);
        if (stage == 3) return true;
        ps.translate(0, 0.5, -11);
        ps.mulPose(Axis.XP.rotationDegrees((float) -spin));
        ps.translate(0, -0.5, 11);
        return true;
    }

    private static double chargerTime(ChargerBlockEntity charger, float interp) {
        return (charger.getLastUsingTicks() + (charger.getUsingTicks() - charger.getLastUsingTicks()) * interp) / 20D;
    }

    /** Ladearm ({@code side} 1 = links, -1 = rechts) aus {@code ChargerRenderer.render}. */
    private static boolean chargerArm(ChargerBlockEntity charger, float interp, PoseStack ps, int side) {
        double time = chargerTime(charger, interp);
        double extend = Math.min(1, time * 2);
        double swivel = Math.max(0, (time - 0.5) * 2);
        ps.translate(-0.34375D, 0.25D, 0);
        ps.mulPose(Axis.ZP.rotationDegrees(10));
        ps.translate(0.34375D, -0.25D, 0);
        ps.translate(0, -0.25 * extend, 0);
        ps.translate(0, 0.28D, 0);
        ps.mulPose(Axis.XP.rotationDegrees((float) (30 * swivel * side)));
        ps.translate(0, -0.28D, 0);
        return true;
    }

    /** Flamme aus {@code CombinationOvenRenderer} (unveraendert). */
    private static void combinationFire(MachineCombinationOvenBlockEntity te, PoseStack ps, net.minecraft.client.renderer.MultiBufferSource buf) {
        if (!te.wasOn || te.getLevel() == null) return;
        ps.pushPose();
        int texIndex = (int) (te.getLevel().getGameTime() / 2 % 14);
        float f0 = 1F / 14F;
        float uMin = texIndex % 5 * f0;
        float uMax = uMin + f0;
        float vMin = 0;
        float vMax = 1;
        ps.translate(0, 1.75, 0);
        ps.mulPose(Axis.YP.rotationDegrees(-Minecraft.getInstance().gameRenderer.getMainCamera().getYRot()));
        float scaleH = 1;
        float scaleV = 3;
        VertexConsumer vc = buf.getBuffer(RenderType.eyes(CombinationOvenRenderer.FIRE));
        org.joml.Matrix4f m = ps.last().pose();
        org.joml.Matrix3f n = ps.last().normal();
        fireVertex(vc, m, n, -scaleH, 0, uMax, vMax);
        fireVertex(vc, m, n, -scaleH, scaleV, uMax, vMin);
        fireVertex(vc, m, n, scaleH, scaleV, uMin, vMin);
        fireVertex(vc, m, n, scaleH, 0, uMin, vMax);
        fireVertex(vc, m, n, scaleH, 0, uMin, vMax);
        fireVertex(vc, m, n, scaleH, scaleV, uMin, vMin);
        fireVertex(vc, m, n, -scaleH, scaleV, uMax, vMin);
        fireVertex(vc, m, n, -scaleH, 0, uMax, vMax);
        ps.popPose();
    }

    private static void fireVertex(VertexConsumer vc, org.joml.Matrix4f m, org.joml.Matrix3f n, float x, float y, float u, float v) {
        //? if < 1.21.1 {
        vc.vertex(m, x, y, 0).color(1F, 1F, 1F, 1F).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(240, 240).normal(n, 0F, 1F, 0F).endVertex();
        //?} else {
        /*com.hbm_m.platform.RenderHooks.normal(vc.addVertex(m, x, y, 0).setColor(1F, 1F, 1F, 1F).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setUv2(240, 240), n, 0F, 1F, 0F);
        *///?}
    }

    /** Schmelzspiegel aus {@code CrucibleRenderer} (unveraendert). */
    private static void crucibleMelt(MachineCrucibleBlockEntity crucible, PoseStack ps, net.minecraft.client.renderer.MultiBufferSource buf) {
        if (crucible.recipeStack.isEmpty() && crucible.wasteStack.isEmpty()) return;
        int totalCap = MachineCrucibleBlockEntity.recipeZCapacity + MachineCrucibleBlockEntity.wasteZCapacity;
        int totalMass = 0;
        for (com.hbm_m.inventory.material.Mats.MaterialStack stack : crucible.recipeStack) totalMass += stack.amount;
        for (com.hbm_m.inventory.material.Mats.MaterialStack stack : crucible.wasteStack) totalMass += stack.amount;
        float level = (float) (((double) totalMass / (double) totalCap) * 0.875D);
        float h = 0.5F + level;
        VertexConsumer vc = buf.getBuffer(RenderType.entityCutoutNoCull(CrucibleRenderer.LAVA));
        org.joml.Matrix4f m = ps.last().pose();
        org.joml.Matrix3f n = ps.last().normal();
        int full = 0xF000F0;
        //? if < 1.21.1 {
        vc.vertex(m, -1, h, -1).color(1F, 1F, 1F, 1F).uv(0, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(full).normal(n, 0, 1, 0).endVertex();
        vc.vertex(m, -1, h, 1).color(1F, 1F, 1F, 1F).uv(0, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(full).normal(n, 0, 1, 0).endVertex();
        vc.vertex(m, 1, h, 1).color(1F, 1F, 1F, 1F).uv(1, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(full).normal(n, 0, 1, 0).endVertex();
        vc.vertex(m, 1, h, -1).color(1F, 1F, 1F, 1F).uv(1, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(full).normal(n, 0, 1, 0).endVertex();
        //?} else {
        /*com.hbm_m.platform.RenderHooks.normal(vc.addVertex(m, -1, h, -1).setColor(1F, 1F, 1F, 1F).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(full), n, 0, 1, 0);
        com.hbm_m.platform.RenderHooks.normal(vc.addVertex(m, -1, h, 1).setColor(1F, 1F, 1F, 1F).setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(full), n, 0, 1, 0);
        com.hbm_m.platform.RenderHooks.normal(vc.addVertex(m, 1, h, 1).setColor(1F, 1F, 1F, 1F).setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(full), n, 0, 1, 0);
        com.hbm_m.platform.RenderHooks.normal(vc.addVertex(m, 1, h, -1).setColor(1F, 1F, 1F, 1F).setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(full), n, 0, 1, 0);
        *///?}
    }

    /** Schriftring aus {@code CyclotronRenderer} (nur wenn alle vier Steckplaetze gefuellt). */
    private static void cyclotronText(MachineCyclotronBlockEntity cyc, PoseStack ps, net.minecraft.client.renderer.MultiBufferSource buf) {
        for (int i = 0; i < 4; i++) if (!cyc.getPlug(i)) return;
        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees((float) (System.currentTimeMillis() * 0.025 % 360)));
        String msg = "plures necat crapula quam gladius";
        ps.translate(0, 2, 0);
        ps.mulPose(Axis.XP.rotationDegrees(180));
        float rot = 0F;
        Font font = Minecraft.getInstance().font;
        for (char c : msg.toCharArray()) {
            ps.pushPose();
            ps.mulPose(Axis.YP.rotationDegrees(rot));
            rot -= font.width(String.valueOf(c)) * 2F;
            ps.translate(2.75, 0, 0);
            ps.mulPose(Axis.YP.rotationDegrees(-90));
            float scale = 0.1F;
            ps.scale(scale, scale, scale);
            font.drawInBatch(Component.literal(String.valueOf(c)).withStyle(CyclotronRenderer.GALACTIC), 0, 0, 0x600060, false,
                    ps.last().pose(), buf, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
            ps.popPose();
        }
        ps.popPose();
    }
}
