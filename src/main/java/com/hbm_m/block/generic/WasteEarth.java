package com.hbm_m.block.generic;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.config.ModClothConfig;
import com.hbm_m.effect.ModEffects;
import com.hbm_m.particle.ModParticleTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.MushroomBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.IPlantable;
import net.minecraftforge.common.PlantType;

/**
 * 1:1 {@link com.hbm.blocks.generic.WasteEarth} ({@code waste_earth}, {@code waste_mycelium}, {@code burning_earth},
 * {@code frozen_grass}): Effekte beim Betreten, Partikel, Myzel-Ausbreitung (Config enableMycelium), Brand-Erde
 * (steckt Gras/Myzel in Brand, frisst Laub und Pflanzen, taut Frosterde, wird danach zu {@code impact_dirt}), Rueckfall
 * zu Erde im Dunkeln (bzw. immer mit cleanupDeadDirt), Pilze werden zu {@code mush}. Nur Hoehlenpflanzen wachsen.
 * Die Drops (Erde bzw. Schneeball) stehen in der Loot-Tabelle.
 */
public class WasteEarth extends Block {

    private final boolean tick;

    public WasteEarth(Properties properties) {
        this(properties, true);
    }

    public WasteEarth(Properties properties, boolean tick) {
        super(properties);
        this.tick = tick;
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return tick;
    }

    @Override
    public void stepOn(Level world, BlockPos pos, BlockState state, Entity entity) {
        super.stepOn(world, pos, state, entity);
        if (entity instanceof LivingEntity living) {
            if (this == ModBlocks.FROZEN_GRASS.get()) {
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 2 * 60 * 20, 2));
            }
            if (this == ModBlocks.WASTE_MYCELIUM.get()) {
                living.addEffect(new MobEffectInstance(ModEffects.RADIATION.get(), 30 * 20, 3));
            }
            if (this == ModBlocks.BURNING_EARTH.get()) {
                living.setSecondsOnFire(5);
            }
        }
    }

    @Override
    public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource rand) {
        super.animateTick(state, world, pos, rand);
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();
        if (this == ModBlocks.WASTE_MYCELIUM.get()) {
            world.addParticle(ModParticleTypes.TOWNAURA.get(), x + rand.nextFloat(), y + 1.1F, z + rand.nextFloat(), 0.0D, 0.0D, 0.0D);
        }
        if (this == ModBlocks.BURNING_EARTH.get()) {
            world.addParticle(ParticleTypes.FLAME, x + rand.nextFloat(), y + 1.1F, z + rand.nextFloat(), 0.0D, 0.0D, 0.0D);
            world.addParticle(ParticleTypes.SMOKE, x + rand.nextFloat(), y + 1.1F, z + rand.nextFloat(), 0.0D, 0.0D, 0.0D);
        }
    }

    private static boolean opaque(Level world, BlockPos pos) {
        return world.getBlockState(pos).isSolidRender(world, pos);
    }

    @Override
    public void randomTick(BlockState state, ServerLevel world, BlockPos pos, RandomSource rand) {
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();

        if (this == ModBlocks.WASTE_MYCELIUM.get() && ModClothConfig.get().enableMycelium) {
            for (int i = -1; i < 2; i++) {
                for (int j = -1; j < 2; j++) {
                    for (int k = -1; k < 2; k++) {
                        BlockPos p = new BlockPos(x + i, y + j, z + k);
                        Block b0 = world.getBlockState(p).getBlock();
                        if (!opaque(world, p.above()) && (b0 == Blocks.DIRT || b0 == Blocks.GRASS_BLOCK || b0 == Blocks.MYCELIUM || b0 == ModBlocks.WASTE_EARTH.get())) {
                            world.setBlock(p, ModBlocks.WASTE_MYCELIUM.get().defaultBlockState(), 3);
                        }
                    }
                }
            }
        }

        if (this == ModBlocks.BURNING_EARTH.get()) {
            if (rand.nextInt(5) == 0) {
                for (int i = -1; i < 2; i++) {
                    for (int j = -1; j < 2; j++) {
                        for (int k = -1; k < 2; k++) {
                            BlockPos p = new BlockPos(x + i, y + j, z + k);
                            if (!world.isLoaded(p)) continue;
                            BlockState s0 = world.getBlockState(p);
                            BlockState s1 = world.getBlockState(p.above());
                            Block b0 = s0.getBlock();
                            Block b1 = s1.getBlock();

                            if (!opaque(world, p.above()) &&
                                    ((b0 == Blocks.GRASS_BLOCK || b0 == Blocks.MYCELIUM || b0 == ModBlocks.WASTE_EARTH.get() ||
                                    b0 == ModBlocks.FROZEN_GRASS.get() || b0 == ModBlocks.WASTE_MYCELIUM.get())
                                    && !world.isRainingAt(pos))) {
                                world.setBlock(p, ModBlocks.BURNING_EARTH.get().defaultBlockState(), 3);
                            }
                            if (b0 instanceof LeavesBlock || b0 instanceof BushBlock) {
                                world.removeBlock(p, false);
                            }
                            if (b0 == ModBlocks.FROZEN_DIRT.get()) {
                                world.setBlock(p, Blocks.DIRT.defaultBlockState(), 3);
                            }
                            // Original prueft die Entflammbarkeit am eigenen Ort (x, y, z) nach oben
                            if (s1.isFlammable(world, pos, Direction.UP) && !(b1 instanceof LeavesBlock || b1 instanceof BushBlock) && world.getBlockState(pos.above()).isAir()) {
                                world.setBlock(pos.above(), BaseFireBlock.getState(world, pos.above()), 3);
                            }
                        }
                    }
                }
            }
            world.setBlock(pos, ModBlocks.IMPACT_DIRT.get().defaultBlockState(), 3);
        }

        if (this == ModBlocks.WASTE_EARTH.get() || this == ModBlocks.WASTE_MYCELIUM.get()) {
            if (ModClothConfig.get().cleanupDeadDirt || (world.getMaxLocalRawBrightness(pos.above()) < 4 && world.getBlockState(pos.above()).getLightBlock(world, pos.above()) > 2)) {
                world.setBlock(pos, Blocks.DIRT.defaultBlockState(), 3);
            }
            if (world.getBlockState(pos.above()).getBlock() instanceof MushroomBlock) {
                world.setBlock(pos.above(), ModBlocks.MUSH.get().defaultBlockState(), 3);
            }
        }
    }

    @Override
    public void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, world, pos, block, fromPos, isMoving);
        if (this == ModBlocks.BURNING_EARTH.get() && !world.isClientSide) {
            BlockState above = world.getBlockState(pos.above());
            if (!above.getFluidState().isEmpty() || above.isRedstoneConductor(world, pos.above())) {
                world.setBlock(pos, Blocks.DIRT.defaultBlockState(), 3);
            }
        }
    }

    @Override
    public boolean canSustainPlant(BlockState state, BlockGetter world, BlockPos pos, Direction facing, IPlantable plantable) {
        if (this == ModBlocks.WASTE_EARTH.get() || this == ModBlocks.WASTE_MYCELIUM.get()) {
            return plantable.getPlantType(world, pos.relative(facing)) == PlantType.CAVE;
        }
        return false;
    }
}
