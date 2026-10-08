package com.hbm_m.block.machines;

import java.util.Map;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineDerrickBlockEntity;
import com.hbm_m.interfaces.IMultiblockController;
import com.hbm_m.multiblock.MultiblockSideTuples;
import com.hbm_m.multiblock.MultiblockStructureHelper;
import com.hbm_m.multiblock.PartRole;

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
import dev.architectury.registry.menu.MenuRegistry;

public class MachineDerrickBlock extends BaseEntityBlock implements IMultiblockController {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    private final MultiblockStructureHelper structureHelper;

    public MachineDerrickBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
        this.structureHelper = defineStructureNew();
    }

    private static MultiblockStructureHelper defineStructureNew() {
       
        String[] layer1 = {
            "O O",
            " C ",
            "O O"        
        };
        
        String[] layers = {
            "OOO",
            "OOO",
            "OOO"        
        };
        Map<Character, PartRole> roleMap = Map.of(
            'O', PartRole.DEFAULT,
            'B', PartRole.UNIVERSAL_CONNECTOR,
            'C', PartRole.CONTROLLER
        );
        Map<Character, Supplier<BlockState>> symbolMap = Map.of();
        Map<Character, boolean[]> energySideMap = Map.of(
            'B', MultiblockSideTuples.energy(true, true, true, true, true, false),
            'C', MultiblockSideTuples.energy(true, true, true, true, true, false)
        );
        Map<Character, boolean[]> fluidSideMap = Map.of(
            'B', MultiblockSideTuples.fluid(true, true, true, true, true, false),
            'C', MultiblockSideTuples.fluid(true, true, true, true, true, false)
        );

        return MultiblockStructureHelper.createFromLayersWithRolesAndSides(
            new String[][]{layer1, layers, layers, layers, layers, layers, layers, layers, layers, layers},
            symbolMap,
            () -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState(),
            roleMap,
            null,
            energySideMap,
            fluidSideMap
        );
    }

    @Override
    public MultiblockStructureHelper getStructureHelper() {
        return this.structureHelper;
    }

    @Override
    public PartRole getPartRole(BlockPos localOffset) {
        return structureHelper.resolvePartRole(localOffset, this);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachineDerrickBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.DERRICK_BE.get(), MachineDerrickBlockEntity::tick);
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
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide()) {
            structureHelper.destroyStructure(level, pos, state.getValue(FACING));
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    /** audit10: Explosionen auf Teilzellen laufen wie im Original ueber findCore hierher. */
    @Override
    public boolean forwardPartExplosions() {
        return true;
    }

    /**
     * audit10: 1:1 {@code MachineOilWell.onBlockExploded} - der Turm wird zerstoert; war noch Oel oder Gas in den
     * Tanks, explodiert er zusaetzlich (VNT 15, Aufloesung 24, ohne Drops) mit grosser Explosionswolke.
     */
    @Override
    public void onBlockExploded(BlockState state, Level world, BlockPos pos, net.minecraft.world.level.Explosion explosion) {
        if (!(world.getBlockEntity(pos) instanceof MachineDerrickBlockEntity well)) {
            super.onBlockExploded(state, world, pos, explosion);
            return;
        }

        boolean filled = well.tanks[0].getFill() > 0 || well.tanks[1].getFill() > 0;

        world.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
        wasExploded(world, pos, explosion);

        if (filled) {
            well.tanks[0].setFill(0);
            well.tanks[1].setFill(0);

            com.hbm_m.explosion.vanillant.ExplosionVNT xnt = new com.hbm_m.explosion.vanillant.ExplosionVNT(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 15F);
            xnt.setBlockAllocator(new com.hbm_m.explosion.vanillant.standard.BlockAllocatorStandard(24));
            xnt.setBlockProcessor(new com.hbm_m.explosion.vanillant.standard.BlockProcessorStandard().setNoDrop());
            xnt.setEntityProcessor(new com.hbm_m.explosion.vanillant.standard.EntityProcessorStandard());
            xnt.setPlayerProcessor(new com.hbm_m.explosion.vanillant.standard.PlayerProcessorStandard());
            xnt.explode();

            if (world instanceof net.minecraft.server.level.ServerLevel server)
                com.hbm_m.particle.helper.ExplosionCreator.composeEffect(server, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 10, 2F, 0.5F, 25F, 5, 8, 20, 0.75F, 1F, -2F, 150);
        }
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.isShiftKeyDown()) return InteractionResult.sidedSuccess(level.isClientSide()); // Original standardOpenBehavior: geschlichen true ohne GUI
        return openMenu(state, level, pos, player, hand, hit);
    }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isShiftKeyDown()) return InteractionResult.sidedSuccess(level.isClientSide()); // Original standardOpenBehavior: geschlichen true ohne GUI
        return openMenu(state, level, pos, player, InteractionHand.MAIN_HAND, hit);
    }
    *///?}

    private InteractionResult openMenu(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide()) {
            BlockEntity entity = level.getBlockEntity(pos);
            if (entity instanceof MenuProvider menuProvider) {
                MenuRegistry.openExtendedMenu((ServerPlayer) player, menuProvider, buf -> buf.writeBlockPos(pos));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        // w16b: nur die Kernzelle (Raycast pro Zelle wie Original); Umriss der ganzen Maschine: MultiblockOutlineForge
        return structureHelper.getControllerCellShape(state.getValue(FACING));
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return structureHelper.getSpecificPartShape(structureHelper.getControllerOffset(), state.getValue(FACING));
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        if (!structureHelper.isFullBlock(structureHelper.getControllerOffset(), state.getValue(FACING))) {
            return Shapes.empty();
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
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<MachineDerrickBlock> CODEC = simpleCodec(MachineDerrickBlock::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return CODEC;
    }
    *///?}
}
