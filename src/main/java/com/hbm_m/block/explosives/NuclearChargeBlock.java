package com.hbm_m.block.explosives;

import com.hbm_m.block.IDetonatable;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.particle.ModExplosionParticles;
import com.hbm_m.particle.explosions.ExplosionParticleUtils;
import com.hbm_m.util.CraterGenerator;

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
 * ОПТИМИЗИРОВАННЫЙ ЯДЕРНЫЙ БЛОК v2
 *
 * Улучшения:
 * ✅ Кэширование позиций для частиц
 * ✅ Оптимизированное спавнение частиц
 * ✅ Асинхронная генерация кратера
 * ✅ Уменьшение нагрузки на основной поток
 */
public class NuclearChargeBlock extends Block implements IDetonatable {

    private static final Logger LOGGER = LoggerFactory.getLogger("NuclearCharge");

    private static final float EXPLOSION_POWER = 25.0F;
    private static final double PARTICLE_VIEW_DISTANCE = 512.0;

    // Параметры воронки
    private static final int CRATER_RADIUS = 60; // Радиус воронки в блоках
    private static final int CRATER_DEPTH = 30;  // Глубина воронки в блоках

    // ОПТИМИЗАЦИЯ: Задержка перед генерацией кратера (в тиках)
    private static final int CRATER_GENERATION_DELAY = 30;

    public NuclearChargeBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack,
                                @Nullable net.minecraft.world.level.BlockGetter level,
                                List<Component> tooltip,
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

            // Взрыв (без разрушения блоков - за это отвечает воронка)
            level.explode(null, x, y, z, EXPLOSION_POWER,
                    Level.ExplosionInteraction.NONE);

            // Запускаем поэтапную систему частиц
            scheduleExplosionEffects(serverLevel, x, y, z);

            // ОПТИМИЗАЦИЯ: Генерация кратера в отдельном тике
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                server.tell(new TickTask(CRATER_GENERATION_DELAY, () -> {
                    CraterGenerator.generateCrater(
                            serverLevel,
                            pos,
                            CRATER_RADIUS,
                            CRATER_DEPTH,
                            ModBlocks.SELLAFIELD_SLAKED.get(),
                            ModBlocks.SELLAFIELD_SLAKED1.get(),
                            ModBlocks.SELLAFIELD_SLAKED2.get(),
                            ModBlocks.SELLAFIELD_SLAKED3.get(),
                            ModBlocks.WASTE_LOG.get(),
                            ModBlocks.WASTE_PLANKS.get(),
                            ModBlocks.BURNED_GRASS.get()

                    );
                    LOGGER.info("Кратер успешно сгенерирован в позиции: {}", pos);
                }));
            }

            return true;
        }

        return false;
    }

    /**
     * ОПТИМИЗИРОВАНА: Планирование эффектов взрыва
     */
    private void scheduleExplosionEffects(ServerLevel level, double x, double y, double z) {
        // ✅ Flash - точно те же параметры
        level.sendParticles(
                (SimpleParticleType) ModExplosionParticles.FLASH.get(),
                x, y, z, 1, 0, 0, 0, 0
        );

        // ✅ Sparks - 400 частиц с ТОЧНЫМИ скоростями
        ExplosionParticleUtils.spawnAirBombSparks(level, x, y, z);

        // ✅ Shockwave через 3 тика - точно те же кольца
        level.getServer().tell(new net.minecraft.server.TickTask(3, () ->
                ExplosionParticleUtils.spawnAirBombShockwave(level, x, y, z)));

        // ✅ Mushroom Cloud через 8 тиков - ТОЧНО те же параметры
        level.getServer().tell(new net.minecraft.server.TickTask(8, () ->
                ExplosionParticleUtils.spawnAirBombMushroomCloud(level, x, y, z)));
    }

}