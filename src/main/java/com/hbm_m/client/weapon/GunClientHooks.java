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
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Clientseite des SEDNA-Waffensystems (Original verteilt auf {@code ModEventHandlerClient}, {@code ModEventHandlerRenderer},
 * {@code HbmKeybinds}, {@code HbmAnimationPacket.Handler}): Animationsstart, Rueckstoss (pro Frame integriert), Sicht-FOV,
 * Fadenkreuz/Zielfernrohr/HUD, Erstperson-Darstellung, Unterdruecken von Angriff und Blockwahl mit gezogener Waffe.
 * Die Forge-Ereignisse leitet {@code GunClientEventsForge} hierher.
 */
public final class GunClientHooks {

    private GunClientHooks() { }

    /** Zeitstempel des letzten Frames fuer die framerate-unabhaengige Rueckstoss-Integration. */
    private static long lastFrameNanos = 0L;

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

    /**
     * Original {@code ModEventHandlerClient} (TickEvent.Phase.END, 1.7.10): Rueckstoss mit
     * Rueckfederung. Die Mathematik des Originals (Impuls auf Offset, exponentielles Decay,
     * Rebound-Feder) bleibt unveraendert, aber die Integration laeuft pro FRAME statt pro
     * Tick: ein 20-Hz-Schritt wirkt auf hohen FPS wie Stottern (gleiches Problem wie bei der
     * frueheren Bildschirm-Rueckstoss-Version). Wie {@link net.minecraft.world.entity.Entity#turn}
     * bei der Maussteuerung schreiben wir jeden Frame den Winkel auf {@code xRot}/{@code yRot}
     * UND {@code xRotO}/{@code yRotO} — dadurch greift der Kick ohne Lerp-Verzoegerung und die
     * Rueckfederung laeuft framerate-unabhaengig exponentiell. Die Projektile folgen dem Kick
     * wie im Original, da sich die echte Spielerrotation aendert.
     */
    public static void onRenderFrame() {
        Player player = clientPlayer();
        if (player == null) {
            lastFrameNanos = 0L;
            return;
        }

        long now = System.nanoTime();
        float dt;
        if (lastFrameNanos == 0L) {
            dt = 1.0F;
        } else {
            dt = Mth.clamp((now - lastFrameNanos) / 50_000_000F, 0.0F, 5.0F);
        }
        lastFrameNanos = now;

        if (!ModClothConfig.get().gunVisualRecoil) {
            ItemGunBaseNT.offsetVertical = 0;
            ItemGunBaseNT.offsetHorizontal = 0;
            ItemGunBaseNT.recoilVertical = 0;
            ItemGunBaseNT.recoilHorizontal = 0;
            return;
        }

        // Wie im Original: der Restimpuls wird pro Tick VOLLEUENDIG auf Rotation/Offset angewendet
        // und danach mit recoilDecay multipliziert. FPS-unabhaengige Aequivalent: pro Frame wird
        // r * (1-decay^dt)/(1-decay) angewendet (bei dt=1 exakt der Originalwert), danach r *= decay^dt.
        // Die Gesamtsumme (geometrische Reihe = kick/(1-decay)) ist damit von der Framerate unabhaengig.
        float decay = Mth.clamp(ItemGunBaseNT.recoilDecay, 0.0F, 0.999F);
        float decayStep = (float) Math.pow(decay, dt);
        float impulseScale = (1.0F - decayStep) / (1.0F - decay);
        // Nicht im Original: "Irre-Rueckstoss" multipliziert den Kick mit 10
        float crazy = ModClothConfig.get().gunCrazyRecoil ? 10.0F : 1.0F;
        float kickV = ItemGunBaseNT.recoilVertical * impulseScale * crazy;
        float kickH = ItemGunBaseNT.recoilHorizontal * impulseScale * crazy;

        float pitch = player.getXRot();
        float yaw = player.getYRot();
        ItemGunBaseNT.offsetVertical += kickV;
        ItemGunBaseNT.offsetHorizontal += kickH;
        pitch -= kickV;
        yaw -= kickH;

        ItemGunBaseNT.recoilVertical *= decayStep;
        ItemGunBaseNT.recoilHorizontal *= decayStep;

        // Rebound-Feder, ebenfalls framerate-unabhaengig (Original: offset *= 1-recoilRebound pro Tick)
        float rebound = 1.0F - (float) Math.pow(1.0F - ItemGunBaseNT.recoilRebound, dt);
        float dV = ItemGunBaseNT.offsetVertical * rebound;
        float dH = ItemGunBaseNT.offsetHorizontal * rebound;
        ItemGunBaseNT.offsetVertical -= dV;
        ItemGunBaseNT.offsetHorizontal -= dH;
        pitch += dV;
        yaw += dH;

        applyRotation(player, pitch, yaw);
    }

    /** Wie {@code Entity.turn}: Winkel auf aktuelle UND vorherige Rotation schreiben — kein Lerp-Nachziehen. */
    private static void applyRotation(Player player, float pitch, float yaw) {
        pitch = Mth.clamp(pitch, -90.0F, 90.0F);
        player.setXRot(pitch);
        player.setYRot(yaw);
        player.xRotO = pitch;
        player.yRotO = yaw;
    }

    public static boolean holdingGun() {
        Player player = clientPlayer();
        // Im Zuschauermodus gilt: kein Waffen-HUD, keine Erste-Person-Darstellung, kein Zoom.
        return player != null && !player.isSpectator() && player.getMainHandItem().getItem() instanceof ItemGunBaseNT;
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
        if (com.hbm_m.client.render.shader.ShaderCompatibilityDetector.isExternalShaderActive()
                && com.hbm_m.client.render.shader.IrisReflectionBridge.isAfterTranslucentPhase()) {
            return true;
        }
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
