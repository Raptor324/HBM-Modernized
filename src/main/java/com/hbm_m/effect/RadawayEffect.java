package com.hbm_m.effect;

// Класс, реализующий эффект "Radaway" для снижения радиации у игрока.
// Эффект полезный (BENEFICIAL) и имеет оранжевый цвет

import java.util.function.Consumer;

import javax.annotation.Nonnull;

import com.hbm_m.lib.RefStrings;
import com.hbm_m.radiation.PlayerHandler;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.extensions.common.IClientMobEffectExtensions;

public class RadawayEffect extends MobEffect {

    // Константа, определяющая, сколько радиации снимается за тик.
    // Можно вынести в конфиг для гибкой настройки.
    // Amplifier 0 -> 0.5 rad/tick. Amplifier 1 -> 1.0 rad/tick.
    private static final float RADAWAY_POWER = 140.0F / 120.0F;

    public RadawayEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    /**
     * Этот метод вызывается каждый игровой тик, пока эффект активен.
     * Здесь мы реализуем нашу основную логику.
     * @param entity Сущность, на которую действует эффект
     * @param amplifier Уровень эффекта (0 для уровня I, 1 для уровня II и т.д.)
     */
    @Override
    public void applyEffectTick(@Nonnull LivingEntity entity, int amplifier) {
        // Убедимся, что это игрок и что мы на серверной стороне
        if (entity instanceof Player && !entity.level().isClientSide()) {
            Player player = (Player) entity;

            // Вызываем ваш метод для уменьшения радиации.
            // (amplifier + 1) чтобы уровень I (amplifier 0) уже имел силу.
            float amountToRemove = (amplifier + 1) * RADAWAY_POWER;
            PlayerHandler.decrementPlayerRads(player, amountToRemove);
        }
    }

    /**
     * Этот метод определяет, должен ли applyEffectTick() вызываться в данный тик.
     * В HBM для radaway было `return true;`, что означает исполнение каждый тик.
     * Мы сделаем так же для максимальной эффективности.
     * @param duration Оставшаяся длительность эффекта
     * @param amplifier Уровень эффекта
     * @return true, если эффект должен сработать в этот тик.
     */
    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }

        @Override
    public void initializeClient(@Nonnull Consumer<IClientMobEffectExtensions> consumer) {
        consumer.accept(new IClientMobEffectExtensions() {
            private static final ResourceLocation POTIONS_SHEET = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/potions.png");
            
            private static final int TEXTURE_WIDTH = 256;
            private static final int TEXTURE_HEIGHT = 256;

            /**
             * Метод для отрисовки иконки в инвентаре.
             * ВАЖНО: x и y здесь - это координаты фона, а не иконки.
             * Нам нужно вручную отцентровать нашу иконку 18x18 внутри фона.
             * Стандартный фон имеет размер около 24x24.
             * TODO: Но иконка эффекта один хуй пока не работает нормально. Похуй, потом доработаю
             */
            @Override
            public boolean renderInventoryIcon(MobEffectInstance instance, EffectRenderingInventoryScreen<?> screen, GuiGraphics guiGraphics, int x, int y, int blitOffset) {
                // Координаты желтого знака радиации на спрайт-шите
                int u = 18;
                int v = 198;
                int iconSize = 18;

                int centeredX = x + 3;
                int centeredY = y + 3;

                // Отрисовка
                guiGraphics.blit(POTIONS_SHEET, centeredX, centeredY, blitOffset, u, v, iconSize, iconSize, TEXTURE_WIDTH, TEXTURE_HEIGHT);

                return true;
            }

            /**
             * Метод для отрисовки иконки на HUD.
             * Здесь проблема та же, но смещение может быть другим.
             * Стандартная иконка на HUD 18x18, а фон 22x22
             */
            @Override
            public boolean renderGuiIcon(MobEffectInstance instance, net.minecraft.client.gui.Gui gui, GuiGraphics guiGraphics, int x, int y, float z, float alpha) {
                // Координаты желтого знака радиации на спрайт-шите
                int u = 18;
                int v = 198;
                int iconSize = 18;

                int centeredX = x + 2;
                int centeredY = y + 2;

                // Устанавливаем цвет и прозрачность. Это помогает сбросить некорректное состояние рендеринга, оставшееся от ванильного кода.
                guiGraphics.setColor(1.0F, 1.0F, 1.0F, alpha);
                
                // Включаем смешивание (blending) для корректной работы прозрачности
                // RenderSystem.enableBlend(); // В 1.20.1 GuiGraphics обычно сам управляет этим, но если проблемы остаются, можно раскомментировать

                guiGraphics.blit(POTIONS_SHEET, centeredX, centeredY, (int) z, u, v, iconSize, iconSize, TEXTURE_WIDTH, TEXTURE_HEIGHT);

                // RenderSystem.disableBlend(); // Не забываем выключить, если включали

                // Сбрасываем цвет обратно в непрозрачный белый
                guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);

                return true; // Мы сами нарисовали иконку
            }
        });
    }
}