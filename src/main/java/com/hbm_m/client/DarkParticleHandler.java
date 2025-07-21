package com.hbm_m.client;

import com.hbm_m.block.HBMBlockTags;
import com.hbm_m.particle.ModParticleTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.hbm_m.lib.RefStrings;

// ПОДПИСЫВАЕМСЯ НА FORGE BUS!
@Mod.EventBusSubscriber(modid = RefStrings.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class DarkParticleHandler {

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            Minecraft mc = Minecraft.getInstance();
            Level level = mc.level;
            if (level == null || mc.player == null || mc.isPaused()) {
                return;
            }

            // Оптимизация: выполняем не каждый тик
            if (level.getGameTime() % 4 != 0) {
                return;
            }

            BlockPos playerPos = mc.player.blockPosition();
            RandomSource random = level.random;
            int radius = 16;
            
            // Ищем блоки с тегом в радиусе
            for (int i = 0; i < 2; ++i) { // Проверяем 2 случайных блока за раз
                BlockPos randomPos = playerPos.offset(
                    random.nextIntBetweenInclusive(-radius, radius),
                    random.nextIntBetweenInclusive(-radius, radius),
                    random.nextIntBetweenInclusive(-radius, radius)
                );

                BlockState state = level.getBlockState(randomPos);
                if (state.is(HBMBlockTags.EMIT_DARK_PARTICLES)) {
                    // Спавним частицу в случайном месте внутри блока
                    double px = randomPos.getX() + random.nextDouble();
                    double py = randomPos.getY() + random.nextDouble();
                    double pz = randomPos.getZ() + random.nextDouble();
                    level.addParticle(ModParticleTypes.DARK_PARTICLE.get(), px, py, pz, 0, 0, 0);
                }
            }
        }
    }
}