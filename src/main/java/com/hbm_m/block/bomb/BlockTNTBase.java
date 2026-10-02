package com.hbm_m.block.bomb;

import javax.annotation.Nullable;

import com.hbm_m.api.block.IToolable;
import com.hbm_m.entity.item.EntityTNTPrimedBase;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 1:1 {@code BlockTNTBase} (dynamite, semtex, tnt_ntm, fissure_bomb): Redstone, Feuer daneben, Feuerzeug und brennende
 * Pfeile zuenden (80 Ticks Lunte). {@code IGNITE_ON_BREAK} ist Meta-Bit 1 des Originals: der Schraubenzieher schaltet,
 * ob Abbauen zuendet; der Defuser baut den Block gefahrlos ab.
 */
public abstract class BlockTNTBase extends BlockDetonatable implements IToolable {

    public static final BooleanProperty IGNITE_ON_BREAK = BooleanProperty.create("ignite_on_break");

    public BlockTNTBase(Properties properties) {
        super(properties, 15, 100, 20, false, false);
        registerDefaultState(stateDefinition.any().setValue(IGNITE_ON_BREAK, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(IGNITE_ON_BREAK);
    }

    @Override
    public void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean moving) {
        super.onPlace(state, world, pos, oldState, moving);
        if (oldState.is(state.getBlock())) return;
        if (world.hasNeighborSignal(pos)) {
            prime(world, pos, true, null);
            world.removeBlock(pos, false);
        } else {
            checkAndIgnite(world, pos);
        }
    }

    @Override
    public void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos fromPos, boolean moving) {
        if (world.hasNeighborSignal(pos)) {
            prime(world, pos, true, null);
            world.removeBlock(pos, false);
        } else {
            checkAndIgnite(world, pos);
        }
    }

    public void checkAndIgnite(Level world, BlockPos pos) {
        if (shouldIgnite(world, pos)) {
            prime(world, pos, true, null);
            world.removeBlock(pos, false);
        }
    }

    /** {@code onBlockDestroyedByPlayer(meta)}: zuendet nur mit gesetztem Meta-Bit. */
    @Override
    public void destroy(LevelAccessor level, BlockPos pos, BlockState state) {
        if (level instanceof Level world) prime(world, pos, state.getValue(IGNITE_ON_BREAK), null);
    }

    public void prime(Level world, BlockPos pos, boolean ignite, @Nullable LivingEntity living) {
        if (!world.isClientSide && ignite) {
            EntityTNTPrimedBase primed = new EntityTNTPrimedBase(world, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, living, this);
            world.addFreshEntity(primed);
            world.playSound(null, primed.getX(), primed.getY(), primed.getZ(), SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack held = player.getItemInHand(hand);
        if (held.is(Items.FLINT_AND_STEEL)) {
            prime(world, pos, true, player);
            world.removeBlock(pos, false);
            held.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
            return InteractionResult.sidedSuccess(world.isClientSide);
        }
        return super.use(state, world, pos, player, hand, hit);
    }
    //?}

    @Override
    public void entityInside(BlockState state, Level world, BlockPos pos, Entity entity) {
        if (entity instanceof AbstractArrow arrow && !world.isClientSide && arrow.isOnFire()) {
            prime(world, pos, true, arrow.getOwner() instanceof LivingEntity l ? l : null);
            world.removeBlock(pos, false);
        }
    }

    @Override
    public void onProjectileHit(Level world, BlockState state, BlockHitResult hit, net.minecraft.world.entity.projectile.Projectile projectile) {
        if (projectile instanceof AbstractArrow) entityInside(state, world, hit.getBlockPos(), projectile);
    }

    @Override
    public boolean onScrew(Level world, Player player, BlockPos pos, Direction side, float fX, float fY, float fZ, InteractionHand hand, ToolType tool) {
        if (tool == ToolType.DEFUSER) {
            if (!world.isClientSide) {
                world.destroyBlock(pos, false);
                Block.popResource(world, pos, new ItemStack(this));
            }
            return true;
        }
        if (tool != ToolType.SCREWDRIVER) return false;

        if (!world.isClientSide) {
            BlockState state = world.getBlockState(pos);
            boolean on = !state.getValue(IGNITE_ON_BREAK);
            world.setBlock(pos, state.setValue(IGNITE_ON_BREAK, on), 3);
            player.sendSystemMessage(on
                    ? Component.literal("[ Ignite On Break: Enabled ]").withStyle(ChatFormatting.RED)
                    : Component.literal("[ Ignite On Break: Disabled ]").withStyle(ChatFormatting.GOLD));
        }
        return true;
    }
}
