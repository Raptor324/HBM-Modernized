package com.hbm_m.block.decorations;

import com.hbm_m.blockentity.decorations.PedestalBlockEntity;
import com.hbm_m.extprop.HbmPlayerProps;
import com.hbm_m.inventory.recipes.PedestalRecipes;
import com.hbm_m.inventory.recipes.PedestalRecipes.PedestalExtraCondition;
import com.hbm_m.inventory.recipes.PedestalRecipes.PedestalRecipe;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import org.jetbrains.annotations.Nullable;

/**
 * Порт {@code BlockPedestal} (1.7.10) — постамент с парящим предметом.
 * ПКМ пустой рукой — взять предмет, ПКМ с предметом — выставить его.
 * Сигнал редстоуна на центральном постаменте запускает рецепты {@link PedestalRecipes}.
 */
public class PedestalBlock extends BaseEntityBlock {

    public PedestalBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    // Геометрия RenderPedestal (1.7.10): нижняя плита 0-4px, колонна 12x12
    // 4-12px, верхняя плита 12-16px — между плитами сквозной вырез.
    private static final net.minecraft.world.phys.shapes.VoxelShape SHAPE =
            net.minecraft.world.phys.shapes.Shapes.or(
                    Block.box(0, 0, 0, 16, 4, 16),
                    Block.box(2, 4, 2, 14, 12, 14),
                    Block.box(0, 12, 0, 16, 16, 16));

    @Override
    public net.minecraft.world.phys.shapes.VoxelShape getShape(net.minecraft.world.level.block.state.BlockState state,
            net.minecraft.world.level.BlockGetter level, net.minecraft.core.BlockPos pos,
            net.minecraft.world.phys.shapes.CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PedestalBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(Level level, BlockState state, net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, com.hbm_m.blockentity.ModBlockEntities.PEDESTAL_BE.get(), PedestalBlockEntity::tick);
    }

    // ─── Original pedestalEntries: Amulette auf Sockeln wirken 3 s lang im Umkreis von 100 Bloecken ──────

    public enum PedestalEntryType {
        CHARM_OF_PROTECTION,
        METEORITE_CHARM
    }

    public record PedestalEntry(PedestalEntryType type, BlockPos pos, long timestamp) { }

    public static final java.util.Map<net.minecraft.resources.ResourceKey<Level>, java.util.List<PedestalEntry>> pedestalEntries = new java.util.HashMap<>();

    public static final int timeout = 60; //3 seconds

    public static void pushPedestalEntry(Level world, PedestalEntryType type, BlockPos pos) {
        pedestalEntries.computeIfAbsent(world.dimension(), k -> new java.util.ArrayList<>()).add(new PedestalEntry(type, pos, world.getGameTime()));
    }

    /** Original {@code checkPedestalEntries}: abgelaufene Eintraege entfernen. */
    public static void checkPedestalEntries(net.minecraft.resources.ResourceKey<Level> dim, long currentTime) {
        java.util.List<PedestalEntry> entries = pedestalEntries.get(dim);
        if (entries == null) return;
        entries.removeIf(x -> x.timestamp() < currentTime - timeout);
    }

