package com.hbm_m.block.machines;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.PWRBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * 1:1 {@code BlockPWR} ({@code pwr_block}): der Traeger, in den der Controller beim Zusammenbau jedes Bauteil
 * verwandelt. Er merkt sich das Bauteil und den Controller; zerschlagen gibt er das Bauteil zurueck und der Reaktor gilt
 * als zerlegt. {@link #PORT} (Original Metadaten 1) sind die ehemaligen Anschluesse - nur sie reichen Fluessigkeit,
 * Inventar und Funkwerte an den Controller durch. Die Aussenhaut ist eine CT-Textur, die sich mit anderen Traegern und
 * dem Controller verbindet.
 */
public class PWRBlock extends BaseEntityBlock {

    public static final BooleanProperty PORT = BooleanProperty.create("port");

    public PWRBlock(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(PORT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PORT);
    }

    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new PWRBlockEntity(pos, state); }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.PWR_BLOCK_BE.get(), PWRBlockEntity::serverTick);
    }

    /** Original: {@code getItemDropped} gibt null. */
    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) { return ItemStack.EMPTY; }

    /**
     * 1:1 {@code breakBlock}: das gemerkte Bauteil kommt zurueck und der Controller verliert den Zusammenbau. Das
     * Zuruecksetzen laeuft nachgereicht, weil mitten im Entfernen an derselben Stelle kein neuer Block gesetzt werden darf.
     */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock() && !level.isClientSide()
                && level.getBlockEntity(pos) instanceof PWRBlockEntity pwr && pwr.block != null) {

            Block restored = pwr.block;
            if (level.getBlockEntity(pwr.core) instanceof com.hbm_m.blockentity.machines.PWRControllerBlockEntity controller) {
                controller.assembled = false;
                controller.setChanged();
            }

            if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                BlockPos target = pos.immutable();
                serverLevel.getServer().submit(() -> {
                    if (serverLevel.getBlockState(target).isAir()) serverLevel.setBlock(target, restored.defaultBlockState(), 3);
                });
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<PWRBlock> CODEC = simpleCodec(PWRBlock::new);
    @Override protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    *///?}
}
