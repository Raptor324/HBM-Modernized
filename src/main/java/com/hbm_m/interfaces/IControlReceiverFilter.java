package com.hbm_m.interfaces;

import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.platform.PlatformHooks;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Порт com.hbm.tileentity.IControlReceiverFilter (1.7.10): копирка слотов-фильтров.
 * Оригинал наследовал IControlReceiver — здесь достаточно ICopiable, приём
 * фильтров из GUI реализуют сами машины.
 *
 * <p>Мёртвое условие "router" из оригинала (slot > index*5 && slot < index*5)
 * не воспроизводится: портируется фактическое поведение — вставка всех скопированных
 * слотов фильтра подряд.
 */
public interface IControlReceiverFilter extends ICopiable {

    /** Начало (включительно) и конец (исключительно) диапазона слотов-фильтров. */
    int[] getFilterSlots();

    /** Переключить режим фильтра слота (whitelist/blacklist/...) после вставки. */
    void nextMode(int i);

    @Override
    default CompoundTag getSettings(Level level, BlockPos pos) {
        var inv = ((BaseMachineBlockEntity) this).getInventory();
        CompoundTag nbt = new CompoundTag();
        ListTag tags = new ListTag();
        int count = 0;
        for (int i = getFilterSlots()[0]; i < getFilterSlots()[1]; i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (!stack.isEmpty()) {
                CompoundTag slotNBT = new CompoundTag();
                slotNBT.putByte("slot", (byte) count);
                PlatformHooks.saveItemStack(stack, slotNBT, PlatformHooks.bestEffortProvider());
                tags.add(slotNBT);
            }
            count++;
        }
        nbt.put("items", tags);
        return nbt;
    }

    @Override
    default void pasteSettings(CompoundTag nbt, int index, Level level, Player player, BlockPos pos) {
        var inv = ((BaseMachineBlockEntity) this).getInventory();
        ListTag items = nbt.getList("items", Tag.TAG_COMPOUND);
        int listSize = items.size();
        if (listSize > 0) {
            int start = getFilterSlots()[0];
            int end = getFilterSlots()[1];
            for (int i = 0; i < listSize; i++) {
                CompoundTag slotNBT = items.getCompound(i);
                int slot = slotNBT.getByte("slot");
                ItemStack loaded = PlatformHooks.itemStackOf(slotNBT, PlatformHooks.bestEffortProvider());
                if (!loaded.isEmpty() && slot + start < end) {
                    loaded.setCount(1);
                    inv.setStackInSlot(slot + start, loaded);
                    nextMode(slot);
                }
            }
        }
        ((BlockEntity) this).setChanged();
    }

    @Override
    default String[] infoForDisplay(Level level, BlockPos pos) {
        return new String[] { "copytool.filter" };
    }
}
