package com.hbm_m.item.food;

import java.util.List;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.advancement.ModAdvancements;
import com.hbm_m.explosion.ExplosionLarge;
import com.hbm_m.extprop.HbmLivingProps;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.special.ModConsumables;
import com.hbm_m.util.ContaminationUtil;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/**
 * 1:1-Port von {@code com.hbm.items.food.ItemEnergy}: Energydrinks in Dosen, Limonaden in Flaschen
 * (brauchen einen Flaschenoeffner im Inventar), Kaffee und Schokomilch.
 *
 * <p>Jedes Getraenk setzt fuenf Sekunden Unvertraeglichkeit, gibt seine Effekte, bei Flaschen den
 * Kronkorken und bei Dosen den Ringverschluss zurueck und hinterlaesst den leeren Behaelter. Ein
 * FakePlayer (Maschine), der trinkt, loest eine Explosion der Staerke 5 aus - so im Original.
 * Die Schokomilch ist Nitroglyzerin und explodiert mit Staerke 50.</p>
 */
public class ItemEnergy extends Item {

    @Nullable private Supplier<Item> container = null;
    @Nullable private Supplier<Item> cap = null;
    private boolean requiresOpener = false;

    public ItemEnergy(Properties properties) {
        super(properties);
    }

    public ItemEnergy makeCan() {
        this.container = () -> ModItems.CAN_EMPTY.get();
        this.cap = () -> ModItems.RING_PULL.get();
        this.requiresOpener = false;
        return this;
    }

    public ItemEnergy makeBottle(Supplier<Item> bottle, Supplier<Item> cap) {
        this.container = bottle;
        this.cap = cap;
        this.requiresOpener = true;
        return this;
    }

    private boolean is(Supplier<? extends Item> item) {
        return this == item.get();
    }

    private static void eff(Player p, MobEffect e, int ticks, int amp) {
        p.addEffect(new MobEffectInstance(e, ticks, amp));
    }

