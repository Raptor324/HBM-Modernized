package com.hbm_m.client.weapon;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.config.ModClothConfig;
import com.hbm_m.item.weapon.sedna.GunConfig;
import com.hbm_m.item.weapon.sedna.ItemGunBaseNT;
import com.hbm_m.item.weapon.sedna.ItemGunBaseNT.LambdaContext;
import com.hbm_m.item.weapon.sedna.Receiver;
import com.hbm_m.item.weapon.sedna.hud.IHUDComponent;
import com.hbm_m.powerarmor.ArmorTrenchmaster;
import com.hbm_m.render.anim.AnimationEnums.GunAnimation;
import com.hbm_m.render.anim.BusAnimation;
import com.hbm_m.render.anim.HbmAnimations;
import com.hbm_m.render.anim.HbmAnimations.Animation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Clientseite des SEDNA-Waffensystems (Original verteilt auf {@code ModEventHandlerClient}, {@code ModEventHandlerRenderer},
 * {@code HbmKeybinds}, {@code HbmAnimationPacket.Handler}): Animationsstart, Rueckstoss am Tickende, Sicht-FOV,
 * Fadenkreuz/Zielfernrohr/HUD, Erstperson-Darstellung, Unterdruecken von Angriff und Blockwahl mit gezogener Waffe.
 * Die Forge-Ereignisse leitet {@code GunClientEventsForge} hierher.
 */
public final class GunClientHooks {

    private GunClientHooks() { }

    @Nullable
    public static Player clientPlayer() {
        return Minecraft.getInstance().player;
    }

    /** Original: {@code currentScreen instanceof GUIWeaponTable}. */
    public static boolean isWeaponTableOpen() {
        var screen = Minecraft.getInstance().screen;
        return screen != null && screen.getClass().getSimpleName().equals("GUIWeaponTable");
    }

    /** Original {@code HbmAnimationPacket.Handler.handleSedna}. */
    public static void handleAnimationPacket(int typeOrdinal, int receiverIndex, int gunIndex) {
        Player player = clientPlayer();
        if (player == null) return;
        ItemStack stack = player.getMainHandItem();
        int slot = player.getInventory().selected;
        if (!stack.isEmpty() && stack.getItem() instanceof com.hbm_m.item.IAnimatedItem<?> animated) {
            handleAnimatedItem(animated, stack, slot, typeOrdinal, gunIndex);
            return;
        }
        if (stack.isEmpty() || !(stack.getItem() instanceof ItemGunBaseNT gun)) return;
        if (typeOrdinal < 0 || typeOrdinal >= GunAnimation.values().length) return;
        GunAnimation type = GunAnimation.values()[typeOrdinal];

        try {
            GunConfig config = gun.getConfig(stack, gunIndex);

            if (type == GunAnimation.CYCLE) {
                if (gunIndex < gun.lastShot.length) gun.lastShot[gunIndex] = System.currentTimeMillis();
                gun.shotRand = player.level().random.nextDouble();
                Receiver[] receivers = config.getReceivers(stack);
                if (receiverIndex >= 0 && receiverIndex < receivers.length) {
                    Receiver rec = receivers[receiverIndex];
                    BiConsumer<ItemStack, LambdaContext> onRecoil = rec.getRecoil(stack);
                    if (onRecoil != null) onRecoil.accept(stack, new LambdaContext(config, player, player.getInventory(), receiverIndex));
                }
            }

            BiFunction<ItemStack, GunAnimation, BusAnimation> anims = config.getAnims(stack);
            if (anims == null) return;
            BusAnimation animation = anims.apply(stack, type);

            if (animation == null && (type == GunAnimation.ALT_CYCLE || type == GunAnimation.CYCLE_EMPTY)) {
                animation = anims.apply(stack, GunAnimation.CYCLE);
            }

            if (animation != null) {
                Minecraft.getInstance().gameRenderer.itemInHandRenderer.itemUsed(net.minecraft.world.InteractionHand.MAIN_HAND);
                boolean isReloadAnimation = type == GunAnimation.RELOAD || type == GunAnimation.RELOAD_CYCLE;
                if (isReloadAnimation && ArmorTrenchmaster.isTrenchMaster(player)) animation.setTimeMult(0.5D);
                if (slot >= 0 && slot < 9 && gunIndex < 8)
                    HbmAnimations.hotbar[slot][gunIndex] = new Animation(HbmAnimations.keyOf(stack), System.currentTimeMillis(), animation, isReloadAnimation && config.getReloadAnimSequential(stack));
            }
        } catch (Exception ignored) { }
    }

