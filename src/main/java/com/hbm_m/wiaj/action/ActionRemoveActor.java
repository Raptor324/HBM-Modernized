package com.hbm_m.wiaj.action;

import com.hbm_m.wiaj.VirtualWorld;
import com.hbm_m.wiaj.WiajScene;

public class ActionRemoveActor implements WiajAction {

	int id;

	public ActionRemoveActor(int id) {
		this.id = id;
	}

	@Override
	public int getDuration() {
		return 0;
	}

	@Override
	public void act(VirtualWorld world, WiajScene scene) {
		scene.script.actors.remove(id);
	}
}
