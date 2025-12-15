package com.hbm_m.api.energy;

import com.hbm_m.block.entity.ModBlockEntities;
import com.hbm_m.block.entity.custom.machines.MachineBatteryBlockEntity;
import com.hbm_m.util.EnergyFormatter;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Универсальный класс блока для всех энергохранилищ.
 * ✅ Корректно интегрирован в энергосеть HBM.
 */
public class MachineBatteryBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private final long capacity;

    public MachineBatteryBlock(Properties properties, long capacity) {
        super(properties);
        this.capacity = capacity;
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public long getCapacity() {
        return this.capacity;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return this.defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachineBatteryBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.MACHINE_BATTERY_BE.get(), MachineBatteryBlockEntity::tick);
    }

    // ✅ Регистрация в энергосети


    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide()) {
            // Оставляем *только* логику удаления из сети
            EnergyNetworkManager.get((ServerLevel) level).removeNode(pos);

            // ... (и другое, если оно не связано с дропом) ...
        }

        // Вызываем super.onRemove, который сам вызовет loot table
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide) {
            BlockEntity entity = level.getBlockEntity(pos);
            if (entity instanceof MachineBatteryBlockEntity battery) {
                NetworkHooks.openScreen((ServerPlayer) player, battery, pos);
            }
            return InteractionResult.CONSUME;
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void setPlacedBy(Level pLevel, BlockPos pPos, BlockState pState, @Nullable LivingEntity pPlacer, ItemStack pStack) {
        super.setPlacedBy(pLevel, pPos, pState, pPlacer, pStack);

        if (!pLevel.isClientSide) {
            BlockEntity be = pLevel.getBlockEntity(pPos);
            if (be instanceof MachineBatteryBlockEntity batteryBE) {

                // Проверяем, есть ли у предмета NBT
                CompoundTag itemNbt = pStack.getTag();

                // Ищем наш специальный тег "BlockEntityTag", который создал лут-тейбл
                if (itemNbt != null && itemNbt.contains("BlockEntityTag")) {

                    // Загружаем все данные из этого тега в наш BlockEntity
                    batteryBE.load(itemNbt.getCompound("BlockEntityTag"));

                    // (Метод load() в MachineBatteryBlockEntity
                    // уже содержит super.load(), так что мы просто передаем ему наши данные)

                    batteryBE.setChanged(); // Уведомляем мир об изменениях
                }
            }
        }
    }
    

    @Override
    public void appendHoverText(ItemStack pStack, @Nullable BlockGetter pLevel, List<Component> pTooltip, TooltipFlag pFlag) {
        super.appendHoverText(pStack, pLevel, pTooltip, pFlag);

        // 1. Получаем сохраненную энергию из NBT
        long energy = 0;
        CompoundTag nbt = pStack.getTag();

        // Мы читаем тот же "BlockEntityTag", который записали в loot table
        if (nbt != null && nbt.contains("BlockEntityTag")) {
            energy = nbt.getCompound("BlockEntityTag").getLong("Energy");
        }

        // 2. Форматируем
        // this.capacity берется из поля класса MachineBatteryBlock
        String energyStr = EnergyFormatter.format(energy);
        String maxEnergyStr = EnergyFormatter.format(this.capacity);

        // 3. Добавляем в тултип
        pTooltip.add(Component.translatable("tooltip.hbm_m.machine_battery.stored", energyStr, maxEnergyStr)
                .withStyle(ChatFormatting.YELLOW));
    }
}