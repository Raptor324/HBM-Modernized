package com.hbm_m.block.generic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Блок кокса — порт BlockCoke 1.7.10 (BlockEnumMulti(EnumCokeType); в порте варианты
 *  вынесены в отдельные блоки). Горюч: flammability 5, fire spread 10, как в оригинале.
 *  Item-вариант блока — топливо (32000 тиков, FuelHandler 1.7.10). */
public class BlockCoke extends Block {

    public BlockCoke(Properties properties) {
        super(properties);
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<BlockCoke> CODEC = simpleCodec(BlockCoke::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.Block> codec() {
        return CODEC;
    }
    *///?}

    // IForgeBlock/IBlockExtension — одинаково на 1.20.1-forge и 1.21.1-neoforge.
    @Override
    public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, @Nullable Direction direction) {
        return 5;
    }

    @Override
    public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, @Nullable Direction direction) {
        return 10;
    }
}
