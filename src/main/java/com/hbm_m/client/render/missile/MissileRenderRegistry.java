package com.hbm_m.client.render.missile;

import java.util.HashMap;
import java.util.Map;

import com.hbm_m.client.render.item.ItemRenderMissileGeneric.RenderMissileType;
import com.hbm_m.item.ModItems;

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Per-item missile render data; {@link RenderMissileType} matches 1.7.10 {@code ClientProxy} item renderer registration.
 */
public final class MissileRenderRegistry {

    private static final Map<Item, MissileRenderData> BY_ITEM = new HashMap<>();

    static {
        register(ModItems.MISSILE_TEST, MissileTextures.MISSILE_MICRO, RenderMissileType.TYPE_TIER0);
        register(ModItems.MISSILE_TAINT, MissileTextures.MISSILE_MICRO_TAINT, RenderMissileType.TYPE_TIER0);
        register(ModItems.MISSILE_MICRO, MissileTextures.MISSILE_MICRO, RenderMissileType.TYPE_TIER0);
        register(ModItems.MISSILE_BHOLE, MissileTextures.MISSILE_MICRO_BHOLE, RenderMissileType.TYPE_TIER0);
        register(ModItems.MISSILE_SCHRABIDIUM, MissileTextures.MISSILE_MICRO_SCHRAB, RenderMissileType.TYPE_TIER0);
        register(ModItems.MISSILE_EMP, MissileTextures.MISSILE_MICRO_EMP, RenderMissileType.TYPE_TIER0);

        register(ModItems.MISSILE_GENERIC, MissileTextures.MISSILE_V2, RenderMissileType.TYPE_TIER1);
        register(ModItems.MISSILE_INCENDIARY, MissileTextures.MISSILE_V2_INC, RenderMissileType.TYPE_TIER1);
        register(ModItems.MISSILE_CLUSTER, MissileTextures.MISSILE_V2_CL, RenderMissileType.TYPE_TIER1);
        register(ModItems.MISSILE_BUSTER, MissileTextures.MISSILE_V2_BU, RenderMissileType.TYPE_TIER1);
        register(ModItems.MISSILE_DECOY, MissileTextures.MISSILE_V2_DECOY, RenderMissileType.TYPE_TIER1);
        BY_ITEM.put(ModItems.MISSILE_STEALTH.get(), MissileRenderData.stealth(itemId(ModItems.MISSILE_STEALTH)));

        register(ModItems.MISSILE_ABM, MissileTextures.MISSILE_ABM, RenderMissileType.TYPE_ABM);

        registerLarge(ModItems.MISSILE_STRONG, MissileTextures.MISSILE_STRONG, RenderMissileType.TYPE_TIER2);
        registerLarge(ModItems.MISSILE_INCENDIARY_STRONG, MissileTextures.MISSILE_STRONG_INC, RenderMissileType.TYPE_TIER2);
        registerLarge(ModItems.MISSILE_CLUSTER_STRONG, MissileTextures.MISSILE_STRONG_CL, RenderMissileType.TYPE_TIER2);
        registerLarge(ModItems.MISSILE_BUSTER_STRONG, MissileTextures.MISSILE_STRONG_BU, RenderMissileType.TYPE_TIER2);
        registerLarge(ModItems.MISSILE_EMP_STRONG, MissileTextures.MISSILE_STRONG_EMP, RenderMissileType.TYPE_TIER2);

        register(ModItems.MISSILE_BURST, MissileTextures.MISSILE_HUGE, RenderMissileType.TYPE_TIER3);
        register(ModItems.MISSILE_INFERNO, MissileTextures.MISSILE_HUGE_INC, RenderMissileType.TYPE_TIER3);
        register(ModItems.MISSILE_RAIN, MissileTextures.MISSILE_HUGE_CL, RenderMissileType.TYPE_TIER3);
        register(ModItems.MISSILE_DRILL, MissileTextures.MISSILE_HUGE_BU, RenderMissileType.TYPE_TIER3);

        register(ModItems.MISSILE_NUCLEAR, MissileTextures.MISSILE_ATLAS_NUCLEAR, RenderMissileType.TYPE_NUCLEAR);
        register(ModItems.MISSILE_NUCLEAR_CLUSTER, MissileTextures.MISSILE_ATLAS_THERMO, RenderMissileType.TYPE_NUCLEAR);
        register(ModItems.MISSILE_VOLCANO, MissileTextures.MISSILE_ATLAS_TECTONIC, RenderMissileType.TYPE_NUCLEAR);
        register(ModItems.MISSILE_DOOMSDAY, MissileTextures.MISSILE_ATLAS_DOOMSDAY, RenderMissileType.TYPE_NUCLEAR);
        register(ModItems.MISSILE_DOOMSDAY_RUSTED, MissileTextures.MISSILE_ATLAS_DOOMSDAY_WEATHERED, RenderMissileType.TYPE_NUCLEAR);

        register(ModItems.MISSILE_SHUTTLE, MissileTextures.MISSILE_SHUTTLE, RenderMissileType.TYPE_ROBIN);
    }

    private MissileRenderRegistry() {
    }

    private static void register(RegistrySupplier<Item> item, ResourceLocation texture, RenderMissileType type) {
        BY_ITEM.put(item.get(), MissileRenderData.standard(itemId(item), texture, type));
    }

    private static void registerLarge(RegistrySupplier<Item> item, ResourceLocation texture, RenderMissileType type) {
        BY_ITEM.put(item.get(), MissileRenderData.large(itemId(item), texture, type));
    }

    private static ResourceLocation itemId(RegistrySupplier<Item> item) {
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
