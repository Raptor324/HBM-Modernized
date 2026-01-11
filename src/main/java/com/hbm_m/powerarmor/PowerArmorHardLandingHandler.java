package com.hbm_m.powerarmor;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class PowerArmorHardLandingHandler {

    private static final float MACE_MIN_FALL = 1.5F;
    private static final float MACE_HEAVY_FALL = 5.0F;

    // Оригинальный HBM-порог для AOE
    private static final float HBM_AOE_MIN_FALL = 10.0F;
    private static final double HBM_RADIUS = 3.0D;

    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide) return;

        if (!ModArmorFSB.hasFSBArmor(player)) return;

        var chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!(chest.getItem() instanceof ModPowerArmorItem armorItem)) return;

        var specs = armorItem.getSpecs();
        if (!specs.hasHardLanding) return;

        // Mace: не срабатывает при fall-flying (elytra)
        if (player.isFallFlying()) return;

        float fallDist = event.getDistance();

        // --- 1) Частицы (mace-like) ---
        if (fallDist > MACE_MIN_FALL && player.level() instanceof ServerLevel level) {
            BlockPos groundPos = BlockPos.containing(player.getX(), player.getY() - 0.2D, player.getZ());
            BlockState groundState = level.getBlockState(groundPos);

            Vec3 center = Vec3.atCenterOf(groundPos).add(0.0, 0.5, 0.0);
            int count = (int) Mth.clamp(50.0F * fallDist, 0.0F, 200.0F);

            level.sendParticles(
                new BlockParticleOption(ParticleTypes.BLOCK, groundState),
                center.x, center.y, center.z,
                count,
                0.3D, 0.3D, 0.3D,
                0.15D
            );

            // --- 2) Звук (mace-like) ---
            // ВАЖНО: эти SoundEvent нужно зарегистрировать в ModSounds (своими ogg, не ванильными).
            var sound = (fallDist > MACE_HEAVY_FALL)
                ? com.hbm_m.sound.ModSounds.MACE_SMASH_GROUND_HEAVY.get()
                : com.hbm_m.sound.ModSounds.MACE_SMASH_GROUND.get();

            level.playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, 1.0F, 1.0F);
        }

        // --- 3) AOE (как в 1.7.10 HBM) ---
        if (fallDist > HBM_AOE_MIN_FALL && player.level() instanceof ServerLevel level) {
            AABB box = player.getBoundingBox().inflate(HBM_RADIUS, 0.0D, HBM_RADIUS);

            for (Entity e : level.getEntities(player, box)) {
                if (e == player) continue;
                if (e instanceof ItemEntity) continue;

                Vec3 dir = e.position().subtract(player.position());
                double d = Math.sqrt(dir.x * dir.x + dir.z * dir.z);
                if (d >= HBM_RADIUS || d <= 1.0E-6) continue;

                double intensity = HBM_RADIUS - d;

                // Отбрасывание "наружу" (эквивалент vec * intensity * -2 из 1.7.10)
                double nx = dir.x / d;
                double nz = dir.z / d;
                e.setDeltaMovement(
                    e.getDeltaMovement().x + nx * intensity * 2.0,
                    e.getDeltaMovement().y + 0.1D * intensity,
                    e.getDeltaMovement().z + nz * intensity * 2.0
                );
                e.hurtMarked = true;

                // Урон: intensity * 10 (как в оригинале)
                float damage = (float) (intensity * 10.0);

                // Лучше сделать кастомный DamageType "hard_landing" с bypass armor,
                // но как fallback можно ударить playerAttack:
                if (e instanceof net.minecraft.world.entity.LivingEntity le) {
                    le.hurt(player.damageSources().playerAttack(player), damage);
                }
            }
        }
    }
}
