package com.hbm_m.client.render.machine;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Анимация части модели станка. Единственное, что пишет разработчик конкретной машины.
 * <p>
 * Контракт: {@code pose} уже несёт блочный трансформ (центр блока + поворот по facing)
 * и УЖЕ спушен движком — просто делайте translate/rotate/scale относительно блока
 * и НЕ вызывайте push/pop: движок сам снимет итоговую матрицу и откатит стек после возврата.
 *
 * @return {@code true} — часть рисуется в этом кадре; {@code false} — пропустить
 *         (например, анимационные данные ещё не готовы).
 */
@FunctionalInterface
public interface PartAnimator<T extends BlockEntity> {
    boolean animate(T blockEntity, float partialTick, long gameTime, PoseStack pose);
}
