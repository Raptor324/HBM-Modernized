package com.hbm_m.block.custom.explosives;

import com.hbm_m.particle.ModExplosionParticles;
import com.hbm_m.particle.explosions.basic.ExplosionParticleUtils;
import com.hbm_m.util.explosions.freaky.MessGenerator;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
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