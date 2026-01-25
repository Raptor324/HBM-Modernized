package com.hbm_m.powerarmor.layer;

import com.hbm_m.main.MainRegistry;
import com.hbm_m.powerarmor.AJRArmor;
import com.hbm_m.powerarmor.render.ClientPowerArmorRender;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Render layer for AJR Power Armor (entity rendering).
 * Mirrors the working T51 pipeline but with AJR textures/model id.
 */
@OnlyIn(Dist.CLIENT)
public class AJRPowerArmorLayer<T extends LivingEntity, M extends HumanoidModel<T>> extends AbstractObjArmorLayer<T, M> {

    private static final ResourceLocation AJR_ATLAS_LOCATION = InventoryMenu.BLOCK_ATLAS;

    public AJRPowerArmorLayer(RenderLayerParent<T, M> parent) {
        super(parent);
    }

    @Override
    protected IArmorLayerConfig createConfig() {
        return new AJRConfig();
    }

    private static class AJRConfig implements IArmorLayerConfig {
        private static final Map<String, Material> PART_MATERIALS = Map.of(
                "Helmet", withTex("block/armor/ajr_helmet"),
                "Chest", withTex("block/armor/ajr_chest"),
                "RightArm", withTex("block/armor/ajr_arm"),
                "LeftArm", withTex("block/armor/ajr_arm"),
                "RightLeg", withTex("block/armor/ajr_leg"),
                "LeftLeg", withTex("block/armor/ajr_leg"),
                "RightBoot", withTex("block/armor/ajr_leg"),
                "LeftBoot", withTex("block/armor/ajr_leg")
        );

        private static final Map<String, Vec3> PART_OFFSETS = Map.of(
                "Helmet", Vec3.ZERO,
                "Chest", Vec3.ZERO,
                "RightArm", Vec3.ZERO,
                "LeftArm", Vec3.ZERO,
                "RightLeg", Vec3.ZERO,
                "LeftLeg", Vec3.ZERO,
                "RightBoot", Vec3.ZERO,
                "LeftBoot", Vec3.ZERO
        );

        private static Material withTex(String path) {
            return new Material(AJR_ATLAS_LOCATION, ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, path));
        }

        @Override
        public String getArmorTypeId() {
            return "ajr";
        }

        @Override
        public ModelResourceLocation getBakedModelLocation() {
            return ClientPowerArmorRender.AJR_MODEL_BAKED;
        }

        @Override
        public Map<String, Material> getPartMaterials() {
            return PART_MATERIALS;
        }

        @Override
        public Map<String, Vec3> getPartOffsets() {
            return PART_OFFSETS;
        }

        @Override
        public boolean isItemValid(@NotNull ItemStack stack) {
            return stack.getItem() instanceof AJRArmor;
        }
    }
}

