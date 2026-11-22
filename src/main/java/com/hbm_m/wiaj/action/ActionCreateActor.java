package com.hbm_m.wiaj.action;

import com.hbm_m.wiaj.VirtualWorld;
import com.hbm_m.wiaj.WiajScene;
import com.hbm_m.wiaj.actor.ISpecialActor;

import net.minecraft.nbt.CompoundTag;

public class ActionCreateActor implements WiajAction {

	int id;
	ISpecialActor actor;
	CompoundTag initialData;

	public ActionCreateActor(int id, ISpecialActor actor) {
		this.id = id;
		this.actor = actor;
		CompoundTag d = actor.getActorData();
		this.initialData = d != null ? d.copy() : null;
	}

	public void reset() {
		if (this.initialData != null) {
			this.actor.setActorData(this.initialData.copy());
		}
	}

	@Override
	public int getDuration() {
		return 0;
	}

	@Override
	public void act(VirtualWorld world, WiajScene scene) {
		if (this.initialData != null) {
			this.actor.setActorData(this.initialData.copy());
		}
		// after the data restore, so the smooth-placement flag (captured in initialData)
		// is what decides; the tick anchors the drop animation (see ActorTileEntity)
		if (this.actor instanceof com.hbm_m.wiaj.actor.ActorBase ab) {
			ab.markPlaced(scene.script.ticksElapsed);
		}
		scene.script.actors.put(id, actor);
	}
}
