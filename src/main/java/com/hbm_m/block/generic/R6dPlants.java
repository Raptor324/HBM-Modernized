package com.hbm_m.block.generic;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.config.ModClothConfig;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** {@code BlockHangingVine}, {@code BlockMush}, {@code BlockMushHuge} des Originals. */
public final class R6dPlants {

    private R6dPlants() {}

    /**
     * 1:1 {@code BlockHangingVine} ({@code vine_phosphor}): haengt unter festen Bloecken oder Ranken, bremst und
     * faengt Fallschaden ab. {@code LOOK} ist die Kombination beider Render-Paesse (Rankenform + leuchtende Punkte).
     */
    public static class HangingVine extends Block {
        public enum Look implements StringRepresentable {
            GROUND("ground"), MIDDLE("middle"), HANG("hang"), HANG_OVER("hang_over");
            private final String n;
            Look(String n) { this.n = n; }
            @Override public String getSerializedName() { return n; }
        }
        public static final EnumProperty<Look> LOOK = EnumProperty.create("look", Look.class);

        public HangingVine(Properties p) {
            super(p);
            registerDefaultState(stateDefinition.any().setValue(LOOK, Look.HANG));
        }

        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(LOOK); }

        /** Pass 0: Boden (feste Oberseite darunter) / Mitte (Ranke darunter) / haengend; Pass 1: Luft darunter -> Haenge-Punkte. */
        private BlockState look(BlockGetter w, BlockPos pos, BlockState s) {
            BlockPos d = pos.below();
            BlockState below = w.getBlockState(d);
            Look l;
            if (below.isFaceSturdy(w, d, Direction.UP)) l = Look.GROUND;
            else if (below.is(this)) l = Look.MIDDLE;
            else l = below.isAir() ? Look.HANG : Look.HANG_OVER;
            return s.setValue(LOOK, l);
        }

        @Override
        public boolean canSurvive(BlockState state, LevelReader w, BlockPos pos) {
            BlockState above = w.getBlockState(pos.above());
            return above.isFaceSturdy(w, pos.above(), Direction.UP) || above.is(this);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext ctx) {
            BlockState s = look(ctx.getLevel(), ctx.getClickedPos(), defaultBlockState());
            return canSurvive(s, ctx.getLevel(), ctx.getClickedPos()) ? s : null;
        }

        @Override
        public BlockState updateShape(BlockState s, Direction d, BlockState n, LevelAccessor w, BlockPos pos, BlockPos np) {
            if (!canSurvive(s, w, pos)) return Blocks.AIR.defaultBlockState();
            return look(w, pos, s);
        }

        @Override
        public void entityInside(BlockState state, Level world, BlockPos pos, Entity entity) {
            entity.setDeltaMovement(entity.getDeltaMovement().scale(0.5));
            entity.fallDistance = 0F;
        }

