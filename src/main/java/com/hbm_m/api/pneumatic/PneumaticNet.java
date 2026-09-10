package com.hbm_m.api.pneumatic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.hbm_m.api.item.ItemHandlerAccess;
import com.hbm_m.api.network.GenNode;
import com.hbm_m.api.network.NodeNet;
import com.hbm_m.blockentity.network.pneumatic.PneumoTubeBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 1:1-Port von {@code PneumaticNetwork} (1.7.10): das Rohrnetz, durch das die Druckluftrohre
 * Gegenstaende schieben.
 *
 * <p>Das Netz selbst haelt keine Gegenstaende. Es fuehrt nur eine Liste von <b>Empfaengern</b> -
 * jeweils das Inventar hinter einem Auswurfrohr samt der Seite, an der das Rohr klebt. Diese
 * Eintraege verfallen nach {@link #TIMEOUT} Millisekunden von selbst, sodass abgebaute oder
 * entladene Ziele nicht liegenbleiben.</p>
 *
 * <p>Ein Sendevorgang laeuft so: das sendende Rohr reicht sein Quellinventar herein, das Netz
 * sortiert die Empfaenger (nach Naehe beim Rundlauf, sonst zufaellig) und probiert bis zu fuenf
 * davon durch. Je Vorgang wandern hoechstens {@link #ITEMS_PER_TRANSFER} Einheiten "Masse":
 * ein Gegenstand, der sich nur zu vier stapelt, zaehlt dabei sechzehnfach, ein unstapelbarer
 * voll - so kostet ein Stapel Erde genausoviel wie ein einzelnes Werkzeug.</p>
 *
 * <p>Dasselbe Netz traegt die zweite Haelfte des Systems, das <b>Lagernetz</b>: die
 * {@link #accessors Zugangspunkte} (Terminals und Ein-/Ausgabegeraete) und die {@link #storages
 * Lager}. Beide finden sich ueber dieses Netz - ein Lager meldet seine Plaetze bei jedem
 * Zugangspunkt an, der es in Reichweite hat.</p>
 *
 * <p><b>Abweichung.</b> 1.20 hat keine {@code IInventory}, darum stehen die Empfaenger als
 * Position und Seite in der Liste und der eigentliche Zugriff laeuft ueber die Gegenstands-
 * schnittstelle des Blocks.</p>
 */
public class PneumaticNet extends NodeNet<PneumaticNet.Receiver, Object, GenNode<PneumaticNet>> {

    public static final byte SEND_FIRST = 0;
    public static final byte SEND_LAST = 1;
    public static final byte SEND_RANDOM = 2;
    public static final byte RECEIVE_ROBIN = 0;
    public static final byte RECEIVE_RANDOM = 1;

    /** Original: {@code timeout = 1_000} Millisekunden. */
    protected static final int TIMEOUT = 1_000;
    /** Original: {@code ITEMS_PER_TRANSFER = 64} - eine Stapelmasse je Vorgang. */
    public static final int ITEMS_PER_TRANSFER = 64;
    /** Original: hoechstens fuenf Empfaenger je Vorgang durchprobieren. */
    private static final int MAX_ATTEMPTS = 5;

    /**
     * Ein Ziel des Netzes: die Position des Inventars, die Seite, an der das Auswurfrohr klebt,
     * und dieses Rohr selbst - dessen Filter gilt naemlich beim Empfangen mit.
     */
    public record Receiver(BlockPos pos, Direction pipeDir, PneumoTubeBlockEntity endpoint) {
        @Override
        public boolean equals(Object o) {
            return o instanceof Receiver other && pos.equals(other.pos);
        }

        @Override
        public int hashCode() {
            return pos.hashCode();
        }
    }

    /** Ziel zu Zeitstempel des letzten Lebenszeichens. */
    public final Map<Receiver, Long> pneumaticReceivers = new LinkedHashMap<>();

    /** Original: {@code accessors} - die Verzeichnisse aller Zugangspunkte in diesem Netz. */
    public final java.util.LinkedHashSet<StackCache> accessors = new java.util.LinkedHashSet<>();
    /** Original: {@code storages} - alle Lager in diesem Netz. */
    public final java.util.LinkedHashSet<ISlotMonitorProvider> storages = new java.util.LinkedHashSet<>();

    @Override
    public void destroy() {
        super.destroy();
        pneumaticReceivers.clear();
        for (StackCache cache : accessors) cache.dissolveCache();
        accessors.clear();
        storages.clear();
    }

    @Override
    public void joinNetworks(NodeNet<Receiver, Object, GenNode<PneumaticNet>> other) {
        super.joinNetworks(other);
        if (!(other instanceof PneumaticNet net)) return;

        pneumaticReceivers.putAll(net.pneumaticReceivers);
        accessors.addAll(net.accessors);

        // Original: jedes uebernommene Lager meldet seine Plaetze bei allen Zugangspunkten des
        // aufnehmenden Netzes neu an - sonst sieht ein Terminal die neu dazugekommenen nicht.
        for (ISlotMonitorProvider storage : net.storages) {
            storages.add(storage);
            for (StackCache cache : net.accessors) storage.onNewCacheHasJoined(cache);
        }
    }

    /** Original: {@code addStackCache} - ein neuer Zugangspunkt holt sich alle vorhandenen Lager. */
    public void addStackCache(StackCache accessor) {
        if (accessors.add(accessor)) {
            for (ISlotMonitorProvider storage : storages) storage.onNewCacheHasJoined(accessor);
        }
    }

    /** Original: {@code addReceiver} - das Auswurfrohr meldet sein Ziel alle zehn Ticks an. */
    public void addPneumaticReceiver(BlockPos pos, Direction pipeDir, PneumoTubeBlockEntity endpoint) {
        pneumaticReceivers.put(new Receiver(pos, pipeDir, endpoint), System.currentTimeMillis());
    }

    @Override
    public void update() {
        // Original: abgelaufene Ziele wegraeumen, damit sich kein Muell ansammelt.
        prune();
        accessors.removeIf(cache -> cache.hasExpired);
    }

    private void prune() {
        long now = System.currentTimeMillis();
        pneumaticReceivers.entrySet().removeIf(e ->
                now - e.getValue() > TIMEOUT || e.getKey().endpoint().isRemoved());
    }

    /**
     * 1:1-Port von {@code send}: schiebt einen Schwung Gegenstaende vom Quellinventar zu einem der
     * Empfaenger.
     *
     * @param level      die Welt, aus der die Inventare geholt werden
     * @param sourcePos  Position des Quellinventars
     * @param tube       das sendende Rohr - sein Filter entscheidet, was ueberhaupt losgeschickt wird
     * @param accessDir  die Seite des Quellinventars, an der das Rohr klebt
     * @param maxRange   Reichweite nach Druckstufe, siehe {@code getRangeFromPressure}
     * @param nextReceiver laufende Nummer fuer den Rundlauf
     * @return true, wenn wirklich etwas bewegt wurde - nur dann kostet es Druckluft
     */
    public boolean send(Level level, BlockPos sourcePos, PneumoTubeBlockEntity tube, Direction accessDir,
                        int sendOrder, int receiveOrder, int maxRange, int nextReceiver) {

        prune();
        if (pneumaticReceivers.isEmpty()) return false;

        var source = ItemHandlerAccess.getItemHandler(level, sourcePos, accessDir);
        if (source == null) return false;

        int[] sourceSlots = slotOrder(source.getSlots(), sendOrder);

        // Erst pruefen, ob ueberhaupt etwas Passendes dasteht - spart Rechenzeit bei Leerlauf.
        boolean hasItem = false;
        for (int i : sourceSlots) {
            ItemStack stack = source.getStackInSlot(i);
            if (stack.isEmpty()) continue;
            if (!tube.passesFilter(stack)) continue;
            if (source.extractItem(i, 1, true).isEmpty()) continue;
            hasItem = true;
            break;
        }
        if (!hasItem) return false;

        List<Receiver> list = new ArrayList<>(pneumaticReceivers.keySet());

        if (receiveOrder == RECEIVE_ROBIN) {
            // Rundlauf: nach Naehe zum sendenden Rohr, bei Gleichstand ueber die Streuzahl.
            BlockPos origin = tube.getBlockPos();
            list.sort(Comparator
                    .comparingInt((Receiver r) -> (int) r.pos().distSqr(origin))
                    .thenComparingInt(r -> identifier(r.pos())));
        } else {
            Collections.shuffle(list, RAND);
        }

        int attempts = 0;
        int maxAttempts = Math.min(list.size(), MAX_ATTEMPTS);

        while (attempts < maxAttempts) {
            int index = receiveOrder == RECEIVE_ROBIN
                    ? Math.floorMod(nextReceiver + attempts, list.size())
                    : attempts;

            Receiver candidate = list.get(index);
            attempts++;

            // Reichweite der eingestellten Druckstufe
            if (sourcePos.distSqr(candidate.pos()) > (long) maxRange * maxRange) continue;

            var dest = ItemHandlerAccess.getItemHandler(level, candidate.pos(), candidate.pipeDir().getOpposite());
            if (dest == null) {
                pneumaticReceivers.remove(candidate);
                continue;
            }

            // ── Umschlag ──
            // Der eigentliche Zugriff steht hier ausgeschrieben statt in einer Hilfsmethode: der
            // Typ der Gegenstandsschnittstelle heisst auf Forge und NeoForge verschieden, und mit
            // "var" bleibt der Code fuer beide gueltig, ohne ihn doppelt zu fuehren.
            int massLeft = ITEMS_PER_TRANSFER;
            boolean didSomething = false;
            PneumoTubeBlockEntity endpoint = candidate.endpoint();

            for (int sourceIndex : sourceSlots) {
                ItemStack sourceStack = source.getStackInSlot(sourceIndex);
                if (sourceStack.isEmpty()) continue;

                // Filter des Senders und - wenn es ein anderes Rohr ist - auch des Empfaengers.
                if (!tube.passesFilter(sourceStack)) continue;
                if (endpoint != null && endpoint != tube && !endpoint.passesFilter(sourceStack)) continue;

                // Die "Masse" eines Gegenstands: 64 geteilt durch seine Stapelgroesse.
                int proportional = Math.max(1, Math.min(64, 64 / Math.max(1, sourceStack.getMaxStackSize())));
                int maxCount = massLeft / proportional;
                if (maxCount <= 0) break;

                ItemStack simulated = source.extractItem(sourceIndex, maxCount, true);
                if (simulated.isEmpty()) continue;

                ItemStack rest = simulated.copy();
                for (int i = 0; i < dest.getSlots() && !rest.isEmpty(); i++) {
                    rest = dest.insertItem(i, rest, false);
                }

                int moved = simulated.getCount() - rest.getCount();
                if (moved <= 0) continue;

                source.extractItem(sourceIndex, moved, false);
                massLeft -= moved * proportional;
                didSomething = true;

                if (massLeft <= 0) break;
            }

            if (didSomething) return true;
        }

        return false;
    }

    /** Original: {@code getSlotAccess} plus die Umsortierung nach der Sendereihenfolge. */
    private int[] slotOrder(int size, int sendOrder) {
        int[] order = new int[size];
        for (int i = 0; i < size; i++) order[i] = i;

        if (sendOrder == SEND_LAST) {
            for (int i = 0; i < size / 2; i++) {
                int tmp = order[i];
                order[i] = order[size - 1 - i];
                order[size - 1 - i] = tmp;
            }
        } else if (sendOrder == SEND_RANDOM) {
            for (int i = size - 1; i > 0; i--) {
                int j = RAND.nextInt(i + 1);
                int tmp = order[i];
                order[i] = order[j];
                order[j] = tmp;
            }
        }
        return order;
    }

    /** Original: {@code TileEntityPneumoTube.getIdentifier} - die Streuzahl von {@code BlockPos}. */
    public static int identifier(BlockPos pos) {
        return (pos.getY() + pos.getZ() * 27644437) * 27644437 + pos.getX();
    }
}
