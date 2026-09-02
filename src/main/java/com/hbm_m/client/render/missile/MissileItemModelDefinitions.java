package com.hbm_m.client.render.missile;

import java.util.List;



import com.hbm_m.item.ModItems;
import com.hbm_m.item.missile.MissileItem;
import com.hbm_m.lib.RefStrings;

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

/**
 * Single source of truth for generated {@code models/item/missile_*.json} files.
 */
public final class MissileItemModelDefinitions {

    public record Definition(
            String itemPath,
            MissileFormFactorModels hull,
            ResourceLocation texture,
            MissileItem.MissileFormFactor formFactor
    ) {
    }

    private static final String LOADER = RefStrings.MODID + ":missile_loader";

    private MissileItemModelDefinitions() {
    }

    public static List<Definition> all() {
        return List.of(
                def(ModItems.MISSILE_TEST, MissileFormFactorModels.MICRO, MissileTextures.MISSILE_MICRO),
                def(ModItems.MISSILE_ABM, MissileFormFactorModels.ABM, MissileTextures.MISSILE_ABM),
                def(ModItems.MISSILE_MICRO, MissileFormFactorModels.MICRO, MissileTextures.MISSILE_MICRO),
                def(ModItems.MISSILE_SCHRABIDIUM, MissileFormFactorModels.MICRO, MissileTextures.MISSILE_MICRO_SCHRAB),
                def(ModItems.MISSILE_BHOLE, MissileFormFactorModels.MICRO, MissileTextures.MISSILE_MICRO_BHOLE),
                def(ModItems.MISSILE_TAINT, MissileFormFactorModels.MICRO, MissileTextures.MISSILE_MICRO_TAINT),
                def(ModItems.MISSILE_EMP, MissileFormFactorModels.MICRO, MissileTextures.MISSILE_MICRO_EMP),

                def(ModItems.MISSILE_GENERIC, MissileFormFactorModels.V2, MissileTextures.MISSILE_V2),
                def(ModItems.MISSILE_INCENDIARY, MissileFormFactorModels.V2, MissileTextures.MISSILE_V2_INC),
                def(ModItems.MISSILE_CLUSTER, MissileFormFactorModels.V2, MissileTextures.MISSILE_V2_CL),
                def(ModItems.MISSILE_BUSTER, MissileFormFactorModels.V2, MissileTextures.MISSILE_V2_BU),
                def(ModItems.MISSILE_DECOY, MissileFormFactorModels.V2, MissileTextures.MISSILE_V2_DECOY),
                def(ModItems.MISSILE_STEALTH, MissileFormFactorModels.STEALTH, MissileTextures.MISSILE_STEALTH),

                def(ModItems.MISSILE_STRONG, MissileFormFactorModels.STRONG, MissileTextures.MISSILE_STRONG),
                def(ModItems.MISSILE_INCENDIARY_STRONG, MissileFormFactorModels.STRONG, MissileTextures.MISSILE_STRONG_INC),
                def(ModItems.MISSILE_CLUSTER_STRONG, MissileFormFactorModels.STRONG, MissileTextures.MISSILE_STRONG_CL),
                def(ModItems.MISSILE_BUSTER_STRONG, MissileFormFactorModels.STRONG, MissileTextures.MISSILE_STRONG_BU),
                def(ModItems.MISSILE_EMP_STRONG, MissileFormFactorModels.STRONG, MissileTextures.MISSILE_STRONG_EMP),

                def(ModItems.MISSILE_BURST, MissileFormFactorModels.HUGE, MissileTextures.MISSILE_HUGE),
                def(ModItems.MISSILE_INFERNO, MissileFormFactorModels.HUGE, MissileTextures.MISSILE_HUGE_INC),
                def(ModItems.MISSILE_RAIN, MissileFormFactorModels.HUGE, MissileTextures.MISSILE_HUGE_CL),
                def(ModItems.MISSILE_DRILL, MissileFormFactorModels.HUGE, MissileTextures.MISSILE_HUGE_BU),
                def(ModItems.MISSILE_SHUTTLE, MissileFormFactorModels.SHUTTLE, MissileTextures.MISSILE_SHUTTLE),

                def(ModItems.MISSILE_NUCLEAR, MissileFormFactorModels.ATLAS, MissileTextures.MISSILE_ATLAS_NUCLEAR),
                def(ModItems.MISSILE_NUCLEAR_CLUSTER, MissileFormFactorModels.ATLAS, MissileTextures.MISSILE_ATLAS_THERMO),
                def(ModItems.MISSILE_VOLCANO, MissileFormFactorModels.ATLAS, MissileTextures.MISSILE_ATLAS_TECTONIC),
                def(ModItems.MISSILE_DOOMSDAY, MissileFormFactorModels.DOOMSDAY, MissileTextures.MISSILE_ATLAS_DOOMSDAY),
                def(ModItems.MISSILE_DOOMSDAY_RUSTED, MissileFormFactorModels.DOOMSDAY, MissileTextures.MISSILE_ATLAS_DOOMSDAY_WEATHERED)
        );
    }

    private static Definition def(RegistrySupplier<Item> item, MissileFormFactorModels hull, ResourceLocation texture) {
        MissileItem missile = (MissileItem) item.get();
        return new Definition(item.getId().getPath(), hull, texture, missile.formFactor);
    }

    public static String loaderId() {
        return LOADER;
    }
}
