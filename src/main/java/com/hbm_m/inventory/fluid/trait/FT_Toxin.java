package com.hbm_m.inventory.fluid.trait;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.Supplier;

import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import com.hbm_m.handler.ArmorRegistry;
import com.hbm_m.handler.HazardClass;
import com.hbm_m.util.ArmorUtil;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.StringUtil;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/**
 * 1:1 {@code FT_Toxin}: Liste von Giftwirkungen (direkter Schaden mit Takt oder Statuseffekte), jeweils mit
 * Schutzklasse fuer die Maske und optional Ganzkoerper-Schutzanzug. Tooltip 1:1: je Eintrag Schutzklasse, DPS bzw.
 * Effekte mit Stufe und Dauer.
 */
public class FT_Toxin extends FluidTrait {

    public final List<ToxinEntry> entries = new ArrayList<>();

    public FT_Toxin addEntry(ToxinEntry entry) {
        entries.add(entry);
        return this;
    }

    @Override
    public void addInfoHidden(List<Component> info) {
        info.add(Component.literal("[").append(Component.translatable("hbmfluid.trait.toxin")).append("]").withStyle(ChatFormatting.LIGHT_PURPLE));
        for (ToxinEntry entry : entries) {
            entry.addInfo(info);
        }
    }

    public void affect(LivingEntity entity, double intensity) {

        for (ToxinEntry entry : entries) {
            entry.poison(entity, intensity);
        }
    }

    @Override
    public void serializeJSON(JsonWriter writer) throws IOException {
        // Tooltip-only: no JSON persistence in Modernized yet
    }

    @Override
    public void deserializeJSON(JsonObject obj) {}

    public abstract static class ToxinEntry {

        public HazardClass clazz;
        public boolean fullBody = false;

        public ToxinEntry(HazardClass clazz, boolean fullBody) {
            this.clazz = clazz;
            this.fullBody = fullBody;
        }

        public boolean isProtected(LivingEntity entity) {

            boolean hasMask = clazz == null;
            boolean hasSuit = !fullBody;

            if (clazz != null && ArmorRegistry.hasProtection(entity, 3, clazz)) {
                ArmorUtil.damageGasMaskFilter(entity, 1);
                hasMask = true;
            }

            if (fullBody && ArmorUtil.checkForHazmat(entity)) {
                hasSuit = true;
            }

            return hasMask && hasSuit;
        }

        public abstract void poison(LivingEntity entity, double intensity);

        public abstract void addInfo(List<Component> info);

        /** "- <Schutzklasse>" + optional " (requires hazmat suit)" in Rot. */
        protected MutableComponent head() {
            MutableComponent c = Component.literal("- ").withStyle(ChatFormatting.YELLOW)
                    .append(Component.translatable(clazz.translationKey).withStyle(ChatFormatting.YELLOW));
            if (fullBody) {
                c.append(Component.literal(" (").withStyle(ChatFormatting.RED)
                        .append(Component.translatable("hbmfluid.trait.hazmat"))
                        .append(")"));
            }
            return c;
        }
    }

    public static class ToxinDirectDamage extends ToxinEntry {

        public Function<Level, DamageSource> damage;
        public float amount;
        public int delay;

        public ToxinDirectDamage(Function<Level, DamageSource> damage, float amount, int delay, HazardClass clazz, boolean fullBody) {
            super(clazz, fullBody);
            this.damage = damage;
            this.amount = amount;
            this.delay = delay;
        }

        @Override
        public void poison(LivingEntity entity, double intensity) {

            if (isProtected(entity)) return;

            if (delay == 0 || entity.level().getGameTime() % delay == 0) {
                entity.hurt(damage.apply(entity.level()), (float) (amount * intensity));
            }
        }

        @Override
        public void addInfo(List<Component> info) {
            info.add(head().append(Component.literal(": " + String.format(Locale.US, "%,.1f", amount * 20 / delay) + " ").withStyle(ChatFormatting.YELLOW))
                    .append(Component.translatable("hbmfluid.trait.perDamage").withStyle(ChatFormatting.YELLOW)));
        }
    }

    public static class ToxinEffects extends ToxinEntry {

        public List<Supplier<MobEffectInstance>> effects = new ArrayList<>();

        public ToxinEffects(HazardClass clazz, boolean fullBody) {
            super(clazz, fullBody);
        }

        @SafeVarargs
        public final ToxinEffects add(Supplier<MobEffectInstance>... effs) {
            for (Supplier<MobEffectInstance> eff : effs) this.effects.add(eff);
            return this;
        }

        @Override
        public void poison(LivingEntity entity, double intensity) {

            if (isProtected(entity)) return;

            for (Supplier<MobEffectInstance> s : effects) {
                MobEffectInstance eff = s.get();
                entity.addEffect(new MobEffectInstance(eff.getEffect(), (int) (eff.getDuration() * intensity), eff.getAmplifier()));
            }
        }

        @Override
        public void addInfo(List<Component> info) {
            info.add(head().append(Component.literal(":").withStyle(ChatFormatting.YELLOW)));
            for (Supplier<MobEffectInstance> s : effects) {
                MobEffectInstance eff = s.get();
                MutableComponent line = Component.literal("   - ").withStyle(ChatFormatting.YELLOW)
                        .append(Component.translatable(eff.getDescriptionId()));
                if (eff.getAmplifier() > 0) {
                    line.append(" ").append(Component.translatable("potion.potency." + eff.getAmplifier()));
                }
                //? if < 1.21.1 {
                line.append(" " + StringUtil.formatTickDuration(eff.getDuration()));
                //?} else {
                /*line.append(" " + StringUtil.formatTickDuration(eff.getDuration(), 20.0F));
                *///?}
                info.add(line);
            }
        }
    }
}
