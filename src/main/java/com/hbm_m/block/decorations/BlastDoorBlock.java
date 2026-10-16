package com.hbm_m.block.decorations;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.bomb.IBomb;
import com.hbm_m.block.entity.doors.BlastDoorBlockEntity;
import com.hbm_m.block.entity.doors.DoorBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.item.ItemCounterfeitKeys;
import com.hbm_m.item.ItemLock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Порт NEXT {@code BlastDoor} — вертикальная сдвижная дверь-шторка (ядро мультиблока 1x7).
 *
 * <p>Отличия от базового {@link DoorBlock} (паритет NEXT useAtCore/useItemOnAtCore):
 * <ul>
 *   <li>клик переключает дверь БЕЗ спец-пути отвёртки (GUI скинов у двери нет,
 *       отвёртка просто тогглит — NEXT parity);</li>
 *   <li>замок/набор ключей в руке отдаёт ход useOn предмета (PASS);</li>
 *   <li>IBomb: детонатор тогглит незапертую дверь, запертая — ERROR_INCOMPATIBLE;</li>
 *   <li>серверный тикер — {@link BlastDoorBlockEntity#serverTick} (rising-edge
 *       редстоун-опрос каждый тик; клиентский — стандартная интерполяция).</li>
 * </ul>
 */
public class BlastDoorBlock extends DoorBlock implements IBomb {

    public BlastDoorBlock(Properties properties, String doorDeclId) {
        super(properties, doorDeclId);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlastDoorBlockEntity(pos, state);
    }

    /**
     * Серверный тикер ОБЯЗАН вести на {@link BlastDoorBlockEntity#serverTick}: внутри
     * него живёт pollRedstone() (rising-edge детект) — базовый DoorBlockEntity.serverTick
     * редстоун не опрашивает (checkRedstonePower у blast door заглушен).
     */
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            BlockEntityTicker<T> blast = createTickerHelper(type, ModBlockEntities.BLAST_DOOR_ENTITY.get(),
                    (world, pos, blockState, blockEntity) -> DoorBlockEntity.clientTick(world, pos, blockState, (DoorBlockEntity) blockEntity));
            BlockEntityTicker<T> legacy = createTickerHelper(type, ModBlockEntities.DOOR_ENTITY.get(),
                    (world, pos, blockState, blockEntity) -> DoorBlockEntity.clientTick(world, pos, blockState, (DoorBlockEntity) blockEntity));
            return blast != null ? blast : legacy;
        }
        // Новый тип: честный BlastDoorBlockEntity (rising-edge редстоун, терминальные
        // состояния, BFS-каскад). Старый hbm_m:door (дверь поставлена до появления
        // выделенного BE — тип BE восстанавливается из NBT): касть нельзя без проверки,
        // ClassCastException роняет тик уровня — безопасная деградация в базовый тик.
        BlockEntityTicker<T> blast = createTickerHelper(type, ModBlockEntities.BLAST_DOOR_ENTITY.get(),
                BlastDoorBlockEntity::serverTick);
        BlockEntityTicker<T> legacy = createTickerHelper(type, ModBlockEntities.DOOR_ENTITY.get(),
                (world, pos, blockState, blockEntity) -> {
                    if (blockEntity instanceof BlastDoorBlockEntity blastDoor) {
                        BlastDoorBlockEntity.serverTick(world, pos, blockState, blastDoor);
                    } else {
                        DoorBlockEntity.serverTick(world, pos, blockState, (DoorBlockEntity) blockEntity);
                    }
                });
        return blast != null ? blast : legacy;
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return hbmOnUse(state, level, pos, player, hand, hit);
    }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return hbmOnUse(state, level, pos, player, InteractionHand.MAIN_HAND, hit);
    }
    *///?}

    /**
     * Порт NEXT useAtCore + useItemOnAtCore (BlastDoor.java:85-105): shift — PASS;
     * замок/набор ключей в руке — PASS (их useOn вешает замок на контроллер через
     * LockHelper); иначе серверный tryToggle. Без спец-пути отвёртки: у этой двери
     * нет GUI скинов, отвёртка тогглит дверь.
     */
    private InteractionResult hbmOnUse(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        ItemStack held = player.getItemInHand(hand);
        if (held.getItem() instanceof ItemLock || held.getItem() instanceof ItemCounterfeitKeys) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof BlastDoorBlockEntity door) {
            door.tryToggle(player);
        }
        return InteractionResult.SUCCESS;
    }

    // ==================== IBomb (порт NEXT BlastDoor.explode, :108-115) ====================

    @Override
    public BombReturnCode explode(Level level, BlockPos pos) {
        if (level.isClientSide) return BombReturnCode.UNDEFINED;
        if (!(level.getBlockEntity(pos) instanceof BlastDoorBlockEntity door)) return BombReturnCode.UNDEFINED;
        if (door.isLocked()) return BombReturnCode.ERROR_INCOMPATIBLE;
        door.tryToggle(null);
        return BombReturnCode.TRIGGERED;
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<BlastDoorBlock> CODEC = simpleCodec(props -> new BlastDoorBlock(props, "blast_door"));

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return CODEC;
    }
    *///?}
}
