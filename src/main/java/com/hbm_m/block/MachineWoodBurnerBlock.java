package com.hbm_m.block;

// Этот класс реализует структуру блока дровяного генератора, который является контроллером мультиблочной структуры.
// Дровяной генератор занимает 2x2x2 блока и использует вспомогательный класс MultiblockStructureHelper для управления своей структурой.
import com.hbm_m.block.entity.ModBlockEntities;
import com.hbm_m.block.entity.MachineWoodBurnerBlockEntity;
import com.hbm_m.multiblock.IMultiblockController;
import com.hbm_m.multiblock.MultiblockStructureHelper;
import com.hbm_m.multiblock.PartRole;
import com.google.common.collect.ImmutableMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.Supplier;

public class MachineWoodBurnerBlock extends BaseEntityBlock implements IMultiblockController {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty LIT = BooleanProperty.create("lit");

    // Форма блока 2x2x2 блока (каждый блок = 16 пикселей, значит 32 пикселя = 2 блока)
    private static final VoxelShape SHAPE = Shapes.box(0.0, 0.0, 0.0, 2.0, 2.0, 2.0);

    public MachineWoodBurnerBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(LIT, Boolean.FALSE));
    }

    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        // Используем "мастер-форму" мультиблока — это устраняет сдвиги outline при наведении.
        VoxelShape master = getCustomMasterVoxelShape(pState);
        return master == null ? SHAPE : master;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        VoxelShape master = getCustomMasterVoxelShape(pState);
        return master == null ? SHAPE : master;
    }

    @Override
    public VoxelShape getCustomMasterVoxelShape(BlockState state) {
        // Генерируем форму по частям структуры (2x2x2). MultiblockStructureHelper возвращает shape
        // в координатах контроллера, что корректно используется UniversalMachinePartBlock.
        return getStructureHelper().generateShapeFromParts(state.getValue(FACING));
    }

    @Override
    public RenderShape getRenderShape(BlockState pState) {
        return RenderShape.MODEL;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(FACING, LIT);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        return this.defaultBlockState().setValue(FACING, pContext.getHorizontalDirection().getOpposite());
    }

    @Override
    public void onPlace(BlockState pState, Level pLevel, BlockPos pPos, BlockState pOldState, boolean pIsMoving) {
        super.onPlace(pState, pLevel, pPos, pOldState, pIsMoving);
        if (!pState.is(pOldState.getBlock())) {
            if (!pLevel.isClientSide) {
                // Построить 2x2x2 структуру частей при установке контроллера
                getStructureHelper().placeStructure(pLevel, pPos, pState.getValue(FACING), this);
            }
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new MachineWoodBurnerBlockEntity(pPos, pState);
    }

    @Override
    public void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pIsMoving) {
        if (pState.getBlock() != pNewState.getBlock()) {
            BlockEntity blockEntity = pLevel.getBlockEntity(pPos);
            if (blockEntity instanceof MachineWoodBurnerBlockEntity) {
                ((MachineWoodBurnerBlockEntity) blockEntity).drops();
            }
            // Destroy multiblock parts when controller removed
            this.getStructureHelper().destroyStructure(pLevel, pPos, pState.getValue(FACING));
        }
        super.onRemove(pState, pLevel, pPos, pNewState, pIsMoving);
    }

    @Override
    public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        if (!pLevel.isClientSide()) {
            BlockEntity entity = pLevel.getBlockEntity(pPos);
            if (entity instanceof MachineWoodBurnerBlockEntity) {
                NetworkHooks.openScreen(((ServerPlayer) pPlayer), (MachineWoodBurnerBlockEntity) entity, pPos);
            } else {
                throw new IllegalStateException("Our Container provider is missing!");
            }
        }
        return InteractionResult.sidedSuccess(pLevel.isClientSide());
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockState pState, BlockEntityType<T> pBlockEntityType) {
        return createTickerHelper(pBlockEntityType, ModBlockEntities.WOOD_BURNER_BE.get(), MachineWoodBurnerBlockEntity::tick);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState pState) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState pState, Level pLevel, BlockPos pPos) {
        BlockEntity blockEntity = pLevel.getBlockEntity(pPos);
        if (blockEntity instanceof MachineWoodBurnerBlockEntity burner) {
            return burner.getComparatorPower();
        }
        return 0;
    }

    // --- IMultiblockController implementation ---

    private static MultiblockStructureHelper STRUCTURE_HELPER;

    private static Map<BlockPos, Supplier<BlockState>> defineStructure() {
        ImmutableMap.Builder<BlockPos, Supplier<BlockState>> builder = ImmutableMap.builder();
        // 2x2x2 cube: x=0..1, y=0..1, z=0..1, skip controller at (0,0,0)
        for (int y = 0; y <= 1; y++) {
            for (int x = 0; x <= 1; x++) {
                for (int z = 0; z <= 1; z++) {
                    if (x == 0 && y == 0 && z == 0) continue;
                    final int fx = x, fy = y, fz = z;
                    builder.put(new BlockPos(fx, fy, fz), () -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
                }
            }
        }
        return builder.build();
    }

    @Override
    public MultiblockStructureHelper getStructureHelper() {
        if (STRUCTURE_HELPER == null) {
            STRUCTURE_HELPER = new MultiblockStructureHelper(defineStructure(), () -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
        }
        return STRUCTURE_HELPER;
    }

    @Override
    public PartRole getPartRole(BlockPos localOffset) {
        // Пока роли не нужны — все части структурные
        return PartRole.DEFAULT;
    }
}