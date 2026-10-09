package com.hbm_m.block.bomb;

import java.util.List;

import javax.annotation.Nullable;

import com.hbm_m.api.block.IFuckingExplode;
import com.hbm_m.api.block.IToolable;
import com.hbm_m.api.bomb.IBomb;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.bomb.ChargeBlockEntity;
import com.hbm_m.entity.item.EntityTNTPrimedBase;
import com.hbm_m.sound.HbmSoundsNT;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
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
 * 1:1 {@code BlockChargeBase}: Haftladung mit Zeitzuender. Klebt an der angeklickten Flaeche ({@code FACING} = Meta =
 * Seite), faellt die Unterlage weg oder wird sie ohne Defuser abgebaut, geht sie hoch. Rechtsklick waehlt die Zeit
 * (0, 5 s, 10 s, 15 s, 30 s, 1 min, 3 min, 5 min), geduckt scharf; der Defuser stoppt den Zuender bzw. baut die
 * entschaerfte Ladung ab.
 */
public abstract class BlockChargeBase extends BaseEntityBlock implements IBomb, IToolable, IFuckingExplode {

    public static boolean safe = false;

    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    private static final double F = 1.0 / 16.0;

    public BlockChargeBase(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ChargeBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.CHARGE.get(), ChargeBlockEntity::serverTick);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockState s = defaultBlockState().setValue(FACING, ctx.getClickedFace());
        return canSurvive(s, ctx.getLevel(), ctx.getClickedPos()) ? s : null;
    }

    /** {@code canPlaceBlockOnSide}: die Flaeche, an der die Ladung haftet, muss fest sein. */
    @Override
    public boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
        Direction dir = state.getValue(FACING);
        BlockPos support = pos.relative(dir.getOpposite());
        return world.getBlockState(support).isFaceSturdy(world, support, dir);
    }

    @Override
    public void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos fromPos, boolean moving) {
        if (!canSurvive(state, world, pos)) world.removeBlock(pos, false);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext ctx) {
        return switch (state.getValue(FACING)) {
            case DOWN -> Shapes.box(0, 10 * F, 0, 1, 1, 1);
            case UP -> Shapes.box(0, 0, 0, 1, 6 * F, 1);
            case NORTH -> Shapes.box(0, 0, 10 * F, 1, 1, 1);
            case SOUTH -> Shapes.box(0, 0, 0, 1, 1, 6 * F);
            case WEST -> Shapes.box(10 * F, 0, 0, 1, 1, 1);
            case EAST -> Shapes.box(0, 0, 0, 6 * F, 1, 1);
        };
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext ctx) {
        return Shapes.empty();
    }

    @Override
    public boolean onScrew(Level world, Player player, BlockPos pos, Direction side, float fX, float fY, float fZ, InteractionHand hand, ToolType tool) {
        if (tool != ToolType.DEFUSER) return false;
        if (!(world.getBlockEntity(pos) instanceof ChargeBlockEntity charge)) return false;

        if (charge.started) {
            charge.started = false;
            world.playSound(null, pos, HbmSoundsNT.get("weapon.fstbmbStart"), SoundSource.BLOCKS, 1.0F, 1.0F);
            charge.setChanged();
        } else if (!world.isClientSide) {
            safe = true;
            dismantle(world, pos);
            safe = false;
        }
        return true;
    }

    /** {@code BlockBase.dismantle}: entfernen und als Gegenstand auswerfen. */
    public void dismantle(Level world, BlockPos pos) {
        world.removeBlock(pos, false);
        float f = world.random.nextFloat() * 0.6F + 0.2F;
        float f1 = world.random.nextFloat() * 0.2F;
        float f2 = world.random.nextFloat() * 0.6F + 0.2F;
        ItemEntity item = new ItemEntity(world, pos.getX() + f, pos.getY() + f1 + 1, pos.getZ() + f2, new ItemStack(this));
        float f3 = 0.05F;
        item.setDeltaMovement(world.random.nextGaussian() * f3, world.random.nextGaussian() * f3 + 0.2F, world.random.nextGaussian() * f3);
        if (!world.isClientSide) world.addFreshEntity(item);
    }

    /** {@code breakBlock}: wer die Ladung ohne Defuser entfernt, bringt sie zur Explosion. */
    @Override
    public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean moving) {
        boolean replaced = !state.is(newState.getBlock());
        super.onRemove(state, world, pos, newState, moving);
        if (replaced && !safe && !world.isClientSide) explode(world, pos);
    }

    @Override
    public void wasExploded(Level world, BlockPos pos, Explosion explosion) {
        if (!world.isClientSide) {
            EntityTNTPrimedBase tntPrimed = new EntityTNTPrimedBase(world, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                    explosion != null ? explosion.getIndirectSourceEntity() : null, this);
            tntPrimed.fuse = 0;
            tntPrimed.detonateOnCollision = false;
            world.addFreshEntity(tntPrimed);
        }
    }

    @Override
    public boolean dropFromExplosion(Explosion explosion) {
        return false;
    }

    @Override
    public void explodeEntity(Level world, double x, double y, double z, @Nullable EntityTNTPrimedBase entity) {
        explode(world, BlockPos.containing(x, y, z));
    }

    @Override
    //? if < 1.21.1 {
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> list, TooltipFlag flag) {
    //?} else {
    /*public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext hbmTooltipCtx, List<Component> list, TooltipFlag flag) {
    *///?}
        list.add(Component.literal("Right-click to change timer.").withStyle(ChatFormatting.YELLOW));
        list.add(Component.literal("Sneak-click to arm.").withStyle(ChatFormatting.YELLOW));
        list.add(Component.literal("Can only be disarmed and removed with defuser.").withStyle(ChatFormatting.RED));
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return activate(world, pos, player);
    }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        return activate(world, pos, player);
    }
    *///?}

    private InteractionResult activate(Level world, BlockPos pos, Player player) {
        if (world.isClientSide) return InteractionResult.SUCCESS;
        if (!(world.getBlockEntity(pos) instanceof ChargeBlockEntity charge)) return InteractionResult.PASS;

        if (!charge.started) {
            if (player.isShiftKeyDown()) {
                if (charge.timer > 0) {
                    charge.started = true;
                    world.playSound(null, pos, HbmSoundsNT.get("weapon.fstbmbStart"), SoundSource.BLOCKS, 1.0F, 1.0F);
                }
            } else {
                charge.timer = switch (charge.timer) {
                    case 0 -> 100;
                    case 100 -> 200;
                    case 200 -> 300;
                    case 300 -> 600;
                    case 600 -> 1200;
                    case 1200 -> 3600;
                    case 3600 -> 6000;
                    default -> 0;
                };
                world.playSound(null, pos, HbmSoundsNT.get("item.techBoop"), SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            charge.setChanged();
        }
        // Original: onBlockActivated gibt serverseitig false zurueck
        return InteractionResult.PASS;
    }
}
