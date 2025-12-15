package com.hbm_m.item.custom.radiation_meter;

import com.hbm_m.block.custom.machines.armormod.util.ArmorModificationHelper;
import com.hbm_m.config.ModClothConfig;
import com.hbm_m.network.ModPacketHandler;
import com.hbm_m.network.sounds.GeigerSoundPacket;
import com.hbm_m.radiation.ChunkRadiationManager;
import com.hbm_m.radiation.PlayerHandler;
import com.hbm_m.sound.ModSounds;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PacketDistributor;

import javax.annotation.Nonnull;

/**
 * Базовый класс для всех измерителей радиации (Счетчик Гейгера, Дозиметр).
 * Содержит общую логику измерения и вывода информации.
 */

public abstract class AbstractRadiationMeterItem extends Item {

    public AbstractRadiationMeterItem(Properties pProperties) {
        super(pProperties);
    }

    @Nonnull
    @Override
    public InteractionResultHolder<ItemStack> use(@Nonnull Level pLevel, @Nonnull Player pPlayer, @Nonnull InteractionHand pUsedHand) {
        if (!pLevel.isClientSide() && pPlayer instanceof ServerPlayer serverPlayer) {
            RadiationData data = measureRadiation(pLevel, serverPlayer);
            Component message = createUsageMessage(data);
            pPlayer.sendSystemMessage(message);
            playSoundOnClick(serverPlayer);
        }
        return InteractionResultHolder.success(pPlayer.getItemInHand(pUsedHand));
    }

    protected RadiationData measureRadiation(Level level, Player player) {
        float chunkRad = 0F;
        float invRad = 0F;
        if (ModClothConfig.get().enableRadiation) {
            chunkRad = ChunkRadiationManager.getRadiation(
                level,
                player.getBlockX(),
                (int)Math.floor(player.getY() + player.getBbHeight() * 0.5),
                player.getBlockZ()
            );
            invRad = PlayerHandler.getInventoryRadiation(player);
        }
        
        float playerRads = PlayerHandler.getPlayerRads(player);
        float totalAbsoluteProtection = 0f;
        for (ItemStack armorStack : player.getArmorSlots()) {
            totalAbsoluteProtection += ArmorModificationHelper.getTotalAbsoluteRadProtection(armorStack);
        }
        float protectionPercent = ArmorModificationHelper.convertAbsoluteToPercent(totalAbsoluteProtection);        
        
        // Передаем новые данные в record
        return new RadiationData(chunkRad, invRad, playerRads, protectionPercent, totalAbsoluteProtection);
    }

    protected abstract Component createUsageMessage(RadiationData data);

    protected void playSoundOnClick(ServerPlayer player) {
        // Проверяем, существует ли наш Optional<RegistryObject>
        if (ModSounds.TOOL_TECH_BOOP.isPresent()) {
            // Получаем сам SoundEvent из RegistryObject
            SoundEvent soundEvent = ModSounds.TOOL_TECH_BOOP.get();
            if (soundEvent != null) {
                // И уже у него получаем ResourceLocation
                ResourceLocation soundLocation = soundEvent.getLocation();
                ModPacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new GeigerSoundPacket(soundLocation, 1.0F, 1.0F));
            }
        }
    }

    protected record RadiationData(float chunkRad, float inventoryRad, float playerRad, float protectionPercent, float protectionAbsolute) {
        public float getTotalEnvironmentRad() {
            return chunkRad + inventoryRad;
        }
    }

    protected static String getRadColor(float rads) {
        if (rads < 0.01f) return "§a";
        if (rads < 1.0f) return "§e";
        if (rads < 10.0f) return "§6";
        if (rads < 100.0f) return "§c";
        if (rads < 1000.0f) return "§4";
        return "§7";
    }
}