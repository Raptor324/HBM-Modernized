package com.hbm_m.client.render.implementations;

import static com.hbm_m.client.render.raptorpath.RaptorParts.mesh;
import static com.hbm_m.client.render.raptorpath.RaptorParts.rotY;
import static com.hbm_m.client.render.raptorpath.RaptorParts.select;
import static com.hbm_m.client.render.raptorpath.RaptorParts.when;

import org.joml.Matrix4f;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.decorations.DecoModelTEBlock;
import com.hbm_m.block.decorations.VendingMachineBlock;
import com.hbm_m.block.machines.DemonLampBlock;
import com.hbm_m.block.machines.DummyableMachineBlock;
import com.hbm_m.block.machines.MachineChimneyBlock;
import com.hbm_m.block.machines.MachineTapeDriveBlock;
import com.hbm_m.block.rail.RailDummyableBlock;
import com.hbm_m.block.rail.RailSwitchBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.decorations.DecoBlockEntity;
import com.hbm_m.blockentity.decorations.VendingMachineBlockEntity;
import com.hbm_m.blockentity.machines.DemonLampBlockEntity;
import com.hbm_m.blockentity.machines.MachineArcWelderBlockEntity;
import com.hbm_m.blockentity.machines.MachineCatalyticReformerBlockEntity;
import com.hbm_m.blockentity.machines.MachineChimneyBlockEntity;
import com.hbm_m.blockentity.machines.MachineCoolingTowerBlockEntity;
import com.hbm_m.blockentity.machines.MachineCrackingTowerBlockEntity;
import com.hbm_m.blockentity.machines.MachineElectrolyserBlockEntity;
import com.hbm_m.blockentity.machines.MachineFractionTowerBlockEntity;
import com.hbm_m.blockentity.machines.MachineFurnaceSteelBlockEntity;
import com.hbm_m.blockentity.machines.MachineRTGBlockEntity;
import com.hbm_m.blockentity.machines.MachineRadiolysisBlockEntity;
import com.hbm_m.blockentity.machines.MachineSilexBlockEntity;
import com.hbm_m.blockentity.machines.MachineSolderingStationBlockEntity;
import com.hbm_m.blockentity.machines.MachineTapeDriveBlockEntity;
import com.hbm_m.blockentity.machines.MachineTowerSmallBlockEntity;
import com.hbm_m.blockentity.machines.MachineTurbineGasBlockEntity;
import com.hbm_m.blockentity.machines.MachineVacuumDistillBlockEntity;
import com.hbm_m.client.ClientRenderHandler;
import com.hbm_m.client.render.machine.MachineRenderers;
import com.hbm_m.client.render.machine.MachineSpecBuilder;
import com.hbm_m.client.render.raptorpath.ObjAtlasMesh;
import com.hbm_m.client.render.util.HorsePronter;
import com.hbm_m.interfaces.IEnergyConnector;
import com.hbm_m.item.ModItems;
import com.hbm_m.main.Polaroid;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Render-Migration Paket P1 (Kategorie A, statisch bzw. Zustand; dazu die kleinen immediate-Reste als Hook).
 * Transformationen 1:1 aus den alten BERs, die unveraendert im Code bleiben ({@code -Dhbm_m.legacyMachineRender}).
 * Ohne {@code getViewDistance} im alten BER: Vanilla-Standard 64.
 */
//? if forge {
@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
//?} elif fabric {
/*@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
*///?} elif neoforge {
/*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
*///?}
public final class RaptorPathP1 {

    private RaptorPathP1() {}

    // ── Geometrie (gleiche SimpleObjModel-Instanzen wie die alten BERs) ──────────────────

