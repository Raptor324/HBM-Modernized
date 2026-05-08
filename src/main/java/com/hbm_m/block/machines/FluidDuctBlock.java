package com.hbm_m.block.machines;


import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.common.collect.ImmutableMap;
import com.hbm_m.api.fluids.HbmFluidRegistry;
import com.hbm_m.api.fluids.VanillaFluidEquivalence;
import com.hbm_m.block.entity.machines.FluidDuctBlockEntity;
import com.hbm_m.block.entity.machines.MachineChemicalPlantBlockEntity;
import com.hbm_m.block.entity.machines.MachineFluidTankBlockEntity;
import com.hbm_m.block.entity.machines.UniversalMachinePartBlockEntity;
import com.hbm_m.client.render.DoorChunkInvalidationHelper;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.multiblock.PartRole;
import com.hbm_m.interfaces.IItemFluidIdentifier;
import com.hbm_m.interfaces.ILookOverlay;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.liquids.FluidDuctItem;

//? if fabric {
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;//?}
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
//? if forge {
/*import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGuiEvent;
import com.hbm_m.api.fluids.FluidCapabilityAccess;
*///?}

/**
 * Fluid duct: multipart blockstate + Forge OBJ visibility on {@code pipe_neo.obj}. Fluid type lives in the block entity.
 * <p>
 * {@link PipeRenderShape} follows NEO pipe rules (isolated / single-axis / complex + octants). Arms use corrected mapping
 * (south → {@code pZ}, north → {@code nZ}) vs the swapped render in NEO’s {@code RenderPipe}.
 * <p>
 * Fluid identifier: normal click sets fluid on one duct; <b>sneak (Shift)</b> + click paints the connected network of the
 * same block type (depth-capped). Vanilla does not expose Ctrl on the server; use sneak for recursive mode.
 */
public class FluidDuctBlock extends BaseEntityBlock implements ILookOverlay {

    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    public static final BooleanProperty UP = BlockStateProperties.UP;
    public static final BooleanProperty DOWN = BlockStateProperties.DOWN;

    public static final EnumProperty<PipeRenderShape> SHAPE =
            EnumProperty.create("shape", PipeRenderShape.class);

    public static final Map<Direction, BooleanProperty> PROPERTY_BY_DIRECTION = ImmutableMap.of(
            Direction.NORTH, NORTH, Direction.SOUTH, SOUTH,
            Direction.WEST, WEST, Direction.EAST, EAST,
            Direction.UP, UP, Direction.DOWN, DOWN);

    private static final VoxelShape CORE = Block.box(4, 4, 4, 12, 12, 12);
    private static final Map<Direction, VoxelShape> ARM_SHAPES = ImmutableMap.of(
            Direction.NORTH, Block.box(4, 4, 0, 12, 12, 4),
            Direction.SOUTH, Block.box(4, 4, 12, 12, 12, 16),
            Direction.WEST, Block.box(0, 4, 4, 4, 12, 12),
            Direction.EAST, Block.box(12, 4, 4, 16, 12, 12),
            Direction.UP, Block.box(4, 12, 4, 12, 16, 12),
            Direction.DOWN, Block.box(4, 0, 4, 12, 4, 12));

    private static final int IDENTIFIER_NETWORK_LIMIT = 512;

    /** ResourceLocation "none" mod fluid → cleared duct ({@link Fluids#EMPTY}). */
    @Nullable
    private static Fluid normalizeDuctPaintFluid(@Nullable Fluid fluid) {
        if (fluid == null) {
            return null;
        }
        return fluid == ModFluids.NONE.getSource() ? Fluids.EMPTY : fluid;
    }

    private final PipeStyle pipeStyle;

