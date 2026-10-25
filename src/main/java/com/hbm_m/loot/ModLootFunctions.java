package com.hbm_m.loot;

import com.hbm_m.lib.RefStrings;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;

/** Eigene Loot-Funktionen. */
public final class ModLootFunctions {

    private ModLootFunctions() { }

    //? if < 1.21.1 {
    public static final DeferredRegister<LootItemFunctionType> FUNCTIONS = DeferredRegister.create(RefStrings.MODID, Registries.LOOT_FUNCTION_TYPE);

    public static final RegistrySupplier<LootItemFunctionType> PERSISTENT_NBT =
            FUNCTIONS.register("persistent_nbt", () -> new LootItemFunctionType(new PersistentNbtFunction.Serializer()));
    //?} else {
    /*public static final DeferredRegister<LootItemFunctionType<?>> FUNCTIONS = DeferredRegister.create(RefStrings.MODID, Registries.LOOT_FUNCTION_TYPE);

    public static final RegistrySupplier<LootItemFunctionType<PersistentNbtFunction>> PERSISTENT_NBT =
            FUNCTIONS.register("persistent_nbt", () -> new LootItemFunctionType<>(PersistentNbtFunction.CODEC));
    *///?}

    public static void init() {
        FUNCTIONS.register();
    }
}
