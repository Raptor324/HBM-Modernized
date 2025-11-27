package com.hbm_m.item.fekal_grenades; // Замените на ваш пакет

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

import static com.hbm_m.entity.grenades.GrenadeIfType.GRENADE_IF;


public class GrenadeIfItem extends Item {

    private final GrenadeIfType grenadeType;

    public GrenadeIfItem(Properties properties, GrenadeIfType grenadeIf, RegistryObject<EntityType<GrenadeIfProjectileEntity>> grenadeType) {
        super(properties);
        this.grenadeType = grenadeIf;

    }
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);

        //  Универсальная подсказка на основе типа гранаты
        String behaviorText;

        switch (grenadeType) {
            case GRENADE_IF_HE -> behaviorText = "Мощный взрыв";
            case GRENADE_IF_SLIME -> behaviorText = "Лучше отскакивает";
            case GRENADE_IF -> behaviorText = "Стандартный взрыв";
            case GRENADE_IF_FIRE -> behaviorText = "Распространяет огонь после детонации";
            default -> behaviorText = "Граната с таймером";
        }

        // БЕЗ пробела в начале и пустых строк
        tooltip.add(Component.literal(behaviorText)
                .withStyle(ChatFormatting.YELLOW));

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