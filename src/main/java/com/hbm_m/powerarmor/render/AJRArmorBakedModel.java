//? if forge {
package com.hbm_m.powerarmor.render;


import java.util.Map;

import com.hbm_m.interfaces.IArmorModelConfig;
import com.hbm_m.powerarmor.AJRArmor;
import com.hbm_m.powerarmor.AJROArmor;
import com.hbm_m.powerarmor.ModPowerArmorItem;

import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.item.ArmorItem;

//? if fabric {
/*import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;*///?}

/**
 * Baked model for rendering AJR armor in GUI/hand.
 * Uses the shared multipart baked model infrastructure (same as T51).
 */
//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
@OnlyIn(Dist.CLIENT)
//?}
//? if fabric {
/*@Environment(EnvType.CLIENT)*///?}
public class AJRArmorBakedModel extends AbstractArmorBakedModel {

    private static final String[] AJR_ORDER = {
            "Helmet", "Chest", "RightArm", "LeftArm", "RightLeg", "LeftLeg", "RightBoot", "LeftBoot"
    };

    private static final AJRModelConfig CONFIG = new AJRModelConfig();

    public AJRArmorBakedModel(Map<String, BakedModel> parts, ItemTransforms transforms, @org.jetbrains.annotations.Nullable ArmorItem.Type itemArmorType) {
        super(parts, transforms, CONFIG, itemArmorType);
    }

    @Override
    public AJRArmorBakedModel withTransforms(ItemTransforms newTransforms) {
        return new AJRArmorBakedModel(this.parts, newTransforms, this.itemArmorType);
    }

    private static class AJRModelConfig implements IArmorModelConfig {
        @Override
        public String getArmorSetId() {
            return "ajr";
        }

        @Override
        public String[] getPartOrder() {
            return AJR_ORDER;
        }

        @Override
        public String[] getPartsForType(ArmorItem.Type armorType) {
            if (armorType == null) return AJR_ORDER;

            if (armorType == ArmorItem.Type.HELMET) return new String[]{"Helmet"};
            if (armorType == ArmorItem.Type.CHESTPLATE) return new String[]{"Chest", "RightArm", "LeftArm"};
            if (armorType == ArmorItem.Type.LEGGINGS) return new String[]{"RightLeg", "LeftLeg"};
            if (armorType == ArmorItem.Type.BOOTS) return new String[]{"RightBoot", "LeftBoot"};
            return AJR_ORDER;
        }

        @Override
        public Class<? extends ModPowerArmorItem> getArmorItemClass() {
            return AJRArmor.class;
        }

        @Override
        public ModelResourceLocation getBaseModelLocation() {
            return ClientPowerArmorRender.AJR_MODEL_BAKED;
        }

        @Override
        public boolean isItemValid(net.minecraft.world.item.ItemStack stack) {
            return stack.getItem() instanceof AJRArmor
                || stack.getItem() instanceof AJROArmor;
        }
    }
}
//?}

