package com.hbm_m.item.hazmat.render;

import java.util.Map;

import com.hbm_m.interfaces.IArmorModelConfig;
import com.hbm_m.item.hazmat.HazmatArmorItem;
import com.hbm_m.powerarmor.render.AbstractArmorBakedModel;
import com.hbm_m.powerarmor.render.ClientPowerArmorRender;

import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ArmorItem;
import org.jetbrains.annotations.Nullable;

/**
 * Модель костюма химзащиты для GUI/руки. На теле рисуется слоем
 * ({@link HazmatArmorLayer}); часть "Filter" в GUI не показывается -
 * она видна только на надетом шлеме при установленном фильтре.
 */
public class HazmatArmorBakedModel extends AbstractArmorBakedModel {

    private static final String[] PART_ORDER = {
            "Helmet", "Filter", "Chest", "RightArm", "LeftArm", "RightLeg", "LeftLeg", "RightBoot", "LeftBoot"
    };

    private static final HazmatModelConfig CONFIG = new HazmatModelConfig();

    public HazmatArmorBakedModel(Map<String, BakedModel> parts, ItemTransforms transforms, @Nullable ArmorItem.Type itemArmorType) {
        super(parts, transforms, CONFIG, itemArmorType);
    }

    @Override
    public HazmatArmorBakedModel withTransforms(ItemTransforms newTransforms) {
        return new HazmatArmorBakedModel(this.parts, newTransforms, this.itemArmorType);
    }

    private static class HazmatModelConfig implements IArmorModelConfig {
        @Override
        public String getArmorSetId() {
            return "hazmat";
        }

        @Override
        public String[] getPartOrder() {
            return PART_ORDER;
        }

        @Override
        public String[] getPartsForType(ArmorItem.Type armorType) {
            if (armorType == null) {
                return PART_ORDER;
            }
            return switch (armorType) {
                case HELMET -> new String[]{"Helmet"};
                case CHESTPLATE -> new String[]{"Chest", "RightArm", "LeftArm"};
                case LEGGINGS -> new String[]{"RightLeg", "LeftLeg"};
                case BOOTS -> new String[]{"RightBoot", "LeftBoot"};
                default -> PART_ORDER;
            };
        }

        @Override
        public Class<? extends net.minecraft.world.item.Item> getArmorItemClass() {
            return HazmatArmorItem.class;
        }

        @Override
        public net.minecraft.client.resources.model.ModelResourceLocation getBaseModelLocation() {
            return ClientPowerArmorRender.HAZMAT_MODEL_BAKED;
        }

        @Override
        public boolean isItemValid(net.minecraft.world.item.ItemStack stack) {
            return stack.getItem() instanceof HazmatArmorItem;
        }
    }
}
