package com.hbm_m.extprop;

import java.util.Map;
import java.util.WeakHashMap;

import com.hbm_m.armormod.util.ArmorModificationHelper;
import com.hbm_m.handler.EnumKeybind;
import com.hbm_m.network.InfoToastPacket;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 1:1-Port von {@code com.hbm.extprop.HbmPlayerProps} (1.7.10): Spielerdaten, die nicht an einer
 * einzelnen Mechanik haengen - Tastenzustaende, Umschalter (HUD/Jetpack/Magnet), Dash-Ausdauer,
 * Ruestungs-"Plink"-Abklingzeit, die Schildinfusion, Laternen-Ruf, der Leiter-Hack fuer Multiblocks,
 * das Granaten-Ziehen und der Maskman-Timer.
 *
 * <p>Die dauerhaften Werte liegen im {@code PlayerPersisted}-Zweig der Entity-NBT, womit sie wie im
 * Original ({@code onPlayerClone}) den Tod ueberleben. Die fluechtigen Werte (Tasten, Dash) haelt
 * ein Objekt je Spielerinstanz.</p>
 */
public final class HbmPlayerProps {

    public static final String KEY = "HbmPlayerProps";

    private static final Map<Player, HbmPlayerProps> CACHE = new WeakHashMap<>();

    public final Player player;

    /* Toggles for keybind */
    public boolean enableHUD = true;
    public boolean enableBackpack = true;
    public boolean enableMagnet = true;

    /** Keybind tracking */
    private final boolean[] keysPressed = new boolean[EnumKeybind.values().length];

    /* Dashes for bismuth armor/cloud in a bottle */
    public boolean dashActivated = true;
    public int dashCooldown = 0;
    public int totalDashCount = 0;
    public int stamina = 0;
    public static final int dashCooldownLength = 5;

    /** Cooldown for armor plinking noise when canceling damage */
    public int plinkCooldown = 0;
    public static final int plinkCooldownLength = 10;

    /** Shield infusion */
    public float shield = 0;
    public float maxShield = 0;
    public int lastDamage = 0;
    public static final float shieldCap = 100;

    /** Latnern repair/destroy count */
    public int reputation;

    /** Hack for allowing ladders on multiblocks */
    public boolean isOnLadder = false;

    /** Pulling the pin on a grenade - it's a player prop instead of an NBT trait */
    public int grenadeDeployment;

    /** Maskman timer */
    public int maskManTimer = 0;

    private HbmPlayerProps(Player player) {
        this.player = player;
        load();
    }

    public static HbmPlayerProps get(Player player) {
        return CACHE.computeIfAbsent(player, HbmPlayerProps::new);
    }

    /** Alias fuer die Aufrufstellen des Originals ({@code getData}). */
    public static HbmPlayerProps getData(Player player) {
        return get(player);
    }

    public float shieldCap() {
        return shieldCap;
    }

    public boolean getKeyPressed(EnumKeybind key) {
        return keysPressed[key.ordinal()];
    }

    public boolean isJetpackActive() {
        return this.enableBackpack && getKeyPressed(EnumKeybind.JETPACK);
    }

    public boolean isMagnetActive() {
        return this.enableMagnet;
    }

    public void setKeyPressed(EnumKeybind key, boolean pressed) {
        if (!getKeyPressed(key) && pressed) {
            if (key == EnumKeybind.TOGGLE_JETPACK && player instanceof ServerPlayer sp) {
                this.enableBackpack = !this.enableBackpack;
                if (this.enableBackpack) InfoToastPacket.sendTo(sp, "Jetpack ON", 20, InfoToastPacket.ID_JETPACK, 0x55FF55);
                else InfoToastPacket.sendTo(sp, "Jetpack OFF", 20, InfoToastPacket.ID_JETPACK, 0xFF5555);
                save();
            }
            if (key == EnumKeybind.TOGGLE_MAGNET && player instanceof ServerPlayer sp) {
                this.enableMagnet = !this.enableMagnet;
                if (this.enableMagnet) InfoToastPacket.sendTo(sp, "Magnet ON", 20, InfoToastPacket.ID_MAGNET, 0x55FF55);
                else InfoToastPacket.sendTo(sp, "Magnet OFF", 20, InfoToastPacket.ID_MAGNET, 0xFF5555);
                save();
            }
            if (key == EnumKeybind.TOGGLE_HEAD && player instanceof ServerPlayer sp) {
                this.enableHUD = !this.enableHUD;
                if (this.enableHUD) InfoToastPacket.sendTo(sp, "HUD ON", 20, InfoToastPacket.ID_HUD, 0x55FF55);
                else InfoToastPacket.sendTo(sp, "HUD OFF", 20, InfoToastPacket.ID_HUD, 0xFF5555);
                save();
            }
            if (key == EnumKeybind.TRAIN && !player.level().isClientSide) {
                // Original: GUI des Waggons oeffnen, auf dem der Spieler sitzt (IGUIProvider).
                if (player.getVehicle() instanceof com.hbm_m.interfaces.ITrainGuiProvider provider && player instanceof ServerPlayer sp) {
                    provider.openTrainGui(sp);
                }
            }
        }
        keysPressed[key.ordinal()] = pressed;
    }

