package com.hbm_m.block.machines;

import java.util.Map;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.energy.EnergyNetworkManager;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.entity.ModBlockEntities;
import com.hbm_m.block.entity.machines.MachineGasCentrifugeBlockEntity;
import com.hbm_m.interfaces.IMultiblockController;
import com.hbm_m.multiblock.MultiblockStructureHelper;
import com.hbm_m.multiblock.PartRole;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;

public class MachineGasCentrifugeBlock extends BaseEntityBlock implements IMultiblockController {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    private final MultiblockStructureHelper structureHelper;

    public MachineGasCentrifugeBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
        this.structureHelper = defineStructure();
    }

    private MultiblockStructureHelper defineStructure() {
        // E = Energy connector (can receive power from cables)
        // A = Default structural part
        // C = Controller (the main block)
        String[] layer0 = { "EAE",
                            "ACA",
                            "EAE" 
                        }; // Bottom - corners are energy connectors

        String[] layer1 = { "AAA",
                            "AAA",
                            "AAA" 
                        }; // Middle

        String[] layer2 = { "AAA",
                            "AAA",
                            "AAA" 
                        }; // Top

        Map<Character, PartRole> roleMap = Map.of(
            'A', PartRole.DEFAULT,
            'C', PartRole.CONTROLLER,
            'E', PartRole.ENERGY_CONNECTOR
        );

        Map<Character, Supplier<BlockState>> symbolMap = Map.of(
            'A', () -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState(),
            'E', () -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState()
        );

        return MultiblockStructureHelper.createFromLayersWithRoles(
            new String[][]{layer0, layer1, layer2},
            symbolMap,
            () -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState(),
            roleMap
        );
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!level.isClientSide() && !state.is(oldState.getBlock())) {
            Direction facing = state.getValue(FACING);
            structureHelper.placeStructure(level, pos, facing, this);

            EnergyNetworkManager.get((ServerLevel) level).addNode(pos);

            for (BlockPos localPos : structureHelper.getStructureMap().keySet()) {
                if (getPartRole(localPos).canReceiveEnergy()) {
                    BlockPos worldPos = structureHelper.getRotatedPos(pos, localPos, facing);
                    EnergyNetworkManager.get((ServerLevel) level).addNode(worldPos);
                }
            }
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            if (!level.isClientSide()) {
                Direction facing = state.getValue(FACING);
                
                EnergyNetworkManager.get((ServerLevel) level).removeNode(pos);
                
                for (BlockPos localPos : structureHelper.getStructureMap().keySet()) {
                    if (getPartRole(localPos).canReceiveEnergy()) {
                        BlockPos worldPos = structureHelper.getRotatedPos(pos, localPos, facing);
                        EnergyNetworkManager.get((ServerLevel) level).removeNode(worldPos);
                    }
                }

                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof MachineGasCentrifugeBlockEntity gasCentrifuge) {
                    gasCentrifuge.drops();
                }
                
                structureHelper.destroyStructure(level, pos, facing);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide()) {
            BlockEntity entity = level.getBlockEntity(pos);
            if (entity instanceof MenuProvider menuProvider) {
                NetworkHooks.openScreen((ServerPlayer) player, menuProvider, pos);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return structureHelper.generateShapeFromParts(state.getValue(FACING));
    }

    @Override
    public MultiblockStructureHelper getStructureHelper() {
        return this.structureHelper;
    }

    @Override
    public PartRole getPartRole(BlockPos localOffset) {
        if (structureHelper != null) {
            return structureHelper.resolvePartRole(localOffset, this);
        }
        return PartRole.DEFAULT;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachineGasCentrifugeBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.GAS_CENTRIFUGE_BE.get(), MachineGasCentrifugeBlockEntity::tick);
    }
}
