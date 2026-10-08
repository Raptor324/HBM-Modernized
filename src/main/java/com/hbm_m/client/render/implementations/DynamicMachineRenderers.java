package com.hbm_m.client.render.implementations;

import com.hbm_m.blockentity.ModBlockEntities;

import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;

/**
 * Audit 6 (Animations-/Dynamik-Abgleich): BERs fuer Maschinen, deren Original-TESR bewegte Teile, Fuellstaende,
 * Strahlen oder Gegenstaende zeichnet, die im Port bisher nur ein statisches Blockmodell hatten.
 */
//? if forge {
@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
//?} elif fabric {
/*@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
*///?} elif neoforge {
/*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
*///?}
public final class DynamicMachineRenderers {

    private DynamicMachineRenderers() {}

    public static void register() {
        BlockEntityRenderers.register(ModBlockEntities.STIRLING_BE.get(), StirlingRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.PILE_CONTROL_BE.get(), PileDeviceRenderers.Control::new);
        BlockEntityRenderers.register(ModBlockEntities.PILE_LOADER_BE.get(), PileDeviceRenderers.Loader::new);
        BlockEntityRenderers.register(ModBlockEntities.PILE_VENT_BE.get(), PileDeviceRenderers.Vent::new);
        BlockEntityRenderers.register(ModBlockEntities.CHARGER_BE.get(), ChargerRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.ASHPIT_BE.get(), AshpitRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.MICROWAVE_BE.get(), MicrowaveRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.DIESEL_GENERATOR_BE.get(), DieselGenRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.PUMPJACK_BE.get(), PumpjackRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.FURNACE_IRON_BE.get(), FurnaceIronRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.BREEDER_BE.get(), BreederRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.CONVEYOR_PRESS_BE.get(), ConveyorPressRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.EPRESS_BE.get(), EPressRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.ARC_WELDER_BE.get(), WorkbenchMachineRenderers.ArcWelder::new);
        BlockEntityRenderers.register(ModBlockEntities.SOLDERING_STATION_BE.get(), WorkbenchMachineRenderers.SolderingStation::new);
        BlockEntityRenderers.register(ModBlockEntities.PA_BEAMLINE_BE.get(), PABeamlineRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.BIGASSTANK_BE.get(), TankRenderers.BigAssTank::new);
        BlockEntityRenderers.register(ModBlockEntities.ORBUS_BE.get(), TankRenderers.Orbus::new);
        BlockEntityRenderers.register(ModBlockEntities.BARREL_IRON_BE.get(), TankRenderers.Barrel::new);
        BlockEntityRenderers.register(ModBlockEntities.BARREL_STEEL_BE.get(), TankRenderers.Barrel::new);
        BlockEntityRenderers.register(ModBlockEntities.BARREL_TCALLOY_BE.get(), TankRenderers.Barrel::new);
        BlockEntityRenderers.register(ModBlockEntities.BARREL_CORRODED_BE.get(), TankRenderers.Barrel::new);
        BlockEntityRenderers.register(ModBlockEntities.BARREL_PLASTIC_BE.get(), TankRenderers.Barrel::new);
        BlockEntityRenderers.register(ModBlockEntities.BARREL_ANTIMATTER_BE.get(), TankRenderers.Barrel::new);
        BlockEntityRenderers.register(ModBlockEntities.TESLA_BE.get(), TeslaRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.ICF_CONTROLLER_BE.get(), ICFControllerRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.CORE_EMITTER_BE.get(), DFCRenderers.Emitter::new);
        BlockEntityRenderers.register(ModBlockEntities.CORE_INJECTOR_BE.get(), DFCRenderers.Injector::new);
        BlockEntityRenderers.register(ModBlockEntities.CORE_RECEIVER_BE.get(), DFCRenderers.Receiver::new);
        BlockEntityRenderers.register(ModBlockEntities.DFC_STABILIZER_BE.get(), DFCRenderers.Stabilizer::new);
        BlockEntityRenderers.register(ModBlockEntities.DFC_CORE_BE.get(), DFCRenderers.Core::new);
        BlockEntityRenderers.register(ModBlockEntities.CYCLOTRON_BE.get(), CyclotronRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.CATALYTIC_REFORMER_BE.get(), CatalyticReformerRenderer::new);
        // Audit 7: RenderSubstation = RenderPylonBase.renderLinesGeneric (Kabel), Modell bleibt Blockmodell
        net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider<com.hbm_m.blockentity.network.PylonBaseBlockEntity> wires = RedPylonWireRenderer::new;
        BlockEntityRenderers.register(ModBlockEntities.SUBSTATION_BE.get(), wires);
        // Audit 7: RenderLaunchPadLarge (grosse Startrampe)
        BlockEntityRenderers.register(ModBlockEntities.LAUNCH_PAD_LARGE_BE.get(), LaunchPadLargeRenderer::new);
        // Audit 7: RenderRTG (Anschlussstutzen)
        BlockEntityRenderers.register(ModBlockEntities.MACHINE_RTG_BE.get(), RTGRenderer::new);
        // Audit 7: RenderMassStorage (Frontanzeige)
        BlockEntityRenderers.register(ModBlockEntities.MACHINE_MASS_STORAGE_BE.get(), MassStorageRenderer::new);
    }
}
