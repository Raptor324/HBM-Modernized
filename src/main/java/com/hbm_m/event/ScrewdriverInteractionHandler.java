package com.hbm_m.event;

import com.hbm_m.block.entity.doors.DoorBlockEntity;
import com.hbm_m.interfaces.IMultiblockPart;
import com.hbm_m.item.ModItems;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.InteractionEvent;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Отмена взаимодействия с дверью при клике отверткой.
 * Блокирует открытие/закрытие двери и открывает GUI выбора скина.
 */
public class ScrewdriverInteractionHandler {

    /**
     * Регистрация обработчика события.
     * Вызывается один раз при инициализации мода.
     */
    public static void init() {
        InteractionEvent.RIGHT_CLICK_BLOCK.register((player, hand, pos, face) -> {
            if (!isScrewdriver(player, hand)) return EventResult.pass();

            Level level = player.level();
            DoorBlockEntity doorEntity = resolveDoorController(level, pos);
            if (doorEntity == null) return EventResult.pass();

            // Блокируем стандартное взаимодействие (открытие/закрытие двери).
            // GUI выбора скина открываем только на клиенте.
            if (level.isClientSide) {
                openSelectionMenu(doorEntity);
            }

            return EventResult.interruptTrue();
        });
    }

    private static boolean isScrewdriver(Player player, InteractionHand hand) {
        ItemStack main = player.getItemInHand(hand);
        ItemStack other = player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        return isScrewdriverStack(main) || isScrewdriverStack(other);
    }

    private static boolean isScrewdriverStack(ItemStack stack) {
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
        EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                com.hbm_m.client.overlay.DoorSelectionClientHooks.openSelectionMenu(doorEntity));
    }
}
