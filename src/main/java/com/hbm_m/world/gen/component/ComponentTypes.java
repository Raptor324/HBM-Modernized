package com.hbm_m.world.gen.component;

import com.hbm_m.lib.RefStrings;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;

/**
 * Port: Registrierung der Bauteil-Typen (Original {@code MapGenStructureIO.func_143031_a(..., "NTM...")} in
 * {@code CivilianFeatures}/{@code OfficeFeatures}/{@code BunkerComponents.registerComponents} und {@code HbmWorld}).
 */
public final class ComponentTypes {

    private ComponentTypes() { }

    public static final DeferredRegister<StructurePieceType> PIECES = DeferredRegister.create(RefStrings.MODID, Registries.STRUCTURE_PIECE);

    private static RegistrySupplier<StructurePieceType> reg(String name, StructurePieceType.ContextlessType type) {
        return PIECES.register(name, () -> type);
    }

    // CivilianFeatures
    public static final RegistrySupplier<StructurePieceType> NTMHouse1 = reg("ntmhouse1", CivilianFeatures.NTMHouse1::new);
    public static final RegistrySupplier<StructurePieceType> NTMHouse2 = reg("ntmhouse2", CivilianFeatures.NTMHouse2::new);
    public static final RegistrySupplier<StructurePieceType> NTMLab1 = reg("ntmlab1", CivilianFeatures.NTMLab1::new);
    public static final RegistrySupplier<StructurePieceType> NTMLab2 = reg("ntmlab2", CivilianFeatures.NTMLab2::new);
    public static final RegistrySupplier<StructurePieceType> RuralHouse1 = reg("ntmruralhouse1", CivilianFeatures.RuralHouse1::new);
    // OfficeFeatures
    public static final RegistrySupplier<StructurePieceType> LargeOffice = reg("ntmlargeoffice", OfficeFeatures.LargeOffice::new);
    public static final RegistrySupplier<StructurePieceType> LargeOfficeCorner = reg("ntmlargeofficecorner", OfficeFeatures.LargeOfficeCorner::new);
    // SiloComponent
    public static final RegistrySupplier<StructurePieceType> SiloComponent = reg("ntmsilocomponent", com.hbm_m.world.gen.component.SiloComponent::new);
    // BunkerComponents
    public static final RegistrySupplier<StructurePieceType> StartingHub = reg("ntmbstartinghub", BunkerComponents.StartingHub::new);
    public static final RegistrySupplier<StructurePieceType> Corridor = reg("ntmbcorridor", BunkerComponents.Corridor::new);
    public static final RegistrySupplier<StructurePieceType> BedroomL = reg("ntmbbedrooml", BunkerComponents.BedroomL::new);
    public static final RegistrySupplier<StructurePieceType> FunJunction = reg("ntmbfunjunction", BunkerComponents.FunJunction::new);
    public static final RegistrySupplier<StructurePieceType> BathroomL = reg("ntmbbathrooml", BunkerComponents.BathroomL::new);
    public static final RegistrySupplier<StructurePieceType> Laboratory = reg("ntmblaboratory", BunkerComponents.Laboratory::new);
    public static final RegistrySupplier<StructurePieceType> PowerRoom = reg("ntmbpowerroom", BunkerComponents.PowerRoom::new);

    public static void register() {
        PIECES.register();
    }
}
