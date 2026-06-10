package com.hbm_m.particle.helper;

import com.hbm_m.config.ModClothConfig;
import com.hbm_m.extprop.HbmLivingProps;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;

/**
 * Client-only entry point for custom NT particles (called from AuxParticlePacket).
 */
public final class ParticleEffectClient {

    private ParticleEffectClient() {}

    public static void effectNT(CompoundTag data) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            ClientLevel level = mc.level;
            if (level == null) return;
            Player player = mc.player;
            String type = data.getString("type");
            double x = data.getDouble("posX");
            double y = data.getDouble("posY");
            double z = data.getDouble("posZ");
            RandomSource rand = RandomSource.create();
            IParticleCreator creator = ParticleCreators.particleCreators().get(type);
            if (creator != null) {
                creator.makeParticle(level, player, rand, x, y, z, data);
            }
        });
    }

    /**
     * Аура радиации вокруг игрока при дозе &gt;600 RAD. Порт client-ветки {@code EntityEffectHandler.handleRadiationFX} (1.7.10).
     */
    public static void tickRadiationAura(Player player) {
        if (!ModClothConfig.get().enableRadiation || player == null) {
            return;
        }

        float radiation = HbmLivingProps.getRadiation(player);
        if (radiation <= 600F) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            return;
        }

        int count = radiation > 900F ? 4 : radiation > 800F ? 2 : 1;
        RandomSource rand = level.random;

        for (int i = 0; i < count; i++) {
            mc.particleEngine.createParticle(
                    ParticleTypes.END_ROD,
                    player.getX() + rand.nextGaussian() * 4.0D,
                    player.getY() + rand.nextGaussian() * 2.0D,
                    player.getZ() + rand.nextGaussian() * 4.0D,
                    rand.nextGaussian(),
                    rand.nextGaussian(),
                    rand.nextGaussian());
        }
    }
}
