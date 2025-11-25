package com.hbm_m.api.energy;


// Импортируй свой класс регистрации BlockEntityTypes
// import com.hbm_m.init.ModBlockEntities;
import com.hbm_m.api.energy.ConverterBlockEntity;
import com.hbm_m.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class ConverterBlock extends BaseEntityBlock {

    public ConverterBlock(Properties properties) {
        super(properties);
    }


    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        // БЫЛО: return new ConverterBlockEntity(ModBlockEntities.CONVERTER.get(), pos, state);
        // СТАЛО (убираем первый аргумент):
        return new ConverterBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL; // Чтобы блок рендерился
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null; // На клиенте тикать не надо

        // Проверяем тип и возвращаем наш метод serverTick
        return createTickerHelper(type, ModBlockEntities.CONVERTER_BE.get(), ConverterBlockEntity::serverTick);
    }
}
