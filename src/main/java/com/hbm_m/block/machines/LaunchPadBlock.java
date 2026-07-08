package com.hbm_m.block.machines;

import java.util.Map;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.bomb.IBomb;
import com.hbm_m.api.energy.EnergyNetworkManager;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.LaunchPadBaseBlockEntity;
import com.hbm_m.blockentity.machines.LaunchPadBlockEntity;
import com.hbm_m.interfaces.IDetonatable;
import com.hbm_m.interfaces.IMultiblockController;
import com.hbm_m.multiblock.MultiblockStructureHelper;
import com.hbm_m.multiblock.PartRole;

import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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


public class LaunchPadBlock extends BaseEntityBlock implements IMultiblockController, IBomb, IDetonatable {

    @Override
    public boolean onDetonate(Level level, BlockPos pos, BlockState state, Player player) {
        if (level.isClientSide) {
            return false;
        }
        BombReturnCode result = explode(level, pos);
        return result != null && result.wasSuccessful();
    }

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    private final MultiblockStructureHelper structureHelper;

    public LaunchPadBlock(Properties pProperties) {
        super(pProperties);
        this.registerDefaultState(this.stateDefinition.any()
        .setValue(FACING, Direction.NORTH));
        this.structureHelper = defineStructureNew();
    }



    @Override
    public void onPlace(BlockState pState, Level pLevel, BlockPos pPos, BlockState pOldState, boolean pIsMoving) {
        super.onPlace(pState, pLevel, pPos, pOldState, pIsMoving);
        if (!pLevel.isClientSide() && !pState.is(pOldState.getBlock())) {
            MultiblockStructureHelper helper = getStructureHelper();
            BlockPos core = placeMultiblockStructure(pLevel, pPos, pState);
            if (core == null) {
                return;
            }
            Direction facing = pState.getValue(FACING);
            for (BlockPos localPos : helper.getStructureMap().keySet()) {
                if (getPartRole(localPos) == PartRole.UNIVERSAL_CONNECTOR) {
                    BlockPos worldPos = helper.getRotatedPos(core, localPos, facing);
                    EnergyNetworkManager.get((ServerLevel) pLevel).addNode(worldPos);
                }
            }
        }
    }



