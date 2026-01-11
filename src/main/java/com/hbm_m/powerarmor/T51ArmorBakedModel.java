package com.hbm_m.powerarmor;

import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.item.ArmorItem;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Map;

/**
 * Модель для рендеринга T51 Power Armor в GUI и руке.
 * Использует абстрактный базовый класс для общей логики рендеринга.
 */
@OnlyIn(Dist.CLIENT)
public class T51ArmorBakedModel extends AbstractArmorBakedModel {

    private static final String[] T51_ORDER = {
            "Helmet", "Chest", "RightArm", "LeftArm", "RightLeg", "LeftLeg", "RightBoot", "LeftBoot"
    };

    private static final T51ModelConfig CONFIG = new T51ModelConfig();

    public T51ArmorBakedModel(Map<String, BakedModel> parts, ItemTransforms transforms) {
        super(parts, transforms, CONFIG);
    }

    @Override
    public T51ArmorBakedModel withTransforms(ItemTransforms newTransforms) {
        return new T51ArmorBakedModel(this.parts, newTransforms);
    }

    /**
     * Конфигурация для T51 Power Armor.
     */
    private static class T51ModelConfig implements IArmorModelConfig {
        @Override
        public String getArmorSetId() {
            return "t51";
        }

        @Override
        public String[] getPartOrder() {
            return T51_ORDER;
        }

        @Override
        public String[] getPartsForType(ArmorItem.Type armorType) {
            if (armorType == null) {
                // Если тип не указан, рендерим все части (fallback)
                return T51_ORDER;
            }

            return switch (armorType) {
                case HELMET -> new String[]{"Helmet"};
                case CHESTPLATE -> new String[]{"Chest", "RightArm", "LeftArm"};
                case LEGGINGS -> new String[]{"RightLeg", "LeftLeg"};
                case BOOTS -> new String[]{"RightBoot", "LeftBoot"};
            };
        }

        @Override
        public Class<? extends ModPowerArmorItem> getArmorItemClass() {
            return ModPowerArmorItem.class;
        }

        @Override
        public ModelResourceLocation getBaseModelLocation() {
            return ClientPowerArmorRender.T51_MODEL_BAKED;
        }
    }
}
