package com.hbm_m.block.generic;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Порт {@link com.hbm.blocks.generic.BlockDragonProof} — блок, который не может быть
 * уничтожен Эндер-драконом.
 *
 * <p>Хук {@code canEntityDestroy(BlockState, BlockGetter, BlockPos, Entity)} присутствует
 * на обеих цельных платформах: Forge 1.20.1 ({@code IForgeBlock}) и NeoForge 1.21.1
 * ({@code IBlockExtension}, default-метод с DRAGON_IMMUNE-логикой) — поэтому
 * stonecutter-ветвление не требуется.</p>
 */
public class BlockDragonProof extends Block {

    public BlockDragonProof(Properties properties) {
        super(properties);
    }

    @Override
    public boolean canEntityDestroy(BlockState state, BlockGetter level, BlockPos pos, Entity entity) {
        return !(entity instanceof EnderDragon);
    }
}
