package com.hbm_m.block.bomb;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.bomb.IBomb;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.bomb.LandMineBlockEntity;
import com.hbm_m.config.ModClothConfig;
import com.hbm_m.explosion.ExplosionLarge;
import com.hbm_m.explosion.ExplosionNukeGeneric;
import com.hbm_m.explosion.vanillant.ExplosionVNT;
import com.hbm_m.explosion.vanillant.standard.BlockAllocatorStandard;
import com.hbm_m.explosion.vanillant.standard.BlockAllocatorWater;
import com.hbm_m.explosion.vanillant.standard.BlockProcessorStandard;
import com.hbm_m.explosion.vanillant.standard.EntityProcessorCrossSmooth;
import com.hbm_m.explosion.vanillant.standard.ExplosionEffectWeapon;
import com.hbm_m.explosion.vanillant.standard.PlayerProcessorStandard;
import com.hbm_m.item.ModItems;
import com.hbm_m.main.Polaroid;
import com.hbm_m.particle.helper.IParticleCreator;
import com.hbm_m.sound.HbmSoundsNT;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 1:1 {@code com.hbm.blocks.bomb.Landmine}: mine_ap, mine_he, mine_shrap, mine_fat, mine_naval.
 * Ausloeser ({@code range}/{@code height}) im {@link LandMineBlockEntity}; Redstone, fehlender Untergrund
 * und Abbauen durch Spieler zuenden; mit dem Entschaerfer wird die Mine aufgehoben.
 */
public class LandmineBlock extends Block implements EntityBlock, IBomb {

    private static final VoxelShape SHAPE_AP = Block.box(5, 0, 5, 11, 1, 11);
    private static final VoxelShape SHAPE_HE = Block.box(4, 0, 4, 12, 2, 12);
    private static final VoxelShape SHAPE_FAT = Block.box(5, 0, 4, 11, 6, 12);

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

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (this == ModBlocks.MINE_AP.get() || this == ModBlocks.MINE_SHRAP.get()) return SHAPE_AP;
        if (this == ModBlocks.MINE_HE.get()) return SHAPE_HE;
        if (this == ModBlocks.MINE_FAT.get()) return SHAPE_FAT;
        return Shapes.block();
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return hasSupport(level, pos);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        if (level.isClientSide) return;

        if (level.hasNeighborSignal(pos)) {
            explode(level, pos);
        }

