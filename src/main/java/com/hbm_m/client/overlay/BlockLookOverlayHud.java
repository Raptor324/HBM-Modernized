package com.hbm_m.client.overlay;

import com.hbm_m.interfaces.ILookOverlay;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Loader-agnostic crosshair HUD hook for blocks implementing {@link ILookOverlay}.
 *
 * Forge: typically called from RenderGuiEvent.
 * Fabric: called from HudRenderCallback.
 */
public final class BlockLookOverlayHud {
    private BlockLookOverlayHud() {}

    public static void render(GuiGraphics guiGraphics) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) return;

        HitResult hr = mc.hitResult;
        Level level = mc.level;
        if (hr == null || level == null) return;

        if (hr.getType() != HitResult.Type.BLOCK) return;
        BlockHitResult bhr = (BlockHitResult) hr;

        if (level.getBlockState(bhr.getBlockPos()).getBlock() instanceof ILookOverlay ilo) {
            ilo.printHook(guiGraphics, level, bhr.getBlockPos());
        }
    }
}

