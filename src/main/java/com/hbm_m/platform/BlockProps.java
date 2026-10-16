package com.hbm_m.platform;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Кросс-версионная обёртка над {@code BlockBehaviour.Properties.copy} / {@code ofFullCopy}.
 *
 * <p><b>Проблема:</b> на 1.20.1 (forge/fabric) блок-свойства копируются через
 * {@code BlockBehaviour.Properties.copy(block)}, а на 1.21.1 (neoforge) этот метод
 * депрекейтнут и заменён на {@code ofFullCopy(block)}. Расставлять 800+ stonecutter-блоков
 * по ModBlocks.java нецелесообразно, поэтому весь call-site сводится к этому хелперу.
 *
 * <p><b>Версионный gating по loader'у:</b> текущий набор проектов — 1.20.1-forge и
 * 1.21.1-neoforge, поэтому loader-gating корректно отражает версионную разницу.
 * При активации 1.21.1-fabric потребуется version-gating ({@code //? if < 1.21.1}).
 */
public final class BlockProps {
    private BlockProps() {}

    /**
     * Эквивалент {@code BlockBehaviour.Properties.copy(block)} — кросс-версионный.
     *
     * @param block блок-донор свойств (например {@code Blocks.IRON_BLOCK})
     * @return свежий {@link BlockBehaviour.Properties}, скопированный с донора
     */
    public static BlockBehaviour.Properties copy(Block block) {
        //? if forge {
        return BlockBehaviour.Properties.copy(block);
        //?}
        //? if neoforge {
        /*return BlockBehaviour.Properties.ofFullCopy(block);
        *///?}
    }
}
