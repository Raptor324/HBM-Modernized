package com.hbm_m.event;

import com.hbm_m.block.entity.custom.doors.DoorBlockEntity;
import com.hbm_m.client.model.variant.DoorModelSelection;
import com.hbm_m.client.model.variant.DoorModelType;
import com.hbm_m.client.overlay.DoorModelSelectionScreen;
import com.hbm_m.item.ModItems;
import com.hbm_m.main.MainRegistry;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Обработчик взаимодействия с дверями через отвертку.
 * 
 * Клик отверткой ПКМ:
 * - Обычный клик: открывает меню выбора модели
 * - Shift + клик: быстрое переключение Legacy <-> Modern
 * 
 * @author HBM-M Team
 */
@Mod.EventBusSubscriber(modid = MainRegistry.MOD_ID, value = Dist.CLIENT)
public class ScrewdriverInteractionHandler {
    
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        if (level == null) return;
        
        Player player = event.getEntity();
        ItemStack heldItem = event.getItemStack();
        
        // Отладочное логирование
        MainRegistry.LOGGER.debug("ScrewdriverInteraction: Checking item {} (isScrewdriver: {})", 
            heldItem.getItem(), isScrewdriver(heldItem));
        
        if (!isScrewdriver(heldItem)) return;
        
        BlockPos pos = event.getPos();
        BlockEntity blockEntity = level.getBlockEntity(pos);
        
        MainRegistry.LOGGER.debug("ScrewdriverInteraction: Screwdriver detected, checking block at {}. BlockEntity: {}", 
            pos, blockEntity != null ? blockEntity.getClass().getSimpleName() : "null");
        
        if (!(blockEntity instanceof DoorBlockEntity doorEntity)) {
            MainRegistry.LOGGER.debug("ScrewdriverInteraction: Not a door block entity");
            return;
        }
        
        if (!doorEntity.isController()) {
            MainRegistry.LOGGER.debug("ScrewdriverInteraction: Door is not controller");
            return;
        }
        
        MainRegistry.LOGGER.info("ScrewdriverInteraction: Opening door model selection screen for door at {}", pos);
        event.setCanceled(true);
        
        if (level.isClientSide) {
            handleClientSide(player, doorEntity);
        }
    }
    
    private static boolean isScrewdriver(ItemStack stack) {
        if (stack.isEmpty()) return false;
        // Проверяем напрямую по предмету из реестра мода
        return stack.getItem() == ModItems.SCREWDRIVER.get();
    }
    
    private static void handleClientSide(Player player, DoorBlockEntity doorEntity) {
        // Shift + клик = быстрое переключение
        if (player.isShiftKeyDown()) {
            quickToggle(doorEntity);
            return;
        }
        
        // Обычный клик = открытие меню
        openSelectionMenu(doorEntity);
    }
    
    private static void quickToggle(DoorBlockEntity doorEntity) {
        DoorModelSelection current = doorEntity.getModelSelection();
        DoorModelType newType = current.isLegacy() ? DoorModelType.MODERN : DoorModelType.LEGACY;
        
        doorEntity.setModelType(newType);
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.displayClientMessage(
                net.minecraft.network.chat.Component.translatable(
                    "message.hbm_m.door_model_toggled",
                    newType.getDisplayName()
                ),
                true
            );
        }
    }
    
    private static void openSelectionMenu(DoorBlockEntity doorEntity) {
        Minecraft mc = Minecraft.getInstance();
        
        mc.setScreen(new DoorModelSelectionScreen(
            doorEntity.getBlockPos(),
            doorEntity.getDoorDeclId(),
            doorEntity.getModelSelection()
        ));
    }
}
