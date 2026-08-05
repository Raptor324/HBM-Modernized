package com.hbm_m.item.tool;

import java.util.List;

import com.hbm_m.blockentity.IRadarCommandReceiver;
import com.hbm_m.multiblock.MultiblockInteractionHelper;
import com.hbm_m.sound.ModSounds;
import com.hbm_m.platform.PlatformHooks;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Порт {@code ItemRadarLinker} (extends {@code ItemCoordinateBase}) из 1.7.10.
 *
 * Связывает радар с пусковой установкой (или другим {@link IRadarCommandReceiver}):
 * правый клик по приёмнику команд записывает его BlockPos в NBT предмета.
 * При клике по цели в GUI радара, радар читает эти координаты и вызывает
 * {@code sendCommandEntity}/{@code sendCommandPosition} на привязанной установке.
 *
 * NBT-ключи {@code xCoord/yCoord/zCoord} совпадают с {@code ItemCoordinateBase.getPosition}.
 */
public class ItemRadarLinker extends Item {

    public ItemRadarLinker(Properties properties) {
        super(properties.stacksTo(1));
    }

    /**
     * Может ли предмет привязаться к блоку в данной позиции (приёмник команд).
     * Позиция предварительно резолвится до контроллера мультиблока
     * ({@link MultiblockInteractionHelper#resolveControllerPos}), поэтому клик
     * по любой части пусковой установки считается кликом по контроллеру.
     */
    public static boolean canLinkTo(Level level, BlockPos pos) {
        if (level == null) {
            return false;
        }
        BlockEntity be = MultiblockInteractionHelper.resolveControllerBlockEntity(level, pos);
        // Порт ItemRadarLinker.canGrabCoordinateHere: IRadarCommandReceiver ИЛИ TileEntityMachineRadarScreen.
        return be instanceof IRadarCommandReceiver
                || be instanceof com.hbm_m.blockentity.machines.MachineRadarScreenBlockEntity;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        CompoundTag tag = PlatformHooks.getItemTag(stack);
        if (tag != null && tag.contains("xCoord")) {
            tooltip.add(Component.translatable("tooltip.hbm_m.radar_linker.linked"));
            tooltip.add(Component.literal("X: " + tag.getInt("xCoord")).withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal("Y: " + tag.getInt("yCoord")).withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal("Z: " + tag.getInt("zCoord")).withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable("tooltip.hbm_m.radar_linker.not_linked"));
        }
    }

    @Override
    public InteractionResult useOn(net.minecraft.world.item.context.UseOnContext context) {
        Level level = context.getLevel();
        // Клик по любой части мультиблока → координаты блока-контроллера.
        BlockPos pos = MultiblockInteractionHelper.resolveControllerPos(level, context.getClickedPos());

        if (!canLinkTo(level, pos)) {
            return InteractionResult.PASS;
        }

        ItemStack stack = context.getItemInHand();
        PlatformHooks.editItemTag(stack, tag -> {
            tag.putInt("xCoord", pos.getX());
            tag.putInt("yCoord", pos.getY());
            tag.putInt("zCoord", pos.getZ());
        });

        if (level.isClientSide()) {
            Player player = context.getPlayer();
            if (player != null) {
                player.displayClientMessage(
                        Component.translatable("message.hbm_m.radar_linker.linked"), true);
            }
        }
        level.playSound(context.getPlayer(), context.getClickedPos(),
                ModSounds.TOOL_TECH_BLEEP.get(), SoundSource.PLAYERS, 1.0F, 1.0F);

        return InteractionResult.SUCCESS;
    }
}
