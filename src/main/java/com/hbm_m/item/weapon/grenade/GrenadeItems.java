package com.hbm_m.item.weapon.grenade;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

import com.hbm_m.item.ModItems;
import com.hbm_m.item.weapon.grenade.ItemGrenadeExtra.EnumGrenadeExtra;
import com.hbm_m.item.weapon.grenade.ItemGrenadeFilling.EnumGrenadeFilling;
import com.hbm_m.item.weapon.grenade.ItemGrenadeFuze.EnumGrenadeFuze;
import com.hbm_m.item.weapon.grenade.ItemGrenadeShell.EnumGrenadeShell;

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.world.item.Item;

/**
 * Registrierung des Baukasten-Granatensystems (Original {@code grenade_shell/filling/fuze/extra} als Metadaten-Items,
 * im Port je Meta ein Gegenstand {@code grenade_<teil>_<typ>}, sowie {@code grenade_universal}).
 */
public final class GrenadeItems {

    private GrenadeItems() { }

    public static final Map<EnumGrenadeShell, RegistrySupplier<Item>> GRENADE_SHELL = new EnumMap<>(EnumGrenadeShell.class);
    public static final Map<EnumGrenadeFilling, RegistrySupplier<Item>> GRENADE_FILLING = new EnumMap<>(EnumGrenadeFilling.class);
    public static final Map<EnumGrenadeFuze, RegistrySupplier<Item>> GRENADE_FUZE = new EnumMap<>(EnumGrenadeFuze.class);
    public static final Map<EnumGrenadeExtra, RegistrySupplier<Item>> GRENADE_EXTRA = new EnumMap<>(EnumGrenadeExtra.class);
    public static RegistrySupplier<Item> GRENADE_UNIVERSAL;

    private static String id(Enum<?> e) { return e.name().toLowerCase(Locale.US); }

    /** Von {@code ModItems} aufgerufen (Original-Reihenfolge: shell, filling, fuze, extra, universal). */
    public static void registerAll() {
        for (EnumGrenadeShell e : EnumGrenadeShell.values()) GRENADE_SHELL.put(e, ModItems.ITEMS.register("grenade_shell_" + id(e), () -> new ItemGrenadeShell(e, new Item.Properties())));
        for (EnumGrenadeFilling e : EnumGrenadeFilling.values()) GRENADE_FILLING.put(e, ModItems.ITEMS.register("grenade_filling_" + id(e), () -> new ItemGrenadeFilling(e, new Item.Properties())));
        for (EnumGrenadeFuze e : EnumGrenadeFuze.values()) GRENADE_FUZE.put(e, ModItems.ITEMS.register("grenade_fuze_" + id(e), () -> new ItemGrenadeFuze(e, new Item.Properties())));
        for (EnumGrenadeExtra e : EnumGrenadeExtra.values()) GRENADE_EXTRA.put(e, ModItems.ITEMS.register("grenade_extra_" + id(e), () -> new ItemGrenadeExtra(e, new Item.Properties())));
        GRENADE_UNIVERSAL = ModItems.ITEMS.register("grenade_universal", () -> new ItemGrenadeUniversal(new Item.Properties()));
    }

    /** Original {@code DispenserBehaviorHandler}: der Spender wirft Baukastengranaten mit der Wurfkraft der Huelle. */
    public static void registerDispenser() {
        net.minecraft.world.level.block.DispenserBlock.registerBehavior(GRENADE_UNIVERSAL.get(), new net.minecraft.core.dispenser.DefaultDispenseItemBehavior() {
            @Override
            protected net.minecraft.world.item.ItemStack execute(net.minecraft.core.BlockSource source, net.minecraft.world.item.ItemStack stack) {

                net.minecraft.core.Direction enumfacing = source.getBlockState().getValue(net.minecraft.world.level.block.DispenserBlock.FACING);

                com.hbm_m.entity.grenade.EntityGrenadeUniversal grenade = new com.hbm_m.entity.grenade.EntityGrenadeUniversal(source.getLevel(), stack);
                EnumGrenadeShell shell = grenade.getShell();

                // this kinda sucks ass but it works so i'm not complaining
                grenade.setPosition(source.x() + enumfacing.getStepX() * 0.75, source.y() + enumfacing.getStepY() * 0.75, source.z() + enumfacing.getStepZ() * 0.75);
                grenade.setDeltaMovement(enumfacing.getStepX() * shell.getYeetForce(), enumfacing.getStepY() * shell.getYeetForce(), enumfacing.getStepZ() * shell.getYeetForce());
                source.getLevel().addFreshEntity(grenade);

                stack.shrink(1);
                return stack;
            }
        });
    }

    public static Item shell(EnumGrenadeShell e) { return GRENADE_SHELL.get(e).get(); }
    public static Item filling(EnumGrenadeFilling e) { return GRENADE_FILLING.get(e).get(); }
    public static Item fuze(EnumGrenadeFuze e) { return GRENADE_FUZE.get(e).get(); }
    public static Item extra(EnumGrenadeExtra e) { return GRENADE_EXTRA.get(e).get(); }
}
