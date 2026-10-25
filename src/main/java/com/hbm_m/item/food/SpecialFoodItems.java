package com.hbm_m.item.food;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.item.ModItems;
import com.hbm_m.item.special.ModConsumables;
import com.hbm_m.powerarmor.ModArmorFSB;
import com.hbm_m.sound.HbmSoundsNT;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/**
 * Die Nahrungsklassen des Originals mit eigenem Benutzungsverhalten: {@code ItemPill},
 * {@code ItemCanteen}, {@code ItemBDCL}, {@code ItemFlask}, {@code ItemPancake}, {@code ItemPeas}
 * und {@code ItemMuchoMango}.
 */
public final class SpecialFoodItems {

    private SpecialFoodItems() {}

    /** {@code ItemPill}: 10 Ticks, immer essbar, gesperrt waehrend der Unvertraeglichkeit. */
    public static class PillItem extends HbmFoodItem {
        public PillItem() {
            super(HbmFoodItem.of(0, 0.6F, false).alwaysEdible().useDuration(10).onEaten(FoodBehaviors::pill));
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (!ModConsumables.isPotionSick(player)) {
                player.startUsingItem(hand);
                return InteractionResultHolder.consume(stack);
            }
            return InteractionResultHolder.pass(stack);
        }
    }

    /** {@code ItemPancake}: nur mit der BJ-Augenklappe (FSB-Satz) kaubar, sonst "zu weiche Zaehne". */
    public static class PancakeItem extends HbmFoodItem {
        public PancakeItem() {
            super(HbmFoodItem.of(20, 20, false).alwaysEdible().noDesc().onEaten(FoodBehaviors::pancake));
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
            if (ModArmorFSB.hasFSBArmorIgnoreCharge(player) && player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.BJ_HELMET.get())) {
                return super.use(world, player, hand);
            }
            if (!world.isClientSide) player.sendSystemMessage(Component.literal("Your teeth are too soft to eat this.").withStyle(ChatFormatting.YELLOW));
            return InteractionResultHolder.pass(player.getItemInHand(hand));
        }

