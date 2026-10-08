package com.hbm_m.item.weapon.sedna.impl;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import com.hbm_m.api.fluids.IFillableItem;
import com.hbm_m.entity.projectile.EntityChemical;
import com.hbm_m.item.weapon.sedna.GunConfig;
import com.hbm_m.item.weapon.sedna.ItemGunBaseNT;
import com.hbm_m.item.weapon.sedna.Receiver;
import com.hbm_m.item.weapon.sedna.mags.IMagazine;
import com.hbm_m.item.weapon.sedna.mags.MagazineFluid;
import com.hbm_m.render.anim.AnimationEnums.GunAnimation;
import com.hbm_m.util.Vec3NT;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

/**
 * 1:1 {@code ItemGunChemthrower}: Chemiewerfer, wird ueber {@link IFillableItem} an Fluessigkeitsquellen (Tanks,
 * Betanker) gefuellt. Portabweichung: Fluidtyp = Vanilla-{@link Fluid} statt {@code FluidType}-ID (siehe {@link MagazineFluid}).
 */
public class ItemGunChemthrower extends ItemGunBaseNT implements IFillableItem {

    public static final int CONSUMPTION = 3;

    public ItemGunChemthrower(WeaponQuality quality, GunConfig... cfg) {
        super(quality, cfg);
    }

    @Override
    public boolean acceptsFluid(Fluid type, ItemStack stack) {
        return getFluidType(stack) == type || getMagCount(stack) == 0;
    }

    public static final int transferSpeed = 50;

    @Override
    public int tryFill(Fluid type, int amount, ItemStack stack) {

        if (!acceptsFluid(type, stack)) return amount;
        if (getMagCount(stack) == 0) setMagType(stack, type);

        int fill = getMagCount(stack);
        int req = this.getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack).getCapacity(stack) - fill;
        int toFill = Math.min(amount, req);
        toFill = Math.min(toFill, transferSpeed);
        setMagCount(stack, fill + toFill);

        return amount - toFill;
    }

    public Fluid getFluidType(ItemStack stack) {
        return getMagType(stack);
    }

    @Override
    public boolean providesFluid(Fluid type, ItemStack stack) {
        return getFluidType(stack) == type;
    }

    @Override
    public int tryEmpty(Fluid type, int amount, ItemStack stack) {
        int fill = getMagCount(stack);
        int toUnload = Math.min(fill, amount);
        toUnload = Math.min(toUnload, transferSpeed);
        setMagCount(stack, fill - toUnload);
        return toUnload;
    }

    @Override public Fluid getFirstFluidType(ItemStack stack) { return getMagType(stack); }
    @Override public int getFill(ItemStack stack) { return getMagCount(stack); }

    public static Fluid getMagType(ItemStack stack) { return MagazineFluid.getMagType(stack, 0); }
    public static void setMagType(ItemStack stack, Fluid value) { MagazineFluid.setMagType(stack, 0, value); }
    public static int getMagCount(ItemStack stack) { return ItemGunBaseNT.getValueInt(stack, MagazineFluid.KEY_MAG_COUNT + 0); }
    public static void setMagCount(ItemStack stack, int value) { ItemGunBaseNT.setValueInt(stack, MagazineFluid.KEY_MAG_COUNT + 0, value); }

    public static BiFunction<ItemStack, LambdaContext, Boolean> LAMBDA_CAN_FIRE = (stack, ctx) -> { return ctx.config.getReceivers(stack)[0].getMagazine(stack).getAmount(stack, ctx.inventory) >= CONSUMPTION; };

    @SuppressWarnings("rawtypes")
    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_FIRE = (stack, ctx) -> {
        LivingEntity entity = ctx.entity;
        Player player = ctx.getPlayer();
        int index = ctx.configIndex;
        ItemGunBaseNT.playAnimation(player, stack, GunAnimation.CYCLE, ctx.configIndex);

        Receiver primary = ctx.config.getReceivers(stack)[0];
        IMagazine mag = primary.getMagazine(stack);

        Vec3NT offset = primary.getProjectileOffset(stack);
        double forwardOffset = offset.xCoord;
        double heightOffset = offset.yCoord;
        double sideOffset = offset.zCoord;

        EntityChemical chem = new EntityChemical(entity.level(), entity, sideOffset, heightOffset, forwardOffset);
        chem.setFluid((Fluid) mag.getType(stack, ctx.inventory));
        entity.level().addFreshEntity(chem);

        mag.useUpAmmo(stack, ctx.inventory, CONSUMPTION);
        ItemGunBaseNT.setWear(stack, index, Math.min(ItemGunBaseNT.getWear(stack, index) + 1F, ctx.config.getDurability(stack)));
    };
}
