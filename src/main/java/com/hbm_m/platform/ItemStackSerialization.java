package com.hbm_m.platform;

/**
 * Кросс-версионная NBT-сериализация инвенторя.
 * 1.20.1: serializeNBT()/deserializeNBT(Tag); 1.21.1: serializeNBT(Provider)/deserializeNBT(Provider,Tag).
 *
 * <p>Вынесен в отдельный файл из {@code ModItemStackHandler.java}, т.к. два public-класса
 * верхнего уровня в одном .java запрещены (compile error «should be declared in a file
 * named ItemStackSerialization.java»).
 */
public final class ItemStackSerialization {
    private ItemStackSerialization() {}

    public static net.minecraft.nbt.CompoundTag serialize(ModItemStackHandler handler, net.minecraft.core.HolderLookup.Provider registries) {
        //? if < 1.21.1 {
        return handler.serializeNBT();
        //?} else {
        /*return handler.serializeNBT(registries);
        *///?}
    }

    public static void deserialize(ModItemStackHandler handler, net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        //? if < 1.21.1 {
        handler.deserializeNBT(tag);
        //?} else {
        /*handler.deserializeNBT(registries, tag);
        *///?}
    }
}
