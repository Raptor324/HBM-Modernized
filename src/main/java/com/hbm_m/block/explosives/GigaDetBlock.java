package com.hbm_m.block.explosives;

import com.hbm_m.block.IDetonatable;
import com.hbm_m.particle.ModExplosionParticles;
import com.hbm_m.util.MessGenerator;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class GigaDetBlock extends Block implements IDetonatable {
    private static final float EXPLOSION_POWER = 25.0F;
    private static final double PARTICLE_VIEW_DISTANCE = 512.0;

    // Параметры воронки
    private static final int CRATER_RADIUS = 30; // Радиус воронки в блоках
    private static final int CRATER_DEPTH = 20; // Глубина воронки в блоках

    // Блоки для поверхности воронки (замени на свои)
    private static final Block BLOCK1 = Blocks.STONE; // Замени на свой
    private static final Block BLOCK2 = Blocks.COBBLESTONE; // Замени на свой
    private static final Block BLOCK3 = Blocks.GRAVEL; // Замени на свой

    public GigaDetBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack,
                                @Nullable BlockGetter level,
                                List<Component> tooltip,
                                TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.hbm_m.gigadet.line1")
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
                    Level.ExplosionInteraction.NONE); // NONE = не разрушает блоки

            // Запускаем поэтапную систему частиц
            scheduleExplosionEffects(serverLevel, x, y, z);

            // ИСПРАВЛЕНИЕ: Проверка на null для сервера
            if (serverLevel.getServer() != null) {
                // Создаём воронку с задержкой (после взрывной волны)
                serverLevel.getServer().tell(new net.minecraft.server.TickTask(30, () -> {
                    // Анимированная версия (2 секунды)
                    MessGenerator.generateCrater(
                            serverLevel,
                            pos,
                            CRATER_RADIUS,
                            CRATER_DEPTH,
                            BLOCK1,
                            BLOCK2,
                            BLOCK3
                    );

                    // ИЛИ мгновенная версия (раскомментируй если нужна)
                    // CraterGenerator.generateCraterInstant(
                    //     serverLevel, pos, CRATER_RADIUS, CRATER_DEPTH,
                    //     BLOCK1, BLOCK2, BLOCK3
                    // );
                }));
            }

            return true;
        }
        return false;
    }

    private void scheduleExplosionEffects(ServerLevel level, double x, double y, double z) {
        // Фаза 1: Яркая вспышка (мгновенно)
        spawnFlash(level, x, y, z);

        // Фаза 2: Искры (0-10 тиков)
        spawnSparks(level, x, y, z);

        // Фаза 3: Взрывная волна (5 тиков задержки)
        level.getServer().tell(new net.minecraft.server.TickTask(5, () -> {
            spawnShockwave(level, x, y, z);
        }));

        // Фаза 4: Гриб из дыма (10 тиков задержки)
        level.getServer().tell(new net.minecraft.server.TickTask(10, () -> {
            spawnMushroomCloud(level, x, y, z);
        }));
    }

    private void spawnFlash(ServerLevel level, double x, double y, double z) {
        // Центральная вспышка - используем addAlwaysVisibleParticle через sendParticles с force=true
        level.sendParticles(
                ModExplosionParticles.FLASH.get(),
                x, y, z,
                1, // count
                0, 0, 0, // offset
                0 // speed
        );
    }

    private void spawnSparks(ServerLevel level, double x, double y, double z) {
        // 200 искр разлетающихся во все стороны
        for (int i = 0; i < 400; i++) {
            double xSpeed = (level.random.nextDouble() - 0.5) * 4.0;
            double ySpeed = level.random.nextDouble() * 3.0;
            double zSpeed = (level.random.nextDouble() - 0.5) * 4.0;

            level.sendParticles(
                    ModExplosionParticles.EXPLOSION_SPARK.get(),
                    x, y, z,
                    1,
                    xSpeed, ySpeed, zSpeed,
                    1.0
            );
        }
    }

    private void spawnShockwave(ServerLevel level, double x, double y, double z) {
        // 3 кольца взрывной волны на разной высоте
        for (int ring = 0; ring < 3; ring++) {
            double ringY = y + (ring * 0.5);

            level.sendParticles(
                    ModExplosionParticles.SHOCKWAVE.get(),
                    x, ringY, z,
                    1,
                    0, 0, 0,
                    0
            );
        }
    }

    private void spawnMushroomCloud(ServerLevel level, double x, double y, double z) {
        // Стебель гриба (вертикальная колонна дыма)
        for (int i = 0; i < 80; i++) {
            double offsetX = (level.random.nextDouble() - 0.5) * 4.0;
            double offsetZ = (level.random.nextDouble() - 0.5) * 4.0;
            double ySpeed = 0.5 + level.random.nextDouble() * 0.3;

            level.sendParticles(
                    ModExplosionParticles.MUSHROOM_SMOKE.get(),
                    x + offsetX, y, z + offsetZ,
                    1,
                    offsetX * 0.05, ySpeed, offsetZ * 0.05,
                    1.0
            );
        }

        // Шапка гриба (расширяющееся облако)
        for (int i = 0; i < 120; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            double radius = 5.0 + level.random.nextDouble() * 8.0;
            double offsetX = Math.cos(angle) * radius;
            double offsetZ = Math.sin(angle) * radius;
            double capY = y + 15 + level.random.nextDouble() * 5;

            double xSpeed = Math.cos(angle) * 0.3;
            double ySpeed = -0.1 + level.random.nextDouble() * 0.1;
            double zSpeed = Math.sin(angle) * 0.3;

            level.sendParticles(
                    ModExplosionParticles.MUSHROOM_SMOKE.get(),
                    x + offsetX, capY, z + offsetZ,
                    1,
                    xSpeed, ySpeed, zSpeed,
                    1.0
            );
        }
    }
}