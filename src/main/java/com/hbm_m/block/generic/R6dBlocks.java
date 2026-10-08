package com.hbm_m.block.generic;

import com.hbm_m.damagesource.ModDamageSources;
import com.hbm_m.damagesource.ModDamageTypes;
import com.hbm_m.sound.HbmSoundsNT;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Kleine Bloecke aus R6d (Originalklassen je Unterklasse genannt). */
public final class R6dBlocks {

    private R6dBlocks() {}

    /** {@code isOpaqueCube() || isNormalCube()} eines Nachbarn. */
    static boolean solidNeighbor(BlockGetter world, BlockPos p) {
        BlockState s = world.getBlockState(p);
        return s.isSolidRender(world, p) || s.isRedstoneConductor(world, p);
    }

    // ================================================================================================

    /** 1:1 {@code BlockSandbags}: Saeule 0.25-0.75, wird zu festen Nachbarn/Sandsaecken hin breiter. */
    public static class Sandbags extends Block {
        public static final BooleanProperty NORTH = BlockStateProperties.NORTH, SOUTH = BlockStateProperties.SOUTH,
                EAST = BlockStateProperties.EAST, WEST = BlockStateProperties.WEST;

        public Sandbags(Properties p) {
            super(p);
            registerDefaultState(stateDefinition.any().setValue(NORTH, false).setValue(SOUTH, false).setValue(EAST, false).setValue(WEST, false));
        }

        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(NORTH, SOUTH, EAST, WEST); }

        private boolean con(BlockGetter w, BlockPos p) { return solidNeighbor(w, p) || w.getBlockState(p).is(this); }

        private BlockState update(BlockGetter w, BlockPos pos, BlockState s) {
            return s.setValue(NORTH, con(w, pos.north())).setValue(SOUTH, con(w, pos.south())).setValue(EAST, con(w, pos.east())).setValue(WEST, con(w, pos.west()));
        }

        @Override public BlockState getStateForPlacement(BlockPlaceContext ctx) { return update(ctx.getLevel(), ctx.getClickedPos(), defaultBlockState()); }

        @Override public BlockState updateShape(BlockState s, Direction d, BlockState n, LevelAccessor w, BlockPos pos, BlockPos np) { return update(w, pos, s); }

