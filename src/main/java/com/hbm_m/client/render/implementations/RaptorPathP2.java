package com.hbm_m.client.render.implementations;

import static com.hbm_m.client.render.raptorpath.RaptorParts.idleStatic;
import static com.hbm_m.client.render.raptorpath.RaptorParts.mesh;
import static com.hbm_m.client.render.raptorpath.RaptorParts.rotY;
import static com.hbm_m.client.render.raptorpath.RaptorParts.select;
import static com.hbm_m.client.render.raptorpath.RaptorParts.when;

import com.hbm_m.block.decorations.FileCabinetBlock;
import com.hbm_m.block.machines.DummyableMachineBlock;
import com.hbm_m.block.machines.MachineAnnihilatorBlock;
import com.hbm_m.block.machines.MachineBoilerBlock;
import com.hbm_m.block.machines.MachineFanBlock;
import com.hbm_m.block.machines.MachineIntakeBlock;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.decorations.FileCabinetBlockEntity;
import com.hbm_m.blockentity.machines.MachineAmmoPressBlockEntity;
import com.hbm_m.blockentity.machines.MachineAmmoPressBlockEntity.AnimationState;
import com.hbm_m.blockentity.machines.MachineAnnihilatorBlockEntity;
import com.hbm_m.blockentity.machines.MachineAshpitBlockEntity;
import com.hbm_m.blockentity.machines.MachineAutosawBlockEntity;
import com.hbm_m.blockentity.machines.MachineBoilerBlockEntity;
import com.hbm_m.blockentity.machines.MachineBreederBlockEntity;
import com.hbm_m.blockentity.machines.MachineChemicalFactoryBlockEntity;
import com.hbm_m.blockentity.machines.MachineCompressorBlockEntity;
import com.hbm_m.blockentity.machines.MachineCompressorCompactBlockEntity;
import com.hbm_m.blockentity.machines.MachineCondenserPoweredBlockEntity;
import com.hbm_m.blockentity.machines.MachineConveyorPressBlockEntity;
import com.hbm_m.blockentity.machines.MachineDieselGeneratorBlockEntity;
import com.hbm_m.blockentity.machines.MachineEPressBlockEntity;
import com.hbm_m.blockentity.machines.MachineFanBlockEntity;
import com.hbm_m.blockentity.machines.MachineIndustrialTurbineBlockEntity;
import com.hbm_m.blockentity.machines.MachineIntakeBlockEntity;
import com.hbm_m.client.render.RenderDistanceHelper;
import com.hbm_m.client.render.UvShiftConsumer;
import com.hbm_m.client.render.machine.MachineRenderers;
import com.hbm_m.client.render.machine.MachineSpecBuilder;
import com.hbm_m.client.render.raptorpath.ObjAtlasMesh;
import com.hbm_m.client.render.util.RenderSparks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;

/**
 * Render-Migration Paket P2 (Kategorie A, bewegt): Animatoren 1:1 aus den alten BERs; die Teil-Ketten (Autosaege,
 * Arme) wiederholen fuer jedes Glied die Transformationen der vorherigen Glieder, wie der alte Matrixstapel.
 */
//? if forge {
@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
//?} elif fabric {
/*@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
*///?} elif neoforge {
/*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
*///?}
public final class RaptorPathP2 {

    private RaptorPathP2() {}

    private static ObjAtlasMesh p(com.hbm_m.client.render.SimpleObjModel m, net.minecraft.resources.ResourceLocation t, String key, String... parts) {
        return ObjAtlasMesh.parts(m, t, "hbm_m/" + key, parts);
    }

