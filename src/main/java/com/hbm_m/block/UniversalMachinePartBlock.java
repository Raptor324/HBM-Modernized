package com.hbm_m.block;

import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.decorations.DoorBlock;
import com.hbm_m.block.entity.doors.DoorBlockEntity;
import com.hbm_m.block.entity.doors.DoorDecl;
import com.hbm_m.block.entity.doors.DoorDeclRegistry;
import com.hbm_m.block.machines.TransitionSealBlock;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.LaunchPadBaseBlockEntity;
import com.hbm_m.blockentity.machines.MachineRadarBlockEntity;
import com.hbm_m.blockentity.machines.TransitionSealBlockEntity;
import com.hbm_m.blockentity.machines.UniversalMachinePartBlockEntity;
import com.hbm_m.interfaces.IDetonatable;
import com.hbm_m.interfaces.IMultiblockController;
import com.hbm_m.interfaces.IMultiblockPart;
import com.hbm_m.multiblock.PartRole;
import com.hbm_m.item.ModItems;
import com.hbm_m.multiblock.MultiblockStructureHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
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
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class UniversalMachinePartBlock extends BaseEntityBlock implements IDetonatable {

    @Override
    public boolean onDetonate(Level level, BlockPos partPos, BlockState partState, Player player) {
        if (level.isClientSide) return false;
        BlockEntity be = level.getBlockEntity(partPos);
        if (!(be instanceof IMultiblockPart part)) return false;
        BlockPos controllerPos = part.getControllerPos();
        if (controllerPos == null) return false;
        BlockState controllerState = level.getBlockState(controllerPos);
        Block controllerBlock = controllerState.getBlock();
        if (controllerBlock instanceof IDetonatable detonatable) {
            return detonatable.onDetonate(level, controllerPos, controllerState, player);
        }
        return false;
    }

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty PASSABLE = BooleanProperty.create("passable");

    public UniversalMachinePartBlock(Properties pProperties) {
        super(pProperties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(PASSABLE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(FACING, PASSABLE);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new UniversalMachinePartBlockEntity(pPos, pState);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide
                ? null
                : createTickerHelper(type, ModBlockEntities.UNIVERSAL_MACHINE_PART_BE.get(),
                        (lvl, pos, st, be) -> UniversalMachinePartBlockEntity.tick(lvl, pos, st, be));
    }

    @Override
    public RenderShape getRenderShape(BlockState pState) {
        return RenderShape.INVISIBLE;
    }

    private boolean isOrphaned(BlockGetter level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof IMultiblockPart part)) {
            return false;
        }
        BlockPos controllerPos = part.getControllerPos();
        if (controllerPos == null) {
            return true; 
        }
        BlockState controllerState = level.getBlockState(controllerPos);
        if (!(controllerState.getBlock() instanceof IMultiblockController)) {
            return true; 
        }
        return false;
    }

    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        // На контрапшене возвращаем смещенную общую форму двери для корректного взаимодействия лучей (raycasting).
        // Это необходимо для того, чтобы юзер мог кликнуть ПКМ по фантомной части.
        if (pLevel instanceof Level lvl && com.hbm_m.compat.ContraptionDoorState.isContraptionWorld(lvl)) {
            BlockPos controllerPos = com.hbm_m.compat.ContraptionDoorState.getControllerForPart(lvl, pPos);
            if (controllerPos != null) {
                VoxelShape masterShape = com.hbm_m.compat.ContraptionDoorState.getShape(lvl, controllerPos);
                if (masterShape != null) {
                    BlockPos vecToController = controllerPos.subtract(pPos);
                    return masterShape.move(vecToController.getX(), vecToController.getY(), vecToController.getZ());
                }
            }
            return Shapes.block(); // Fallback so it remains clickable
        }

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

        VoxelShape customShape = controller.getCustomMasterVoxelShape(controllerState);
        if (customShape != null && !customShape.isEmpty()) {
            masterShape = customShape;
        } else if (controllerBlock instanceof DoorBlock doorBlock) {
            DoorDecl decl = DoorDeclRegistry.getById(doorBlock.getDoorDeclId());

            if (decl != null && decl.isDynamicShape()) {
                masterShape = doorBlock.getShape(controllerState, pLevel, controllerPos, pContext);
            } else {
                Direction facing = controllerState.getValue(DoorBlock.FACING);
                masterShape = controller.getStructureHelper().generateShapeFromParts(facing);
            }
        } else {
            Direction facing = controllerState.getValue(HorizontalDirectionalBlock.FACING);
            masterShape = controller.getStructureHelper().generateShapeFromParts(facing);
        }

        BlockPos vecToController = controllerPos.subtract(pPos);
        return masterShape.move(vecToController.getX(), vecToController.getY(), vecToController.getZ());
    }

    @Override
    public VoxelShape getCollisionShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        if (pLevel instanceof Level lvl && com.hbm_m.compat.ContraptionDoorState.isContraptionWorld(lvl)) {
            return Shapes.empty(); // На контрапшене всю коллизию берет на себя контроллер
        }

        if (!(pLevel.getBlockEntity(pPos) instanceof IMultiblockPart part)) {
            return Shapes.block();
        }

        BlockPos controllerPos = part.getControllerPos();
        if (controllerPos == null) {
            return Shapes.block();
        }

        BlockState controllerState = pLevel.getBlockState(controllerPos);
        Block controllerBlock = controllerState.getBlock(); 

        if (!(controllerBlock instanceof IMultiblockController controller)) {
            return Shapes.block();
        }

        VoxelShape masterShape = controller.getCustomMasterVoxelShape(controllerState);

        if (masterShape != null && !masterShape.isEmpty()) {
            BlockPos vecToController = controllerPos.subtract(pPos);
            return masterShape.move(vecToController.getX(), vecToController.getY(), vecToController.getZ());
        }

        if (controllerBlock instanceof TransitionSealBlock) {
            if (pLevel.getBlockEntity(controllerPos) instanceof TransitionSealBlockEntity sealBE && sealBE.isOpen()) {
                return TransitionSealBlock.getCellCollisionShape(pLevel, controllerPos, controllerState, pPos);
            }
            return Shapes.block();
        }

        if (controllerBlock instanceof DoorBlock doorBlock) {
            BlockEntity controllerBE = pLevel.getBlockEntity(controllerPos);

            DoorDecl decl = DoorDeclRegistry.getById(doorBlock.getDoorDeclId());

            if (decl != null && decl.getStructureDefinition() != null) {
                DoorDecl.DoorStructureDefinition def = decl.getStructureDefinition();

                Direction facing = controllerState.getValue(DoorBlock.FACING);
                BlockPos worldOffset = pPos.subtract(controllerPos);
                BlockPos localOffset = MultiblockStructureHelper.rotateBack(worldOffset, facing);

                boolean isOpen;
                if (controllerBE instanceof DoorBlockEntity doorBE) {
                    isOpen = doorBE.getState() != 0;
                } else {
                    isOpen = controllerState.getValue(DoorBlock.OPEN);
                }

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

        MultiblockStructureHelper helper = controller.getStructureHelper();
        Direction facing = controllerState.getValue(HorizontalDirectionalBlock.FACING);

        BlockPos worldOffset = pPos.subtract(controllerPos);
        BlockPos localOffset = MultiblockStructureHelper.rotateBack(worldOffset, facing);
        BlockPos gridPos = localOffset.offset(helper.getControllerOffset());

        return helper.getSpecificCollisionShape(gridPos, facing);
    }

    @Override
    public boolean isPathfindable(BlockState state, BlockGetter level, BlockPos pos, PathComputationType type) {
        return switch (type) {
            case LAND, AIR -> state.getValue(PASSABLE);
            default -> false;
        };
    }

    @Override
    public void neighborChanged(BlockState pState, Level pLevel, BlockPos pPos, Block pBlock, BlockPos pFromPos, boolean pIsMoving) {
        super.neighborChanged(pState, pLevel, pPos, pBlock, pFromPos, pIsMoving);

        MultiblockStructureHelper.onNeighborChangedForPart(pLevel, pPos, pFromPos);

        if (pLevel.isClientSide()) {
            if (isOrphaned(pLevel, pPos)) {
                com.hbm_m.client.ClientRenderHandler.addOrphanedPhantomBlock(pPos);
            } else {
                com.hbm_m.client.ClientRenderHandler.removeOrphanedPhantomBlock(pPos);
            }
        }
        if (pLevel.getBlockEntity(pPos) instanceof IMultiblockPart part) {
            BlockPos controllerPos = part.getControllerPos();
            if (controllerPos != null) {
                BlockEntity be = pLevel.getBlockEntity(controllerPos);
                if (be instanceof DoorBlockEntity doorBE) {
                    doorBE.checkRedstonePower();
                } else if (be instanceof LaunchPadBaseBlockEntity launchPadBE) {
                    launchPadBE.checkRedstonePower();
                }
            }
        }
    }

    public boolean isLadder(BlockState pState, LevelReader pLevel, BlockPos pPos, LivingEntity pEntity) {
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
        if (level instanceof Level lvl && com.hbm_m.compat.ContraptionDoorState.isContraptionWorld(lvl)) {
            return Shapes.empty();
        }
        
        if (level.getBlockEntity(pos) instanceof IMultiblockPart part) {
            BlockPos ctrlPos = part.getControllerPos();
            if (ctrlPos != null) {
                Block ctrlBlock = level.getBlockState(ctrlPos).getBlock();
                if (ctrlBlock instanceof DoorBlock || ctrlBlock instanceof TransitionSealBlock) {
                    return Shapes.empty();
                }
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
                    
                    BlockPos worldOffset = pos.subtract(controllerPos);
                    BlockPos localOffset = MultiblockStructureHelper.rotateBack(worldOffset, facing);
                    BlockPos gridPos = localOffset.offset(helper.getControllerOffset());
                    
                    return helper.isFullBlock(gridPos, facing);
                }
            }
        }
        return true; 
    }

    @Override
    public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        if (pLevel.getBlockEntity(pPos) instanceof IMultiblockPart part) {
            BlockPos controllerPos = part.getControllerPos();
            if (controllerPos == null) {
                if (!pLevel.isClientSide()) {
                    pLevel.setBlock(pPos, Blocks.AIR.defaultBlockState(), 3);
                }
                return InteractionResult.sidedSuccess(pLevel.isClientSide());
            }

            BlockState controllerState = pLevel.getBlockState(controllerPos);
            if (controllerState.getBlock() instanceof IMultiblockController) {
                BlockEntity ctrlBe = pLevel.getBlockEntity(controllerPos);
                if (ctrlBe instanceof DoorBlockEntity && hasScrewdriver(pPlayer)) {
                    return InteractionResult.sidedSuccess(pLevel.isClientSide());
                }
                return controllerState.use(pLevel, pPlayer, pHand, pHit.withPosition(controllerPos));
            } else {
                if (!pLevel.isClientSide()) {
                    pLevel.setBlock(pPos, Blocks.AIR.defaultBlockState(), 3);
                }
                return InteractionResult.sidedSuccess(pLevel.isClientSide());
            }
        }
        return InteractionResult.PASS;
    }

    private static boolean hasScrewdriver(net.minecraft.world.entity.player.Player player) {
        return player.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND).getItem() == ModItems.SCREWDRIVER.get()
                || player.getItemInHand(net.minecraft.world.InteractionHand.OFF_HAND).getItem() == ModItems.SCREWDRIVER.get()
                || player.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND).getItem() == ModItems.SCREWDRIVER_DESH.get()
                || player.getItemInHand(net.minecraft.world.InteractionHand.OFF_HAND).getItem() == ModItems.SCREWDRIVER_DESH.get();
    }

    @Override
    public void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pIsMoving) {
        if (!pState.is(pNewState.getBlock())) {
            if (pLevel.getBlockEntity(pPos) instanceof IMultiblockPart partBe) {
                BlockPos controllerPos = partBe.getControllerPos();
                if (controllerPos != null && !pLevel.isClientSide()) {
                    BlockState controllerState = pLevel.getBlockState(controllerPos);
                    if (controllerState.getBlock() instanceof IMultiblockController controller) {
                        if (!MultiblockStructureHelper.isDestroying() && !MultiblockStructureHelper.isRepairing() && controller.shouldDestroyOnPartRemoved()) {
                            if (controllerState.hasProperty(HorizontalDirectionalBlock.FACING)) {
                                Direction facing = controllerState.getValue(HorizontalDirectionalBlock.FACING);
                                controller.getStructureHelper().destroyStructure(pLevel, controllerPos, facing);
                            }
                            pLevel.destroyBlock(controllerPos, true);
                        }
                    }
                }
            }
        }
        super.onRemove(pState, pLevel, pPos, pNewState, pIsMoving);
    }

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof IMultiblockPart partBe) {
            BlockPos controllerPos = partBe.getControllerPos();
            if (controllerPos != null) {
                BlockState controllerState = level.getBlockState(controllerPos);
                if (controllerState.getBlock() instanceof IMultiblockController) {
                    boolean dropController = !player.getAbilities().instabuild;
                    level.destroyBlock(controllerPos, dropController);
                }
            }
        }
        super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        if (level.getBlockEntity(pos) instanceof IMultiblockPart part) {
            BlockPos controllerPos = part.getControllerPos();
            if (controllerPos != null) {
                BlockState controllerState = level.getBlockState(controllerPos);
                return controllerState.getBlock().getCloneItemStack(level, controllerPos, controllerState);
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public float getDestroyProgress(BlockState pState, Player pPlayer, BlockGetter pLevel, BlockPos pPos) {
        if (isOrphaned(pLevel, pPos)) {
            return 1.0f;
        }
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

    public boolean addLandingEffects(BlockState state1, ServerLevel level, BlockPos pos, BlockState state2, LivingEntity entity, int numberOfParticles) {
        if (level.getBlockEntity(pos) instanceof IMultiblockPart part) {
            BlockPos controllerPos = part.getControllerPos();
            if (controllerPos != null) {
                BlockState controllerState = level.getBlockState(controllerPos);
                
                if (!controllerState.isAir()) {
                    level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, controllerState),
                            entity.getX(), entity.getY(), entity.getZ(),
                            numberOfParticles, 0.0D, 0.0D, 0.0D, 0.15D);
                    
                    return true; 
                }
            }
        }
        return false;
    }

    public boolean addRunningEffects(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide && level.getBlockEntity(pos) instanceof IMultiblockPart part) {
            BlockPos controllerPos = part.getControllerPos();
            if (controllerPos != null) {
                BlockState controllerState = level.getBlockState(controllerPos);
                
                if (!controllerState.isAir()) {
                    if (level.random.nextFloat() < 0.1F) { 
                        double x = entity.getX() + (level.random.nextDouble() - 0.5D) * (double)entity.getBbWidth();
                        double y = entity.getY() + 0.1D;
                        double z = entity.getZ() + (level.random.nextDouble() - 0.5D) * (double)entity.getBbWidth();
                        
                        level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, controllerState), 
                            x, y, z, 
                            -entity.getDeltaMovement().x * 4.0D, 1.5D, -entity.getDeltaMovement().z * 4.0D);
                    }
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
        return resolveRadarRedPower(level, pos);
    }

    @Override
    public int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
        return getSignal(state, level, pos, side);
    }

    private static int resolveRadarRedPower(BlockGetter level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof IMultiblockPart part)) {
            return 0;
        }
        if (part.getPartRole() != PartRole.ENERGY_CONNECTOR) {
            return 0;
        }

        BlockPos controllerPos = part.getControllerPos();
        if (controllerPos == null) {
            return 0;
        }

        BlockEntity controller = level.getBlockEntity(controllerPos);
        if (controller instanceof MachineRadarBlockEntity radar) {
            return radar.getRedPower();
        }
        return 0;
    }
}