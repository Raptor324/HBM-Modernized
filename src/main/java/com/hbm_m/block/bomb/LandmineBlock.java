package com.hbm_m.block.bomb;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.bomb.IBomb;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.bomb.LandMineBlockEntity;
import com.hbm_m.explosion.ExplosionNukeGeneric;
import com.hbm_m.explosion.vanillant.ExplosionVNT;
import com.hbm_m.explosion.vanillant.standard.BlockAllocatorStandard;
import com.hbm_m.explosion.vanillant.standard.BlockAllocatorWater;
import com.hbm_m.explosion.vanillant.standard.BlockProcessorStandard;
import com.hbm_m.explosion.vanillant.standard.EntityProcessorCrossSmooth;
import com.hbm_m.explosion.vanillant.standard.ExplosionEffectStandard;
import com.hbm_m.network.AuxParticlePacket;
import com.hbm_m.network.ModPacketHandler;
import com.hbm_m.sound.ModSounds;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class LandmineBlock extends Block implements EntityBlock, IBomb {

    private static final VoxelShape SHAPE_AP = Block.box(5, 0, 5, 11, 1, 11);
    private static final VoxelShape SHAPE_FAT = Block.box(5, 0, 4, 11, 6, 12);

    private static final float MINE_AP_DAMAGE = 10.0F;
    private static final float MINE_NAVAL_DAMAGE = 60.0F;
    private static final float MINE_NUKE_DAMAGE = 100.0F;

    public static boolean safeMode = false;

    public final double range;
    public final double height;

    public static final DirectionProperty FACING = DirectionProperty.create("facing", Direction.Plane.HORIZONTAL);

    public LandmineBlock(Properties properties, double range, double height) {
        super(properties);
        this.range = range;
        this.height = height;
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    //? if < 1.21.1 {
    @Override
    public void appendHoverText(ItemStack stack,
                                 @Nullable BlockGetter level,
                                 List<Component> tooltip,
                                 TooltipFlag flag) {
        if (this == ModBlocks.MINE_FAT.get()) {
            tooltip.add(Component.translatable("tooltip.hbm_m.mine_nuke.line1").withStyle(ChatFormatting.DARK_RED));
            tooltip.add(Component.translatable("tooltip.hbm_m.mine_nuke.line2").withStyle(ChatFormatting.RED));
            tooltip.add(Component.translatable("tooltip.hbm_m.mine_nuke.line3").withStyle(ChatFormatting.GRAY));
        } else if (this == ModBlocks.NAVAL_MINE.get()) {
            tooltip.add(Component.translatable("tooltip.hbm_m.naval_mine.line1").withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable("tooltip.hbm_m.mine.line1").withStyle(ChatFormatting.GRAY));
        }
    }
    //?} else {
    /*@Override
    public void appendHoverText(ItemStack stack,
                                 net.minecraft.world.item.Item.TooltipContext level,
                                 List<Component> tooltip,
                                 TooltipFlag flag) {
        if (this == ModBlocks.MINE_FAT.get()) {
            tooltip.add(Component.translatable("tooltip.hbm_m.mine_nuke.line1").withStyle(ChatFormatting.DARK_RED));
            tooltip.add(Component.translatable("tooltip.hbm_m.mine_nuke.line2").withStyle(ChatFormatting.RED));
            tooltip.add(Component.translatable("tooltip.hbm_m.mine_nuke.line3").withStyle(ChatFormatting.GRAY));
        } else if (this == ModBlocks.NAVAL_MINE.get()) {
            tooltip.add(Component.translatable("tooltip.hbm_m.naval_mine.line1").withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable("tooltip.hbm_m.mine.line1").withStyle(ChatFormatting.GRAY));
        }
    }
    *///?}

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (this == ModBlocks.MINE_AP.get()) return SHAPE_AP;
        if (this == ModBlocks.MINE_FAT.get()) return SHAPE_FAT;
        return Shapes.block();
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (this == ModBlocks.NAVAL_MINE.get()) return true;
        return hasSupport(level, pos);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        if (level.isClientSide) return;

        if (level.hasNeighborSignal(pos)) {
            explode(level, pos);
            return;
        }

        if (this != ModBlocks.NAVAL_MINE.get() && !hasSupport(level, pos)) {
            if (!safeMode) {
                explode(level, pos);
            } else {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            }
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!level.isClientSide && !state.is(newState.getBlock()) && !safeMode) {
            explode(level, pos);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    private static boolean hasSupport(LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        BlockState belowState = level.getBlockState(below);
        return belowState.isFaceSturdy(level, below, Direction.UP)
                || belowState.getBlock() instanceof FenceBlock;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LandMineBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : (lvl, pos, st, te) -> {
            if (te instanceof LandMineBlockEntity landmine) {
                LandMineBlockEntity.tick(lvl, pos, st, landmine);
            }
        };
    }

    @Override
    public BombReturnCode explode(Level level, BlockPos pos) {
        if (level.isClientSide) return BombReturnCode.UNDEFINED;

        safeMode = true;
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        safeMode = false;

        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;

        if (this == ModBlocks.MINE_AP.get()) {
            new ExplosionVNT(level, x, y, z, 3F)
                    .setEntityProcessor(new EntityProcessorCrossSmooth(0.5, MINE_AP_DAMAGE).setupPiercing(5F, 0.2F))
                    .setSFX(new ExplosionEffectStandard())
                    .explode();
        } else if (this == ModBlocks.MINE_FAT.get()) {
            new ExplosionVNT(level, x, y, z, 10F)
                    .setBlockAllocator(new BlockAllocatorStandard(64))
                    .setBlockProcessor(new BlockProcessorStandard())
                    .setEntityProcessor(new EntityProcessorCrossSmooth(2, MINE_NUKE_DAMAGE).withRangeMod(1.5F))
                    .setSFX(new ExplosionEffectStandard())
                    .explode();

            ExplosionNukeGeneric.incrementRad(level, x, y, z, 1.5F);

            if (level instanceof ServerLevel serverLevel) {
                CompoundTag data = new CompoundTag();
                data.putString("type", "muke");
                data.putBoolean("balefire", level.random.nextInt(100) == 0);
                ModPacketHandler.sendToPlayersNear(serverLevel, x, y, z, 250.0D,
                        ModPacketHandler.AUX_PARTICLE, new AuxParticlePacket(data, x, y, z));
            }

            ModSounds.MUKE_EXPLOSION.ifPresent(sound -> level.playSound(
                    null, x, y, z, sound, SoundSource.BLOCKS, 25.0F, 0.9F));
        } else if (this == ModBlocks.NAVAL_MINE.get()) {
            new ExplosionVNT(level, x + 5, y + 5, z + 5, 25F)
                    .setBlockAllocator(new BlockAllocatorWater(32))
                    .setBlockProcessor(new BlockProcessorStandard())
                    .setEntityProcessor(new EntityProcessorCrossSmooth(0.5, MINE_NAVAL_DAMAGE).setupPiercing(5F, 0.2F))
                    .setSFX(new ExplosionEffectStandard())
                    .explode();

            ModSounds.EXPLOSION_LARGE_NEAR.ifPresent(sound -> level.playSound(
                    null, x, y, z, sound, SoundSource.BLOCKS, 10.0F, 1.0F));
        }

        return BombReturnCode.DETONATED;
    }

    public boolean isWaterAbove(Level level, BlockPos pos) {
        for (int xo = -1; xo <= 1; xo++) {
            for (int zo = -1; zo <= 1; zo++) {
                if (level.getFluidState(pos.offset(xo, 1, zo)).is(FluidTags.WATER)) {
                    return true;
                }
            }
        }
        return false;
    }
}
