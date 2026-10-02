package com.hbm_m.item.hazmat.render;

import java.util.Set;

import com.hbm_m.client.loader.AbstractObjPartModelLoader;

import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ArmorItem;

import java.util.HashMap;
import org.jetbrains.annotations.Nullable;
//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//?} elif neoforge {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
*///?}

/**
 * Загрузчик OBJ-костюма химзащиты (все три окраса шарят одну геометрию,
 * текстура выбирается в item-модели через "default" и в слое - через PartMaterials).
 * Имена частей совпадают с "o <name>" в hazmat.obj.
 */
@OnlyIn(Dist.CLIENT)
public class HazmatArmorModelLoader extends AbstractObjPartModelLoader<HazmatArmorBakedModel> {

    private static final Set<String> PART_NAMES = Set.of(
            "Helmet",
            "Filter",
            "Chest",
            "LeftArm",
            "RightArm",
            "LeftLeg",
            "RightLeg",
            "LeftBoot",
            "RightBoot"
    );

    @Override
    protected Set<String> getPartNames(com.google.gson.JsonObject jsonObject) {
        return PART_NAMES;
    }

    @Override
    protected HazmatArmorBakedModel createBakedModel(HashMap<String, BakedModel> bakedParts,
                                                     ItemTransforms transforms,
                                                     ResourceLocation modelLocation) {
        return new HazmatArmorBakedModel(bakedParts, transforms, resolveHazmatType(modelLocation));
    }

    @Override
    protected boolean flipV() {
        return true;
    }

    /**
     * Предметные модели хазмата используют и современные суффиксы (hazmat_chestplate),
     * и оригинальные 1.7.10 (hazmat_plate_red / hazmat_legs_red) - поэтому contains
     * вместо endsWith, более длинные суффиксы проверяются первыми.
     */
    @Nullable
    public static ArmorItem.Type resolveHazmatType(@Nullable ResourceLocation modelName) {
        if (modelName == null) {
            return null;
        }
        String path = modelName.getPath();
        if (path.contains("_chestplate")) return ArmorItem.Type.CHESTPLATE;
        if (path.contains("_leggings")) return ArmorItem.Type.LEGGINGS;
        if (path.contains("_helmet")) return ArmorItem.Type.HELMET;
        if (path.contains("_plate")) return ArmorItem.Type.CHESTPLATE;
        if (path.contains("_legs")) return ArmorItem.Type.LEGGINGS;
        if (path.contains("_boots")) return ArmorItem.Type.BOOTS;
        // Общая модель сета (hazmat_armor) - тип неизвестен, слой рисует части по слоту.
        return null;
    }
}
