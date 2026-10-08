package com.hbm_m.block.machines.custom;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.custom.CustomMachineBlockEntity;
import com.hbm_m.item.ModItems;

import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 1:1 {@code BlockCustomMachine}: Steuerung einer Custom Machine. Vorderseite zum Setzenden; der Maschinentyp kommt
 * aus dem Gegenstand ({@code machineType}). Rechtsklick prueft den Bauplan und oeffnet bei Erfolg die GUI, mit dem
 * Bau-Zauberstab {@code wand_s} wird ein unvollstaendiger Bauplan gesetzt. Beim Abbau faellt die Steuerung mit ihrem
 * Typ heraus, die Filter bleiben zurueck.
 */
public class CustomMachineBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    public CustomMachineBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CustomMachineBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.CUSTOM_MACHINE_BE.get(), CustomMachineBlockEntity::serverTick);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.getBlockEntity(pos) instanceof CustomMachineBlockEntity tile) {
            String type = ItemCustomMachine.machineType(stack);
            if (type != null && com.hbm_m.config.CustomMachineConfigJSON.customMachines.containsKey(type)) {
                tile.machineType = type;
                tile.init();
                tile.setChanged();
            }
        }
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.isShiftKeyDown()) return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.PASS; // Original: Client true, Server geschlichen false
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(pos) instanceof CustomMachineBlockEntity tile) {
            if (tile.checkStructure()) {
                MenuRegistry.openExtendedMenu((ServerPlayer) player, tile, buf -> buf.writeBlockPos(pos));
            } else if (player.getItemInHand(hand).is(ModItems.WAND_S.get())) {
                tile.buildStructure();
            }
        }
        return InteractionResult.SUCCESS;
    }
    //?}

    /** {@code getDrops}: die Steuerung mit ihrem Maschinentyp. */
    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        List<ItemStack> ret = new ArrayList<>();
        BlockEntity be = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (be instanceof CustomMachineBlockEntity tile && tile.machineType != null && tile.config != null) {
            ret.add(ItemCustomMachine.make(tile.machineType));
        }
        return ret;
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        if (level.getBlockEntity(pos) instanceof CustomMachineBlockEntity tile && tile.machineType != null && !tile.machineType.isEmpty()) {
            return ItemCustomMachine.make(tile.machineType);
        }
        return super.getCloneItemStack(level, pos, state);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide && level.getBlockEntity(pos) instanceof CustomMachineBlockEntity tile) {
            tile.dropInventoryContents();
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<CustomMachineBlock> CODEC = simpleCodec(CustomMachineBlock::new);
    @Override protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    *///?}
}
