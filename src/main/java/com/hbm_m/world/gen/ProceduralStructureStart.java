package com.hbm_m.world.gen;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import com.hbm_m.world.gen.component.Component;

import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;

/**
 * 1:1 {@code com.hbm.world.gen.ProceduralStructureStart}: baut Bauteil-Strukturen aus gewichteten Bauteilen
 * ({@link Weight}) auf, ausgehend von einem Startbauteil, mit Groessen- und Entfernungsgrenzen. Port: statt
 * {@code StructureStart} eine Liste von Bauteilen fuer {@link com.hbm_m.world.gen.nbt.NBTStructureGen}.
 */
public class ProceduralStructureStart {

    /** Alle Bauteile der Struktur (Original {@code components}). */
    public final List<StructurePiece> components = new ArrayList<>();
    /** Warteschlange der Bauteile, deren {@code buildComponent} noch zufaellig aufgerufen wird. */
    public List<StructurePiece> queuedComponents = new ArrayList<>();
    /** Gewichte dieser Struktur - verbrauchte Gewichte fallen heraus. */
    protected List<Weight> componentWeightList;

    protected final int chunkX;
    protected final int chunkZ;

    public ProceduralStructureStart(int chunkX, int chunkZ) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
    }

    /** 'starter' ist das Startbauteil (wie der Dorfbrunnen). */
    public ProceduralStructureStart buildStart(Random rand, StructurePiece starter, Weight... weights) {
        prepareWeights(weights);

        components.add(starter);
        queuedComponents.add(starter);
        while (!queuedComponents.isEmpty()) {
            final int i = rand.nextInt(queuedComponents.size());
            StructurePiece component = queuedComponents.remove(i);
            if (component instanceof ProceduralComponent proc)
                proc.buildComponent(this, rand); // weitere Bauteile landen in der Liste
        }

        return this;
    }

    public void prepareWeights(Weight... weights) {
        componentWeightList = new ArrayList<>(weights.length);

        for (int i = 0; i < weights.length; i++) {
            weights[i].instancesSpawned = 0;
            componentWeightList.add(weights[i]);
        }
    }

    protected int getTotalWeight() {
        boolean flag = false;
        int totalWeight = 0;
        Weight weight;

        for (Iterator<Weight> iterator = componentWeightList.iterator(); iterator.hasNext(); totalWeight += weight.weight) { // ganze Liste fuer das Gesamtgewicht
            weight = iterator.next();

            if (weight.instanceLimit >= 0 && weight.instancesSpawned < weight.instanceLimit) // koennen ueberhaupt noch Teile dazu?
                flag = true;
        }

        return flag ? totalWeight : -1;
    }

    protected StructurePiece getWeightedComponent(StructurePiece last, Random rand, int minX, int minY, int minZ, int coordMode, int componentType) {
        int totalWeight = getTotalWeight();

        if (totalWeight < 0)
            return null;

        for (int i = 0; i < 5; i++) {
            int value = rand.nextInt(totalWeight); // zufaelliger Wert nach Anzahl der Teile
            Iterator<Weight> iterator = componentWeightList.iterator();

            while (iterator.hasNext()) {
                Weight weight = iterator.next();
                value -= weight.weight; // bis der Wert unter 0 faellt

                if (value < 0) {
                    if (!weight.canSpawnStructure(componentType, coordMode, last)) // zusaetzliche Zustandspruefung? von vorn
                        break;

                    StructurePiece component = weight.lambda.findValidPlacement(components, rand, minX, minY, minZ, coordMode, componentType); // gewaehltes Bauteil bauen

                    if (component != null) { // gebaut: hinzufuegen
                        weight.instancesSpawned++;

                        if (!weight.canSpawnMoreStructures()) // kann nie mehr erscheinen? aus der Auswahl nehmen
                            componentWeightList.remove(weight);

                        return component;
                    }

                }
            }
        }

        return null;
    }

    protected int sizeLimit = 50;
    protected int distanceLimit = 64;

    /** Naechstes gueltiges Bauteil nach den Grenzen dieses Starts. */
    protected StructurePiece getNextValidComponent(StructurePiece last, Random rand, int minX, int minY, int minZ, int coordMode, int componentType) {

        if (components.size() > sizeLimit) // harte Grenze fuer die Teilezahl
            return null;

        if (Math.abs(minX - (chunkX * 16 + 8)) <= distanceLimit && Math.abs(minZ - (chunkZ * 16 + 8)) <= distanceLimit) { // harte Grenze fuer die Ausdehnung

            StructurePiece structure = getWeightedComponent(last, rand, minX, minY, minZ, coordMode, componentType + 1); // null, wenn alles scheitert

            if (structure != null) {
                this.components.add(structure);
                this.queuedComponents.add(structure);
            }

            return structure;
        }

        return null;
    }

    /** Begrenzungsrahmen aller Bauteile (Original {@code StructureStart.boundingBox}). */
    public BoundingBox getBoundingBox() {
        BoundingBox box = null;
        for (StructurePiece piece : components) {
            box = box == null ? piece.getBoundingBox() : BoundingBox.encapsulatingBoxes(List.of(box, piece.getBoundingBox())).orElse(box);
        }
        return box;
    }

    /** 1.7.10 {@code StructureStart.markAvailableHeight}: Struktur unter Meereshoehe absenken. */
    public void markAvailableHeight(Random rand, int offset) {
        BoundingBox box = getBoundingBox();
        int i = 63 - offset;
        int j = box.getYSpan() + 1;

        if (j < i) {
            j += rand.nextInt(i - j);
        }

        int k = j - box.maxY();

        for (StructurePiece component : components) {
            component.move(0, k, 0);
        }
    }

    /**
     * Setzt anhand von Anker, Versatz und Abmessung den Rahmen des naechsten Bauteils. Versaetze verschieben den
     * Anker immer Richtung +x, +y, +z (bezogen auf Sued); die Abmessungen sind die echten Groessen.
     */
    public static BoundingBox getComponentToAddBoundingBox(int posX, int posY, int posZ, int offsetX, int offsetY, int offsetZ, int maxX, int maxY, int maxZ, int coordMode) {
        switch (coordMode) {
            default:
            case 0: return new BoundingBox(posX + offsetX, posY + offsetY, posZ + offsetZ, posX + maxX - 1 + offsetX, posY + maxY - 1 + offsetY, posZ + maxZ - 1 + offsetZ); // Sued
            case 1: return new BoundingBox(posX - maxZ + 1 - offsetZ, posY + offsetY, posZ + offsetX, posX - offsetZ, posY + maxY - 1 + offsetY, posZ + maxX - 1 + offsetX); // West
            case 2: return new BoundingBox(posX - maxX + 1 - offsetX, posY + offsetY, posZ - maxZ + 1 - offsetZ, posX - offsetX, posY + maxY - 1 + offsetY, posZ + offsetZ); // Nord
            case 3: return new BoundingBox(posX + offsetZ, posY + offsetY, posZ - maxX + 1 - offsetX, posX + maxZ - 1 + offsetZ, posY + maxY - 1 + offsetY, posZ - offsetX); // Ost
        }
    }

    /** Bauteil mit Folgebauteilen. */
    public interface ProceduralComponent {

        default void buildComponent(ProceduralStructureStart start, Random rand) { }

        private static int type(StructurePiece caller) {
            return caller instanceof Component c ? c.getComponentType() : caller.getGenDepth();
        }

        /** Naechstes Bauteil in Blickrichtung; +1 auf den minZ-Anker (bezogen auf Sued). */
        default StructurePiece getNextComponentNormal(ProceduralStructureStart start, StructurePiece caller, int coordMode, Random rand, int offset, int offsetY) {
            BoundingBox box = caller.getBoundingBox();
            switch (coordMode) {
                case 0: return start.getNextValidComponent(caller, rand, box.minX() + offset, box.minY() + offsetY, box.maxZ() + 1, coordMode, type(caller)); // Sued
                case 1: return start.getNextValidComponent(caller, rand, box.minX() - 1, box.minY() + offsetY, box.minZ() + offset, coordMode, type(caller)); // West
                case 2: return start.getNextValidComponent(caller, rand, box.maxX() - offset, box.minY() + offsetY, box.minZ() - 1, coordMode, type(caller)); // Nord
                case 3: return start.getNextValidComponent(caller, rand, box.maxX() + 1, box.minY() + offsetY, box.maxZ() - offset, coordMode, type(caller)); // Ost
                default: return null;
            }
        }

        /** Naechstes Bauteil entgegen der Blickrichtung. */
        default StructurePiece getNextComponentAntiNormal(ProceduralStructureStart start, StructurePiece caller, int coordMode, Random rand, int offset, int offsetY) {
            BoundingBox box = caller.getBoundingBox();
            switch (coordMode) {
                case 0: return start.getNextValidComponent(caller, rand, box.maxX() - offset, box.minY() + offsetY, box.minZ() - 1, 2, type(caller)); // Sued
                case 1: return start.getNextValidComponent(caller, rand, box.maxX() + 1, box.minY() + offsetY, box.maxZ() - offset, 3, type(caller)); // West
                case 2: return start.getNextValidComponent(caller, rand, box.minX() + offset, box.minY() + offsetY, box.maxZ() + 1, 0, type(caller)); // Nord
                case 3: return start.getNextValidComponent(caller, rand, box.minX() - 1, box.minY() + offsetY, box.minZ() + offset, 1, type(caller)); // Ost
                default: return null;
            }
        }

        /** Naechstes Bauteil nach Westen (-X) relativ zu diesem Bauteil. */
        default StructurePiece getNextComponentWest(ProceduralStructureStart start, StructurePiece caller, int coordMode, Random rand, int offset, int offsetY) {
            BoundingBox box = caller.getBoundingBox();
            switch (coordMode) {
                case 0: return start.getNextValidComponent(caller, rand, box.minX() - 1, box.minY() + offsetY, box.minZ() + offset, 1, type(caller)); // Sued
                case 1: return start.getNextValidComponent(caller, rand, box.maxX() - offset, box.minY() + offsetY, box.minZ() - 1, 2, type(caller)); // West
                case 2: return start.getNextValidComponent(caller, rand, box.maxX() + 1, box.minY() + offsetY, box.maxZ() - offset, 3, type(caller)); // Nord
                case 3: return start.getNextValidComponent(caller, rand, box.minX() + offset, box.minY() + offsetY, box.maxZ() + 1, 0, type(caller)); // Ost
                default: return null;
            }
        }

        /** Naechstes Bauteil nach Osten (+X) relativ zu diesem Bauteil. */
        default StructurePiece getNextComponentEast(ProceduralStructureStart start, StructurePiece caller, int coordMode, Random rand, int offset, int offsetY) {
            BoundingBox box = caller.getBoundingBox();
            switch (coordMode) {
                case 0: return start.getNextValidComponent(caller, rand, box.maxX() + 1, box.minY() + offsetY, box.maxZ() - offset, 3, type(caller)); // Sued
                case 1: return start.getNextValidComponent(caller, rand, box.minX() + offset, box.minY() + offsetY, box.maxZ() + 1, 0, type(caller)); // West
                case 2: return start.getNextValidComponent(caller, rand, box.minX() - 1, box.minY() + offsetY, box.minZ() + offset, 1, type(caller)); // Nord
                case 3: return start.getNextValidComponent(caller, rand, box.maxX() - offset, box.minY() + offsetY, box.minZ() - 1, 2, type(caller)); // Ost
                default: return null;
            }
        }
    }

    /** Liefert eine neue Instanz des Bauteils oder null, wenn es nicht passt (Rahmenpruefung). */
    @FunctionalInterface
    public interface InstantiateStructure {
        StructurePiece findValidPlacement(List<StructurePiece> components, Random rand, int minX, int minY, int minZ, int coordMode, int componentType);
    }

    public static class Weight {

        public final InstantiateStructure lambda; // siehe oben

        public final int weight; // Gewicht dieses Bauteils
        public int instancesSpawned; // wie viele schon erschienen sind
        public int instanceLimit; // Hoechstzahl: -1 = unbegrenzt

        public Weight(int weight, int limit, InstantiateStructure lambda) {
            this.weight = weight;
            this.instanceLimit = limit;
            this.lambda = lambda;
        }

        // kann nach den Eingangsdaten noch ein Teil erscheinen?
        public boolean canSpawnStructure(int componentAmount, int coordMode, StructurePiece component) {
            return this.instanceLimit < 0 || this.instancesSpawned < this.instanceLimit;
        }

        // kann ueberhaupt noch ein Teil erscheinen? (sonst aus der Liste nehmen)
        public boolean canSpawnMoreStructures() {
            return this.instanceLimit < 0 || this.instancesSpawned < this.instanceLimit;
        }
    }
}
