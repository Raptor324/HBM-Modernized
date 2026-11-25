package com.hbm_m.client.render.weapon.bridge;

import com.hbm_m.render.anim.AnimationEnums.GunAnimation;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Client-seitige SPI, mit der ein externer Mod (z.B. ein TACZ-Addon) die Darstellung einzelner
 * {@code ItemGunBaseNT}-Stacks uebernehmen kann, ohne dass HBM-Modernized irgend etwas ueber den
 * externen Mod weiss (Master Spec "PHASE 1: Multiloader ExternalGunRenderBridge SPI").
 *
 * <p>Der Inhaber des Sets ({@code register}) wird waehrend der Client-Initialisierung des externen
 * Mods gesetzt; {@link #get()} liefert danach nie {@code null}, sondern eine No-Op-Standard-
 * implementierung ({@link None}), solange nichts registriert ist. Dadurch ist das Verhalten ohne
 * registrierte Bruecke bitidentisch zum Verhalten vor der Bruecke.
 *
 * <p>Nur Vanilla- und HBM-Typen — keine Loader- (Forge/NeoForge) und keine externen Mod-Imports,
 * damit die Klasse auf beiden Stonecutter-Zielen (1.20.1-forge, 1.21.1-neoforge) kompiliert.
 */
public abstract class ExternalGunRenderBridge {

    /** Registrierte Bruecke; nie {@code null} (Standard: {@link None}). */
    private static ExternalGunRenderBridge current = None.INSTANCE;

    /** Liefert die registrierte Bruecke oder die No-Op-Standardimplementierung, falls keine registriert ist. */
    public static ExternalGunRenderBridge get() {
        return current;
    }

    /** Registriert die Bruecke des externen Mods; {@code null} setzt die No-Op-Standardimplementierung zurueck. */
    public static void register(ExternalGunRenderBridge bridge) {
        current = bridge == null ? None.INSTANCE : bridge;
    }

    /**
     * Liefert {@code true}, wenn der externe Mod fuer diesen HBM-Waffen-Stack ein gueltiges
     * Display/Modell besitzt und die Darstellungsverantwortung uebernimmt (Renderung, Fadenkreuz,
     * Zielfernrohr, FOV). Muss fuer beliebige Stacks (auch Nicht-Waffen) sicher {@code false} liefern.
     */
    public abstract boolean isHandled(ItemStack stack);

    /**
     * Liefert den externen BEWLR ({@code ItemBlockRenderTypes}-Ersatz, der im externen Mod
     * SBM-{@code IFPGeoItemRenderer} implementiert und bei {@code !isHandled(stack)} an den
     * {@code fallback} delegiert), oder den {@code fallback}, wenn keine Bruecke registriert ist.
     */
    public abstract BlockEntityWithoutLevelRenderer resolveCustomRenderer(BlockEntityWithoutLevelRenderer fallback);

    /**
     * Optionaler direkter Hook fuer {@code GunItemRenderer.renderByItem} (Third-Person, GUI, Boden,
     * Item-Rahmen). Liefert {@code true}, wenn der externe Mod den Stack in diesem Kontext gezeichnet
     * hat und HBM die eigene Darstellung ueberspringen soll.
     */
    public abstract boolean renderByItem(ItemStack stack, ItemDisplayContext ctx, PoseStack pose, MultiBufferSource buffers, int light, int overlay);

    /**
     * Wird von {@code GunClientHooks.handleAnimationPacket} gefeuert, wenn der Server
     * {@code HbmAnimationPacket} sendet ({@code CYCLE}, {@code CYCLE_DRY}, {@code RELOAD},
     * {@code RELOAD_CYCLE}, {@code RELOAD_END}, {@code JAMMED}, {@code INSPECT}, {@code EQUIP}).
     * Der externe Mod uebersetzt dies in seine eigene Animationspipeline.
     */
    public abstract void onGunAnimation(ItemStack stack, GunAnimation type, int receiverIndex, int gunIndex);

    /**
     * Liefert {@code true}, wenn {@code GunClientHooks.onRenderFrame} den nativen HBM-Kamerarueckstoss
     * ({@code recoilVertical}/{@code recoilHorizontal}) fuer diesen Stack ueberspringen soll, weil der
     * externe Mod seinen eigenen Kamerarueckstoss anwendet; {@code false} behaelt den HBM-Kick bei.
     */
    public abstract boolean shouldSuppressNativeRecoil(ItemStack stack);

    /**
     * Liefert {@code true}, wenn {@code WeaponRenderRegistry.bindBakedModels} fuer dieses Waffenmodell
     * keine nativen {@code WeaponPartVboRenderer}-VBOs vorkompilieren soll (VRAM-Ersparnis, solange der
     * externe Mod das Modell zeichnet). Musste fuer {@code casings.obj}, Granaten und Nicht-Waffen-
     * Modelle stets {@code false} liefern.
     */
    public abstract boolean shouldSkipNativeVboBake(ResourceLocation modelLocation);

    /** No-Op-Standard, aktiv bis ein externer Mod eine Bruecke registriert (Verhalten wie vor der Bruecke). */
    private static final class None extends ExternalGunRenderBridge {

        static final None INSTANCE = new None();

        private None() { }

        @Override
        public boolean isHandled(ItemStack stack) {
            return false;
        }

        @Override
        public BlockEntityWithoutLevelRenderer resolveCustomRenderer(BlockEntityWithoutLevelRenderer fallback) {
            return fallback;
        }

        @Override
        public boolean renderByItem(ItemStack stack, ItemDisplayContext ctx, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
            return false;
        }

        @Override
        public void onGunAnimation(ItemStack stack, GunAnimation type, int receiverIndex, int gunIndex) {
        }

        @Override
        public boolean shouldSuppressNativeRecoil(ItemStack stack) {
            return false;
        }

        @Override
        public boolean shouldSkipNativeVboBake(ResourceLocation modelLocation) {
            return false;
        }
    }
}
