package com.hbm_m.block.decorations;

import com.hbm_m.item.ModItems;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.platform.PlatformHooks;
import com.hbm_m.worldgen.RedRoomGenerator;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Порт {@code BlockKeyhole} / {@code BlockRedBrickKeyhole} (1.7.10) — потайная скважина.
 *
 * <p>{@code blackRoom=false} — каменная скважина ({@code stone_keyhole}),
 * маскируется под обычную породу и ведёт в красную комнату;
 * {@code blackRoom=true} — кирпичная скважина ({@code stone_keyhole_meta}),
 * встраивается в стену красной комнаты и ведёт в чёрную комнату.</p>
 *
 * <p>Как и в 1.7.10, комната строится ЗА той гранью, по которой кликнул
 * игрок (сторона клика задаёт ориентацию, собственная ориентация блока из
 * worldgen не используется). При активации ключом ({@code key_red}, остаётся;
 * или {@code key_red_cracked}, расходуется) генерируется комната, а скважина
 * заменяется дверью.</p>
 */
public class KeyholeBlock extends Block {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    /** true — порт BlockRedBrickKeyhole (чёрная комната), false — BlockKeyhole (красная). */
    private final boolean blackRoom;

    public KeyholeBlock(Properties properties, boolean blackRoom) {
        super(properties);
        this.blackRoom = blackRoom;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
            net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
        return activate(state, level, pos, player, hit);
    }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hit) {
        return activate(state, level, pos, player, hit);
    }
    *///?}

    private InteractionResult activate(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        ItemStack held = player.getMainHandItem();
        if (!level.isClientSide) {
            Direction face = hit.getDirection();
            // В оригинале верх/низ не открывают комнату
            if (face == Direction.UP || face == Direction.DOWN) {
                return InteractionResult.PASS;
            }
            boolean cracked;
            if (held.is(ModItems.KEY_RED.get())) {
                cracked = false;
            } else if (held.is(ModItems.KEY_RED_CRACKED.get())) {
                cracked = true;
            } else {
                return InteractionResult.PASS;
            }
            if (cracked) {
                held.shrink(1);
            }
            if (level instanceof ServerLevel serverLevel) {
                if (blackRoom) {
                    RedRoomGenerator.generateBlackRoom(serverLevel, pos, face);
                } else {
                    RedRoomGenerator.generateRedRoom(serverLevel, pos, face);
                }
            }
            // Порт достижения redRoom ("The Other Side") из 1.7.10
            if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                net.minecraft.resources.ResourceLocation advId =
                        //? if < 1.21.1 {
                        new net.minecraft.resources.ResourceLocation(RefStrings.MODID, "red_room");
                        //?} else {
                        /*net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "red_room");
                        *///?}
                PlatformHooks.awardAdvancementIfEligible(serverPlayer, advId, true);
            }
            level.playSound(null, pos, SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, SoundSource.BLOCKS, 0.7F, 1.4F);
            return InteractionResult.CONSUME;
        }
        return held.is(ModItems.KEY_RED.get()) || held.is(ModItems.KEY_RED_CRACKED.get())
                ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }
}
