package com.hbm_m.block.machines;

import java.util.Map;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineFrackingTowerBlockEntity;
import com.hbm_m.interfaces.IMultiblockController;
import com.hbm_m.multiblock.MultiblockStructureHelper;
import com.hbm_m.multiblock.PartRole;

import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
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


/**
 * Hydraulic Frackining Tower (Multiblock).
 *
 * Modernized implementation: places a tall 3x3 tower of phantom parts.
 * The exact legacy 1.7.10 shape can be refined later, but this keeps
 * the machine consistent with the current MultiblockStructureHelper system.
 */
public class MachineFrackingTowerBlock extends BaseEntityBlock implements IMultiblockController {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    private final MultiblockStructureHelper structureHelper;

    public MachineFrackingTowerBlock(Properties pProperties) {
        super(pProperties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
        this.structureHelper = defineStructureNew();
    }

    /**
     * w16b: 1:1 {@code MachineFrackingTower.fillSpace} (getOffset 0, alle Zellen voll - das Original hat keine bounding-Liste):
     * <ul>
     *   <li>{@code {3,0,0,0,0,0}} Kernsaeule y0..3, {@code {1,0,3,3,3,3}} ab y2: Plattform 7x7 (y2..3)</li>
     *   <li>vier 2x2-Fuesse y0..1 in den Ecken ({@code {-1,2,0,1,0,1}} an x/z -2 bzw. +3)</li>
     *   <li>{@code {10,-4,2,2,2,2}} Turm 5x5 y4..10, {@code {24,-9,1,1,1,1}} Schaft 3x3 y9..24</li>
     *   <li>{@code {1,0,1,1,-2,3}} mit festem {@code WEST}: Ausleger y15..16, x-1..1, z+2..3</li>
     * </ul>
     * Frueher hohle Ringe ('N' ohne Kollision) und eine Ebene zu wenig: Spieler fiel durch den Turm.
     * Die Kernsaeule ('X') bleibt Leiter wie bisher im Port.
     */
    private static MultiblockStructureHelper defineStructureNew() {
        String[][] layers = new String[25][7];
        for (int y = 0; y <= 24; y++) {
            for (int z = -3; z <= 3; z++) {
                StringBuilder row = new StringBuilder();
                for (int x = -3; x <= 3; x++) {
                    int ax = Math.abs(x), az = Math.abs(z);
                    boolean cell = (x == 0 && z == 0 && y <= 3)
                            || (y >= 2 && y <= 3)
                            || (y <= 1 && ax >= 2 && az >= 2)
                            || (y >= 4 && y <= 10 && ax <= 2 && az <= 2)
                            || (y >= 9 && ax <= 1 && az <= 1)
                            || (y >= 15 && y <= 16 && ax <= 1 && z >= 2);
                    char c = '.';
                    if (cell) c = (x == 0 && z == 0) ? (y == 0 ? 'C' : 'X') : 'O';
                    row.append(c);
                }
                layers[y][z + 3] = row.toString();
            }
        }

        Map<Character, PartRole> roleMap = Map.of(
            'O', PartRole.DEFAULT,
            'X', PartRole.LADDER,
            'C', PartRole.CONTROLLER
        );
        Map<Character, Supplier<BlockState>> symbolMap = Map.of();

        // Original getOffset() = 0: der Kern sitzt auf der Klickzelle
        return MultiblockStructureHelper.createFromLayersWithRoles(
            layers,
            symbolMap,
            () -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState(),
            roleMap
        ).withPlacementOffset(0);
    }

    @Override public MultiblockStructureHelper getStructureHelper() { return this.structureHelper; }

    @Override 
    public PartRole getPartRole(BlockPos localOffset) { 
        // Используем универсальный метод разрешения ролей из хелпера
        if (structureHelper != null) {
            return structureHelper.resolvePartRole(localOffset, this);
        }
        return PartRole.DEFAULT;
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        MultiblockStructureHelper helper = getStructureHelper();
        if (helper != null) {
            // Проверяем, является ли сам контроллер полным блоком
            if (!helper.isFullBlock(helper.getControllerOffset(), state.getValue(FACING))) {
                return Shapes.empty();
            }
        }
        return Shapes.block();
    }

    @Override
    public int getOffset() {
    return 0;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachineFrackingTowerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.HYDRAULIC_FRACKINING_TOWER_BE.get(), 
                MachineFrackingTowerBlockEntity::tick);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!state.is(oldState.getBlock()) && !level.isClientSide()) {
            BlockPos core = placeMultiblockStructure(level, pos, state);
            if (core == null) {
                return;
            }
            Direction facing = state.getValue(FACING);
        }
    }


    @Override
    public boolean canSurvive(BlockState state, net.minecraft.world.level.LevelReader level, BlockPos pos) {
        return super.canSurvive(state, level, pos) && canSurviveMultiblockPlacement(state, level, pos);
    }

    

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide()) {
            Direction facing = state.getValue(FACING);
            getStructureHelper().destroyStructure(level, pos, facing);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        if (pPlayer.isShiftKeyDown()) return InteractionResult.sidedSuccess(pLevel.isClientSide()); // Original standardOpenBehavior: geschlichen true ohne GUI
        return hbmOnUse(pState, pLevel, pPos, pPlayer, pHand, pHit);
    }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, BlockHitResult pHit) {
        if (pPlayer.isShiftKeyDown()) return InteractionResult.sidedSuccess(pLevel.isClientSide()); // Original standardOpenBehavior: geschlichen true ohne GUI
        return hbmOnUse(pState, pLevel, pPos, pPlayer, InteractionHand.MAIN_HAND, pHit);
    }
    *///?}

    private InteractionResult hbmOnUse(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        if (!pLevel.isClientSide()) {
            if (pLevel.getBlockEntity(pPos) instanceof MenuProvider provider) {
                MenuRegistry.openExtendedMenu((ServerPlayer) pPlayer, provider, buf -> buf.writeBlockPos(pPos));
            }
        }
        return InteractionResult.sidedSuccess(pLevel.isClientSide());
    }

    // 1. РАМКА ВЫДЕЛЕНИЯ: Показывает всю структуру целиком
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
            // Берём форму ТОЛЬКО для позиции контроллера
            // Она автоматически возьмётся из shapeMap через хелпер
            return helper.getSpecificPartShape(helper.getControllerOffset(), pState.getValue(FACING));
        }
        return Shapes.block();
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // w16d: Original MachineFrackingTower ist weltfest - alle Fuellbereiche ausser dem Ausleger sind symmetrisch, der
        // Ausleger ({1,0,1,1,-2,3}) wird mit festem ForgeDirection.WEST gefuellt und liegt immer bei z+2..3 (Sueden);
        // RenderFrackingTower dreht fest um 180 Grad. FACING bleibt deshalb immer NORTH (lokal = Welt).
        return this.defaultBlockState().setValue(FACING, Direction.NORTH);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<MachineFrackingTowerBlock> CODEC = simpleCodec(MachineFrackingTowerBlock::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return CODEC;
    }
    *///?}
}
