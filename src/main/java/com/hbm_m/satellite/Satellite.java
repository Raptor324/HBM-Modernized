package com.hbm_m.satellite;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.hbm_m.api.redstoneoverradio.IRORInteractive;
import com.hbm_m.blockentity.network.radio.RTTYNetwork;
import com.hbm_m.item.ModItems;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 1:1 {@code com.hbm.saveddata.satellites.SatelliteBase} + {@code XSatelliteRegistry}: ein Satellit im Orbit mit
 * Zielkoordinaten (targetX/targetZ), einem Antwortpuffer {@code tx} fuer die Befehle der Satelliten-Uplink-Schuessel
 * und den Aktionen fuer Designator/Koordinaten-Fernbedienung. Die IDs (0 Mapper ... 11 RayScan) entsprechen dem
 * Original; die Metadaten-Varianten von {@code satellite} sind im Port eigene Gegenstaende ({@code satellite_<typ>}).
 */
public abstract class Satellite {

    public static final String CHAN_SATLINK = "SAT_LINK";

    public static final String CMD_SETTARGET = "settarget";
    public static final String CMD_GETTARGET = "gettarget";
    public static final String CMD_GETTARGETX = "gettargetx";
    public static final String CMD_GETTARGETZ = "gettargetz";

    public int targetX;
    public int targetZ;
    public String tx = "";

    // ---------------- XSatelliteRegistry ----------------

    public static final BiMap<Integer, Class<? extends Satellite>> ID_TO_CLASS = HashBiMap.create(20);
    public static final Map<Item, Class<? extends Satellite>> ITEM_TO_CLASS = new HashMap<>();

    public static void register() {
        ID_TO_CLASS.put(0, SatelliteMapper.class);
        ID_TO_CLASS.put(1, SatelliteScanner.class);
        ID_TO_CLASS.put(2, SatelliteRadar.class);
        ID_TO_CLASS.put(3, SatelliteDeathRay.class);
        ID_TO_CLASS.put(4, SatelliteResonator.class);
        ID_TO_CLASS.put(5, SatelliteRelay.class);
        ID_TO_CLASS.put(6, SatelliteMiner.class);
        ID_TO_CLASS.put(7, SatelliteLunarMiner.class);
        ID_TO_CLASS.put(8, SatelliteHorizons.class);
        ID_TO_CLASS.put(9, SatellitePrecisionLaser.class);
        ID_TO_CLASS.put(10, SatelliteDetector.class);
        ID_TO_CLASS.put(11, SatelliteRayScan.class);
        ID_TO_CLASS.put(12, SatelliteScience.class);

        registerSatellite(SatelliteMapper.class, ModItems.SATELLITE_SPY);
        registerSatellite(SatelliteScanner.class, ModItems.SATELLITE_SCANNER);
        registerSatellite(SatelliteRadar.class, ModItems.SATELLITE_RADAR);
        registerSatellite(SatelliteDeathRay.class, ModItems.SATELLITE_DEATH_RAY);
        registerSatellite(SatelliteResonator.class, ModItems.SATELLITE_XENIUM_RESONATOR);
        registerSatellite(SatelliteRelay.class, ModItems.SATELLITE_RELAY);
        registerSatellite(SatelliteMiner.class, ModItems.SATELLITE_MINER_ASTRO);
        registerSatellite(SatelliteLunarMiner.class, ModItems.SATELLITE_MINER_LUNAR);
        registerSatellite(SatelliteHorizons.class, ModItems.SAT_GERALD);
        registerSatellite(SatellitePrecisionLaser.class, ModItems.SATELLITE_PRECISION_LASER);
        registerSatellite(SatelliteDetector.class, ModItems.SATELLITE_DETECTOR);
        registerSatellite(SatelliteRayScan.class, ModItems.SATELLITE_RAY_SCAN);
        registerSatellite(SatelliteScience.class, ModItems.SATELLITE_SCIENCE);
        // and all the legacy crap
        registerSatellite(SatelliteMapper.class, ModItems.SAT_MAPPER);
        registerSatellite(SatelliteScanner.class, ModItems.SAT_SCANNER);
        registerSatellite(SatelliteRadar.class, ModItems.SAT_RADAR);
        registerSatellite(SatelliteDeathRay.class, ModItems.SAT_LASER);
        registerSatellite(SatelliteResonator.class, ModItems.SAT_RESONATOR);
        registerSatellite(SatelliteMiner.class, ModItems.SAT_MINER);
        registerSatellite(SatelliteLunarMiner.class, ModItems.SAT_LUNAR_MINER);
    }