    @Nullable
    public static java.util.List<PedestalEntry> getEntriesForDimension(net.minecraft.resources.ResourceKey<Level> dim) {
        return pedestalEntries.get(dim);
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
            net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
        return swap(level, pos, player);
    }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hit) {
        return swap(level, pos, player);
    }
    *///?}

    private static InteractionResult swap(Level level, BlockPos pos, Player player) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof PedestalBlockEntity pedestal)) return InteractionResult.PASS;
        if (player.isSecondaryUseActive()) return InteractionResult.PASS;

        ItemStack held = player.getMainHandItem();
        ItemStack current = pedestal.getItem();
        if (current.isEmpty() && !held.isEmpty()) {
            if (!level.isClientSide) {
                pedestal.setItem(held.copy());
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        } else if (!current.isEmpty() && held.isEmpty()) {
            if (!level.isClientSide) {
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, current.copy());
                pedestal.setItem(ItemStack.EMPTY);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<PedestalBlock> CODEC = simpleCodec(PedestalBlock::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }
    *///?}

    /**
     * Original {@code onNeighborBlockChange}: wird der mittlere Sockel mit Redstone versorgt, werden die neun Sockel
     * (Mitte, gerade 3 Bloecke, diagonal je 2 Bloecke) gegen {@link PedestalRecipes} geprueft.
     */
    @Override
    public void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos fromPos, boolean moving) {
        super.neighborChanged(state, world, pos, block, fromPos, moving);
        if (world.isClientSide || !world.hasNeighborSignal(pos)) return;

        int x = pos.getX(), y = pos.getY(), z = pos.getZ();
        // NORTH = -Z, SOUTH = +Z, WEST = -X, EAST = +X
        PedestalBlockEntity nw = castOrNull(world, x - 2, y, z - 2);
        PedestalBlockEntity n = castOrNull(world, x, y, z - 3);
        PedestalBlockEntity ne = castOrNull(world, x + 2, y, z - 2);
        PedestalBlockEntity w = castOrNull(world, x - 3, y, z);
        PedestalBlockEntity center = castOrNull(world, x, y, z);
        PedestalBlockEntity e = castOrNull(world, x + 3, y, z);
        PedestalBlockEntity sw = castOrNull(world, x - 2, y, z + 2);
        PedestalBlockEntity s = castOrNull(world, x, y, z + 3);
        PedestalBlockEntity se = castOrNull(world, x + 2, y, z + 2);
        if (center == null) return;

        PedestalBlockEntity[] tileArray = new PedestalBlockEntity[] {nw, n, ne, w, center, e, sw, s, se};
        java.util.List<Player> nearbyPlayers = world.getEntitiesOfClass(Player.class, new net.minecraft.world.phys.AABB(x, y, z, x + 1, y + 1, z + 1).inflate(20, 20, 20));
        float celestial = world.getTimeOfDay(0F);

        outer: for (PedestalRecipe recipe : PedestalRecipes.getRecipes()) {

            /// EXTRA CONDITIONS ///
            if (recipe.extra == PedestalExtraCondition.FULL_MOON) {
                if (celestial < 0.35 || celestial > 0.65) continue;
                if (world.getMoonPhase() != 0) continue;
            }

            if (recipe.extra == PedestalExtraCondition.NEW_MOON) {
                if (celestial < 0.35 || celestial > 0.65) continue;
                if (world.getMoonPhase() != 4) continue;
            }

            if (recipe.extra == PedestalExtraCondition.SUN) {
                if (celestial > 0.15 && celestial < 0.85) continue;
            }

            if (recipe.extra == PedestalExtraCondition.BAD_KARMA) {
                boolean matches = false;
                for (Player player : nearbyPlayers) if (HbmPlayerProps.get(player).reputation <= -10) { matches = true; break; }
                if (!matches) continue;
            }

            if (recipe.extra == PedestalExtraCondition.GOOD_KARMA) {
                boolean matches = false;
                for (Player player : nearbyPlayers) if (HbmPlayerProps.get(player).reputation >= 10) { matches = true; break; }
                if (!matches) continue;
            }

            /// CHECK ITEMS ///
            for (int i = 0; i < 9; i++) {
                ItemStack pedestal = tileArray[i] != null ? tileArray[i].getItem() : ItemStack.EMPTY;
                if (pedestal.isEmpty() && recipe.input[i] != null) continue outer;
                if (!pedestal.isEmpty() && recipe.input[i] == null) continue outer;
                if (pedestal.isEmpty() && recipe.input[i] == null) continue;

                if (!recipe.input[i].matchesRecipe(pedestal, true) || recipe.input[i].stacksize != pedestal.getCount()) continue outer;
            }

            /// REMOVE ITEMS ///
            for (int i = 0; i < 9; i++) {
                if (i == 4) continue;
                ItemStack pedestal = tileArray[i] != null ? tileArray[i].getItem() : ItemStack.EMPTY;
                if (pedestal.isEmpty() && recipe.input[i] == null) continue;
                tileArray[i].setItem(ItemStack.EMPTY);
            }

            /// PRODUCE RESULT ///
            center.setItem(recipe.getOutput().copy());
            if (world instanceof net.minecraft.server.level.ServerLevel server)
                com.hbm_m.particle.helper.ExplosionSmallCreator.composeEffect(server, x + 0.5, y + 1.5, z + 0.5, 10, 2.5F, 1F);

            // Original: jeder Spieler in 50 Bloecken erhaelt statLegendary
            java.util.List<Player> players = world.getEntitiesOfClass(Player.class,
                    new net.minecraft.world.phys.AABB(x + 0.5, y, z + 0.5, x + 0.5, y, z + 0.5).inflate(50, 50, 50));
            for (Player player : players) player.awardStat(com.hbm_m.advancement.ModStats.LEGENDARY.get(), 1);

            return;
        }
    }

    @Nullable
    private static PedestalBlockEntity castOrNull(Level world, int x, int y, int z) {
        BlockPos p = new BlockPos(x, y, z);
        if (!world.isLoaded(p)) return null;
        return world.getBlockEntity(p) instanceof PedestalBlockEntity pedestal ? pedestal : null;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof PedestalBlockEntity pedestal && !pedestal.getItem().isEmpty()) {
                level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        pedestal.getItem().copy()));
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
