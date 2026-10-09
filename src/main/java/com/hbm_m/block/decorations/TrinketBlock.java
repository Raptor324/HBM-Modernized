package com.hbm_m.block.decorations;

import javax.annotation.Nullable;

import com.hbm_m.block.decorations.TrinketTypes.PlushieType;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.decorations.TrinketBlockEntity;
import com.hbm_m.item.TrinketBlockItem;
import com.hbm_m.sound.HbmSoundsNT;

import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 1:1 {@code BlockBobble} ({@code bobblehead}), {@code BlockSnowglobe} ({@code snowglobe}) und {@code BlockPlushie}
 * ({@code plushie}): Figur mit Typ im Blockentity, 16 Drehstufen ({@code (yaw + 180) * 16 / 360}). Kein normaler
 * Drop; wer ausserhalb des Kreativmodus abbaut, bekommt die Figur mit ihrem Typ ({@code onBlockHarvested}).
 * Wackelkopf/Schneekugel oeffnen beim Anklicken ihre Tafel, das Plueschtier quietscht (Hundun heult).
 */
public class TrinketBlock extends BaseEntityBlock {

    public enum Kind { BOBBLE, SNOWGLOBE, PLUSHIE }

    public static final IntegerProperty ROTATION = BlockStateProperties.ROTATION_16;

    private static final VoxelShape BOBBLE_SHAPE = Block.box(5.5, 0, 5.5, 10.5, 10, 10.5);
    private static final VoxelShape SNOWGLOBE_SHAPE = Block.box(4, 0, 4, 12, 5, 12);

    public final Kind kind;

    public TrinketBlock(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
        registerDefaultState(stateDefinition.any().setValue(ROTATION, 0));
    }

    public int typeCount() {
        return switch (kind) {
            case BOBBLE -> TrinketTypes.BobbleType.values().length;
            case SNOWGLOBE -> TrinketTypes.SnowglobeType.values().length;
            case PLUSHIE -> PlushieType.values().length;
        };
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ROTATION);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(ROTATION, Mth.floor((ctx.getRotation() + 180.0F) * 16.0F / 360.0F + 0.5D) & 15);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (level.getBlockEntity(pos) instanceof TrinketBlockEntity te) {
            te.setType(TrinketBlockItem.getType(stack) % typeCount());
        }
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return switch (kind) {
            case BOBBLE -> BOBBLE_SHAPE;
            case SNOWGLOBE -> SNOWGLOBE_SHAPE;
            case PLUSHIE -> Shapes.block();
        };
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TrinketBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide && kind == Kind.PLUSHIE ? createTickerHelper(type, ModBlockEntities.TRINKET.get(), TrinketBlockEntity::clientTick) : null;
    }

    private ItemStack stackFor(BlockGetter level, BlockPos pos) {
        int type = level.getBlockEntity(pos) instanceof TrinketBlockEntity te ? te.type : 0;
        return TrinketBlockItem.make(asItem(), type);
    }

    @Override
    //? if < 1.21.1 {
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
    //?} else {
    /*public ItemStack getCloneItemStack(net.minecraft.world.level.LevelReader level, BlockPos pos, BlockState state) {
    *///?}
        return stackFor(level, pos);
    }

    //? if < 1.21.1 {
    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        dropTrinket(level, pos, player);
        super.playerWillDestroy(level, pos, state, player);
    }
    //?} else {
    /*@Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        dropTrinket(level, pos, player);
        return super.playerWillDestroy(level, pos, state, player);
    }
    *///?}

    private void dropTrinket(Level level, BlockPos pos, Player player) {
        if (!player.isCreative() && !level.isClientSide) {
            ItemEntity item = new ItemEntity(level, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, stackFor(level, pos));
            item.setDeltaMovement(0, 0, 0);
            level.addFreshEntity(item);
        }
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return activate(level, pos);
    }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return activate(level, pos);
    }
    *///?}

    private InteractionResult activate(Level level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof TrinketBlockEntity te)) return InteractionResult.PASS;

        if (kind == Kind.PLUSHIE) {
            if (level.isClientSide) {
                te.squishTimer = 11;
            } else if (TrinketTypes.safe(PlushieType.class, te.type) == PlushieType.HUNDUN) {
                level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, HbmSoundsNT.get("block.hunduns_magnificent_howl"), SoundSource.BLOCKS, 100F, 1F);
            } else {
                level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, HbmSoundsNT.get("block.squeakyToy"), SoundSource.BLOCKS, 0.25F, 1F);
            }
            return InteractionResult.SUCCESS;
        }

        if (level.isClientSide) {
            EnvExecutor.runInEnv(Env.CLIENT, () -> () -> com.hbm_m.client.gui.TrinketScreens.open(kind, te.type));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(ROTATION, rotation.rotate(state.getValue(ROTATION), 16));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(ROTATION, mirror.mirror(state.getValue(ROTATION), 16));
    }
    //? if >= 1.21.1 {
    /*public static final com.mojang.serialization.MapCodec<TrinketBlock> CODEC = com.hbm_m.platform.BlockCodecs.unsupported(TrinketBlock.class);
    @Override protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() { return CODEC; }
    *///?}
}
