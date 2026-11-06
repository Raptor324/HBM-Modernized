package com.hbm_m.wiaj.actor;

import com.hbm_m.client.render.ClientRenderFlags;
import com.hbm_m.wiaj.WiajScene;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;

/**
 * Bridges a scene actor to an {@link ITileActorRenderer}.
 * <p>
 * Port of {@code com.hbm.wiaj.actors.ActorTileEntity} (1.7.10).
 * <p>
 * Extension over the original: actors flagged with {@link ActorBase#smoothPlace()} are
 * lowered into place instead of popping in - an ease-out vertical drop
 * ({@link #SMOOTH_DROP_HEIGHT} blocks above the final spot) with a slight sideways settle
 * ({@link #SMOOTH_DRIFT}) and a quick fade-in driven through
 * {@code ClientRenderFlags#setGuiFadeAlpha} (the machine {@code MachineBer}s render with
 * the regular fade pipeline). The drop pose composes with whatever pose the renderer
 * builds, so no renderer changes are needed.
 */
public class ActorTileEntity extends ActorBase {

	ITileActorRenderer renderer;

	/** Drop start height above the final position, in blocks. */
	static final float SMOOTH_DROP_HEIGHT = 2.0F;
	/**
	 * Portion of the animation spent fading in - deliberately very short. Alpha blending
	 * draws the machine double-sided and unsorted (the isometric scene renders with culling
	 * off, and neither the VBO nor the buffered path sorts translucent quads), so a long
	 * translucent phase shows the machine's internals. ~2 of 12 ticks is a blink, and the
	 * ease-out covers most of the drop within that window.
	 */
	static final float SMOOTH_FADE_PORTION = 0.15F;
	/**
	 * Sideways settle drift at the start of the drop, in blocks. A FIXED direction per
	 * actor, decaying quadratically - no oscillation: a sway cycle over the fall reads as
	 * an S-shaped path instead of a vertical lowering.
	 */
	static final float SMOOTH_DRIFT = 0.1F;

	public ActorTileEntity(ITileActorRenderer renderer) {
		this.renderer = renderer;
	}

	public ActorTileEntity(ITileActorRenderer renderer, CompoundTag data) {
		this(renderer);
		this.data = data;
	}

	@Override
	public void drawForegroundComponent(GuiGraphics gui, int w, int h, int ticks, float interp) { }

	@Override
	public void drawBackgroundComponent(GuiGraphics gui, com.hbm_m.wiaj.VirtualWorld world, int ticks, float interp) {
		float progress = smoothPlaceProgress(ticks, interp);
		if(progress >= 1F) {
			renderer.renderActor(gui, world, ticks, interp, data);
			return;
		}

		// ease-out cubic: fast fall, gentle landing; the drift is a fixed per-actor
		// direction decaying quadratically to zero at touchdown - pure vertical lowering
		// with a slight sideways settle, no oscillation
		float eased = 1F - (1F - progress) * (1F - progress) * (1F - progress);
		float settle = SMOOTH_DRIFT * (1F - eased) * (1F - eased);
		float angle = (this.hashCode() % 360) * ((float) Math.PI / 180F);

		gui.pose().pushPose();
		gui.pose().translate(
				(float) (Math.cos(angle) * settle),
				(1F - eased) * SMOOTH_DROP_HEIGHT,
				(float) (Math.sin(angle) * settle));

		float prevFade = ClientRenderFlags.guiFadeAlpha();
		ClientRenderFlags.setGuiFadeAlpha(progress / SMOOTH_FADE_PORTION);
		try {
			renderer.renderActor(gui, world, ticks, interp, data);
		} finally {
			ClientRenderFlags.setGuiFadeAlpha(prevFade);
		}
		gui.pose().popPose();
	}

	/** 0..1 progress of the smooth placement animation, 1 = settled (or not flagged). */
	private float smoothPlaceProgress(int ticks, float interp) {
		if(!this.isSmoothPlace() || this.placedTick < 0) return 1F;
		float elapsed = (ticks - this.placedTick) + interp;
		return Mth.clamp(elapsed / this.getSmoothPlaceDuration(), 0F, 1F);
	}

	@Override
	public void updateActor(WiajScene scene) {
		renderer.updateActor(scene.script.ticksElapsed, data);
	}
}
