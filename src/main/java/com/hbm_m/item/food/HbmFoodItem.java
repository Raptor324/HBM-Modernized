package com.hbm_m.item.food;

import java.util.List;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.platform.PlatformHooks;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * Gemeinsame Basis fuer die {@code ItemFood}-Abkoemmlinge des Originals ({@code ItemLemon},
 * {@code ItemPill}, {@code ItemCottonCandy}, {@code ItemAppleSchrabidium}, {@code ItemSoup} ...).
 *
 * <p>{@code new ItemFood(heal, saturationModifier, wolf)} bildet 1.20 exakt ab: beide rechnen die
 * Saettigung als {@code heal * modifier * 2}. {@code setAlwaysEdible()} = {@code alwaysEat()},
 * {@code setPotionEffect(id, sek, amp, chance)} = {@code effect(...)}. Das Verhalten nach dem Essen
 * ({@code onFoodEaten}) kommt als {@link OnEaten}, der zurueckgegebene Behaelter ({@code ItemSoup}:
 * Schuessel) als {@link #container}. Tooltipzeilen werden wie im Original aus
 * {@code item.<id>.desc} gelesen, mit {@code $} als Zeilentrenner.</p>
 */
public class HbmFoodItem extends Item {

    @FunctionalInterface
    public interface OnEaten {
        void eaten(ItemStack stack, Level level, Player player);
    }

    private final OnEaten onEaten;
    private final int useDuration;
    private final boolean drink;
    @Nullable
    private final Supplier<Item> container;
    private final boolean foil;
    @Nullable
    private final Rarity rarityOverride;
    private final boolean desc;

    protected HbmFoodItem(Builder b) {
        super(b.props.food(b.food()));
        this.onEaten = b.onEaten;
        this.useDuration = b.useDuration;
        this.drink = b.drink;
        this.container = b.container;
        this.foil = b.foil;
        this.rarityOverride = b.rarity;
        this.desc = b.desc;
        //? if >= 1.21.1 {
        /*com.hbm_m.platform.ItemComponentHooks.deferRarity(this, () -> this.rarityOverride);
        *///?}
    }

    public static Builder of(int heal, float saturation, boolean wolf) {
        return new Builder(heal, saturation, wolf);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack result = super.finishUsingItem(stack, level, entity);
        if (entity instanceof Player player) {
            if (onEaten != null) onEaten.eaten(stack, level, player);
            if (container != null && !player.getAbilities().instabuild) {
                // ItemSoup: nach dem Essen die Schuessel zurueck.
                ItemStack c = new ItemStack(container.get());
                if (result.isEmpty()) return c;
                if (!player.getInventory().add(c)) player.drop(c, false);
            }
        }
        return result;
    }

    @Override
    //? if < 1.21.1 {
    public int getUseDuration(ItemStack stack) {
    //?} else {
    /*public int getUseDuration(ItemStack stack, net.minecraft.world.entity.LivingEntity hbmUser) {
    *///?}
        return useDuration;
    }

    @Override
    public net.minecraft.world.item.UseAnim getUseAnimation(ItemStack stack) {
        return drink ? net.minecraft.world.item.UseAnim.DRINK : net.minecraft.world.item.UseAnim.EAT;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return foil || super.isFoil(stack);
    }

    //? if < 1.21.1 {
    @Override
    public Rarity getRarity(ItemStack stack) {
        return rarityOverride != null ? rarityOverride : super.getRarity(stack);
    }
    //?}

    @Override
    //? if < 1.21.1 {
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
    //?} else {
    /*public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext hbmTooltipCtx, List<Component> tooltip, TooltipFlag flag) {
    *///?}
        if (desc) addDescLines(this.getDescriptionId() + ".desc", tooltip);
    }

    /** Original: {@code I18nUtil.resolveKey(unloc)} mit {@code $}-Trennern; nur wenn der Schluessel existiert. */
    public static void addDescLines(String key, List<Component> tooltip) {
        String loc = net.minecraft.locale.Language.getInstance().getOrDefault(key).replace("%%", "%");
        if (loc.equals(key)) return;
        for (String line : loc.split("\\$")) tooltip.add(Component.literal(line).withStyle(ChatFormatting.GRAY));
    }

    public static final class Builder {
        private final int heal;
        private final float saturation;
        private final boolean wolf;
        private boolean alwaysEdible;
        private Item.Properties props = new Item.Properties();
        private OnEaten onEaten;
        private int useDuration = 32;
        private boolean drink;
        private Supplier<Item> container;
        private boolean foil;
        private Rarity rarity;
        private boolean desc = true;
        private MobEffect effect;
        private int effectSeconds, effectAmp;
        private float effectChance;

        private Builder(int heal, float saturation, boolean wolf) {
            this.heal = heal;
            this.saturation = saturation;
            this.wolf = wolf;
        }

        public Builder alwaysEdible() { this.alwaysEdible = true; return this; }
        public Builder props(Item.Properties p) { this.props = p; return this; }
        public Builder stacksTo(int n) { this.props = this.props.stacksTo(n); return this; }
        public Builder onEaten(OnEaten e) { this.onEaten = e; return this; }
        public Builder useDuration(int t) { this.useDuration = t; return this; }
        public Builder drink() { this.drink = true; return this; }
        public Builder container(Supplier<Item> c) { this.container = c; return this; }
        public Builder foil() { this.foil = true; return this; }
        public Builder rarity(Rarity r) { this.rarity = r; return this; }
        public Builder noDesc() { this.desc = false; return this; }

        //? if >= 1.21.1 {
        /*// 1.21.1: Vanilla-MobEffects sind Holder
        public Builder potion(net.minecraft.core.Holder<MobEffect> effect, int seconds, int amplifier, float chance) {
            return potion(effect.value(), seconds, amplifier, chance);
        }
        *///?}
        /** Original {@code setPotionEffect(id, seconds, amplifier, probability)}. */
        public Builder potion(MobEffect effect, int seconds, int amplifier, float chance) {
            this.effect = effect;
            this.effectSeconds = seconds;
            this.effectAmp = amplifier;
            this.effectChance = chance;
            return this;
        }

        FoodProperties food() {
            FoodProperties.Builder b = PlatformHooks.foodBuilder(heal, saturation);
            if (wolf) b = PlatformHooks.setMeat(b);
            if (alwaysEdible) {
                //? if < 1.21.1 {
                b = b.alwaysEat();
                //?} else {
                /*b = b.alwaysEdible();
                *///?}
            }
            if (effect != null) {
                MobEffect e = effect;
                int t = effectSeconds * 20;
                int a = effectAmp;
                //? if < 1.21.1 {
                b = b.effect(() -> new MobEffectInstance(e, t, a), effectChance);
                //?} else {
                /*b = b.effect(() -> new MobEffectInstance(net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.wrapAsHolder(e), t, a), effectChance);
                *///?}
            }
            return b.build();
        }

        public HbmFoodItem build() {
            return new HbmFoodItem(this);
        }
    }
}
