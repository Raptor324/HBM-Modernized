package com.hbm_m.item.custom.grenades_and_activators; // Замените на ваш пакет

import com.hbm_m.entity.grenades.GrenadeIfProjectileEntity;
import com.hbm_m.entity.grenades.GrenadeIfType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
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

import javax.annotation.Nullable;
import java.util.List;


public class GrenadeIfItem extends Item {

    private final GrenadeIfType grenadeType;

    public GrenadeIfItem(Properties properties, GrenadeIfType grenadeIf, RegistryObject<EntityType<GrenadeIfProjectileEntity>> grenadeType) {
        super(properties);
        this.grenadeType = grenadeIf;

    }
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);

        // 1-я строка — общая для всех IF‑гранат
        tooltip.add(Component.translatable("tooltip.hbm_m.grenade_if.common.line1")
                .withStyle(ChatFormatting.YELLOW));

        // 2-я строка — зависит от типа
        String key;
        switch (grenadeType) {
            case GRENADE_IF_HE ->
                    key = "tooltip.hbm_m.grenade_if.he.line2";
            case GRENADE_IF_SLIME ->
                    key = "tooltip.hbm_m.grenade_if.slime.line2";
            case GRENADE_IF_FIRE ->
                    key = "tooltip.hbm_m.grenade_if.fire.line2";
            case GRENADE_IF ->
                    key = "tooltip.hbm_m.grenade_if.standard.line2";
            default ->
                    key = "tooltip.hbm_m.grenade_if.default.line2";
        }

        tooltip.add(Component.translatable(key)
                .withStyle(ChatFormatting.GRAY));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.SNOWBALL_THROW, SoundSource.NEUTRAL,
                0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));

        if (!level.isClientSide) {
            // ModEntities.GRENADE_IF_PROJECTILE.get() - Замените на ваш зарегистрированный EntityType
            GrenadeIfProjectileEntity grenade = new GrenadeIfProjectileEntity(level, player, grenadeType);
            grenade.setItem(itemstack);
            grenade.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.5F, 1.0F);
            level.addFreshEntity(grenade);
        }

        player.awardStat(Stats.ITEM_USED.get(this));
        if (!player.getAbilities().instabuild) {
            itemstack.shrink(1);
        }

        return InteractionResultHolder.sidedSuccess(itemstack, level.isClientSide());
    }

}