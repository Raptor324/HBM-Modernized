package com.hbm_m.item.tools_and_armor;

import com.hbm_m.item.ITooltipProvider;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.entity.doors.DoorBlockEntity;
import com.hbm_m.block.machines.pile.BlockGraphiteDrilledBase;
import com.hbm_m.interfaces.IMultiblockPart;
import com.hbm_m.lib.RefStrings;

import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Отвертка для настройки дверей и конвертера энергии.
 * ПКМ по двери (контроллеру или любой части) - открывает GUI, дверь не открывается.
 */
public class ScrewdriverItem extends Item implements ITooltipProvider {

    public ScrewdriverItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    /**
     * Отвёртка — «контейнерный» ингредиент, как ведро в торте: в shaped-рецептах
     * (например, key_kit) возвращается в инвентарь после крафта.
     */
    @Override
    public ItemStack getCraftingRemainingItem(ItemStack itemStack) {
        return itemStack.copy();
    }

    @Override
    public void appendHbmTooltip(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip." + RefStrings.MODID + ".screwdriver").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;

        DoorBlockEntity doorEntity = resolveDoorController(level, pos);
        if (doorEntity == null) {
            // NEXT: ItemTooling.useOn dispatches IToolable.onScrew with no sneak gate. The pile
            // blocks' sneak interactions (detector flux readout / defuser limit, drilled
            // unshroud) are only reachable from here: vanilla skips the block use hooks
            // whenever the player sneaks with a non-empty hand.
            BlockState pileState = level.getBlockState(pos);
            if (pileState.getBlock() instanceof BlockGraphiteDrilledBase pile)
                return pile.onScrewForTool(pileState, level, pos, player, context.getClickedFace(), context.getHand());
            return InteractionResult.PASS;
        }

        if (level.isClientSide) {
            EnvExecutor.runInEnv(Env.CLIENT, () -> () ->
                    com.hbm_m.client.overlay.DoorSelectionClientHooks.openSelectionMenu(doorEntity));
        }
        // CONSUME явно помечает взаимодействие как обработанное - блок use() не вызывается
        return InteractionResult.CONSUME;
    }

    private static DoorBlockEntity resolveDoorController(Level level, BlockPos clickedPos) {
        BlockEntity be = level.getBlockEntity(clickedPos);
        // Blast door: GUI скинов нет (NEXT BlastDoor.java:99-105) — отвёртка тогглит дверь
        // как пустая рука; не перехватываем клик (паритет BlastDoorBlock/UniversalMachinePartBlock)
        if (be instanceof com.hbm_m.block.entity.doors.BlastDoorBlockEntity) {
            return null;
        }
        if (be instanceof DoorBlockEntity doorBE && doorBE.isController()) {
            return doorBE;
        }
        if (be instanceof IMultiblockPart part) {
            BlockPos controllerPos = part.getControllerPos();
            if (controllerPos != null) {
                BlockEntity ctrlBe = level.getBlockEntity(controllerPos);
                if (ctrlBe instanceof DoorBlockEntity doorBE && doorBE.isController()
                        && !(doorBE instanceof com.hbm_m.block.entity.doors.BlastDoorBlockEntity)) {
                    return doorBE;
                }
            }
        }
        return null;
    }

}
