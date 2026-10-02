package com.hbm_m.powerarmor;

import java.util.function.Consumer;

import com.hbm_m.item.ModItems;
import com.hbm_m.item.tools_and_armor.ModArmorMaterials;
import com.hbm_m.item.tools_and_armor.ModArmorMaterialsAccess;
import com.hbm_m.powerarmor.overlay.FSBHelmetOverlay;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
//? if forge {
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
//?}

/**
 * 1:1 {@code com.hbm.items.armor.ArmorModel}: Schutzbrille ({@code goggles}), Hut und Umhaenge mit eigenen
 * Modellen (Render-Layer {@code client.render.ArmorModelLayer}). Die Brille verschmiert mit zunehmendem
 * Verschleiss (overlay_goggles_0..5).
 */
public class ArmorModel extends ArmorItem {

    private ResourceLocation[] gogglesBlurs;

    public ArmorModel(ModArmorMaterials material, Type type, Properties properties) {
        super(ModArmorMaterialsAccess.holder(material), type, properties);
    }

    //? if forge {
    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        if (stack.getItem() == ModItems.GOGGLES.get()) {
            return "hbm_m:textures/models/goggles.png";
        }
        if (stack.getItem() == ModItems.CAPE_RADIATION.get()) {
            return "hbm_m:textures/models/capes/caperadiation.png";
        }
        if (stack.getItem() == ModItems.CAPE_GASMASK.get()) {
            return "hbm_m:textures/models/capes/capegasmask.png";
        }
        if (stack.getItem() == ModItems.CAPE_SCHRABIDIUM.get()) {
            return "hbm_m:textures/models/capes/capeschrabidium.png";
        }
        if (stack.getItem() == ModItems.CAPE_HIDDEN.get()) {
            return "hbm_m:textures/models/capes/capehidden.png";
        }
        return "hbm_m:textures/models/capes/capeunknown.png";
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public void renderHelmetOverlay(ItemStack stack, Player player, int width, int height, float partialTick) {
                ArmorModel.this.renderHelmetOverlay(stack, width, height);
            }

            private com.hbm_m.powerarmor.layer.PowerArmorEmptyModel model;

            /** Brille, Hut, No9, Aschebrille und Umhaenge haben eigene Modelle (ArmorAccessoryLayer). */
            @Override
            public net.minecraft.client.model.HumanoidModel<?> getHumanoidArmorModel(net.minecraft.world.entity.LivingEntity living, ItemStack stack,
                    EquipmentSlot slot, net.minecraft.client.model.HumanoidModel<?> original) {
                if (this.model == null) {
                    this.model = new com.hbm_m.powerarmor.layer.PowerArmorEmptyModel(net.minecraft.client.Minecraft.getInstance()
                            .getEntityModels().bakeLayer(com.hbm_m.powerarmor.layer.ModModelLayers.POWER_ARMOR));
                }
                return com.hbm_m.powerarmor.layer.PowerArmorEmptyModel.prepare(this.model, slot, original);
            }

        });
    }
    //?}

    public void renderHelmetOverlay(ItemStack stack, int width, int height) {
        if (this != ModItems.GOGGLES.get() && this != ModItems.HAZMAT_HELMET_RED.get() && this != ModItems.HAZMAT_HELMET_GREY.get()) return;

        if (gogglesBlurs == null) {
            gogglesBlurs = new ResourceLocation[6];
            for (int i = 0; i < 6; i++) gogglesBlurs[i] = ResourceLocation.tryParse("hbm_m:textures/misc/overlay_goggles_" + i + ".png");
        }

        int blurTextureIndex = (int) ((double) stack.getDamageValue() / (double) stack.getMaxDamage() * 6D);

        if (blurTextureIndex < 0 || blurTextureIndex > 5)
            blurTextureIndex = 5;

        FSBHelmetOverlay.render(gogglesBlurs[blurTextureIndex], width, height);
    }
}
