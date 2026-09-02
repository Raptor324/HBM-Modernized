package com.hbm_m.compat.curios;

import java.util.UUID;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import com.google.common.collect.Multimap;
import com.hbm_m.item.gasmask.GasMaskUtil;

import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;

/**
 * Обёртка противогаза для слота лица Curios.
 * Наши противогазы нельзя надеть в слот лица,
 * пока на голове элемент силовой брони ({@link com.hbm_m.powerarmor.ModArmorFSBPowered}
 * — закрывает лицо, маска под него не пролезает).
 */
public class GasMaskCurio implements ICurio {

    private final ItemStack stack;

    public GasMaskCurio(ItemStack stack) {
        this.stack = stack;
    }

    @Override
    public ItemStack getStack() {
        return stack;
    }

    @Override
    public boolean canEquip(SlotContext ctx) {
        // Силовая броня на голове закрывает лицо; второй противогаз (на голове) — тоже нельзя.
        return !CuriosAccess.isPowerArmorHead(ctxWearer(ctx)) && !GasMaskUtil.isMaskOnHead(ctxWearer(ctx));
    }

    /** Доступ к сущности из SlotContext: геттер переименован между версиями Curios. */
    private static LivingEntity ctxWearer(SlotContext ctx) {
        //? if < 1.21.1 {
        return ctx.getWearer();
        //?} else {
        /*return ctx.entity();
         *///?}
    }

    @Override
    public boolean canEquipFromUse(SlotContext ctx) {
        return canEquip(ctx);
    }

    /**
     * Атрибуты маски в слоте лица — как при ношении на голове (очки брони и т.п.).
     * Ванильная броня даёт модификаторы с общими на слот UUID — если вернуть их как есть,
     * они конфликтуют с шлемом на голове (атрибут не суммируется). Поэтому переключаем
     * каждый модификатор на UUID, выданный Curios. В 1.21.1 тип ключа — Holder<Attribute>,
     * а id модификатора — ResourceLocation вместо UUID.
     */
    //? if < 1.21.1 {
    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(SlotContext ctx, UUID uuid) {
        Multimap<Attribute, AttributeModifier> base = this.stack.getItem().getDefaultAttributeModifiers(EquipmentSlot.HEAD);
        if (base.isEmpty()) {
            return base;
        }
        com.google.common.collect.ImmutableMultimap.Builder<Attribute, AttributeModifier> builder =
                com.google.common.collect.ImmutableMultimap.builder();
        for (java.util.Map.Entry<Attribute, AttributeModifier> e : base.entries()) {
            AttributeModifier m = e.getValue();
            builder.put(e.getKey(), new AttributeModifier(uuid, m.getName() + "_curios", m.getAmount(), m.getOperation()));
        }
        return builder.build();
    }
    //?} else {
    /*@Override
    public Multimap<net.minecraft.core.Holder<Attribute>, AttributeModifier> getAttributeModifiers(SlotContext ctx, net.minecraft.resources.ResourceLocation id) {
        // В 9.x вызывается только эта перегрузка (UUID-версия deprecated и не вызывается).
        com.google.common.collect.ImmutableMultimap.Builder<net.minecraft.core.Holder<Attribute>, AttributeModifier> builder =
                com.google.common.collect.ImmutableMultimap.builder();
        this.stack.forEachModifier(EquipmentSlot.HEAD, (holder, m) -> builder.put(holder,
                new AttributeModifier(id, m.amount(), m.operation())));
        return builder.build();
    }
     *///?}
}
