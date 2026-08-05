package com.hbm_m.item.tool;

import java.util.List;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.platform.PlatformHooks;
import com.hbm_m.util.confetti.ConfettiUtil;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * Тестовый предмет для проверки эффектов {@link ConfettiUtil}.
 *
 * <p>ПКМ — перебор доступных эффектов (сейчас только скелетонизация, список расширяем).
 * ЛКМ по сущности — ваншот удар с применением выбранного эффекта к трупу.</p>
 */
public class ConfettiTesterItem extends Item {

    private static final String NBT_EFFECT = "ConfettiEffect";

    /**
     * Расширяемый список тестируемых эффектов.
     * Сюда добавлять новые эффекты по мере портирования ConfettiUtil
     * (pulverize, gib и т.д.).
     */
    private static final EffectEntry[] EFFECTS = {
            new EffectEntry("Skeletonize (Cremate)", ConfettiUtil::cremate),
            new EffectEntry("Skeletonize (Pulverize)", ConfettiUtil::pulverize),
    };

    public ConfettiTesterItem() {
        super(new Item.Properties().stacksTo(1));
    }

    private static int getEffectIndex(ItemStack stack) {
        CompoundTag tag = PlatformHooks.getItemTag(stack);
        if (tag == null || !tag.contains(NBT_EFFECT)) return 0;
        int index = tag.getInt(NBT_EFFECT);
        return Math.floorMod(index, EFFECTS.length);
    }

    private static void setEffectIndex(ItemStack stack, int index) {
        PlatformHooks.editItemTag(stack, t -> t.putInt(NBT_EFFECT, Math.floorMod(index, EFFECTS.length)));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        int next = getEffectIndex(stack) + 1;
        setEffectIndex(stack, next);

        EffectEntry effect = EFFECTS[getEffectIndex(stack)];
        level.playSound(player, player.getX(), player.getY(), player.getZ(),
                SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0F, 1.0F);

        if (!level.isClientSide) {
            player.displayClientMessage(
                    Component.literal("[Confetti Tester] Effect: " + effect.name)
                            .withStyle(ChatFormatting.GREEN), true);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    /**
     * ЛКМ по сущности (сервер): мгновенное убийство + выбранный эффект.
     */
    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        Level level = target.level();
        if (!level.isClientSide) {
            EffectEntry effect = EFFECTS[getEffectIndex(stack)];
            // Убить гарантированно, даже если у цели абсорбция/броня
            target.hurt(target.damageSources().generic(), Float.MAX_VALUE);
            if (target.isAlive()) {
                target.kill();
            }
            // Эффект применить к трупу. Он не зависит от типа урона.
            effect.action.accept(target);
        }
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        EffectEntry effect = EFFECTS[getEffectIndex(stack)];
        tooltip.add(Component.literal("Effect: " + effect.name).withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal("RMB: cycle effect").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        tooltip.add(Component.literal("LMB: kill + apply").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }

    private record EffectEntry(String name, Consumer<LivingEntity> action) {}
}
