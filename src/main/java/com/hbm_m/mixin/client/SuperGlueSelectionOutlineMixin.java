package com.hbm_m.mixin.client;

//? if forge || neoforge {
import com.hbm_m.compat.create.GlueOutlineCompat;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Super glue counterpart of {@link com.hbm_m.mixin.HoneyGlueSelectionExpansionMixin}: while super glue is held,
 * the whole HBM multiblock under the selection is outlined, not only the blocks inside the box.
 * The glued group itself is already expanded by {@link com.hbm_m.mixin.SuperGlueSelectionHelperMixin}.
 */
@Pseudo
@Mixin(targets = "com.simibubi.create.content.contraptions.glue.SuperGlueSelectionHandler")
public abstract class SuperGlueSelectionOutlineMixin {

    @Shadow
    private BlockPos firstPos;

    @Shadow
    private BlockPos hoveredPos;

    @Inject(method = "tick", at = @At("TAIL"), remap = false, require = 0)
    private void hbm_m$outlineFullMultiblock(CallbackInfo ci) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null || mc.player == null || hoveredPos == null) {
                return;
            }
            String held = mc.player.getMainHandItem().getItem().getClass().getName();
            if (!held.equals("com.simibubi.create.content.contraptions.glue.SuperGlueItem")) {
                return;
            }
            BlockPos p1 = firstPos != null ? firstPos : hoveredPos;
            BlockPos p2 = hoveredPos;
            AABB box = new AABB(
                    Math.min(p1.getX(), p2.getX()),
                    Math.min(p1.getY(), p2.getY()),
                    Math.min(p1.getZ(), p2.getZ()),
                    Math.max(p1.getX(), p2.getX()) + 1.0,
                    Math.max(p1.getY(), p2.getY()) + 1.0,
                    Math.max(p1.getZ(), p2.getZ()) + 1.0);
            GlueOutlineCompat.showMultiblockClusterIn(mc.level, box);
        } catch (Throwable ignored) {
            // The outline must never break glue selection itself.
        }
    }
}
//?}
