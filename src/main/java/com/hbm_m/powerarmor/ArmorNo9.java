package com.hbm_m.powerarmor;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.armormod.item.ItemArmorMod;
import com.hbm_m.extprop.HbmLivingProps;
import com.hbm_m.extprop.HbmPlayerProps;
import com.hbm_m.item.ITooltipProvider;
import com.hbm_m.item.tools_and_armor.ModArmorMaterials;
import com.hbm_m.powerarmor.resist.DamageResistanceHandler;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * 1:1 {@code com.hbm.items.armor.ArmorNo9}: Grubenhelm, +0,5 DT, haelt die Staublunge unter 90 % und baut
 * sie oberhalb von 25 % ab; die Lampe folgt dem HUD-Schalter (NBT "isOn").
 */
public class ArmorNo9 extends ArmorModel implements IAttackHandler, IDamageHandler, ITooltipProvider {

    public ArmorNo9(ModArmorMaterials material, Type type, Properties properties) {
        super(material, type, properties);
    }

    //? if !fabric {
    @Override
    public boolean isDamageable(ItemStack stack) {
        return false;
    }

    @Override
    public int getMaxDamage(ItemStack stack) {
        return 0;
    }
    //?}

    @Override
    public void appendHbmTooltip(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
        list.add(Component.literal("+0.5 DT").withStyle(ChatFormatting.BLUE));
        list.add(Component.literal("Lets you breathe coal, neat!").withStyle(ChatFormatting.YELLOW));
    }

    @Override
    public void handleDamage(LivingEntity entity, ItemArmorMod.Hurt event, ItemStack stack) {

        if (DamageResistanceHandler.isUnblockable(event.source))
            return;

        event.amount -= 0.5F;

        if (event.amount < 0)
            event.amount = 0;
    }

    @Override
    public void handleAttack(LivingEntity entity, ItemArmorMod.Hurt event, ItemStack armor) {

        if (DamageResistanceHandler.isUnblockable(event.source))
            return;

        if (event.amount <= 0.5F) {
            event.canceled = true;
        }
    }

    //? if forge {
    @Override
    @SuppressWarnings("removal")
    //?}
    // NeoForge: Aufruf ueber ArmorTickNeoForge
    public void onArmorTick(@NotNull ItemStack armor, @NotNull Level world, @NotNull Player player) {

        if (!world.isClientSide) {

            boolean turnOn = HbmPlayerProps.getData(player).enableHUD;
            boolean wasOn = com.hbm_m.platform.StackNbt.orCreate(armor).getBoolean("isOn");

            if (turnOn && !wasOn) world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FLINTANDSTEEL_USE, SoundSource.PLAYERS, 1F, 1.5F);
            if (!turnOn && wasOn) world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.5F, 2F);
            com.hbm_m.platform.StackNbt.orCreate(armor).putBoolean("isOn", turnOn); // a crude way of syncing the "enableHUD" prop to other players is just by piggybacking off the NBT sync

            if (HbmLivingProps.getBlackLung(player) > HbmLivingProps.maxBlackLung * 0.9) {
                HbmLivingProps.setBlackLung(player, (int) (HbmLivingProps.maxBlackLung * 0.9));
            }
            if (HbmLivingProps.getBlackLung(player) >= HbmLivingProps.maxBlackLung * 0.25) {
                HbmLivingProps.setBlackLung(player, HbmLivingProps.getBlackLung(player) - 1);
            }
        }
    }
}
