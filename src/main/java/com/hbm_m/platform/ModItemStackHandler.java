package com.hbm_m.platform;

//? if forge {
import net.minecraftforge.items.ItemStackHandler;
//?} elif neoforge {
/*import net.neoforged.neoforge.items.ItemStackHandler;
*///?}

/**
 * Кросс-версионный инвентарь-хендлер: 1.20.1 {@code serializeNBT()/deserializeNBT(Tag)},
 * 1.21.1 {@code serializeNBT(Provider)/deserializeNBT(Provider, Tag)} — хелперы
 * {@link #serialize}/{@link #deserialize} скрывают разницу (бывший ItemStackSerialization).
 */
public abstract class ModItemStackHandler extends ItemStackHandler {

    public ModItemStackHandler(int size) {
        super(size);
    }

    public net.minecraft.nbt.CompoundTag serialize(net.minecraft.core.HolderLookup.Provider registries) {
        //? if < 1.21.1 {
        return serializeNBT();
        //?} else {
        /*return serializeNBT(registries);
        *///?}
    }

    public void deserialize(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        //? if < 1.21.1 {
        deserializeNBT(tag);
        //?} else {
        /*deserializeNBT(registries, tag);
        *///?}
    }

    /**
     * Пустая заглушка-хендлер для меню, открытых на клиенте без BlockEntity
     * (например, при проигрывании реплея Flashback: виртуальный мир не содержит тайлов).
     * Позволяет конструктору меню создать слоты и не уронить обработку пакета
     * ClientboundOpenScreenPacket. Все изменения содержимого молча отбрасываются.
     */
    public static final class Dummy extends ModItemStackHandler {

        public Dummy(int size) {
            super(size);
        }

        @Override
        protected void onContentsChanged(int slot) {
            // заглушка: на клиенте без тайла изменения никуда не сохраняются
        }
    }
}
