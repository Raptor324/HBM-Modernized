package com.hbm_m.armormod.item;

import java.util.List;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.advancement.ModAdvancements;
import com.hbm_m.armormod.util.ArmorModificationHelper;
import com.hbm_m.network.AuxParticlePacket;
import com.hbm_m.particle.helper.IParticleCreator;
import com.hbm_m.sound.HbmSoundsNT;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import com.hbm_m.platform.AttributeHooks;

/**
 * 1:1 port of {@code ItemModKnife} - the injector knife.
 *
 * <p>An "extra" armour mod that does exactly one thing: every fifty ticks it takes two hearts off
 * your maximum health, permanently, until you are down to a single heart and cannot be reduced
 * further. Reaching that floor is the original's {@code achSomeWounds}, which is the joke - the
 * advancement is called "Some Wounds Never Heal" and you earn it by ruining yourself.</p>
 */
public class ItemModKnife extends ItemArmorMod {

    /** The original's fixed modifier UUID, so repeated applications replace rather than stack. */
    private static final UUID TRIGAMMA_UUID = UUID.fromString("86d44ca9-44f1-4ca6-bdbb-d9d33bead251");

    private static final int INTERVAL = 50;
    /** Below this the knife has nothing left to take. */
    private static final float FLOOR = 2F;

    public ItemModKnife(Properties properties) {
        super(properties.stacksTo(1), ArmorModificationHelper.extra, false, true, false, false);
    }

    @Override
    public List<Component> getEffectTooltipLines() {
        return List.of(
                Component.literal("Pain.").withStyle(ChatFormatting.RED),
                Component.empty(),
                Component.literal("Hurts, doesn't it?").withStyle(ChatFormatting.RED));
    }

    @Override
    public void addDesc(List<Component> list, ItemStack stack, ItemStack armor) {
        list.add(descLine(ChatFormatting.RED, stack, ""));
    }

    @Override
    public void modUpdate(LivingEntity entity, ItemStack armor) {
        if (entity.level().isClientSide) return;
        if (entity.tickCount % INTERVAL != 0) return;
        if (entity.getMaxHealth() <= FLOOR) return;

        AttributeInstance maxHealth = entity.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth == null) return;

        entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                HbmSoundsNT.get("hbm:entity.slicer"), SoundSource.PLAYERS, 1.0F, 1.0F);

        if (entity.level() instanceof ServerLevel server) {
            CompoundTag nbt = new CompoundTag();
            nbt.putString("type", "bloodvomit");
            nbt.putInt("entity", entity.getId());
            IParticleCreator.sendPacket(server, entity.getX(), entity.getY(), entity.getZ(), 25, nbt);
        }

        // The original reads the current max, drops the old modifier, and applies a new one two
        // points lower - so the loss accumulates across the whole modifier rather than stacking
        // separate ones.
        //? if < 1.21.1 {
        AttributeModifier existing = maxHealth.getModifier(TRIGAMMA_UUID);
        double previous = existing != null ? existing.getAmount() : 0D;
        if (existing != null) maxHealth.removeModifier(TRIGAMMA_UUID);
        maxHealth.addPermanentModifier(com.hbm_m.platform.AttributeHooks.modifier(
                TRIGAMMA_UUID, "digamma", previous - 2D, AttributeModifier.Operation.ADDITION));
        //?} else {
        /*// 1.21 identifiziert Modifikatoren per ResourceLocation; dieselbe Ableitung aus der UUID
        // wie in PlatformHooks.attributeModifier, damit der Schluessel stabil bleibt.
        net.minecraft.resources.ResourceLocation trigammaId =
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                        com.hbm_m.lib.RefStrings.MODID, "am_" + TRIGAMMA_UUID.toString().replace('-', '_'));
        AttributeModifier existing = maxHealth.getModifier(trigammaId);
        double previous = existing != null ? existing.amount() : 0D;
        if (existing != null) maxHealth.removeModifier(trigammaId);
        maxHealth.addPermanentModifier(com.hbm_m.platform.AttributeHooks.modifier(
                TRIGAMMA_UUID, "digamma", previous - 2D, AttributeModifier.Operation.ADD_VALUE));
        *///?}

        // Clamp so the wearer cannot be knifed into negative health.
        if (entity.getHealth() > entity.getMaxHealth()) entity.setHealth(entity.getMaxHealth());

        if (entity instanceof ServerPlayer player) {
            CompoundTag data = new CompoundTag();
            data.putString("type", "properJolt");

            if (entity.getMaxHealth() > FLOOR) {
                data.putInt("time", 10000 + entity.getRandom().nextInt(10000));
                data.putInt("maxTime", 10000);
            } else {
                data.putInt("time", 0);
                data.putInt("maxTime", 0);
                ModAdvancements.grant(player, ModAdvancements.SOME_WOUNDS);
            }

            AuxParticlePacket.sendTo(player, data, 0, 0, 0);
        }
    }
}
