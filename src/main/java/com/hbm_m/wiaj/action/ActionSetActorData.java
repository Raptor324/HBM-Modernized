package com.hbm_m.wiaj.action;

import com.hbm_m.wiaj.VirtualWorld;
import com.hbm_m.wiaj.WiajScene;

import net.minecraft.nbt.CompoundTag;

public class ActionSetActorData implements WiajAction {

	int id;
	CompoundTag data;

	public ActionSetActorData(int id, CompoundTag data) {
		this.id = id;
		this.data = data;
	}

	@Override
	public int getDuration() {
		return 0;
	}

	@Override
	public void act(VirtualWorld world, WiajScene scene) {
		scene.script.actors.get(id).setActorData(data.copy());
	}
}
