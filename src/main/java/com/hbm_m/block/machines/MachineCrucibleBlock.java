package com.hbm_m.block.machines;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineCrucibleBlockEntity;
import com.hbm_m.interfaces.IMultiblockController;
import com.hbm_m.inventory.menu.MachineCrucibleMenu;
import com.hbm_m.multiblock.MultiblockStructureHelper;
import com.hbm_m.multiblock.PartRole;
import com.hbm_m.util.CrucibleUtil;
import com.hbm_m.inventory.material.MaterialStack;

import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
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
 * Crucible — порт {@code MachineCrucible} (1.7.10, BlockDummyable
 * {@code {1,0,1,1,1,1}}): мультиблок 3×3×2 — нижний слой с ядром в центре,
 * верхний слой открытой чаши (в центре пусто, чтобы предметы падали внутрь).
 *
 * <p>Коллизия 1:1 из оригинального списка {@code bounding}: плита-дно 3×3
 * (0..0.5 блока) и бортик 0.25 блока толщиной на кольце ±1..±1.25 от центра,
 * высотой 0.5..1.5. Лопата высыпает расплав в шлак, клик открывает GUI.
 */
public class MachineCrucibleBlock extends BaseEntityBlock implements IMultiblockController {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    /**
     * Оригинальные {@code bounding}-боксы в 1/16 блока (формат x0,y0,z0,x1,y1,z1):
     * дно 3×3 × 4 стенки-бортика. Координаты относительно min-угла КОНТРОЛЬНОЙ
     * клетки — сетка размещения 3×3×2 покрывает -16..32 px (центр структуры =
     * +8px, центр контролльной клетки), бортики стоят на center±16..20.
     */
    private static final double[][] BOUNDING = {
            { -16, 0, -16, 32, 8, 32 },       // дно 3x3
            { -12, 8, -12, -8, 24, 28 },      // западный бортик
            { 24, 8, -12, 28, 24, 28 },       // восточный бортик
            { -12, 8, -12, 28, 24, -8 },      // северный бортик
            { -12, 8, 24, 28, 24, 28 },       // южный бортик
    };

    private final MultiblockStructureHelper structureHelper;

