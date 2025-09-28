package com.hbm_m.client;
// Обработчик частиц для блоков с тегом "emit_dark_particles".
// Каждые несколько тиков выбирает случайные блоки в радиусе вокруг игрока,
// и если блок имеет нужный тег, спавнит частицы в соседних воздушных блоках. (НЕ работает)

import com.hbm_m.block.HBMBlockTags;
import com.hbm_m.particle.ModParticleTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.hbm_m.lib.RefStrings;

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

            if (level.getGameTime() % 4 != 0) {
                return;
            }

            BlockPos playerPos = mc.player.blockPosition();
            RandomSource random = level.random;
            int radius = 16;
            
            for (int i = 0; i < 2; ++i) {
                BlockPos randomPos = playerPos.offset(
                    random.nextIntBetweenInclusive(-radius, radius),
                    random.nextIntBetweenInclusive(-radius, radius),
                    random.nextIntBetweenInclusive(-radius, radius)
                );

                BlockState state = level.getBlockState(randomPos);
                
                // 1. Проверяем, имеет ли блок искомый тег.
                if (state.is(HBMBlockTags.EMIT_DARK_PARTICLES)) {
                    
                    // 2. Итерируем по всем 6 направлениям (вверх, вниз, север, юг, запад, восток).
                    for (Direction direction : Direction.values()) {
                        
                        // 3. Получаем позицию соседнего блока.
                        BlockPos adjacentPos = randomPos.relative(direction);
                        
                        // 4. Проверяем, является ли соседний блок воздухом.
                        if (level.getBlockState(adjacentPos).isAir()) {
                            
                            // 5. Спавним частицу ВНУТРИ этого воздушного блока.
                            double px = adjacentPos.getX() + random.nextDouble();
                            double py = adjacentPos.getY() + random.nextDouble();
                            double pz = adjacentPos.getZ() + random.nextDouble();
                            level.addParticle(ModParticleTypes.DARK_PARTICLE.get(), px, py, pz, 1, 1, 1);
                        }
                    }
                }
            }
        }
    }
}