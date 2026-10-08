package com.hbm_m.item.special;

import com.hbm_m.platform.EffectHooks;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.IFillableItem;
import com.hbm_m.armormod.util.ArmorModificationHelper;
import com.hbm_m.effect.ModEffects;
import com.hbm_m.extprop.HbmLivingProps;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.item.ITooltipProvider;
import com.hbm_m.item.ModItems;
import com.hbm_m.main.MainRegistry;
import com.hbm_m.sound.HbmSoundsNT;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * 1:1 {@code com.hbm.items.special.ItemSyringe} (Stimpak, Med-X, Psycho, Super-Stimpak, Taint,
 * MKUNICORN, Erste-Hilfe-Kasten, Jetpack-Reservetank, CBT-Geraet). Die Radaway-/Gasmaskenzweige des
 * Originals sind dort toter Code (die Items sind andere Klassen) und fehlen daher hier.
 */
public class ItemSyringe extends Item implements ITooltipProvider {

    public enum Type { STIMPAK, MEDX, PSYCHO, SUPER, TAINT, MKUNICORN, MED_BAG, JETPACK_TANK, CBT_DEVICE }

    private final Type type;

    public ItemSyringe(Type type, Properties properties) {
        super(properties);
        this.type = type;
    }

    private static SoundEvent snd(String key) {
        return HbmSoundsNT.get(key);
    }

