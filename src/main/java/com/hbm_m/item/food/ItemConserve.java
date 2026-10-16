package com.hbm_m.item.food;

// Для того чтобы после употребления консервы в инвентаре появлялась пустая банка,
// нужно переопределить finishUsingItem (порт {@code com.hbm.items.food.ItemConserve}).

import com.hbm_m.item.ITooltipProvider;
import com.hbm_m.entity.ModEntities;
import com.hbm_m.entity.effect.VortexEntity;
import com.hbm_m.item.ModItems;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ItemConserve extends Item implements ITooltipProvider {

    public ItemConserve(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide) {
            onFoodEaten(stack, level, entity);
        }

        ItemStack result = super.finishUsingItem(stack, level, entity);

        if (entity instanceof Player player && !player.isCreative()) {
            ItemStack emptyCan = new ItemStack(ModItems.CAN_KEY.get());
            if (result.isEmpty()) {
                return emptyCan;
            }
            if (!player.getInventory().add(emptyCan)) {
                player.drop(emptyCan, false);
            }
        }

        return result;
    }

    /** Порт {@code ItemConserve#onFoodEaten} — особые эффекты отдельных консервов. */
    protected void onFoodEaten(ItemStack stack, Level level, LivingEntity entity) {
        if (stack.is(ModItems.CANNED_BHOLE.get())) {
            VortexEntity vortex = new VortexEntity(ModEntities.VORTEX.get(), level);
            vortex.setPos(entity.getX(), entity.getY(), entity.getZ());
            vortex.setSize(0.5F);
            vortex.setShrinkRate(0.01F);
            vortex.noBreak();
            level.addFreshEntity(vortex);
        }
    }

    @Override
    public void appendHbmTooltip(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (stack.is(ModItems.CANNED_BHOLE.get())) {
            tooltip.add(Component.translatable("item.hbm_m.canned_bhole.desc").withStyle(ChatFormatting.GRAY));
        }
    }
}
