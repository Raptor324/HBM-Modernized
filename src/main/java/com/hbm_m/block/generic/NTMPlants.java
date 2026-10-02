package com.hbm_m.block.generic;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.item.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 1:1 {@code BlockNTMFlower} ({@code plant_flower}), {@code BlockTallPlant} ({@code plant_tall}), {@code BlockReeds}
 * ({@code plant_reeds}), {@code BlockStalagmite}, {@code BlockResourceStone} und {@code BlockNoSpawn}. Die Metadaten-Arten
 * sind im Port je eigene Bloecke ({@code plant_flower_<art>}, {@code plant_tall_<art>}, {@code stalagmite_<art>} ...).
 */
public final class NTMPlants {

    private NTMPlants() {}

    /** Original {@code canPlaceBlockOn}: Gras, Erde (inkl. grober Erde/Podsol), Ackerland, tote und oelige Erde. */
    static boolean isPlantSoil(BlockState s) {
        return s.is(Blocks.GRASS_BLOCK) || s.is(Blocks.DIRT) || s.is(Blocks.COARSE_DIRT) || s.is(Blocks.PODZOL) || s.is(Blocks.FARMLAND)
                || s.is(ModBlocks.DIRT_DEAD.get()) || s.is(ModBlocks.DIRT_OILY.get());
    }

    static boolean isDeadSoil(BlockState s) {
        return s.is(ModBlocks.DIRT_DEAD.get()) || s.is(ModBlocks.DIRT_OILY.get());
    }

    /** Cadmiumweiden wachsen nur mit Wasser neben dem Boden. */
    static boolean waterNearSoil(LevelReader world, BlockPos soilPos) {
        for (Direction d : Direction.Plane.HORIZONTAL) {
            if (world.getFluidState(soilPos.relative(d)).is(FluidTags.WATER) || world.getBlockState(soilPos.relative(d)).is(Blocks.WATER)) return true;
        }
        return false;
    }

    // ═════════════════════════════════════════════════════════════════════════════════════════
    //  plant_flower
    // ═════════════════════════════════════════════════════════════════════════════════════════

    public enum FlowerType {
        FOXGLOVE(false), TOBACCO(false), NIGHTSHADE(false), WEED(false), CD0(true), CD1(true);
        public final boolean needsOil;
        FlowerType(boolean needsOil) { this.needsOil = needsOil; }

        /** Original {@code getRenderColor}/{@code colorMultiplier}: Tabak und Hanf sind laubfarben getoent. */
        public boolean tinted() { return this == TOBACCO || this == WEED; }
    }

    public static class Flower extends BushBlock implements BonemealableBlock {
        public final FlowerType type;

        public Flower(Properties p, FlowerType type) {
            super(p.randomTicks());
            this.type = type;
        }

        @Override protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) { return isPlantSoil(state); }

        @Override
        public void randomTick(BlockState state, ServerLevel world, BlockPos pos, RandomSource rand) {
            if (!(type == FlowerType.WEED || type == FlowerType.CD0 || type == FlowerType.CD1)) return;
            if (isValidBonemealTarget(world, pos, state, false) && isBonemealSuccess(world, rand, pos, state) && rand.nextInt(3) == 0) {
                performBonemeal(world, rand, pos, state);
            }
        }

        /* Wachstumsbedingung */
        @Override
        public boolean isValidBonemealTarget(LevelReader world, BlockPos pos, BlockState state, boolean client) {
            if (type == FlowerType.CD0 || type == FlowerType.CD1) {
                if (!waterNearSoil(world, pos.below())) return false;
            }
            if (type == FlowerType.WEED || type == FlowerType.CD1) return world.isEmptyBlock(pos.above());
            return true;
        }

        /* Zufall */
        @Override
        public boolean isBonemealSuccess(Level world, RandomSource rand, BlockPos pos, BlockState state) {
            if (type == FlowerType.WEED || type == FlowerType.CD0 || type == FlowerType.CD1) return rand.nextFloat() < 0.33F;
            return true;
        }