    @Override
    public boolean canSurvive(BlockState state, net.minecraft.world.level.LevelReader level, BlockPos pos) {
        return super.canSurvive(state, level, pos) && canSurviveMultiblockPlacement(state, level, pos);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            if (!level.isClientSide()) {
                MultiblockStructureHelper helper = getStructureHelper();
                Direction facing = state.getValue(FACING);
                for (BlockPos localPos : helper.getStructureMap().keySet()) {
                    if (getPartRole(localPos) == PartRole.UNIVERSAL_CONNECTOR) {
                        BlockPos worldPos = helper.getRotatedPos(pos, localPos, facing);
                        EnergyNetworkManager.get((ServerLevel) level).removeNode(worldPos);
                    }
                }

                BlockEntity blockEntity = level.getBlockEntity(pos);
                if (blockEntity instanceof LaunchPadBaseBlockEntity launchPadBe) {
                    var handler = launchPadBe.getInventory();
                    for (int i = 0; i < handler.getSlots(); i++) {
                        ItemStack stack = handler.getStackInSlot(i);
                        if (!stack.isEmpty()) {
                            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
                        }
                    }
                }

                helper.destroyStructure(level, pos, facing);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    /**
     * Триггер запуска через систему IBomb (детонаторы, командные системы).
     * Сейчас используется только синхронно из соседних блоков на сервере.
     */
    @Override
    public BombReturnCode explode(Level level, BlockPos pos) {
        if (level.isClientSide) {
            return BombReturnCode.UNDEFINED;
        }
        if (level.getBlockEntity(pos) instanceof LaunchPadBaseBlockEntity launchPad) {
            return launchPad.triggerLaunch();
        }
        return BombReturnCode.UNDEFINED;
    }

    /**
     * Реакция на изменение редстоун‑сигнала: обновляем счётчик питания у BE,
     * а сам пуск выполняется в commonServerTick по фронту 0 → +.
     */
    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
                                BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        if (!level.isClientSide
                && level.getBlockEntity(pos) instanceof LaunchPadBaseBlockEntity launchPad) {
            launchPad.checkRedstonePower();
        }
    }

    @Override public RenderShape getRenderShape(BlockState pState) { return RenderShape.MODEL; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) { pBuilder.add(FACING); }
    @Nullable @Override public BlockState getStateForPlacement(BlockPlaceContext pContext) { return this.defaultBlockState().setValue(FACING, pContext.getHorizontalDirection().getOpposite()); }
    @Nullable @Override public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) { return new LaunchPadBlockEntity(pPos, pState); }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockState pState, BlockEntityType<T> pType) {
        return createTickerHelper(pType, ModBlockEntities.LAUNCH_PAD_BE.get(), LaunchPadBlockEntity::tick);
    }

    @Override
    public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        if (!pLevel.isClientSide()) {
            if (pLevel.getBlockEntity(pPos) instanceof MenuProvider provider) {
                MenuRegistry.openExtendedMenu((ServerPlayer) pPlayer, provider, buf -> buf.writeBlockPos(pPos));
            }
        }
        return InteractionResult.sidedSuccess(pLevel.isClientSide());
    }

    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        MultiblockStructureHelper helper = getStructureHelper();
        if (helper != null) {
            // Теперь это вернет идеально подогнанную форму 3х3х3
            return helper.generateShapeFromParts(pState.getValue(FACING));
        }
        return Shapes.block();
    }
    
    @Override public MultiblockStructureHelper getStructureHelper() { return this.structureHelper; }
    
    /**
     * Определяет структуру мультиблока используя рецептоподобный способ с ролями.
     * ВАЖНО: Структура ОБЯЗАТЕЛЬНО должна содержать ровно ОДИН контроллер (символ с ролью CONTROLLER).
     * 
     * @return MultiblockStructureHelper с определённой структурой и ролями
     */
    private static MultiblockStructureHelper defineStructureNew() {
        // - 'A' = DEFAULT (обычная часть структуры)
        // - 'B' = UNIVERSAL_CONNECTOR (универсальный коннектор)
        // - 'L' = LADDER (по нему можно взобраться как по лестнице)
        // - 'C' = CONTROLLER (блок контроллера - ОБЯЗАТЕЛЬНО, ровно 1!)
        // - '.' = пустота (символ не в roleMap, будет игнорирован)
        
        // Слои структуры 3x3x3
        String[] layer0 = {
            "BAB",  // 'B' в углах - универсальные коннекторы
            "ACA",
            "BAB"
        };
        
        // === roleMap: программист сам определяет маппинг ===
        // ВАЖНО: роль CONTROLLER ОБЯЗАТЕЛЬНА и должен быть ровно ОДИН контроллер!
        Map<Character, PartRole> roleMap = Map.of(
            'A', PartRole.DEFAULT,              // Обычная часть структуры
            'B', PartRole.UNIVERSAL_CONNECTOR, // Универсальный коннектор
            'C', PartRole.CONTROLLER           // Контроллер (ОБЯЗАТЕЛЬНО!)
        );
        
        // === symbolMap: какой BlockState использовать для каждого символа ===
        // Контроллер 'C' НЕ добавляется в symbolMap - он размещается игроком отдельно!
        Map<Character, Supplier<BlockState>> symbolMap = Map.of(
            // 'A', () -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState(),
            // 'B', () -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState(),
            // 'L', () -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState()
        );

        Map<Character, VoxelShape> shapeMap = Map.of(
            'C', Block.box(0, 8, 0, 16, 16, 16),
            'A', Block.box(0, 8, 0, 16, 16, 16), // upper slab
            'B', Shapes.block()
        );

        Map<Character, VoxelShape> collisionMap = Map.of(
            'C', Block.box(0, 8, 0, 16, 16, 16),
            'A', Block.box(0, 8, 0, 16, 16, 16), // upper slab
            'B', Shapes.block()
        );
        
        // Используем createFromLayersWithRoles - автоматически найдёт позицию контроллера
        return MultiblockStructureHelper.createFromLayersWithRoles(
            new String[][]{layer0},
            symbolMap,
            () -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState(),
            roleMap,
            shapeMap,
            collisionMap
        );
    }
    
    /**
     * Старый способ определения структуры
     */
    // private static Map<BlockPos, Supplier<BlockState>> defineStructure() {
    //     ImmutableMap.Builder<BlockPos, Supplier<BlockState>> builder = ImmutableMap.builder();
    //     for (int y = 0; y <= 2; y++) for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
    //         if (x == 0 && y == 0 && z == 0) continue;
    //         builder.put(new BlockPos(x, y, z), () -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
    //     }
    //     return builder.build();
    // }
    
    @Override 
    public PartRole getPartRole(BlockPos localOffset) { 
        // Используем универсальный метод разрешения ролей из хелпера
        if (structureHelper != null) {
            return structureHelper.resolvePartRole(localOffset, this);
        }
        return PartRole.DEFAULT;
    }

    @Override
    public float getShadeBrightness(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
        return 1.0F; // Убирает тени под блоком и Ambient Occlusion
    }

    public boolean propagatesSkylightDown(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
        return true;
    }
}