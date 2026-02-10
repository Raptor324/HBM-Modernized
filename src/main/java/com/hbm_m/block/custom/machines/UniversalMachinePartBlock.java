package com.hbm_m.block.custom.machines;

import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.custom.decorations.DoorBlock;
import com.hbm_m.block.entity.custom.doors.DoorBlockEntity;
import com.hbm_m.block.entity.custom.doors.DoorDecl;
import com.hbm_m.block.entity.custom.doors.DoorDeclRegistry;
// Этот класс реализует фантомный блок, который является частью каждой мультиблочной структуры.
// Фантомные блоки невидимы и не имеют коллизии, но позволяют игроку взаимодействовать с контроллером структуры,
// наведя курсор на любую часть структуры. Форма фантомного блока определяется формой всей структуры, сдвинутой так,
// чтобы правильно отображаться на позиции этой части.
import com.hbm_m.block.entity.custom.machines.UniversalMachinePartBlockEntity;
import com.hbm_m.multiblock.IMultiblockController;
import com.hbm_m.multiblock.IMultiblockPart;
import com.hbm_m.multiblock.MultiblockStructureHelper;
import com.hbm_m.multiblock.PartRole;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

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

    /**
     * Проверяет, потерял ли блок связь с контроллером (осиротел).
     */
    private boolean isOrphaned(BlockGetter level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof IMultiblockPart part)) {
            return false;
        }

        BlockPos controllerPos = part.getControllerPos();
        if (controllerPos == null) {
            return true; // Нет контроллера = осиротел
        }

        BlockState controllerState = level.getBlockState(controllerPos);
        if (!(controllerState.getBlock() instanceof IMultiblockController)) {
            return true; // Контроллер не существует или не является контроллером = осиротел
        }

        return false; // Контроллер валиден
    }

    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        if (!(pLevel.getBlockEntity(pPos) instanceof IMultiblockPart part)) {
            return Shapes.block();
        }

        BlockPos controllerPos = part.getControllerPos();
        if (controllerPos == null) return Shapes.block();

        BlockState controllerState = pLevel.getBlockState(controllerPos);
        Block controllerBlock = controllerState.getBlock();

        if (!(controllerBlock instanceof IMultiblockController controller)) {
            return Shapes.block();
        }

        VoxelShape masterShape;

        // 1. Определяем "Мастер-форму" (общий контур всего мультиблока)
        if (controllerBlock instanceof DoorBlock doorBlock) {
            DoorDecl decl = DoorDeclRegistry.getById(doorBlock.getDoorDeclId());

            if (decl != null && decl.isDynamicShape()) {
                // Для люков берем динамически собранную форму (которую мы реализовали в DoorBlock)
                masterShape = doorBlock.getShape(controllerState, pLevel, controllerPos, pContext);
            } else {
                // Для обычных дверей берем статическую форму всей структуры
                Direction facing = controllerState.getValue(DoorBlock.FACING);
                masterShape = controller.getStructureHelper().generateShapeFromParts(facing);
            }
        } else {
            // Логика для остальных мультиблоков (Assembler и т.д.)
            Direction facing = controllerState.getValue(HorizontalDirectionalBlock.FACING);
            masterShape = controller.getStructureHelper().generateShapeFromParts(facing);
        }

        // Сдвигаем общую форму мультиблока относительно текущего фантомного блока.
        // Без этого рамка будет смещена.
        BlockPos vecToController = controllerPos.subtract(pPos);
        return masterShape.move(vecToController.getX(), vecToController.getY(), vecToController.getZ());
    }


    @Override
    public VoxelShape getCollisionShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {

        // 1. Получаем TileEntity этой части
        if (!(pLevel.getBlockEntity(pPos) instanceof IMultiblockPart part)) {
            return Shapes.block();
        }

        // 2. Находим позицию контроллера
        BlockPos controllerPos = part.getControllerPos();
        if (controllerPos == null) {
            return Shapes.block();
        }

        // 3. Получаем состояние и блок контроллера
        BlockState controllerState = pLevel.getBlockState(controllerPos);
        Block controllerBlock = controllerState.getBlock(); // Получаем блок один раз

        if (!(controllerBlock instanceof IMultiblockController controller)) {
            return Shapes.block();
        }

        // Сначала проверяем, есть ли у контроллера общая сложная форма (мастер-шейп)
        VoxelShape masterShape = controller.getCustomMasterVoxelShape(controllerState);
        
        if (masterShape != null && !masterShape.isEmpty()) {
            BlockPos vecToController = controllerPos.subtract(pPos);
            return masterShape.move(vecToController.getX(), vecToController.getY(), vecToController.getZ());
        }

        // Используем pattern matching или приведение типов, чтобы задействовать переменную doorBlock
        if (controllerBlock instanceof DoorBlock doorBlock) {
            BlockEntity controllerBE = pLevel.getBlockEntity(controllerPos);
            
            if (controllerBE instanceof DoorBlockEntity doorBE) {
                String declId = doorBlock.getDoorDeclId();
                DoorDecl decl = com.hbm_m.block.entity.custom.doors.DoorDeclRegistry.getById(declId);
                
                if (decl != null && decl.getStructureDefinition() != null) {
                    DoorDecl.DoorStructureDefinition def = decl.getStructureDefinition();
                    
                    Direction facing = controllerState.getValue(DoorBlock.FACING);
                    BlockPos worldOffset = pPos.subtract(controllerPos);
                    BlockPos localOffset = MultiblockStructureHelper.rotateBack(worldOffset, facing);

                    // Считаем открытой, если состояние не 0 (0 = закрыта)
                    boolean isOpen = doorBE.getState() != 0; 
                    
                    // Выбираем карту коллизии используя переменную
                    Map<BlockPos, VoxelShape> map = isOpen ? def.getOpenShapes() : def.getClosedShapes();
                    
                    VoxelShape shape = map.get(localOffset);
                    if (shape != null) {
                        if (!shape.isEmpty()) {
                            return MultiblockStructureHelper.rotateShape(shape, facing);
                        }
                        return shape;
                    }
                }
            }
       }

        // 4. Общая логика для всех остальных частей мультиблока (MachineAssembler и т.д.)
        MultiblockStructureHelper helper = controller.getStructureHelper();
        Direction facing = controllerState.getValue(HorizontalDirectionalBlock.FACING);

        BlockPos worldOffset = pPos.subtract(controllerPos);
        BlockPos localOffset = MultiblockStructureHelper.rotateBack(worldOffset, facing);
        BlockPos gridPos = localOffset.offset(helper.getControllerOffset());

        return helper.getSpecificCollisionShape(gridPos, facing);
    }

    @Override
    public void neighborChanged(BlockState pState, Level pLevel, BlockPos pPos, Block pBlock, BlockPos pFromPos, boolean pIsMoving) {
        super.neighborChanged(pState, pLevel, pPos, pBlock, pFromPos, pIsMoving);

        // Делегируем всю логику в MultiblockStructureHelper
        MultiblockStructureHelper.onNeighborChangedForPart(pLevel, pPos, pFromPos);

        // Немедленная проверка на клиенте при изменении соседей (оптимизация для быстрого обнаружения)
        // Это особенно важно, когда контроллер был разрушен - блок сразу станет осиротевшим
        if (pLevel.isClientSide()) {
            if (isOrphaned(pLevel, pPos)) {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    com.hbm_m.client.ClientRenderHandler.addOrphanedPhantomBlock(pPos);
                });
            } else {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    com.hbm_m.client.ClientRenderHandler.removeOrphanedPhantomBlock(pPos);
                });
            }
        }
        if (pLevel.getBlockEntity(pPos) instanceof IMultiblockPart part) {
            BlockPos controllerPos = part.getControllerPos();
            if (controllerPos != null) {
                BlockEntity be = pLevel.getBlockEntity(controllerPos);
                if (be instanceof DoorBlockEntity doorBE) {
                    // Фантом не говорит "у меня нет сигнала", он говорит "перепроверь всю дверь"
                    doorBE.checkRedstonePower();
                }
            }
        }
    }

    /**
     * Позволяет блокам с ролью LADDER быть использованными как лестница.
     * Работает с любых боковых сторон.
     */
    @Override
    public boolean isLadder(BlockState pState, LevelReader pLevel, BlockPos pPos, LivingEntity pEntity) {
        // Проверяем, что это мультиблочная часть с ролью LADDER
        if (pLevel instanceof Level level && level.getBlockEntity(pPos) instanceof IMultiblockPart part) {
            PartRole role = part.getPartRole();
            
            if (role == PartRole.LADDER) {
                return true;
            }
        }
        return false;
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        // Если это не полный блок (например, труба или плита), возвращаем пустую форму.
        // Это скажет игре: "Не выталкивай игрока из этого блока".
        if (level.getBlockEntity(pos) instanceof IMultiblockPart part) {
            BlockPos ctrlPos = part.getControllerPos();
            if (ctrlPos != null && level.getBlockState(ctrlPos).getBlock() instanceof DoorBlock) {
                return Shapes.empty();
            }
        }
        
        return isFullBlockInGrid(level, pos) ? Shapes.block() : Shapes.empty();
    }

    private boolean isFullBlockInGrid(BlockGetter level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof IMultiblockPart part) {
            BlockPos controllerPos = part.getControllerPos();
            if (controllerPos != null) {
                BlockState controllerState = level.getBlockState(controllerPos);
                if (controllerState.getBlock() instanceof IMultiblockController controller) {
                    MultiblockStructureHelper helper = controller.getStructureHelper();
                    Direction facing = controllerState.getValue(HorizontalDirectionalBlock.FACING);
                    
                    // Вычисляем позицию в сетке
                    BlockPos worldOffset = pos.subtract(controllerPos);
                    BlockPos localOffset = MultiblockStructureHelper.rotateBack(worldOffset, facing);
                    BlockPos gridPos = localOffset.offset(helper.getControllerOffset());
                    
                    return helper.isFullBlock(gridPos, facing);
                }
            }
        }
        return true; // По умолчанию считаем полным для безопасности
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
                if (controllerPos != null && !pLevel.isClientSide()) {
                    BlockState controllerState = pLevel.getBlockState(controllerPos);
                    // Проверяем, что контроллер ещё существует
                    if (controllerState.getBlock() instanceof IMultiblockController controller) {
                        // Вызываем разрушение структуры
                        if (controllerState.hasProperty(HorizontalDirectionalBlock.FACING)) {
                            Direction facing = controllerState.getValue(HorizontalDirectionalBlock.FACING);
                            controller.getStructureHelper().destroyStructure(pLevel, controllerPos, facing);
                        }
                        // Ломаем контроллер
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

    /**
     * Возвращает скорость разрушения блока. Для осиротевших блоков возвращает очень большое значение,
     * чтобы они ломались мгновенно даже рукой.
     */
    @Override
    public float getDestroyProgress(BlockState pState, Player pPlayer, BlockGetter pLevel, BlockPos pPos) {
        // Если блок осиротел (потерял связь с контроллером), делаем его мгновенно ломаемым
        if (isOrphaned(pLevel, pPos)) {
            return 1.0f; // Максимальная скорость разрушения (мгновенно)
        }
        // Для нормальных блоков используем стандартную логику
        return super.getDestroyProgress(pState, pPlayer, pLevel, pPos);
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter reader, BlockPos pos) {
        return true;
    }

    @Override
    public float getShadeBrightness(BlockState state, BlockGetter world, BlockPos pos) {
        return 1.0F;
    }

    @Override
    public int getLightBlock(BlockState state, BlockGetter world, BlockPos pos) {
        return 0;
    }
}