    /** Original {@code HbmAnimationPacket.Handler.handleItem} fuer {@link com.hbm_m.item.IAnimatedItem}. */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static void handleAnimatedItem(com.hbm_m.item.IAnimatedItem item, ItemStack stack, int slot, int type, int itemIndex) {
        Enum<?>[] values = (Enum<?>[]) item.getEnum().getEnumConstants();
        if (values == null || values.length == 0) return;
        Enum<?> e = values[Math.abs(type) % values.length];
        BusAnimation animation = item.getAnimation(e, stack);
        if (animation != null && slot >= 0 && slot < 9 && itemIndex >= 0 && itemIndex < 8) {
            HbmAnimations.hotbar[slot][itemIndex] = new Animation(HbmAnimations.keyOf(stack), System.currentTimeMillis(), animation);
        }
    }

    /** Original {@code ModEventHandlerClient} (Tickende): sichtbarer Rueckstoss mit Rueckfederung. */
    public static void onClientTickEnd() {
        Player player = clientPlayer();
        if (player == null) return;

        if (ModClothConfig.get().gunVisualRecoil) {
            ItemGunBaseNT.offsetVertical += ItemGunBaseNT.recoilVertical;
            ItemGunBaseNT.offsetHorizontal += ItemGunBaseNT.recoilHorizontal;
            player.setXRot(player.getXRot() - ItemGunBaseNT.recoilVertical);
            player.setYRot(player.getYRot() - ItemGunBaseNT.recoilHorizontal);

            ItemGunBaseNT.recoilVertical *= ItemGunBaseNT.recoilDecay;
            ItemGunBaseNT.recoilHorizontal *= ItemGunBaseNT.recoilDecay;
            float dV = ItemGunBaseNT.offsetVertical * ItemGunBaseNT.recoilRebound;
            float dH = ItemGunBaseNT.offsetHorizontal * ItemGunBaseNT.recoilRebound;

            ItemGunBaseNT.offsetVertical -= dV;
            ItemGunBaseNT.offsetHorizontal -= dH;
            player.setXRot(player.getXRot() + dV);
            player.setYRot(player.getYRot() + dH);
        } else {
            ItemGunBaseNT.offsetVertical = 0;
            ItemGunBaseNT.offsetHorizontal = 0;
            ItemGunBaseNT.recoilVertical = 0;
            ItemGunBaseNT.recoilHorizontal = 0;
        }
    }

    public static boolean holdingGun() {
        Player player = clientPlayer();
        return player != null && player.getMainHandItem().getItem() instanceof ItemGunBaseNT;
    }

    /** Original {@code setupFOV}: der Renderer darf das Sichtfeld aendern (Zielfernrohr-Zoom). */
    public static float modifyFov(float fov) {
        Player player = clientPlayer();
        if (player == null) return fov;
        ItemStack held = player.getMainHandItem();
        ItemRenderWeaponBase renderer = GunItemRenderer.get(held);
        return renderer != null ? renderer.getViewFOV(held, fov) : fov;
    }

    /** Original {@code onRenderHand}: Erstperson zeichnet der Waffen-Renderer; true = Vanilla-Hand abbrechen. */
    public static boolean renderHand(float partialTicks) {
        Player player = clientPlayer();
        if (player == null) return false;
        ItemStack toRender = player.getMainHandItem();
        ItemRenderWeaponBase renderer = GunItemRenderer.get(toRender);
        if (renderer == null || !renderer.customFirstPerson()) return false;
        renderer.setPerspectiveAndRender(toRender, partialTicks, Minecraft.getInstance().renderBuffers().bufferSource());
        return true;
    }

    /** Original {@code renderHUD(CROSSHAIRS)}: true = Vanilla-Fadenkreuz abbrechen. */
    public static boolean renderCrosshair(GuiGraphics g) {
        Player player = clientPlayer();
        if (player == null) return false;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof ItemGunBaseNT gun)) return false;
        GunConfig config = gun.getConfig(stack, 0);
        if (config.getHideCrosshair(stack) && ItemGunBaseNT.aimingProgress >= 1F) return true;
        GunHud.renderCustomCrosshairs(g, config.getCrosshair(stack));
        return true;
    }

    /** Original {@code renderHUD(HOTBAR)} + Zielfernrohr-Overlay. */
    public static void renderHotbarExtras(GuiGraphics g) {
        Player player = clientPlayer();
        if (player == null) return;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof ItemGunBaseNT gun)) return;

        if (ItemGunBaseNT.aimingProgress == ItemGunBaseNT.prevAimingProgress && ItemGunBaseNT.aimingProgress == 1F) {
            GunConfig cfg = gun.getConfig(stack, 0);
            if (cfg.getScopeTexture(stack) != null) GunHud.renderScope(g, cfg.getScopeTexture(stack));
        }

        int confNo = gun.getConfigCount();
        for (int i = 0; i < confNo; i++) {
            IHUDComponent[] components = gun.getConfig(stack, i).getHUDComponents(stack);
            int bottomOffset = 0;
            if (components != null) for (IHUDComponent component : components) {
                component.renderHUDComponent(g, player, stack, bottomOffset, i);
                bottomOffset += component.getComponentHeight(player, stack);
            }
        }
    }
}
