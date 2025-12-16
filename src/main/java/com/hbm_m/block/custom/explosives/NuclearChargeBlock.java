package com.hbm_m.block.custom.explosives;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.particle.ModExplosionParticles;
import com.hbm_m.particle.explosions.ExplosionParticleUtils;
import com.hbm_m.particle.explosions.NuclearExplosionExtensions;
import com.hbm_m.util.explosions.nuclear.CraterGenerator;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.TickTask;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.annotation.Nullable;
import java.util.List;

/**
 * ✅ ЯДЕРНЫЙ БЛОК v5 - С ПОЛНЫМ ГРИБНЫМ ОБЛАКОМ
 *
 * НОВЫЕ ОСОБЕННОСТИ:
 * ✅ Полный ядерный эффект (гриб + ударная волна + искры)
 * ✅ Использует расширенные частицы (LargeExplosionSpark, LargeDarkSmoke)
 * ✅ Поэтапный спавн эффектов (как в NuclearExplosionExtensions)
 * ✅ Синхронизирован с кратером
 *
 * ПОЭТАПНОСТЬ ЭФФЕКТОВ:
 * - 0 тик: Flash (вспышка)
 * - 1 тик: Большие искры
 * - 3 тика: Ударная волна
 * - 8 тиков: Грибное облако
 * - 30 тиков: Кратер начинает генерироваться
 */
public class NuclearChargeBlock extends Block implements IDetonatable {

    private static final Logger LOGGER = LoggerFactory.getLogger("NuclearCharge");

    private static final float EXPLOSION_POWER = 25.0F;
    private static final double PARTICLE_VIEW_DISTANCE = 512.0;
    private static final int CRATER_GENERATION_DELAY = 30;

    public NuclearChargeBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack,
                                @Nullable net.minecraft.world.level.BlockGetter level,
                                List tooltip,
                                TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.hbm_m.nuclear_charge.line1")
                .withStyle(ChatFormatting.DARK_RED));
        tooltip.add(Component.translatable("tooltip.hbm_m.nuclear_charge.line2")
                .withStyle(ChatFormatting.RED));
        tooltip.add(Component.translatable("tooltip.hbm_m.nuclear_charge.line3")
                .withStyle(ChatFormatting.RED));
        tooltip.add(Component.translatable("tooltip.hbm_m.nuclear_charge.line4")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.hbm_m.nuclear_charge.line5")
                .withStyle(ChatFormatting.GRAY));
    }

    @Override
    public boolean onDetonate(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide) {
            ServerLevel serverLevel = (ServerLevel) level;
            double x = pos.getX() + 0.5;
            double y = pos.getY() + 0.5;
            double z = pos.getZ() + 0.5;

            level.removeBlock(pos, false);

            // ✅ Взрыв (без разрушения блоков - за это отвечает кратер)
            level.explode(null, x, y, z, EXPLOSION_POWER,
                    Level.ExplosionInteraction.NONE);

            // ✅ НОВОЕ: Запуск полного ядерного эффекта
            scheduleFullNuclearExplosion(serverLevel, x, y, z);

            // ✅ ГЕНЕРАЦИЯ КРАТЕРА (через 30 тиков после взрыва)
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                server.tell(new TickTask(CRATER_GENERATION_DELAY, () -> {
                    CraterGenerator.generateCrater(
                            serverLevel,
                            pos,
                            ModBlocks.SELLAFIELD_SLAKED.get(),
                            ModBlocks.SELLAFIELD_SLAKED1.get(),
                            ModBlocks.SELLAFIELD_SLAKED2.get(),
                            ModBlocks.SELLAFIELD_SLAKED3.get(),
                            ModBlocks.WASTE_LOG.get(),
                            ModBlocks.WASTE_PLANKS.get(),
                            ModBlocks.BURNED_GRASS.get(),
                            ModBlocks.DEAD_DIRT.get()
                    );
                    LOGGER.info("Кратер успешно сгенерирован в позиции: {}", pos);
                }));
            }

            return true;
        }

        return false;
    }

    /**
     * ✅ ПОЛНОЕ ПЛАНИРОВАНИЕ ЭФФЕКТОВ ЯДЕРНОГО ВЗРЫВА
     *
     * Включает всё: вспышку, искры, ударную волну, и грибное облако!
     */
    private void scheduleFullNuclearExplosion(ServerLevel level, double x, double y, double z) {

        LOGGER.info("[NUCLEAR] 🌋 Triggering full nuclear explosion at ({}, {}, {})", x, y, z);

        // ════════════════════════════════════════════════════════════════════
        // ФАЗА 0 (тик 0): ВСПЫШКА
        // ════════════════════════════════════════════════════════════════════
        level.sendParticles(
                (SimpleParticleType) ModExplosionParticles.FLASH.get(),
                x, y, z, 1, 0, 0, 0, 0
        );
        LOGGER.info("[NUCLEAR] Phase 0: Flash");

        // ════════════════════════════════════════════════════════════════════
        // ФАЗА 1 (тик 1): БОЛЬШИЕ ИСКРЫ (600 штук)
        // ════════════════════════════════════════════════════════════════════
        level.getServer().tell(new TickTask(1, () -> {
            LOGGER.info("[NUCLEAR] Phase 1: Large explosion sparks (600 particles)");
            NuclearExplosionExtensions.spawnLargeExplosionSparks(level, x, y, z, 600);
        }));

        // ════════════════════════════════════════════════════════════════════
        // ФАЗА 2 (тик 3): УДАРНАЯ ВОЛНА (мощная)
        // ════════════════════════════════════════════════════════════════════
        level.getServer().tell(new TickTask(3, () -> {
            LOGGER.info("[NUCLEAR] Phase 2: Enhanced shockwave");
            NuclearExplosionExtensions.spawnEnhancedShockwave(level, x, y, z);
        }));

        // ════════════════════════════════════════════════════════════════════
        // ФАЗА 3 (тик 8): ГРИБНОЕ ОБЛАКО (многоуровневое)
        // ════════════════════════════════════════════════════════════════════
        level.getServer().tell(new TickTask(8, () -> {
            LOGGER.info("[NUCLEAR] Phase 3: Mushroom cloud formation");
            NuclearExplosionExtensions.spawnNuclearMushroomCloud(level, x, y, z);
        }));

        LOGGER.info("[NUCLEAR] ✅ All explosion phases scheduled!");
    }
}