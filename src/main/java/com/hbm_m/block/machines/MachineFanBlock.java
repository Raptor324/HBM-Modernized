package com.hbm_m.block.machines;

import com.hbm_m.platform.PlatformHooks;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.block.IToolable;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineFanBlockEntity;
import com.hbm_m.network.InfoToastPacket;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

/**
 * 1:1 {@code MachineFan}: per Redstone betriebener Ventilator, Ausrichtung wie ein Kolben, schiebt Wesen bis zu zehn
 * Bloecke weit. Schraubendreher dreht um, Handbohrer schaltet den Abfall mit der Entfernung, Entschaerfer saugt statt
 * zu blasen.
 */
public class MachineFanBlock extends BaseEntityBlock implements IToolable {

    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    public MachineFanBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.UP));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    /** Original {@code BlockPistonBase.determineOrientation}: zeigt zum Spieler. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, determineOrientation(ctx.getClickedPos(), ctx.getPlayer()));
    }

    public static Direction determineOrientation(BlockPos pos, @Nullable LivingEntity player) {
        if (player == null) return Direction.UP;
        if (Mth.abs((float) player.getX() - pos.getX()) < 2.0F && Mth.abs((float) player.getZ() - pos.getZ()) < 2.0F) {
            double d0 = player.getY() + 1.82D - PlatformHooks.getMyRidingOffset(player);
            if (d0 - pos.getY() > 2.0D) return Direction.UP;
            if (pos.getY() - d0 > 0.0D) return Direction.DOWN;
        }
        int l = Mth.floor(player.getYRot() * 4.0F / 360.0F + 0.5D) & 3;
        return switch (l) {
            case 0 -> Direction.NORTH;
            case 1 -> Direction.EAST;
            case 2 -> Direction.SOUTH;
            default -> Direction.WEST;
        };
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachineFanBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.FAN_BE.get(), MachineFanBlockEntity::tick);
    }

    @Override
    public boolean onScrew(Level world, Player player, BlockPos pos, Direction side, float fX, float fY, float fZ, InteractionHand hand, ToolType tool) {
        BlockState state = world.getBlockState(pos);

        if (tool == ToolType.SCREWDRIVER) {
            world.setBlock(pos, state.setValue(FACING, state.getValue(FACING).getOpposite()), 3);
            return true;
        }

        if (tool == ToolType.HAND_DRILL) {
            if (world.getBlockEntity(pos) instanceof MachineFanBlockEntity tile) {
                tile.falloff = !tile.falloff;
                tile.setChanged();
                inform(world, player, pos, tile.falloff ? ".falloffOn" : ".falloffOff");
            }
            return true;
        }

        if (tool == ToolType.DEFUSER) {
            if (world.getBlockEntity(pos) instanceof MachineFanBlockEntity tile) {
                tile.suck = !tile.suck;
                tile.setChanged();
                inform(world, player, pos, tile.suck ? ".suckOn" : ".suckOff");
            }
            return true;
        }

        return false;
    }

    private void inform(Level world, Player player, BlockPos pos, String suffix) {
        if (world.isClientSide) return;
        if (player instanceof ServerPlayer sp) {
            InfoToastPacket.sendTo(sp, Component.translatable(getDescriptionId() + suffix).withStyle(ChatFormatting.GOLD), 60, 10, 0xFFAA00);
        }
        world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.5F, 0.5F);
    }

    @Override
    //? if < 1.21.1 {
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> list, TooltipFlag flag) {
    //?} else {
    /*public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext hbmTooltipCtx, List<Component> list, TooltipFlag flag) {
    *///?}
        com.hbm_m.util.StandardInfo.add(list, getDescriptionId() + ".desc");
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<MachineFanBlock> CODEC = simpleCodec(MachineFanBlock::new);
    @Override protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    *///?}
}
