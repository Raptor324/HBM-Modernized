package com.hbm_m.platform.legacy;

import java.util.Set;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * [26.x] Ersatz für {@code BlockEntityType.Builder} (entfernt in 1.21.2). Hält die Aufrufkette
 * {@code of(factory, blocks...).build(null)} aus ModBlockEntities unverändert; Stonecutter
 * leitet {@code BlockEntityType.Builder.of(} hierher um (Regel {@code mc26_be_builder}).
 */
public final class BeTypes {

    private BeTypes() {}

    public static <T extends BlockEntity> Builder<T> of(BlockEntityType.BlockEntitySupplier<? extends T> factory, Block... blocks) {
        return new Builder<>(factory, blocks);
    }

    public static final class Builder<T extends BlockEntity> {
        private final BlockEntityType.BlockEntitySupplier<? extends T> factory;
        private final Block[] blocks;

        private Builder(BlockEntityType.BlockEntitySupplier<? extends T> factory, Block[] blocks) {
            this.factory = factory;
            this.blocks = blocks;
        }

        /** Das Argument (früher der DataFixer-Typ) wird ignoriert. */
        public BlockEntityType<T> build(Object ignoredDataType) {
            return new BlockEntityType<>(factory, Set.of(blocks));
        }
    }
}
