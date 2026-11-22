package com.hbm_m.wiaj.action;

import com.hbm_m.wiaj.VirtualWorld;
import com.hbm_m.wiaj.WiajScene;

public class ActionSetZoom implements WiajAction {

	int time;
	double zoom;

	public ActionSetZoom(double zoom, int time) {
		this.zoom = zoom / (time + 1);
		this.time = time;
	}

	@Override
	public int getDuration() {
		return this.time;
	}

	@Override
	public void act(VirtualWorld world, WiajScene scene) {

		if(this.getDuration() == 0) {
			scene.script.lastZoom = scene.script.zoom = this.zoom;
		} else {
			scene.script.zoom += this.zoom;
		}
	}
}