    private static void registerSatellite(Class<? extends Satellite> sat, Supplier<? extends Item> item) {
        // Original: nur wenn weder der Gegenstand noch die Satellitenklasse schon vergeben ist -
        // die Legacy-Chips (sat_mapper ...) bekommen dadurch keinen Satelliten
        if (!ITEM_TO_CLASS.containsKey(item.get()) && !ITEM_TO_CLASS.containsValue(sat)) {
            ITEM_TO_CLASS.put(item.get(), sat);
        }
    }

    public static Satellite createFromId(int i) {
        try {
            return ID_TO_CLASS.get(i).getDeclaredConstructor().newInstance();
        } catch (Exception e) { }
        return null;
    }

    public static Satellite createFromItem(ItemStack stack) {
        try {
            return ITEM_TO_CLASS.get(stack.getItem()).getDeclaredConstructor().newInstance();
        } catch (Exception e) { }
        return null;
    }

    // ---------------- SatelliteBase ----------------

    public int getID() {
        Integer id = ID_TO_CLASS.inverse().get(this.getClass());
        return id == null ? -1 : id;
    }

    public abstract String getType();

    // ---------------- Daten & Fracht (Original driveInput/driveOutput, requestableSlots) ----------------

    /** Original {@code EnumDriveType}: im Port eigene Gegenstaende, hier per Registriername ({@code drive_disk_empty} ...). */
    @org.jetbrains.annotations.Nullable public String driveInput = null;
    @org.jetbrains.annotations.Nullable public String driveOutput = null;

    /** Fertige Fracht, die ein Sat-Dock mit passendem Chip per Landekapsel abruft. */
    public ItemStack[] requestableSlots = new ItemStack[0];

    /** Original {@code isDirty}: der Weltticker markiert danach die Satellitendaten als geaendert. */
    public boolean isDirty = false;

    public void markDirty() {
        this.isDirty = true;
    }

    public void writeToNBT(CompoundTag nbt) {
        nbt.putInt("targetX", targetX);
        nbt.putInt("targetZ", targetZ);
        nbt.putString("tx", tx);

        if (driveInput != null) nbt.putString("driveInput", driveInput);
        if (driveOutput != null) nbt.putString("driveOutput", driveOutput);

        nbt.putInt("itemCount", this.requestableSlots.length);
        net.minecraft.nbt.ListTag items = new net.minecraft.nbt.ListTag();
        for (int i = 0; i < this.requestableSlots.length; i++) {
            if (this.requestableSlots[i] == null || this.requestableSlots[i].isEmpty()) continue;
            CompoundTag itemTag = new CompoundTag();
            itemTag.putByte("slot", (byte) i);
            com.hbm_m.platform.PlatformHooks.saveItemStack(this.requestableSlots[i], itemTag, com.hbm_m.platform.PlatformHooks.bestEffortProvider());
            items.add(itemTag);
        }
        nbt.put("requestableSlots", items);
    }

    public void readFromNBT(CompoundTag nbt) {
        this.targetX = nbt.getInt("targetX");
        this.targetZ = nbt.getInt("targetZ");
        this.tx = nbt.getString("tx");

        this.driveInput = nbt.contains("driveInput") ? nbt.getString("driveInput") : null;
        this.driveOutput = nbt.contains("driveOutput") ? nbt.getString("driveOutput") : null;

        int itemCount = nbt.getInt("itemCount");
        net.minecraft.nbt.ListTag items = nbt.getList("requestableSlots", 10);
        this.requestableSlots = new ItemStack[itemCount];
        java.util.Arrays.fill(this.requestableSlots, ItemStack.EMPTY);
        for (int i = 0; i < items.size(); i++) {
            CompoundTag itemTag = items.getCompound(i);
            int j = itemTag.getByte("slot") & 255;
            if (j < this.requestableSlots.length) this.requestableSlots[j] = com.hbm_m.platform.PlatformHooks.itemStackOf(itemTag, com.hbm_m.platform.PlatformHooks.bestEffortProvider());
        }
    }

