package com.hbm_m.block.machines;

import java.util.Map;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineIndustrialBoilerBlockEntity;
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
public class MachineIndustrialBoilerBlock extends BaseEntityBlock implements IMultiblockController, com.hbm_m.interfaces.ILookOverlay {

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
            if (blockEntity instanceof com.hbm_m.blockentity.BaseMachineBlockEntity be) {
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

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return openMenu(state, level, pos, player, hand, hit);
    }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return openMenu(state, level, pos, player, InteractionHand.MAIN_HAND, hit);
    }
    *///?}

    /** Original {@code MachineHeatBoilerIndustrial.onBlockActivated}: kein GUI, nur Fluidtyp per Fluid-ID. */
    private InteractionResult openMenu(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide() && !player.isShiftKeyDown()) {
            net.minecraft.world.item.ItemStack held = player.getItemInHand(hand);
            if (!held.isEmpty() && held.getItem() instanceof com.hbm_m.item.liquids.FluidIdentifierItem) {
                if (!(level.getBlockEntity(pos) instanceof MachineIndustrialBoilerBlockEntity boiler)) return InteractionResult.PASS;

                net.minecraft.world.level.material.Fluid type = com.hbm_m.item.liquids.FluidIdentifierItem.resolvePrimaryForTank(held);
                com.hbm_m.inventory.fluid.trait.FT_Heatable trait = type == null ? null
                        : com.hbm_m.inventory.fluid.FluidType.getTrait(type, com.hbm_m.inventory.fluid.trait.FT_Heatable.class);

                if (trait != null && trait.getEfficiency(com.hbm_m.inventory.fluid.trait.FT_Heatable.HeatingType.BOILER) > 0) {
                    boiler.tanks[0].setTankType(type);
                    boiler.setChanged();
                    player.displayClientMessage(net.minecraft.network.chat.Component.literal("Changed type to ").withStyle(net.minecraft.ChatFormatting.YELLOW)
                            .append(com.hbm_m.inventory.fluid.FluidType.forFluid(type).getLocalizedName())
                            .append(net.minecraft.network.chat.Component.literal("!")), false);
                }
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void printHook(net.minecraft.client.gui.GuiGraphics g, Level world, BlockPos pos) {
        if (!(world.getBlockEntity(pos) instanceof MachineIndustrialBoilerBlockEntity boiler)) return;

        java.util.List<net.minecraft.network.chat.Component> text = new java.util.ArrayList<>();
        text.add(net.minecraft.network.chat.Component.literal(String.format(java.util.Locale.US, "%,d", boiler.heat) + "TU"));
        text.add(net.minecraft.network.chat.Component.literal("-> ").withStyle(net.minecraft.ChatFormatting.GREEN)
                .append(net.minecraft.network.chat.Component.literal("").withStyle(net.minecraft.ChatFormatting.RESET)
                .append(com.hbm_m.inventory.fluid.FluidType.forFluid(boiler.tanks[0].getTankType()).getLocalizedName())
                .append(": " + String.format(java.util.Locale.US, "%,d", boiler.tanks[0].getFill()) + " / " + String.format(java.util.Locale.US, "%,d", boiler.tanks[0].getMaxFill()) + "mB")));
        text.add(net.minecraft.network.chat.Component.literal("<- ").withStyle(net.minecraft.ChatFormatting.RED)
                .append(net.minecraft.network.chat.Component.literal("").withStyle(net.minecraft.ChatFormatting.RESET)
                .append(com.hbm_m.inventory.fluid.FluidType.forFluid(boiler.tanks[1].getTankType()).getLocalizedName())
                .append(": " + String.format(java.util.Locale.US, "%,d", boiler.tanks[1].getFill()) + " / " + String.format(java.util.Locale.US, "%,d", boiler.tanks[1].getMaxFill()) + "mB")));

        com.hbm_m.interfaces.ILookOverlay.printGeneric(g, net.minecraft.network.chat.Component.translatable(getDescriptionId()), 0xffff00, 0x404000, text);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.INDUSTRIAL_BOILER_BE.get(), MachineIndustrialBoilerBlockEntity::tick);
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
    /*public static final com.mojang.serialization.MapCodec<MachineIndustrialBoilerBlock> CODEC = simpleCodec(MachineIndustrialBoilerBlock::new);

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
