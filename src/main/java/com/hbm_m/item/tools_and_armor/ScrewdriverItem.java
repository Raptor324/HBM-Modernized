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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code screwdriver}/{@code screwdriver_desh} = {@code ItemTooling(ToolType.SCREWDRIVER, 100 | 0)}: bleibt beim
 * Craften erhalten (nutzt sich um 1 ab) und ruft {@code IToolable.onScrew}; zusaetzlich oeffnet er im Port das
 * Tuer-Auswahlmenue.
 */
public class ScrewdriverItem extends com.hbm_m.item.tool.ItemTooling implements ITooltipProvider {

    public ScrewdriverItem(int durability, Properties properties) {
        super(com.hbm_m.api.block.IToolable.ToolType.SCREWDRIVER, durability, properties);
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

        // Shift-click dismantles the top floor of a cargo elevator shaft (any part: core or cell).
        // Vanilla skips the block use hooks when sneaking with a non-empty hand, so this lives here.
        if (player.isShiftKeyDown()) {
            BlockEntity clicked = level.getBlockEntity(pos);
            com.hbm_m.blockentity.machines.CargoElevatorBlockEntity elevator = null;
            if (clicked instanceof com.hbm_m.blockentity.machines.CargoElevatorBlockEntity be) {
                elevator = be.isCore() ? be : be.resolveCore(com.hbm_m.blockentity.machines.CargoElevatorBlockEntity.class);
            } else if (clicked instanceof IMultiblockPart part && part.getControllerPos() != null
                    && level.getBlockEntity(part.getControllerPos()) instanceof com.hbm_m.blockentity.machines.CargoElevatorBlockEntity be) {
                elevator = be;
            }
            if (elevator != null) {
                if (com.hbm_m.block.machines.CargoElevatorBlock.dismantleTopFloor(level, elevator.getBlockPos(), elevator, player)) {
                    return level.isClientSide ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
                }
                return InteractionResult.FAIL;
            }
        }

        DoorBlockEntity doorEntity = resolveDoorController(level, pos);
        if (doorEntity == null) {
            // NEXT: ItemTooling.useOn dispatches IToolable.onScrew with no sneak gate. The pile
            // blocks' sneak interactions (detector flux readout / defuser limit, drilled
            // unshroud) are only reachable from here: vanilla skips the block use hooks
            // whenever the player sneaks with a non-empty hand.
            BlockState pileState = level.getBlockState(pos);
            if (pileState.getBlock() instanceof BlockGraphiteDrilledBase pile)
                return pile.onScrewForTool(pileState, level, pos, player, context.getClickedFace(), context.getHand());
            return super.useOn(context);
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
