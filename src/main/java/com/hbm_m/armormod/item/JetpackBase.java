package com.hbm_m.armormod.item;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.armormod.util.ArmorModificationHelper;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * 1:1 {@code com.hbm.items.armor.JetpackBase}: Rueckenmodul, das allein im Brustslot getragen oder als
 * Brustplatten-Mod eingebaut werden kann (dann tickt es ueber {@link #modUpdate}).
 */
public abstract class JetpackBase extends ItemArmorMod implements Equipable {

    public JetpackBase(Properties properties) {
        super(properties.stacksTo(1), ArmorModificationHelper.plate_only, false, true, false, false);
    }

    @Override
    public void appendHbmTooltip(ItemStack itemstack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
        super.appendHbmTooltip(itemstack, level, list, flag);
        list.add(Component.literal("Can be worn on its own!").withStyle(ChatFormatting.GOLD));
    }

    @Override
    public void addDesc(List<Component> list, ItemStack stack, ItemStack armor) {
        ItemStack jetpack = ArmorModificationHelper.pryMods(armor)[ArmorModificationHelper.plate_only];

        if (jetpack == null || jetpack.isEmpty())
            return;

        list.add(Component.literal("  ").append(stack.getHoverName()).withStyle(ChatFormatting.RED));
    }

    @Override
    public void modUpdate(LivingEntity entity, ItemStack armor) {

        if (!(entity instanceof Player player))
            return;

        ItemStack jetpack = ArmorModificationHelper.pryMods(armor)[ArmorModificationHelper.plate_only];

        if (jetpack == null || jetpack.isEmpty())
            return;

        jetpackTick(entity.level(), player, jetpack);
        ArmorModificationHelper.applyMod(armor, jetpack);
    }

    /** Original onArmorTick (allein getragen oder als Mod). */
    public abstract void jetpackTick(Level world, Player player, ItemStack stack);

    //? if forge {
    @Override
    @SuppressWarnings("removal")
    public void onArmorTick(@NotNull ItemStack stack, @NotNull Level world, @NotNull Player player) {
        jetpackTick(world, player, stack);
    }
    //?}

    /** Original isValidArmor: nur der Brustslot. */
    @Override
    public @NotNull EquipmentSlot getEquipmentSlot() {
        return EquipmentSlot.CHEST;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        return this.swapWithEquipmentSlot(this, level, player, hand);
    }

    /** Textur des Rueckenmodells (Original getArmorTexture). */
    public abstract String getModelTexture();
}
