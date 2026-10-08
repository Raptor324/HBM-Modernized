package com.hbm_m.block.machines;

import java.util.Map;
import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineFluidTankBlockEntity;
import com.hbm_m.interfaces.IMultiblockController;
import com.hbm_m.multiblock.MultiblockSideTuples;
import com.hbm_m.multiblock.MultiblockStructureHelper;
import com.hbm_m.multiblock.PartRole;

import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
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
//? if forge {
import net.minecraftforge.common.capabilities.ForgeCapabilities;
//?}


import net.minecraft.world.level.Explosion;

public class MachineFluidTankBlock extends BaseEntityBlock implements IMultiblockController, com.hbm_m.api.block.IToolable, com.hbm_m.interfaces.ILookOverlay {

    /** Original {@code onScrew}: Schweissbrenner repariert ueber {@code IRepairable.tryRepairMultiblock}. */
    @Override
    public boolean onScrew(Level world, Player player, BlockPos pos, net.minecraft.core.Direction side, float fX, float fY, float fZ, InteractionHand hand, com.hbm_m.api.block.IToolable.ToolType tool) {
        if (tool != com.hbm_m.api.block.IToolable.ToolType.TORCH) return false;
        return com.hbm_m.api.tile.IRepairable.tryRepairMultiblock(world, pos, player);
    }

    @Override
    public void printHook(net.minecraft.client.gui.GuiGraphics g, Level world, BlockPos pos) {
        com.hbm_m.api.tile.IRepairable.addGenericOverlay(g, world, pos, net.minecraft.network.chat.Component.translatable(getDescriptionId()));
    }


    /**
     * Whether this machine has been blown up. Drives the model swap to the wrecked variant - the
     * original renders {@code *_exploded.obj} in its place - and is set from the block entity's
     * {@code explode()} / {@code repair()}.
     */
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty EXPLODED =
            net.minecraft.world.level.block.state.properties.BooleanProperty.create("exploded");

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    private final MultiblockStructureHelper structureHelper;

