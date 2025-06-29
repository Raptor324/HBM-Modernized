package com.hbm_m.item;

import com.hbm_m.main.MainRegistry;
import com.hbm_m.radiation.ChunkRadiationManager;
// import com.hbm_m.radiation.ChunkRadiationHandlerSimple;
import com.hbm_m.radiation.PlayerRadiationHandler;
import com.hbm_m.sound.ModSounds;
import com.hbm_m.network.ModPacketHandler;
import com.hbm_m.network.RadiationDataPacket;
import com.hbm_m.network.GeigerSoundPacket;
import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.PacketDistributor;
import com.hbm_m.config.ModClothConfig;

import java.util.Random;

public class GeigerCounterItem extends Item {

    private static final Random RANDOM = new Random();
    private static final int SOUND_INTERVAL_TICKS = 5; // 0.25 секунды (соответствует 5 тикам в старой версии)
    private int soundTickCounter = 0;

    public GeigerCounterItem(Properties pProperties) {
        super(pProperties);
    }

    @Nonnull
    @Override
    public InteractionResultHolder<ItemStack> use(@Nonnull Level pLevel, @Nonnull Player pPlayer, @Nonnull InteractionHand pUsedHand) {
        if (!pLevel.isClientSide()) {
            float playerRads = PlayerRadiationHandler.getPlayerRads(pPlayer);

            // Обновляем призмы для текущего чанка перед расчетом
            // Обновление призм временно закомментировано, так как метод updateChunkPrisms отсутствует в ChunkRadiationHandlerSimple
            // if (ChunkRadiationManager.proxy instanceof ChunkRadiationHandlerSimple prismHandler) {
            //     prismHandler.updateChunkPrisms(
            //         pLevel,
            //         new net.minecraft.world.level.ChunkPos((int)Math.floor(pPlayer.getX()) >> 4, (int)Math.floor(pPlayer.getZ()) >> 4)
            //     );
            // }

            float chunkRad = 0F;
            float invRad = 0F;
            float totalEnvironmentRads = 0F;
            if (ModClothConfig.get().enableRadiation) {
                chunkRad = ChunkRadiationManager.getRadiation(
                    pLevel,
                    (int)Math.floor(pPlayer.getX()),
                    (int)Math.floor(pPlayer.getY() + pPlayer.getBbHeight() * 0.5),
                    (int)Math.floor(pPlayer.getZ())
                );
                invRad = PlayerRadiationHandler.getInventoryRadiation(pPlayer);
                totalEnvironmentRads = chunkRad + invRad;
            }

            float protection = 0.0f;
            float protectionAbs = 0.0f;
            // protection = PlayerRadiationHandler.getProtectionPercent(pPlayer);
            // protectionAbs = PlayerRadiationHandler.getProtectionAbs(pPlayer);

            Component msg = Component.translatable(
                "item.hbm_m.geiger_counter.full_message",
                String.format("%.1f", chunkRad),
                String.format("%.1f", totalEnvironmentRads),
                String.format("%.1f", playerRads),
                String.format("%.1f", protection),
                String.format("%.1f", protectionAbs)
            );
            pPlayer.sendSystemMessage(msg);

            
            if (ModSounds.TOOL_TECH_BOOP != null && ModSounds.TOOL_TECH_BOOP.isPresent() && pPlayer instanceof ServerPlayer serverPlayer) {
                ResourceLocation soundLocation = ModSounds.TOOL_TECH_BOOP.get().getLocation();
                float volume = 1.0F; // Можно настроить громкость
                float pitch = 1.0F; // Можно настроить высоту тона
                ModPacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> serverPlayer), new GeigerSoundPacket(soundLocation, volume, pitch));
            }
        }
        return InteractionResultHolder.success(pPlayer.getItemInHand(pUsedHand));
    }

    
    @Override
    public void inventoryTick(@Nonnull ItemStack pStack, @Nonnull Level pLevel, @Nonnull Entity pEntity, int pSlotId, boolean pIsSelected) {
        // Звук треска проигрывается если предмет есть в инвентаре (НЕ только в руке)
        if (!pLevel.isClientSide() && pEntity instanceof ServerPlayer serverPlayer && serverPlayer.isAlive()) {
            soundTickCounter++;
            if (soundTickCounter >= SOUND_INTERVAL_TICKS) {
                soundTickCounter = 0;

                // Обновляем призмы для текущего чанка перед расчетом
                // Обновление призм временно закомментировано, так как метод updateChunkPrisms отсутствует в ChunkRadiationHandlerSimple
                // if (ChunkRadiationManager.proxy instanceof ChunkRadiationHandlerSimple prismHandler) {
                //     prismHandler.updateChunkPrisms(
                //         pLevel,
                //         new net.minecraft.world.level.ChunkPos((int)Math.floor(serverPlayer.getX()) >> 4, (int)Math.floor(serverPlayer.getZ()) >> 4)
                //     );
                // }

                float chunkRad = 0F;
                float invRad = 0F;
                float totalEnvironmentRads = 0F;
                if (ModClothConfig.get().enableRadiation) {
                    chunkRad = ChunkRadiationManager.getRadiation(
                        pLevel,
                        (int)Math.floor(serverPlayer.getX()),
                        (int)Math.floor(serverPlayer.getY() + serverPlayer.getBbHeight() * 0.5),
                        (int)Math.floor(serverPlayer.getZ())
                    );
                    invRad = PlayerRadiationHandler.getInventoryRadiation(serverPlayer);
                    totalEnvironmentRads = chunkRad + invRad;
                }
                MainRegistry.LOGGER.debug("GeigerCounter: chunkRad = {}, invRad = {}, totalEnvironmentRads = {}", chunkRad, invRad, totalEnvironmentRads);

                // Отправляем пакет только если игрок не в креативе или наблюдателе
                if (!serverPlayer.isCreative() && !serverPlayer.isSpectator()) {
                    ModPacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> serverPlayer), new RadiationDataPacket(totalEnvironmentRads));
                }

                int soundIndex = 0;
                List<Integer> soundOptions = new ArrayList<>();
                ResourceLocation soundLocation = null;

                if (totalEnvironmentRads > 0) {
                    if (totalEnvironmentRads > 0 && totalEnvironmentRads < 10) soundOptions.add(1);
                    if (totalEnvironmentRads > 5 && totalEnvironmentRads < 15) soundOptions.add(2);
                    if (totalEnvironmentRads > 10 && totalEnvironmentRads < 20) soundOptions.add(3);
                    if (totalEnvironmentRads > 15 && totalEnvironmentRads < 25) soundOptions.add(4);
                    if (totalEnvironmentRads > 20 && totalEnvironmentRads < 30) soundOptions.add(5);
                    if (totalEnvironmentRads > 25) soundOptions.add(6);

                    if (!soundOptions.isEmpty()) {
                        soundIndex = soundOptions.get(RANDOM.nextInt(soundOptions.size()));
                    } else {
                        soundIndex = 0;
                    }
                } else if (totalEnvironmentRads == 0 && RANDOM.nextInt(50) == 0) { // Добавлено условие totalEnvironmentRads == 0
                    soundIndex = 1;
                } else {
                    soundIndex = 0;
                }
                MainRegistry.LOGGER.debug("GeigerCounter: soundIndex = {}", soundIndex);

                // Безопасно получаем нужный звук
                switch (soundIndex) {
                    case 1:
                        if (ModSounds.GEIGER_1 != null && ModSounds.GEIGER_1.isPresent())
                            soundLocation = ModSounds.GEIGER_1.get().getLocation();
                        break;
                    case 2:
                        if (ModSounds.GEIGER_2 != null && ModSounds.GEIGER_2.isPresent())
                            soundLocation = ModSounds.GEIGER_2.get().getLocation();
                        break;
                    case 3:
                        if (ModSounds.GEIGER_3 != null && ModSounds.GEIGER_3.isPresent())
                            soundLocation = ModSounds.GEIGER_3.get().getLocation();
                        break;
                    case 4:
                        if (ModSounds.GEIGER_4 != null && ModSounds.GEIGER_4.isPresent())
                            soundLocation = ModSounds.GEIGER_4.get().getLocation();
                        break;
                    case 5:
                        if (ModSounds.GEIGER_5 != null && ModSounds.GEIGER_5.isPresent())
                            soundLocation = ModSounds.GEIGER_5.get().getLocation();
                        break;
                    case 6:
                        if (ModSounds.GEIGER_6 != null && ModSounds.GEIGER_6.isPresent())
                            soundLocation = ModSounds.GEIGER_6.get().getLocation();
                        break;
                    default:
                        soundLocation = null;
                        break;
                }

                if (soundLocation != null) {
                    float volume = 0.4F;
                    float pitch = 1.0F;
                    
                    ModPacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> serverPlayer), new GeigerSoundPacket(soundLocation, volume, pitch));
                }
            }
        }
    }
}