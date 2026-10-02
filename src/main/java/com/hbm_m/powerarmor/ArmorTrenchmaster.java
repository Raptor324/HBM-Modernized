package com.hbm_m.powerarmor;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.armormod.item.ItemArmorMod;
import com.hbm_m.armormod.util.ArmorModificationHelper;
import com.hbm_m.extprop.HbmPlayerProps;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.tools_and_armor.ModArmorMaterials;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** 1:1 {@code com.hbm.items.armor.ArmorTrenchmaster}. */
public class ArmorTrenchmaster extends ModArmorFSB {

    public ArmorTrenchmaster(ModArmorMaterials material, Type type, Properties properties, String texture) {
        super(material, type, properties, texture);
    }

    @Override
    public boolean isObjArmor() {
        return true;
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
        super.appendHbmTooltip(stack, level, list, flag);

        //list.add(EnumChatFormatting.RED + "  " + I18nUtil.resolveKey("armor.fasterReload"));
        list.add(line(ChatFormatting.RED, "armor.moreAmmo"));
    }

    @Override
    public void handleHurt(LivingEntity e, ItemArmorMod.Hurt event) {
        super.handleHurt(e, event);

        if (e instanceof Player player) {

            if (ModArmorFSB.hasFSBArmor(player)) {

                if (event.source.is(DamageTypeTags.IS_EXPLOSION) && event.source.getDirectEntity() == player) {
                    event.amount = 0;
                    return;
                }
            }
        }
    }

    @Override
    public void handleAttack(LivingEntity e, ItemArmorMod.Hurt event) {
        super.handleAttack(e, event);

        if (e instanceof Player player) {

            if (ModArmorFSB.hasFSBArmor(player)) {

                if (e.getRandom().nextInt(3) == 0) {
                    HbmPlayerProps.plink(player, SoundEvents.ITEM_BREAK, 0.5F, 1.0F + e.getRandom().nextFloat() * 0.5F);
                    event.canceled = true;
                }
            }
        }
    }

    public static boolean isTrenchMaster(Player player) {
        if (player == null) return false;
        ItemStack plate = player.getItemBySlot(EquipmentSlot.CHEST);
        return !plate.isEmpty() && plate.getItem() == ModItems.TRENCHMASTER_PLATE.get() && ModArmorFSB.hasFSBArmor(player);
    }

    public static boolean hasAoS(Player player) {
        if (player == null) return false;
        ItemStack helmetStack = player.getItemBySlot(EquipmentSlot.HEAD);
        if (!helmetStack.isEmpty()) {
            ItemStack[] mods = ArmorModificationHelper.pryMods(helmetStack);
            ItemStack helmet = mods[ArmorModificationHelper.helmet_only];
            return helmet != null && !helmet.isEmpty() && helmet.getItem() == ModItems.CARD_AOS.get();
        }
        return false;
    }
}
