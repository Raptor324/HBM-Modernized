package com.hbm_m.item;

import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.hbm_m.block.decorations.TrinketBlock;
import com.hbm_m.block.decorations.TrinketTypes;
import com.hbm_m.block.decorations.TrinketTypes.PlushieType;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * Gegenstand zu {@link TrinketBlock}: der Untertyp des Originals (Item-Schaden) steht als NBT {@code type}.
 * Plueschtiere heissen "%s Plushie" und zeigen ihre Aufschrift ({@code getOverrideDisplayName}/{@code addInformation}).
 */
public class TrinketBlockItem extends BlockItem {

    public TrinketBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    public static int getType(ItemStack stack) {
        return stack.getTag() != null ? Math.abs(stack.getTag().getInt("type")) : 0;
    }

    public static ItemStack make(Item item, int type) {
        ItemStack stack = new ItemStack(item);
        stack.getOrCreateTag().putInt("type", type);
        return stack;
    }

    private TrinketBlock.Kind kind() {
        return ((TrinketBlock) getBlock()).kind;
    }

    @Override
    public Component getName(ItemStack stack) {
        if (kind() == TrinketBlock.Kind.PLUSHIE) {
            PlushieType type = TrinketTypes.safe(PlushieType.class, getType(stack));
            return Component.translatable(getDescriptionId(stack), type == PlushieType.NONE ? "" : type.label);
        }
        return super.getName(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (kind() == TrinketBlock.Kind.PLUSHIE) {
            PlushieType type = TrinketTypes.safe(PlushieType.class, getType(stack));
            if (type.inscription != null) tooltip.add(Component.literal(type.inscription));
        }
    }

    //? if forge {
    @Override
    public void initializeClient(Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions() {
            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return com.hbm_m.client.render.item.TrinketItemRenderer.instance();
            }
        });
    }
    //?}
}
