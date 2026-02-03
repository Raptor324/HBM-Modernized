package com.hbm_m.powerarmor.layer;

import java.util.Map;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.main.MainRegistry;
import com.hbm_m.powerarmor.DNTArmor;
import com.hbm_m.powerarmor.render.ClientPowerArmorRender;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Render layer for DNT Power Armor (entity rendering).
 * Mirrors the T51 pipeline but uses DNT OBJ/model id and textures.
 */
@OnlyIn(Dist.CLIENT)
public class DNTPowerArmorLayer<T extends LivingEntity, M extends HumanoidModel<T>> extends AbstractObjArmorLayer<T, M> {

    private static final ResourceLocation DNT_ATLAS_LOCATION = InventoryMenu.BLOCK_ATLAS;

    public DNTPowerArmorLayer(RenderLayerParent<T, M> parent) {
        super(parent);
    }

    @Override
    protected IArmorLayerConfig createConfig() {
        return new DNTConfig();
    }

    private static class DNTConfig implements IArmorLayerConfig {
        private static final Map<String, Material> PART_MATERIALS = Map.of(
                "Helmet", withTex("block/armor/dnt_helmet"),
                "Chest", withTex("block/armor/dnt_chest"),
                "RightArm", withTex("block/armor/dnt_arm"),
                "LeftArm", withTex("block/armor/dnt_arm"),
                "RightLeg", withTex("block/armor/dnt_leg"),
                "LeftLeg", withTex("block/armor/dnt_leg"),
                "RightBoot", withTex("block/armor/dnt_leg"),
                "LeftBoot", withTex("block/armor/dnt_leg")
        );

        private static Material withTex(String path) {
            return new Material(DNT_ATLAS_LOCATION, ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, path));
        }

        @Override
        public String getArmorTypeId() {
            return "dnt";
        }

        @Override
        public ModelResourceLocation getBakedModelLocation() {
            return ClientPowerArmorRender.DNT_MODEL_BAKED;
        }

        @Override
        public Map<String, Material> getPartMaterials() {
            return PART_MATERIALS;
        }

        @Override
        public boolean isItemValid(@NotNull ItemStack stack) {
            return stack.getItem() instanceof DNTArmor;
        }
    }
}

