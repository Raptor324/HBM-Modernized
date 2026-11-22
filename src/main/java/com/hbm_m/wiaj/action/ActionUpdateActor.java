package com.hbm_m.wiaj.action;

import com.hbm_m.wiaj.VirtualWorld;
import com.hbm_m.wiaj.WiajScene;

public class ActionUpdateActor implements WiajAction {

	int id;
	String key;
	Object data;

	public ActionUpdateActor(int id, String key, Object data) {
		this.id = id;
		this.key = key;
		this.data = data;
	}

	@Override
	public int getDuration() {
		return 0;
	}

	@Override
	public void act(VirtualWorld world, WiajScene scene) {
		com.hbm_m.wiaj.actor.ISpecialActor actor = scene.script.actors.get(id);
		if (actor != null) {
			actor.setDataPoint(key, data);
		}
	}
}
