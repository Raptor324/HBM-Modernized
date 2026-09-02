package com.hbm_m.particle.helper;

import com.hbm_m.client.handler.ClientVanishHandler;
import com.hbm_m.particle.nt.ParticleAshesNT;
import com.hbm_m.particle.nt.ParticleEngineNT;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * Порт {@code AshesCreator} из 1.7.10.
 * Создаёт кучу пепла (+ ванильный огонёк) на месте трупа сущности.
 */
public class AshesCreator implements IParticleCreator {

    /**
     * @param level мир
     * @param toPulverize испепеляемая сущность
     * @param ashesCount количество частиц пепла
     * @param ashesScale масштаб каждой частицы (0.125 как в оригинале)
     */
    public static void composeEffect(net.minecraft.world.level.Level level, Entity toPulverize,
                                     int ashesCount, float ashesScale) {
        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            CompoundTag data = new CompoundTag();
            data.putString("type", "ashes");
            data.putInt("entityID", toPulverize.getId());
            data.putInt("ashesCount", ashesCount);
            data.putFloat("ashesScale", ashesScale);
            IParticleCreator.sendPacket(serverLevel,
                    toPulverize.getX(), toPulverize.getY(), toPulverize.getZ(), 100, data);
        }
    }

    @Override
    public void makeParticle(ClientLevel level, Player player, RandomSource rand,
                             double x, double y, double z, CompoundTag data) {
        int entityID = data.getInt("entityID");
        Entity entity = level.getEntity(entityID);

        int amount = data.getInt("ashesCount");
        float scale = data.getFloat("ashesScale");

        // Ваниш делаем даже если сущность не человекоподобная (могла быть без скелета).
        if (entity != null) {
            ClientVanishHandler.vanish(entityID);
        }

        // Для позиции используем либо сущность (если ещё существует), либо координаты пакета.
        double baseX = entity != null ? entity.getX() : x;
        double baseY = entity != null ? entity.getY() : y;
        double baseZ = entity != null ? entity.getZ() : z;
        float w = entity != null ? entity.getBbWidth() : 0.6F;
        float h = entity != null ? entity.getBbHeight() : 1.8F;

        for (int i = 0; i < amount; i++) {
            double px = baseX + (w + scale * 2.0) * (rand.nextDouble() - 0.5);
            double py = baseY + h * rand.nextDouble();
            double pz = baseZ + (w + scale * 2.0) * (rand.nextDouble() - 0.5);

            ParticleAshesNT ash = new ParticleAshesNT(level, px, py, pz, scale);
            ParticleEngineNT.INSTANCE.add(ash);

            // Ванильный огонёк поверх пепла, как в оригинале (EntityFlameFX).
            level.addParticle(ParticleTypes.FLAME, px, py, pz, 0.0, 0.0, 0.0);
        }
    }
}
