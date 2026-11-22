package com.hbm_m.blockentity;

import net.minecraft.nbt.CompoundTag;

/**
 * Порт 1.7.10 {@code com.hbm.tileentity.IPersistentNBT}: состояние блок-сущности
 * сохраняется в дропнутом предмете под ключом {@link #NBT_PERSISTENT_KEY} и
 * восстанавливается при обратной установке.
 *
 * <p>В оригинале запись выполнялась из {@code Block#getDrops} через статический
 * хелпер интерфейса, восстановление — из {@code BlockDummyable.onBlockPlacedBy}.
 * В 1.20+/1.21+ пути дропа иные (LootParams / DataComponents), поэтому хелперы
 * живут в блоке машины ({@code MachineFluidTankBlock#getDrops}/{@code setPlacedBy}),
 * а интерфейс оставляет только пару запись/чтение.</p>
 */
public interface IPersistentNBT {

    String NBT_PERSISTENT_KEY = "persistent";

    void writeNBT(CompoundTag nbt);

    void readNBT(CompoundTag nbt);
}
