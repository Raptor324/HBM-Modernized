package com.hbm_m.item.special;

import java.util.function.BiConsumer;

import com.hbm_m.effect.ModEffects;
import com.hbm_m.damagesource.ModDamageSources;
import com.hbm_m.damagesource.ModDamageTypes;
import com.hbm_m.item.ItemCustomLore;
import com.hbm_m.item.ModItems;
import com.hbm_m.sound.HbmSoundsNT;
import com.hbm_m.util.EnchantmentUtil;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 1:1 {@code com.hbm.items.special.ItemSimpleConsumable}: Rechtsklick- und Schlag-Aktionen
 * (Client/Server getrennt), plus die in {@code init()} definierten Spritzen, Blutbeutel und RadAway.
 */
public class ItemSimpleConsumable extends ItemCustomLore {

    /** Original {@code Pair<EntityLivingBase, EntityLivingBase>}: key = Getroffener, value = Anwender. */
    public record Pair(LivingEntity key, LivingEntity value) {}

    //if java is giving me the power of generics and delegates then i'm going to use them, damn it!
    private BiConsumer<ItemStack, Player> useAction;
    private BiConsumer<ItemStack, Player> useActionServer;
    private BiConsumer<ItemStack, Pair> hitAction;
    private BiConsumer<ItemStack, Pair> hitActionServer;

    public ItemSimpleConsumable(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (this.useAction != null)
            this.useAction.accept(stack, player);

        if (!world.isClientSide && this.useActionServer != null)
            this.useActionServer.accept(stack, player);

        return InteractionResultHolder.pass(stack);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity entity, LivingEntity entityPlayer) {

        if (this.hitAction != null)
            this.hitAction.accept(stack, new Pair(entity, entityPlayer));

        if (!entity.level().isClientSide && this.hitActionServer != null)
            this.hitActionServer.accept(stack, new Pair(entity, entityPlayer));

        return false;
    }

    public static void doRadaway(ItemStack stack, Player user, int duration) {
        ModConsumables.giveSoundAndDecrement(stack, user, HbmSoundsNT.get("hbm:item.radaway"), new ItemStack(ModItems.IV_EMPTY.get()));
        ModConsumables.addPotionEffect(user, ModEffects.RADAWAY.get(), duration, 0);
    }

    //this formatting style probably already has a name but i will call it "the greg"
    public ItemSimpleConsumable setUseAction(       BiConsumer<ItemStack, Player> delegate) { this.useAction = delegate;        return this; }
    public ItemSimpleConsumable setUseActionServer( BiConsumer<ItemStack, Player> delegate) { this.useActionServer = delegate;  return this; }
    public ItemSimpleConsumable setHitAction(       BiConsumer<ItemStack, Pair> delegate)   { this.hitAction = delegate;        return this; }
    public ItemSimpleConsumable setHitActionServer( BiConsumer<ItemStack, Pair> delegate)   { this.hitActionServer = delegate;  return this; }

    @Override
    public ItemSimpleConsumable setRarity(net.minecraft.world.item.Rarity rarity) { super.setRarity(rarity); return this; }

    @Override
    public ItemSimpleConsumable setEffect() { super.setEffect(); return this; }

    // ------------------------------------------------------------------ init()

    /// SYRINGES ///
    public static ItemSimpleConsumable syringeAntidote() {
        return new ItemSimpleConsumable(new Properties())
                .setUseActionServer((stack, user) -> effectAntidote(stack, user, user)).setHitActionServer((stack, pair) -> effectAntidote(stack, pair.key, pair.value));
    }

    public static ItemSimpleConsumable syringePoison() {
        return new ItemSimpleConsumable(new Properties())
                .setUseActionServer((stack, user) -> effectPoison(stack, user, user)).setHitActionServer((stack, pair) -> effectPoison(stack, pair.key, pair.value));
    }

    public static ItemSimpleConsumable syringeAwesome() {
        return new ItemSimpleConsumable(new Properties())
                .setUseActionServer((stack, user) -> effectAwesome(stack, user, user)).setHitActionServer((stack, pair) -> effectAwesome(stack, pair.key, pair.value))
                .setRarity(net.minecraft.world.item.Rarity.UNCOMMON).setEffect();
    }

