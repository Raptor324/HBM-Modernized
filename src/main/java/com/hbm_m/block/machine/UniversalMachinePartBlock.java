package com.hbm_m.block.machine;

import com.hbm_m.block.DoorBlock;
import com.hbm_m.block.entity.DoorBlockEntity;
import com.hbm_m.block.entity.machine.UniversalMachinePartBlockEntity;
import com.hbm_m.multiblock.IMultiblockController;
import com.hbm_m.multiblock.IMultiblockPart;
import com.hbm_m.multiblock.MultiblockStructureHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

public class UniversalMachinePartBlock extends BaseEntityBlock {

    // FACING is the only property we need to sync with the controller
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    public UniversalMachinePartBlock(Properties pProperties) {
        super(pProperties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(FACING);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new UniversalMachinePartBlockEntity(pPos, pState);
    }

    @Override
    public RenderShape getRenderShape(BlockState pState) {
        // Phantom blocks are always invisible, the controller's BlockEntityRenderer handles the visuals.
        return RenderShape.INVISIBLE;
    }

    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        // 1. Get the BlockEntity of this part
        if (!(pLevel.getBlockEntity(pPos) instanceof IMultiblockPart part)) {
            return SMALL_INTERACT_SHAPE;
        }

        // 2. Find the controller's position
        BlockPos controllerPos = part.getControllerPos();
        if (controllerPos == null) {
            return SMALL_INTERACT_SHAPE;
        }

        // 3. Get the controller's state and block
        BlockState controllerState = pLevel.getBlockState(controllerPos);
        if (!(controllerState.getBlock() instanceof IMultiblockController controller)) {
            return SMALL_INTERACT_SHAPE;
        }

        if (controller instanceof DoorBlock doorBlock) {
            // ИСПРАВЛЕНО: Используем OBJ AABB вместо generateShapeFromParts
            String doorId = doorBlock.getDoorDeclId();
            Direction facing = controllerState.getValue(DoorBlock.FACING);
            
            Map<String, AABB> allParts = MultiblockStructureHelper.getDoorPartAABBs(doorId);
            if (allParts.isEmpty()) {
                // Fallback на стандартную форму
                VoxelShape fallback = controller.getStructureHelper()
                    .generateShapeFromParts(facing);
                BlockPos offset = pPos.subtract(controllerPos);
                return fallback.move(-offset.getX(), -offset.getY(), -offset.getZ());
            }
            
            // Генерируем рамку из всех частей OBJ (независимо от прогресса — для outline)
            java.util.List<String> allPartNames = new ArrayList<>(allParts.keySet());
            VoxelShape shape = MultiblockStructureHelper.generateShapeFromDoorParts(
                doorId, allPartNames, facing
            );
            
            BlockPos offset = pPos.subtract(controllerPos);
            return shape.move(-offset.getX(), -offset.getY(), -offset.getZ());
        }

        // 4. Для обычных мультиблоков - показываем полную форму
        VoxelShape masterShape = controller.getCustomMasterVoxelShape(controllerState);
        if (masterShape == null) {
            masterShape = controller.getStructureHelper().generateShapeFromParts(controllerState.getValue(FACING));
        }

        if (masterShape.isEmpty()) {
            return SMALL_INTERACT_SHAPE;
        }

        BlockPos offset = pPos.subtract(controllerPos);
        return masterShape.move(-offset.getX(), -offset.getY(), -offset.getZ());
    }

    @Override
    public VoxelShape getCollisionShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        
        // 1. Get the BlockEntity of this part
        if (!(pLevel.getBlockEntity(pPos) instanceof IMultiblockPart part)) {
            return Shapes.empty(); // Для коллизии возвращаем пустоту, если что-то не так
        }

        // 2. Find the controller's position
        BlockPos controllerPos = part.getControllerPos();
        if (controllerPos == null) {
            return Shapes.empty();
        }

        // 3. Get the controller's state and block
        BlockState controllerState = pLevel.getBlockState(controllerPos);
        if (!(controllerState.getBlock() instanceof IMultiblockController controller)) {
            return Shapes.empty();
        }

        if (controller instanceof DoorBlock doorBlock) {
            BlockEntity controllerBE = pLevel.getBlockEntity(controllerPos);
            if (!(controllerBE instanceof DoorBlockEntity doorBE)) return Shapes.empty();
    
            String doorId = doorBlock.getDoorDeclId();
            Direction facing = controllerState.getValue(DoorBlock.FACING);
            
            byte doorState = doorBE.getState();
            
            // Полностью открыта (state == 1) — без коллизии
            if (doorState == 1) {
                return Shapes.empty();
            }
            
            // Полностью закрыта (state == 0) — полная коллизия из всех AABB
            if (doorState == 0) {
                Map<String, AABB> allParts = MultiblockStructureHelper.getDoorPartAABBs(doorId);
                if (allParts.isEmpty()) {
                    // Fallback: используем стандартную форму мультиблока
                    VoxelShape fallback = controller.getStructureHelper()
                        .generateShapeFromParts(facing);
                    BlockPos offset = pPos.subtract(controllerPos);
                    return fallback.move(-offset.getX(), -offset.getY(), -offset.getZ());
                }
                
                // Генерируем полную коллизию из всех частей OBJ
                java.util.List<String> allPartNames = new ArrayList<>(allParts.keySet());
                VoxelShape shape = MultiblockStructureHelper.generateShapeFromDoorParts(
                    doorId, allPartNames, facing
                );
                
                BlockPos offset = pPos.subtract(controllerPos);
                return shape.move(-offset.getX(), -offset.getY(), -offset.getZ());
            }
            
            // В процессе движения (state == 2 или 3) — динамическая коллизия по прогрессу
            float progress = doorBE.getOpenProgress();
            java.util.List<String> visibleParts = getVisiblePartsForProgress(doorId, progress);
            
            if (visibleParts.isEmpty()) return Shapes.empty();
            
            VoxelShape shape = MultiblockStructureHelper.generateShapeFromDoorParts(
                doorId, visibleParts, facing
            );
            
            BlockPos offset = pPos.subtract(controllerPos);
            return shape.move(-offset.getX(), -offset.getY(), -offset.getZ());
        }

