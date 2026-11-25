package com.hbm_m.client.render.armor;

import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.powerarmor.ModArmorFSB;
import com.hbm_m.powerarmor.layer.ModModelLayers;
import com.hbm_m.powerarmor.layer.PowerArmorEmptyModel;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Shared client item extensions for ALL 3D OBJ/NMesh armors and hazmat suits across Forge (1.20.1)
 * and NeoForge (1.21.1).
 *
 * <ul>
 *   <li>Suppresses vanilla 2D {@code HumanoidArmorLayer} via {@link PowerArmorEmptyModel}</li>
 *   <li>Provides {@link ArmorItemRenderer#INSTANCE} as the {@link BlockEntityWithoutLevelRenderer}
 *       for inventory, hand, ground, and item-frame 3D rendering</li>
 *   <li>Forwards optional helmet HUD overlays (e.g. {@link ModArmorFSB#renderHelmetOverlay})</li>
 * </ul>
 */
public final class ArmorClientExtensions {

    private ArmorClientExtensions() {}

    @FunctionalInterface
    public interface OverlayCallback {
        void render(ItemStack stack, Player player, int width, int height, float partialTick);
    }

    //? if forge {
    @SuppressWarnings("removal")
    public static void acceptForge3dArmor(
            Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer,
            @Nullable OverlayCallback overlay) {
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions() {
            private PowerArmorEmptyModel emptyModel;

            @Override
            public @NotNull HumanoidModel<?> getHumanoidArmorModel(LivingEntity living, ItemStack stack,
                                                                   EquipmentSlot slot, HumanoidModel<?> original) {
                if (this.emptyModel == null) {
                    ModelPart layer = Minecraft.getInstance().getEntityModels().bakeLayer(ModModelLayers.POWER_ARMOR);
                    this.emptyModel = new PowerArmorEmptyModel(layer);
                }
                return PowerArmorEmptyModel.prepare(this.emptyModel, slot, original);
            }

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return ArmorItemRenderer.INSTANCE;
            }

            @Override
            public void renderHelmetOverlay(ItemStack stack, Player player, int width, int height, float partialTick) {
                if (overlay != null) {
                    overlay.render(stack, player, width, height, partialTick);
                }
            }
        });
    }
    //?}

    //? if neoforge {
    /*public static net.neoforged.neoforge.client.extensions.common.IClientItemExtensions createNeoForge3dArmorExtensions(boolean customRenderer) {
        return new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions() {
            private PowerArmorEmptyModel emptyModel;

            @Override
            public HumanoidModel<?> getHumanoidArmorModel(LivingEntity living, ItemStack stack,
                                                          EquipmentSlot slot, HumanoidModel<?> original) {
                if (this.emptyModel == null) {
                    ModelPart layer = Minecraft.getInstance().getEntityModels().bakeLayer(ModModelLayers.POWER_ARMOR);
                    this.emptyModel = new PowerArmorEmptyModel(layer);
                }
                return PowerArmorEmptyModel.prepare(this.emptyModel, slot, original);
            }

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return customRenderer ? ArmorItemRenderer.INSTANCE
                        : net.neoforged.neoforge.client.extensions.common.IClientItemExtensions.super.getCustomRenderer();
            }

            @Override
            public void renderHelmetOverlay(ItemStack stack, Player player,
                                            net.minecraft.client.gui.GuiGraphics gui,
                                            net.minecraft.client.DeltaTracker delta) {
                if (stack.getItem() instanceof ModArmorFSB fsb) {
                    fsb.renderHelmetOverlay(stack, player, gui.guiWidth(), gui.guiHeight(),
                            delta.getGameTimeDeltaPartialTick(true));
                }
            }
        };
    }

    public static void registerAllNeoForgeExtensions(net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent event) {
        var objExt = createNeoForge3dArmorExtensions(true);
        Item[] items = ArmorRenderRegistry.getAll3dArmorItems().toArray(Item[]::new);
        event.registerItem(objExt, items);

        // Non-OBJ-item 3D helmets (rendered by GasMaskLayer as M65 mask with PowerArmorEmptyModel)
        event.registerItem(createNeoForge3dArmorExtensions(false), com.hbm_m.item.ModItems.LIQUIDATOR_HELMET.get());
    }
    *///?}
}