    private static final ObjAtlasMesh CRACKER = ObjAtlasMesh.all(CatalyticCrackerRenderer.MODEL, CatalyticCrackerRenderer.TEX, "hbm_m/catalytic_cracker");
    private static final ObjAtlasMesh REFORMER = ObjAtlasMesh.all(CatalyticReformerRenderer.MODEL, CatalyticReformerRenderer.TEX, "hbm_m/catalytic_reformer");
    private static final ObjAtlasMesh CHIMNEY_BRICK = ObjAtlasMesh.all(ChimneyRenderer.BRICK, ChimneyRenderer.BRICK_TEX, "hbm_m/chimney_brick");
    private static final ObjAtlasMesh CHIMNEY_INDUSTRIAL = ObjAtlasMesh.all(ChimneyRenderer.INDUSTRIAL, ChimneyRenderer.INDUSTRIAL_TEX, "hbm_m/chimney_industrial");
    private static final ObjAtlasMesh TOWER_LARGE = ObjAtlasMesh.all(CoolingTowerRenderer.TOWER_LARGE, CoolingTowerRenderer.TOWER_LARGE_TEX, "hbm_m/tower_large");
    private static final ObjAtlasMesh TOWER_SMALL = ObjAtlasMesh.all(CoolingTowerRenderer.TOWER_SMALL, CoolingTowerRenderer.TOWER_SMALL_TEX, "hbm_m/tower_small");
    private static final ObjAtlasMesh BOXCAR = ObjAtlasMesh.all(DecoBlockRenderer.BOXCAR, DecoBlockRenderer.BOXCAR_TEX, "hbm_m/deco_boxcar");
    private static final ObjAtlasMesh DUCHESS = ObjAtlasMesh.all(DecoBlockRenderer.DUCHESS, DecoBlockRenderer.DUCHESS_TEX, "hbm_m/deco_duchess");
    private static final ObjAtlasMesh DEMON_LAMP = ObjAtlasMesh.all(DemonLampRenderer.MODEL, DemonLampRenderer.TEX, "hbm_m/demon_lamp");
    private static final ObjAtlasMesh ELECTROLYSER = ObjAtlasMesh.all(ElectrolyserRenderer.MODEL, ElectrolyserRenderer.TEX, "hbm_m/electrolyser");
    private static final ObjAtlasMesh FRACTION = ObjAtlasMesh.all(FractionTowerRenderer.MODEL, FractionTowerRenderer.TEX, "hbm_m/fraction_tower");
    private static final ObjAtlasMesh RADIOLYSIS = ObjAtlasMesh.all(RadiolysisRenderer.MODEL, RadiolysisRenderer.TEX, "hbm_m/radiolysis");
    private static final ObjAtlasMesh SILEX = ObjAtlasMesh.all(SilexRenderer.MODEL, SilexRenderer.TEX, "hbm_m/silex");
    private static final ObjAtlasMesh TURBINE_GAS = ObjAtlasMesh.all(TurbineGasRenderer.MODEL, TurbineGasRenderer.TEX, "hbm_m/turbinegas");
    private static final ObjAtlasMesh VACUUM = ObjAtlasMesh.all(VacuumDistillRenderer.MODEL, VacuumDistillRenderer.TEX, "hbm_m/vacuum_distill");
    private static final ObjAtlasMesh VENDING_SODA = ObjAtlasMesh.parts(VendingMachineRenderer.MODEL, VendingMachineRenderer.TEX, "hbm_m/vending_soda", "Soda");
    private static final ObjAtlasMesh VENDING_SNACKS = ObjAtlasMesh.parts(VendingMachineRenderer.MODEL, VendingMachineRenderer.TEX, "hbm_m/vending_snacks", "Obamna");
    private static final ObjAtlasMesh ARC_WELDER = ObjAtlasMesh.all(WorkbenchMachineRenderers.ARC_WELDER, WorkbenchMachineRenderers.ARC_WELDER_TEX, "hbm_m/arc_welder");
    private static final ObjAtlasMesh SOLDERING = ObjAtlasMesh.all(WorkbenchMachineRenderers.SOLDERING, WorkbenchMachineRenderers.SOLDERING_TEX, "hbm_m/soldering_station");
    private static final ObjAtlasMesh FURNACE_STEEL = ObjAtlasMesh.all(FurnaceSteelRenderer.MODEL, FurnaceSteelRenderer.TEX, "hbm_m/furnace_steel");
    private static final ObjAtlasMesh TAPE_FRAME = ObjAtlasMesh.parts(TapeDriveRenderer.MODEL, TapeDriveRenderer.TEX, "hbm_m/tape_drive/frame", "Frame");