    private static void play(Level world, Entity e, String key) {
        world.playSound(null, e.getX(), e.getY(), e.getZ(), snd(key), SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    private static void giveMetalEmpty(Player player) {
        ItemStack empty = new ItemStack(ModItems.SYRINGE_METAL_EMPTY.get());
        if (!player.getInventory().add(empty)) player.drop(empty, false);
    }

    /** Stimpak/Med-X/Psycho/Super: Wirkung, Stack -1, Klang, leere Spritze zurueck, Uebelkeit. */
    private InteractionResultHolder<ItemStack> injectSelf(ItemStack stack, Level world, Player player, int sickness) {
        stack.shrink(1);
        play(world, player, "hbm:item.syringe");

        // Original kehrt hier vor applyPotionSickness zurueck - die letzte Spritze macht nicht uebel.
        if (stack.isEmpty()) {
            return InteractionResultHolder.success(new ItemStack(ModItems.SYRINGE_METAL_EMPTY.get()));
        }

        giveMetalEmpty(player);
        ModConsumables.applyPotionSickness(player, sickness);
        return InteractionResultHolder.success(stack);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        switch (type) {
            case STIMPAK -> {
                if (!ModConsumables.isPotionSick(player) && !world.isClientSide) {
                    player.heal(5);
                    return injectSelf(stack, world, player, 5);
                }
            }
            case MEDX -> {
                if (!ModConsumables.isPotionSick(player) && !world.isClientSide) {
                    player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 4 * 60 * 20, 2));
                    return injectSelf(stack, world, player, 5);
                }
            }
            case PSYCHO -> {
                if (!ModConsumables.isPotionSick(player) && !world.isClientSide) {
                    player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 2 * 60 * 20, 0));
                    player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 2 * 60 * 20, 0));
                    return injectSelf(stack, world, player, 5);
                }
            }
            case SUPER -> {
                if (!ModConsumables.isPotionSick(player) && !world.isClientSide) {
                    player.heal(25);
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10 * 20, 0));
                    return injectSelf(stack, world, player, 15);
                }
            }
            case MED_BAG -> {
                if (!ModConsumables.isPotionSick(player) && !world.isClientSide) {
                    player.setHealth(player.getMaxHealth());

                    player.removeEffect(MobEffects.BLINDNESS);
                    player.removeEffect(MobEffects.CONFUSION);
                    player.removeEffect(MobEffects.DIG_SLOWDOWN);
                    player.removeEffect(MobEffects.HUNGER);
                    player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                    player.removeEffect(MobEffects.POISON);
                    player.removeEffect(MobEffects.WEAKNESS);
                    player.removeEffect(MobEffects.WITHER);
                    player.removeEffect(EffectHooks.of(ModEffects.RADIATION));

                    ModConsumables.applyPotionSickness(player, 15);

                    stack.shrink(1);
                }
            }
            case TAINT -> {
                if (!world.isClientSide) {
                    player.addEffect(new MobEffectInstance(EffectHooks.of(ModEffects.TAINT), 60 * 20, 0));
                    player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 5 * 20, 0));

                    stack.shrink(1);
                    play(world, player, "hbm:item.syringe");
                }

                // Original gibt beides auch clientseitig; im Port nur serverseitig (sonst Geister-Items).
                if (!world.isClientSide) {
                    giveMetalEmpty(player);

                    ItemStack bottle = new ItemStack(ModItems.BOTTLE2_EMPTY.get());
                    if (!player.getInventory().add(bottle)) player.drop(bottle, false);
                }
            }
            case JETPACK_TANK -> {
                ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
                if (!chest.isEmpty() && !world.isClientSide) {

                    ItemStack jetpack = chest;

                    if (jetpack.getItem() instanceof ArmorItem && ArmorModificationHelper.hasMods(jetpack)) {
                        jetpack = ArmorModificationHelper.pryMods(jetpack)[ArmorModificationHelper.plate_only];
                    }

                    if (jetpack == null || jetpack.isEmpty() || !(jetpack.getItem() instanceof IFillableItem fillable))
                        return InteractionResultHolder.pass(stack);

                    if (!fillable.acceptsFluid(ModFluids.KEROSENE.getSource(), jetpack))
                        return InteractionResultHolder.pass(stack);

                    if (fillable.tryFill(ModFluids.KEROSENE.getSource(), 1000, jetpack) < 1000) {
                        play(world, player, "hbm:item.jetpackTank");
                        stack.shrink(1);
                    }

                    if (jetpack.getItem() != chest.getItem())
                        ArmorModificationHelper.applyMod(chest, jetpack);
                }
            }
            case CBT_DEVICE -> {
                if (!world.isClientSide) {
                    player.addEffect(new MobEffectInstance(EffectHooks.of(ModEffects.BANG), 30, 0));

                    stack.shrink(1);
                    play(world, player, "hbm:item.vice");
                }
            }
            default -> { }
        }

        return InteractionResultHolder.pass(stack);
    }

    /** Anwendung am Getroffenen: leere Spritze an den Anwender. */
    private static void afterHit(ItemStack stack, Level world, LivingEntity entity, LivingEntity user) {
        stack.shrink(1);
        play(world, entity, "hbm:item.syringe");
        if (user instanceof Player player) giveMetalEmpty(player);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity entity, LivingEntity entityPlayer) {
        Level world = entity.level();
        if (world.isClientSide) return false;

        switch (type) {
            case STIMPAK -> {
                if (!ModConsumables.isPotionSick(entity)) {
                    entity.heal(5);
                    ModConsumables.applyPotionSickness(entity, 5);
                    afterHit(stack, world, entity, entityPlayer);
                }
            }
            case MEDX -> {
                if (!ModConsumables.isPotionSick(entity)) {
                    entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 4 * 60 * 20, 2));
                    ModConsumables.applyPotionSickness(entity, 5);
                    afterHit(stack, world, entity, entityPlayer);
                }
            }
            case PSYCHO -> {
                if (!ModConsumables.isPotionSick(entity)) {
                    entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 2 * 60 * 20, 0));
                    entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 2 * 60 * 20, 0));
                    ModConsumables.applyPotionSickness(entity, 5);
                    afterHit(stack, world, entity, entityPlayer);
                }
            }
            case SUPER -> {
                if (!ModConsumables.isPotionSick(entity)) {
                    entity.heal(25);
                    entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10 * 20, 0));
                    ModConsumables.applyPotionSickness(entity, 15);
                    afterHit(stack, world, entity, entityPlayer);
                }
            }
            case TAINT -> {
                entity.addEffect(new MobEffectInstance(EffectHooks.of(ModEffects.TAINT), 60 * 20, 0));
                entity.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 5 * 20, 0));
                afterHit(stack, world, entity, entityPlayer);
                if (entityPlayer instanceof Player player) {
                    ItemStack bottle = new ItemStack(ModItems.BOTTLE2_EMPTY.get());
                    if (!player.getInventory().add(bottle)) player.drop(bottle, false);
                }
            }
            case MKUNICORN -> {
                HbmLivingProps.setContagion(entity, 3 * 60 * 60 * 20);
                play(world, entity, "hbm:item.syringe");
                stack.shrink(1);
                MainRegistry.LOGGER.info("[MKU] {} used an MKU syringe!", entityPlayer.getName().getString());
            }
            default -> { }
        }

        return false;
    }

    @Override
    public void appendHbmTooltip(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
        switch (type) {
            case MEDX -> list.add(Component.literal("Resistance III for 4 minutes"));
            case PSYCHO -> {
                list.add(Component.literal("Resistance I for 2 minutes"));
                list.add(Component.literal("Strength I for 2 minutes"));
            }
            case STIMPAK -> list.add(Component.literal("Heals 2.5 hearts"));
            case SUPER -> {
                list.add(Component.literal("Heals 25 hearts"));
                list.add(Component.literal("Slowness I for 10 seconds"));
            }
            case MED_BAG -> {
                list.add(Component.literal("Full heal, regardless of max health"));
                list.add(Component.literal("Removes negative effects"));
            }
            case TAINT -> {
                list.add(Component.literal("Tainted I for 60 seconds"));
                list.add(Component.literal("Nausea I for 5 seconds"));
                list.add(Component.literal("Cloud damage + taint = tainted heart effect"));
            }
            case JETPACK_TANK -> list.add(Component.literal("Fills worn jetpack with up to 1000mB of kerosene"));
            case MKUNICORN -> list.add(Component.literal("?").withStyle(ChatFormatting.RED));
            default -> { }
        }
    }
}
