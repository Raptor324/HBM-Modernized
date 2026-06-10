package com.hbm_m.item.special;

import com.hbm_m.config.ModClothConfig;
import com.hbm_m.entity.ModEntities;
import com.hbm_m.entity.effect.BlackHoleEntity;
import com.hbm_m.item.ModItems;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Опасные предметы при падении на землю (порт {@code com.hbm.item.special.ItemDrop}).
 */
public class ItemDrop extends Item {

    public ItemDrop(Properties properties) {
        super(properties);
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity itemEntity) {
        Level level = itemEntity.level();
        if (level.isClientSide) {
            return false;
        }

        if (!itemEntity.onGround()) {
            return false;
        }

        ModClothConfig config = AutoConfig.getConfigHolder(ModClothConfig.class).getConfig();

        if (stack.is(ModItems.BLACK_HOLE.get()) && config.dropSingularity) {
            BlackHoleEntity hole = new BlackHoleEntity(ModEntities.BLACK_HOLE.get(), level);
            hole.setPos(itemEntity.getX(), itemEntity.getY(), itemEntity.getZ());
            hole.setSize(1.5F);
            level.addFreshEntity(hole);
            itemEntity.discard();
            return true;
        }

        if (stack.is(ModItems.PELLET_ANTIMATTER.get()) && config.dropCell) {
            level.explode(null, itemEntity.getX(), itemEntity.getY(), itemEntity.getZ(),
                    20.0F, true, Level.ExplosionInteraction.BLOCK);
            itemEntity.discard();
            return true;
        }

        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);

        if (stack.is(ModItems.PELLET_ANTIMATTER.get())) {
            tooltip.add(Component.literal("Very heavy antimatter cluster."));
            tooltip.add(Component.literal("Gets rid of black holes."));
        }
        if (stack.is(ModItems.BLACK_HOLE.get())) {
            tooltip.add(Component.literal("Contains a regular singularity"));
            tooltip.add(Component.literal("in the center. Large enough to"));
            tooltip.add(Component.literal("stay stable. It's not the end"));
            tooltip.add(Component.literal("of the world as we know it,"));
            tooltip.add(Component.literal("and I don't feel fine."));
        }

        tooltip.add(Component.translatable("trait.drop").withStyle(ChatFormatting.RED));
    }
}
