package com.hbm_m.block.machines.icf;

import net.minecraft.world.level.block.Block;

/**
 * Ein Bauteil des ICF-Laseraufbaus. Von diesem Block gibt es sechs Ausfuehrungen, siehe
 * {@link ICFLaserPart}; welche es ist, steht fest im Block und nicht im Zustand, weil sie sich
 * getrennt herstellen lassen sollen.
 */
public class ICFLaserComponentBlock extends Block {

    private final ICFLaserPart part;

    public ICFLaserComponentBlock(Properties properties, ICFLaserPart part) {
        super(properties);
        this.part = part;
    }

    public ICFLaserPart getPart() {
        return part;
    }
}
