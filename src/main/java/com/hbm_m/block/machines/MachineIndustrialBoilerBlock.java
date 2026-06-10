package com.hbm_m.block.machines;

import java.util.Map;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.energy.EnergyNetworkManager;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.entity.ModBlockEntities;
import com.hbm_m.block.entity.machines.MachineIndustrialBoilerBlockEntity;
import com.hbm_m.interfaces.IMultiblockController;
import com.hbm_m.multiblock.MultiblockStructureHelper;
import com.hbm_m.multiblock.PartRole;

import dev.architectury.registry.menu.MenuRegistry;
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
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
//? if forge {
import net.minecraftforge.common.capabilities.ForgeCapabilities;
//?}


/**
 * Industrial Boiler - converts water to steam using heat.
 * Multiblock structure: 3x3x5 (no ladder parts).
 */
public class MachineIndustrialBoilerBlock extends BaseEntityBlock implements IMultiblockController {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty LIT = BooleanProperty.create("lit");

    private final MultiblockStructureHelper structureHelper;

    public MachineIndustrialBoilerBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(LIT, false));
        this.structureHelper = defineStructure();
    }

    private static MultiblockStructureHelper defineStructure() {
        // Слои снизу вверх: ножки с контроллером, два пояса, слой с жидкостными точками, верх.
        String[] base = {
            "OFO",
            "FCF",
            "OFO"
        };
        String[] layer = {
            "OOO",
            "OOO",
            "OOO"
        };

        Map<Character, PartRole> roleMap = Map.of(
                'C', PartRole.CONTROLLER,
                'O', PartRole.DEFAULT,
                'E', PartRole.ENERGY_CONNECTOR,
                'F', PartRole.FLUID_CONNECTOR
        );

        Map<Character, Supplier<BlockState>> symbolMap = Map.of();

        return MultiblockStructureHelper.createFromLayersWithRoles(
                new String[][] { base, layer, layer, layer, layer },
                symbolMap,
                () -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState(),
                roleMap,
                null,
                null
        );
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);

        if (!state.is(oldState.getBlock()) && !level.isClientSide()) {
            Direction facing = state.getValue(FACING);
            structureHelper.placeStructure(level, pos, facing, this);

            EnergyNetworkManager.get((ServerLevel) level).addNode(pos);

            for (BlockPos gridPos : structureHelper.getStructureMap().keySet()) {
                PartRole role = structureHelper.resolvePartRole(gridPos, this);
                if (role.canReceiveEnergy()) {
                    BlockPos worldPos = structureHelper.getRotatedPos(pos, gridPos, facing);
                    EnergyNetworkManager.get((ServerLevel) level).addNode(worldPos);
                }
            }
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock() && !level.isClientSide()) {
            Direction facing = state.getValue(FACING);

            EnergyNetworkManager.get((ServerLevel) level).removeNode(pos);

            for (BlockPos gridPos : structureHelper.getStructureMap().keySet()) {
                PartRole role = structureHelper.resolvePartRole(gridPos, this);
                if (role.canReceiveEnergy()) {
                    BlockPos worldPos = structureHelper.getRotatedPos(pos, gridPos, facing);
                    EnergyNetworkManager.get((ServerLevel) level).removeNode(worldPos);
                }
            }

            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof com.hbm_m.block.entity.BaseMachineBlockEntity be) {
                be.dropInventoryContents();
            }

            structureHelper.destroyStructure(level, pos, facing);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachineIndustrialBoilerBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide()) {
            if (level.getBlockEntity(pos) instanceof MenuProvider provider) {
                MenuRegistry.openExtendedMenu((ServerPlayer) player, provider, buf -> buf.writeBlockPos(pos));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.INDUSTRIAL_BOILER_BE.get(), MachineIndustrialBoilerBlockEntity::tick);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return structureHelper.generateShapeFromParts(state.getValue(FACING));
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return structureHelper.getSpecificCollisionShape(structureHelper.getControllerOffset(), state.getValue(FACING));
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        if (structureHelper.isFullBlock(structureHelper.getControllerOffset(), state.getValue(FACING))) {
            return Shapes.block();
        }
        return Shapes.empty();
    }

    // --- IMultiblockController ---

    @Override
    public MultiblockStructureHelper getStructureHelper() {
        return structureHelper;
    }

    @Override
    public PartRole getPartRole(BlockPos localOffset) {
        return structureHelper.resolvePartRole(localOffset, this);
    }
}
