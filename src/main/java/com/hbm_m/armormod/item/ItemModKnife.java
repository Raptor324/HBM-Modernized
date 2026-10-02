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

/**
 * 1:1 {@code ItemModKnife} (injector_knife): alle 50 Ticks zwei Punkte maximale Gesundheit weniger,
 * Schnittklang, Blutspucken und der "properJolt"-Bildschirmruck; bei 2 HP Erfolg "Some Wounds".
 */
public class ItemModKnife extends ItemArmorMod {

    public static final UUID trigamma_UUID = UUID.fromString("86d44ca9-44f1-4ca6-bdbb-d9d33bead251");

    public ItemModKnife(Properties properties) {
        super(properties.stacksTo(1), ArmorModificationHelper.extra, false, true, false, false);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable Level level, List<Component> list) {
        list.add(line("Pain.", ChatFormatting.RED));
        list.add(Component.empty());
        list.add(line("Hurts, doesn't it?", ChatFormatting.RED));
        list.add(Component.empty());
    }

    @Override
    public void addDesc(List<Component> list, ItemStack stack, ItemStack armor) {
        list.add(descLine(ChatFormatting.RED, stack, ""));
    }

    @Override
    public void modUpdate(LivingEntity entity, ItemStack armor) {
        if (entity.level().isClientSide) return;

        if (entity.tickCount % 50 == 0 && entity.getMaxHealth() > 2F) {

            entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), HbmSoundsNT.get("hbm:entity.slicer"), SoundSource.PLAYERS, 1.0F, 1.0F);

            if (entity.level() instanceof ServerLevel server) {
                CompoundTag nbt = new CompoundTag();
                nbt.putString("type", "bloodvomit");
                nbt.putInt("entity", entity.getId());
                IParticleCreator.sendPacket(server, entity.getX(), entity.getY(), entity.getZ(), 25, nbt);
            }

            AttributeInstance attributeinstance = entity.getAttribute(Attributes.MAX_HEALTH);
            if (attributeinstance == null) return;

            float health = entity.getMaxHealth();

            //? if < 1.21.1 {
            attributeinstance.removeModifier(trigamma_UUID);
            attributeinstance.addPermanentModifier(com.hbm_m.platform.PlatformHooks.attributeModifier(
                    trigamma_UUID, "digamma", -(entity.getMaxHealth() - health + 2), AttributeModifier.Operation.ADDITION));
            //?} else {
            /*net.minecraft.resources.ResourceLocation trigammaId = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                    com.hbm_m.lib.RefStrings.MODID, "am_" + trigamma_UUID.toString().replace('-', '_'));
            attributeinstance.removeModifier(trigammaId);
            attributeinstance.addPermanentModifier(com.hbm_m.platform.PlatformHooks.attributeModifier(
                    trigamma_UUID, "digamma", -(entity.getMaxHealth() - health + 2), AttributeModifier.Operation.ADD_VALUE));
            *///?}

            if (entity instanceof ServerPlayer player) {
                CompoundTag data = new CompoundTag();
                data.putString("type", "properJolt");

                if (entity.getMaxHealth() > 2F) {
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
}