        @Override
        public VoxelShape getShape(BlockState s, BlockGetter w, BlockPos pos, CollisionContext ctx) {
            return Shapes.box(s.getValue(WEST) ? 0 : 0.25, 0, s.getValue(NORTH) ? 0 : 0.25, s.getValue(EAST) ? 1 : 0.75, 1, s.getValue(SOUTH) ? 1 : 0.75);
        }
    }

    // ================================================================================================

    /** 1:1 {@code Spikes}: ohne Kollision; wer faellt ({@code motionY < -0.1}), bekommt 100 Schaden. */
    public static class Spikes extends Block {
        public Spikes(Properties p) { super(p); }

        @Override public VoxelShape getCollisionShape(BlockState s, BlockGetter w, BlockPos pos, CollisionContext ctx) { return Shapes.empty(); }

        @Override
        public void entityInside(BlockState state, Level world, BlockPos pos, Entity ent) {
            if (ent instanceof LivingEntity && ent.getDeltaMovement().y < -0.1) {
                if (ent.hurt(ModDamageSources.create(world, ModDamageTypes.SPIKES), 100))
                    world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, HbmSoundsNT.get("entity.slicer"), SoundSource.BLOCKS, 1.0F, 1.0F);
            }
        }
    }

    // ================================================================================================

    //? if < 1.21.1 {
    public static final BlockSetType STEEL_TRAPDOOR_SET = new BlockSetType("hbm_m_steel", true, net.minecraft.world.level.block.SoundType.METAL,
    //?} else {
    /*public static final BlockSetType STEEL_TRAPDOOR_SET = new BlockSetType("hbm_m_steel", true, true, true, BlockSetType.PressurePlateSensitivity.EVERYTHING, net.minecraft.world.level.block.SoundType.METAL,
    *///?}
            SoundEvents.WOODEN_DOOR_CLOSE, SoundEvents.WOODEN_DOOR_OPEN, SoundEvents.WOODEN_DOOR_CLOSE, SoundEvents.WOODEN_DOOR_OPEN,
            SoundEvents.METAL_PRESSURE_PLATE_CLICK_OFF, SoundEvents.METAL_PRESSURE_PLATE_CLICK_ON, SoundEvents.STONE_BUTTON_CLICK_OFF, SoundEvents.STONE_BUTTON_CLICK_ON);

    /**
     * 1:1 {@code BlockNTMTrapdoor} ({@code trapdoor_steel}): Eisenfalltuer, die man trotzdem von Hand oeffnet
     * (Tuergeraeusch 1003). Offen ueber einer Leiter wird sie selbst zur Leiter mit leiterduenner Hitbox.
     */
    public static class SteelTrapdoor extends TrapDoorBlock {
        //? if < 1.21.1 {
        public SteelTrapdoor(Properties p) { super(p, STEEL_TRAPDOOR_SET); }
        //?} else {
        /*public SteelTrapdoor(Properties p) { super(STEEL_TRAPDOOR_SET, p); }
        *///?}

        @Override
        public boolean isLadder(BlockState state, LevelReader world, BlockPos pos, LivingEntity entity) {
            if (!state.getValue(OPEN)) return false;
            BlockState below = world.getBlockState(pos.below());
            return below.isLadder(world, pos.below(), entity);
        }

        @Override
        public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext ctx) {
            if (world instanceof LevelReader r && isLadder(state, r, pos, null)) {
                double t = 0.125;
                return switch (state.getValue(FACING)) {
                    case NORTH -> Shapes.box(0, 0, 1 - t, 1, 1, 1);
                    case SOUTH -> Shapes.box(0, 0, 0, 1, 1, t);
                    case WEST -> Shapes.box(1 - t, 0, 0, 1, 1, 1);
                    default -> Shapes.box(0, 0, 0, t, 1, 1);
                };
            }
            return super.getCollisionShape(state, world, pos, ctx);
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
            state = state.cycle(OPEN);
            world.setBlock(pos, state, 2);
            if (state.getValue(WATERLOGGED)) world.scheduleTick(pos, net.minecraft.world.level.material.Fluids.WATER, net.minecraft.world.level.material.Fluids.WATER.getTickDelay(world));
            world.playSound(player, pos, state.getValue(OPEN) ? SoundEvents.WOODEN_DOOR_OPEN : SoundEvents.WOODEN_DOOR_CLOSE, SoundSource.BLOCKS, 1.0F, world.random.nextFloat() * 0.1F + 0.9F);
            return InteractionResult.sidedSuccess(world.isClientSide);
        }
    }

    // ================================================================================================

    /** 1:1 {@code BlockNTMGlassPane}: Scheibe, verbindet sich zusaetzlich mit NTM-Glas, durchscheinende Ebene. */
    public static class GlassPane extends IronBarsBlock {
        public GlassPane(Properties p) { super(p); }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext ctx) {
            BlockState s = super.getStateForPlacement(ctx);
            BlockGetter w = ctx.getLevel();
            BlockPos pos = ctx.getClickedPos();
            return s.setValue(NORTH, s.getValue(NORTH) || w.getBlockState(pos.north()).getBlock() instanceof BlockNTMGlass)
                    .setValue(SOUTH, s.getValue(SOUTH) || w.getBlockState(pos.south()).getBlock() instanceof BlockNTMGlass)
                    .setValue(WEST, s.getValue(WEST) || w.getBlockState(pos.west()).getBlock() instanceof BlockNTMGlass)
                    .setValue(EAST, s.getValue(EAST) || w.getBlockState(pos.east()).getBlock() instanceof BlockNTMGlass);
        }

        @Override
        public BlockState updateShape(BlockState s, Direction d, BlockState n, LevelAccessor w, BlockPos pos, BlockPos np) {
            BlockState r = super.updateShape(s, d, n, w, pos, np);
            if (d.getAxis().isHorizontal() && n.getBlock() instanceof BlockNTMGlass) r = r.setValue(PROPERTY_BY_DIRECTION.get(d), true);
            return r;
        }
    }

    // ================================================================================================

    /**
     * 1:1 {@code BlockBarrier} ({@code wood_barrier}): Palisade an festen Nachbarn und an der Seite der Ausrichtung,
     * oben eine Querlatte unter festen Bloecken. {@code FACING} = Meta (Seite der Platte = Gegenrichtung).
     */
    public static class Barrier extends Block {
        public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
        public static final BooleanProperty NX = BooleanProperty.create("nx"), NZ = BooleanProperty.create("nz"),
                PX = BooleanProperty.create("px"), PZ = BooleanProperty.create("pz"), PY = BooleanProperty.create("py");

        public Barrier(Properties p) {
            super(p);
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(NX, false).setValue(NZ, false).setValue(PX, false).setValue(PZ, false).setValue(PY, false));
        }

        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(FACING, NX, NZ, PX, PZ, PY); }

        private static BlockState update(BlockGetter w, BlockPos pos, BlockState s) {
            Direction f = s.getValue(FACING);
            return s.setValue(NX, solidNeighbor(w, pos.west()) || f == Direction.EAST)
                    .setValue(NZ, solidNeighbor(w, pos.north()) || f == Direction.SOUTH)
                    .setValue(PX, solidNeighbor(w, pos.east()) || f == Direction.WEST)
                    .setValue(PZ, solidNeighbor(w, pos.south()) || f == Direction.NORTH)
                    .setValue(PY, solidNeighbor(w, pos.above()));
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext ctx) {
            Direction side = ctx.getClickedFace();
            Direction f = side.getAxis().isHorizontal() ? side : ctx.getHorizontalDirection().getOpposite();
            return update(ctx.getLevel(), ctx.getClickedPos(), defaultBlockState().setValue(FACING, f));
        }

        @Override public BlockState updateShape(BlockState s, Direction d, BlockState n, LevelAccessor w, BlockPos pos, BlockPos np) { return update(w, pos, s); }

        @Override
        public VoxelShape getShape(BlockState s, BlockGetter w, BlockPos pos, CollisionContext ctx) {
            return switch (s.getValue(FACING)) {
                case EAST -> Shapes.box(0, 0, 0, 0.125, 1, 1);
                case SOUTH -> Shapes.box(0, 0, 0, 1, 1, 0.125);
                case WEST -> Shapes.box(0.875, 0, 0, 1, 1, 1);
                default -> Shapes.box(0, 0, 0.875, 1, 1, 1);
            };
        }

        @Override
        public VoxelShape getCollisionShape(BlockState s, BlockGetter w, BlockPos pos, CollisionContext ctx) {
            VoxelShape r = Shapes.empty();
            if (s.getValue(NX)) r = Shapes.or(r, Shapes.box(0, 0, 0, 0.125, 1, 1));
            if (s.getValue(NZ)) r = Shapes.or(r, Shapes.box(0, 0, 0, 1, 1, 0.125));
            if (s.getValue(PX)) r = Shapes.or(r, Shapes.box(0.875, 0, 0, 1, 1, 1));
            if (s.getValue(PZ)) r = Shapes.or(r, Shapes.box(0, 0, 0.875, 1, 1, 1));
            return r;
        }
    }

    // ================================================================================================

    /**
     * 1:1 {@code BlockWoodStructure} ({@code wood_structure} Dach / Geruest / Decke als Bloecke): Latten, beim Geruest
     * mit Stuetzen und Leiterwirkung; Anschluesse nur an gleiche Bloecke.
     */
    public static class WoodStructure extends Block {
        public enum Type { ROOF, SCAFFOLD, CEILING }
        public static final BooleanProperty NX = BooleanProperty.create("nx"), NZ = BooleanProperty.create("nz"),
                PX = BooleanProperty.create("px"), PZ = BooleanProperty.create("pz"), PY = BooleanProperty.create("py");
        public final Type type;

        public WoodStructure(Properties p, Type type) {
            super(p);
            this.type = type;
            registerDefaultState(stateDefinition.any().setValue(NX, false).setValue(NZ, false).setValue(PX, false).setValue(PZ, false).setValue(PY, false));
        }

        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(NX, NZ, PX, PZ, PY); }

        private BlockState update(BlockGetter w, BlockPos pos, BlockState s) {
            return s.setValue(NX, w.getBlockState(pos.west()).is(this)).setValue(NZ, w.getBlockState(pos.north()).is(this))
                    .setValue(PX, w.getBlockState(pos.east()).is(this)).setValue(PZ, w.getBlockState(pos.south()).is(this))
                    .setValue(PY, w.getBlockState(pos.above()).is(this));
        }

        @Override public BlockState getStateForPlacement(BlockPlaceContext ctx) { return update(ctx.getLevel(), ctx.getClickedPos(), defaultBlockState()); }

        @Override public BlockState updateShape(BlockState s, Direction d, BlockState n, LevelAccessor w, BlockPos pos, BlockPos np) { return update(w, pos, s); }

        @Override
        public VoxelShape getShape(BlockState s, BlockGetter w, BlockPos pos, CollisionContext ctx) {
            return switch (type) {
                case ROOF -> Shapes.box(0, 0, 0, 1, 0.1875, 1);
                case SCAFFOLD -> Shapes.block();
                case CEILING -> Shapes.box(0, 0.875, 0, 1, 1, 1);
            };
        }

        @Override
        public VoxelShape getCollisionShape(BlockState s, BlockGetter w, BlockPos pos, CollisionContext ctx) {
            return type == Type.SCAFFOLD ? Shapes.box(0.0625, 0, 0.0625, 0.9375, 1, 0.9375) : getShape(s, w, pos, ctx);
        }

        @Override
        public boolean isLadder(BlockState state, LevelReader world, BlockPos pos, LivingEntity entity) {
            return type == Type.SCAFFOLD && entity instanceof Player;
        }
    }

    // ================================================================================================

    /** 1:1 {@code BlockPorous}: Stein, der mit Behutsamkeit glatten Stein und sonst Bruchstein liefert (Loot-Tabelle). */
    public static class Porous extends Block {
        public Porous(Properties p) { super(p); }
    }
}
