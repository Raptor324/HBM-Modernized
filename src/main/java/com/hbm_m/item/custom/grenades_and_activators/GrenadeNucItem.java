package com.hbm_m.item.custom.grenades_and_activators;


import java.util.List;
import com.hbm_m.entity.grenades.GrenadeNucProjectileEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.RegistryObject;

public class GrenadeNucItem extends Item {

    public GrenadeNucItem(Properties properties, RegistryObject<? extends EntityType<?>> grenadeNucProjectile) {
        super(properties);
    }
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, @Nullable List<Component> tooltip, TooltipFlag flag) {
        if (tooltip == null) return;

        // 1. Тёмно-красный
        tooltip.add(Component.translatable("tooltip.hbm_m.grenade_nuc.line1")
                .withStyle(ChatFormatting.DARK_RED));

        // 2. Красный
        tooltip.add(Component.translatable("tooltip.hbm_m.grenade_nuc.line2")
                .withStyle(ChatFormatting.RED));

        // 3. Серый
        tooltip.add(Component.translatable("tooltip.hbm_m.grenade_nuc.line3")
                .withStyle(ChatFormatting.GRAY));
    }



    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);

        // Звук броска чуть ниже тоном (тяжелый предмет) с повышенной громкостью
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.SNOWBALL_THROW, SoundSource.NEUTRAL,
                1.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));

        if (!level.isClientSide) {
            GrenadeNucProjectileEntity grenade = new GrenadeNucProjectileEntity(level, player);
            grenade.setItem(itemstack);

            // Бросаем чуть слабее (скорость 1.2F вместо 1.5F), так как это тяжелая ядерная граната
            grenade.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.2F, 1.0F);
            level.addFreshEntity(grenade);

            player.awardStat(Stats.ITEM_USED.get(this));
            if (!player.getAbilities().instabuild) {
                itemstack.shrink(1);
            }
        }

        return InteractionResultHolder.sidedSuccess(itemstack, level.isClientSide());
    }

}