    private static final net.minecraft.resources.ResourceLocation RTG_TEX =
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.hbm_m.lib.RefStrings.MODID, "textures/block/machine/rtg.png");

    // RTG: Verbinder je Nachbar (Original: Drehung um die Blockmitte vor renderPart, eingebacken)
    private static final ObjAtlasMesh RTG_EAST = rtgConnector("e", 0F);
    private static final ObjAtlasMesh RTG_WEST = rtgConnector("w", 180F);
    private static final ObjAtlasMesh RTG_NORTH = rtgConnector("n", 90F);
    private static final ObjAtlasMesh RTG_SOUTH = rtgConnector("s", -90F);

    private static ObjAtlasMesh rtgConnector(String side, float yaw) {
        return ObjAtlasMesh.of("hbm_m/rtg/connector_" + side, b -> {
            b.pose.mulPose(Axis.YP.rotationDegrees(yaw));
            b.obj(RTGRenderer.MODEL, RTG_TEX, "Connector");
        });
    }

    // Weichen: Schild gerade/abgebogen, normal/gespiegelt (Modelle wie RailSwitchRenderer, dort privat)
    private static final com.hbm_m.client.render.SimpleObjModel RAIL_SWITCH_MODEL = new com.hbm_m.client.render.SimpleObjModel(
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.hbm_m.lib.RefStrings.MODID, "models/block/rail/rail_standard_switch.obj"));
    private static final com.hbm_m.client.render.SimpleObjModel RAIL_SWITCH_FLIPPED_MODEL = new com.hbm_m.client.render.SimpleObjModel(
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.hbm_m.lib.RefStrings.MODID, "models/block/rail/rail_standard_switch_flipped.obj"));
    private static final ObjAtlasMesh[] RAIL_SIGNS = {
            railSign(false, false), railSign(false, true), railSign(true, false), railSign(true, true) };

    private static ObjAtlasMesh railSign(boolean flipped, boolean switched) {
        String part = switched ? "SignTurn" : "SignStraight";
        return ObjAtlasMesh.parts(flipped ? RAIL_SWITCH_FLIPPED_MODEL : RAIL_SWITCH_MODEL,
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.hbm_m.lib.RefStrings.MODID,
                        flipped ? "textures/block/rail_switch_sign_flipped.png" : "textures/block/rail_switch_sign.png"),
                "hbm_m/rail_switch/" + (flipped ? "flipped_" : "") + part, part);
    }

    // Bandlaufwerk: je Schacht ein Laufwerk mit eingebackenem Versatz
    private static final ObjAtlasMesh[] TAPE_DRIVES = new ObjAtlasMesh[MachineTapeDriveBlockEntity.SLOT_COUNT];

    static {
        for (int i = 0; i < TAPE_DRIVES.length; i++) {
            final int slot = i;
            TAPE_DRIVES[i] = ObjAtlasMesh.of("hbm_m/tape_drive/drive" + i, b -> {
                b.pose.translate(0, 0.25 - 0.5 * (slot / 6), 0.3125 - (slot % 6) * 0.125);
                b.obj(TapeDriveRenderer.MODEL, TapeDriveRenderer.TEX, "Drive");
            });
        }
    }

    public static void register() {
        // RenderCatalyticCracker: Metadaten 2/4/3/5 -> 90/180/270/0
        mesh(MachineRenderers.machine("hbm_m/catalytic_cracker", ModBlockEntities.CRACKING_TOWER_BE.get(), MachineCrackingTowerBlockEntity.class)
                .blockTransform((be, a) -> {
                    a.translate(0.5, 0, 0.5);
                    switch (be.getBlockState().getValue(DummyableMachineBlock.FACING)) {
                        case NORTH -> rotY(a, 90);
                        case WEST -> rotY(a, 180);
                        case SOUTH -> rotY(a, 270);
                        default -> { }
                    }
                }), "body", CRACKER)
                .viewDistance(256).register();

        // RenderCatalyticReformer: Metadaten 2/4/3/5 -> 90/180/270/0; Polaroid-11-Pferd immediate
        mesh(MachineRenderers.machine("hbm_m/catalytic_reformer", ModBlockEntities.CATALYTIC_REFORMER_BE.get(), MachineCatalyticReformerBlockEntity.class)
                .blockTransform((be, a) -> {
                    a.translate(0.5, 0, 0.5);
                    switch (ObjBerHelper.meta(be)) {
                        case 2 -> rotY(a, 90);
                        case 4 -> rotY(a, 180);
                        case 3 -> rotY(a, 270);
                        default -> { }
                    }
                }), "body", REFORMER)
                .hook((be, f, ps, buf, light, overlay, api) -> {
                    if (Polaroid.id() != 11) return;
                    ps.pushPose();
                    ps.translate(-1.125, 1.375, 1);
                    float s = 0.125F;
                    ps.scale(s, s, s);
                    ps.mulPose(Axis.YN.rotationDegrees((float) (System.currentTimeMillis() / 5D % 360D)));
                    ps.translate(0, 0.1, -0.5);
                    HorsePronter.reset();
                    double r = 60;
                    HorsePronter.pose(HorsePronter.id_body, 0, -r, 0);
                    HorsePronter.pose(HorsePronter.id_tail, 0, 45, 90);
                    HorsePronter.pose(HorsePronter.id_lbl, 0, -90 + r, 35);
                    HorsePronter.pose(HorsePronter.id_rbl, 0, -90 + r, -35);
                    HorsePronter.pose(HorsePronter.id_lfl, 0, r - 10, 5);
                    HorsePronter.pose(HorsePronter.id_rfl, 0, r - 10, -5);
                    HorsePronter.pose(HorsePronter.id_head, 0, r, 0);
                    HorsePronter.enableHorn();
                    HorsePronter.enableWings();
                    HorsePronter.pront(ps, buf.getBuffer(RenderType.entityCutoutNoCull(CatalyticReformerRenderer.EXTRA)), light);
                    double scale = 0.25;
                    ps.translate(0.02, 1.13, -0.42);
                    ps.scale((float) scale, (float) scale, (float) scale);
                    ps.mulPose(Axis.YN.rotationDegrees(90));
                    ps.mulPose(Axis.ZN.rotationDegrees(60));
                    TrinketRenderer.renderItem2D(ps, buf, new ItemStack(ModItems.CIGARETTE.get()), light);
                    ps.popPose();
                })
                .viewDistance(256).register();

        // RenderChimneyBrick/Industrial: 180 Grad, Modell nach Blocksorte
        select(MachineRenderers.machine("hbm_m/chimney", ModBlockEntities.CHIMNEY_BE.get(), MachineChimneyBlockEntity.class)
                .blockTransform((be, a) -> {
                    a.translate(0.5, 0, 0.5);
                    rotY(a, 180);
                }), "body", be -> be.getBlockState().getBlock() instanceof MachineChimneyBlock c && c.isIndustrial()
                        ? CHIMNEY_INDUSTRIAL : CHIMNEY_BRICK)
                .viewDistance(256).register();

        // RenderCoolingTower (gross/klein): nur Versatz zur Blockmitte
        mesh(MachineRenderers.machine("hbm_m/tower_large", ModBlockEntities.COOLING_TOWER_BE.get(), MachineCoolingTowerBlockEntity.class)
                .blockTransform((be, a) -> a.translate(0.5, 0, 0.5)), "body", TOWER_LARGE)
                .viewDistance(256).register();
        mesh(MachineRenderers.machine("hbm_m/tower_small", ModBlockEntities.TOWER_SMALL_BE.get(), MachineTowerSmallBlockEntity.class)
                .blockTransform((be, a) -> a.translate(0.5, 0, 0.5)), "body", TOWER_SMALL)
                .viewDistance(256).register();

        // RenderDecoBlock (Boxcar / Duchess Gambit)
        select(MachineRenderers.machine("hbm_m/deco_block", ModBlockEntities.DECO_BLOCK.get(), DecoBlockEntity.class)
                .blockTransform(RaptorPathP1::decoTransform), "body", be -> {
                    if (!(be.getBlockState().getBlock() instanceof DecoModelTEBlock block)) return null;
                    return block.kind == DecoModelTEBlock.Kind.BOXCAR ? BOXCAR : DUCHESS;
                })
                .viewDistance(64).register();

        // RenderDemonLamp: Lampe nach FACING, Lichtkegel immediate
        mesh(MachineRenderers.machine("hbm_m/demon_lamp", ModBlockEntities.DEMON_LAMP.get(), DemonLampBlockEntity.class)
                .blockTransform((be, a) -> {
                    a.translate(0.5, 0.5, 0.5);
                    switch (be.getBlockState().getValue(DemonLampBlock.FACING)) {
                        case DOWN -> a.rotate(180, 1, 0, 0);
                        case UP -> { }
                        case NORTH -> { a.rotate(90, 1, 0, 0); a.rotate(180, 0, 0, 1); }
                        case SOUTH -> a.rotate(90, 1, 0, 0);
                        case WEST -> { a.rotate(90, 1, 0, 0); a.rotate(90, 0, 0, 1); }
                        case EAST -> { a.rotate(90, 1, 0, 0); a.rotate(270, 0, 0, 1); }
                    }
                    a.translate(0, -0.5F, 0);
                }), "body", DEMON_LAMP)
                .hook((be, f, ps, buf, light, overlay, api) -> demonLampGlow(ps, buf))
                .viewDistance(256).register();

        // RenderElectrolyser: FACING WEST/SOUTH/EAST -> 90/180/270, danach 180
        mesh(MachineRenderers.machine("hbm_m/electrolyser", ModBlockEntities.ELECTROLYSER_BE.get(), MachineElectrolyserBlockEntity.class)
                .blockTransform((be, a) -> {
                    a.translate(0.5, 0, 0.5);
                    switch (be.getBlockState().getValue(DummyableMachineBlock.FACING)) {
                        case WEST -> rotY(a, 90);
                        case SOUTH -> rotY(a, 180);
                        case EAST -> rotY(a, 270);
                        default -> { }
                    }
                    rotY(a, 180);
                }), "body", ELECTROLYSER)
                .viewDistance(256).register();

        // RenderFractionTower
        mesh(MachineRenderers.machine("hbm_m/fraction_tower", ModBlockEntities.FRACTION_TOWER_BE.get(), MachineFractionTowerBlockEntity.class)
                .blockTransform((be, a) -> a.translate(0.5, 0, 0.5)), "body", FRACTION)
                .viewDistance(256).register();

        // RenderRTG: Verbinder zu energiefuehrenden Nachbarn (Library.canConnect)
        MachineSpecBuilder<MachineRTGBlockEntity> rtg = MachineRenderers.machine("hbm_m/rtg", ModBlockEntities.MACHINE_RTG_BE.get(), MachineRTGBlockEntity.class)
                .blockTransform((be, a) -> {
                    a.translate(0.5, 0, 0.5);
                    rotY(a, 180);
                });
        when(rtg, "con_e", be -> rtgConnects(be, BlockPos::east, Direction.WEST), RTG_EAST);
        when(rtg, "con_w", be -> rtgConnects(be, BlockPos::west, Direction.EAST), RTG_WEST);
        when(rtg, "con_n", be -> rtgConnects(be, BlockPos::north, Direction.SOUTH), RTG_NORTH);
        when(rtg, "con_s", be -> rtgConnects(be, BlockPos::south, Direction.NORTH), RTG_SOUTH);
        rtg.viewDistance(64).register();

        // RenderRadiolysis: Metadaten 2/3/4/5 -> 180/0/90/270
        mesh(MachineRenderers.machine("hbm_m/radiolysis", ModBlockEntities.RADIOLYSIS_BE.get(), MachineRadiolysisBlockEntity.class)
                .blockTransform((be, a) -> {
                    a.translate(0.5, 0, 0.5);
                    switch (be.getBlockState().getValue(DummyableMachineBlock.FACING)) {
                        case NORTH -> rotY(a, 180);
                        case WEST -> rotY(a, 90);
                        case EAST -> rotY(a, 270);
                        default -> { }
                    }
                }), "body", RADIOLYSIS)
                .viewDistance(256).register();

        // RenderRailSwitch (Weichenschild): nur Kernmetadaten 12-15
        select(MachineRenderers.machine("hbm_m/rail_switch", ModBlockEntities.RAIL_SWITCH.get(), RailSwitchBlockEntity.class)
                .blockTransform((be, a) -> {
                    int meta = railMeta(be);
                    if (meta < 12) return;
                    if (meta == 12) a.translate(0.5F, 0F, 0F);
                    if (meta == 13) a.translate(-0.5F, 0F, 0F);
                    if (meta == 14) a.translate(0F, 0F, -0.5F);
                    if (meta == 15) a.translate(0F, 0F, 0.5F);
                    a.translate(0.5F, 0F, 0.5F);
                    if (meta == 15) rotY(a, 90F);
                    if (meta == 12) rotY(a, 180F);
                    if (meta == 14) rotY(a, 270F);
                }), "sign", be -> {
                    if (railMeta(be) < 12) return null;
                    boolean flipped = be.getBlockState().is(ModBlocks.RAIL_LARGE_SWITCH_FLIPPED.get());
                    return RAIL_SIGNS[(flipped ? 2 : 0) + (be.isSwitched ? 1 : 0)];
                })
                .viewDistance(64).register();

        // RenderSilex: Metadaten 4/3/5/2 -> 180/270/0/90
        mesh(MachineRenderers.machine("hbm_m/silex", ModBlockEntities.SILEX_BE.get(), MachineSilexBlockEntity.class)
                .blockTransform((be, a) -> {
                    a.translate(0.5, 0, 0.5);
                    switch (be.getBlockState().getValue(DummyableMachineBlock.FACING)) {
                        case WEST -> rotY(a, 180);
                        case SOUTH -> rotY(a, 270);
                        case NORTH -> rotY(a, 90);
                        default -> { }
                    }
                }), "body", SILEX)
                .viewDistance(256).register();

        // RenderTurbineGas: Metadaten 2/4/3/5 -> 90/180/270/0
        mesh(MachineRenderers.machine("hbm_m/turbinegas", ModBlockEntities.TURBINEGAS_BE.get(), MachineTurbineGasBlockEntity.class)
                .blockTransform((be, a) -> {
                    a.translate(0.5, 0, 0.5);
                    switch (be.getBlockState().getValue(DummyableMachineBlock.FACING)) {
                        case NORTH -> rotY(a, 90);
                        case WEST -> rotY(a, 180);
                        case SOUTH -> rotY(a, 270);
                        default -> { }
                    }
                }), "body", TURBINE_GAS)
                .viewDistance(256).register();

        // RenderVacuumDistill
        mesh(MachineRenderers.machine("hbm_m/vacuum_distill", ModBlockEntities.VACUUM_DISTILL_BE.get(), MachineVacuumDistillBlockEntity.class)
                .blockTransform((be, a) -> a.translate(0.5, 0, 0.5)), "body", VACUUM)
                .viewDistance(256).register();

        // RenderVendingMachine: nur mit belegtem Oberteil, Soda/Snacks nach Blocksorte
        select(MachineRenderers.machine("hbm_m/vending_machine", ModBlockEntities.VENDING_MACHINE.get(), VendingMachineBlockEntity.class)
                .blockTransform((be, a) -> {
                    a.translate(0.5D, 0.0D, 0.5D);
                    BlockState state = be.getBlockState();
                    if (!(state.getBlock() instanceof VendingMachineBlock)) return;
                    switch (state.getValue(VendingMachineBlock.FACING)) {
                        case NORTH -> rotY(a, 90);
                        case WEST -> rotY(a, 180);
                        case SOUTH -> rotY(a, 270);
                        default -> { }
                    }
                }), "body", be -> {
                    if (!(be.getBlockState().getBlock() instanceof VendingMachineBlock block) || be.getLevel() == null) return null;
                    if (be.getLevel().getBlockState(be.getBlockPos().above()).isAir()) return null;
                    return block.snacks ? VENDING_SNACKS : VENDING_SODA;
                })
                .viewDistance(64).register();

        // RenderArcWelder / RenderSolderingStation: Anzeigegegenstand immediate
        mesh(MachineRenderers.machine("hbm_m/arc_welder", ModBlockEntities.ARC_WELDER_BE.get(), MachineArcWelderBlockEntity.class)
                .blockTransform((be, a) -> {
                    workbenchOrient(a, be);
                    a.translate(-0.5, 0, 0);
                }), "body", ARC_WELDER)
                .hook((be, f, ps, buf, light, overlay, api) -> WorkbenchMachineRenderers.renderDisplay(ps, buf, be.display, light, be))
                .viewDistance(64).register();
        mesh(MachineRenderers.machine("hbm_m/soldering_station", ModBlockEntities.SOLDERING_STATION_BE.get(), MachineSolderingStationBlockEntity.class)
                .blockTransform((be, a) -> {
                    workbenchOrient(a, be);
                    a.translate(-0.5, 0, 0.5);
                }), "body", SOLDERING)
                .hook((be, f, ps, buf, light, overlay, api) -> WorkbenchMachineRenderers.renderDisplay(ps, buf, be.display, light, be))
                .viewDistance(64).register();

        // RenderFurnaceSteel: Metadaten 3/5/2/4 -> 0/90/180/270, danach -90; Glut immediate
        mesh(MachineRenderers.machine("hbm_m/furnace_steel", ModBlockEntities.FURNACE_STEEL_BE.get(), MachineFurnaceSteelBlockEntity.class)
                .blockTransform((be, a) -> {
                    a.translate(0.5, 0, 0.5);
                    switch (be.getBlockState().getValue(DummyableMachineBlock.FACING)) {
                        case EAST -> rotY(a, 90);
                        case NORTH -> rotY(a, 180);
                        case WEST -> rotY(a, 270);
                        default -> { }
                    }
                    rotY(a, -90);
                }), "body", FURNACE_STEEL)
                .hook((be, f, ps, buf, light, overlay, api) -> {
                    if (be.wasOn) furnaceSteelGlow(ps, buf);
                })
                .viewDistance(256).register();

        // RenderTapeDrive: Laufwerke je belegtem Schacht, Statuslampen immediate
        MachineSpecBuilder<MachineTapeDriveBlockEntity> tape = MachineRenderers.machine("hbm_m/tape_drive", ModBlockEntities.TAPE_DRIVE_BE.get(), MachineTapeDriveBlockEntity.class)
                .blockTransform((be, a) -> {
                    a.translate(0.5, 0, 0.5);
                    switch (be.getBlockState().getValue(MachineTapeDriveBlock.FACING)) {
                        case SOUTH -> rotY(a, 270);
                        case NORTH -> rotY(a, 90);
                        case WEST -> rotY(a, 180);
                        default -> { }
                    }
                });
        mesh(tape, "frame", TAPE_FRAME);
        for (int i = 0; i < TAPE_DRIVES.length; i++) {
            final int slot = i;
            when(tape, "drive" + i, be -> be.getTapeState(slot) != MachineTapeDriveBlockEntity.SLOT_EMPTY, TAPE_DRIVES[i]);
        }
        tape.hook((be, f, ps, buf, light, overlay, api) -> tapeLights(be, ps, buf))
                .viewDistance(64).register();
    }

    // ── Hilfen ───────────────────────────────────────────────────────────────────────────

    private static void decoTransform(DecoBlockEntity tile, com.hbm_m.client.render.LegacyAnimator a) {
        BlockState state = tile.getBlockState();
        if (!(state.getBlock() instanceof DecoModelTEBlock block)) return;
        a.translate(0.5D, 1.5D, 0.5D);
        a.rotate(180, 0, 0, 1);
        if (block.kind == DecoModelTEBlock.Kind.BOXCAR) {
            a.rotate(180, 0, 0, 1);
            a.translate(0, -1.5F, 0);
            switch (state.getValue(DecoModelTEBlock.FACING)) {
                case WEST -> { }
                case NORTH -> rotY(a, 270);
                case EAST -> rotY(a, 180);
                case SOUTH -> rotY(a, 90);
                default -> {
                    a.rotate(180, 0, 0, 1);
                    a.rotate(90, 1, 0, 0);
                    a.translate(0, -1.5F, 0);
                }
            }
        } else {
            a.rotate(180, 0, 0, 1);
            a.translate(0, 0, -1.5F);
            a.translate(0, 0.5F, 0);
        }
    }

    private static boolean rtgConnects(MachineRTGBlockEntity te, java.util.function.Function<BlockPos, BlockPos> step, Direction side) {
        Level level = te.getLevel();
        if (level == null) return false;
        BlockEntity be = level.getBlockEntity(step.apply(te.getBlockPos()));
        return be instanceof IEnergyConnector c && c.canConnectEnergy(side);
    }

    private static int railMeta(RailSwitchBlockEntity sw) {
        BlockState state = sw.getBlockState();
        if (!state.hasProperty(RailDummyableBlock.META)) return -1;
        return state.getValue(RailDummyableBlock.META);
    }

    private static void workbenchOrient(com.hbm_m.client.render.LegacyAnimator a, BlockEntity be) {
        a.translate(0.5, 0, 0.5);
        switch (ObjBerHelper.meta(be)) {
            case 2 -> rotY(a, 90);
            case 4 -> rotY(a, 180);
            case 3 -> rotY(a, 270);
            default -> { }
        }
    }

    /** Lichtkegel aus {@code DemonLampRenderer} (unveraendert). */
    private static void demonLampGlow(PoseStack ps, MultiBufferSource buf) {
        VertexConsumer vc = buf.getBuffer(ClientRenderHandler.CustomRenderTypes.DETONATOR_LASER_GLOW);
        Matrix4f m = ps.last().pose();
        double near = 0.375D, far = 15D;
        for (int j = 0; j < 2; j++) {
            double h = 0.5;
            double height = j == 0 ? -h : h;
            double vx = 1, vz = 0;
            for (int i = 0; i < 16; i++) {
                double y0 = 0.5D + j * 0.125D, y1 = y0 + height;
                float x0 = (float) (vx * near), z0 = (float) (vz * near), x1 = (float) (vx * far), z1 = (float) (vz * far);
                double an = Math.PI * 2D / 16D, c = Math.cos(an), s = Math.sin(an);
                double nvx = vx * c + vz * s, nvz = vz * c - vx * s;
                //? if < 1.21.1 {
                vc.vertex(m, x0, (float) y0, z0).color(0F, 0.75F, 1F, 0.25F).endVertex();
                vc.vertex(m, x1, (float) y1, z1).color(0F, 0.75F, 1F, 0F).endVertex();
                vc.vertex(m, (float) (nvx * far), (float) y1, (float) (nvz * far)).color(0F, 0.75F, 1F, 0F).endVertex();
                vc.vertex(m, (float) (nvx * near), (float) y0, (float) (nvz * near)).color(0F, 0.75F, 1F, 0.25F).endVertex();
                //?} else {
                /*vc.addVertex(m, x0, (float) y0, z0).setColor(0F, 0.75F, 1F, 0.25F);
                vc.addVertex(m, x1, (float) y1, z1).setColor(0F, 0.75F, 1F, 0F);
                vc.addVertex(m, (float) (nvx * far), (float) y1, (float) (nvz * far)).setColor(0F, 0.75F, 1F, 0F);
                vc.addVertex(m, (float) (nvx * near), (float) y0, (float) (nvz * near)).setColor(0F, 0.75F, 1F, 0.25F);
                *///?}
                vx = nvx;
                vz = nvz;
            }
        }
    }

    /** Glut aus {@code FurnaceSteelRenderer} (unveraendert). */
    private static void furnaceSteelGlow(PoseStack ps, MultiBufferSource buf) {
        float col = (float) Math.sin(System.currentTimeMillis() * 0.001);
        float r = 0.875F + col * 0.125F, g = 0.625F + col * 0.375F, b = 0F, al = 0.5F;
        VertexConsumer vc = buf.getBuffer(ClientRenderHandler.CustomRenderTypes.DETONATOR_LASER_GLOW);
        Matrix4f m = ps.last().pose();
        for (int i = 0; i < 4; i++) {
            float x = 1 + i * 0.0625F;
            //? if < 1.21.1 {
            vc.vertex(m, x, 1, -1).color(r, g, b, al).endVertex();
            vc.vertex(m, x, 1, 1).color(r, g, b, al).endVertex();
            vc.vertex(m, x, 0.5F, 1).color(r, g, b, al).endVertex();
            vc.vertex(m, x, 0.5F, -1).color(r, g, b, al).endVertex();
            //?} else {
            /*vc.addVertex(m, x, 1, -1).setColor(r, g, b, al);
            vc.addVertex(m, x, 1, 1).setColor(r, g, b, al);
            vc.addVertex(m, x, 0.5F, 1).setColor(r, g, b, al);
            vc.addVertex(m, x, 0.5F, -1).setColor(r, g, b, al);
            *///?}
        }
    }

    /** Statuslampen aus {@code TapeDriveRenderer} (unveraendert). */
    private static void tapeLights(MachineTapeDriveBlockEntity drive, PoseStack ps, MultiBufferSource buf) {
        VertexConsumer lights = buf.getBuffer(ClientRenderHandler.CustomRenderTypes.SOLID_COLOR_NOCULL);
        for (int i = 0; i < MachineTapeDriveBlockEntity.SLOT_COUNT; i++) {
            byte tape = drive.getTapeState(i);
            if (tape == MachineTapeDriveBlockEntity.SLOT_EMPTY) continue;
            float r = 1F, g = 0F;
            if (tape == MachineTapeDriveBlockEntity.SLOT_EMPTY_TAPE) g = 0.75F;
            if (tape == MachineTapeDriveBlockEntity.SLOT_FILLED_TAPE) { r = 0F; g = 1F; }
            ps.pushPose();
            ps.translate(0, 0.25 - 0.5 * (i / 6), 0.3125 - (i % 6) * 0.125);
            TapeDriveRenderer.MODEL.renderPartColor("Light", ps, lights, r, g, 0F, 1F);
            ps.popPose();
        }
    }
}