    public FluidDuctBlock(Properties properties, PipeStyle pipeStyle) {
        super(properties);
        this.pipeStyle = pipeStyle;
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(NORTH, false).setValue(EAST, false).setValue(SOUTH, false)
                .setValue(WEST, false).setValue(UP, false).setValue(DOWN, false)
                .setValue(SHAPE, PipeRenderShape.ISOLATED));
    }

    public PipeStyle getPipeStyle() {
        return pipeStyle;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN, SHAPE);
    }

    @Override
    public VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos,
            @NotNull CollisionContext context) {
        VoxelShape shape = CORE;
        for (Direction dir : Direction.values()) {
            if (state.getValue(PROPERTY_BY_DIRECTION.get(dir))) {
                shape = Shapes.or(shape, ARM_SHAPES.get(dir));
            }
        }
        return shape;
    }

    /** Full connection + render shape for a duct at {@code pos} (block there must be this block type). */
    public BlockState getConnectionState(LevelAccessor level, BlockPos pos) {
        BlockState self = level.getBlockState(pos);
        Block blk = self.getBlock();
        if (!(blk instanceof FluidDuctBlock duct)) {
            return self;
        }
        boolean north = duct.canConnectTo(level, pos, pos.relative(Direction.NORTH), Direction.NORTH);
        boolean south = duct.canConnectTo(level, pos, pos.relative(Direction.SOUTH), Direction.SOUTH);
        boolean east = duct.canConnectTo(level, pos, pos.relative(Direction.EAST), Direction.EAST);
        boolean west = duct.canConnectTo(level, pos, pos.relative(Direction.WEST), Direction.WEST);
        boolean up = duct.canConnectTo(level, pos, pos.relative(Direction.UP), Direction.UP);
        boolean down = duct.canConnectTo(level, pos, pos.relative(Direction.DOWN), Direction.DOWN);
        PipeRenderShape shape = PipeRenderShape.fromConnections(north, south, east, west, up, down);
        return duct.defaultBlockState()
                .setValue(NORTH, north).setValue(SOUTH, south).setValue(EAST, east)
                .setValue(WEST, west).setValue(UP, up).setValue(DOWN, down)
                .setValue(SHAPE, shape);
    }

    /**
     * Убирает 1-кадровый "isolated" глитч при постановке:
     * сразу ставим корректный blockstate соединений.
     */
    @Nullable
    @Override
    public BlockState getStateForPlacement(@NotNull BlockPlaceContext ctx) {
        return getConnectionState(ctx.getLevel(), ctx.getClickedPos());
    }

    @Override
    public BlockState updateShape(@NotNull BlockState state, @NotNull Direction facing,
            @NotNull BlockState facingState, @NotNull LevelAccessor level,
            @NotNull BlockPos currentPos, @NotNull BlockPos facingPos) {
        BlockState self = level.getBlockState(currentPos);
        if (self.getBlock() instanceof FluidDuctBlock duct) {
            return duct.getConnectionState(level, currentPos);
        }
        return state;
    }

    @Override
    public void onPlace(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos,
            @NotNull BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);

        // Клиент: убираем 1-кадровый глитч baked-меша (isolated → connected).
        // BlockState уже корректный из getStateForPlacement, но чанковый меш может быть не пересобран мгновенно.
        if (level.isClientSide) {
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_IMMEDIATE | Block.UPDATE_CLIENTS);
            DoorChunkInvalidationHelper.scheduleChunkInvalidation(pos);
            // Соседи тоже могут изменить соединения в этот же кадр.
            for (Direction d : Direction.values()) {
                DoorChunkInvalidationHelper.scheduleChunkInvalidation(pos.relative(d));
            }
        }
    }

    @Override
    public void neighborChanged(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos,
            @NotNull Block block, @NotNull BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        BlockState newState = getConnectionState(level, pos);
        if (!newState.equals(state)) {
            level.setBlock(pos, newState, Block.UPDATE_CLIENTS);
        }
        // При загрузке чанков уведомления от соседей приходят до готовности BE/ролей/капабилити.
        // Планируем отложенный пересчёт через 1 тик, чтобы состояние сошлось детерминированно.
        level.scheduleTick(pos, this, 1);
    }

    @Override
    public void tick(@NotNull BlockState state, @NotNull ServerLevel level, @NotNull BlockPos pos,
            @NotNull RandomSource random) {
        // One-shot delayed recompute (also refreshes same-block neighbors).
        refreshAdjacentDucts(level, pos);
    }

    private boolean canConnectTo(LevelAccessor level, BlockPos myPos, BlockPos neighborPos, Direction direction) {
        BlockState neighborState = level.getBlockState(neighborPos);
        Block selfBlock = level.getBlockState(myPos).getBlock();

        if (neighborState.getBlock() == selfBlock && neighborState.getBlock() instanceof FluidDuctBlock) {
            BlockEntity myBe = level.getBlockEntity(myPos);
            BlockEntity neighborBe = level.getBlockEntity(neighborPos);
            if (myBe instanceof FluidDuctBlockEntity myDuct && neighborBe instanceof FluidDuctBlockEntity neighborDuct) {
                return VanillaFluidEquivalence.sameSubstance(myDuct.getFluidType(), neighborDuct.getFluidType());
            }
            return true;
        }

        BlockEntity be = level.getBlockEntity(neighborPos);
        if (be != null) {
            BlockEntity myBe = level.getBlockEntity(myPos);
            Fluid ductFluid = Fluids.EMPTY;
            if (myBe instanceof FluidDuctBlockEntity myDuct) {
                Fluid raw = normalizeDuctPaintFluid(myDuct.getFluidType());
                ductFluid = raw != null ? raw : Fluids.EMPTY;
            }

            // === Коннектор мультиблока (UniversalMachinePart) ===
            // Важно: не даём трубе "липнуть" к контроллеру, если подключение разрешено только через части-коннекторы.
            // Раньше тут был хардкод под цистерну, из-за чего трубы не коннектились к коннекторам других мультиблоков
            // (например, хим. установки). Теперь разрешаем подключение к любому контроллеру с IFluidHandler.
            if (be instanceof UniversalMachinePartBlockEntity part
                    && part.getControllerPos() != null
                    && (part.getPartRole() == PartRole.FLUID_CONNECTOR || part.getPartRole() == PartRole.UNIVERSAL_CONNECTOR)) {
                // Проверка стороны коннектора: если набор задан, пусто трактуем как "все стороны" (совместимость IMultiblockPart).
                var allowed = part.getAllowedFluidSides();
                if (allowed != null && !allowed.isEmpty() && !allowed.contains(direction.getOpposite())) {
                    return false;
                }
                BlockEntity ctrl = level.getBlockEntity(part.getControllerPos());
                if (ctrl == null) return false;

                // Химическая установка: коннект трубы визуально не фильтруем по краске/наличию рецепта
                // (входные жидкости задаются рецептом; см. MachineChemicalPlantBlockEntity#getActiveFluidInputSlotCount).
                if (ctrl instanceof MachineChemicalPlantBlockEntity) {
                    return true;
                }

                // Цистерна: более строгая визуальная логика (не показываем "руку" пока тип не задан/не залит)
                if (ctrl instanceof MachineFluidTankBlockEntity tank) {
                    if (ductFluid == Fluids.EMPTY) return true;
                    return ductFluidMatchesTankForVisual(ductFluid, tank);
                }

                // Остальные контроллеры:
                // - если труба не окрашена (EMPTY) — показываем соединение просто по наличию fluid handler
                // - если окрашена — показываем соединение только если контроллер реально принимает этот fluid (SIMULATE fill > 0)
                //? if forge {
                /*var cap = ctrl.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER, null);
                if (!cap.isPresent()) return false;
                if (ductFluid == Fluids.EMPTY) return true;
                var handler = cap.resolve().orElse(null);
                if (handler == null) return false;
                int canFill = handler.fill(new net.minecraftforge.fluids.FluidStack(ductFluid, 1), net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE);
                return canFill > 0;
                *///?}

                //? if fabric {
                return ductFluid == Fluids.EMPTY;  TODO: Fabric Transfer API sided check
                //?}
            }

            // Контроллер цистерны: прямое подключение запрещено правилами мультиблока.
            // (Иначе при загрузке чанка возможно кратковременное "прилипание" до восстановления ролей/сторон.)
            if (be instanceof MachineFluidTankBlockEntity) {
                return false;
            }

            return FluidCapabilityAccess.hasFluidHandler(level, neighborPos, direction.getOpposite());
        }
        return false;
    }

    // resolveFluidTankForConnection больше не нужен: визуальное подключение к цистерне делается только через части-коннекторы.

    /** Окрашенная труба не показывает «руку» к цистерне без заданного типа/заполнения; типы сверяются через {@link VanillaFluidEquivalence}. */
    private static boolean ductFluidMatchesTankForVisual(Fluid ductFluidNorm, MachineFluidTankBlockEntity tank) {
        Fluid tankType = tank.getFluidTank().getTankType();
        int fill = tank.getFluidTank().getFill();
        boolean tankCommitted = fill > 0 || FluidTank.isFluidTypeExplicitlySet(tankType);
        if (!tankCommitted) {
            return false;
        }
        return VanillaFluidEquivalence.sameSubstance(ductFluidNorm, tankType);
    }

    /** Refresh this duct and same-block neighbors (after fluid / connection change). */
    public static void refreshAdjacentDucts(Level level, BlockPos pos) {
        BlockState st = level.getBlockState(pos);
        Block b = st.getBlock();
        if (!(b instanceof FluidDuctBlock duct)) {
            return;
        }
        Set<BlockPos> update = new HashSet<>();
        update.add(pos);
        for (Direction d : Direction.values()) {
            update.add(pos.relative(d));
        }
        for (BlockPos p : update) {
            if (level.getBlockState(p).getBlock() != b) {
                continue;
            }
            BlockState next = duct.getConnectionState(level, p);
            level.setBlock(p, next, Block.UPDATE_CLIENTS);
            BlockEntity be = level.getBlockEntity(p);
            if (be instanceof FluidDuctBlockEntity dbe) {
                dbe.syncFluidToClients();
            }
        }
    }

    @Override
    public InteractionResult use(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos,
            @NotNull Player player, @NotNull InteractionHand hand, @NotNull BlockHitResult hit) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.isEmpty() || !(stack.getItem() instanceof IItemFluidIdentifier idItem)) {
            return InteractionResult.PASS;
        }
        Fluid fluid = normalizeDuctPaintFluid(idItem.getType(level, pos, stack));
        if (fluid == null) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        boolean recursive = player.isShiftKeyDown();
        Block startBlock = state.getBlock();
        if (!(startBlock instanceof FluidDuctBlock)) {
            return InteractionResult.PASS;
        }
        if (recursive) {
            applyIdentifierRecursive(level, pos, fluid, startBlock);
        } else {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof FluidDuctBlockEntity duct) {
                duct.setFluidType(fluid);
                level.setBlock(pos, getConnectionState(level, pos), Block.UPDATE_CLIENTS);
                refreshAdjacentDucts(level, pos);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static void applyIdentifierRecursive(Level level, BlockPos start, Fluid fluid, Block ductBlock) {
        Set<BlockPos> visited = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        visited.add(start);
        while (!queue.isEmpty() && visited.size() < IDENTIFIER_NETWORK_LIMIT) {
            BlockPos p = queue.poll();
            BlockState st = level.getBlockState(p);
            if (st.getBlock() != ductBlock) {
                continue;
            }
            for (Direction dir : Direction.values()) {
                if (!st.getValue(PROPERTY_BY_DIRECTION.get(dir))) {
                    continue;
                }
                BlockPos n = p.relative(dir);
                if (visited.contains(n)) {
                    continue;
                }
                BlockState ns = level.getBlockState(n);
                if (ns.getBlock() != ductBlock) {
                    continue;
                }
                if (!ns.getValue(PROPERTY_BY_DIRECTION.get(dir.getOpposite()))) {
                    continue;
                }
                visited.add(n);
                queue.add(n);
            }
        }
        for (BlockPos p : visited) {
            BlockEntity be = level.getBlockEntity(p);
            if (be instanceof FluidDuctBlockEntity duct) {
                duct.setFluidTypeSilent(fluid);
            }
        }
        FluidDuctBlock fd = (FluidDuctBlock) ductBlock;
        for (BlockPos p : visited) {
            level.setBlock(p, fd.getConnectionState(level, p), Block.UPDATE_CLIENTS);
        }
        for (BlockPos p : visited) {
            BlockEntity be = level.getBlockEntity(p);
            if (be instanceof FluidDuctBlockEntity duct) {
                duct.syncFluidToClients();
            }
        }
        // Neighbors outside the BFS may need new connection flags
        Set<BlockPos> border = new HashSet<>();
        for (BlockPos p : visited) {
            for (Direction d : Direction.values()) {
                border.add(p.relative(d));
            }
        }
        for (BlockPos p : border) {
            if (visited.contains(p)) {
                continue;
            }
            if (level.getBlockState(p).getBlock() == ductBlock) {
                level.setBlock(p, fd.getConnectionState(level, p), Block.UPDATE_CLIENTS);
                BlockEntity be = level.getBlockEntity(p);
                if (be instanceof FluidDuctBlockEntity dbe) {
                    dbe.syncFluidToClients();
                }
            }
        }
    }

    /**
     * Shift + fluid identifier on a duct: paints the whole connected network of the same duct block type.
     * Used from {@link com.hbm_m.item.liquids.FluidIdentifierItem#useOn} because item {@code useOn} runs before block use
     * and must consume the interaction so it is not confused with other UI.
     */
    public static void paintConnectedDuctNetwork(Level level, BlockPos start, Fluid fluid) {
        fluid = normalizeDuctPaintFluid(fluid);
        if (level.isClientSide || fluid == null) {
            return;
        }
        BlockState st = level.getBlockState(start);
        Block b = st.getBlock();
        if (!(b instanceof FluidDuctBlock)) {
            return;
        }
        applyIdentifierRecursive(level, start, fluid, b);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new FluidDuctBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level,
            @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
        return level.isClientSide ? null : (lvl, pos, st, be) -> {
            if (be instanceof FluidDuctBlockEntity duct) {
                FluidDuctBlockEntity.tick(lvl, pos, st, duct);
            }
        };
    }

    @Override
    public RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL;
    }

    @NotNull
    @Override
    public ItemStack getCloneItemStack(@NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull BlockState state) {
        ItemStack stack = new ItemStack(getDuctItem());
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof FluidDuctBlockEntity ductBe
                && ductBe.getFluidType() != Fluids.EMPTY) {
            FluidDuctItem.setFluidType(stack, ductBe.getFluidType());
        }
        return stack;
    }

    private net.minecraft.world.item.Item getDuctItem() {
        return switch (pipeStyle) {
            case NEO -> ModItems.FLUID_DUCT.get();
            case COLORED -> ModItems.FLUID_DUCT_COLORED.get();
            case SILVER -> ModItems.FLUID_DUCT_SILVER.get();
        };
    }

    @Override
    public List<ItemStack> getDrops(@NotNull BlockState state, LootParams.Builder builder) {
        BlockEntity be = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        ItemStack drop = new ItemStack(getDuctItem());
        if (be instanceof FluidDuctBlockEntity ductBe
                && ductBe.getFluidType() != Fluids.EMPTY) {
            FluidDuctItem.setFluidType(drop, ductBe.getFluidType());
        }
        return List.of(drop);
    }

    @Override
