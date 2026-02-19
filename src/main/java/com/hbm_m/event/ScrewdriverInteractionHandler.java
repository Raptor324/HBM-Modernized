package com.hbm_m.event;

import com.hbm_m.block.entity.custom.doors.DoorBlockEntity;
import com.hbm_m.item.ModItems;
import com.hbm_m.multiblock.IMultiblockPart;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;

/**
 * Отмена взаимодействия с дверью при клике отверткой.
 * Блокирует открытие/закрытие двери и открывает GUI выбора скина.
 */
@Mod.EventBusSubscriber(modid = com.hbm_m.main.MainRegistry.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ScrewdriverInteractionHandler {

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!isScrewdriver(event)) return;

        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        DoorBlockEntity doorEntity = resolveDoorController(level, pos);
        if (doorEntity == null) return;

        event.setCanceled(true);

        if (level.isClientSide) {
            openSelectionMenu(doorEntity);
        }
    }

    private static boolean isScrewdriver(PlayerInteractEvent.RightClickBlock event) {
        net.minecraft.world.entity.player.Player player = event.getEntity();
        return isScrewdriverStack(player.getItemInHand(event.getHand()))
                || isScrewdriverStack(player.getItemInHand(event.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND
                        ? net.minecraft.world.InteractionHand.OFF_HAND : net.minecraft.world.InteractionHand.MAIN_HAND));
    }

    private static boolean isScrewdriverStack(net.minecraft.world.item.ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() == ModItems.SCREWDRIVER.get();
    }

    private static DoorBlockEntity resolveDoorController(Level level, BlockPos clickedPos) {
        BlockEntity be = level.getBlockEntity(clickedPos);
        if (be instanceof DoorBlockEntity doorBE && doorBE.isController()) {
            return doorBE;
        }
        if (be instanceof IMultiblockPart part) {
            BlockPos controllerPos = part.getControllerPos();
            if (controllerPos != null) {
                BlockEntity ctrlBe = level.getBlockEntity(controllerPos);
                if (ctrlBe instanceof DoorBlockEntity doorBE && doorBE.isController()) {
                    return doorBE;
                }
            }
        }
        return null;
    }

    private static void openSelectionMenu(DoorBlockEntity doorEntity) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                com.hbm_m.client.overlay.DoorSelectionClientHooks.openSelectionMenu(doorEntity));
    }
}
