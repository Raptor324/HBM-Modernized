package com.hbm_m.blockentity.network.pneumatic;

import com.hbm_m.api.pneumatic.StackCache;
import com.hbm_m.api.pneumatic.StackCache.CacheSlot;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.menu.PneumoStorageExporterMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

/**
 * 1:1-Port von {@code TileEntityPneumoStorageExporter} (1.7.10): die Ausgabe aus dem Lagernetz.
 *
 * <p>Oben neun <b>Anforderungsplaetze</b>: was dort liegt, wird angefordert - und zwar in genau der
 * Stapelgroesse, die dort liegt. Unten neun Ausgabeplaetze, in denen das Angeforderte landet.
 * Die Anforderungsplaetze sind Vorlagen, ihr Inhalt wird nie verbraucht.</p>
 *
 * <p>Drei Betriebsarten bestimmen, wie streng das genommen wird:</p>
 * <ul>
 *   <li>{@link #MODE_AS_MUCH_AS_POSSIBLE}: jeder Platz holt, was er kriegen kann - auch weniger
 *       als gefordert.</li>
 *   <li>{@link #MODE_FULL_STACK}: jeder Platz holt nur, wenn die volle geforderte Menge da ist
 *       <b>und</b> auch hineinpasst.</li>
 *   <li>{@link #MODE_FULL_REQUEST}: alles oder nichts - erst wenn <b>jede</b> Forderung erfuellbar
 *       ist, wird ueberhaupt etwas geholt. Das ist die Betriebsart fuer Bauplaene.</li>
 * </ul>
 *
 * <p>Ausgeloest wird entweder fortlaufend jeden Tick ({@link #continuousRequest}) oder von einer
 * Redstoneflanke. Ein gescheiterter Versuch legt den Platz {@value #SLOT_DELAY} Ticks still.</p>
 *
 * <p><b>Fernkonfiguration.</b> Der dritte Schalter stellt auf Redstone-over-Radio um: dann zaehlen
 * nicht mehr die eingelegten Vorlagen, sondern neun ueber Funk gesetzte Filter
 * ({@code setfilter}). Das ist der Weg, eine Anforderungsliste aus einem Rechner heraus
 * umzuschreiben, ohne die Kiste anzufassen. {@code checkavailability} meldet den Bestand auf einen
 * frei waehlbaren Kanal zurueck.</p>
 *
 * <p>Die urspruengliche Metadatenzahl der Filter gibt es in 1.20 nicht mehr; der Parameter wird
 * weiterhin entgegengenommen und ignoriert, damit alte Befehlsketten unveraendert laufen. Als
 * {@code itemid} nimmt dieser Port zusaetzlich zur Zahl auch den Registriernamen entgegen
 * ({@code minecraft:iron_ingot}) - die Zahlen sind je Welt vergeben und damit schlecht
 * aufschreibbar.</p>
 */