    // AmmoPress
    private static final ObjAtlasMesh AMMO_FRAME = p(AmmoPressRenderer.MODEL, AmmoPressRenderer.TEX, "ammo_press/frame", "Frame");
    private static final ObjAtlasMesh AMMO_PRESS = p(AmmoPressRenderer.MODEL, AmmoPressRenderer.TEX, "ammo_press/press", "Press");
    private static final ObjAtlasMesh AMMO_SHELLS = p(AmmoPressRenderer.MODEL, AmmoPressRenderer.TEX, "ammo_press/shells", "Shells");
    private static final ObjAtlasMesh AMMO_BULLETS = p(AmmoPressRenderer.MODEL, AmmoPressRenderer.TEX, "ammo_press/bullets", "Bullets");
    // Annihilator
    private static final ObjAtlasMesh ANNI_BODY = p(AnnihilatorRenderer.MODEL, AnnihilatorRenderer.TEX, "annihilator/body", "Annihilator");
    private static final ObjAtlasMesh ANNI_ROLLER = p(AnnihilatorRenderer.MODEL, AnnihilatorRenderer.TEX, "annihilator/roller", "Roller");
    // Ashpit (Modell des Heizofens, eigene Textur)
    private static final ObjAtlasMesh ASH_MAIN = p(HeatingOvenRenderer.MODEL, AshpitRenderer.TEX, "ashpit/main", "Main");
    private static final ObjAtlasMesh ASH_DOOR = p(HeatingOvenRenderer.MODEL, AshpitRenderer.TEX, "ashpit/door", "Door");
    private static final ObjAtlasMesh ASH_INNER = p(HeatingOvenRenderer.MODEL, AshpitRenderer.TEX, "ashpit/inner", "Inner");
    private static final ObjAtlasMesh ASH_INNER_BURNING = p(HeatingOvenRenderer.MODEL, AshpitRenderer.TEX, "ashpit/inner_burning", "InnerBurning");
    // Autosaw
    private static final ObjAtlasMesh SAW_BASE = p(AutosawRenderer.MODEL, AutosawRenderer.TEX, "autosaw/base", "Base");
    private static final ObjAtlasMesh SAW_MAIN = p(AutosawRenderer.MODEL, AutosawRenderer.TEX, "autosaw/main", "Main");
    private static final ObjAtlasMesh SAW_ENGINE = p(AutosawRenderer.MODEL, AutosawRenderer.TEX, "autosaw/engine", "Engine");
    private static final ObjAtlasMesh SAW_ARM_UPPER = p(AutosawRenderer.MODEL, AutosawRenderer.TEX, "autosaw/arm_upper", "ArmUpper");
    private static final ObjAtlasMesh SAW_ARM_LOWER = p(AutosawRenderer.MODEL, AutosawRenderer.TEX, "autosaw/arm_lower", "ArmLower");
    private static final ObjAtlasMesh SAW_ARM_TIP = p(AutosawRenderer.MODEL, AutosawRenderer.TEX, "autosaw/arm_tip", "ArmTip");
    private static final ObjAtlasMesh SAW_BLADE = p(AutosawRenderer.MODEL, AutosawRenderer.TEX, "autosaw/sawblade", "Sawblade");
    // Boiler
    private static final ObjAtlasMesh BOILER = ObjAtlasMesh.all(BoilerRenderer.MODEL, BoilerRenderer.TEX, "hbm_m/boiler");
    private static final ObjAtlasMesh BOILER_BURST = ObjAtlasMesh.all(BoilerRenderer.MODEL_BURST, BoilerRenderer.TEX, "hbm_m/boiler_burst");
    // Breeder
    private static final ObjAtlasMesh BREEDER = ObjAtlasMesh.all(BreederRenderer.MODEL, BreederRenderer.TEX, "hbm_m/breeder");
    // Chemical factory
    private static final ObjAtlasMesh CF_BASE = p(ChemicalFactoryRenderer.MODEL, ChemicalFactoryRenderer.TEX, "chemical_factory/base", "Base");
    private static final ObjAtlasMesh CF_FRAME = p(ChemicalFactoryRenderer.MODEL, ChemicalFactoryRenderer.TEX, "chemical_factory/frame", "Frame");
    private static final ObjAtlasMesh CF_FAN1 = p(ChemicalFactoryRenderer.MODEL, ChemicalFactoryRenderer.TEX, "chemical_factory/fan1", "Fan1");
    private static final ObjAtlasMesh CF_FAN2 = p(ChemicalFactoryRenderer.MODEL, ChemicalFactoryRenderer.TEX, "chemical_factory/fan2", "Fan2");
    // Compressor
    private static final ObjAtlasMesh COMP_BODY = p(CompressorRenderer.MODEL, CompressorRenderer.TEX, "compressor/body", "Compressor");
    private static final ObjAtlasMesh COMP_PUMP = p(CompressorRenderer.MODEL, CompressorRenderer.TEX, "compressor/pump", "Pump");
    private static final ObjAtlasMesh COMP_FAN = p(CompressorRenderer.MODEL, CompressorRenderer.TEX, "compressor/fan", "Fan");
    private static final ObjAtlasMesh COMPACT_BODY = p(CompressorRenderer.CONDENSER, CompressorRenderer.TEX_COMPACT, "compressor_compact/body", "Condenser");
    private static final ObjAtlasMesh COMPACT_FAN1 = p(CompressorRenderer.CONDENSER, CompressorRenderer.TEX_COMPACT, "compressor_compact/fan1", "Fan1");
    private static final ObjAtlasMesh COMPACT_FAN2 = p(CompressorRenderer.CONDENSER, CompressorRenderer.TEX_COMPACT, "compressor_compact/fan2", "Fan2");
    // Condenser (Strom)
    private static final ObjAtlasMesh COND_BODY = p(CompressorRenderer.CONDENSER, CondenserPoweredRenderer.TEX, "condenser_powered/body", "Condenser");
    private static final ObjAtlasMesh COND_FAN1 = p(CompressorRenderer.CONDENSER, CondenserPoweredRenderer.TEX, "condenser_powered/fan1", "Fan1");
    private static final ObjAtlasMesh COND_FAN2 = p(CompressorRenderer.CONDENSER, CondenserPoweredRenderer.TEX, "condenser_powered/fan2", "Fan2");
    // Conveyor press
    private static final ObjAtlasMesh CP_PRESS = p(ConveyorPressRenderer.MODEL, ConveyorPressRenderer.TEX, "conveyor_press/press", "Press");
    private static final ObjAtlasMesh CP_PISTON = p(ConveyorPressRenderer.MODEL, ConveyorPressRenderer.TEX, "conveyor_press/piston", "Piston");
    // Diesel generator
    private static final ObjAtlasMesh DG_GEN = p(DieselGenRenderer.MODEL, DieselGenRenderer.TEX, "dieselgen/generator", "Generator");
    private static final ObjAtlasMesh DG_ENGINE = p(DieselGenRenderer.MODEL, DieselGenRenderer.TEX, "dieselgen/engine", "Engine");
    // EPress
    private static final ObjAtlasMesh EP_BODY = ObjAtlasMesh.all(EPressRenderer.BODY, EPressRenderer.BODY_TEX, "hbm_m/epress/body");
    private static final ObjAtlasMesh EP_HEAD = ObjAtlasMesh.all(EPressRenderer.HEAD, EPressRenderer.HEAD_TEX, "hbm_m/epress/head");
    // Fan
    private static final ObjAtlasMesh FAN_FRAME = p(FanRenderer.MODEL, FanRenderer.TEX, "fan/frame", "Frame");
    private static final ObjAtlasMesh FAN_BLADES = p(FanRenderer.MODEL, FanRenderer.TEX, "fan/blades", "Blades");
    // File cabinet (Holz/Stahl)
    private static final ObjAtlasMesh[] CAB = {
            p(FileCabinetRenderer.MODEL, FileCabinetRenderer.TEX, "file_cabinet/cabinet", "Cabinet"),
            p(FileCabinetRenderer.MODEL, FileCabinetRenderer.TEX_STEEL, "file_cabinet/cabinet_steel", "Cabinet") };
    private static final ObjAtlasMesh[] CAB_LOWER = {
            p(FileCabinetRenderer.MODEL, FileCabinetRenderer.TEX, "file_cabinet/lower", "LowerDrawer"),
            p(FileCabinetRenderer.MODEL, FileCabinetRenderer.TEX_STEEL, "file_cabinet/lower_steel", "LowerDrawer") };
    private static final ObjAtlasMesh[] CAB_UPPER = {
            p(FileCabinetRenderer.MODEL, FileCabinetRenderer.TEX, "file_cabinet/upper", "UpperDrawer"),
            p(FileCabinetRenderer.MODEL, FileCabinetRenderer.TEX_STEEL, "file_cabinet/upper_steel", "UpperDrawer") };
    // Industrial turbine: Zeiger je Dampfsorte (Zustand, eingebacken)
    private static final ObjAtlasMesh IT_BODY = p(IndustrialTurbineRenderer.MODEL, IndustrialTurbineRenderer.TEX, "industrial_turbine/body", "Turbine");
    private static final ObjAtlasMesh IT_FLYWHEEL = p(IndustrialTurbineRenderer.MODEL, IndustrialTurbineRenderer.TEX, "industrial_turbine/flywheel", "Flywheel");
    private static final ObjAtlasMesh[] IT_GAUGE = new ObjAtlasMesh[4];
    // Intake
    private static final ObjAtlasMesh INTAKE_BASE = p(IntakeRenderer.MODEL, IntakeRenderer.TEX, "intake/base", "Base");
    private static final ObjAtlasMesh INTAKE_FAN = p(IntakeRenderer.MODEL, IntakeRenderer.TEX, "intake/fan", "Fan");

