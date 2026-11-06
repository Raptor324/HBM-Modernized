package com.hbm_m.handler;

import com.hbm_m.item.ItemCounterfeitKeys;
import com.hbm_m.item.ItemLock;
import com.hbm_m.util.LockHelper;
import com.hbm_m.interfaces.ILockable;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Общая логика гарда открытия запертых ванильных контейнеров (сундуки, шалкеры,
 * бочки). Порт проверки {@code TileEntityCrateBase.canAccess} из 1.7.10: запертый
 * контейнер не открывает GUI, пока игрок не докажет доступ (ключ/взлом).
 *
 * <p>Подписчики: {@code LockClickGuardForge}/{@code LockClickGuardNeoForge} —
 * глушат только {@code useBlock} ({@code setUseBlock(DENY)}), чтобы useOn предметов
 * (key_kit, padlock) продолжал работать поверх запертого контейнера. При этих
 * предметах в руке {@code canAccess} заранее не зовётся — иначе попытка взлома
 * сожгла бы отмычку до того, как сработает useOn.
 */
public final class LockClickGuard {

    private LockClickGuard() {}

    /** true — контейнер заперт и доступ не подтверждён: открытие GUI нужно заглушить. */
    public static boolean shouldDenyOpen(Player player, Level level, net.minecraft.core.BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        ILockable lockable = LockHelper.resolve(be);
        if (lockable == null || !lockable.isLocked()) return false;

        // Замочные предметы решают свою логику в useOn — не мешаем им
        net.minecraft.world.item.ItemStack held = player.getMainHandItem();
        if (held.getItem() instanceof ItemCounterfeitKeys || held.getItem() instanceof ItemLock) {
            return false;
        }
        return !lockable.canAccess(player);
    }
}
