package com.hbm_m.block.machines;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineRtgFurnaceBlockEntity;

import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 1:1 {@code MachineRtgFurnace} (im Original {@code @Deprecated}, ohne Kreativ-Tab und ohne Rezept). Das Original tauscht
 * zwischen {@code machine_rtg_furnace_off}/{@code _on}; hier ueber {@code LIT} (Fronttextur, Licht 1.0, Rauch/Flamme).
 * Laesst sich selbst nicht fallen ({@code getItemDropped -> null}), nur den Inhalt.
 */
public class MachineRtgFurnaceBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public MachineRtgFurnaceBlock(Properties properties) {
        super(properties.lightLevel(st -> st.getValue(LIT) ? 15 : 0));
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }

    /** Original {@code onBlockPlacedBy}: Front zeigt zum Spieler. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachineRtgFurnaceBlockEntity(pos, state);
    }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.MACHINE_RTG_FURNACE_BE.get(),
                (lvl, pos, st, be) -> MachineRtgFurnaceBlockEntity.tick(lvl, pos, st, (MachineRtgFurnaceBlockEntity) be));
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return activate(level, pos, player);
    }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return activate(level, pos, player);
    }
    *///?}

    /** Original {@code onBlockActivated}: ohne Schleichen GUI, mit Schleichen nichts. */
    private InteractionResult activate(Level level, BlockPos pos, Player player) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!player.isShiftKeyDown()) {
            if (level.getBlockEntity(pos) instanceof MachineRtgFurnaceBlockEntity be) {
                MenuRegistry.openExtendedMenu((ServerPlayer) player, be, buf -> buf.writeBlockPos(pos));
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    /** Original {@code breakBlock}: Inhalt verstreuen, ausser beim reinen An/Aus-Wechsel ({@code keepInventory}). */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof MachineRtgFurnaceBlockEntity be) {
                be.dropInventoryContents();
                level.updateNeighbourForOutputSignal(pos, this);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    /** Original {@code randomDisplayTick}: Rauch und Flamme vor der Front, solange er an ist. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rand) {
        if (!state.getValue(LIT)) return;
        Direction l = state.getValue(FACING);
        float f = pos.getX() + 0.5F;
        float f1 = pos.getY() + 0.25F + rand.nextFloat() * 6.0F / 16.0F;
        float f2 = pos.getZ() + 0.5F;
        float f3 = 0.52F;
        float f4 = rand.nextFloat() * 0.6F - 0.3F;
        rand.nextFloat();
        rand.nextFloat();

        double px, pz;
        switch (l) {
            case WEST -> { px = f - f3; pz = f2 + f4; }
            case EAST -> { px = f + f3; pz = f2 + f4; }
            case NORTH -> { px = f + f4; pz = f2 - f3; }
            default -> { px = f + f4; pz = f2 + f3; }
        }
        level.addParticle(ParticleTypes.SMOKE, px, f1, pz, 0.0D, 0.0D, 0.0D);
        level.addParticle(ParticleTypes.FLAME, px, f1, pz, 0.0D, 0.0D, 0.0D);
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<MachineRtgFurnaceBlock> CODEC = simpleCodec(MachineRtgFurnaceBlock::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return CODEC;
    }
    *///?}
}
