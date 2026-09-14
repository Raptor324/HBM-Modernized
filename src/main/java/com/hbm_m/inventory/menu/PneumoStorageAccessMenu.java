package com.hbm_m.inventory.menu;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import com.hbm_m.api.pneumatic.StackCache;
import com.hbm_m.api.pneumatic.StackCache.CacheSlot;
import com.hbm_m.blockentity.network.pneumatic.PneumoStorageAccessBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 1:1-Port von {@code ContainerPneumoStorageAccess} (1.7.10): das Zugangsterminal.
 *
 * <p>Die {@value #GRID_COLS} mal {@value #GRID_ROWS} Felder ab (42, 17) sind keine echten
 * Lagerplaetze, sondern ein <b>Schaufenster</b> auf das Verzeichnis des Terminals. Jedes Feld zeigt
 * einen Gegenstand und daneben die Gesamtmenge, die im ganzen Netz davon liegt - auch wenn sie ueber
 * ein Dutzend Lager verteilt ist und weit ueber eine Stapelgroesse hinausgeht.</p>
 *
 * <p><b>Bedienung.</b> Linksklick nimmt einen vollen Stapel heraus, Rechtsklick einen halben,
 * Umschalt-Klick schiebt so viel wie moeglich ins Spielerinventar. Ein Klick mit etwas in der Hand
 * legt es ins Netz. Gescrollt wird zeilenweise ueber {@link #setListingStart(int)}.</p>
 *
 * <p><b>Sortierung.</b> Vier Weisen wie im Original: nach Menge, nach Gegenstandsnummer, nach
 * angezeigtem Namen und nach internem Namen. Die letzte ist die nuetzlichste, wenn man ein Lager
 * nach Material durchsuchen will - dort stehen alle Barren eines Metalls beieinander, egal wie sie
 * uebersetzt heissen.</p>
 *
 * <p>Die <b>ausfuehrliche Suche</b> durchsucht zusaetzlich die Kurzinfos der Gegenstaende, nicht
 * nur ihren Namen.</p>
 *
 * <p><b>Anmerkung:</b> das Original schickt nur die Aenderungen an den Bildschirm; dieser Port
 * laesst die Standardsynchronisierung von 1.20 die Felder uebertragen. Die Mengen laufen dabei
 * ueber {@code ContainerData} und sind damit auf gut zwei Milliarden je Feld begrenzt - in der
 * Praxis reicht das.</p>
 */
public class PneumoStorageAccessMenu extends AbstractContainerMenu {

    public static final int GRID_COLS = 8;
    public static final int GRID_ROWS = 6;
    public static final int GRID_SIZE = GRID_COLS * GRID_ROWS;

    /** Original: die Oberflaeche ist um 34 Bildpunkte breiter, das Gitter beginnt entsprechend. */
    private static final int H_OFFSET = 34;

    private final PneumoStorageAccessBlockEntity blockEntity;
    /** Die Anzeigestapel des Schaufensters - server-seitig jeden Tick neu gefuellt. */
    private final SimpleContainer display = new SimpleContainer(GRID_SIZE);
    /** Die Mengen dazu, plus die Gesamtzahl der Eintraege als letzter Wert. */
    private final ContainerData amounts = new SimpleContainerData(GRID_SIZE + 1);

    // Original: die vier Sortierweisen aus {@code ContainerPneumoStorageAccess}.
    public static final int SORT_BY_STACK_SIZE = 0;
    public static final int SORT_BY_ID = 1;
    public static final int SORT_BY_LOCALIZED = 2;
    public static final int SORT_BY_INTERNAL = 3;

    private int listingStart = 0;
    private String searchString = "";
    private int sorting = SORT_BY_STACK_SIZE;
    /** Original: {@code detailedSearch} - die Suche greift dann auch in die Kurzinfos. */
    private boolean detailedSearch = false;

    public PneumoStorageAccessMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
        this(id, inv, getBlockEntity(inv, extraData));
    }

    public PneumoStorageAccessMenu(int id, Inventory inv, PneumoStorageAccessBlockEntity blockEntity) {
        super(ModMenuTypes.PNEUMO_STORAGE_ACCESS_MENU.get(), id);
        this.blockEntity = blockEntity;

        addDataSlots(amounts);

        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLS; col++) {
                this.addSlot(new Slot(display, col + row * GRID_COLS,
                        8 + col * 18 + H_OFFSET, 17 + row * 18) {
                    /** Das Schaufenster nimmt nichts an - eingelegt wird ueber den Klick. */
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return false;
                    }
                });
            }
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18 + H_OFFSET, 169 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inv, col, 8 + col * 18 + H_OFFSET, 227));
        }
    }

    private static PneumoStorageAccessBlockEntity getBlockEntity(Inventory inv, FriendlyByteBuf data) {
        BlockPos pos = data.readBlockPos();
        BlockEntity be = inv.player.level().getBlockEntity(pos);
        if (be instanceof PneumoStorageAccessBlockEntity access) return access;
        throw new IllegalStateException("No PneumoStorageAccessBlockEntity at " + pos);
    }

    public PneumoStorageAccessBlockEntity getBlockEntity() { return blockEntity; }

    public long getAmount(int gridIndex) {
        return gridIndex >= 0 && gridIndex < GRID_SIZE ? Integer.toUnsignedLong(amounts.get(gridIndex)) : 0L;
    }

    /** Wieviele Eintraege das Verzeichnis insgesamt hat - der Bildschirm braucht das zum Scrollen. */
    public int getListingSize() { return amounts.get(GRID_SIZE); }
    public int getListingStart() { return listingStart; }

    public void setListingStart(int start) {
        this.listingStart = Math.max(0, start);
    }

    public String getSearchString()  { return searchString; }
    public int getSorting()          { return sorting; }
    public boolean isDetailedSearch() { return detailedSearch; }

    public void setSearchString(String search) {
        this.searchString = search.toLowerCase(Locale.ROOT);
        this.listingStart = 0;
    }

    /** Original: {@code setSorter} - eine neue Sortierweise setzt die Anzeige an den Anfang. */
    public void setSorting(int sorting) {
        this.sorting = Math.max(0, Math.min(SORT_BY_INTERNAL, sorting));
        this.listingStart = 0;
    }

    public void setDetailedSearch(boolean detailed) {
        this.detailedSearch = detailed;
        this.listingStart = 0;
    }

    // ── Aktualisierung ──────────────────────────────────────────────────────

    /** Original: {@code rebuildClientIndex} - das Schaufenster aus dem Verzeichnis neu fuellen. */
    private void rebuild() {
        StackCache cache = blockEntity.getCache();

        List<CacheSlot> entries = new ArrayList<>();
        if (cache != null && !cache.hasExpired) {
            for (CacheSlot slot : cache.cacheSlots.values()) {
                if (slot.displayStack == null || slot.displayStack.isEmpty()) continue;
                if (slot.stacksize <= 0) continue;
                if (!matchesSearch(slot.displayStack)) continue;
                entries.add(slot);
            }
        }

        entries.sort(comparator());

        amounts.set(GRID_SIZE, entries.size());

        int offset = listingStart * GRID_COLS;
        for (int i = 0; i < GRID_SIZE; i++) {
            int index = offset + i;

            if (index < entries.size()) {
                CacheSlot slot = entries.get(index);
                display.setItem(i, slot.displayStack.copy());
                amounts.set(i, (int) Math.min(slot.stacksize, Integer.MAX_VALUE));
            } else {
                display.setItem(i, ItemStack.EMPTY);
                amounts.set(i, 0);
            }
        }
    }

    /**
     * 1:1-Port der Filterzeile: ohne die ausfuehrliche Suche zaehlt nur der angezeigte Name, mit
     * ihr zusaetzlich jede Zeile der Kurzinfo.
     */
    private boolean matchesSearch(ItemStack stack) {
        if (searchString.isEmpty()) return true;

        if (stack.getHoverName().getString().toLowerCase(Locale.ROOT).contains(searchString)) return true;
        if (!detailedSearch) return false;

        List<net.minecraft.network.chat.Component> tooltip = new ArrayList<>();
        try {
            stack.getItem().appendHoverText(stack, null, tooltip,
                    net.minecraft.world.item.TooltipFlag.Default.NORMAL);
        } catch (RuntimeException ignored) {
            // Manche Kurzinfos setzen eine Welt oder einen Spieler voraus - dann bleibt es beim Namen.
            return false;
        }

        for (net.minecraft.network.chat.Component line : tooltip) {
            if (line.getString().toLowerCase(Locale.ROOT).contains(searchString)) return true;
        }
        return false;
    }

    /** 1:1-Port der vier Vergleicher aus {@code ContainerPneumoStorageAccess}. */
    private Comparator<CacheSlot> comparator() {
        // Original: SORT_BY_ID vergleicht Nummer, dann Menge - und dient allen anderen als Nachrang.
        Comparator<CacheSlot> byId = Comparator
                .comparingInt((CacheSlot slot) ->
                        net.minecraft.core.registries.BuiltInRegistries.ITEM.getId(slot.displayStack.getItem()))
                .thenComparing(Comparator.comparingLong((CacheSlot slot) -> slot.stacksize).reversed());

        return switch (sorting) {
            case SORT_BY_ID -> byId;
            case SORT_BY_LOCALIZED -> Comparator
                    .comparing((CacheSlot slot) -> slot.displayStack.getHoverName().getString(),
                            String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(byId);
            case SORT_BY_INTERNAL -> Comparator
                    .comparing((CacheSlot slot) -> slot.displayStack.getItem().getDescriptionId(),
                            String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(byId);
            // Original: nach Menge absteigend, bei Gleichstand nach Nummer.
            default -> Comparator
                    .comparingLong((CacheSlot slot) -> slot.stacksize).reversed()
                    .thenComparing(byId);
        };
    }

    @Override
    public void broadcastChanges() {
        if (blockEntity.getLevel() != null && !blockEntity.getLevel().isClientSide()) rebuild();
        super.broadcastChanges();
    }

    // ── Bedienung ───────────────────────────────────────────────────────────

    /**
     * 1:1-Port von {@code slotClick} fuer das Schaufenster: entnehmen, halb entnehmen, in das
     * Inventar schieben oder einlegen.
     */
    @Override
    public void clicked(int index, int button, ClickType clickType, Player player) {
        if (index < 0 || index >= GRID_SIZE) {
            super.clicked(index, button, clickType, player);
            return;
        }
        if (player.level().isClientSide()) return;

        StackCache cache = blockEntity.getCache();
        if (cache == null || cache.hasExpired) return;

        ItemStack held = getCarried();

        // Etwas in der Hand: es geht ins Netz.
        if (!held.isEmpty()) {
            long left = cache.addItemsAndReturnQuantity(held, held.getCount());
            held.setCount((int) left);
            setCarried(held.isEmpty() ? ItemStack.EMPTY : held);
            return;
        }

        ItemStack template = display.getItem(index);
        if (template.isEmpty()) return;

        long available = getAmount(index);
        if (available <= 0) return;

        int max = template.getMaxStackSize();
        long wanted = switch (clickType) {
            // Umschalt-Klick: so viel, wie ins Inventar passt - hoechstens ein Stapel je Griff.
            case QUICK_MOVE -> Math.min(available, max);
            // Rechtsklick: ein halber Stapel.
            case PICKUP -> button == 1 ? Math.min(available, Math.max(1, max / 2)) : Math.min(available, max);
            default -> Math.min(available, max);
        };

        long taken = cache.consumeItemsAndReturnQuantity(template, wanted);
        if (taken <= 0) return;

        ItemStack pulled = template.copy();
        pulled.setCount((int) taken);

        if (clickType == ClickType.QUICK_MOVE) {
            // Ins Spielerinventar schieben; was nicht passt, geht zurueck ins Netz.
            if (!moveItemStackTo(pulled, GRID_SIZE, slots.size(), true) || !pulled.isEmpty()) {
                cache.addItemsAndReturnQuantity(pulled, pulled.getCount());
            }
        } else {
            setCarried(pulled);
        }

        rebuild();
    }

    /** Umschalt-Klick im Spielerinventar legt den Stapel ins Netz. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < GRID_SIZE) return ItemStack.EMPTY;

        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;

        StackCache cache = blockEntity.getCache();
        if (cache == null || cache.hasExpired) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        long left = cache.addItemsAndReturnQuantity(stack, stack.getCount());

        if (left == stack.getCount()) return ItemStack.EMPTY;

        stack.setCount((int) left);
        slot.setChanged();
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        if (blockEntity == null || blockEntity.getLevel() != player.level()) return false;
        BlockPos pos = blockEntity.getBlockPos();
        return player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D;
    }
}