    static {
        for (int i = 0; i < IT_GAUGE.length; i++) {
            final int steam = i + 2;
            IT_GAUGE[i] = ObjAtlasMesh.of("hbm_m/industrial_turbine/gauge" + steam, b -> {
                b.pose.translate(0, 1.5, 0);
                b.pose.mulPose(Axis.ZP.rotationDegrees(135 - (steam - 2) * 90));
                b.pose.translate(0, -1.5, 0);
                b.obj(IndustrialTurbineRenderer.MODEL, IndustrialTurbineRenderer.TEX, "Gauge");
            });
        }
    }

    public static void register() {
        // RenderAmmoPress: Metadaten 2/4/3/5 -> 90/180/270/0
        MachineSpecBuilder<MachineAmmoPressBlockEntity> ammo = MachineRenderers.machine("hbm_m/ammo_press", ModBlockEntities.AMMO_PRESS_BE.get(), MachineAmmoPressBlockEntity.class)
                .blockTransform((be, a) -> {
                    a.translate(0.5D, 0D, 0.5D);
                    switch (be.getBlockState().getValue(DummyableMachineBlock.FACING)) {
                        case NORTH -> rotY(a, 90);
                        case WEST -> rotY(a, 180);
                        case SOUTH -> rotY(a, 270);
                        default -> { }
                    }
                });
        mesh(ammo, "frame", AMMO_FRAME);
        mesh(ammo, "press", (be, f, t, ps) -> {
            float press = be.prevPress + (be.press - be.prevPress) * f;
            ps.translate(0, -press * 0.25F, 0);
            return true;
        }, AMMO_PRESS);
        mesh(ammo, "shells", RaptorPathP2::ammoShells, AMMO_SHELLS);
        when(ammo, "bullets", RaptorPathP2::ammoShells,
                be -> be.animState == AnimationState.RETRACTING || be.animState == AnimationState.LOWERING, AMMO_BULLETS);
        ammo.viewDistance(256).register();

        // RenderAnnihilator: 90 + Metadaten; Walze nach Systemzeit, Band (UV-Lauf auf Einzeltextur) immediate
        MachineSpecBuilder<MachineAnnihilatorBlockEntity> anni = MachineRenderers.machine("hbm_m/annihilator", ModBlockEntities.ANNIHILATOR_BE.get(), MachineAnnihilatorBlockEntity.class)
                .blockTransform((be, a) -> {
                    a.translate(0.5D, 0D, 0.5D);
                    rotY(a, 90);
                    switch (be.getBlockState().getValue(MachineAnnihilatorBlock.FACING)) {
                        case WEST -> rotY(a, 90);
                        case SOUTH -> rotY(a, 180);
                        case EAST -> rotY(a, 270);
                        default -> { }
                    }
                });
        mesh(anni, "body", ANNI_BODY);
        mesh(anni, "roller", (be, f, t, ps) -> {
            ps.translate(0, 1.75, 0);
            ps.mulPose(Axis.ZN.rotationDegrees((float) (System.currentTimeMillis() * 0.15 % 360)));
            ps.translate(0, -1.75, 0);
            return true;
        }, ANNI_ROLLER);
        anni.hook((be, f, ps, buf, light, overlay, api) -> {
            float shift = (float) (-System.currentTimeMillis() / 3000D % 1D);
            AnnihilatorRenderer.MODEL.renderPart("Belt", ps,
                    new UvShiftConsumer(buf.getBuffer(RenderType.entityCutout(AnnihilatorRenderer.BELT_TEX)), shift, 0F), light);
        }).viewDistance(256).register();

        // RenderAshpit: Metadaten 3/5/2/4 -> 0/90/180/270, danach -90; Tuer schiebt, Innenteil nach Fuellung
        MachineSpecBuilder<MachineAshpitBlockEntity> ash = MachineRenderers.machine("hbm_m/ashpit", ModBlockEntities.ASHPIT_BE.get(), MachineAshpitBlockEntity.class)
                .blockTransform((be, a) -> {
                    a.translate(0.5D, 0D, 0.5D);
                    switch (ObjBerHelper.facing(be)) {
                        case EAST -> rotY(a, 90);
                        case NORTH -> rotY(a, 180);
                        case WEST -> rotY(a, 270);
                        default -> { }
                    }
                    rotY(a, -90);
                });
        mesh(ash, "main", ASH_MAIN);
        idleStatic(ash, "door", (be, f, t, ps) -> {
            float door = be.prevDoorAngle + (be.doorAngle - be.prevDoorAngle) * f;
            ps.translate(0, 0, door * 0.75D / 135D);
            return true;
        }, be -> be.prevDoorAngle != 0F || be.doorAngle != 0F, ASH_DOOR);
        select(ash, "inner", be -> be.isFull ? ASH_INNER_BURNING : ASH_INNER);
        ash.viewDistance(256).register();

        // RenderAutosaw: Kette Main -> ArmUpper -> ArmLower -> ArmTip -> Sawblade
        MachineSpecBuilder<MachineAutosawBlockEntity> saw = MachineRenderers.machine("hbm_m/autosaw", ModBlockEntities.AUTOSAW_BE.get(), MachineAutosawBlockEntity.class)
                .blockTransform((be, a) -> a.translate(0.5D, 0D, 0.5D));
        mesh(saw, "base", SAW_BASE);
        mesh(saw, "main", (be, f, t, ps) -> autosaw(be, f, ps, 0), SAW_MAIN);
        mesh(saw, "engine", (be, f, t, ps) -> autosaw(be, f, ps, 1), SAW_ENGINE);
        mesh(saw, "arm_upper", (be, f, t, ps) -> autosaw(be, f, ps, 2), SAW_ARM_UPPER);
        mesh(saw, "arm_lower", (be, f, t, ps) -> autosaw(be, f, ps, 3), SAW_ARM_LOWER);
        mesh(saw, "arm_tip", (be, f, t, ps) -> autosaw(be, f, ps, 4), SAW_ARM_TIP);
        mesh(saw, "sawblade", (be, f, t, ps) -> autosaw(be, f, ps, 5), SAW_BLADE);
        saw.viewDistance(256).register();

        // RenderBoiler: Metadaten 3/5/2/4 -> 0/90/180/270; fast voll = Atmen (ungleichmaessige Skalierung -> immediate)
        MachineSpecBuilder<MachineBoilerBlockEntity> boiler = MachineRenderers.machine("hbm_m/boiler", ModBlockEntities.BOILER_BE.get(), MachineBoilerBlockEntity.class)
                .blockTransform((be, a) -> {
                    a.translate(0.5D, 0D, 0.5D);
                    switch (be.getBlockState().getValue(MachineBoilerBlock.FACING)) {
                        case EAST -> rotY(a, 90);
                        case NORTH -> rotY(a, 180);
                        case WEST -> rotY(a, 270);
                        default -> { }
                    }
                });
        select(boiler, "body", be -> {
            if (be.hasExploded) return BOILER_BURST;
            // Atmen nur, solange der Hook gezeichnet wird; jenseits davon die ruhende Form
            return boilerBreathing(be) && RenderDistanceHelper.computeAnimatedFade(be) >= 0 ? null : BOILER;
        });
        boiler.hook((be, f, ps, buf, light, overlay, api) -> {
            if (be.hasExploded || !boilerBreathing(be)) return;
            ps.pushPose();
            double sine = Math.sin(System.currentTimeMillis() / 50D % (Math.PI * 2));
            sine *= 0.01D;
            ps.scale((float) (1 - sine), (float) (1 + sine), (float) (1 - sine));
            BoilerRenderer.MODEL.renderAll(ps, buf.getBuffer(RenderType.entityCutout(BoilerRenderer.TEX)), light);
            ps.popPose();
        }).viewDistance(256).register();

        // RenderBreeder: 90 + Metadaten 2/4/3/5 -> 0/90/180/270; Funken immediate
        mesh(MachineRenderers.machine("hbm_m/breeder", ModBlockEntities.BREEDER_BE.get(), MachineBreederBlockEntity.class)
                .blockTransform((be, a) -> {
                    a.translate(0.5D, 0D, 0.5D);
                    rotY(a, 90);
                    switch (ObjBerHelper.meta(be)) {
                        case 4 -> rotY(a, 90);
                        case 3 -> rotY(a, 180);
                        case 5 -> rotY(a, 270);
                        default -> { }
                    }
                }), "body", BREEDER)
                .hook((be, f, ps, buf, light, overlay, api) -> {
                    if (be.getProgress() <= 0) return;
                    for (int i = 0; i < 3; i++) {
                        ps.pushPose();
                        // Original: glRotatef((float)(Math.PI * i), 0, 1, 0) - Grad, nicht Bogenmass
                        ObjBerHelper.rotY(ps, (float) (Math.PI * i));
                        RenderSparks.renderSpark(ps, buf, (int) ((System.currentTimeMillis() % 10000) / 100 + i), 0, 1.5625, 0, 0.15F, 3, 4, 0x00ff00, 0xffffff);
                        ps.popPose();
                    }
                })
                .viewDistance(64).register();

        // RenderChemicalFactory: 90 + FACING; Luefter gegenlaeufig um ihre Achsen
        MachineSpecBuilder<MachineChemicalFactoryBlockEntity> cf = MachineRenderers.machine("hbm_m/chemical_factory", ModBlockEntities.CHEMICAL_FACTORY_BE.get(), MachineChemicalFactoryBlockEntity.class)
                .blockTransform((be, a) -> {
                    a.translate(0.5, 0, 0.5);
                    rotY(a, 90);
                    Direction facing = be.getBlockState().hasProperty(HorizontalDirectionalBlock.FACING)
                            ? be.getBlockState().getValue(HorizontalDirectionalBlock.FACING) : Direction.NORTH;
                    switch (facing) {
                        case WEST -> rotY(a, 90);
                        case SOUTH -> rotY(a, 180);
                        case EAST -> rotY(a, 270);
                        default -> { }
                    }
                });
        mesh(cf, "base", CF_BASE);
        when(cf, "frame", be -> be.frame, CF_FRAME);
        mesh(cf, "fan1", (be, f, t, ps) -> {
            float anim = be.prevAnim + (be.anim - be.prevAnim) * f;
            ps.translate(1, 0, 0);
            ps.mulPose(Axis.YP.rotationDegrees((float) (-anim * 45 % 360D)));
            ps.translate(-1, 0, 0);
            return true;
        }, CF_FAN1);
        mesh(cf, "fan2", (be, f, t, ps) -> {
            float anim = be.prevAnim + (be.anim - be.prevAnim) * f;
            ps.translate(-1, 0, 0);
            ps.mulPose(Axis.YP.rotationDegrees((float) (-anim * 45 % 360D)));
            ps.translate(1, 0, 0);
            return true;
        }, CF_FAN2);
        cf.viewDistance(256).register();

        // RenderCompressor (gross): Metadaten 3/5/2/4 -> 270/0/90/180
        MachineSpecBuilder<MachineCompressorBlockEntity> comp = MachineRenderers.machine("hbm_m/compressor", ModBlockEntities.COMPRESSOR_BE.get(), MachineCompressorBlockEntity.class)
                .blockTransform((be, a) -> {
                    a.translate(0.5D, 0D, 0.5D);
                    compressorRotate(a, be.getBlockState().getValue(DummyableMachineBlock.FACING));
                });
        mesh(comp, "body", COMP_BODY);
        mesh(comp, "pump", (be, f, t, ps) -> {
            float lift = be.prevPiston + (be.piston - be.prevPiston) * f;
            ps.translate(0, lift * 3 - 3, 0);
            return true;
        }, COMP_PUMP);
        mesh(comp, "fan", (be, f, t, ps) -> {
            float fan = be.prevFanSpin + (be.fanSpin - be.prevFanSpin) * f;
            ps.translate(0, 1.5, 0);
            ps.mulPose(Axis.XP.rotationDegrees(fan));
            ps.translate(0, -1.5, 0);
            return true;
        }, COMP_FAN);
        comp.viewDistance(256).register();

        // RenderCompressorCompact
        MachineSpecBuilder<MachineCompressorCompactBlockEntity> compact = MachineRenderers.machine("hbm_m/compressor_compact", ModBlockEntities.COMPRESSOR_COMPACT_BE.get(), MachineCompressorCompactBlockEntity.class)
                .blockTransform((be, a) -> {
                    a.translate(0.5D, 0D, 0.5D);
                    compressorRotate(a, be.getBlockState().getValue(DummyableMachineBlock.FACING));
                });
        mesh(compact, "body", COMPACT_BODY);
        mesh(compact, "fan1", (be, f, t, ps) -> condenserFan(ps, be.prevFanSpin + (be.fanSpin - be.prevFanSpin) * f), COMPACT_FAN1);
        mesh(compact, "fan2", (be, f, t, ps) -> condenserFan(ps, -(be.prevFanSpin + (be.fanSpin - be.prevFanSpin) * f)), COMPACT_FAN2);
        compact.viewDistance(256).register();

        // RenderCondenserPowered: Metadaten 2/4/3/5 -> 90/180/270/0; beschaedigt (nur Space) bleibt immediate
        MachineSpecBuilder<MachineCondenserPoweredBlockEntity> cond = MachineRenderers.machine("hbm_m/condenser_powered", ModBlockEntities.CONDENSER_POWERED_BE.get(), MachineCondenserPoweredBlockEntity.class)
                .blockTransform((be, a) -> {
                    a.translate(0.5D, 0D, 0.5D);
                    switch (be.getBlockState().getValue(DummyableMachineBlock.FACING)) {
                        case NORTH -> rotY(a, 90);
                        case WEST -> rotY(a, 180);
                        case SOUTH -> rotY(a, 270);
                        default -> { }
                    }
                });
        when(cond, "body", be -> !be.damaged, COND_BODY);
        when(cond, "fan1", (be, f, t, ps) -> condenserFan(ps, be.lastSpin + (be.spin - be.lastSpin) * f), be -> !be.damaged, COND_FAN1);
        when(cond, "fan2", (be, f, t, ps) -> condenserFan(ps, -(be.lastSpin + (be.spin - be.lastSpin) * f)), be -> !be.damaged, COND_FAN2);
        cond.hook((be, f, ps, buf, light, overlay, api) -> {
            if (!be.damaged) return;
            ps.pushPose();
            ps.mulPose(Axis.YP.rotationDegrees(180));
            CondenserPoweredRenderer.DAMAGED.renderAll(ps, buf.getBuffer(RenderType.entityCutoutNoCull(CondenserPoweredRenderer.DAMAGED_TEX)), light);
            ps.popPose();
        }).viewDistance(256).register();

        // RenderConveyorPress: Metadaten 2/4/3/5 -> 90/180/270/0; Stempel nur mit Stempelplatte; Band immediate
        MachineSpecBuilder<MachineConveyorPressBlockEntity> cp = MachineRenderers.machine("hbm_m/conveyor_press", ModBlockEntities.CONVEYOR_PRESS_BE.get(), MachineConveyorPressBlockEntity.class)
                .blockTransform((be, a) -> {
                    a.translate(0.5, 0, 0.5);
                    switch (ObjBerHelper.meta(be)) {
                        case 2 -> rotY(a, 90);
                        case 4 -> rotY(a, 180);
                        case 3 -> rotY(a, 270);
                        default -> { }
                    }
                });
        mesh(cp, "press", CP_PRESS);
        when(cp, "piston", (be, f, t, ps) -> {
            double piston = be.getRenderPress(f);
            ps.translate(0, -piston * 0.75, 0);
            return true;
        }, be -> !be.getStampStack().isEmpty(), CP_PISTON);
        cp.hook((be, f, ps, buf, light, overlay, api) -> {
            long time = be.getLevel() != null ? be.getLevel().getGameTime() : 0L;
            int ticks = (int) (time % 16) - 2;
            ConveyorPressRenderer.MODEL.renderPart("Belt", ps,
                    new UvShiftConsumer(buf.getBuffer(RenderType.entityCutout(ConveyorPressRenderer.BELT_TEX)), 0F, ticks / 16F), light);
        }).viewDistance(64).register();

        // RenderDieselGen: Metadaten 3/5/2/4 -> 270/0/90/180; Motor ruettelt nur im Betrieb
        MachineSpecBuilder<MachineDieselGeneratorBlockEntity> dg = MachineRenderers.machine("hbm_m/dieselgen", ModBlockEntities.DIESEL_GENERATOR_BE.get(), MachineDieselGeneratorBlockEntity.class)
                .blockTransform((be, a) -> {
                    a.translate(0.5D, 0D, 0.5D);
                    switch (ObjBerHelper.meta(be)) {
                        case 3 -> rotY(a, 270);
                        case 2 -> rotY(a, 90);
                        case 4 -> rotY(a, 180);
                        default -> { }
                    }
                });
        mesh(dg, "generator", DG_GEN);
        idleStatic(dg, "engine", (be, f, t, ps) -> {
            double swingSide = Math.sin(System.currentTimeMillis() / 50D) * 0.005;
            double swingFront = Math.sin(System.currentTimeMillis() / 25D) * 0.005;
            ps.translate(swingFront, 0, swingSide);
            return true;
        }, be -> be.hasAcceptableFuel() && be.getTank().getFill() > 0, DG_ENGINE);
        dg.viewDistance(64).register();

        // RenderEPress: Koerper und Kopf mit eigener Ausrichtung; Kopf faehrt, Gegenstand im Rahmen immediate
        MachineSpecBuilder<MachineEPressBlockEntity> ep = MachineRenderers.machine("hbm_m/epress", ModBlockEntities.EPRESS_BE.get(), MachineEPressBlockEntity.class)
                .blockTransform((be, a) -> {
                    a.translate(0.5D, 0D, 0.5D);
                    rotY(a, 180);
                    switch (ObjBerHelper.meta(be)) {
                        case 2 -> rotY(a, 270);
                        case 3 -> rotY(a, 90);
                        case 5 -> rotY(a, 180);
                        default -> { }
                    }
                });
        mesh(ep, "body", EP_BODY);
        // translate(0.5, 1, 0.5) vor der Drehung = translate(0, 1, 0) danach (Drehung um Y)
        mesh(ep, "head", (be, f, t, ps) -> {
            double pr = (be.lastPress + (be.renderPress - be.lastPress) * f) / (double) MachineEPressBlockEntity.getMaxPress();
            ps.translate(0D, 1D, 0D);
            ps.translate(0, Mth.clamp(1D - pr, 0D, 1D) * 0.875D, 0);
            return true;
        }, EP_HEAD);
        ep.hook((be, f, ps, buf, light, overlay, api) -> {
            if (be.getSyncStack().isEmpty()) return;
            ps.pushPose();
            ps.translate(0D, 1D, 0D);
            ps.mulPose(Axis.YP.rotationDegrees(90));
            ps.mulPose(Axis.XP.rotationDegrees(-90));
            ps.translate(1.0F, 1.0F - 0.0625F * 165 / 100, 0.0F);
            ps.translate(-1, -1.15F, 0);
            ObjBerHelper.renderDecoItemInFrame(ps, buf, be.getSyncStack(), light, be.getLevel());
            ps.popPose();
        }).viewDistance(64).register();

        // RenderFan: Rahmen nach FACING (6 Seiten), Fluegel drehen
        MachineSpecBuilder<MachineFanBlockEntity> fan = MachineRenderers.machine("hbm_m/fan", ModBlockEntities.FAN_BE.get(), MachineFanBlockEntity.class)
                .blockTransform((be, a) -> {
                    a.translate(0.5D, 0.5D, 0.5D);
                    switch (be.getBlockState().getValue(MachineFanBlock.FACING)) {
                        case DOWN -> a.rotate(180, 1, 0, 0);
                        case NORTH -> a.rotate(-90, 1, 0, 0);
                        case WEST -> a.rotate(90, 0, 0, 1);
                        case SOUTH -> a.rotate(90, 1, 0, 0);
                        case EAST -> a.rotate(-90, 0, 0, 1);
                        default -> { }
                    }
                    a.translate(0D, -0.5D, 0D);
                });
        mesh(fan, "frame", FAN_FRAME);
        mesh(fan, "blades", (be, f, t, ps) -> {
            float rot = be.prevSpin + (be.spin - be.prevSpin) * f;
            ps.mulPose(Axis.YP.rotationDegrees(-rot));
            return true;
        }, FAN_BLADES);
        fan.viewDistance(256).register();

        // RenderFileCabinet: Schubladen fahren aus; Holz/Stahl nach Blocksorte
        MachineSpecBuilder<FileCabinetBlockEntity> cab = MachineRenderers.machine("hbm_m/file_cabinet", ModBlockEntities.FILE_CABINET.get(), FileCabinetBlockEntity.class)
                .blockTransform((be, a) -> {
                    a.translate(0.5D, 0.0D, 0.5D);
                    if (!(be.getBlockState().getBlock() instanceof FileCabinetBlock)) return;
                    switch (be.getBlockState().getValue(FileCabinetBlock.FACING)) {
                        case NORTH -> rotY(a, 180);
                        case WEST -> rotY(a, 270);
                        case EAST -> rotY(a, 90);
                        default -> { }
                    }
                });
        select(cab, "cabinet", be -> cabinetMesh(be, CAB));
        select(cab, "lower", (be, f, t, ps) -> {
            float lower = be.prevLowerExtent + (be.lowerExtent - be.prevLowerExtent) * f;
            ps.translate(0F, 0F, 0.6875F * lower);
            return true;
        }, be -> drawerMoving(be, true) ? cabinetMesh(be, CAB_LOWER) : null);
        select(cab, "lower_idle", be -> drawerMoving(be, true) ? null : cabinetMesh(be, CAB_LOWER));
        select(cab, "upper", (be, f, t, ps) -> {
            float upper = be.prevUpperExtent + (be.upperExtent - be.prevUpperExtent) * f;
            ps.translate(0F, 0F, 0.6875F * upper);
            return true;
        }, be -> drawerMoving(be, false) ? cabinetMesh(be, CAB_UPPER) : null);
        select(cab, "upper_idle", be -> drawerMoving(be, false) ? null : cabinetMesh(be, CAB_UPPER));
        cab.viewDistance(64).register();

        // RenderIndustrialTurbine: FACING; Zeiger nach Dampfsorte, Schwungrad dreht
        MachineSpecBuilder<MachineIndustrialTurbineBlockEntity> it = MachineRenderers.machine("hbm_m/industrial_turbine", ModBlockEntities.INDUSTRIAL_TURBINE_BE.get(), MachineIndustrialTurbineBlockEntity.class)
                .blockTransform((be, a) -> {
                    a.translate(0.5, 0, 0.5);
                    Direction facing = be.getBlockState().hasProperty(HorizontalDirectionalBlock.FACING)
                            ? be.getBlockState().getValue(HorizontalDirectionalBlock.FACING) : Direction.NORTH;
                    switch (facing) {
                        case NORTH -> rotY(a, 180);
                        case WEST -> rotY(a, 270);
                        case EAST -> rotY(a, 90);
                        default -> { }
                    }
                });
        mesh(it, "body", IT_BODY);
        select(it, "gauge", be -> {
            int id = ChungusRenderer.steamId(be.getSteamTank().getTankType());
            return IT_GAUGE[Mth.clamp(id - 2, 0, IT_GAUGE.length - 1)];
        });
        mesh(it, "flywheel", (be, f, t, ps) -> {
            ps.translate(0, 1.5, 0);
            ps.mulPose(Axis.ZN.rotationDegrees(be.getRotor(f)));
            ps.translate(0, -1.5, 0);
            return true;
        }, IT_FLYWHEEL);
        it.viewDistance(256).register();

        // RenderIntake: Metadaten 2/4/3/5 -> 90/180/270/0, Versatz; Luefter dreht
        MachineSpecBuilder<MachineIntakeBlockEntity> intake = MachineRenderers.machine("hbm_m/intake", ModBlockEntities.INTAKE_BE.get(), MachineIntakeBlockEntity.class)
                .blockTransform((be, a) -> {
                    a.translate(0.5D, 0D, 0.5D);
                    switch (be.getBlockState().getValue(MachineIntakeBlock.FACING)) {
                        case NORTH -> rotY(a, 90);
                        case WEST -> rotY(a, 180);
                        case SOUTH -> rotY(a, 270);
                        default -> { }
                    }
                    a.translate(-0.5D, 0D, 0.5D);
                });
        mesh(intake, "base", INTAKE_BASE);
        mesh(intake, "fan", (be, f, t, ps) -> {
            float rot = be.prevFan + (be.fan - be.prevFan) * f;
            ps.mulPose(Axis.YP.rotationDegrees(-rot));
            return true;
        }, INTAKE_FAN);
        intake.viewDistance(256).register();
    }

