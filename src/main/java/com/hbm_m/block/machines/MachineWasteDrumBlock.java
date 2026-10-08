package com.hbm_m.block.machines;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineWasteDrumBlockEntity;
import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/** Port of {@code WasteDrum} (1.7.10 Original). */
public class MachineWasteDrumBlock extends BaseEntityBlock {

    public MachineWasteDrumBlock(Properties properties) { super(properties); }

    @Override
    public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }


    /** audit10: 1:1 {@code WasteDrum.breakBlock} - Inhalt faellt beim Entfernen heraus. */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof com.hbm_m.blockentity.machines.MachineWasteDrumBlockEntity be) {
            com.hbm_m.block.BlockDropUtil.dropSlots(level, pos, be.getInventory(), 0, Integer.MAX_VALUE);
        }
        super.onRemove(state, level, pos, newState, moving);
    }

    /** audit10: 1:1 {@code WasteDrum.randomDisplayTick} - an Wasserseiten (ausser unten) steigen Blasen auf. */
    @Override
    public void animateTick(BlockState state, Level world, BlockPos pos, net.minecraft.util.RandomSource rand) {
        super.animateTick(state, world, pos, rand);
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();

        for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.values()) {

            if (dir == net.minecraft.core.Direction.DOWN)
                continue;

            if (world.getFluidState(pos.relative(dir)).is(net.minecraft.tags.FluidTags.WATER)) {

                double ix = x + 0.5F + dir.getStepX() + rand.nextDouble() - 0.5D;
                double iy = y + 0.5F + dir.getStepY() + rand.nextDouble() - 0.5D;
                double iz = z + 0.5F + dir.getStepZ() + rand.nextDouble() - 0.5D;

                if (dir.getStepX() != 0)
                    ix = x + 0.5F + dir.getStepX() * 0.5 + rand.nextDouble() * 0.125 * dir.getStepX();
                if (dir.getStepY() != 0)
                    iy = y + 0.5F + dir.getStepY() * 0.5 + rand.nextDouble() * 0.125 * dir.getStepY();
                if (dir.getStepZ() != 0)
                    iz = z + 0.5F + dir.getStepZ() * 0.5 + rand.nextDouble() * 0.125 * dir.getStepZ();

                world.addParticle(net.minecraft.core.particles.ParticleTypes.BUBBLE, ix, iy, iz, 0.0, 0.2, 0.0);
            }
        }
    }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachineWasteDrumBlockEntity(pos, state);
    }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.MACHINE_WASTE_DRUM_BE.get(),
                (lvl, pos, st, be) -> MachineWasteDrumBlockEntity.tick(lvl, pos, st, (MachineWasteDrumBlockEntity) be));
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
    /*public static final com.mojang.serialization.MapCodec<MachineWasteDrumBlock> CODEC = simpleCodec(MachineWasteDrumBlock::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return CODEC;
    }
    *///?}
}
