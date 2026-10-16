package com.hbm_m.item.food;

import com.hbm_m.advancement.ModAdvancements;
import com.hbm_m.extprop.HbmLivingProps;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.NotNull;

/**
 * 1:1 port of the coffee branch of {@code ItemEnergy}.
 *
 * <p>Plain coffee heals ten and grants Speed II for a minute. The radium variant does the same and
 * then hands the drinker 500 RAD for the privilege - which is the joke, and the reason it carries
 * its own advancement.</p>
 */
public class ItemCoffee extends Item {

    /** Nutrition is zero on both: the original heals directly rather than feeding. */
    public static final FoodProperties COFFEE = new FoodProperties.Builder()
            .nutrition(0).saturationMod(0F).alwaysEat().build();

    private final boolean radium;

    public ItemCoffee(boolean radium, Properties properties) {
        super(properties.food(COFFEE));
        this.radium = radium;
    }

    @Override
    public @NotNull UseAnim getUseAnimation(@NotNull ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public @NotNull ItemStack finishUsingItem(@NotNull ItemStack stack, @NotNull Level level,
                                              @NotNull LivingEntity entity) {
        ItemStack result = super.finishUsingItem(stack, level, entity);

        if (!level.isClientSide && entity instanceof Player player) {
            player.heal(10F);
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60 * 20, 2));

            if (radium) {
                HbmLivingProps.incrementRadiation(player, 500F);
                ModAdvancements.grant(player, ModAdvancements.RADIUM);
            }
        }

        return result;
    }
}
