package com.hbm_m.client.render.missile;

import java.util.HashMap;
import java.util.Map;

import com.hbm_m.item.ModItems;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class MissileRenderRegistry {

    private static final Map<Item, MissileRenderData> BY_ITEM = new HashMap<>();

    static {
        register(ModItems.MISSILE_TEST, MissileTextures.MISSILE_MICRO);
        register(ModItems.MISSILE_ABM, MissileTextures.MISSILE_ABM);
        register(ModItems.MISSILE_MICRO, MissileTextures.MISSILE_MICRO);
        register(ModItems.MISSILE_SCHRABIDIUM, MissileTextures.MISSILE_MICRO_SCHRAB);
        register(ModItems.MISSILE_BHOLE, MissileTextures.MISSILE_MICRO_BHOLE);
        register(ModItems.MISSILE_TAINT, MissileTextures.MISSILE_MICRO_TAINT);
        register(ModItems.MISSILE_EMP, MissileTextures.MISSILE_MICRO_EMP);

        register(ModItems.MISSILE_GENERIC, MissileTextures.MISSILE_V2);
        register(ModItems.MISSILE_INCENDIARY, MissileTextures.MISSILE_V2_INC);
        register(ModItems.MISSILE_CLUSTER, MissileTextures.MISSILE_V2_CL);
        register(ModItems.MISSILE_BUSTER, MissileTextures.MISSILE_V2_BU);
        register(ModItems.MISSILE_DECOY, MissileTextures.MISSILE_V2_DECOY);
        BY_ITEM.put(ModItems.MISSILE_STEALTH.get(), MissileRenderData.stealth(itemId(ModItems.MISSILE_STEALTH)));

        registerLarge(ModItems.MISSILE_STRONG, MissileTextures.MISSILE_STRONG);
        registerLarge(ModItems.MISSILE_INCENDIARY_STRONG, MissileTextures.MISSILE_STRONG_INC);
        registerLarge(ModItems.MISSILE_CLUSTER_STRONG, MissileTextures.MISSILE_STRONG_CL);
        registerLarge(ModItems.MISSILE_BUSTER_STRONG, MissileTextures.MISSILE_STRONG_BU);
        registerLarge(ModItems.MISSILE_EMP_STRONG, MissileTextures.MISSILE_STRONG_EMP);

        register(ModItems.MISSILE_BURST, MissileTextures.MISSILE_HUGE);
        register(ModItems.MISSILE_INFERNO, MissileTextures.MISSILE_HUGE_INC);
        register(ModItems.MISSILE_RAIN, MissileTextures.MISSILE_HUGE_CL);
        register(ModItems.MISSILE_DRILL, MissileTextures.MISSILE_HUGE_BU);
        register(ModItems.MISSILE_SHUTTLE, MissileTextures.MISSILE_SHUTTLE);

        register(ModItems.MISSILE_NUCLEAR, MissileTextures.MISSILE_ATLAS_NUCLEAR);
        register(ModItems.MISSILE_NUCLEAR_CLUSTER, MissileTextures.MISSILE_ATLAS_THERMO);
        register(ModItems.MISSILE_VOLCANO, MissileTextures.MISSILE_ATLAS_TECTONIC);
        register(ModItems.MISSILE_DOOMSDAY, MissileTextures.MISSILE_ATLAS_DOOMSDAY);
        register(ModItems.MISSILE_DOOMSDAY_RUSTED, MissileTextures.MISSILE_ATLAS_DOOMSDAY_WEATHERED);
    }

    private MissileRenderRegistry() {
    }

    private static void register(dev.architectury.registry.registries.RegistrySupplier<Item> item, ResourceLocation texture) {
        BY_ITEM.put(item.get(), MissileRenderData.standard(itemId(item), texture));
    }

    private static void registerLarge(dev.architectury.registry.registries.RegistrySupplier<Item> item, ResourceLocation texture) {
        BY_ITEM.put(item.get(), MissileRenderData.large(itemId(item), texture));
    }

    private static ResourceLocation itemId(dev.architectury.registry.registries.RegistrySupplier<Item> item) {
        return BuiltInRegistries.ITEM.getKey(item.get());
    }

    @Nullable
    public static MissileRenderData get(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        return get(stack.getItem());
    }

    @Nullable
    public static MissileRenderData get(Item item) {
        return BY_ITEM.get(item);
    }
}
