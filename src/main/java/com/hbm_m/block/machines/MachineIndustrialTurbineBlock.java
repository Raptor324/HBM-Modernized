package com.hbm_m.block.machines;

import java.util.Map;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineIndustrialTurbineBlockEntity;
import com.hbm_m.interfaces.IMultiblockController;
import com.hbm_m.multiblock.MultiblockStructureHelper;
import com.hbm_m.multiblock.PartRole;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Industrial Turbine - converts steam to energy (HE).
 * Multiblock structure: 3x3x7 (в плане 3x7, высота 3 блока).
 * Model parts: Turbine (body), Flywheel (animated), Gauge
 */
public class MachineIndustrialTurbineBlock extends BaseEntityBlock implements IMultiblockController, com.hbm_m.interfaces.ILookOverlay {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    private final MultiblockStructureHelper structureHelper;

    public MachineIndustrialTurbineBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
        this.structureHelper = defineStructure();
    }

    private static MultiblockStructureHelper defineStructure() {
        // 1:1 MachineIndustrialTurbine: getDimensions {2,0,3,3,1,1}, getOffset 3 (Kern in der Mitte, 3x3x7 laengs dir);
        // makeExtra (fluid+power) bei dir*3 +- rot, -dir +- rot, dir*3 / -dir auf Hoehe 2, -dir*3 auf Hoehe 1.
        return com.hbm_m.multiblock.DummyableStructureBuilder.create()
                .box(2, 0, 3, 3, 1, 1)
                .extra(3, 0, 1)
                .extra(3, 0, -1)
                .extra(-1, 0, 1)
                .extra(-1, 0, -1)
                .extra(3, 2, 0)
                .extra(-1, 2, 0)
                .extra(-3, 1, 0)
                .placementOffset(3)
                .build(() -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        // Original RenderIndustrialTurbine (BER)
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
            placeMultiblockStructure(level, pos, state);
        }
    }


    @Override
    public boolean canSurvive(BlockState state, net.minecraft.world.level.LevelReader level, BlockPos pos) {
        return super.canSurvive(state, level, pos) && canSurviveMultiblockPlacement(state, level, pos);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock() && !level.isClientSide()) {
            Direction facing = state.getValue(FACING);

            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof MachineIndustrialTurbineBlockEntity turbine) {
                turbine.drops();
            }

            structureHelper.destroyStructure(level, pos, facing);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachineIndustrialTurbineBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.INDUSTRIAL_TURBINE_BE.get(), MachineIndustrialTurbineBlockEntity::tick);
    }

    //? if < 1.21.1 {
    @Override
    public net.minecraft.world.InteractionResult use(BlockState state, Level level, BlockPos pos,
            net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand,
            net.minecraft.world.phys.BlockHitResult hit) {
        if (player.isShiftKeyDown()) return net.minecraft.world.InteractionResult.PASS; // Original: geschlichen false
        return pullLever(level, pos, player);
    }
    //?} else {
    /*@Override
    protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            net.minecraft.world.entity.player.Player player, net.minecraft.world.phys.BlockHitResult hit) {
        if (player.isShiftKeyDown()) return net.minecraft.world.InteractionResult.PASS; // Original: geschlichen false
        return pullLever(level, pos, player);
    }
    *///?}

    /** 1:1-Port von {@code onBlockActivated}: der Hebel schaltet die Dampfstufe. */
    private net.minecraft.world.InteractionResult pullLever(Level level, BlockPos pos,
            net.minecraft.world.entity.player.Player player) {

        if (!level.isClientSide()
                && level.getBlockEntity(pos) instanceof MachineIndustrialTurbineBlockEntity turbine) {

            if (turbine.pullLever()) {
                level.playSound(null, pos, net.minecraft.sounds.SoundEvents.LEVER_CLICK,
                        net.minecraft.sounds.SoundSource.BLOCKS, 1.5F, 1.0F);
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                        "chat.hbm_m.chungus.stage",
                        turbine.getSteamTank().getStoredFluid().getFluidType().getDescription()), true);
            } else {
                player.displayClientMessage(net.minecraft.network.chat.Component
                        .translatable("chat.hbm_m.chungus.busy")
                        .withStyle(net.minecraft.ChatFormatting.RED), true);
            }
        }
        return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide());
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
    /*public static final com.mojang.serialization.MapCodec<MachineIndustrialTurbineBlock> CODEC = simpleCodec(MachineIndustrialTurbineBlock::new);

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

    private static final String[] OVERLAY_BLOCKS = new String[] {"\u2596 ", "\u2598 ", " \u2598", " \u2596"};

    @Override
    public void printHook(net.minecraft.client.gui.GuiGraphics guiGraphics, Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof MachineIndustrialTurbineBlockEntity turbine)) return;

        var tankInput = turbine.getSteamTank();
        var tankOutput = turbine.getSpentSteamTank();

        net.minecraft.world.level.material.Fluid inputType = tankInput.getTankType();
        net.minecraft.world.level.material.Fluid outputType = com.hbm_m.inventory.fluid.ModFluids.NONE.getSource();
        com.hbm_m.inventory.fluid.trait.FT_Coolable trait =
                com.hbm_m.inventory.fluid.FluidType.getTrait(inputType, com.hbm_m.inventory.fluid.trait.FT_Coolable.class);
        if (trait != null) {
            outputType = trait.coolsTo;
        }

        double spin = turbine.getSpin();
        int color = ((int) (0xFF - 0xFF * spin)) << 16 | ((int) (0xFF * spin) << 8);
        int time = (int) ((level.getGameTime() / 4) % 4);

        java.util.List<net.minecraft.network.chat.Component> text = new java.util.ArrayList<>();
        text.add(net.minecraft.network.chat.Component.literal("-> ").withStyle(net.minecraft.ChatFormatting.GREEN)
                .append(com.hbm_m.inventory.fluid.FluidType.forFluid(inputType).getLocalizedName().copy().withStyle(net.minecraft.ChatFormatting.RESET))
                .append(net.minecraft.network.chat.Component.literal(
                        ": " + String.format(java.util.Locale.US, "%,d", tankInput.getFill())
                        + "/" + String.format(java.util.Locale.US, "%,d", tankInput.getMaxFill()) + "mB")
                        .withStyle(net.minecraft.ChatFormatting.RESET)));
        text.add(net.minecraft.network.chat.Component.literal("<- ").withStyle(net.minecraft.ChatFormatting.RED)
                .append(com.hbm_m.inventory.fluid.FluidType.forFluid(outputType).getLocalizedName().copy().withStyle(net.minecraft.ChatFormatting.RESET))
                .append(net.minecraft.network.chat.Component.literal(
                        ": " + String.format(java.util.Locale.US, "%,d", tankOutput.getFill())
                        + "/" + String.format(java.util.Locale.US, "%,d", tankOutput.getMaxFill()) + "mB")
                        .withStyle(net.minecraft.ChatFormatting.RESET)));
        text.add(net.minecraft.network.chat.Component.literal("<- ").withStyle(net.minecraft.ChatFormatting.RED)
                .append(net.minecraft.network.chat.Component.literal(
                        com.hbm_m.util.BobMathUtil.getShortNumber(turbine.powerBuffer) + "HE (")
                        .withStyle(net.minecraft.ChatFormatting.WHITE))
                .append(net.minecraft.network.chat.Component.literal(
                        OVERLAY_BLOCKS[turbine.powerBuffer <= 0 ? 0 : time] + (int) Math.round(spin * 100) + "%")
                        .withStyle(s -> s.withColor(color)))
                .append(net.minecraft.network.chat.Component.literal(")").withStyle(net.minecraft.ChatFormatting.WHITE)));

        com.hbm_m.interfaces.ILookOverlay.printGeneric(guiGraphics,
                net.minecraft.network.chat.Component.translatable(getDescriptionId()), 0xffff00, 0x404000, text);
    }

    // Verbatim port of the original MachineIndustrialTurbine.getAllPorts.
    @Override
    public int[][] getAllPorts(Direction facing) {
        return rotatePorts(new int[][] {
            {1, 0, 3, Direction.EAST.get3DDataValue()},
            {-1, 0, 3, Direction.WEST.get3DDataValue()},
            {1, 0, -1, Direction.EAST.get3DDataValue()},
            {-1, 0, -1, Direction.WEST.get3DDataValue()},
            {0, 2, 3, Direction.UP.get3DDataValue()},
            {0, 2, -1, Direction.UP.get3DDataValue()},
            {0, 1, -3, Direction.NORTH.get3DDataValue()}
        }, facing);
    }
}
