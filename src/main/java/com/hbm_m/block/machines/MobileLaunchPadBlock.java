package com.hbm_m.block.machines;

import java.util.Map;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.bomb.IBomb;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.LaunchPadBaseBlockEntity;
import com.hbm_m.blockentity.machines.MobileLaunchPadBlockEntity;
import com.hbm_m.interfaces.IDetonatable;
import com.hbm_m.interfaces.IMultiblockController;
import com.hbm_m.multiblock.MultiblockStructureHelper;
import com.hbm_m.multiblock.PartRole;

import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

/**
 * Mobile Startrampe: der Iskander-M TEL als Multiblock.
 *
 * <p>Der Aufbau folgt {@link LaunchPadBlock}, nur der Grundriss ist ein anderer. Das Fahrzeug ist
 * 13,7 Bloecke lang und 3,7 breit, der Multiblock deckt das mit 15x5 Feldern ab; der Controller
 * steht in der Mitte, Laengsachse in Blickrichtung. Die Teile sind flache Platten (1/16 hoch),
 * damit man bis an die Raeder herantreten kann - das Fahrzeug selbst ist Modell, keine Kollision.
 */
public class MobileLaunchPadBlock extends BaseEntityBlock implements IMultiblockController, IBomb, IDetonatable {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    private final MultiblockStructureHelper structureHelper;

    public MobileLaunchPadBlock(Properties pProperties) {
        super(pProperties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
        this.structureHelper = defineStructure();
    }

    @Override
    public boolean onDetonate(Level level, BlockPos pos, BlockState state, Player player) {
        if (level.isClientSide) {
            return false;
        }
        BombReturnCode result = explode(level, pos);
        return result != null && result.wasSuccessful();
    }

    @Override
    public void onPlace(BlockState pState, Level pLevel, BlockPos pPos, BlockState pOldState, boolean pIsMoving) {
        super.onPlace(pState, pLevel, pPos, pOldState, pIsMoving);
        if (!pLevel.isClientSide() && !pState.is(pOldState.getBlock())) {
            placeMultiblockStructure(pLevel, pPos, pState);
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
                Direction facing = state.getValue(FACING);

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

                getStructureHelper().destroyStructure(level, pos, facing);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

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
    @Nullable @Override public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) { return new MobileLaunchPadBlockEntity(pPos, pState); }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockState pState, BlockEntityType<T> pType) {
        return createTickerHelper(pType, ModBlockEntities.MOBILE_LAUNCH_PAD_BE.get(), MobileLaunchPadBlockEntity::tick);
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        return openMenu(pLevel, pPos, pPlayer);
    }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, BlockHitResult pHit) {
        return openMenu(pLevel, pPos, pPlayer);
    }
    *///?}

    private InteractionResult openMenu(Level pLevel, BlockPos pPos, Player pPlayer) {
        if (!pLevel.isClientSide()) {
            if (pLevel.getBlockEntity(pPos) instanceof MenuProvider provider) {
                MenuRegistry.openExtendedMenu((ServerPlayer) pPlayer, provider, buf -> buf.writeBlockPos(pPos));
            }
        }
        return InteractionResult.sidedSuccess(pLevel.isClientSide());
    }

    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        // w16b: nur die Kernzelle (Raycast pro Zelle wie Original); Umriss der ganzen Maschine: MultiblockOutlineForge
        MultiblockStructureHelper helper = getStructureHelper();
        if (helper != null) {
            return helper.getControllerCellShape(pState.getValue(FACING));
        }
        return Shapes.block();
    }

    @Override public MultiblockStructureHelper getStructureHelper() { return this.structureHelper; }

    /**
     * Grundriss 15 (Laenge, Z) x 5 (Breite, X), eine Lage hoch. Der Controller 'C' steht in der
     * Mitte, die Ecken sind Universalanschluesse fuer Strom und Treibstoff.
     */
    private static MultiblockStructureHelper defineStructure() {
        String[] layer0 = {
            "BAAAB",
            "AAAAA",
            "AAAAA",
            "AAAAA",
            "AAAAA",
            "AAAAA",
            "AAAAA",
            "AACAA",
            "AAAAA",
            "AAAAA",
            "AAAAA",
            "AAAAA",
            "AAAAA",
            "AAAAA",
            "BAAAB"
        };

        Map<Character, PartRole> roleMap = Map.of(
            'A', PartRole.DEFAULT,
            'B', PartRole.UNIVERSAL_CONNECTOR,
            'C', PartRole.CONTROLLER
        );

        Map<Character, Supplier<BlockState>> symbolMap = Map.of();

        // Flache Platte: das Fahrzeug steht darauf, man kann aber bis an die Raeder herantreten.
        VoxelShape plate = Block.box(0, 0, 0, 16, 1, 16);
        Map<Character, VoxelShape> shapeMap = Map.of(
            'C', plate,
            'A', plate,
            'B', plate
        );

        return MultiblockStructureHelper.createFromLayersWithRoles(
            new String[][]{layer0},
            symbolMap,
            () -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState(),
            roleMap,
            shapeMap,
            shapeMap
        );
    }

    @Override
    public PartRole getPartRole(BlockPos localOffset) {
        if (structureHelper != null) {
            return structureHelper.resolvePartRole(localOffset, this);
        }
        return PartRole.DEFAULT;
    }

    @Override
    public float getShadeBrightness(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
        return 1.0F;
    }

    @Override
    public boolean propagatesSkylightDown(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
        return true;
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<MobileLaunchPadBlock> CODEC = simpleCodec(MobileLaunchPadBlock::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return CODEC;
    }
    *///?}
}
