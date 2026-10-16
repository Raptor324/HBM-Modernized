package com.hbm_m.block.machines;

import java.util.Map;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachinePumpElectricBlockEntity;
import com.hbm_m.blockentity.machines.MachinePumpSteamBlockEntity;
import com.hbm_m.interfaces.IMultiblockController;
import com.hbm_m.multiblock.MultiblockStructureHelper;
import com.hbm_m.multiblock.PartRole;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 1:1 port of {@code MachinePump} (both {@code pump_steam}/{@code pump_electric} variants), on this
 * repo's own {@link IMultiblockController} framework. Footprint: 3 wide x 1 deep, 1 level, controller
 * centered - simplified-but-proportional replacement for the original's dummy-block dimension array
 * {@code {3,0,1,1,1,1}} (matches the same adaptation already used by {@code MachineBoilerBlock}).
 * No GUI (original only shows status via hover tooltip).
 */
public class MachinePumpBlock extends BaseEntityBlock implements IMultiblockController, com.hbm_m.interfaces.ILookOverlay {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    private final MultiblockStructureHelper structureHelper;
    private final boolean electric;

    public MachinePumpBlock(Properties properties, boolean electric) {
        super(properties);
        this.electric = electric;
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
        this.structureHelper = defineStructure();
    }

    private static MultiblockStructureHelper defineStructure() {
        String[] layer = { "OCO" };

        Map<Character, PartRole> roleMap = Map.of(
                'O', PartRole.DEFAULT,
                'C', PartRole.CONTROLLER
        );

        Map<Character, Supplier<BlockState>> symbolMap = Map.of();

        return MultiblockStructureHelper.createFromLayersWithRoles(
                new String[][] { layer },
                symbolMap,
                () -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState(),
                roleMap,
                null,
                null
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

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return structureHelper.generateShapeFromParts(state.getValue(FACING));
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

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return electric
                ? new MachinePumpElectricBlockEntity(pos, state)
                : new MachinePumpSteamBlockEntity(pos, state);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> type
    ) {
        if (electric) {
            return createTickerHelper(
                    type,
                    ModBlockEntities.MACHINE_PUMP_ELECTRIC_BE.get(),
                    (lvl, pos, st, be) -> MachinePumpElectricBlockEntity.tick(lvl, pos, st, (MachinePumpElectricBlockEntity) be)
            );
        }
        return createTickerHelper(
                type,
                ModBlockEntities.MACHINE_PUMP_STEAM_BE.get(),
                (lvl, pos, st, be) -> MachinePumpSteamBlockEntity.tick(lvl, pos, st, (MachinePumpSteamBlockEntity) be)
        );
    }

    /**
     * Порт {@code MachinePump.printHook}: паровые баки (пар →, ЛПС ←, вода ←) либо заряд
     * электрической версии (→ HE) и вода (←), плюс мигающие предупреждения о высоте
     * (ядро выше 70) и отсутствии грунта — как в оригинале.
     */
    @Override
    public void printHook(net.minecraft.client.gui.GuiGraphics guiGraphics, Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof MachinePumpSteamBlockEntity) && !(be instanceof MachinePumpElectricBlockEntity)) return;

        java.util.List<net.minecraft.network.chat.Component> text = new java.util.ArrayList<>();

        if (be instanceof MachinePumpSteamBlockEntity pump) {
            addTankLine(text, "-> ", net.minecraft.ChatFormatting.GREEN, pump.getSteamTank());
            addTankLine(text, "<- ", net.minecraft.ChatFormatting.RED, pump.getLpsTank());
            addTankLine(text, "<- ", net.minecraft.ChatFormatting.RED, pump.getWaterTank());
        }

        if (be instanceof MachinePumpElectricBlockEntity pump) {
            text.add(net.minecraft.network.chat.Component.literal("-> ").withStyle(net.minecraft.ChatFormatting.GREEN)
                    .append(net.minecraft.network.chat.Component.literal(
                            String.format(java.util.Locale.US, "%,d", pump.getEnergyStored())
                                    + " / " + String.format(java.util.Locale.US, "%,d", MachinePumpElectricBlockEntity.MAX_POWER) + "HE")
                            .withStyle(net.minecraft.ChatFormatting.WHITE)));
            addTankLine(text, "<- ", net.minecraft.ChatFormatting.RED, pump.getWaterTank());
        }

        if (pos.getY() > 70) {
            text.add(blinking("! ! ! ALTITUDE ! ! !"));
        }

        boolean onGround = (be instanceof MachinePumpSteamBlockEntity steam) ? steam.onGround
                : (be instanceof MachinePumpElectricBlockEntity electric) ? electric.onGround : true;
        if (!onGround) {
            text.add(blinking("! ! ! NO VALID GROUND ! ! !"));
        }

        com.hbm_m.interfaces.ILookOverlay.printGeneric(guiGraphics,
                net.minecraft.network.chat.Component.translatable(getDescriptionId()), 0xffff00, 0x404000, text);
    }

    /** Строка "стрелка имя: fill / max mB" — формат MachinePump.printHook (стрелка цветная, текст белый). */
    private static void addTankLine(java.util.List<net.minecraft.network.chat.Component> text, String arrow,
            net.minecraft.ChatFormatting arrowColor, com.hbm_m.inventory.fluid.tank.FluidTank tank) {
        String body = com.hbm_m.inventory.fluid.FluidType.forFluid(tank.getTankType()).getLocalizedName().getString()
                + ": " + String.format(java.util.Locale.US, "%,d", tank.getFill())
                + " / " + String.format(java.util.Locale.US, "%,d", tank.getMaxFill()) + "mB";
        text.add(net.minecraft.network.chat.Component.literal(arrow).withStyle(arrowColor)
                .append(net.minecraft.network.chat.Component.literal(body).withStyle(net.minecraft.ChatFormatting.WHITE)));
    }

    /** Мигающая строка (оригинальный {@code "&[" + (getBlink() ? красный : жёлтый) + "&]"}). */
    private static net.minecraft.network.chat.Component blinking(String message) {
        int color = System.currentTimeMillis() % 1000 < 500 ? 0xff0000 : 0xffff00;
        return net.minecraft.network.chat.Component.literal(message)
                .withStyle(net.minecraft.network.chat.Style.EMPTY
                        .withColor(net.minecraft.network.chat.TextColor.fromRgb(color)));
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!state.is(oldState.getBlock()) && !level.isClientSide()) {
            structureHelper.placeStructure(level, pos, state.getValue(FACING), this);
        }
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                  Player player, InteractionHand hand, BlockHitResult hit) {

        return InteractionResult.PASS; // Kein GUI im Original.
        }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {

        return InteractionResult.PASS; // Kein GUI im Original.
        }
    *///?}


    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos,
                          BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            if (!level.isClientSide()) {
                structureHelper.destroyStructure(level, pos, state.getValue(FACING));
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    //? if >1.20.1 {
    /*@Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return simpleCodec(p -> new MachinePumpBlock(p, this.electric));
    }
    *///?}

}