    public MachineFluidTankBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(EXPLODED, false));
        this.structureHelper = defineStructure();
    }

    /**
     * 1:1 {@code MachineFluidTank}: {@code getDimensions {2,0,1,1,2,2}} (5 breit, 3 hoch, 3 tief), {@code getOffset 1}
     * - der Kern sitzt in der Mitte der unteren Lage, die Klickzelle ist die Mitte der vorderen Reihe.
     * {@code makeExtra} an den vier Diagonalen des Kerns (x +-1, z +-1) = 'F'. Der ganze Quader ist belegt
     * (frueher standen hier zwei Reihen 'E' ohne Rolle, also Luftloecher im Tank).
     */
    protected MultiblockStructureHelper defineStructure() {
        // Строка 0 - передняя (ближняя к игроку), Строка 2 - задняя.

        // Направления лестницы: MultiblockSideTuples.ladder(north, south, west, east) - локально к схеме до поворота FACING.
        String[] layer0 = {
            "LFAFA",
            "LACAA",
            "AFAFA"
        };

        String[] layer1 = {
            "LAAAA",
            "LAAAA",
            "AAAAA"
        };

        String[] layer2 = {
            "LAAAA",
            "LAAAA",
            "AAAAA"
        };

        Map<Character, PartRole> roleMap = Map.of(
            'A', PartRole.DEFAULT,
            'C', PartRole.CONTROLLER,
            'L', PartRole.LADDER,
            'F', PartRole.FLUID_CONNECTOR
        );

        Map<Character, Supplier<BlockState>> symbolMap = Map.of(
        );

        Map<Character, boolean[]> ladderSideMap = Map.of(
            'L', MultiblockSideTuples.ladder(false, false, true, false)
        );

        // Символ контроллера 'C': жидкость с лица самого контроллера отключена (только F-коннекторы). См. {@link com.hbm_m.interfaces.IMultiblockSidedIO#setAllowedFluidSidesFromMultiblockStructure}.
        Map<Character, boolean[]> controllerFluidSideMap = Map.of(
            'C', MultiblockSideTuples.fluid(false, false, false, false, false, false)
        );

        return MultiblockStructureHelper.createFromLayersWithRolesAndSides(
            new String[][]{layer0, layer1, layer2},
            symbolMap,
            () -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState(),
            roleMap,
            ladderSideMap,
            null,
            controllerFluidSideMap
        );
    }

    // --- ЛОГИКА МУЛЬТИБЛОКА ---

    @Override
    public MultiblockStructureHelper getStructureHelper() {
        return this.structureHelper;
    }

    /**
     * Migration: bis zur 1:1-Umstellung sass der Kern in der Mitte der vorderen Reihe (Raster (0,0,-1)). Die
     * Auto-Reparatur verschiebt ihn samt NBT um eine Zelle nach hinten in die Mitte; die Grundflaeche bleibt
     * gleich. Nur fuer den Tank selbst, nicht fuer abgeleitete Bloecke (BAT9000 hat eine eigene Struktur).
     */
    @Nullable
    @Override
    public BlockPos getLegacyControllerOffset() {
        return getClass() == MachineFluidTankBlock.class ? new BlockPos(0, 0, -1) : null;
    }

    @Override
    public PartRole getPartRole(BlockPos localOffset) {
        // Используем хелпер для автоматического определения ролей из схемы
        return structureHelper.resolvePartRole(localOffset, this);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!state.is(oldState.getBlock()) && !level.isClientSide()) {
            BlockPos core = placeMultiblockStructure(level, pos, state);
            if (core == null) {
                return;
            }
        }
    }


    @Override
    public boolean canSurvive(BlockState state, net.minecraft.world.level.LevelReader level, BlockPos pos) {
        return super.canSurvive(state, level, pos) && canSurviveMultiblockPlacement(state, level, pos);
    }

    @Override
    public void onRemove(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide()) {
            Direction facing = state.getValue(FACING);

            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof com.hbm_m.blockentity.BaseMachineBlockEntity be) {
                be.dropInventoryContents();
            }
            // Kernverschiebung der Migration: die Teile raeumt attemptAutoRepair selbst um - hier nichts abreissen
            if (!MultiblockStructureHelper.isRepairing()) {
                structureHelper.destroyStructure(level, pos, facing);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    // 1. РАМКА ВЫДЕЛЕНИЯ: Показывает всю структуру целиком (3x3x3)
    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        // w16b: nur die Kernzelle (Raycast pro Zelle wie Original); Umriss der ganzen Maschine: MultiblockOutlineForge
        MultiblockStructureHelper helper = getStructureHelper();
        if (helper != null) {
            // Возвращаем объединенную форму всех частей
            return helper.getControllerCellShape(pState.getValue(FACING));
        }
        return Shapes.block();
    }

    // 2. КОЛЛИЗИЯ: Использует только форму самого блока контроллера из shapeMap
    @Override
    public VoxelShape getCollisionShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        MultiblockStructureHelper helper = getStructureHelper();
        if (helper != null) {
            // Берём форму ТОЛЬКО для позиции контроллера (С)
            // Она автоматически возьмётся из вашей shapeMap через хелпер
            return helper.getSpecificPartShape(helper.getControllerOffset(), pState.getValue(FACING));
        }
        return Shapes.block();
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull InteractionHand hand, @NotNull BlockHitResult hit) {
        return openMenu(state, level, pos, player, hand, hit);
    }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull BlockHitResult hit) {
        return openMenu(state, level, pos, player, InteractionHand.MAIN_HAND, hit);
    }
    *///?}

    private InteractionResult openMenu(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull InteractionHand hand, @NotNull BlockHitResult hit) {
        // Original: geschlichen keine GUI; der Identifikator (doesSneakBypassUse) stellt dann im useOn die Sorte
        if (player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.sidedSuccess(true);
        }

        BlockEntity entity = level.getBlockEntity(pos);
        if (!(entity instanceof MachineFluidTankBlockEntity tank)) {
            return InteractionResult.PASS;
        }

        MenuRegistry.openExtendedMenu((ServerPlayer) player, tank, buf -> buf.writeBlockPos(pos));
        return InteractionResult.sidedSuccess(false);
    }

    @Override
    public RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new MachineFluidTankBlockEntity(pos, state);
    }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.FLUID_TANK_BE.get(), MachineFluidTankBlockEntity::tick);
    }

    @Nullable @Override
    public BlockState getStateForPlacement(@NotNull BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(@NotNull StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, EXPLODED);
    }

    /** audit10: Original findCore - Explosionen auf Teilzellen setzen ebenfalls den ganzen Aufbau in Brand. */
    @Override
    public boolean forwardPartExplosions() {
        return true;
    }

    /**
     * 1:1 port of {@code MachineFluidTank.onBlockExploded}. A blast anywhere on the structure sets
     * the tank alight rather than knocking out a single block; a second blast on an already
     * burning tank finishes it off.
     *
     * <p>Doing this with a bomblet zeta is the original's {@code achInferno}, which is the only
     * way that advancement can be earned.</p>
     */
    @Override
    public void onBlockExploded(BlockState state, Level level, BlockPos pos, Explosion explosion) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof com.hbm_m.blockentity.machines.MachineFluidTankBlockEntity core)) {
            super.onBlockExploded(state, level, pos, explosion);
            return;
        }

        // One explosion touches many blocks of the same machine; only the first one counts.
        if (core.lastExplosion == explosion) return;
        core.lastExplosion = explosion;

        if (core.hasExploded) {
            level.removeBlock(pos, false);
            return;
        }

        core.explode();

        // The original only counts a tank that was actually holding something flammable -
        // blowing up a water tank is not an inferno.
        if (explosion.getDirectSourceEntity() instanceof com.hbm_m.entity.projectile.EntityBombletZeta
                && core.onFire) {
            com.hbm_m.advancement.ModAdvancements.grantNearby(level,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 100D,
                    com.hbm_m.advancement.ModAdvancements.INFERNO);
        }
    }

    /** {@code canDropFromExplosion}: a bombed tank leaves nothing to pick up. */
    @Override
    public boolean dropFromExplosion(Explosion explosion) {
        return false;
    }


    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<MachineFluidTankBlock> CODEC = simpleCodec(MachineFluidTankBlock::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return CODEC;
    }
    *///?}

    /** Original {@code getComparatorInputOverride}: Fuellstand des Tanks (gilt auch fuer BAT9000). */
    @Override
    public boolean hasAnalogOutputSignal(net.minecraft.world.level.block.state.BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos) {
        net.minecraft.world.level.block.entity.BlockEntity te = level.getBlockEntity(pos);
        return te instanceof com.hbm_m.blockentity.machines.MachineFluidTankBlockEntity tank ? tank.getComparatorPower() : 0;
    }
}
