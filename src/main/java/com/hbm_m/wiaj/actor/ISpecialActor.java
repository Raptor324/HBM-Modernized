package com.hbm_m.wiaj.actor;

import com.hbm_m.wiaj.VirtualWorld;
import com.hbm_m.wiaj.WiajScene;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;

/**
 * Actors, anything that can receive ticks (for rendering movement for example) and renders on screen
 * Can be tile entity models, faux entities or tooltips
 * <p>
 * Port of {@code com.hbm.wiaj.actors.ISpecialActor} (1.7.10) — the draw methods take a
 * {@link GuiGraphics} in modern Minecraft.
 */
public interface ISpecialActor {

	/** Draws things in the foreground like text boxes */
	void drawForegroundComponent(GuiGraphics gui, int w, int h, int ticks, float interp);
	/** Draws things in the background, fitted to the world renderer like TESRs. The GuiGraphics pose is the isometric scene transform. */
	void drawBackgroundComponent(GuiGraphics gui, VirtualWorld world, int ticks, float interp);
	/** Update ticks to emulate serverside ticking */
	void updateActor(WiajScene scene);
	/** Sets the data object to the passed NBT */
	void setActorData(CompoundTag data);
	/** Auto-detects the passed object's type and sets the specified NBT tag */
	void setDataPoint(String tag, Object o);
	/** Returns the actor's NBT data container */
	default CompoundTag getActorData() { return new CompoundTag(); }
}
