package com.hbm_m.world.gen.nbt;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** 1:1 {@code com.hbm.world.gen.nbt.JigsawPool}: gewichtete Teileliste mit optionalem Ersatzpool. */
public class JigsawPool {

    // Gewichtete Teile
    List<Entry> pieces = new ArrayList<>();
    int totalWeight = 0;

    public String fallback;

    private boolean isClone;

    public record Entry(JigsawPiece piece, int weight) { }

    public void add(JigsawPiece piece, int weight) {
        if (weight <= 0) throw new IllegalStateException("JigsawPool spawn weight must be positive!");
        pieces.add(new Entry(piece, weight));
        totalWeight += weight;
    }

    public int getAverageWeight() {
        if (pieces.size() == 0) return 1;
        return totalWeight / pieces.size();
    }

    protected JigsawPool copy() {
        JigsawPool clone = new JigsawPool();
        clone.pieces = new ArrayList<>(this.pieces);
        clone.fallback = this.fallback;
        clone.totalWeight = this.totalWeight;
        clone.isClone = true;

        return clone;
    }

    /** Aus einer Kopie gezogen, wird das Teil daraus entfernt. */
    public JigsawPiece get(Random rand) {
        if (totalWeight <= 0) return null;
        int weight = rand.nextInt(totalWeight);

        for (int i = 0; i < pieces.size(); i++) {
            Entry pair = pieces.get(i);
            weight -= pair.weight();

            if (weight < 0) {
                if (isClone) {
                    pieces.remove(i);
                    totalWeight -= pair.weight();
                }

                return pair.piece();
            }
        }

        return null;
    }

    public List<Entry> getPieces() {
        return pieces;
    }
}