    // ── Hilfen ───────────────────────────────────────────────────────────────────────────

    private static boolean ammoShells(MachineAmmoPressBlockEntity be, float f, long t, PoseStack ps) {
        float lift = be.prevLift + (be.lift - be.prevLift) * f;
        ps.translate(0, lift * 0.5F - 0.5F, 0);
        return true;
    }

    /** AutosawRenderer.renderCommon bis zum Glied {@code stage} (0 Main, 1 Engine, 2 ArmUpper, 3 ArmLower, 4 ArmTip, 5 Sawblade). */
    private static boolean autosaw(MachineAutosawBlockEntity saw, float interp, PoseStack ps, int stage) {
        double turn = saw.prevRotationYaw + (saw.rotationYaw - saw.prevRotationYaw) * interp;
        double angle = 80 - (saw.prevRotationPitch + (saw.rotationPitch - saw.prevRotationPitch) * interp);
        float spin = saw.lastSpin + (saw.spin - saw.lastSpin) * interp;
        ps.mulPose(Axis.YN.rotationDegrees((float) turn));
        if (stage == 0) return true;
        if (stage == 1) {
            double engine = saw.isOn && saw.getLevel() != null
                    ? Math.sin(saw.getLevel().getGameTime() * 2 % (Math.PI * 2) + interp) : 0;
            ps.translate(0, engine * 0.01, 0);
            return true;
        }
        ps.translate(0, 1.75, 0);
        ps.mulPose(Axis.XP.rotationDegrees((float) angle));
        ps.translate(0, -1.75, 0);
        if (stage == 2) return true;
        ps.translate(0, 1.75, -4);
        ps.mulPose(Axis.XP.rotationDegrees((float) (angle * -2)));
        ps.translate(0, -1.75, 4);
        ps.translate(-0.01, 0, 0);
        if (stage == 3) return true;
        ps.translate(0.01, 0, 0);
        ps.translate(0, 1.75, -8);
        ps.mulPose(Axis.XP.rotationDegrees((float) angle));
        ps.translate(0, -1.75, 8);
        if (stage == 4) return true;
        ps.translate(0, 1.75, -10);
        ps.mulPose(Axis.YN.rotationDegrees(spin));
        ps.translate(0, -1.75, 10);
        return true;
    }