//? if forge {
/*@OnlyIn(Dist.CLIENT)
*///?}
//? if fabric {
@Environment(EnvType.CLIENT)//?}
    public void printHook(net.minecraft.client.gui.GuiGraphics guiGraphics, Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof FluidDuctBlockEntity ductBe)) {
            return;
        }
        List<Component> text = new ArrayList<>();
        Fluid fluid = ductBe.getFluidType();
        if (fluid == null || fluid == Fluids.EMPTY) {
            text.add(Component.translatable("gui.hbm_m.fluid_duct.overlay.fluid_empty"));
        } else {
            int rgb = HbmFluidRegistry.getTintColor(fluid) & 0xFFFFFF;
            String name = HbmFluidRegistry.getFluidName(fluid);
            text.add(Component.literal(name).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(rgb))));
            text.add(Component.literal("Net nodes: " + ductBe.getNetworkSize())
                    .withStyle(net.minecraft.ChatFormatting.GRAY));
            text.add(Component.literal("Transfer/t: " + ductBe.getFluidTracker() + " mB")
                    .withStyle(net.minecraft.ChatFormatting.GRAY));
        }
        ILookOverlay.printGeneric(guiGraphics, Component.translatable(getDescriptionId()), 0xffff00, 0x404000, text);
    }
}
