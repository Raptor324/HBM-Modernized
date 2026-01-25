package com.hbm_m.block.custom.explosives;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.particle.ModExplosionParticles;
import com.hbm_m.particle.explosions.nuclear.medium.MediumNuclearMushroomCloud; // Наш класс с методами
import com.hbm_m.util.explosions.nuclear.CraterGenerator;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.List;

/**
 * ✅ ЯДЕРНЫЙ БЛОК v6 - АНИМИРОВАННЫЙ РОСТ
 * Управляет таймингами появления каждой части гриба.
 */
public class NuclearChargeBlock extends Block implements IDetonatable {

    private static final Logger LOGGER = LoggerFactory.getLogger("NuclearCharge");
    private static final float EXPLOSION_POWER = 25.0F;
    private static final int CRATER_DELAY = 40; // Кратер появляется после всех эффектов

    public NuclearChargeBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable net.minecraft.world.level.BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.hbm_m.nuclear_charge.line1").withStyle(ChatFormatting.DARK_RED));
        tooltip.add(Component.translatable("tooltip.hbm_m.nuclear_charge.line2").withStyle(ChatFormatting.RED));
    }

    @Override
    public boolean onDetonate(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide) {
            ServerLevel serverLevel = (ServerLevel) level;
            double x = pos.getX() + 0.5;
            double y = pos.getY() + 0.5;
            double z = pos.getZ() + 0.5;

            level.removeBlock(pos, false);
            level.explode(null, x, y, z, EXPLOSION_POWER, Level.ExplosionInteraction.NONE);

            // 🚀 ЗАПУСК АНИМАЦИИ
            scheduleAnimatedNuclearExplosion(serverLevel, x, y, z);

            // 🌍 КРАТЕР
            scheduleCraterGeneration(serverLevel, pos);

            return true;
        }
        return false;
    }

    /**
     * 🎬 ГЛАВНЫЙ СЦЕНАРИЙ АНИМАЦИИ
     */
    private void scheduleAnimatedNuclearExplosion(ServerLevel level, double x, double y, double z) {
        LOGGER.info("[NUCLEAR] Starting animation sequence at ({}, {}, {})", x, y, z);
        MinecraftServer server = level.getServer();

        // T=0: Вспышка + Черная сфера
        level.sendParticles((SimpleParticleType) ModExplosionParticles.FLASH.get(), x, y, z, 1, 0, 0, 0, 0);
        MediumNuclearMushroomCloud.spawnBlackSphere(level, x, y, z, level.random);

        // T=2: Ударная волна
        server.tell(new TickTask(level.getServer().getTickCount() + 2, () -> {
            MediumNuclearMushroomCloud.spawnShockwaveRing(level, x, y, z, level.random);
        }));

        // T=5..15: РОСТ СТОЛБА (по 3 слоя за тик)
        // Имитируем подъем дыма
        for (int i = 0; i < 10; i++) {
            final int step = i;
            server.tell(new TickTask(level.getServer().getTickCount() + 5 + i, () -> {
                // Высота текущего сегмента
                double currentY = y + (step * 2.0);
                // Спавним кусочек столба
                MediumNuclearMushroomCloud.spawnStemSegment(level, x, currentY, z, level.random);
            }));
        }

        // T=8: Основание (пока столб растет)
        server.tell(new TickTask(level.getServer().getTickCount() + 8, () -> {
            MediumNuclearMushroomCloud.spawnMushroomBase(level, x, y, z, level.random);
        }));

        // T=18: Шапка (на вершине выросшего столба)
        server.tell(new TickTask(level.getServer().getTickCount() + 18, () -> {
            MediumNuclearMushroomCloud.spawnMushroomCap(level, x, y, z, level.random);
        }));

        // T=22: Кольцо
        server.tell(new TickTask(level.getServer().getTickCount() + 22, () -> {
            MediumNuclearMushroomCloud.spawnCondensationRing(level, x, y + 15, z, level.random);
        }));
    }

    private void scheduleCraterGeneration(ServerLevel level, BlockPos pos) {
        MinecraftServer server = level.getServer();
        server.tell(new TickTask(level.getServer().getTickCount() + CRATER_DELAY, () -> {
            CraterGenerator.generateCrater(
                    level, pos,
                    ModBlocks.SELLAFIELD_SLAKED.get(), ModBlocks.SELLAFIELD_SLAKED1.get(),
                    ModBlocks.SELLAFIELD_SLAKED2.get(), ModBlocks.SELLAFIELD_SLAKED3.get(),
                    ModBlocks.WASTE_LOG.get(), ModBlocks.WASTE_PLANKS.get(),
                    ModBlocks.BURNED_GRASS.get(), ModBlocks.DEAD_DIRT.get()
            );
        }));
    }
}