    /// BLOOD BAGS ///
    public static ItemSimpleConsumable ivEmpty() {
        return new ItemSimpleConsumable(new Properties()).setUseActionServer((stack, user) -> {
            ModConsumables.giveSoundAndDecrement(stack, user, HbmSoundsNT.get("hbm:item.syringe"), new ItemStack(ModItems.IV_BLOOD.get()));
            user.setHealth(Math.max(user.getHealth() - 5F, 0F));
            if (user.getHealth() <= 0) user.die(user.damageSources().magic());
        });
    }

    public static ItemSimpleConsumable ivBlood() {
        return new ItemSimpleConsumable(new Properties()).setUseActionServer((stack, user) -> {
            ModConsumables.giveSoundAndDecrement(stack, user, HbmSoundsNT.get("hbm:item.radaway"), new ItemStack(ModItems.IV_EMPTY.get()));
            user.heal(5F);
        });
    }

    public static ItemSimpleConsumable ivXpEmpty() {
        return new ItemSimpleConsumable(new Properties()).setUseActionServer((stack, user) -> {
            if (EnchantmentUtil.getTotalExperience(user) >= 100) {
                ModConsumables.giveSoundAndDecrement(stack, user, HbmSoundsNT.get("hbm:item.syringe"), new ItemStack(ModItems.IV_XP.get()));
                EnchantmentUtil.setExperience(user, EnchantmentUtil.getTotalExperience(user) - 100);
            }
        });
    }

    public static ItemSimpleConsumable ivXp() {
        return new ItemSimpleConsumable(new Properties()).setUseActionServer((stack, user) -> {
            ModConsumables.giveSoundAndDecrement(stack, user, SoundEvents.EXPERIENCE_ORB_PICKUP, new ItemStack(ModItems.IV_XP_EMPTY.get()));
            EnchantmentUtil.addExperience(user, 100, false);
        });
    }

    /// RADAWAY ///
    public static ItemSimpleConsumable radaway(int duration) {
        return new ItemSimpleConsumable(new Properties()).setUseActionServer((stack, user) -> doRadaway(stack, user, duration));
    }

    public static void effectAntidote(ItemStack stack, LivingEntity affected, LivingEntity source) {
        if (ModConsumables.isPotionSick(affected)) return;
        affected.removeAllEffects();
        ModConsumables.giveSoundAndDecrement(stack, source, HbmSoundsNT.get("hbm:item.syringe"), new ItemStack(ModItems.SYRINGE_EMPTY.get()));
        ModConsumables.applyPotionSickness(affected, 5);
    }

    public static void effectPoison(ItemStack stack, LivingEntity affected, LivingEntity source) {
        if (affected == source) {
            affected.hurt(ModDamageSources.create(affected.level(), affected.getRandom().nextBoolean() ? ModDamageTypes.EUTHANIZED_SELF : ModDamageTypes.EUTHANIZED_SELF_2), 30);
        } else {
            DamageSource euthanized = ModDamageSources.create(source, source, ModDamageTypes.EUTHANIZED);
            affected.hurt(euthanized, 30);
        }
        ModConsumables.giveSoundAndDecrement(stack, source, HbmSoundsNT.get("hbm:item.syringe"), new ItemStack(ModItems.SYRINGE_EMPTY.get()));
    }

    public static void effectAwesome(ItemStack stack, LivingEntity affected, LivingEntity source) {
        if (ModConsumables.isPotionSick(affected)) return;
        ModConsumables.giveSoundAndDecrement(stack, source, HbmSoundsNT.get("hbm:item.syringe"), new ItemStack(ModItems.SYRINGE_EMPTY.get()));
        affected.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 50 * 20, 9));
        affected.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 50 * 20, 9));
        affected.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 50 * 20, 0));
        affected.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 50 * 20, 24));
        affected.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 50 * 20, 9));
        affected.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 50 * 20, 6));
        affected.addEffect(new MobEffectInstance(MobEffects.JUMP, 50 * 20, 9));
        affected.addEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST, 50 * 20, 9));
        affected.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 50 * 20, 4));
        affected.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 5 * 20, 4));
        affected.addEffect(new MobEffectInstance(ModEffects.RADX.get(), 50 * 20, 9));
        ModConsumables.applyPotionSickness(affected, 5);
    }
}
