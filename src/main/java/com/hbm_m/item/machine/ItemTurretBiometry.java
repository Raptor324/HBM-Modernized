package com.hbm_m.item.machine;

import com.hbm_m.platform.StackNbt;

import java.util.Arrays;
import java.util.List;

import javax.annotation.Nullable;

import com.hbm_m.item.ITooltipProvider;
import com.hbm_m.sound.ModSounds;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * 1:1 {@code com.hbm.items.machine.ItemTurretBiometry} (Basis von {@code turret_chip}): Rechtsklick traegt den
 * eigenen Namen ein ("playercount", "player_&lt;n&gt;"); die Geschuetze lesen die Liste als Freundesliste.
 */
public class ItemTurretBiometry extends Item implements ITooltipProvider {

    public ItemTurretBiometry(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHbmTooltip(ItemStack itemstack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
        String[] names = getNames(itemstack);
        if (names != null)
            for (String name : names)
                list.add(Component.literal(name));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        addName(stack, player.getGameProfile().getName());

        if (world.isClientSide)
            player.sendSystemMessage(Component.literal("Added player data!"));

        world.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.TOOL_TECH_BLEEP.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        player.swing(hand);
        return InteractionResultHolder.success(stack);
    }

    @Nullable
    public static String[] getNames(ItemStack stack) {
        if (!StackNbt.has(stack)) {
            return null;
        }

        CompoundTag tag = StackNbt.read(stack);
        String[] names = new String[tag.getInt("playercount")];

        for (int i = 0; i < names.length; i++) {
            names[i] = tag.getString("player_" + i);
        }

        if (names.length == 0)
            return null;

        return names;
    }

    public static void addName(ItemStack stack, String s) {
        CompoundTag tag = StackNbt.orCreate(stack);

        String[] names = getNames(stack);
        int count = 0;

        if (names != null && Arrays.asList(names).contains(s))
            return;

        if (names != null)
            count = names.length;

        tag.putInt("playercount", count + 1);
        tag.putString("player_" + count, s);
    }

    public static void clearNames(ItemStack stack) {
        StackNbt.orCreate(stack).putInt("playercount", 0);
    }
}
