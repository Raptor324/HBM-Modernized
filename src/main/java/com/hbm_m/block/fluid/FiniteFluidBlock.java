package com.hbm_m.block.fluid;

import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

/**
 * 1:1-Port von Forges {@code BlockFluidFinite} (1.7.10), wie HBM ihn fuer {@code concrete_liquid}
 * (4 Quanten) und {@code corium_block} (5 Quanten) benutzt: die Fluessigkeit hat eine feste Menge,
 * die sich bei jedem Tick auf die niedrigeren Nachbarn verteilt, statt wie Wasser endlos
 * nachzufliessen. {@link #LEVEL} ist das Original-Metadatum ({@code Quanten - 1}).
 */
public class FiniteFluidBlock extends Block {

    public static final IntegerProperty LEVEL = IntegerProperty.create("quanta", 0, 15);

    protected final int quantaPerBlock;
    protected int tickRate;

    public FiniteFluidBlock(Properties properties, int quantaPerBlock, int tickRate) {
        super(properties);
        this.quantaPerBlock = quantaPerBlock;
        this.tickRate = tickRate;
        registerDefaultState(defaultBlockState().setValue(LEVEL, quantaPerBlock - 1));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LEVEL);
    }

    public int getQuantaPerBlock() {
        return quantaPerBlock;
    }

    /** Original {@code onBlockPlaced}: {@code quantaPerBlock - 1}. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(LEVEL, quantaPerBlock - 1);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        level.scheduleTick(pos, this, tickRate);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from, boolean moving) {
        level.scheduleTick(pos, this, tickRate);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        float h = (state.getValue(LEVEL) + 1) / (float) quantaPerBlock;
        return Shapes.box(0, 0, 0, 1, Math.max(0.0625F, h * 0.875F), 1);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Shapes.empty();
    }

    @Override
    public boolean canBeReplaced(BlockState state, BlockPlaceContext ctx) {
        return true;
    }

    @Override
    public List<net.minecraft.world.item.ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        return List.of();
    }

    // ------------------------------------------------------------------ BlockFluidBase

    /** {@code getQuantaValue}: Luft 0, eigener Block Meta+1, sonst -1. */
    protected int getQuantaValue(BlockGetter level, BlockPos pos) {
        BlockState s = level.getBlockState(pos);
        if (s.isAir()) return 0;
        if (s.getBlock() != this) return -1;
        return s.getValue(LEVEL) + 1;
    }

    protected int getQuantaValueBelow(BlockGetter level, BlockPos pos, int belowThis) {
        int quantaRemaining = getQuantaValue(level, pos);
        if (quantaRemaining >= belowThis) return -1;
        return quantaRemaining;
    }

    /** Original {@code canDisplace}: Luft/ersetzbar ja, andere Fluessigkeiten nein. */
    public boolean canDisplace(Level level, BlockPos pos) {
        BlockState s = level.getBlockState(pos);
        if (s.isAir()) return true;
        if (s.getBlock() == this) return false;
        if (!s.getFluidState().isEmpty()) return false;
        return s.canBeReplaced() && !s.blocksMotion();
    }

    public boolean displaceIfPossible(Level level, BlockPos pos) {
        BlockState s = level.getBlockState(pos);
        if (s.isAir()) return true;
        if (s.getBlock() == this) return false;
        if (!canDisplace(level, pos)) return false;
        Block.dropResources(s, level, pos);
        return true;
    }

    // ------------------------------------------------------------------ BlockFluidFinite

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource rand) {
        boolean changed = false;
        int quantaRemaining = state.getValue(LEVEL) + 1;

        int prevRemaining = quantaRemaining;
        quantaRemaining = tryToFlowVerticallyInto(level, pos, quantaRemaining);

        if (quantaRemaining < 1) {
            return;
        } else if (quantaRemaining != prevRemaining) {
            changed = true;
            if (quantaRemaining == 1) {
                setQuanta(level, pos, quantaRemaining);
                return;
            }
        } else if (quantaRemaining == 1) {
            return;
        }

        int lowerthan = quantaRemaining - 1;
        Direction[] dirs = { Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST };
        for (Direction d : dirs) {
            if (displaceIfPossible(level, pos.relative(d))) level.setBlock(pos.relative(d), Blocks.AIR.defaultBlockState(), 3);
        }
        int[] q = new int[4];
        int total = quantaRemaining;
        int count = 1;
        for (int i = 0; i < 4; i++) {
            q[i] = getQuantaValueBelow(level, pos.relative(dirs[i]), lowerthan);
            if (q[i] >= 0) {
                count++;
                total += q[i];
            }
        }

        if (count == 1) {
            if (changed) setQuanta(level, pos, quantaRemaining);
            return;
        }

        int each = total / count;
        int rem = total % count;

        for (int i = 0; i < 4; i++) {
            if (q[i] < 0) continue;
            int newQ = each;
            if (rem == count || rem > 1 && rand.nextInt(count - rem) != 0) {
                ++newQ;
                --rem;
            }
            if (newQ != q[i]) {
                BlockPos p = pos.relative(dirs[i]);
                if (newQ == 0) level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
                else level.setBlock(p, defaultBlockState().setValue(LEVEL, newQ - 1), 2);
                level.scheduleTick(p, this, tickRate);
            }
            --count;
        }

        if (rem > 0) ++each;
        setQuanta(level, pos, each);
    }

    private void setQuanta(Level level, BlockPos pos, int quanta) {
        if (quanta <= 0) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        else level.setBlock(pos, defaultBlockState().setValue(LEVEL, Math.min(15, quanta - 1)), 2);
    }

    public int tryToFlowVerticallyInto(Level level, BlockPos pos, int amtToInput) {
        BlockPos other = pos.below();
        if (other.getY() < level.getMinBuildHeight() || other.getY() >= level.getMaxBuildHeight()) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            return 0;
        }

        int amt = getQuantaValueBelow(level, other, quantaPerBlock);
        if (amt >= 0) {
            amt += amtToInput;
            if (amt > quantaPerBlock) {
                level.setBlock(other, defaultBlockState().setValue(LEVEL, quantaPerBlock - 1), 3);
                level.scheduleTick(other, this, tickRate);
                return amt - quantaPerBlock;
            } else if (amt > 0) {
                level.setBlock(other, defaultBlockState().setValue(LEVEL, amt - 1), 3);
                level.scheduleTick(other, this, tickRate);
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                return 0;
            }
            return amtToInput;
        } else {
            BlockState below = level.getBlockState(other);
            // Dichte "unendlich" = kein Fluid: verdraengen, falls moeglich.
            if (below.getFluidState().isEmpty() && below.getBlock() != this) {
                if (displaceIfPossible(level, other)) {
                    level.setBlock(other, defaultBlockState().setValue(LEVEL, amtToInput - 1), 3);
                    level.scheduleTick(other, this, tickRate);
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                    return 0;
                }
            }
            return amtToInput;
        }
    }

    @Override
    public boolean canHarvestBlock(BlockState state, BlockGetter level, BlockPos pos, Player player) {
        return false;
    }
}
