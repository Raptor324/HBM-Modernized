package com.hbm_m.block.machines;

import java.util.Map;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineStrandCasterBlockEntity;
import com.hbm_m.inventory.material.MaterialStack;
import com.hbm_m.interfaces.IMultiblockController;
import com.hbm_m.item.material.ItemCastMold;
import com.hbm_m.multiblock.MultiblockStructureHelper;
import com.hbm_m.multiblock.PartRole;
import com.hbm_m.sound.ModSounds;
import com.hbm_m.util.CrucibleUtil;

import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
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
 * Strand Caster — порт {@code MachineStrandCaster} (1.7.10, BlockDummyable
 * {@code getAllDimensions = {{0,0,6,0,1,0}, {2,0,1,0,1,0}}}, offset 0):
 * стол непрерывного литья 2×7×1 с башней-приёмником 2×2×3 над ядром
 * (ядро — северо-западный нижний угол башни; стол уходит назад по FACING).
 *
 * <p>Клик с изложницей вставляет её (порт onBlockActivated), лопата высыпает
 * расплав в шлак, отвёртка вынимает изложницу, иначе открывается GUI.
 */
public class MachineStrandCasterBlock extends BaseEntityBlock implements IMultiblockController {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    private final MultiblockStructureHelper structureHelper;

    public MachineStrandCasterBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
        this.structureHelper = defineStructure();
    }

    /**
     * Стол 2×7×1 (7 рядов вперёд-назад × 2 колонки) + башня 2×2×3 в передних
     * рядах; ядро — передний ряд, западная колонка (оригинал: башня растёт от
     * ядра вперёд по FACING и влево). Смещение установки 0 — как в оригинале.
     */
    private static MultiblockStructureHelper defineStructure() {
        String[] y0 = { "CO", "OO", "OO", "OO", "OO", "OO", "OO" };
        String[] y1 = { "OO", "OO" };
        String[] y2 = { "OO", "OO" };

        Map<Character, PartRole> roleMap = Map.of(
                'O', PartRole.DEFAULT,
                'C', PartRole.CONTROLLER
        );

        Map<Character, Supplier<BlockState>> symbolMap = Map.of();

        return MultiblockStructureHelper.createFromLayersWithRoles(
                new String[][] { y0, y1, y2 },
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
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
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

    @Override
    public boolean canSurvive(BlockState state, net.minecraft.world.level.LevelReader level, BlockPos pos) {
        return super.canSurvive(state, level, pos) && canSurviveMultiblockPlacement(state, level, pos);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachineStrandCasterBlockEntity(pos, state);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!state.is(oldState.getBlock()) && !level.isClientSide()) {
            placeMultiblockStructure(level, pos, state);
        }
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.STRAND_CASTER_BE.get(), MachineStrandCasterBlockEntity::tick);
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return hbmOnUse(state, level, pos, player, hand);
    }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return hbmOnUse(state, level, pos, player, InteractionHand.MAIN_HAND);
    }
    *///?}

    /**
     * Порт onBlockActivated: изложница в руку → вставить в слот 0 (звук
     * upgradePlug); лопата → высыпать расплав шлаком; отвёртка → вынуть
     * изложницу; иначе GUI.
     */
    private InteractionResult hbmOnUse(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof MachineStrandCasterBlockEntity caster)) {
            return InteractionResult.PASS;
        }

        ItemStack held = player.getItemInHand(hand);

        // вставка изложницы
        if (held.getItem() instanceof ItemCastMold && caster.getStackInSlot(0).isEmpty()) {
            caster.setStackInSlot(0, held.copyWithCount(1));
            held.shrink(1);
            level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    ModSounds.UPGRADE_PLUG.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            return InteractionResult.CONSUME;
        }

        // лопата — высыпать расплав
        if (held.getItem() instanceof ShovelItem) {
            if (caster.amount > 0 && caster.type != null) {
                ItemStack scrap = CrucibleUtil.createScrap(new MaterialStack(caster.type, caster.amount));
                if (!scrap.isEmpty()) {
                    if (!player.getInventory().add(scrap)) {
                        Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, scrap);
                    }
                }
                caster.amount = 0;
                caster.type = null;
                caster.setChanged();
                caster.syncToClient();
            }
            return InteractionResult.CONSUME;
        }

        // отвёртка — вынуть изложницу
        if (held.getItem() == com.hbm_m.item.ModItems.SCREWDRIVER.get()) {
            ItemStack mold = caster.getStackInSlot(0);
            if (mold.isEmpty()) {
                return InteractionResult.CONSUME;
            }
            if (!player.getInventory().add(mold.copy())) {
                Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, mold.copy());
            }
            caster.setStackInSlot(0, ItemStack.EMPTY);
            return InteractionResult.CONSUME;
        }

        if (player instanceof ServerPlayer serverPlayer) {
            MenuRegistry.openExtendedMenu(serverPlayer, caster, buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (state.getBlock() != newState.getBlock()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof MachineStrandCasterBlockEntity caster) {
                caster.drops();
                // Порт breakBlock: расплав высыпается шлаком
                if (!level.isClientSide() && caster.amount > 0 && caster.type != null) {
                    ItemStack scrap = CrucibleUtil.createScrap(new MaterialStack(caster.type, caster.amount));
                    if (!scrap.isEmpty()) {
                        Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, scrap);
                    }
                    caster.amount = 0;
                }
            }
            if (!level.isClientSide()) {
                structureHelper.destroyStructure(level, pos, state.getValue(FACING));
            }
        }
        super.onRemove(state, level, pos, newState, moving);
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<MachineStrandCasterBlock> CODEC = simpleCodec(MachineStrandCasterBlock::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return CODEC;
    }
    *///?}
}