    public void setDashCooldown(int cooldown) { this.dashCooldown = cooldown; }
    public int getDashCooldown() { return this.dashCooldown; }
    public void setStamina(int stamina) { this.stamina = stamina; }
    public int getStamina() { return this.stamina; }
    public void setDashCount(int count) { this.totalDashCount = count; }
    public int getDashCount() { return this.totalDashCount; }

    public static void plink(Player player, SoundEvent sound, float volume, float pitch) {
        HbmPlayerProps props = get(player);
        if (props.plinkCooldown <= 0) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
            props.plinkCooldown = plinkCooldownLength;
        }
    }

    /** Maximaler Schild inklusive eines Schild-Moduls ({@code ItemModShield}) im Kevlar-Slot der Brust. */
    public float getEffectiveMaxShield() {
        float max = this.maxShield;
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!chest.isEmpty()) {
            ItemStack[] mods = ArmorModificationHelper.pryMods(chest);
            ItemStack kevlar = mods[ArmorModificationHelper.kevlar];
            if (kevlar != null && kevlar.getItem() instanceof com.hbm_m.armormod.item.IShieldMod mod) {
                max += mod.getShield();
            }
        }
        return max;
    }

    // ------------------------------------------------------------------ Persistenz

    private CompoundTag root() {
        CompoundTag data = player.getPersistentData();
        CompoundTag persisted = data.getCompound(Player.PERSISTED_NBT_TAG);
        data.put(Player.PERSISTED_NBT_TAG, persisted);
        return persisted;
    }

    public CompoundTag write() {
        CompoundTag props = new CompoundTag();
        props.putFloat("shield", shield);
        props.putFloat("maxShield", maxShield);
        props.putBoolean("enableBackpack", enableBackpack);
        props.putBoolean("enableMagnet", enableMagnet);
        props.putBoolean("enableHUD", enableHUD);
        props.putInt("reputation", reputation);
        props.putBoolean("isOnLadder", isOnLadder);
        props.putInt("maskManTimer", maskManTimer);
        return props;
    }

    public void read(CompoundTag props) {
        if (props == null || props.isEmpty()) return;
        this.shield = props.getFloat("shield");
        this.maxShield = props.getFloat("maxShield");
        this.enableBackpack = !props.contains("enableBackpack") || props.getBoolean("enableBackpack");
        this.enableMagnet = !props.contains("enableMagnet") || props.getBoolean("enableMagnet");
        this.enableHUD = !props.contains("enableHUD") || props.getBoolean("enableHUD");
        this.reputation = props.getInt("reputation");
        this.isOnLadder = props.getBoolean("isOnLadder");
        this.maskManTimer = props.getInt("maskManTimer");
    }

    public void save() {
        root().put(KEY, write());
    }

    private void load() {
        read(root().getCompound(KEY));
    }

    /** Schreibt und synchronisiert (Original: {@code ExtPropPacket} jeden Tick). */
    public void sync(Player p) {
        save();
        if (p instanceof ServerPlayer sp) com.hbm_m.network.ExtPropPacket.sendTo(sp);
    }

    /** Clientseitig vom {@code ExtPropPacket} gesetzt. */
    public void clientRead(CompoundTag tag) {
        read(tag);
    }
}
