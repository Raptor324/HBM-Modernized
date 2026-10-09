package com.hbm_m.block.machines;

import java.util.Map;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineChungusBlockEntity;
import com.hbm_m.interfaces.IMultiblockController;
import com.hbm_m.multiblock.MultiblockStructureHelper;
import com.hbm_m.multiblock.PartRole;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Chungus / Leviathan Steam Turbine - das Endgame-Upgrade zur Industrial Turbine.
 * Multiblock-Struktur rekonstruiert aus den Original-Dateien MachineChungus.java,
 * BlockDummyable.java und MultiblockHandlerXR.java (siehe Plan-Dokumentation):
 * Hauptkörper (5 breit x 4 hoch x 4 tief vor dem Controller) + schmalere Kappe oben drauf +
 * ein nach hinten auslaufender, sich verjüngender Heck-Schacht (insgesamt 10 Blöcke hinter
 * dem Controller) mit dem Energie-Port an der Spitze, plus 2 seitliche und 1 vorderer
 * Fluid-Port (UNIVERSAL_CONNECTOR).
 */
public class MachineChungusBlock extends BaseEntityBlock implements IMultiblockController {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    private final MultiblockStructureHelper structureHelper;

    public MachineChungusBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
        this.structureHelper = defineStructure();
    }

    private static MultiblockStructureHelper defineStructure() {
        // 1:1 MachineChungus: getDimensions {3,0,0,3,2,2}, getOffset 3 (Kern hinten im Gehaeuse, Gehaeuse zum Spieler),
        // fillSpace-Zusatzquader {4,-4,0,3,1,1}, {3,0,6,-1,1,1}, {2,0,10,-7,1,1} (Schacht nach hinten),
        // Dummy + Frontanschluss bei Klick + dir auf Hoehe 2 (= Kern + 4 dir), Heckanschluss Kern - 10 dir,
        // Seitenanschluesse Kern +- 2 rot.
        return com.hbm_m.multiblock.DummyableStructureBuilder.create()
                .box(3, 0, 0, 3, 2, 2)
                .box(4, -4, 0, 3, 1, 1)
                .box(3, 0, 6, -1, 1, 1)
                .box(2, 0, 10, -7, 1, 1)
                .extra(4, 2, 0)
                .extra(-10, 0, 0)
                .extra(0, 0, 2)
                .extra(0, 0, -2)
                .placementOffset(3)
                .build(() -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        // Original RenderChungus (BER)
        return RenderShape.ENTITYBLOCK_ANIMATED;
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

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);

        if (!state.is(oldState.getBlock()) && !level.isClientSide()) {
            Direction facing = state.getValue(FACING);
            structureHelper.placeStructure(level, pos, facing, this);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock() && !level.isClientSide()) {
            Direction facing = state.getValue(FACING);

            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof MachineChungusBlockEntity chungus) {
                chungus.drops();
            }

            structureHelper.destroyStructure(level, pos, facing);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachineChungusBlockEntity(pos, state);
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.isShiftKeyDown()) return InteractionResult.PASS; // Original: geschlichen false
        return handleUse(state, level, pos, player, hand, hit);
    }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isShiftKeyDown()) return InteractionResult.PASS; // Original: geschlichen false
        return handleUse(state, level, pos, player, InteractionHand.MAIN_HAND, hit);
    }
    *///?}

    private InteractionResult handleUse(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide()) {
            if (level.getBlockEntity(pos) instanceof MachineChungusBlockEntity chungus) {
                // 1:1: der Hebel schaltet die Dampfstufe - im Betrieb weigert er sich.
                if (chungus.pullLever()) {
                    level.playSound(null, pos, net.minecraft.sounds.SoundEvents.LEVER_CLICK,
                            net.minecraft.sounds.SoundSource.BLOCKS, 1.5F, 1.0F);
                    player.displayClientMessage(Component.translatable("chat.hbm_m.chungus.stage",
                            chungus.getSteamTank().getStoredFluid().getFluidType().getDescription()), true);
                } else {
                    player.displayClientMessage(
                            Component.translatable("chat.hbm_m.chungus.busy")
                                    .withStyle(net.minecraft.ChatFormatting.RED), true);
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.MACHINE_CHUNGUS_BE.get(), MachineChungusBlockEntity::tick);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        // w16b: nur die Kernzelle (Raycast pro Zelle wie Original); Umriss der ganzen Maschine: MultiblockOutlineForge
        return structureHelper.getControllerCellShape(state.getValue(FACING));
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

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<MachineChungusBlock> CODEC = simpleCodec(MachineChungusBlock::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return CODEC;
    }
    *///?}

    /** Original {@code addInformation}: {@code addStandardInfo} (Umschalttaste zeigt {@code .desc}). */
    @Override
    //? if < 1.21.1 {
    public void appendHoverText(net.minecraft.world.item.ItemStack stack, @org.jetbrains.annotations.Nullable net.minecraft.world.level.BlockGetter level,
                                java.util.List<net.minecraft.network.chat.Component> list, net.minecraft.world.item.TooltipFlag flag) {
    //?} else {
    /*public void appendHoverText(net.minecraft.world.item.ItemStack stack, net.minecraft.world.item.Item.TooltipContext hbmTooltipCtx, java.util.List<net.minecraft.network.chat.Component> list, net.minecraft.world.item.TooltipFlag flag) {
    *///?}
        com.hbm_m.util.StandardInfo.add(list, getDescriptionId() + ".desc");
    }
}