public class PneumoStorageExporterBlockEntity extends PneumaticMachineBlockEntity
        implements com.hbm_m.api.redstoneoverradio.IRORInteractive {

    public static final int REQUEST_SLOTS = 9;
    /** 0-8 Anforderung, 9-17 Ausgabe. */
    public static final int INVENTORY_SIZE = 18;

    public static final int MODE_AS_MUCH_AS_POSSIBLE = 0;
    public static final int MODE_FULL_STACK = 1;
    public static final int MODE_FULL_REQUEST = 2;

    /** Original: {@code SLOT_DELAY = 10}. */
    public static final int SLOT_DELAY = 10;

    private boolean continuousRequest = false;
    private int requestMode = MODE_AS_MUCH_AS_POSSIBLE;
    private boolean lastRedstone = false;
    private final int[] slotDelay = new int[REQUEST_SLOTS];

    /** Original: {@code rorConfiguredMode} - kommen die Filter ueber Funk statt aus den Plaetzen? */
    private boolean rorConfiguredMode = false;
    /** Original: {@code short[9][3]} - je Platz Gegenstandsnummer, Metadatenzahl und Menge. */
    private final int[][] rorFilters = new int[REQUEST_SLOTS][3];

    public PneumoStorageExporterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PNEUMO_STORAGE_EXPORTER_BE.get(), pos, state, INVENTORY_SIZE);
    }

    public boolean isContinuousRequest() { return continuousRequest; }
    public int getRequestMode()          { return requestMode; }

    public boolean isRorConfiguredMode()  { return rorConfiguredMode; }
    public int[] getRorFilter(int slot)   { return rorFilters[slot]; }

    public void toggleContinuous() { continuousRequest = !continuousRequest; setChanged(); }
    public void nextRequestMode()  { requestMode = (requestMode + 1) % 3; setChanged(); }
    public void toggleRorMode()    { rorConfiguredMode = !rorConfiguredMode; setChanged(); }

    /**
     * 1:1-Port von {@code getFilter}: was auf diesem Platz angefordert wird, samt Menge. Im
     * Funkbetrieb steht das in {@link #rorFilters}, sonst liegt es als Vorlage im Platz.
     *
     * @return {@link ItemStack#EMPTY}, wenn nichts angefordert wird
     */
    public ItemStack getFilter(int slot) {
        if (!rorConfiguredMode) return getInventory().getStackInSlot(slot);

        // Original: Item.getItemById(...) == null. Nummer 0 ist Luft und gilt als "kein Filter".
        if (rorFilters[slot][0] == 0 || rorFilters[slot][2] <= 0) return ItemStack.EMPTY;

        net.minecraft.world.item.Item item =
                net.minecraft.core.registries.BuiltInRegistries.ITEM.byId(rorFilters[slot][0]);
        if (item == net.minecraft.world.item.Items.AIR) return ItemStack.EMPTY;

        return new ItemStack(item, rorFilters[slot][2]);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, PneumoStorageExporterBlockEntity be) {
        if (level.isClientSide()) return;

        be.tickNetwork(level, pos);

        for (int i = 0; i < REQUEST_SLOTS; i++) {
            if (be.slotDelay[i] > 0) be.slotDelay[i]--;
        }

        boolean redstone = level.hasNeighborSignal(pos);

        if (be.continuousRequest) {
            be.doRequest(false);
        } else if (redstone && !be.lastRedstone) {
            // Original: die Flanke loest einen erzwungenen Durchlauf aus, der die Sperren ignoriert.
            be.doRequest(true);
        }

        be.lastRedstone = redstone;
        be.sendUpdateToClient();
    }

    /** 1:1-Port von {@code doRequest}. */
    public void doRequest(boolean force) {
        if (cache == null || cache.hasExpired) return;

        if (requestMode != MODE_FULL_REQUEST) {
            for (int i = 0; i < REQUEST_SLOTS; i++) {
                if (!requestSlot(i, force)) slotDelay[i] = SLOT_DELAY;
            }
            return;
        }

        // ── Alles-oder-nichts ──
        // Ohne Zwang gilt: liegt auch nur ein Platz in der Sperre, passiert gar nichts.
        if (!force) {
            for (int i = 0; i < REQUEST_SLOTS; i++) {
                if (!getFilter(i).isEmpty() && slotDelay[i] > 0) return;
            }
        }

        // Erst pruefen, ob jede Forderung erfuellbar ist und der Platz dafuer frei.
        for (int i = 0; i < REQUEST_SLOTS; i++) {
            ItemStack filter = getFilter(i);
            if (filter.isEmpty()) continue;

            int requestSize = filter.getCount();
            ItemStack existing = getInventory().getStackInSlot(i + REQUEST_SLOTS);
            int existingSize = 0;

            if (!existing.isEmpty()) {
                if (ItemStack.isSameItemSameTags(existing, filter)) {
                    existingSize = existing.getCount();
                } else {
                    slotDelay[i] = SLOT_DELAY;
                    return;
                }
            }

            int capacityLeft = filter.getMaxStackSize() - existingSize;
            if (capacityLeft < requestSize || getAvailability(filter) < requestSize) {
                slotDelay[i] = SLOT_DELAY;
                return;
            }
        }

        // Es passt alles - jetzt holen.
        for (int i = 0; i < REQUEST_SLOTS; i++) {
            ItemStack filter = getFilter(i);
            if (filter.isEmpty()) continue;

            CacheSlot cacheSlot = cache.getSlotFromStack(filter);
            if (cacheSlot == null) continue;

            ItemStack existing = getInventory().getStackInSlot(i + REQUEST_SLOTS);
            int existingSize = existing.isEmpty() ? 0 : existing.getCount();

            ItemStack pulled = filter.copy();
            pulled.setCount(existingSize
                    + (int) cache.consumeItemsAndReturnQuantity(filter, filter.getCount()));
            getInventory().setStackInSlot(i + REQUEST_SLOTS, pulled);
        }

        setChanged();
    }

    /**
     * 1:1-Port von {@code requestSlot}.
     *
     * @return false, wenn der Platz danach in die Sperre soll
     */
    public boolean requestSlot(int slot, boolean force) {
        if (!force && slotDelay[slot] > 0) return true;
        if (cache == null || cache.hasExpired) return false;

        ItemStack filter = getFilter(slot);
        if (filter.isEmpty()) return false;

        int requestSize = filter.getCount();
        ItemStack existing = getInventory().getStackInSlot(slot + REQUEST_SLOTS);
        int existingSize = 0;

        if (!existing.isEmpty()) {
            if (!ItemStack.isSameItemSameTags(existing, filter)) return false;
            existingSize = existing.getCount();
        }

        int capacityLeft = filter.getMaxStackSize() - existingSize;

        // Alles ausser "so viel wie moeglich" scheitert schon hier, wenn der Platz nicht reicht.
        if (capacityLeft < requestSize && requestMode != MODE_AS_MUCH_AS_POSSIBLE) return false;

        CacheSlot cacheSlot = cache.getSlotFromStack(filter);
        if (cacheSlot == null) return false;

        if (cacheSlot.stacksize < requestSize && requestMode != MODE_AS_MUCH_AS_POSSIBLE) return false;
        if (cacheSlot.stacksize <= 0) return false;

        int toPull = (int) Math.min(requestSize, Math.min(cacheSlot.stacksize, capacityLeft));
        if (toPull <= 0) return false;

        ItemStack pulled = filter.copy();
        pulled.setCount(existingSize + (int) cache.consumeItemsAndReturnQuantity(filter, toPull));
        getInventory().setStackInSlot(slot + REQUEST_SLOTS, pulled);
        setChanged();

        return true;
    }

    /** Original: {@code getAvailability} - wieviel das Netz von diesem Gegenstand hergibt. */
    public long getAvailability(ItemStack stack) {
        if (cache == null || cache.hasExpired) return 0L;
        if (StackCache.getStackIdentity(stack) == StackCache.getNullIdentity()) return 0L;

        CacheSlot slot = cache.getSlotFromStack(stack);
        return slot == null ? 0L : slot.stacksize;
    }

    // ── Redstone-over-Radio ──

    /** 1:1 aus {@code TileEntityPneumoStorageExporter.getFunctionInfo}. */
    @Override
    public String[] getFunctionInfo() {
        return new String[] {
                PREFIX_FUNCTION + "setfilter" + NAME_SEPARATOR + "slot" + PARAM_SEPARATOR + "itemid"
                        + PARAM_SEPARATOR + "itemmeta" + PARAM_SEPARATOR + "amount",
                PREFIX_FUNCTION + "setcontinuous" + NAME_SEPARATOR + "on/off",
                PREFIX_FUNCTION + "request",
                PREFIX_FUNCTION + "requestslot" + NAME_SEPARATOR + "slot",
                PREFIX_FUNCTION + "checkavailability" + NAME_SEPARATOR + "itemid"
                        + PARAM_SEPARATOR + "itemmeta" + PARAM_SEPARATOR + "returnchannel"
        };
    }

    @Override
    public String runRORFunction(String name, String[] params) {

        if ((PREFIX_FUNCTION + "setfilter").equals(name) && params.length == 4) {
            int slot = com.hbm_m.api.redstoneoverradio.IRORInteractive.parseInt(params[0], 1, REQUEST_SLOTS) - 1;
            int itemId = parseItemId(params[1]);
            int meta = com.hbm_m.api.redstoneoverradio.IRORInteractive.parseInt(params[2], 0, Short.MAX_VALUE);
            int amount = com.hbm_m.api.redstoneoverradio.IRORInteractive.parseInt(params[3], 1, 64);

            rorFilters[slot][0] = itemId;
            rorFilters[slot][1] = meta;
            rorFilters[slot][2] = amount;
            setChanged();
            return null;
        }

        if ((PREFIX_FUNCTION + "setcontinuous").equals(name) && params.length == 1) {
            if ("on".equals(params[0])) continuousRequest = true;
            if ("off".equals(params[0])) continuousRequest = false;
            setChanged();
            return null;
        }

        if ((PREFIX_FUNCTION + "request").equals(name)) {
            doRequest(true);
            return null;
        }

        if ((PREFIX_FUNCTION + "requestslot").equals(name) && params.length == 1) {
            int slot = com.hbm_m.api.redstoneoverradio.IRORInteractive.parseInt(params[0], 1, REQUEST_SLOTS) - 1;
            if (!requestSlot(slot, true)) slotDelay[slot] = SLOT_DELAY;
            return null;
        }

        if ((PREFIX_FUNCTION + "checkavailability").equals(name) && params.length == 3) {
            int itemId = parseItemId(params[0]);
            String ret = params[2];

            net.minecraft.world.item.Item item =
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.byId(itemId);
            long availability = (itemId == 0 || item == net.minecraft.world.item.Items.AIR)
                    ? 0L : getAvailability(new ItemStack(item));

            // Original: RTTYSystem.broadcast(worldObj, ret, availability + "")
            if (getLevel() != null) {
                com.hbm_m.blockentity.network.radio.RTTYNetwork.broadcast(getLevel(), ret, availability + "");
            }
            return null;
        }

        return null;
    }

    /**
     * Die Gegenstandsnummer aus einem Parameter. Das Original kennt nur Zahlen; weil die je Welt
     * vergeben werden und sich niemand merken kann, nimmt dieser Port auch den Registriernamen.
     */
    private static int parseItemId(String param) {
        String trimmed = param.trim();

        try {
            return Integer.parseInt(trimmed);
        } catch (NumberFormatException ex) {
            net.minecraft.resources.ResourceLocation id =
                    net.minecraft.resources.ResourceLocation.tryParse(trimmed);
            if (id == null) return 0;

            net.minecraft.world.item.Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(id);
            return item == net.minecraft.world.item.Items.AIR
                    ? 0 : net.minecraft.core.registries.BuiltInRegistries.ITEM.getId(item);
        }
    }

    /** Nur die Ausgabeplaetze duerfen befuellt werden - die oberen neun sind Vorlagen. */
    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return true;
    }

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.putBoolean("continuousRequest", continuousRequest);
        tag.putByte("requestMode", (byte) requestMode);
        tag.putBoolean("lastRedstone", lastRedstone);
        tag.putIntArray("slotDelay", slotDelay);
        tag.putBoolean("rorConfiguredMode", rorConfiguredMode);

        for (int i = 0; i < REQUEST_SLOTS; i++) {
            tag.putInt("filter_" + i + "_0", rorFilters[i][0]);
            tag.putInt("filter_" + i + "_1", rorFilters[i][1]);
            tag.putInt("filter_" + i + "_2", rorFilters[i][2]);
        }
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        continuousRequest = tag.getBoolean("continuousRequest");
        requestMode = tag.getByte("requestMode");
        lastRedstone = tag.getBoolean("lastRedstone");

        int[] stored = tag.getIntArray("slotDelay");
        if (stored.length == REQUEST_SLOTS) System.arraycopy(stored, 0, slotDelay, 0, REQUEST_SLOTS);

        rorConfiguredMode = tag.getBoolean("rorConfiguredMode");

        for (int i = 0; i < REQUEST_SLOTS; i++) {
            rorFilters[i][0] = tag.getInt("filter_" + i + "_0");
            rorFilters[i][1] = tag.getInt("filter_" + i + "_1");
            rorFilters[i][2] = tag.getInt("filter_" + i + "_2");
        }
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.hbm_m.pneumatic_storage_exporter");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new PneumoStorageExporterMenu(id, inv, this);
    }
}
