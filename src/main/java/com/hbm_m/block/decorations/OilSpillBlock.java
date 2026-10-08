package com.hbm_m.block.decorations;

import com.hbm_m.block.generic.BlockLayering;

/**
 * Порт {@code BlockLayering} (1.7.10) для oil_spill: тонкий слой (ковёр),
 * высота как у снега, замещаемый при установке, стекается до 8 слоёв.
 * В отличие от снега не тает и не требует холода.
 * audit10: erbt jetzt die 1:1-Regeln von {@link BlockLayering} (canPlaceBlockAt/isReplaceable, kein Schmelzen).
 */
public class OilSpillBlock extends BlockLayering {

    public OilSpillBlock(Properties props) {
        super(props);
    }
}
