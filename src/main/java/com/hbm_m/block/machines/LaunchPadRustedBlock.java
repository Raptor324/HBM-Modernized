package com.hbm_m.block.machines;

import java.util.Map;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.hbm_m.api.energy.EnergyNetworkManager;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.entity.ModBlockEntities;
import com.hbm_m.block.entity.machines.LaunchPadBaseBlockEntity;
import com.hbm_m.block.entity.machines.LaunchPadRustedBlockEntity;
import com.hbm_m.multiblock.IMultiblockController;
import com.hbm_m.multiblock.MultiblockStructureHelper;
import com.hbm_m.multiblock.PartRole;

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
import net.minecraftforge.network.NetworkHooks;

/**
 * Ржавая пусковая площадка.
 *
 * Поведение мультиблока и энергетики повторяет обычную LaunchPadBlock,
 * но использует LaunchPadRustedBlockEntity и отдельный GUI.
 */
public class LaunchPadRustedBlock extends BaseEntityBlock implements IMultiblockController {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    private final MultiblockStructureHelper structureHelper;

    public LaunchPadRustedBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
        this.structureHelper = defineStructureNew();
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!level.isClientSide() && !state.is(oldState.getBlock())) {
            MultiblockStructureHelper helper = getStructureHelper();
            Direction facing = state.getValue(FACING);

            helper.placeStructure(level, pos, facing, this);
            for (BlockPos localPos : helper.getStructureMap().keySet()) {
                if (getPartRole(localPos) == PartRole.ENERGY_CONNECTOR) {
                    BlockPos worldPos = helper.getRotatedPos(pos, localPos, facing);
                    EnergyNetworkManager.get((ServerLevel) level).addNode(worldPos);
                }
            }
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            if (!level.isClientSide()) {
                MultiblockStructureHelper helper = getStructureHelper();
                Direction facing = state.getValue(FACING);
                for (BlockPos localPos : helper.getStructureMap().keySet()) {
                    if (getPartRole(localPos) == PartRole.ENERGY_CONNECTOR) {
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

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LaunchPadRustedBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.LAUNCH_PAD_RUSTED_BE.get(), LaunchPadRustedBlockEntity::serverTick);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide()) {
            if (level.getBlockEntity(pos) instanceof MenuProvider provider) {
                NetworkHooks.openScreen((ServerPlayer) player, provider, pos);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        MultiblockStructureHelper helper = getStructureHelper();
        if (helper != null) {
            return helper.generateShapeFromParts(state.getValue(FACING));
        }
        return Shapes.block();
    }

    @Override
    public MultiblockStructureHelper getStructureHelper() {
        return this.structureHelper;
    }

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

    @Override
    public PartRole getPartRole(BlockPos localOffset) {
        if (structureHelper != null) {
            return structureHelper.resolvePartRole(localOffset, this);
        }
        return PartRole.DEFAULT;
    }
}

