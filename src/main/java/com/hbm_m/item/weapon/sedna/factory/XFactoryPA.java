package com.hbm_m.item.weapon.sedna.factory;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.item.weapon.sedna.Crosshair;
import com.hbm_m.item.weapon.sedna.GunConfig;
import com.hbm_m.item.weapon.sedna.ItemGunBaseNT;
import com.hbm_m.item.weapon.sedna.ItemGunBaseNT.GunState;
import com.hbm_m.item.weapon.sedna.ItemGunBaseNT.LambdaContext;
import com.hbm_m.item.weapon.sedna.ItemGunBaseNT.WeaponQuality;
import com.hbm_m.item.weapon.sedna.Receiver;
import com.hbm_m.item.weapon.sedna.impl.IPAMelee;
import com.hbm_m.item.weapon.sedna.impl.IPARanged;
import com.hbm_m.item.weapon.sedna.impl.IPAWeaponsProvider;
import com.hbm_m.render.anim.AnimationEnums.GunAnimation;
import com.hbm_m.render.anim.BusAnimation;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * 1:1 {@code XFactoryPA}: Power-armor conditional weapons (melee controller and ranged attack remote).
 * Die Komponenten liefert das Brustteil ueber {@link IPAWeaponsProvider} (Original ArmorNCRPA/ArmorRPA).
 */
public class XFactoryPA {

    public static void init() {

        GunFactory.reg("gun_pa_melee", new ItemGunPA(WeaponQuality.UTILITY, new GunConfig()
                .draw(10).crosshair(Crosshair.NONE)
                .rec(new Receiver(0))
                .pp(LAMBDA_CLICK_MELEE_PRIMARY).ps(LAMBDA_CLICK_MELEE_SENONDARY).decider(GunStateDecider.LAMBDA_STANDARD_DECIDER)
                .anim(LAMBDA_MELEE_ANIMS).orchestra(ORCHESTRA)
                ));

        // Original zusaetzlich setFull3D().setTextureName("gun_pa_ranged") - im Port ueber Modell/Textur geregelt
        GunFactory.reg("gun_pa_ranged", new ItemGunPA(WeaponQuality.UTILITY, new GunConfig()
                .draw(0).crosshair(Crosshair.CROSS)
                .rec(new Receiver(0))
                .pp(LAMBDA_CLICK_RANGED_PRIMARY).ps(LAMBDA_CLICK_RANGED_SENONDARY).decider(GunStateDecider.LAMBDA_STANDARD_DECIDER)
                ));
    }

    public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA = (stack, ctx) -> {
        IPAMelee component = IPAWeaponsProvider.getMeleeComponentCommon(ctx.getPlayer());
        if (component != null) component.orchestra(stack, ctx);
    };

    public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_MELEE_ANIMS = (stack, type) -> {
        IPAMelee component = IPAWeaponsProvider.getMeleeComponentClient();
        if (component != null) return component.playAnim(stack, type);
        return null;
    };

    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_CLICK_MELEE_PRIMARY = (stack, ctx) -> {
        IPAMelee component = IPAWeaponsProvider.getMeleeComponentCommon(ctx.getPlayer());
        if (component != null) component.clickPrimary(stack, ctx);
    };
    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_CLICK_MELEE_SENONDARY = (stack, ctx) -> {
        IPAMelee component = IPAWeaponsProvider.getMeleeComponentCommon(ctx.getPlayer());
        if (component != null) component.clickSecondary(stack, ctx);
    };

    public static void doSwing(ItemStack stack, LambdaContext ctx, GunAnimation anim, int cooldown) {

        Player player = ctx.getPlayer();
        int index = ctx.configIndex;
        GunState state = ItemGunBaseNT.getState(stack, index);

        if (state == GunState.IDLE) {
            ItemGunBaseNT.playAnimation(player, stack, anim, ctx.configIndex);
            ItemGunBaseNT.setState(stack, index, GunState.COOLDOWN);
            ItemGunBaseNT.setTimer(stack, index, cooldown);
        }
    }

    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_CLICK_RANGED_PRIMARY = (stack, ctx) -> {
        IPARanged component = IPAWeaponsProvider.getRangedComponentCommon(ctx.getPlayer());
        if (component != null) component.clickPrimary(stack, ctx);
    };
    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_CLICK_RANGED_SENONDARY = (stack, ctx) -> {
        IPARanged component = IPAWeaponsProvider.getRangedComponentCommon(ctx.getPlayer());
        if (component != null) component.clickSecondary(stack, ctx);
    };

    public static class ItemGunPA extends ItemGunBaseNT {

        public ItemGunPA(WeaponQuality quality, GunConfig... cfg) {
            super(quality, cfg);
        }

        @Override
        //? if < 1.21.1 {
        public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) { }
        //?} else {
        /*public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext hbmTooltipCtx, List<Component> list, TooltipFlag flag) { }
        *///?}
    }
}