        /* Wachsen */
        @Override
        public void performBonemeal(ServerLevel world, RandomSource rand, BlockPos pos, BlockState state) {
            BlockState onTop = world.getBlockState(pos.below());

            if (type == FlowerType.WEED && isDeadSoil(onTop)) {
                world.setBlock(pos, ModBlocks.PLANT_DEAD.get().defaultBlockState(), 3);
                return;
            }
            if (type == FlowerType.WEED) {
                TallPlant.placeBoth(world, pos, ModBlocks.PLANT_TALL_WEED.get());
                return;
            }
            if (type == FlowerType.CD0) {
                world.setBlock(pos, ModBlocks.PLANT_FLOWER_CD1.get().defaultBlockState(), 3);
                return;
            }
            if (type == FlowerType.CD1) {
                TallPlant.placeBoth(world, pos, ModBlocks.PLANT_TALL_CD2.get());
                return;
            }
            // uebrige Blumen: Knochenmehl verdoppelt sie
            Block.popResource(world, pos, new ItemStack(this));
        }
    }

    // ═════════════════════════════════════════════════════════════════════════════════════════
    //  plant_tall
    // ═════════════════════════════════════════════════════════════════════════════════════════

    public enum TallType {
        WEED(false), CD2(true), CD3(true), CD4(true);
        public final boolean needsOil;
        TallType(boolean needsOil) { this.needsOil = needsOil; }
    }

    public static class TallPlant extends Block implements BonemealableBlock {
        public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
        private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 16, 14);
        /** Original {@code detectCut}: waehrend des Wachsens nicht in Blumen zurueckfallen. */
        public static boolean detectCut = true;

        public final TallType type;

        public TallPlant(Properties p, TallType type) {
            super(p.randomTicks());
            this.type = type;
            registerDefaultState(stateDefinition.any().setValue(HALF, DoubleBlockHalf.LOWER));
        }

        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(HALF); }
        @Override public VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return SHAPE; }
        @Override public VoxelShape getCollisionShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return Shapes.empty(); }

        public static void placeBoth(Level world, BlockPos pos, Block block) {
            detectCut = false;
            world.setBlock(pos, block.defaultBlockState().setValue(HALF, DoubleBlockHalf.LOWER), 3);
            world.setBlock(pos.above(), block.defaultBlockState().setValue(HALF, DoubleBlockHalf.UPPER), 3);
            detectCut = true;
        }

        @Nullable
        @Override
        public BlockState getStateForPlacement(BlockPlaceContext ctx) {
            BlockPos pos = ctx.getClickedPos();
            if (!ctx.getLevel().isEmptyBlock(pos.above())) return null;
            if (!isPlantSoil(ctx.getLevel().getBlockState(pos.below()))) return null;
            return defaultBlockState();
        }

        @Override
        public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
            world.setBlock(pos.above(), defaultBlockState().setValue(HALF, DoubleBlockHalf.UPPER), 2);
        }

        @Override
        public boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
            if (state.getValue(HALF) == DoubleBlockHalf.UPPER) {
                BlockState below = world.getBlockState(pos.below());
                return below.is(this) && below.getValue(HALF) == DoubleBlockHalf.LOWER;
            }
            return isPlantSoil(world.getBlockState(pos.below()));
        }

        @Override
        public void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos from, boolean moving) {
            if (world.isClientSide) return;
            if (!canSurvive(state, world, pos)) {
                if (state.getValue(HALF) == DoubleBlockHalf.LOWER) Block.dropResources(state, world, pos);
                world.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                return;
            }
            if (!detectCut) return;
            if (state.getValue(HALF) == DoubleBlockHalf.LOWER) {
                BlockState above = world.getBlockState(pos.above());
                boolean hasTop = above.is(this) && above.getValue(HALF) == DoubleBlockHalf.UPPER;
                if (!hasTop && isPlantSoil(world.getBlockState(pos.below()))) {
                    world.setBlock(pos, (type == TallType.WEED ? ModBlocks.PLANT_FLOWER_WEED : ModBlocks.PLANT_FLOWER_CD0).get().defaultBlockState(), 3);
                }
            }
        }

        /** Original {@code onBlockHarvested}: die untere Haelfte nimmt die obere mit. */
        @Override
        public void playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
            if (!world.isClientSide && state.getValue(HALF) == DoubleBlockHalf.LOWER) {
                BlockState above = world.getBlockState(pos.above());
                if (above.is(this)) {
                    if (!player.isCreative()) Block.dropResources(above, world, pos.above());
                    detectCut = false;
                    world.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), 2 | 16);
                    detectCut = true;
                }
            }
            super.playerWillDestroy(world, pos, state, player);
        }

        /** Original: jede Haelfte gibt eine Blume (Hanf bzw. Cadmiumweide); die oberste Weide zusaetzlich 3-6 Blaetter. */
        @Override
        public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
            List<ItemStack> ret = new ArrayList<>();
            ret.add(new ItemStack((type == TallType.WEED ? ModBlocks.PLANT_FLOWER_WEED : ModBlocks.PLANT_FLOWER_CD0).get()));
            if (type == TallType.CD4 && state.getValue(HALF) == DoubleBlockHalf.UPPER) {
                ServerLevel level = builder.getLevel();
                ret.add(new ItemStack(com.hbm_m.item.PartTabMetaItems.get("plant_item_mustardwillow").get(), 3 + level.random.nextInt(4)));
            }
            return ret;
        }

        @Override
        public void randomTick(BlockState state, ServerLevel world, BlockPos pos, RandomSource rand) {
            if (state.getValue(HALF) == DoubleBlockHalf.UPPER) return;

            BlockState onTop = world.getBlockState(pos.below());
            if (!type.needsOil && isDeadSoil(onTop)) {
                world.setBlock(pos, ModBlocks.PLANT_DEAD_BIGFLOWER.get().defaultBlockState(), 3);
                return;
            }

            if (isValidBonemealTarget(world, pos, state, false) && isBonemealSuccess(world, rand, pos, state) && rand.nextInt(3) == 0) {
                performBonemeal(world, rand, pos, state);
            }
        }

        private BlockPos lower(BlockPos pos, BlockState state) {
            return state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
        }

        /* Wachstumsbedingung */
        @Override
        public boolean isValidBonemealTarget(LevelReader world, BlockPos pos, BlockState state, boolean client) {
            BlockPos base = lower(pos, state);
            if (type == TallType.CD2 || type == TallType.CD3) {
                if (!waterNearSoil(world, base.below())) return false;
            }
            if (type == TallType.CD3) return isDeadSoil(world.getBlockState(base.below()));
            return type != TallType.CD4 && type != TallType.WEED;
        }

        /* Zufall */
        @Override
        public boolean isBonemealSuccess(Level world, RandomSource rand, BlockPos pos, BlockState state) {
            if (type == TallType.CD3) return true;
            return rand.nextFloat() < 0.33F;
        }

        /* Wachsen: CD2 -> CD3 -> CD4; CD3 verwandelt dabei den Boden zu Erde */
        @Override
        public void performBonemeal(ServerLevel world, RandomSource rand, BlockPos pos, BlockState state) {
            if (type != TallType.CD2 && type != TallType.CD3) return;
            BlockPos base = lower(pos, state);
            Block next = type == TallType.CD2 ? ModBlocks.PLANT_TALL_CD3.get() : ModBlocks.PLANT_TALL_CD4.get();
            placeBoth(world, base, next);
            if (type == TallType.CD3) world.setBlock(base.below(), Blocks.DIRT.defaultBlockState(), 3);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════════════════════
    //  plant_reeds
    // ═════════════════════════════════════════════════════════════════════════════════════════

    /** 1:1 {@code BlockReeds}: steht auf Wasser, faellt ohne Drop ab, gibt beim Abbauen einen Stock. */
    public static class Reeds extends Block {
        public Reeds(Properties p) { super(p); }

        @Override
        public boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
            BlockState below = world.getBlockState(pos.below());
            return below.is(Blocks.WATER);
        }

        @Nullable
        @Override
        public BlockState getStateForPlacement(BlockPlaceContext ctx) {
            return canSurvive(defaultBlockState(), ctx.getLevel(), ctx.getClickedPos()) ? defaultBlockState() : null;
        }

        @Override
        public void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos from, boolean moving) {
            if (!world.isClientSide && !canSurvive(state, world, pos)) world.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        }

        @Override public VoxelShape getCollisionShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return Shapes.empty(); }

        @Override
        public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
            return List.of(new ItemStack(Items.STICK));
        }
    }

    // ═════════════════════════════════════════════════════════════════════════════════════════
    //  stalagmite / stalactite
    // ═════════════════════════════════════════════════════════════════════════════════════════

    public enum StalagmiteType { SULFUR, ASBESTOS, ICE, SNOW, GLYPHID1, GLYPHID2, GLYPHID3 }

    /** 1:1 {@code BlockStalagmite}: haengt/steht an festem Untergrund, keine Kollision, eigener Drop je Art. */
    public static class Stalagmite extends Block {
        public final StalagmiteType type;
        public final boolean ceiling;

        public Stalagmite(Properties p, StalagmiteType type, boolean ceiling) {
            super(p);
            this.type = type;
            this.ceiling = ceiling;
        }

        @Override
        public boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
            if (ceiling) return world.getBlockState(pos.above()).isFaceSturdy(world, pos.above(), Direction.DOWN);
            return world.getBlockState(pos.below()).isFaceSturdy(world, pos.below(), Direction.UP);
        }

        @Nullable
        @Override
        public BlockState getStateForPlacement(BlockPlaceContext ctx) {
            return canSurvive(defaultBlockState(), ctx.getLevel(), ctx.getClickedPos()) ? defaultBlockState() : null;
        }

        @Override
        public void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos from, boolean moving) {
            if (!world.isClientSide && !canSurvive(state, world, pos)) world.destroyBlock(pos, true);
        }

        @Override public VoxelShape getCollisionShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return Shapes.empty(); }

        @Override
        public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
            return switch (type) {
                case SULFUR -> List.of(new ItemStack(ModItems.SULFUR.get()));
                case ASBESTOS -> List.of(new ItemStack(com.hbm_m.item.material.ModMaterialItems.item(com.hbm_m.item.material.ModMaterials.ASBESTOS, com.hbm_m.item.material.MaterialShape.POWDER)));
                case ICE -> List.of(new ItemStack(ModItems.POWDER_ICE.get()));
                case SNOW -> List.of(new ItemStack(Items.SNOWBALL));
                default -> List.of();
            };
        }
    }

    // ═════════════════════════════════════════════════════════════════════════════════════════
    //  stone_resource
    // ═════════════════════════════════════════════════════════════════════════════════════════

    public enum StoneType { SULFUR, ASBESTOS, HEMATITE, MALACHITE, LIMESTONE, BAUXITE }

    /** 1:1 {@code BlockResourceStone}: Asbestgestein setzt beim Abbau Asbestgas frei, Malachit gibt 3+ Brocken. */
    public static class ResourceStone extends Block {
        public final StoneType type;

        public ResourceStone(Properties p, StoneType type) {
            super(p);
            this.type = type;
        }

        @Override
        public void spawnAfterBreak(BlockState state, ServerLevel world, BlockPos pos, ItemStack tool, boolean dropXp) {
            super.spawnAfterBreak(state, world, pos, tool, dropXp);
            if (type == StoneType.ASBESTOS) world.setBlock(pos, ModBlocks.GAS_ASBESTOS.get().defaultBlockState(), 3);
        }

        @Override
        public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
            if (type == StoneType.MALACHITE) {
                ItemStack tool = builder.getOptionalParameter(LootContextParams.TOOL);
                int fortune = tool == null ? 0 : EnchantmentHelper.getItemEnchantmentLevel(Enchantments.BLOCK_FORTUNE, tool);
                return List.of(new ItemStack(ModItems.MALACHITE_CHUNK.get(), 3 + fortune + builder.getLevel().random.nextInt(fortune + 2)));
            }
            return List.of(new ItemStack(this));
        }
    }

    // ═════════════════════════════════════════════════════════════════════════════════════════
    //  ducrete
    // ═════════════════════════════════════════════════════════════════════════════════════════

    /** 1:1 {@code BlockNoSpawn}: nichts spawnt darauf; roter Hinweis im Tooltip. */
    public static class NoSpawn extends Block {
        public NoSpawn(Properties p) { super(p.isValidSpawn((s, l, pos, e) -> false)); }

        @Override
        public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> list, TooltipFlag flag) {
            list.add(Component.translatable("tile.nospawn").withStyle(net.minecraft.ChatFormatting.RED));
        }
    }

}