    private static boolean boilerBreathing(MachineBoilerBlockEntity boiler) {
        return boiler.tanks[1].getFill() > boiler.tanks[1].getMaxFill() * 0.9;
    }

    /** CompressorRenderer.rotate: Metadaten 3/5/2/4 -> 270/0/90/180. */
    private static void compressorRotate(com.hbm_m.client.render.LegacyAnimator a, Direction dir) {
        switch (dir) {
            case SOUTH -> rotY(a, 270);
            case NORTH -> rotY(a, 90);
            case WEST -> rotY(a, 180);
            default -> { }
        }
    }

    private static boolean condenserFan(PoseStack ps, float rot) {
        ps.translate(0, 1.5, 0);
        ps.mulPose(Axis.XP.rotationDegrees(rot));
        ps.translate(0, -1.5, 0);
        return true;
    }

    private static ObjAtlasMesh cabinetMesh(FileCabinetBlockEntity be, ObjAtlasMesh[] pair) {
        if (!(be.getBlockState().getBlock() instanceof FileCabinetBlock block)) return null;
        return pair[block.steel ? 1 : 0];
    }

    /** Schublade nicht ganz zu: bewegtes Teil; zu: statisch (Modelllage, bleibt bis zur statischen Sichtweite). */
    private static boolean drawerMoving(FileCabinetBlockEntity be, boolean lower) {
        return lower ? (be.prevLowerExtent != 0F || be.lowerExtent != 0F) : (be.prevUpperExtent != 0F || be.upperExtent != 0F);
    }
}
