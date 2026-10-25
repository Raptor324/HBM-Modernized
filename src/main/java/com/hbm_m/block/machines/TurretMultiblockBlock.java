package com.hbm_m.block.machines;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.machines.TurretBaseBlockEntity;
import com.hbm_m.multiblock.DummyableStructureBuilder;
import com.hbm_m.multiblock.MultiblockStructureHelper;

import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 1:1 {@code TurretBaseNT} / {@code TurretArty} / {@code TurretHIMARS}: die Geschuetztuerme des Originals sind
 * {@code BlockDummyable}-Strukturen, kein Einzelblock. Sentry und beschaedigter Sentry sind im Original
 * {@code BlockContainer} (1x1) und bleiben {@link TurretBlock}.
 *
 * <ul>
 *   <li>{@link Small}: {@code getDimensions {0,0,1,0,1,0}}, {@code getOffset 0} - 2x2x1, Hitbox/Kollision
 *       je Zelle 0..0.5 hoch ({@code setBlockBoundsBasedOnState}).</li>
 *   <li>{@link Large}: {@code getDimensions {1,0,2,1,2,1}}, {@code getOffset 1} - 4x4x2, volle Bloecke.</li>
 * </ul>
 *
 * <p>Im Original ist jede Dummy-Zelle ein {@code TileEntityProxyCombo().inventory().power()} (Fritz zusaetzlich
 * {@code .fluid()}, Tauon {@code (true, true, false)}), Howard (beschaedigt) hat gar keine Proxies. Im Port sind
 * die Zellen deshalb {@code UNIVERSAL_CONNECTOR}; Fluessigkeit reicht der Konnektor nur weiter, wenn der Kern
 * einen Tank meldet (nur Fritz).</p>
 *
 * <p>Alte Welten: Tuerme aus der Einzelblock-Zeit tragen {@code TurretBaseBlockEntity#isLegacySingle()}. Sie
 * bleiben als 1x1 mit Mitte in der Blockmitte voll funktionsfaehig; sobald die Struktur gebaut werden kann
 * (Auto-Reparatur beim Laden/alle 100 Ticks, nur bei freiem Platz), wird der Block zum Kern der Struktur.</p>
 */
public abstract class TurretMultiblockBlock extends DummyableMachineBlock {

    private final Supplier<BlockEntityType<TurretBaseBlockEntity>> beType;

    protected TurretMultiblockBlock(Properties properties, Supplier<BlockEntityType<TurretBaseBlockEntity>> beType) {
        super(properties);
        this.beType = beType;
    }

    /**
     * Baut die Struktur aus {@code getDimensions()}; mit {@code proxies} wird jede Zelle ausser dem Kern
     * Anschlusszelle (Original: alle Metas &lt; 12 bekommen {@code TileEntityProxyCombo}).
     */
    protected static MultiblockStructureHelper turretStructure(int[] dim, int offset, boolean proxies) {
        DummyableStructureBuilder builder = DummyableStructureBuilder.create().box(dim);
        if (proxies) {
            // box(): x aus [-E, +W], y aus [-D, +U], z aus [-S, +N]; extra(forward, up, side) -> (side, up, -forward)
            for (int x = -dim[5]; x <= dim[4]; x++) {
                for (int y = -dim[1]; y <= dim[0]; y++) {
                    for (int z = -dim[3]; z <= dim[2]; z++) {
                        if (x == 0 && y == 0 && z == 0) continue;
                        builder.extra(-z, y, x);
                    }
                }
            }
        }
        return builder.placementOffset(offset)
                .build(() -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
    }

    /** Altbestand aus der Einzelblock-Zeit, dessen Struktur (noch) nicht steht. */
    protected static boolean isLegacy(BlockGetter level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof TurretBaseBlockEntity turret && turret.isLegacySingle();
    }

    /** Original {@code getRenderType() == -1}: alles (auch der Sockel) zeichnet der TESR am Turm-Zentrum. */
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (isLegacy(level, pos)) return getCellShape();
        VoxelShape custom = getCustomMasterVoxelShape(state);
        // w16d: nur der Anteil der Kernzelle (Raycast pro Zelle); den Umriss der ganzen Struktur zeichnet MultiblockOutlineForge
        return custom != null ? MultiblockStructureHelper.cellShape(custom, BlockPos.ZERO) : super.getShape(state, level, pos, context);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getCellShape();
    }

    /** Form einer einzelnen Zelle. */
    protected VoxelShape getCellShape() {
        return Shapes.block();
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide() && isLegacy(level, pos)) {
            // Einzelblock-Altbestand: es gibt keine eigene Struktur - fremde Maschinenteile ringsum nicht abreissen
            MultiblockStructureHelper.runSafeRemove(() -> super.onRemove(state, level, pos, newState, isMoving));
            return;
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return beType.get().create(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, beType.get(), TurretBaseBlockEntity::tick);
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return hbmOnUse(level, pos, player);
    }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return hbmOnUse(level, pos, player);
    }
    *///?}

    /** Original {@code standardOpenBehavior(world, x, y, z, player, 0)}: geschlichen keine GUI (Klick trotzdem verbraucht). */
    protected InteractionResult hbmOnUse(Level level, BlockPos pos, Player player) {
        if (!level.isClientSide() && !player.isShiftKeyDown()) {
            BlockEntity entity = level.getBlockEntity(pos);
            if (entity instanceof MenuProvider menuProvider && player instanceof ServerPlayer serverPlayer) {
                MenuRegistry.openExtendedMenu(serverPlayer, menuProvider, buf -> buf.writeBlockPos(pos));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    /** {@code TurretBaseNT}: 2x2, Kern ohne Versatz, Platte 0.5 hoch. */
    public static class Small extends TurretMultiblockBlock {

        private static final VoxelShape CELL = net.minecraft.world.level.block.Block.box(0, 0, 0, 16, 8, 16);
        private final Map<Direction, VoxelShape> masterShapes = new EnumMap<>(Direction.class);

        public Small(Properties properties, Supplier<BlockEntityType<TurretBaseBlockEntity>> beType) {
            super(properties, beType);
        }

        @Override
        protected MultiblockStructureHelper defineStructure() {
            return turretStructure(new int[] { 0, 0, 1, 0, 1, 0 }, 0, true);
        }

        @Override
        protected VoxelShape getCellShape() {
            return CELL;
        }

        /** {@code setBlockBoundsBasedOnState}/{@code getCollisionBoundingBoxFromPool}: jede Zelle 0..0.5. */
        @Nullable
        @Override
        public VoxelShape getCustomMasterVoxelShape(BlockState state) {
            Direction facing = state.getValue(FACING);
            return masterShapes.computeIfAbsent(facing, f -> {
                VoxelShape shape = CELL;
                for (BlockPos p : getStructureHelper().getAllPartPositions(BlockPos.ZERO, f)) {
                    shape = Shapes.or(shape, CELL.move(p.getX(), p.getY(), p.getZ()));
                }
                return shape.optimize();
            });
        }

        //? if >1.20.1 {
        /*public static final com.mojang.serialization.MapCodec<Small> CODEC = simpleCodec(props -> new Small(props, () -> com.hbm_m.blockentity.ModBlockEntities.TURRET_CHEKHOV_BE.get()));
        @Override protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() { return CODEC; }
        *///?}
    }

    /**
     * 1:1 {@code TurretHowardDamaged}: Struktur wie {@code TurretBaseNT}, aber {@code createNewTileEntity}
     * liefert fuer die Dummies {@code null} (keine Proxies), {@code onBlockActivated -> false},
     * {@code getItemDropped -> null} (Loot-Tabelle leer).
     */
    public static class SmallDamaged extends Small {

        public SmallDamaged(Properties properties, Supplier<BlockEntityType<TurretBaseBlockEntity>> beType) {
            super(properties, beType);
        }

        @Override
        protected MultiblockStructureHelper defineStructure() {
            return turretStructure(new int[] { 0, 0, 1, 0, 1, 0 }, 0, false);
        }

        @Override
        protected InteractionResult hbmOnUse(Level level, BlockPos pos, Player player) {
            return InteractionResult.PASS;
        }

        //? if >1.20.1 {
        /*public static final com.mojang.serialization.MapCodec<SmallDamaged> CODEC = simpleCodec(props -> new SmallDamaged(props, () -> com.hbm_m.blockentity.ModBlockEntities.TURRET_HOWARD_DAMAGED_BE.get()));
        @Override protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() { return CODEC; }
        *///?}
    }

    /** {@code TurretArty} / {@code TurretHIMARS}: 4x4x2, {@code getOffset 1}, volle Bloecke. */
    public static class Large extends TurretMultiblockBlock {

        public Large(Properties properties, Supplier<BlockEntityType<TurretBaseBlockEntity>> beType) {
            super(properties, beType);
        }

        @Override
        protected MultiblockStructureHelper defineStructure() {
            return turretStructure(new int[] { 1, 0, 2, 1, 2, 1 }, 1, true);
        }

        //? if >1.20.1 {
        /*public static final com.mojang.serialization.MapCodec<Large> CODEC = simpleCodec(props -> new Large(props, () -> com.hbm_m.blockentity.ModBlockEntities.TURRET_ARTY_BE.get()));
        @Override protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() { return CODEC; }
        *///?}
    }
}
