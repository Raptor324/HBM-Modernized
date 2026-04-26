package com.hbm_m.powerarmor.render;


import java.util.Map;

import com.hbm_m.interfaces.IArmorModelConfig;
import com.hbm_m.powerarmor.BismuthArmor;
import com.hbm_m.powerarmor.ModPowerArmorItem;

import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.item.ArmorItem;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//? if fabric {
/*import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;*///?}

/**
 * Baked model for rendering Bismuth power armor in GUI/hand.
 * Uses the same multipart baked model infrastructure as T51/AJR.
 */
//? if forge {
@OnlyIn(Dist.CLIENT)
//?}
//? if fabric {
/*@Environment(EnvType.CLIENT)*///?}
public class BismuthArmorBakedModel extends AbstractArmorBakedModel {

    private static final String[] BISMUTH_ORDER = {
            "Helmet", "Chest", "RightArm", "LeftArm", "RightLeg", "LeftLeg", "RightBoot", "LeftBoot"
    };

    private static final BismuthModelConfig CONFIG = new BismuthModelConfig();

    public BismuthArmorBakedModel(Map<String, BakedModel> parts, ItemTransforms transforms) {
        super(parts, transforms, CONFIG);
    }

    @Override
    public BismuthArmorBakedModel withTransforms(ItemTransforms newTransforms) {
        return new BismuthArmorBakedModel(this.parts, newTransforms);
    }

    private static class BismuthModelConfig implements IArmorModelConfig {
        @Override
        public String getArmorSetId() {
            return "bismuth";
        }

        @Override
        public String[] getPartOrder() {
            return BISMUTH_ORDER;
        }

        @Override
        public String[] getPartsForType(ArmorItem.Type armorType) {
            if (armorType == null) return BISMUTH_ORDER;

            return switch (armorType) {
                case HELMET -> new String[]{"Helmet"};
                case CHESTPLATE -> new String[]{"Chest", "RightArm", "LeftArm"};
                case LEGGINGS -> new String[]{"RightLeg", "LeftLeg"};
                case BOOTS -> new String[]{"RightBoot", "LeftBoot"};
            };
        }

        @Override
        public Class<? extends ModPowerArmorItem> getArmorItemClass() {
            return BismuthArmor.class;
        }

        @Override
        public ModelResourceLocation getBaseModelLocation() {
            return ClientPowerArmorRender.BISMUTH_MODEL_BAKED;
        }

        @Override
        public boolean isItemValid(net.minecraft.world.item.ItemStack stack) {
            return stack.getItem() instanceof BismuthArmor;
        }
    }
}

