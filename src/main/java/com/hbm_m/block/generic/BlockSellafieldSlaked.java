package com.hbm_m.block.generic;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * Порт {@code BlockSellafieldSlaked} 1.7.10 — инертный sellafite.
 *
 * <p>В 1.7.10 {@code sellafield_slaked} (и {@code sellafield_bedrock}) — это plain {@code Block},
 * <b>без</b> {@code rad}-поля, {@code updateTick}, эмиттинга {@code incrementRad} и self-decay.
 * Метадата (0–9) используется только для {@code colorMultiplier} (косметический tinting).
 * {@code EntityFalloutRain} размещает именно этот инертный блок — кратер от взрыва
 * <b>не</b> накачивает chunk radiation.</p>
 *
 * <p>Горячий {@code BlockSellafield extends BlockHazard} ({@code rad = 0.5}, meta 0–5,
 * эмиттит через random tick с self-decay) в 1.7.10 — это отдельный creative-only блок,
 * который fallout'ом <b>не</b> размещается и в Modernized не портирован.</p>
 *
 * <p>В Modernized {@code COLOR_LEVEL} (0–10) остаётся косметическим свойством для тинтинга —
 * визуально эпицентр кратера (L9) темнее периферии (L0), точно как meta 0–9 в
 * {@code BlockSellafieldSlaked.colorMultiplier = 1 - meta/15F} оригинала.</p>
 *
 * <p>VARIANT — визуальная вариация текстуры (0–3), вычисляется из позиции.</p>
 */
public class BlockSellafieldSlaked extends Block {

    public static final IntegerProperty VARIANT = IntegerProperty.create("variant", 0, 3);
    public static final IntegerProperty COLOR_LEVEL = IntegerProperty.create("color_level", 0, 10);

    public BlockSellafieldSlaked(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(VARIANT, 0).setValue(COLOR_LEVEL, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(VARIANT, COLOR_LEVEL);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        return defaultBlockState().setValue(VARIANT, variantFromPos(pos));
    }

    /**
     * Установка VARIANT по позиции — важна когда блок ставится не игроком (например осадком
     * Fatman), т.к. в этом случае {@link #getStateForPlacement} не вызывается.
     * 1:1 с 1.7.10 {@code BlockSellafieldSlaked} — без scheduled/random tick, без эмиттера.
     */
    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide && state.getValue(VARIANT) != variantFromPos(pos)) {
            level.setBlock(pos, state.setValue(VARIANT, variantFromPos(pos)), Block.UPDATE_CLIENTS);
        }
    }

    static int variantFromPos(BlockPos pos) {
        long l = (pos.getX() * 3129871L) ^ (long) pos.getY() * 116129781L ^ (long) pos.getZ();
        l = l * l * 42317861L + l * 11L;
        return Math.abs((int) (l >> 16 & 3L)) % 4;
    }
}
