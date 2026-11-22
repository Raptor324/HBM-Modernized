package com.hbm_m.wiaj.action;

import com.hbm_m.wiaj.VirtualWorld;
import com.hbm_m.wiaj.WiajScene;

public class ActionWait implements WiajAction {

	private int ticks;

	public ActionWait(int ticks) {
		this.ticks = ticks;
	}

	@Override
	public int getDuration() {
		return this.ticks;
	}

	@Override
	public void act(VirtualWorld world, WiajScene scene) { }
}
