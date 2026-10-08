package com.hbm_m.block.generic;

import com.hbm_m.platform.StackNbt;

import java.util.List;

import javax.annotation.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.generic.TrappedBrickBlockEntity;
import com.hbm_m.damagesource.ModDamageSources;
import com.hbm_m.entity.ModEntities;
import com.hbm_m.entity.mob.botprime.EntityBOTPrimeHead;
import com.hbm_m.item.ModItems;
import com.hbm_m.sound.HbmSoundsNT;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;

/** Enargit-Ziegel des Originals: {@code BlockBallsSpawner}, {@code FragileBrick}, {@code BlockGlyph}, {@code TrappedBrick}. */
public final class JungleBricks {

    private JungleBricks() {}

    /** Gegenstand einer Metadaten-Art, als {@code BlockStateTag} (Original: Item-Schaden). */
    public static ItemStack stack(Item item, String prop, String value) {
        ItemStack s = new ItemStack(item);
        CompoundTag bst = new CompoundTag();
        bst.putString(prop, value);
        com.hbm_m.platform.BlockStateItemData.put(s, bst);
        return s;
    }

    public static String stateOf(ItemStack stack, String prop) {
        CompoundTag t = StackNbt.tag(stack);
        if (t == null || !t.contains("BlockStateTag")) return "";
        return t.getCompound("BlockStateTag").getString(prop);
    }

    // ================================================================================================

    /** 1:1 {@code BlockBallsSpawner} ({@code brick_jungle_circle}): mit dem Mechanisten-Schluessel ruft man Balls-O-Tron aus 300 Hoehe. */
    public static class Circle extends Block {
        public Circle(Properties p) { super(p); }

        //? if < 1.21.1 {
        @Override
        public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
            return activate(world, pos, player, player.getItemInHand(hand));
        }
        //?} else {
        /*@Override
        protected net.minecraft.world.ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
            activate(world, pos, player, stack);
            return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        *///?}

