package com.hbm_m.item.special;

import java.util.List;

import com.hbm_m.extprop.HbmLivingProps;
import com.hbm_m.item.ITooltipProvider;
import com.hbm_m.particle.helper.IParticleCreator;
import com.hbm_m.sound.ModSounds;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Порт {@link com.hbm.items.special.ItemCigarette} (1.7.10).
 * Затяжка (ПКМ, 30 тиков): +2000 black lung, +2000 asbestos, +100 RAD (шутка про Po-210),
 * звук кашля и облако дыма.
 */
public class ItemCigarette extends Item implements ITooltipProvider {

    public ItemCigarette(Properties properties) {
        super(properties);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    //? if < 1.21.1 {
    @Override
    public int getUseDuration(ItemStack stack) {
        return 30;
    }
    //?} else {
    /*@Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 30;
    }*///?}

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        stack.shrink(1);

        if (!level.isClientSide()) {
            HbmLivingProps.incrementBlackLung(entity, 2000);
            HbmLivingProps.incrementAsbestos(entity, 2000);
            HbmLivingProps.incrementRadiation(entity, 100F);

            level.playSound(
                    null,
                    entity.getX(),
                    entity.getY(),
                    entity.getZ(),
                    ModSounds.PLAYER_COUGH.get(),
                    SoundSource.PLAYERS,
                    1.0F,
                    1.0F);

            if (level instanceof ServerLevel serverLevel) {
                CompoundTag nbt = new CompoundTag();
                nbt.putString("type", "vomit");
                nbt.putString("mode", "smoke");
                nbt.putInt("count", 30);
                nbt.putInt("entity", entity.getId());
                IParticleCreator.sendPacket(serverLevel, entity.getX(), entity.getY(), entity.getZ(), 25, nbt);
            }
        }

        return stack;
    }

    @Override
    public void appendHbmTooltip(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.hbm_m.cigarette.line1").withStyle(ChatFormatting.RED));
        tooltip.add(Component.translatable("tooltip.hbm_m.cigarette.line2").withStyle(ChatFormatting.RED));
        tooltip.add(Component.translatable("tooltip.hbm_m.cigarette.line3").withStyle(ChatFormatting.RED));
        tooltip.add(Component.translatable("tooltip.hbm_m.cigarette.line4").withStyle(ChatFormatting.RED));
    }
}
