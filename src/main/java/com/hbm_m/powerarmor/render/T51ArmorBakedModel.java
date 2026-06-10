//? if forge {
package com.hbm_m.powerarmor.render;


import java.util.Map;

import com.hbm_m.interfaces.IArmorModelConfig;
import com.hbm_m.powerarmor.ModPowerArmorItem;
import com.hbm_m.powerarmor.T51Armor;

import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ArmorItem;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//? if fabric {
/*import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;*///?}

/**
 * Модель для рендеринга T51 Power Armor в GUI и руке.
 * Использует абстрактный базовый класс для общей логики рендеринга.
 */
//? if forge {
@OnlyIn(Dist.CLIENT)
//?}
//? if fabric {
/*@Environment(EnvType.CLIENT)*///?}
public class T51ArmorBakedModel extends AbstractArmorBakedModel {

    private static final String[] T51_ORDER = {
            "Helmet", "Chest", "RightArm", "LeftArm", "RightLeg", "LeftLeg", "RightBoot", "LeftBoot"
    };

    private static final T51ModelConfig CONFIG = new T51ModelConfig();

    public T51ArmorBakedModel(Map<String, BakedModel> parts, ItemTransforms transforms, @org.jetbrains.annotations.Nullable ArmorItem.Type itemArmorType) {
        super(parts, transforms, CONFIG, itemArmorType);
    }

    @Override
    public T51ArmorBakedModel withTransforms(ItemTransforms newTransforms) {
        return new T51ArmorBakedModel(this.parts, newTransforms, this.itemArmorType);
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

            if (armorType == ArmorItem.Type.HELMET) {
                return new String[]{"Helmet"};
            }
            if (armorType == ArmorItem.Type.CHESTPLATE) {
                return new String[]{"Chest", "RightArm", "LeftArm"};
            }
            if (armorType == ArmorItem.Type.LEGGINGS) {
                return new String[]{"RightLeg", "LeftLeg"};
            }
            if (armorType == ArmorItem.Type.BOOTS) {
                return new String[]{"RightBoot", "LeftBoot"};
            }
            return T51_ORDER;
        }

        @Override
        public Class<? extends ModPowerArmorItem> getArmorItemClass() {
            return T51Armor.class;
        }

        @Override
        public ModelResourceLocation getBaseModelLocation() {
            return ClientPowerArmorRender.T51_MODEL_BAKED;
        }

        @Override
        public boolean isItemValid(net.minecraft.world.item.ItemStack stack) {
            return stack.getItem() instanceof T51Armor;
        }
    }
}
//?}