    /** Ob Daten zum Abholen bereitliegen; darf {@link #produceData} ausloesen, wenn eine Abklingzeit abgelaufen ist. */
    public boolean hasData(ServerLevel world) {
        return this.driveInput != null && this.driveOutput != null;
    }

    /** Original {@code getOutputData}: das beschriebene Laufwerk fuer ein passendes leeres, sonst {@code null}. */
    @org.jetbrains.annotations.Nullable
    public Item getOutputData(ItemStack input) {
        if (driveInput == null || driveOutput == null || input.isEmpty()) return null;
        if (!net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(input.getItem()).getPath().equals(driveInput)) return null;
        Item out = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.hbm_m.lib.RefStrings.MODID, driveOutput));
        return out == net.minecraft.world.item.Items.AIR ? null : out;
    }

    public void produceData(String input, String output) {
        this.driveInput = input;
        this.driveOutput = output;
    }

    public void consumeData() {
        this.driveInput = null;
        this.driveOutput = null;
    }

    /** Jeden Weltentick (Original {@code ModEventHandler.onWorldTick}). */
    public void onUpdateTick(ServerLevel world) { }

    /** Original {@code getInfo}: Zeilen fuer die Anzeige am Satelliten-Uplink (leer = keine). */
    public java.util.List<net.minecraft.network.chat.Component> getInfo(ServerLevel world) {
        return java.util.List.of();
    }

    /** Original {@code tryRequestItems}: schickt die fertige Fracht per Landekapsel zum Dock. */
    public boolean tryRequestItems(ServerLevel world, int x, int y, int z) {
        if (this.requestableSlots == null || this.requestableSlots.length <= 0) return false;

        com.hbm_m.entity.missile.SatellitePodEntity pod = com.hbm_m.entity.ModEntities.SATELLITE_POD.get().create(world);
        if (pod == null) return false;
        pod.setup(y, requestableSlots);
        pod.setPos(x + 0.5, 300, z + 0.5);
        if (!world.addFreshEntity(pod)) return false;

        this.requestableSlots = new ItemStack[0];
        this.markDirty();
        return true;
    }

    /** When a satellite is created, i.e. this frequency is occupied for the first time */
    public void onOrbit(ServerLevel world, double x, double y, double z) {
        setTarget((int) Math.floor(x), (int) Math.floor(z));
        RTTYNetwork.broadcast(world, CHAN_SATLINK, "Established connection to " + getType() + " at " + targetX + " / " + targetZ);
    }

    /** For subsequent items sent under the same frequency as an existing satellite */
    public void onPartDelivered(ServerLevel world, ItemStack part) { }

    public void onCommand(ServerLevel world, String... cmd) {
        onCommandTarget(world, cmd);
        onCommandImpl(world, cmd);
    }

    public void onCommandTarget(ServerLevel world, String... cmd) {
        if (cmd.length <= 0) return;

        if (cmd[0].equals(CMD_SETTARGET)) {
            if (cmd.length == 3) {
                targetX = IRORInteractive.parseInt(cmd[1]);
                targetZ = IRORInteractive.parseInt(cmd[2]);
            }
            if (cmd.length == 4) {
                targetX = IRORInteractive.parseInt(cmd[1]);
                targetZ = IRORInteractive.parseInt(cmd[3]);
            }
            return;
        }
        if (cmd[0].equals(CMD_GETTARGET)) {
            this.tx = targetX + ";" + targetZ;
            return;
        }
        if (cmd[0].equals(CMD_GETTARGETX)) {
            this.tx = "" + targetX;
            return;
        }
        if (cmd[0].equals(CMD_GETTARGETZ)) {
            this.tx = "" + targetZ;
        }
    }

    public void setTarget(int x, int z) {
        this.targetX = x;
        this.targetZ = z;
    }

    public void onCommandImpl(ServerLevel world, String... cmd) { }

    public void onCoordAction(ServerLevel world, Player player, int x, int y, int z) { }
}
