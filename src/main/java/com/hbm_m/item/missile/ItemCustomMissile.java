package com.hbm_m.item.missile;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.item.ModItems;
import com.hbm_m.item.missile.ItemCustomMissilePart.FuelType;
import com.hbm_m.item.missile.ItemCustomMissilePart.WarheadType;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * 1:1 {@code ItemCustomMissile} (missile_custom): fertige Baukasten-Rakete aus der Raketenmontage. Die Teile stehen
 * als Registry-IDs im NBT ({@code chip}, {@code warhead}, {@code fuselage}, {@code stability}, {@code thruster}).
 */
public class ItemCustomMissile extends Item {

    public ItemCustomMissile(Properties props) {
        super(props.stacksTo(1));
    }

    //? if forge {
    @Override
    public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions() {
            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return com.hbm_m.client.render.item.ItemRenderMissileCustom.INSTANCE;
            }
        });
    }
    //?}

    public static ItemStack buildMissile(Item chip, Item warhead, Item fuselage, @Nullable Item stability, Item thruster) {
        return buildMissile(new ItemStack(chip), new ItemStack(warhead), new ItemStack(fuselage), stability == null ? null : new ItemStack(stability), new ItemStack(thruster));
    }

    public static ItemStack buildMissile(ItemStack chip, ItemStack warhead, ItemStack fuselage, @Nullable ItemStack stability, ItemStack thruster) {
        ItemStack missile = new ItemStack(ModItems.MISSILE_CUSTOM.get());
        write(missile, "chip", chip.getItem());
        write(missile, "warhead", warhead.getItem());
        write(missile, "fuselage", fuselage.getItem());
        write(missile, "thruster", thruster.getItem());
        if (stability != null && !stability.isEmpty()) write(missile, "stability", stability.getItem());
        return missile;
    }

    private static void write(ItemStack stack, String key, Item item) {
        stack.getOrCreateTag().putString(key, BuiltInRegistries.ITEM.getKey(item).toString());
    }

    @Nullable
    public static ItemCustomMissilePart read(ItemStack stack, String key) {
        if (!stack.hasTag() || !stack.getTag().contains(key)) return null;
        ResourceLocation rl = ResourceLocation.tryParse(stack.getTag().getString(key));
        if (rl == null) return null;
        return BuiltInRegistries.ITEM.get(rl) instanceof ItemCustomMissilePart p ? p : null;
    }

    private static Component line(String key, Component value) {
        return Component.translatable(key).withStyle(ChatFormatting.BOLD).append(Component.literal(": ").withStyle(ChatFormatting.BOLD))
                .append(value.copy().withStyle(s -> s.withBold(false)));
    }

    private static Component gray(String s) {
        return Component.literal(s).withStyle(ChatFormatting.GRAY);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
        if (!stack.hasTag()) return;

        try {
            ItemCustomMissilePart chip = read(stack, "chip");
            ItemCustomMissilePart warhead = read(stack, "warhead");
            ItemCustomMissilePart fuselage = read(stack, "fuselage");
            ItemCustomMissilePart stability = read(stack, "stability");
            ItemCustomMissilePart thruster = read(stack, "thruster");

            list.add(line("item.missile.desc.warhead", ItemCustomMissilePart.getWarhead((WarheadType) warhead.attributes[0])));
            list.add(line("item.missile.desc.strength", gray("" + warhead.attributes[1])));
            list.add(line("item.missile.desc.fuelType", ItemCustomMissilePart.getFuel((FuelType) fuselage.attributes[0])));
            list.add(line("item.missile.desc.fuelAmount", gray(fuselage.attributes[1] + "l")));
            list.add(line("item.missile.desc.chipInaccuracy", gray((Float) chip.attributes[0] * 100 + "%")));

            if (stability != null) list.add(line("item.missile.desc.finInaccuracy", gray((Float) stability.attributes[0] * 100 + "%")));
            else list.add(line("item.missile.desc.finInaccuracy", gray("100%")));

            list.add(line("item.missile.desc.size", Component.empty().append(ItemCustomMissilePart.getSize(fuselage.top)).append("/").append(ItemCustomMissilePart.getSize(fuselage.bottom))));

            float health = warhead.health + fuselage.health + thruster.health;
            if (stability != null) health += stability.health;
            list.add(line("item.missile.desc.health", gray(health + "HP")));

        } catch (Exception ex) {
            list.add(Component.translatable("error.generic").withStyle(ChatFormatting.RED));
        }
    }

    @Nullable
    public static MissileStruct getStruct(ItemStack stack) {
        if (stack == null || !(stack.getItem() instanceof ItemCustomMissile)) return null;
        return new MissileStruct(read(stack, "warhead"), read(stack, "fuselage"), read(stack, "stability"), read(stack, "thruster"));
    }
}
