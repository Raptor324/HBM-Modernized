package com.hbm_m.block.machines;

import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.PantsirBlockEntity;
import com.hbm_m.interfaces.IMultiblockController;
import com.hbm_m.item.ModItems;
import com.hbm_m.multiblock.MultiblockStructureHelper;
import com.hbm_m.multiblock.PartRole;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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
 * Pantsir-S1 als Multiblock. Das Fahrzeug zeichnet ein TESR, die Struktur ist nur die Standflaeche.
 * Beim Platzieren schaut die Kabine zum Spieler. Rechtsklick schaltet die Anlage um, mit einer 57E6 oder
 * einem Gurt ammo_dgk in der Hand wird stattdessen nachgeladen. Schleichen und Rechtsklick schaltet die
 * Freund-Feind-Erkennung um. Geladene Munition faellt beim Abbau heraus.
 */
public class PantsirBlock extends BaseEntityBlock implements IMultiblockController {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    private final MultiblockStructureHelper structureHelper;

    public PantsirBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
        this.structureHelper = defineStructure();
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!level.isClientSide() && !state.is(oldState.getBlock())) {
            placeMultiblockStructure(level, pos, state);
        }
    }

    @Override
    public boolean canSurvive(BlockState state, net.minecraft.world.level.LevelReader level, BlockPos pos) {
        return super.canSurvive(state, level, pos) && canSurviveMultiblockPlacement(state, level, pos);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide()) {
            if (level instanceof net.minecraft.server.level.ServerLevel server
                    && level.getBlockEntity(pos) instanceof PantsirBlockEntity holder) {
                holder.releaseChunk(server);
            }
            if (level.getBlockEntity(pos) instanceof PantsirBlockEntity pantsir && pantsir.getAmmo() > 0) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(),
                        new ItemStack(ModItems.MISSILE_57E6.get(), pantsir.getAmmo()));
            }
            if (level.getBlockEntity(pos) instanceof PantsirBlockEntity pantsir) {
                int belts = pantsir.getShells() / PantsirBlockEntity.SHELLS_PER_BELT;
                if (belts > 0) {
                    Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(),
                            new ItemStack(ModItems.AMMO_DGK.get(), belts));
                }
            }
            getStructureHelper().destroyStructure(level, pos, state.getValue(FACING));
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.ENTITYBLOCK_ANIMATED; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING); }
    @Nullable @Override public BlockState getStateForPlacement(BlockPlaceContext context) { return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()); }
    @Nullable @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new PantsirBlockEntity(pos, state); }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.PANTSIR_S1_BE.get(), PantsirBlockEntity::tick);
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return toggle(level, pos, player);
    }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return toggle(level, pos, player);
    }
    *///?}

    private InteractionResult toggle(Level level, BlockPos pos, Player player) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof PantsirBlockEntity pantsir) {
            ItemStack held = player.getMainHandItem();
            if (player.isShiftKeyDown()) {
                pantsir.toggleIff(player);
            } else if (held.is(ModItems.MISSILE_57E6.get())) {
                if (pantsir.loadMissile() && !player.getAbilities().instabuild) {
                    held.shrink(1);
                }
                pantsir.reportLoaded(player);
            } else if (held.is(ModItems.AMMO_DGK.get()) && level instanceof net.minecraft.server.level.ServerLevel server) {
                if (pantsir.loadBelt(server) && !player.getAbilities().instabuild) {
                    held.shrink(1);
                }
                pantsir.reportLoaded(player);
            } else {
                pantsir.toggle();
                pantsir.reportStatus(player);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        MultiblockStructureHelper helper = getStructureHelper();
        return helper != null ? helper.generateShapeFromParts(state.getValue(FACING)) : Shapes.block();
    }

    @Override public MultiblockStructureHelper getStructureHelper() { return this.structureHelper; }

    /** Grundriss 13 (Laenge) x 5 (Breite), eine Lage hoch, Controller in der Mitte. */
    private static MultiblockStructureHelper defineStructure() {
        String[] layer = new String[13];
        for (int i = 0; i < layer.length; i++) {
            layer[i] = "AAAAA";
        }
        layer[6] = "AACAA";

        Map<Character, PartRole> roles = Map.of('A', PartRole.DEFAULT, 'C', PartRole.CONTROLLER);
        VoxelShape plate = Block.box(0, 0, 0, 16, 1, 16);
        Map<Character, VoxelShape> shapes = Map.of('C', plate, 'A', plate);

        return MultiblockStructureHelper.createFromLayersWithRoles(
            new String[][]{layer},
            Map.of(),
            () -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState(),
            roles,
            shapes,
            shapes
        );
    }

    @Override
    public PartRole getPartRole(BlockPos localOffset) {
        return structureHelper != null ? structureHelper.resolvePartRole(localOffset, this) : PartRole.DEFAULT;
    }

    @Override
    public float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0F;
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<PantsirBlock> CODEC = simpleCodec(PantsirBlock::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return CODEC;
    }
    *///?}
}
