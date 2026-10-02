package com.hbm_m.item.food;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.entity.ModEntities;
import com.hbm_m.entity.effect.VortexEntity;
import com.hbm_m.item.ModItems;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
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

/**
 * 1:1-Port von {@code com.hbm.items.food.ItemConserve}: Konservendosen. Im Original ein
 * {@code ItemEnumMulti} ueber {@link EnumFoodType}; der Port folgt seiner Konvention mit einem Item
 * pro Sorte ({@code canned_<sorte>}, genau die Namen aus {@code getUnlocalizedName}).
 *
 * <p>Keine {@code FoodProperties}: das Original ist kein {@code ItemFood}, sondern ruft
 * {@code getFoodStats().addStats} selbst, raelpst und gibt den Dosenschluessel zurueck. Das schwarze
 * Loch beschwoert einen Vortex, die Rekursion gibt mit 9:10 eine neue Rekursion, die Faust
 * schlaegt mit 2 Magieschaden zu.</p>
 */
public class ItemConserve extends Item {

    public enum EnumFoodType {
        BEEF(8, 0.75F),
        TUNA(4, 0.75F),
        MYSTERY(6, 0.5F),
        PASHTET(4, 0.5F),
        CHEESE(3, 1F),
        SLIME(15, 5F),
        MILK(5, 0.25F),
        ASS(6, 0.75F), // :3
        PIZZA(8, 075F), // sic: 075F ist 75.0F
        TUBE(2, 0.25F),
        TOMATO(4, 0.5F),
        ASBESTOS(7, 1F),
        BHOLE(10, 1F),
        HOTDOGS(5, 0.75F),
        LEFTOVERS(1, 0.1F),
        YOGURT(3, 0.5F),
        STEW(5, 0.5F),
        CHINESE(6, 0.1F),
        OIL(3, 1F),
        FIST(6, 0.75F),
        SPAM(8, 1F),
        FRIED(10, 0.75F),
        NAPALM(6, 1F),
        DIESEL(6, 1F),
        KEROSENE(6, 1F),
        RECURSION(1, 1F),
        BARK(2, 1F);

        public final int foodLevel;
        public final float saturation;

        EnumFoodType(int level, float sat) {
            this.foodLevel = level;
            this.saturation = sat;
        }
    }

    public final EnumFoodType type;

    public ItemConserve(Properties properties, EnumFoodType type) {
        super(properties);
        this.type = type;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level world, LivingEntity entity) {
        if (!(entity instanceof Player player)) return stack;
        stack.shrink(1);
        player.getFoodData().eat(type.foodLevel, type.saturation);
        world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 0.5F, world.random.nextFloat() * 0.1F + 0.9F);
        this.onFoodEaten(stack, world, player);
        return stack;
    }

    protected void onFoodEaten(ItemStack stack, Level world, Player player) {
        player.getInventory().add(new ItemStack(ModItems.CAN_KEY.get()));

        if (type == EnumFoodType.BHOLE && !world.isClientSide) {
            VortexEntity vortex = new VortexEntity(ModEntities.VORTEX.get(), world);
            vortex.setSize(0.5F);
            vortex.setShrinkRate(0.01F);
            vortex.noBreak();
            vortex.setPos(player.getX(), player.getY(), player.getZ());
            world.addFreshEntity(vortex);
        } else if (type == EnumFoodType.RECURSION && world.random.nextInt(10) > 0) {
            player.getInventory().add(new ItemStack(ModItems.CANNED_RECURSION.get()));
        } else if (type == EnumFoodType.FIST) {
            player.hurt(world.damageSources().magic(), 2F);
        }
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 32;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.EAT;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.canEat(false)) player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        HbmFoodItem.addDescLines(this.getDescriptionId() + ".desc", tooltip);
    }
}