        if (!hasSupport(level, pos)) {
            if (!safeMode) {
                explode(level, pos);
            } else {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            }
        }
    }

    /** Original {@code onBlockDestroyedByPlayer}. */
    @Override
    public void destroy(LevelAccessor level, BlockPos pos, BlockState state) {
        if (!safeMode && level instanceof Level world) {
            explode(world, pos);
        }
        super.destroy(level, pos, state);
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
    //?} else {
    /*@Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack hbmHeld, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return com.hbm_m.platform.BlockUseHooks.item(hbmUse(state, world, pos, player, hand, hit));
    }
    private InteractionResult hbmUse(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
    *///?}
        ItemStack held = player.getItemInHand(hand);
        if (!held.isEmpty() && held.is(ModItems.DEFUSER.get())) {
            safeMode = true;
            world.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);

            ItemStack itemstack = new ItemStack(this, 1);
            float f = world.random.nextFloat() * 0.6F + 0.2F;
            float f1 = world.random.nextFloat() * 0.2F;
            float f2 = world.random.nextFloat() * 0.6F + 0.2F;

            ItemEntity entityitem = new ItemEntity(world, pos.getX() + f, pos.getY() + f1 + 1, pos.getZ() + f2, itemstack);

            float f3 = 0.05F;
            entityitem.setDeltaMovement((float) world.random.nextGaussian() * f3, (float) world.random.nextGaussian() * f3 + 0.2F, (float) world.random.nextGaussian() * f3);

            if (!world.isClientSide)
                world.addFreshEntity(entityitem);

            safeMode = false;
            return InteractionResult.sidedSuccess(world.isClientSide);
        }
        return InteractionResult.PASS;
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

    @Override
    public BombReturnCode explode(Level world, BlockPos pos) {
        if (!world.isClientSide) {
            int x = pos.getX();
            int y = pos.getY();
            int z = pos.getZ();
            ModClothConfig cfg = ModClothConfig.get();

            LandmineBlock.safeMode = true;
            world.destroyBlock(pos, false);
            LandmineBlock.safeMode = false;

            if (this == ModBlocks.MINE_AP.get()) {
                ExplosionVNT vnt = new ExplosionVNT(world, x + 0.5, y + 0.5, z + 0.5, 3F);
                vnt.setEntityProcessor(new EntityProcessorCrossSmooth(0.5, cfg.mineApDamage).setupPiercing(5F, 0.2F));
                vnt.setPlayerProcessor(new PlayerProcessorStandard());
                vnt.setSFX(new ExplosionEffectWeapon(5, 1F, 0.5F));
                vnt.explode();
            } else if (this == ModBlocks.MINE_HE.get()) {
                ExplosionVNT vnt = new ExplosionVNT(world, x + 0.5, y + 0.5, z + 0.5, 4F);
                vnt.setBlockAllocator(new BlockAllocatorStandard());
                vnt.setBlockProcessor(new BlockProcessorStandard());
                vnt.setEntityProcessor(new EntityProcessorCrossSmooth(1, cfg.mineHeDamage).setupPiercing(15F, 0.2F));
                vnt.setPlayerProcessor(new PlayerProcessorStandard());
                vnt.setSFX(new ExplosionEffectWeapon(15, 3.5F, 1.25F));
                vnt.explode();
            } else if (this == ModBlocks.MINE_SHRAP.get()) {
                ExplosionVNT vnt = new ExplosionVNT(world, x + 0.5, y + 0.5, z + 0.5, 3F);
                vnt.setEntityProcessor(new EntityProcessorCrossSmooth(0.5, cfg.mineShrapDamage));
                vnt.setPlayerProcessor(new PlayerProcessorStandard());
                vnt.setSFX(new ExplosionEffectWeapon(5, 1F, 0.5F));
                vnt.explode();

                ExplosionLarge.spawnShrapnelShower(world, x + 0.5, y + 0.5, z + 0.5, 0, 1D, 0, 45, 0.2D);
                ExplosionLarge.spawnShrapnels(world, x + 0.5, y + 0.5, z + 0.5, 5);
            } else if (this == ModBlocks.MINE_NAVAL.get()) {
                ExplosionVNT vnt = new ExplosionVNT(world, x + 5, y + 5, z + 5, 25F);
                vnt.setBlockAllocator(new BlockAllocatorWater(32));
                vnt.setBlockProcessor(new BlockProcessorStandard());
                vnt.setEntityProcessor(new EntityProcessorCrossSmooth(0.5, cfg.mineNavalDamage).setupPiercing(5F, 0.2F));
                vnt.setPlayerProcessor(new PlayerProcessorStandard());
                vnt.setSFX(new ExplosionEffectWeapon(10, 1F, 0.5F));
                vnt.explode();

                ExplosionLarge.spawnParticlesRadial(world, x + 0.5, y + 2, z + 0.5, 30);
                ExplosionLarge.spawnRubble(world, x + 0.5, y + 0.5, z + 0.5, 5);

                // Only spawn water effects if there's water above the mine
                if (isWaterAbove(world, pos)) {
                    ExplosionLarge.spawnFoam(world, x + 0.5, y + 0.5, z + 0.5, 60);
                }
            } else if (this == ModBlocks.MINE_FAT.get()) {
                ExplosionVNT vnt = new ExplosionVNT(world, x + 0.5, y + 0.5, z + 0.5, 10);
                vnt.setBlockAllocator(new BlockAllocatorStandard(64));
                vnt.setBlockProcessor(new BlockProcessorStandard());
                vnt.setEntityProcessor(new EntityProcessorCrossSmooth(2, cfg.mineNukeDamage).withRangeMod(1.5F));
                vnt.setPlayerProcessor(new PlayerProcessorStandard());
                vnt.explode();

                ExplosionNukeGeneric.incrementRad(world, x, y, z, 1.5F);
                if (world instanceof ServerLevel server) {
                    CompoundTag data = new CompoundTag();
                    data.putString("type", "muke");
                    data.putBoolean("balefire", Polaroid.id() == 11 || world.random.nextInt(100) == 0);
                    IParticleCreator.sendPacket(server, x + 0.5, y + 0.5, z + 0.5, 250, data);
                }
                com.hbm_m.satellite.DetectorEvents.reportEvent(world, com.hbm_m.satellite.DetectorEvents.DURATION_LOW,
                        com.hbm_m.satellite.DetectorEvents.BurstIntensity.LOW, x + 0.5, z + 0.5);
                world.playSound(null, x + 0.5, y + 0.5, z + 0.5, HbmSoundsNT.get("hbm:weapon.mukeExplosion"), SoundSource.BLOCKS, 25.0F, 0.9F); // this has to be the single worst solution ever
            }
        }

        return BombReturnCode.DETONATED;
    }
}
