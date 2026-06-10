//? if forge {
package com.hbm_m.powerarmor.render;


import java.util.Map;

import com.hbm_m.interfaces.IArmorModelConfig;
import com.hbm_m.powerarmor.DNTArmor;
import com.hbm_m.powerarmor.ModPowerArmorItem;

import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.item.ArmorItem;

//? if fabric {
/*import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;*///?}

/**
 * Baked model for rendering DNT power armor in GUI and hand.
 * Uses the same multipart baked model infrastructure as T51/AJR/Bismuth.
 */
//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
@OnlyIn(Dist.CLIENT)
//?}
//? if fabric {
/*@Environment(EnvType.CLIENT)*///?}
public class DNTArmorBakedModel extends AbstractArmorBakedModel {

    private static final String[] DNT_ORDER = {
            "Helmet", "Chest", "RightArm", "LeftArm", "RightLeg", "LeftLeg", "RightBoot", "LeftBoot"
    };

    private static final DNTModelConfig CONFIG = new DNTModelConfig();

    public DNTArmorBakedModel(Map<String, BakedModel> parts, ItemTransforms transforms, @org.jetbrains.annotations.Nullable ArmorItem.Type itemArmorType) {
        super(parts, transforms, CONFIG, itemArmorType);
    }

    @Override
    public DNTArmorBakedModel withTransforms(ItemTransforms newTransforms) {
        return new DNTArmorBakedModel(this.parts, newTransforms, this.itemArmorType);
    }

    private static class DNTModelConfig implements IArmorModelConfig {
        @Override
        public String getArmorSetId() {
            return "dnt";
        }

        @Override
        public String[] getPartOrder() {
            return DNT_ORDER;
        }

        @Override
        public String[] getPartsForType(ArmorItem.Type armorType) {
            if (armorType == null) {
                return DNT_ORDER;
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
            return DNT_ORDER;
        }

        @Override
        public Class<? extends ModPowerArmorItem> getArmorItemClass() {
            return DNTArmor.class;
        }

        @Override
        public ModelResourceLocation getBaseModelLocation() {
            return ClientPowerArmorRender.DNT_MODEL_BAKED;
        }

        @Override
        public boolean isItemValid(net.minecraft.world.item.ItemStack stack) {
            return stack.getItem() instanceof DNTArmor;
        }
    }
}
//?}