        @Override public VoxelShape getCollisionShape(BlockState s, BlockGetter w, BlockPos pos, CollisionContext ctx) { return Shapes.empty(); }
    }

    // ================================================================================================

    /**
     * 1:1 {@code BlockMush} ({@code mush}): leuchtender Pilz auf Hoehlenboden oder Atommuell-Boden; waechst
     * (bei weniger als 3 Pilzen in der Naehe) in die Umgebung, macht verseuchte Erde zu Myzel und wird mit
     * Knochenmehl (40 %) zum Riesenpilz.
     */
    public static class Mush extends Block implements BonemealableBlock {
        private static final VoxelShape SHAPE = Shapes.box(0.3, 0.0, 0.3, 0.7, 0.4, 0.7);

        public Mush(Properties p) { super(p); }

        @Override public VoxelShape getShape(BlockState s, BlockGetter w, BlockPos pos, CollisionContext ctx) { return SHAPE; }
        @Override public VoxelShape getCollisionShape(BlockState s, BlockGetter w, BlockPos pos, CollisionContext ctx) { return Shapes.empty(); }

        public static boolean canMushGrowHere(LevelReader world, BlockPos pos) {
            BlockState b = world.getBlockState(pos.below());
            return b.is(ModBlocks.WASTE_EARTH.get()) || b.is(ModBlocks.WASTE_MYCELIUM.get()) || b.is(ModBlocks.WASTE_TRINITITE.get())
                    || b.is(ModBlocks.WASTE_TRINITITE_RED.get()) || b.is(ModBlocks.BLOCK_WASTE.get()) || b.is(ModBlocks.BLOCK_WASTE_PAINTED.get())
                    || b.is(ModBlocks.BLOCK_WASTE_VITRIFIED.get());
        }

        @Override
        public boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
            if (pos.getY() < world.getMinBuildHeight() || pos.getY() >= world.getMaxBuildHeight()) return false;
            BlockPos d = pos.below();
            BlockState below = world.getBlockState(d);
            // canSustainPlant(Cave): Hoehlenpflanzen wachsen auf jeder festen Oberseite
            return below.isFaceSturdy(world, d, Direction.UP) || canMushGrowHere(world, pos);
        }

        @Override
        public BlockState updateShape(BlockState s, Direction d, BlockState n, LevelAccessor w, BlockPos pos, BlockPos np) {
            return canSurvive(s, w, pos) ? s : Blocks.AIR.defaultBlockState();
        }

        @Override
        public void randomTick(BlockState state, ServerLevel world, BlockPos pos, RandomSource rand) {
            if (!canSurvive(state, world, pos)) {
                world.destroyBlock(pos, true);
                return;
            }
            if (ModClothConfig.get().enableMycelium && world.getBlockState(pos.below()).is(ModBlocks.WASTE_EARTH.get()) && rand.nextInt(5) == 0) {
                world.setBlockAndUpdate(pos.below(), ModBlocks.WASTE_MYCELIUM.get().defaultBlockState());
            }
            if (rand.nextInt(25) != 0) return;

            int x = pos.getX(), y = pos.getY(), z = pos.getZ();
            int range = 4, maxShroom = 3;
            // Original vertauscht hier y und z (getBlock(ix, iz, iy)) - 1:1 uebernommen
            for (int ix = x - range; ix <= x + range; ++ix)
                for (int iy = y - range; iy <= y + range; ++iy)
                    for (int iz = z - 1; iz <= z + 1; ++iz)
                        if (world.getBlockState(new BlockPos(ix, iz, iy)).is(this) && --maxShroom <= 0) return;

            int ix = x + rand.nextInt(5) - 2;
            int iy = z + rand.nextInt(2) - rand.nextInt(2);
            int iz = y + rand.nextInt(5) - 2;
            for (int l1 = 0; l1 < 4; ++l1) {
                BlockPos p = new BlockPos(ix, iy, iz);
                if (world.isEmptyBlock(p) && canMushGrowHere(world, p)) { x = ix; z = iy; y = iz; }
                ix = x + rand.nextInt(5) - 2;
                iy = z + rand.nextInt(2) - rand.nextInt(2);
                iz = y + rand.nextInt(5) - 2;
            }
            BlockPos p = new BlockPos(ix, iy, iz);
            if (world.isEmptyBlock(p) && canMushGrowHere(world, p)) world.setBlock(p, defaultBlockState(), 2);
        }

        @Override public boolean isValidBonemealTarget(LevelReader w, BlockPos pos, BlockState s, boolean client) { return canSurvive(s, w, pos); }
        @Override public boolean isBonemealSuccess(Level w, RandomSource rand, BlockPos pos, BlockState s) { return rand.nextFloat() < 0.4D; }

        @Override
        public void performBonemeal(ServerLevel world, RandomSource rand, BlockPos pos, BlockState s) {
            world.removeBlock(pos, false);
            hugeMush(world, pos);
        }

        /** 1:1 {@code HugeMush.generate}. */
        public static void hugeMush(Level world, BlockPos p) {
            BlockState skin = ModBlocks.MUSH_BLOCK.get().defaultBlockState(), stem = ModBlocks.MUSH_BLOCK_STEM.get().defaultBlockState();
            int x = p.getX(), y = p.getY(), z = p.getZ();
            for (int i = -1; i < 2; i++) for (int j = -1; j < 2; j++) world.setBlock(new BlockPos(x + i, y, z + j), skin, 3);
            for (int i = -1; i < 2; i++) for (int j = -1; j < 2; j++) world.setBlock(new BlockPos(x + i, y + 3, z + j), skin, 3);
            for (int i = -2; i < 3; i++) for (int j = -2; j < 3; j++) world.setBlock(new BlockPos(x + i, y + 5, z + j), skin, 3);
            for (int i = -4; i < 5; i++) for (int j = -4; j < 5; j++) for (int k = 0; k < 3; k++) world.setBlock(new BlockPos(x + i, y + 6 + k, z + j), skin, 3);
            for (int i = -3; i < 4; i++) for (int j = -3; j < 4; j++) world.setBlock(new BlockPos(x + i, y + 9, z + j), skin, 3);
            for (int i = -1; i < 2; i++) for (int j = -1; j < 2; j++) world.setBlock(new BlockPos(x + i, y + 10, z + j), skin, 3);
            for (int i = 0; i < 8; i++) world.setBlock(new BlockPos(x, y + i, z), stem, 3);
        }
    }

    // ================================================================================================

    /** 1:1 {@code BlockMushHuge}: Riesenpilz-Block, laesst 0-2 kleine Pilze fallen ({@code nextInt(10) - 7}). */
    public static class MushHuge extends Block {
        public MushHuge(Properties p) { super(p); }

        @Override
        public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
            List<ItemStack> out = new ArrayList<>();
            int i = params.getLevel().random.nextInt(10) - 7;
            if (i > 0) out.add(new ItemStack(ModBlocks.MUSH.get(), i));
            return out;
        }

        @Override
        public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
            return new ItemStack(ModBlocks.MUSH.get());
        }
    }
}
