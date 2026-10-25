package com.hbm_m.block.machines;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineDifurnaceRtgBlockEntity;

import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
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
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Port of {@code MachineDiFurnaceRTG} (1.7.10 Original). Das Original tauscht zwischen zwei Bloecken
 * ({@code _off}/{@code _on}); hier gleichwertig ueber die Eigenschaft {@code LIT} (Textur und Licht).
 */
public class MachineDifurnaceRtgBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    /** Original: {@code machine_difurnace_rtg_on} (Lichtstaerke 1.0) statt {@code _off}. */
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty LIT = BlockStateProperties.LIT;

    public MachineDifurnaceRtgBlock(Properties properties) {
        super(properties.lightLevel(st -> st.getValue(LIT) ? 15 : 0));
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    /** audit10: 1:1 {@code MachineDiFurnaceRTG.breakBlock} - Inhalt faellt heraus. */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (state.getBlock() != newState.getBlock() && level.getBlockEntity(pos) instanceof MachineDifurnaceRtgBlockEntity furnace) {
            furnace.dropInventoryContents();
        }
        super.onRemove(state, level, pos, newState, moving);
    }

    /** audit10: 1:1 {@code MachineDiFurnaceRTG.randomDisplayTick} - in Betrieb Flamme vor der Front, Rauch oben. */
    @Override
    public void animateTick(BlockState state, Level world, BlockPos pos, net.minecraft.util.RandomSource rand) {
        if (state.getValue(LIT)) {
            Direction dir = state.getValue(FACING);
            int x = pos.getX(), y = pos.getY(), z = pos.getZ();
            float f = x + 0.5F;
            float f1 = y + 0.25F + rand.nextFloat() * 6.0F / 16.0F;
            float f2 = z + 0.5F;
            float f3 = 0.52F;
            float f4 = rand.nextFloat() * 0.5F - 0.25F;
            float f5 = rand.nextFloat() * 0.75F + 0.125F;
            float f6 = rand.nextFloat() * 0.75F + 0.125F;

            double px = dir.getStepX() != 0 ? f + dir.getStepX() * f3 : f + f4;
            double pz = dir.getStepZ() != 0 ? f2 + dir.getStepZ() * f3 : f2 + f4;
            world.addParticle(net.minecraft.core.particles.ParticleTypes.FLAME, px, f1, pz, 0.0D, 0.0D, 0.0D);
            world.addParticle(net.minecraft.core.particles.ParticleTypes.SMOKE, x + f5, (double) y + 1, z + f6, 0.0D, 0.0D, 0.0D);
        }
    }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachineDifurnaceRtgBlockEntity(pos, state);
    }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.MACHINE_DIFURNACE_RTG_BE.get(),
                (lvl, pos, st, be) -> MachineDifurnaceRtgBlockEntity.tick(lvl, pos, st, (MachineDifurnaceRtgBlockEntity) be));
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide() && player.isShiftKeyDown()) return InteractionResult.PASS; // Original: geschlichen auf dem Server false

        if (!level.isClientSide()) {
            BlockEntity entity = level.getBlockEntity(pos);
            if (entity instanceof MenuProvider menuProvider) {
                MenuRegistry.openExtendedMenu((ServerPlayer) player, menuProvider, buf -> buf.writeBlockPos(pos));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
        }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player.isShiftKeyDown()) return InteractionResult.PASS; // Original: geschlichen auf dem Server false

        if (!level.isClientSide()) {
            BlockEntity entity = level.getBlockEntity(pos);
            if (entity instanceof MenuProvider menuProvider) {
                MenuRegistry.openExtendedMenu((ServerPlayer) player, menuProvider, buf -> buf.writeBlockPos(pos));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
        }
    *///?}


    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<MachineDifurnaceRtgBlock> CODEC = simpleCodec(MachineDifurnaceRtgBlock::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return CODEC;
    }
    *///?}
}
