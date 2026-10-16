package com.hbm_m.interfaces;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.item.ItemKey;
import com.hbm_m.item.ItemKeyPin;
import com.hbm_m.item.ModItems;
import com.hbm_m.platform.PlatformHooks;
import com.hbm_m.sound.ModSounds;

import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Порт {@code TileEntityLockableBase} (1.7.10) — навесной замок с числовым пин-кодом.
 *
 * <p>Оригинал был базовым классом TileEntity; здесь — интерфейс, т.к. {@code DoorBlockEntity}
 * уже унаследован от {@code BaseHbmBlockEntity}. Логика подбора ({@code tryPick}) и проверки
 * доступа ({@code canAccess}) совпадает с оригиналом 1:1:
 * <ul>
 *   <li>ключ в главной руке ({@code ItemKeyPin}) с совпадающим кодом — доступ + {@code block.lock_open};</li>
 *   <li>{@code key_red} — универсальный мастер-ключ;</li>
 *   <li>взлом: отмычка ({@code pin}) в руке + отвёртка в инвентаре (или наоборот),
 *       шанс = {@code lockMod * 100}, ×100 при баллистической куртке ({@code jackt}/
 *       {@code jackt2}) в нагрудном слоте; успех — {@code item.pin_unlock},
 *       провал — {@code item.pin_break} (pitch 0.8–1.0), 1 отмычка расходуется за попытку.</li>
 * </ul>
 */
public interface ILockable {

    /** Пин-код замка. 0 = не установлен (замок с кодом 0 повесить нельзя). */
    int getPins();

    void setPins(int pins);

    boolean isLocked();

    /** ПОВЕСИТЬ замок (isLocked = true). С кодом 0 запрещено — как в оригинале. */
    void lock();

    /** СНЯТЬ замок (isLocked = false). */
    void unlock();

    /** Базовый шанс взлома (0.1 = 10%). */
    double getLockMod();

    void setLockMod(double mod);

    /** Можно ли сделать поддельный ключ к этому замку ({@code key_kit}). */
    boolean isCheesable();

    void setCheesable(boolean cheesable);

    /** Позиция для звуков (у BlockEntity — worldPosition). */
    net.minecraft.core.BlockPos getLockPos();

    /** Уровень для звуков/инвентаря. */
    net.minecraft.world.level.Level getLockLevel();

    // ==================== canAccess / tryPick (порт 1:1) ====================

    /**
     * Порт {@code TileEntityLockableBase.canAccess}: незапертый замок пропускает всех;
     * запертый — только по ключу с совпадающим кодом, по {@code key_red} или через взлом.
     */
    default boolean canAccess(@Nullable Player player) {
        if (!isLocked()) return true;
        if (player == null) return false;

        ItemStack stack = player.getMainHandItem();
        // Только «настоящие» ключи (ItemKey: key/key_fake) открывают замок — как в оригинале,
        // где canAccess проверял instanceof ItemKey. Наследники ItemKeyPin (заготовки key_pin,
        // навесной замок ItemLock, отмычка не при делах) с совпадающим кодом НЕ открывают.
        if (!stack.isEmpty() && stack.getItem() instanceof ItemKey && ItemKeyPin.getCode(stack) == getPins()) {
            playLockOpen(player);
            return true;
        }
        if (!stack.isEmpty() && stack.getItem() == ModItems.KEY_RED.get()) {
            playLockOpen(player);
            return true;
        }
        return tryPick(player);
    }

    /** Звук отпирания ключом — у позиции игрока, как playSoundAtEntity в оригинале. */
    private void playLockOpen(Player player) {
        PlatformHooks.playSound(player.level(), player.blockPosition(),
                ModSounds.LOCK_OPEN.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    /**
     * Порт {@code TileEntityLockableBase.tryPick}: нужна отмычка и отвёртка, разделённые
     * между рукой и инвентарём (в любом сочетании). Каждая попытка расходует 1 отмычку.
     */
    default boolean tryPick(@Nullable Player player) {
        if (player == null) return false;

        double chanceOfSuccess = getLockMod() * 100; // 0.1 → 10
        boolean canPick = false;

        ItemStack stack = player.getMainHandItem();
        boolean hasScrewdriver = hasItem(player, ModItems.SCREWDRIVER.get())
                || hasItem(player, ModItems.SCREWDRIVER_DESH.get());
        boolean isScrewdriver = stack.getItem() == ModItems.SCREWDRIVER.get()
                || stack.getItem() == ModItems.SCREWDRIVER_DESH.get();

        if (stack.getItem() == ModItems.PIN.get() && hasScrewdriver) {
            stack.shrink(1);
            canPick = true;
        } else if (isScrewdriver && hasItem(player, ModItems.PIN.get())) {
            consumeOne(player, ModItems.PIN.get());
            canPick = true;
        }

        if (canPick) {
            // Jailor's Jacket (jackt/jackt2) в нагрудном слоте: шанс ×100 (10% → 100%)
            Item chest = player.getItemBySlot(EquipmentSlot.CHEST).getItem();
            if (chest == ModItems.JACKT.get() || chest == ModItems.JACKT2.get()) {
                chanceOfSuccess *= 100.0D;
            }
            double rand = player.getRandom().nextDouble() * 100;
            if (chanceOfSuccess > rand) {
                PlatformHooks.playSound(player.level(), player.blockPosition(),
                        ModSounds.PIN_UNLOCK.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
                return true;
            }
            PlatformHooks.playSound(player.level(), player.blockPosition(),
                    ModSounds.PIN_BREAK.get(), SoundSource.PLAYERS, 1.0F,
                    0.8F + player.getRandom().nextFloat() * 0.2F);
        }
        return false;
    }

    private static boolean hasItem(Player player, Item item) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (player.getInventory().getItem(i).is(item)) return true;
        }
        return false;
    }

    private static void consumeOne(Player player, Item item) {
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.is(item)) {
                stack.shrink(1);
                return;
            }
        }
    }
}