    public MachineCrucibleBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
        this.structureHelper = defineStructure();
    }

    /**
     * 3×3×2 с пер-позиционными формами, вычисленными пересечением оригинальных
     * bounding-боксов с каждой клеткой мультиблока.
     */
    private static MultiblockStructureHelper defineStructure() {
        Supplier<BlockState> phantom = () -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState();

        Map<BlockPos, Supplier<BlockState>> structureMap = new HashMap<>();
        Map<BlockPos, Character> positionSymbols = new HashMap<>();
        Map<BlockPos, VoxelShape> partShapes = new HashMap<>();
        Map<BlockPos, VoxelShape> collisionShapes = new HashMap<>();
        Map<Character, PartRole> roleMap = Map.of('C', PartRole.CONTROLLER, 'O', PartRole.DEFAULT);

        BlockPos controllerOffset = BlockPos.ZERO;
        positionSymbols.put(controllerOffset, 'C');

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dy = 0; dy <= 1; dy++) {
                    BlockPos pos = new BlockPos(dx, dy, dz);
                    if (pos.equals(controllerOffset)) continue;
                    structureMap.put(pos, phantom);
                    positionSymbols.put(pos, 'O');
                }
            }
        }

        // Пересечение bounding-боксов с каждой клеткой (в локальных координатах клетки)
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dy = 0; dy <= 1; dy++) {
                    VoxelShape shape = Shapes.empty();
                    double cellMinX = dx * 16.0, cellMinY = dy * 16.0, cellMinZ = dz * 16.0;
                    for (double[] b : BOUNDING) {
                        double x0 = Math.max(b[0], cellMinX) - cellMinX;
                        double y0 = Math.max(b[1], cellMinY) - cellMinY;
                        double z0 = Math.max(b[2], cellMinZ) - cellMinZ;
                        double x1 = Math.min(b[3], cellMinX + 16) - cellMinX;
                        double y1 = Math.min(b[4], cellMinY + 16) - cellMinY;
                        double z1 = Math.min(b[5], cellMinZ + 16) - cellMinZ;
                        if (x1 > x0 && y1 > y0 && z1 > z0) {
                            shape = Shapes.or(shape, Block.box(x0, y0, z0, x1, y1, z1));
                        }
                    }
                    BlockPos pos = new BlockPos(dx, dy, dz);
                    partShapes.put(pos, shape);
                    collisionShapes.put(pos, shape);
                }
            }
        }

        return new MultiblockStructureHelper(
                structureMap, phantom, roleMap, positionSymbols, partShapes, collisionShapes, controllerOffset);
    }

    @Override
    public MultiblockStructureHelper getStructureHelper() {
        return structureHelper;
    }

    @Override
    public PartRole getPartRole(BlockPos localOffset) {
        return structureHelper.resolvePartRole(localOffset, this);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachineCrucibleBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return createTickerHelper(type, ModBlockEntities.CRUCIBLE_BE.get(),
                    (lvl, pos, st, be) -> MachineCrucibleBlockEntity.clientTick(lvl, pos, st, (MachineCrucibleBlockEntity) be));
        }
        return createTickerHelper(type, ModBlockEntities.CRUCIBLE_BE.get(),
                (lvl, pos, st, be) -> MachineCrucibleBlockEntity.serverTick(lvl, pos, st, (MachineCrucibleBlockEntity) be));
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!state.is(oldState.getBlock()) && !level.isClientSide()) {
            placeMultiblockStructure(level, pos, state);
        }
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
        return Shapes.empty();
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
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit) {
        return hbmOnUse(state, level, pos, player, hand, hit);
    }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return hbmOnUse(state, level, pos, player, InteractionHand.MAIN_HAND, hit);
    }
    *///?}

    /**
     * Порт onBlockActivated: лопата высыпает содержимое (recipeStack + wasteStack)
     * в шлак-предметы игроку, обычный клик открывает GUI.
     */
    private InteractionResult hbmOnUse(BlockState state, Level level, BlockPos pos,
                                       Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof MachineCrucibleBlockEntity crucible)) {
            return InteractionResult.PASS;
        }

        ItemStack held = player.getItemInHand(hand);
        if (!held.isEmpty() && held.getItem() instanceof ShovelItem) {
            crucible.dumpToPlayer(player, hit.getLocation());
            return InteractionResult.CONSUME;
        }

        if (player instanceof ServerPlayer serverPlayer) {
            MenuRegistry.openExtendedMenu(serverPlayer,
                    new SimpleMenuProvider(
                            (containerId, playerInventory, p) -> new MachineCrucibleMenu(
                                    containerId, playerInventory, crucible, crucible.getData()),
                            Component.translatable("container.hbm_m.crucible")),
                    buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public boolean canSurvive(BlockState state, net.minecraft.world.level.LevelReader level, BlockPos pos) {
        return super.canSurvive(state, level, pos) && canSurviveMultiblockPlacement(state, level, pos);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos,
                         BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            if (!level.isClientSide()) {
                structureHelper.destroyStructure(level, pos, state.getValue(FACING));
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof MachineCrucibleBlockEntity crucible) {
                    // Порт breakBlock: содержимое высыпается шлаком на землю
                    for (MaterialStack stack : crucible.getAllStacks()) {
                        ItemStack scrap = CrucibleUtil.createScrap(stack);
                        if (!scrap.isEmpty()) {
                            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, scrap);
                        }
                    }
                    crucible.clearStacks();
                }
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<MachineCrucibleBlock> CODEC = simpleCodec(MachineCrucibleBlock::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return CODEC;
    }
    *///?}
}
