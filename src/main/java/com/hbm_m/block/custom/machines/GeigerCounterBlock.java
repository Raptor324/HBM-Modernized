package com.hbm_m.block.custom.machines;

// Этот класс реализует блок счетчика Гейгера, который измеряет радиацию в окружающей среде.
// При взаимодействии с блоком игрок получает информацию о радиации в виде сообщения в чат.
// Блок также может выдавать сигнал компаратору на основе уровня радиации.
import com.hbm_m.block.custom.machines.armormod.util.ArmorModificationHelper;
import com.hbm_m.block.entity.custom.machines.GeigerCounterBlockEntity;
import com.hbm_m.block.entity.ModBlockEntities;
import com.hbm_m.network.ModPacketHandler;
import com.hbm_m.network.sounds.GeigerSoundPacket;
import com.hbm_m.radiation.PlayerHandler;
import com.hbm_m.sound.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.PacketDistributor;
import net.minecraft.network.chat.Component;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class GeigerCounterBlock extends BaseEntityBlock {
    // Свойство для хранения направления, куда "смотрит" блок
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 8.0, 16.0);

    public GeigerCounterBlock(Properties pProperties) {
        super(pProperties);
        // Устанавливаем состояние по умолчанию
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public VoxelShape getShape(@Nonnull BlockState pState, @Nonnull BlockGetter pLevel, @Nonnull BlockPos pPos, @Nonnull CollisionContext pContext) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@Nonnull BlockPos pPos, @Nonnull BlockState pState) {
        return new GeigerCounterBlockEntity(pPos, pState);
    }

    @Override
    public InteractionResult use(@Nonnull BlockState pState, @Nonnull Level pLevel, @Nonnull BlockPos pPos, @Nonnull Player pPlayer, @Nonnull InteractionHand pHand, @Nonnull BlockHitResult pHit) {
        // Выполняем логику только на стороне сервера
        if (!pLevel.isClientSide()) {
            
            // Собираем ВСЕ данные, как в ручном счетчике

            // 1. Радиация окружения
            // Радиация в точке блока (из BlockEntity для точности и консистентности)
            float chunkRad = 0F;
            BlockEntity blockEntity = pLevel.getBlockEntity(pPos);
            if (blockEntity instanceof GeigerCounterBlockEntity geiger) {
                 chunkRad = geiger.getLastMeasuredRads();
            }
            // Радиация инвентаря кликнувшего игрока
            float invRad = PlayerHandler.getInventoryRadiation(pPlayer);
            float totalEnvironmentRad = chunkRad + invRad;

            // 2. Радиация самого игрока
            float playerRads = PlayerHandler.getPlayerRads(pPlayer);
            
            // 3. Защита игрока
            float totalAbsoluteProtection = 0f;
            for (ItemStack armorStack : pPlayer.getArmorSlots()) {
                totalAbsoluteProtection += ArmorModificationHelper.getTotalAbsoluteRadProtection(armorStack);
            }
            float protectionPercent = ArmorModificationHelper.convertAbsoluteToPercent(totalAbsoluteProtection);

            //Формируем сообщение

            // Создаем цветные строки для каждого значения
            String chunkRadStr = getRadColor(chunkRad) + String.format("%.1f RAD/s", chunkRad);
            String envRadStr = getRadColor(totalEnvironmentRad) + String.format("%.1f RAD/s\n", totalEnvironmentRad);
            String playerRadStr = getRadColor(playerRads) + String.format("%.1f RAD", playerRads);
            String protectionPercentStr = String.format("%.2f%%", protectionPercent * 100);
            String protectionAbsoluteStr = String.format("%.3f", totalAbsoluteProtection);

            // Собираем заголовок. Используем название блока.
            String titleString = "\n§6===== ☢ " + Component.translatable("item.hbm_m.meter.geiger_counter.name").getString() + " ☢ =====\n";
            MutableComponent message = Component.translatable("item.hbm_m.meter.title_format", titleString);

            // Добавляем строки данных, используя те же ключи локализации
            message.append(Component.translatable("item.hbm_m.meter.chunk_rads", chunkRadStr));
            message.append(Component.translatable("item.hbm_m.meter.env_rads", envRadStr));
            message.append(Component.translatable("item.hbm_m.meter.player_rads", playerRadStr));
            message.append(Component.translatable("item.hbm_m.meter.protection", protectionPercentStr, protectionAbsoluteStr));

            // Отправляем собранное сообщение игроку
            pPlayer.sendSystemMessage(message);

            if (ModSounds.TOOL_TECH_BOOP.isPresent()) {
                SoundEvent soundEvent = ModSounds.TOOL_TECH_BOOP.get();
                if (soundEvent != null) {
                    ResourceLocation soundLocation = soundEvent.getLocation();
                    // Важно: pPlayer нужно привести к ServerPlayer для отправки пакета
                    if (pPlayer instanceof ServerPlayer serverPlayer) {
                        ModPacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> serverPlayer), new GeigerSoundPacket(soundLocation, 1.0F, 1.0F));
                    }
                }
            }
        }
        
        return InteractionResult.sidedSuccess(pLevel.isClientSide());
    }

    // Вспомогательный метод для получения цвета
    private static String getRadColor(float rads) {
        if (rads < 0.01f) return "§a";
        if (rads < 1.0f) return "§e";
        if (rads < 10.0f) return "§6";
        if (rads < 100.0f) return "§c";
        if (rads < 1000.0f) return "§4";
        return "§7";
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@Nonnull Level pLevel, @Nonnull BlockState pState, @Nonnull BlockEntityType<T> pBlockEntityType) {
        // Мы хотим, чтобы наш BlockEntity работал только на стороне сервера
        if (pLevel.isClientSide()) {
            return null;
        }
        // Возвращаем тикер для нашего BlockEntity
        return createTickerHelper(pBlockEntityType, ModBlockEntities.GEIGER_COUNTER_BE.get(),
                (level, pos, state, blockEntity) -> blockEntity.tick(level, pos, state));
    }

    // Вращение блока 

    @Override
    public BlockState getStateForPlacement(@Nonnull BlockPlaceContext pContext) {
        // Устанавливаем направление блока в зависимости от того, куда смотрел игрок при установке
        return this.defaultBlockState().setValue(FACING, pContext.getHorizontalDirection());
    }

    @Override
    protected void createBlockStateDefinition(@Nonnull StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(FACING);
    }

    // Рендер и сигналы 
    @Override
    public RenderShape getRenderShape(@Nonnull BlockState pState) {
        return RenderShape.MODEL;
    }
    
    // Как в старом моде, блок будет выдавать сигнал компаратору
    @Override
    public boolean hasAnalogOutputSignal(@Nonnull BlockState pState) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(@Nonnull BlockState pState, @Nonnull Level pLevel, @Nonnull BlockPos pPos) {
        BlockEntity blockEntity = pLevel.getBlockEntity(pPos);
        if (blockEntity instanceof GeigerCounterBlockEntity geiger) {
            // Возвращаем уровень радиации, ограниченный 15 (макс. сигнал редстоуна)
            return Math.min(15, (int) Math.ceil(geiger.getLastMeasuredRads()));
        }
        return 0;
    }
}