    private static boolean isFakePlayer(Player player) {
        //? if forge {
        return player instanceof net.minecraftforge.common.util.FakePlayer;
        //?} else {
        /*return player.getClass().getSimpleName().contains("FakePlayer");
        *///?}
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level world, LivingEntity entity) {
        if (!(entity instanceof Player player)) return stack;

        if (!player.getAbilities().instabuild) stack.shrink(1);

        if (!world.isClientSide) {
            if (isFakePlayer(player)) {
                world.explode(player, player.getX(), player.getY(), player.getZ(), 5F, true, Level.ExplosionInteraction.TNT);
                return stack;
            }

            ModConsumables.applyPotionSickness(player, 5);

            if (is(ModItems.CAN_SMART)) {
                eff(player, MobEffects.MOVEMENT_SPEED, 30 * 20, 1);
                eff(player, MobEffects.DAMAGE_RESISTANCE, 30 * 20, 2);
                eff(player, MobEffects.DAMAGE_BOOST, 30 * 20, 0);
            }
            if (is(ModItems.CAN_CREATURE)) {
                eff(player, MobEffects.MOVEMENT_SPEED, 30 * 20, 0);
                eff(player, MobEffects.DAMAGE_RESISTANCE, 30 * 20, 2);
                eff(player, MobEffects.REGENERATION, 30 * 20, 1);
            }
            if (is(ModItems.CAN_REDBOMB)) {
                eff(player, MobEffects.MOVEMENT_SPEED, 30 * 20, 0);
                eff(player, MobEffects.ABSORPTION, 30 * 20, 2);
                eff(player, MobEffects.JUMP, 30 * 20, 1);
            }
            if (is(ModItems.CAN_MRSUGAR)) {
                eff(player, MobEffects.MOVEMENT_SPEED, 30 * 20, 0);
                eff(player, MobEffects.DIG_SPEED, 30 * 20, 1);
                eff(player, MobEffects.JUMP, 30 * 20, 2);
            }
            if (is(ModItems.CAN_OVERCHARGE)) {
                eff(player, MobEffects.MOVEMENT_SPEED, 30 * 20, 1);
                eff(player, MobEffects.DAMAGE_RESISTANCE, 30 * 20, 2);
                eff(player, MobEffects.DAMAGE_BOOST, 30 * 20, 0);
            }
            if (is(ModItems.CAN_LUNA)) {
                eff(player, MobEffects.MOVEMENT_SPEED, 30 * 20, 1);
                eff(player, MobEffects.DAMAGE_RESISTANCE, 30 * 20, 2);
                eff(player, MobEffects.DAMAGE_BOOST, 30 * 20, 1);
                eff(player, MobEffects.REGENERATION, 30 * 20, 2);
            }
            if (is(ModItems.CAN_BEPIS)) {
                eff(player, MobEffects.MOVEMENT_SPEED, 30 * 20, 3);
                eff(player, MobEffects.DAMAGE_RESISTANCE, 30 * 20, 3);
            }
            if (is(ModItems.CAN_BREEN)) {
                eff(player, MobEffects.CONFUSION, 30 * 20, 0);
            }
            if (is(ModItems.CAN_MUG)) {
                eff(player, MobEffects.DAMAGE_RESISTANCE, 3 * 60 * 20, 2);
                eff(player, MobEffects.REGENERATION, 60 * 20, 2);
            }
            if (is(ModItems.CHOCOLATE_MILK)) {
                ExplosionLarge.explode(world, player.getX(), player.getY(), player.getZ(), 50, true, false, false);
            }
            if (is(ModItems.BOTTLE_NUKA)) {
                player.heal(4F);
                eff(player, MobEffects.MOVEMENT_SPEED, 30 * 20, 1);
                eff(player, MobEffects.DIG_SPEED, 30 * 20, 1);
                ContaminationUtil.contaminate(player, ContaminationUtil.HazardType.RADIATION, ContaminationUtil.ContaminationType.RAD_BYPASS, 5.0F);
            }
            if (is(ModItems.BOTTLE_CHERRY)) {
                player.heal(6F);
                eff(player, MobEffects.MOVEMENT_SPEED, 30 * 20, 0);
                eff(player, MobEffects.JUMP, 30 * 20, 2);
                ContaminationUtil.contaminate(player, ContaminationUtil.HazardType.RADIATION, ContaminationUtil.ContaminationType.RAD_BYPASS, 5.0F);
            }
            if (is(ModItems.BOTTLE_QUANTUM)) {
                player.heal(10F);
                eff(player, MobEffects.MOVEMENT_SPEED, 30 * 20, 1);
                eff(player, MobEffects.DAMAGE_RESISTANCE, 30 * 20, 2);
                eff(player, MobEffects.DAMAGE_BOOST, 30 * 20, 1);
                ContaminationUtil.contaminate(player, ContaminationUtil.HazardType.RADIATION, ContaminationUtil.ContaminationType.RAD_BYPASS, 15.0F);
            }
            if (is(ModItems.BOTTLE2_KORL)) {
                player.heal(6);
                eff(player, MobEffects.MOVEMENT_SPEED, 30 * 20, 1);
                eff(player, MobEffects.DIG_SPEED, 30 * 20, 2);
                eff(player, MobEffects.DAMAGE_BOOST, 30 * 20, 2);
            }
            if (is(ModItems.BOTTLE2_FRITZ)) {
                player.heal(6);
                eff(player, MobEffects.MOVEMENT_SPEED, 30 * 20, 1);
                eff(player, MobEffects.DAMAGE_RESISTANCE, 30 * 20, 2);
                eff(player, MobEffects.JUMP, 30 * 20, 2);
            }
            if (is(ModItems.BOTTLE_SPARKLE)) {
                player.heal(10F);
                eff(player, MobEffects.MOVEMENT_SPEED, 120 * 20, 1);
                eff(player, MobEffects.DAMAGE_RESISTANCE, 120 * 20, 2);
                eff(player, MobEffects.DAMAGE_BOOST, 120 * 20, 2);
                eff(player, MobEffects.DIG_SPEED, 120 * 20, 1);
                ContaminationUtil.contaminate(player, ContaminationUtil.HazardType.RADIATION, ContaminationUtil.ContaminationType.RAD_BYPASS, 5.0F);
            }
            if (is(ModItems.BOTTLE_RAD)) {
                player.heal(10F);
                eff(player, MobEffects.MOVEMENT_SPEED, 120 * 20, 1);
                eff(player, MobEffects.DAMAGE_RESISTANCE, 120 * 20, 2);
                eff(player, MobEffects.FIRE_RESISTANCE, 120 * 20, 0);
                eff(player, MobEffects.DAMAGE_BOOST, 120 * 20, 4);
                eff(player, MobEffects.DIG_SPEED, 120 * 20, 1);
                ContaminationUtil.contaminate(player, ContaminationUtil.HazardType.RADIATION, ContaminationUtil.ContaminationType.RAD_BYPASS, 15.0F);
            }
            if (is(ModItems.COFFEE)) {
                player.heal(10);
                eff(player, MobEffects.MOVEMENT_SPEED, 60 * 20, 2);
            }
            if (is(ModItems.COFFEE_RADIUM)) {
                player.heal(10);
                eff(player, MobEffects.MOVEMENT_SPEED, 60 * 20, 2);
                HbmLivingProps.incrementRadiation(player, 500F);
                ModAdvancements.grant(player, ModAdvancements.RADIUM);
            }

            if (!player.getAbilities().instabuild) {
                if (this.cap != null) player.getInventory().add(new ItemStack(this.cap.get()));
                if (this.container != null) {
                    if (stack.isEmpty()) return new ItemStack(this.container.get());
                    player.getInventory().add(new ItemStack(this.container.get()));
                }
            }
            player.inventoryMenu.broadcastChanges();
        }

        return stack;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 32;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (ModConsumables.isPotionSick(player)) return InteractionResultHolder.pass(stack);
        if (this.requiresOpener && !player.getInventory().contains(new ItemStack(ModItems.BOTTLE_OPENER.get()))) return InteractionResultHolder.pass(stack);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
        String line = null;
        if (is(ModItems.CAN_SMART)) line = "Cheap and full of bubbles";
        if (is(ModItems.CAN_CREATURE)) line = "Basically gasoline in a tin can";
        if (is(ModItems.CAN_REDBOMB)) line = "Liquefied explosives";
        if (is(ModItems.CAN_MRSUGAR)) line = "An intellectual drink, for the chosen ones!";
        if (is(ModItems.CAN_OVERCHARGE)) line = "Possible side effects include heart attacks, seizures or zombification";
        if (is(ModItems.CAN_LUNA)) line = "Contains actual selenium and star metal. Tastes like night.";
        if (is(ModItems.CAN_BEPIS)) line = "beppp";
        if (line != null) list.add(Component.literal(line));
        if (is(ModItems.CAN_BREEN)) {
            list.add(Component.literal("Don't drink the water. They put something in it, to make you forget."));
            list.add(Component.literal("I don't even know how I got here."));
        }
        if (is(ModItems.CHOCOLATE_MILK)) {
            list.add(Component.literal("Regular chocolate milk. Safe to drink."));
            list.add(Component.literal("Totally not made from nitroglycerine."));
        }
        if (is(ModItems.BOTTLE_NUKA)) list.add(Component.literal("Contains about 210 kcal and 1500 mSv."));
        if (is(ModItems.BOTTLE_CHERRY)) list.add(Component.literal("Now with severe radiation poisoning in every seventh bottle!"));
        if (is(ModItems.BOTTLE_QUANTUM)) list.add(Component.literal("Comes with a colorful mix of over 70 isotopes!"));
        if (is(ModItems.BOTTLE2_KORL)) list.add(Component.literal("Contains actual orange juice!"));
        if (is(ModItems.BOTTLE2_FRITZ)) list.add(Component.literal("moremore caffeine"));
        if (is(ModItems.BOTTLE_SPARKLE)) list.add(Component.literal(com.hbm_m.main.Polaroid.id() == 11 ? "Contains trace amounts of taint." : "The most delicious beverage in the wasteland!"));
        if (is(ModItems.BOTTLE_RAD)) list.add(Component.literal(com.hbm_m.main.Polaroid.id() == 11 ? "Now with 400% more radiation!" : "Tastes like radish and radiation."));
        if (this.requiresOpener) list.add(Component.literal("[Requires bottle opener]"));
    }
}
