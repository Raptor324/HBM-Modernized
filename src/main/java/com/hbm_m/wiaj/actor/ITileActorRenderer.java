package com.hbm_m.wiaj.actor;

import com.hbm_m.wiaj.VirtualWorld;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/**
 * Modern analog of {@code com.hbm.wiaj.actors.ITileActorRenderer} (1.7.10): a renderer
 * for a machine inside the WIAJ scene, reading its state from the actor data tag.
 */
public interface ITileActorRenderer {

	/** Draw inside the isometric scene transform. */
	void renderActor(GuiGraphics gui, VirtualWorld world, int ticks, float interp, CompoundTag data);
	/** Per fake-tick state update (animation integration lives here). */
	void updateActor(int ticks, CompoundTag data);
}