        @Override
        //? if < 1.21.1 {
        public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
        //?} else {
        /*public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext hbmTooltipCtx, List<Component> list, TooltipFlag flag) {
        *///?}
            list.add(Component.literal("Can be eaten to recharge lunar cybernetic armor"));
            list.add(Component.literal("Not for people with weak molars"));
            list.add(Component.literal(""));
            list.add(Component.literal("Half burnt and smells horrible"));
        }
    }

    /** {@code ItemMuchoMango}: die comically large can - 200 Ticks trinken. */
    public static class MuchoMangoItem extends HbmFoodItem {
        public MuchoMangoItem() {
            super(HbmFoodItem.of(10, 0.6F, true).alwaysEdible().useDuration(200).drink().noDesc().onEaten(FoodBehaviors::muchoMango));
        }

        @Override
        //? if < 1.21.1 {
        public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
        //?} else {
        /*public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext hbmTooltipCtx, List<Component> list, TooltipFlag flag) {
        *///?}
            list.add(Component.literal("The Comically Large Can"));
        }
    }

    /**
     * {@code ItemCanteen}: wiederverwendbar - nach dem Trinken steht der Schaden auf Maximum und
     * sinkt pro Sekunde um eins (Abklingzeit = maxDamage Sekunden).
     */
    public static class CanteenItem extends Item {
        public CanteenItem(int cooldown) {
            super(new Item.Properties().durability(cooldown));
        }

        @Override
        public void inventoryTick(ItemStack stack, Level world, Entity entity, int slot, boolean selected) {
            if (stack.getDamageValue() > 0 && entity.tickCount % 20 == 0) stack.setDamageValue(stack.getDamageValue() - 1);
        }

        @Override
        public ItemStack finishUsingItem(ItemStack stack, Level world, LivingEntity entity) {
            stack.setDamageValue(stack.getMaxDamage());
            if (entity instanceof Player player) {
                if (this == ModItems.CANTEEN_VODKA.get()) {
                    player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 10 * 20, 0));
                    player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 30 * 20, 2));
                }
                ModConsumables.applyPotionSickness(player, 5);
            }
            return stack;
        }

        @Override
        //? if < 1.21.1 {
        public int getUseDuration(ItemStack stack) {
        //?} else {
        /*public int getUseDuration(ItemStack stack, net.minecraft.world.entity.LivingEntity hbmUser) {
        *///?}
            return 10;
        }

        @Override
        public UseAnim getUseAnimation(ItemStack stack) {
            return UseAnim.DRINK;
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.getDamageValue() == 0 && !ModConsumables.isPotionSick(player)) {
                player.startUsingItem(hand);
                return InteractionResultHolder.consume(stack);
            }
            return InteractionResultHolder.pass(stack);
        }

        @Override
        public boolean isBarVisible(ItemStack stack) {
            return stack.getDamageValue() > 0;
        }

        @Override
        //? if < 1.21.1 {
        public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
        //?} else {
        /*public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext hbmTooltipCtx, List<Component> list, TooltipFlag flag) {
        *///?}
            if (this == ModItems.CANTEEN_VODKA.get()) {
                list.add(Component.literal("Cooldown: 3 minutes"));
                list.add(Component.literal("Nausea I for 10 seconds"));
                list.add(Component.literal("Strength III for 30 seconds"));
                list.add(Component.literal(""));
                if (com.hbm_m.main.Polaroid.id() == 11) list.add(Component.literal("Time to get hammered & sickled!"));
                else list.add(Component.literal("Smells like disinfectant, tastes like disinfectant."));
            }
        }
    }

    /**
     * {@code ItemBDCL}: 40 Ticks Schlucken mit Gluckgeraeusch; kurz vor Schluss wird der Zaehler
     * kuenstlich gebremst, und am Ende stoehnt der Spieler.
     */
    public static class BDCLItem extends Item {
        public BDCLItem() {
            super(new Item.Properties());
        }

        @Override
        //? if < 1.21.1 {
        public int getUseDuration(ItemStack stack) {
        //?} else {
        /*public int getUseDuration(ItemStack stack, net.minecraft.world.entity.LivingEntity hbmUser) {
        *///?}
            return 40;
        }

        @Override
        public UseAnim getUseAnimation(ItemStack stack) {
            return UseAnim.DRINK;
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(player.getItemInHand(hand));
        }

        @Override
        public ItemStack finishUsingItem(ItemStack stack, Level world, LivingEntity entity) {
            if (!(entity instanceof Player p) || !p.getAbilities().instabuild) stack.shrink(1);
            return stack;
        }

        @Override
        public void onUseTick(Level world, LivingEntity entity, ItemStack stack, int count) {
            if (count % 5 == 0 && count >= 10) {
                entity.playSound(HbmSoundsNT.get("hbm:player.gulp"), 1F, 1F);
            }
            if (count == 1) {
                this.finishUsingItem(stack, world, entity);
                entity.stopUsingItem();
                entity.playSound(HbmSoundsNT.get("hbm:player.groan"), 1F, 1F);
                return;
            }
            // Original: itemInUseCount-- - die letzten Schlucke laufen doppelt so schnell
            if (count <= 24 && count % 4 == 0) {
                com.hbm_m.mixin.LivingEntityUseAccessor acc = (com.hbm_m.mixin.LivingEntityUseAccessor) entity;
                acc.hbm_m$setUseItemRemaining(acc.hbm_m$getUseItemRemaining() - 1);
            }
        }
    }

    /** {@code ItemFlask} ({@code flask_infusion}): Schild-Infusion, +5 maximaler Schild. */
    public static class FlaskItem extends Item {
        public FlaskItem() {
            super(new Item.Properties());
        }

        @Override
        public ItemStack finishUsingItem(ItemStack stack, Level world, LivingEntity entity) {
            if (!(entity instanceof Player player)) return stack;
            if (!player.getAbilities().instabuild) stack.shrink(1);
            if (world.isClientSide) return stack;
            float infusion = 5F;
            var props = com.hbm_m.extprop.HbmPlayerProps.get(player);
            props.maxShield = Math.min(props.shieldCap(), props.maxShield + infusion);
            props.shield = Math.min(props.shield + infusion, props.getEffectiveMaxShield());
            props.sync(player);
            return stack;
        }

        @Override
        //? if < 1.21.1 {
        public int getUseDuration(ItemStack stack) {
        //?} else {
        /*public int getUseDuration(ItemStack stack, net.minecraft.world.entity.LivingEntity hbmUser) {
        *///?}
            return 32;
        }

        @Override
        public UseAnim getUseAnimation(ItemStack stack) {
            return UseAnim.DRINK;
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            var props = com.hbm_m.extprop.HbmPlayerProps.get(player);
            if (props.maxShield >= props.shieldCap()) return InteractionResultHolder.pass(stack);
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }
    }

    /** {@code ItemPeas}: vertreibt alle Quackos im Umkreis von 50 Bloecken. */
    public static class PeasItem extends Item {
        public PeasItem() {
            super(new Item.Properties());
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (!player.getAbilities().instabuild) stack.shrink(1);
            for (Entity e : world.getEntities(null, player.getBoundingBox().inflate(50, 50, 50))) {
                var key = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(e.getType());
                if (key != null && key.getPath().equals("entity_elder_one") && e instanceof com.hbm_m.entity.mob.IDespawnable d) d.despawn();
            }
            return InteractionResultHolder.sidedSuccess(stack, world.isClientSide);
        }

        @Override
        //? if < 1.21.1 {
        public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
        //?} else {
        /*public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext hbmTooltipCtx, List<Component> list, TooltipFlag flag) {
        *///?}
            HbmFoodItem.addDescLines(this.getDescriptionId() + ".desc", list);
        }
    }

    @SuppressWarnings("unused")
    private static void sound(Level w, Player p, net.minecraft.sounds.SoundEvent e) {
        w.playSound(null, p.getX(), p.getY(), p.getZ(), e, SoundSource.PLAYERS, 1F, 1F);
    }
}