        private InteractionResult activate(Level world, BlockPos pos, Player player, ItemStack held) {
            if (!held.isEmpty() && held.is(ModItems.MECH_KEY.get())) {
                held.shrink(1);
                if (!world.isClientSide) {
                    EntityBOTPrimeHead bot = ModEntities.BOT_PRIME_HEAD.get().create(world);
                    if (bot != null) {
                        bot.moveTo(pos.getX() + 0.5, 300, pos.getZ() + 0.5, 0, 0);
                        bot.setDeltaMovement(0, -1.0, 0);
                        world.addFreshEntity(bot);
                        bot.spawnSegments();
                    }
                    world.setBlockAndUpdate(pos, ModBlocks.BRICK_JUNGLE_CRACKED.get().defaultBlockState());
                }
            }
            return InteractionResult.PASS;
        }
    }

    // ================================================================================================

    /** 1:1 {@code FragileBrick}: zerfaellt beim Betreten und reisst benachbarte bruechige Ziegel nach 8-11 Ticks mit. */
    public static class Fragile extends Block {
        public Fragile(Properties p) { super(p); }

        @Override
        public void tick(BlockState state, ServerLevel world, BlockPos pos, RandomSource rand) {
            world.destroyBlock(pos, false);
            notifyNeighbors(world, pos);
        }

        @Override
        public void stepOn(Level world, BlockPos pos, BlockState state, Entity entity) {
            if (world.isClientSide) return;
            world.destroyBlock(pos, false);
            notifyNeighbors(world, pos);
        }

        private void notifyNeighbors(Level world, BlockPos pos) {
            for (Direction dir : Direction.values()) {
                BlockPos n = pos.relative(dir);
                if (world.getBlockState(n).is(this)) world.scheduleTick(n, this, world.random.nextInt(4) + 8);
            }
        }

        @Override
        public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean moved) {
            if (!world.isClientSide) notifyNeighbors(world, pos);
            super.onRemove(state, world, pos, newState, moved);
        }
    }

    // ================================================================================================

    /** 1:1 {@code BlockGlyph}: 16 Glyphen an den Seiten, Ober-/Unterseite schlichter Ziegel; laesst die eigene Glyphe fallen. */
    public static class Glyph extends Block {
        public static final IntegerProperty GLYPH = IntegerProperty.create("glyph", 0, 15);

        public Glyph(Properties p) {
            super(p);
            registerDefaultState(stateDefinition.any().setValue(GLYPH, 0));
        }

        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(GLYPH); }

        @Override
        //? if < 1.21.1 {
        public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        //?} else {
        /*public ItemStack getCloneItemStack(net.minecraft.world.level.LevelReader level, BlockPos pos, BlockState state) {
        *///?}
            return stack(asItem(), "glyph", Integer.toString(state.getValue(GLYPH)));
        }
    }

    // ================================================================================================

    /** 1:1 {@code TrappedBrick}: 15 Fallen; Tretfallen loesen beim Betreten aus, Melder-Fallen ueber das Blockentity. */
    public static class Trapped extends BaseEntityBlock {

        public enum TrapType { ON_STEP, DETECTOR }

        public enum Trap implements StringRepresentable {
            FALLING_ROCKS(TrapType.DETECTOR),
            FIRE(TrapType.ON_STEP),
            ARROW(TrapType.DETECTOR),
            SPIKES(TrapType.ON_STEP),
            MINE(TrapType.ON_STEP),
            WEB(TrapType.ON_STEP),
            FLAMING_ARROW(TrapType.DETECTOR),
            PILLAR(TrapType.DETECTOR),
            RAD_CONVERSION(TrapType.ON_STEP),
            MAGIC_CONVERSTION(TrapType.ON_STEP),
            SLOWNESS(TrapType.ON_STEP),
            WEAKNESS(TrapType.ON_STEP),
            POISON_DART(TrapType.DETECTOR),
            ZOMBIE(TrapType.DETECTOR),
            SPIDERS(TrapType.DETECTOR);

            public final TrapType type;
            Trap(TrapType type) { this.type = type; }
            @Override public String getSerializedName() { return name().toLowerCase(); }

            public static Trap get(int i) {
                if (i >= 0 && i < values().length) return values()[i];
                return FIRE;
            }
        }

        public static final EnumProperty<Trap> TRAP = EnumProperty.create("trap", Trap.class);

        public Trapped(Properties p) {
            super(p);
            registerDefaultState(stateDefinition.any().setValue(TRAP, Trap.FALLING_ROCKS));
        }

        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(TRAP); }
        @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

        @Nullable @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return state.getValue(TRAP).type == TrapType.DETECTOR ? new TrappedBrickBlockEntity(pos, state) : null;
        }

        @Nullable @Override
        public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
            return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.TRAPPED_BRICK.get(), TrappedBrickBlockEntity::serverTick);
        }

        @Override
        //? if < 1.21.1 {
        public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        //?} else {
        /*public ItemStack getCloneItemStack(net.minecraft.world.level.LevelReader level, BlockPos pos, BlockState state) {
        *///?}
            return stack(asItem(), "trap", state.getValue(TRAP).getSerializedName());
        }

        @Override
        public void stepOn(Level world, BlockPos pos, BlockState state, Entity entity) {
            Trap trap = state.getValue(TRAP);
            if (world.isClientSide || trap.type != TrapType.ON_STEP || !(entity instanceof Player player)) return;
            int x = pos.getX(), y = pos.getY(), z = pos.getZ();
            BlockPos up = pos.above();

            switch (trap) {
                case FIRE -> { if (world.getBlockState(up).canBeReplaced()) world.setBlockAndUpdate(up, Blocks.FIRE.defaultBlockState()); }
                case SPIKES -> {
                    if (world.getBlockState(up).canBeReplaced()) world.setBlockAndUpdate(up, ModBlocks.SPIKES.get().defaultBlockState());
                    List<Entity> targets = world.getEntitiesOfClass(Entity.class, new AABB(x, y + 1, z, x + 1, y + 2, z + 1));
                    for (Entity e : targets) e.hurt(ModDamageSources.spikes(world), 10);
                    world.playSound(null, x + 0.5, y + 1.5, z + 0.5, HbmSoundsNT.get("entity.slicer"), SoundSource.BLOCKS, 1.0F, 1.0F);
                }
                case MINE -> world.explode(null, x + 0.5, y + 1.5, z + 0.5, 1F, false, Level.ExplosionInteraction.TNT);
                case WEB -> { if (world.getBlockState(up).canBeReplaced()) world.setBlockAndUpdate(up, Blocks.COBWEB.defaultBlockState()); }
                case RAD_CONVERSION -> convert(world, pos, ModBlocks.BRICK_JUNGLE_OOZE.get());
                case MAGIC_CONVERSTION -> convert(world, pos, ModBlocks.BRICK_JUNGLE_MYSTIC.get());
                case SLOWNESS -> player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 300, 2));
                case WEAKNESS -> player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 300, 2));
                default -> { }
            }
            world.playSound(null, x + 0.5D, y + 0.5D, z + 0.5D, SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.3F, 0.6F);
            world.setBlockAndUpdate(pos, ModBlocks.BRICK_JUNGLE.get().defaultBlockState());
        }

        private static void convert(Level world, BlockPos pos, Block to) {
            for (int a = -3; a <= 3; a++) for (int b = -3; b <= 3; b++) for (int c = -3; c <= 3; c++) {
                if (world.random.nextBoolean()) continue;
                BlockPos p = pos.offset(a, b, c);
                BlockState bl = world.getBlockState(p);
                if (bl.is(ModBlocks.BRICK_JUNGLE.get()) || bl.is(ModBlocks.BRICK_JUNGLE_CRACKED.get()) || bl.is(ModBlocks.BRICK_JUNGLE_LAVA.get()))
                    world.setBlockAndUpdate(p, to.defaultBlockState());
            }
        }

        //? if >1.20.1 {
        /*public static final com.mojang.serialization.MapCodec<Trapped> CODEC = simpleCodec(Trapped::new);
        @Override protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
        *///?}
    }
}