        // 4. Для обычных мультиблоков (не дверей) - используем стандартную логику
        VoxelShape masterShape = controller.getCustomMasterVoxelShape(controllerState);
        if (masterShape == null) {
            masterShape = controller.getStructureHelper().generateShapeFromParts(controllerState.getValue(FACING));
        }

        if (masterShape.isEmpty()) {
            return Shapes.empty();
        }

        BlockPos offset = pPos.subtract(controllerPos);
        return masterShape.move(-offset.getX(), -offset.getY(), -offset.getZ());
    }

    /**
     * Определяет, какие части видимы на текущем прогрессе открытия.
     * Здесь можно реализовать логику удаления частей створок по мере открытия.
     */
    private java.util.List<String> getVisiblePartsForProgress(String doorId, float progress) {
        Map<String, AABB> allParts = MultiblockStructureHelper.getDoorPartAABBs(doorId);
        if (allParts.isEmpty()) return Collections.emptyList();

        // Простая стратегия: если прогресс < 0.99, показываем все части (рамка + створки)
        // Можно расширить, чтобы удалять «doorLeft»/«doorRight» по порогам прогресса
        if (progress >= 0.99f) {
            // Полностью открыто — только рамка (если есть)
            return allParts.keySet().stream()
                .filter(name -> name.toLowerCase().contains("frame"))
                .collect(java.util.stream.Collectors.toList());
        }

        // Не полностью открыто — все части
        return new ArrayList<>(allParts.keySet());
    }

    @Override
    public void neighborChanged(BlockState pState, Level pLevel, BlockPos pPos, Block pBlock, BlockPos pFromPos, boolean pIsMoving) {
        super.neighborChanged(pState, pLevel, pPos, pBlock, pFromPos, pIsMoving);
        
        // Делегируем всю логику в MultiblockStructureHelper
        MultiblockStructureHelper.onNeighborChangedForPart(pLevel, pPos, pFromPos);
    }

    @Override
    public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        if (pLevel.getBlockEntity(pPos) instanceof IMultiblockPart part) {
            BlockPos controllerPos = part.getControllerPos();
            // Если контроллера нет (пусто) — удаляем фантомный блок на сервере
            if (controllerPos == null) {
                if (!pLevel.isClientSide()) {
                    pLevel.setBlock(pPos, Blocks.AIR.defaultBlockState(), 3);
                }
                return InteractionResult.sidedSuccess(pLevel.isClientSide());
            }

            BlockState controllerState = pLevel.getBlockState(controllerPos);
            if (controllerState.getBlock() instanceof IMultiblockController) {
                // Redirect the interaction to the main controller block
                return controllerState.use(pLevel, pPlayer, pHand, pHit.withPosition(controllerPos));
            } else {
                // Контроллер сохранён, но в мире он невалиден — удаляем фантом как резервный механизм
                if (!pLevel.isClientSide()) {
                    pLevel.setBlock(pPos, Blocks.AIR.defaultBlockState(), 3);
                }
                return InteractionResult.sidedSuccess(pLevel.isClientSide());
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pIsMoving) {
        if (!pState.is(pNewState.getBlock())) {
            if (pLevel.getBlockEntity(pPos) instanceof IMultiblockPart partBe) {
                BlockPos controllerPos = partBe.getControllerPos();
                if (controllerPos != null) {
                    BlockState controllerState = pLevel.getBlockState(controllerPos);
                    // Check if the controller block is still there before trying to destroy it
                    if (controllerState.getBlock() instanceof IMultiblockController) {
                        pLevel.destroyBlock(controllerPos, true);
                    }
                }
            }
        }
        super.onRemove(pState, pLevel, pPos, pNewState, pIsMoving);
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        if (level.getBlockEntity(pos) instanceof IMultiblockPart part) {
            BlockPos controllerPos = part.getControllerPos();
            if (controllerPos != null) {
                BlockState controllerState = level.getBlockState(controllerPos);
                // Return the item of the main controller block
                return controllerState.getBlock().getCloneItemStack(level, controllerPos, controllerState);
            }
        }
        return ItemStack.EMPTY;
    }

    // добавляем в класс константу небольшого хитбокса (в конце класса или рядом с другими полями)
    private static final VoxelShape SMALL_INTERACT_SHAPE = Shapes.box(0.45, 0.0, 0.45, 0.55, 0.5, 0.55);
}