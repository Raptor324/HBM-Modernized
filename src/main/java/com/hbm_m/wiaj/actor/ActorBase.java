package com.hbm_m.wiaj.actor;

import net.minecraft.nbt.CompoundTag;

/**
 * Very basic actor base class that holds NBT, not very useful on its own
 * <p>
 * Port of {@code com.hbm.wiaj.actors.ActorBase} (1.7.10).
 * <p>
 * Extensions over the original: the {@link #smoothPlace()} marker flags an actor for the
 * smooth crane-drop placement animation (see {@link ActorTileEntity}) - the 1.7.10 original
 * had actors pop in instantly.
 */
public abstract class ActorBase implements ISpecialActor {

	/** Data tag marking the actor for the smooth placement animation. */
	public static final String TAG_SMOOTH_PLACE = "smoothPlace";

	/** Default duration of the smooth placement animation, in ticks (0.6 s). */
	public static final int DEFAULT_SMOOTH_PLACE_TICKS = 12;

	protected CompoundTag data = new CompoundTag();

	/** Scene tick this actor was created at (set by ActionCreateActor), -1 = not placed yet. */
	protected int placedTick = -1;
	/** Smooth placement duration in ticks; 0 = use {@link #DEFAULT_SMOOTH_PLACE_TICKS}. */
	protected int smoothPlaceDuration = 0;

	/**
	 * Marks this actor to be placed smoothly: it lowers from above with a slight drift
	 * and a quick fade-in instead of popping into existence. Fluent, call right on the
	 * {@code new ActorTileEntity(...)} expression.
	 */
	public ActorBase smoothPlace() {
		return this.smoothPlace(0);
	}

	/** Same as {@link #smoothPlace()} with an explicit animation duration in ticks. */
	public ActorBase smoothPlace(int durationTicks) {
		this.data.putBoolean(TAG_SMOOTH_PLACE, true);
		if(durationTicks > 0) this.smoothPlaceDuration = durationTicks;
		return this;
	}

	public boolean isSmoothPlace() {
		return this.data.getBoolean(TAG_SMOOTH_PLACE);
	}

	public int getSmoothPlaceDuration() {
		return this.smoothPlaceDuration > 0 ? this.smoothPlaceDuration : DEFAULT_SMOOTH_PLACE_TICKS;
	}

	public int getPlacedTick() {
		return this.placedTick;
	}

	/** Called by {@code ActionCreateActor} when the actor enters the scene (also on rewind replay). */
	public void markPlaced(int sceneTick) {
		if(this.isSmoothPlace()) this.placedTick = sceneTick;
	}

	@Override
	public void setActorData(CompoundTag data) {
		this.data = data;
	}

	@Override
	public CompoundTag getActorData() {
		return this.data;
	}

	@Override
	public void setDataPoint(String tag, Object o) {
		if(o instanceof String) this.data.putString(tag, (String) o);
		if(o instanceof Integer) this.data.putInt(tag, (Integer) o);
		if(o instanceof Float) this.data.putFloat(tag, (Float) o);
		if(o instanceof Double) this.data.putDouble(tag, (Double) o);
		if(o instanceof Boolean) this.data.putBoolean(tag, (Boolean) o);
		if(o instanceof Byte) this.data.putByte(tag, (Byte) o);
		if(o instanceof Short) this.data.putShort(tag, (Short) o);
	}
}
