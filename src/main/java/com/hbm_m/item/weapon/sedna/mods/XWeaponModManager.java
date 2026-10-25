package com.hbm_m.item.weapon.sedna.mods;

import com.hbm_m.platform.StackNbt;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import com.google.common.collect.HashBiMap;
import com.hbm_m.inventory.ComparableStack;
import com.hbm_m.item.weapon.sedna.ItemGunBaseNT;
import com.hbm_m.item.weapon.sedna.WeaponItems;
import com.hbm_m.item.weapon.sedna.factory.GunFactory.EnumModCaliber;
import com.hbm_m.item.weapon.sedna.factory.GunFactory.EnumModGeneric;
import com.hbm_m.item.weapon.sedna.factory.GunFactory.EnumModSpecial;
import com.hbm_m.item.weapon.sedna.mags.IMagazine;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 1:1 {@code XWeaponModManager}: The mod manager operates by scraping upgrades from a gun, then iterating over them and
 * evaluating the given value, passing the modified value to successive mods. The way that mods stack (additive vs
 * multiplicative) depends on the order the mod is installed in. Die Zuordnung Mod-Gegenstand -> Waffe steht in
 * {@link XWeaponModInit#init()} (Original {@code init()}), weil sie alle Waffen und Geschosskonfigurationen braucht.
 *
 * @author hbm
 */
public class XWeaponModManager {

    public static final String KEY_MOD_LIST = "KEY_MOD_LIST_";

    /** Mapping of mods to IDs, keep the register order consistent! */
    public static HashBiMap<Integer, IWeaponMod> idToMod = HashBiMap.create();
    /** Mapping of mod items to mod definitions */
    public static HashMap<ComparableStack, WeaponModDefinition> stackToMod = new HashMap<>();
    /** Map for turning individual mods back into their item form, used when uninstaling mods */
    public static HashMap<IWeaponMod, ItemStack> modToStack = new HashMap<>();

    private static boolean initialized = false;

    /** Assigns the IWeaponMod instances to items */
    public static void init() {
        if (initialized) return;
        initialized = true;
        XWeaponModInit.init();
    }

    public static final int ID_SILENCER = 201;
    public static final int ID_SCOPE = 202;
    public static final int ID_SAWED_OFF = 203;
    public static final int ID_NO_SHIELD = 204;
    public static final int ID_NO_STOCK = 205;
    public static final int ID_GREASEGUN_CLEAN = 206;
    public static final int ID_MINIGUN_SPEED = 208;
    public static final int ID_FURNITURE_GREEN = 211;
    public static final int ID_FURNITURE_BLACK = 212;
    public static final int ID_MAS_BAYONET = 213;
    public static final int ID_UZI_SATURN = 215;
    public static final int ID_LAS_SHOTGUN = 216;
    public static final int ID_LAS_CAPACITOR = 217;
    public static final int ID_LAS_AUTO = 218;
    public static final int ID_CARBINE_BAYONET = 219;
    public static final int ID_NI4NI_NICKEL = 220;
    public static final int ID_NI4NI_DOUBLOONS = 221;
    public static final int ID_DRILL_HSS = 222;
    public static final int ID_DRILL_WSTEEL = 223;
    public static final int ID_DRILL_TCALLOY = 224;
    public static final int ID_DRILL_SATURN = 225;
    public static final int ID_ENGINE_DIESEL = 226;
    public static final int ID_ENGINE_AVIATION = 227;
    public static final int ID_ENGINE_ELECTRIC = 228;
    public static final int ID_ENGINE_TURBO = 229;

    public static ItemStack[] getUpgradeItems(ItemStack stack, int cfg) {
        if (!StackNbt.has(stack)) return new ItemStack[0];
        int[] modIds = StackNbt.read(stack).getIntArray(KEY_MOD_LIST + cfg);
        if (modIds.length == 0) return new ItemStack[0];
        ItemStack[] mods = new ItemStack[modIds.length];
        for (int i = 0; i < mods.length; i++) {
            IWeaponMod mod = idToMod.get(modIds[i]);
            mods[i] = ItemStack.EMPTY;
            if (mod != null) {
                ItemStack s = modToStack.get(mod);
                if (s != null) mods[i] = s.copy();
            }
        }
        return mods;
    }

    public static boolean hasUpgrade(ItemStack stack, int cfg, int id) {
        if (!StackNbt.has(stack)) return false;
        int[] modIds = StackNbt.read(stack).getIntArray(KEY_MOD_LIST + cfg);
        for (int modId : modIds) {
            if (modId == id) return true;
        }
        return false;
    }

    private static Object prevMagType;
    private static int prevMagCount;
    private static boolean changedMagState = false;

    public static void changedMagState() {
        changedMagState = true;
    }

    /** Saves the state on receiver 0 so that if the mag changes through upgrading, the state may potentially be restored, if compatible */
    private static void saveMagState(ItemStack stack, int cfg) {
        IMagazine<?> mag = ((ItemGunBaseNT) stack.getItem()).getConfig(stack, cfg).getReceivers(stack)[0].getMagazine(stack);
        if (mag == null) return;
        prevMagType = mag.getType(stack, null);
        prevMagCount = mag.getAmount(stack, null);
    }

    private static void restoreMagState(ItemStack stack, int cfg) {
        if (!changedMagState) return;
        changedMagState = false;
        IMagazine<?> mag = ((ItemGunBaseNT) stack.getItem()).getConfig(stack, cfg).getReceivers(stack)[0].getMagazine(stack);
        if (mag == null) return;
        if (mag.getType(stack, null) == prevMagType) {
            mag.setAmount(stack, Mth.clamp(prevMagCount, 0, mag.getCapacity(stack)));
        } else {
            mag.setAmount(stack, 0);
        }
    }

    /**
     * Saves the mag state on receiver 0, uninstalls all existing mods to ensure there's no double install calls,
     * then installs the mods. If a mag state change has been reported, the mag on receiver 0 is validated,
     * i.e. if the type is still the same, the amount is restored, otherwise the mag is cleared.
     */
    public static void install(ItemStack stack, int cfg, ItemStack... mods) {
        saveMagState(stack, cfg);
        // we need to always clear things, so existing mods aren't installed twice, i.e. enchantment levels applied twice
        uninstall(stack, cfg);

        List<IWeaponMod> toInstall = new ArrayList<>();
        ComparableStack gun = new ComparableStack(stack).makeSingular();

        for (ItemStack mod : mods) {
            if (mod == null || mod.isEmpty()) continue;
            ComparableStack comp = new ComparableStack(mod).makeSingular();
            WeaponModDefinition def = stackToMod.get(comp);
            if (def != null) {
                IWeaponMod forGun = def.modByGun.get(gun);
                if (forGun != null) {
                    toInstall.add(forGun); //since this code only runs for upgrading, we can just indexOf because who cares
                } else {
                    forGun = def.modByGun.get(null);
                    if (forGun != null) toInstall.add(forGun);
                }
            }
        }

        if (toInstall.isEmpty()) return;
        toInstall.sort(modSorter);

        CompoundTag tag = StackNbt.orCreate(stack);
        int[] modIds = new int[toInstall.size()];
        for (int i = 0; i < modIds.length; i++) {
            IWeaponMod mod = toInstall.get(i);
            modIds[i] = idToMod.inverse().get(mod);
            onInstallStack(stack, modToStack.get(mod), cfg);
        }
        // 1.21.1: onInstallStack ersetzt die custom_data-Komponente -> Tag hier neu holen (1.20.1: dasselbe Objekt)
        StackNbt.orCreate(stack).putIntArray(KEY_MOD_LIST + cfg, modIds);
        restoreMagState(stack, cfg);
    }

    /** Wipes all mods from the gun */
    public static void uninstall(ItemStack stack, int cfg) {
        if (stack != null && StackNbt.has(stack)) {
            for (ItemStack mod : getUpgradeItems(stack, cfg)) {
                XWeaponModManager.onUninstallStack(stack, mod, cfg);
            }
            StackNbt.tag(stack).remove(KEY_MOD_LIST + cfg);
        }
    }

    public static void onInstallStack(ItemStack gun, ItemStack mod, int cfg) {
        IWeaponMod newMod = modFromStack(gun, mod, cfg);
        if (newMod == null) return;
        newMod.onInstall(gun, mod, cfg);
    }

    public static void onUninstallStack(ItemStack gun, ItemStack mod, int cfg) {
        IWeaponMod newMod = modFromStack(gun, mod, cfg);
        if (newMod == null) return;
        newMod.onUninstall(gun, mod, cfg);
    }

    public static IWeaponMod modFromStack(ItemStack gun, ItemStack mod, int cfg) {
        if (gun == null || mod == null || gun.isEmpty() || mod.isEmpty()) return null;
        WeaponModDefinition def = stackToMod.get(new ComparableStack(mod).makeSingular());
        if (def == null) return null;
        IWeaponMod newMod = def.modByGun.get(new ComparableStack(gun.getItem(), 1)); //shift clicking causes the gun to have stack size 0!
        if (newMod == null) newMod = def.modByGun.get(null);
        return newMod;
    }

    public static boolean isApplicable(ItemStack gun, ItemStack mod, int cfg, boolean checkMutex) {
        IWeaponMod newMod = modFromStack(gun, mod, cfg);
        if (newMod == null) return false; //if there's just no mod applicable

        if (checkMutex && StackNbt.has(gun)) for (int i : StackNbt.read(gun).getIntArray(KEY_MOD_LIST + cfg)) {
            IWeaponMod iMod = idToMod.get(i);
            if (iMod != null) for (String mutex0 : newMod.getSlots()) for (String mutex1 : iMod.getSlots()) {
                if (mutex0.equals(mutex1)) return false; //if any of the mod's slots are already taken
            }
        }

        return true; //yippie!
    }

    public static Comparator<IWeaponMod> modSorter = (o1, o2) -> o2.getModPriority() - o1.getModPriority();

    /** Scrapes all upgrades, iterates over them and evaluates the given value. The parent (i.e. holder of the base value)
     * is passed for context (so upgrades can differentiate primary and secondary receivers for example). Passing a null
     * stack causes the base value to be returned. */
    public static <T> T eval(T base, ItemStack stack, String key, Object parent, int cfg) {
        if (stack == null || stack.isEmpty()) return base;
        if (!StackNbt.has(stack)) return base;

        for (int i : StackNbt.read(stack).getIntArray(KEY_MOD_LIST + cfg)) {
            IWeaponMod mod = idToMod.get(i);
            if (mod != null) base = mod.eval(base, stack, key, parent);
        }

        return base;
    }

    public static class WeaponModDefinition {

        /** Holds the weapon mod handlers for each given gun. Key null refers to mods that apply to ALL guns that are otherwise not included. */
        public HashMap<ComparableStack, IWeaponMod> modByGun = new HashMap<>();
        public ItemStack stack;

        public WeaponModDefinition(ItemStack stack) {
            this.stack = stack;
            stackToMod.put(new ComparableStack(stack).makeSingular(), this);
        }

        public WeaponModDefinition(EnumModGeneric num) {
            this(new ItemStack(WeaponItems.WEAPON_MOD_GENERIC.get(num).get()));
        }

        public WeaponModDefinition(EnumModSpecial num) {
            this(new ItemStack(WeaponItems.WEAPON_MOD_SPECIAL.get(num).get()));
        }

        public WeaponModDefinition(EnumModCaliber num) {
            this(new ItemStack(WeaponItems.WEAPON_MOD_CALIBER.get(num).get()));
        }

        public WeaponModDefinition addMod(ItemStack gun, IWeaponMod mod) { return addMod(new ComparableStack(gun.getItem(), 1), mod); }
        public WeaponModDefinition addMod(Item gun, IWeaponMod mod) { return addMod(new ComparableStack(gun, 1), mod); }
        public WeaponModDefinition addMod(Item[] gun, IWeaponMod mod) { for (Item item : gun) addMod(new ComparableStack(item, 1), mod); return this; }
        public WeaponModDefinition addMod(ComparableStack gun, IWeaponMod mod) {
            modByGun.put(gun, mod);
            modToStack.put(mod, stack);
            if (gun != null && gun.getItem() instanceof ItemGunBaseNT nt) {
                ComparableStack comp = new ComparableStack(stack).makeSingular();
                if (!nt.recognizedMods.contains(comp)) nt.recognizedMods.add(comp);
            }
            return this;
        }

        public WeaponModDefinition addDefault(IWeaponMod mod) {
            return addMod((ComparableStack) null, mod);
        }
    }
}
