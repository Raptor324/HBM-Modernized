package com.hbm_m.wiaj;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.wiaj.action.WiajAction;

/**
 * A scene is a simple sequence of tasks, every script can have multiple scenes
 * Scenes depend on each other, in order to rewind we'll have to re-init the playing field and FFW through all previous scenes
 * <p>
 * Port of {@code com.hbm.wiaj.JarScene} (1.7.10).
 */
public class WiajScene {

	public List<WiajAction> actions = new ArrayList<>();
	public WiajScript script;

	public int actionNumber = 0;
	public WiajAction currentAction; //the action that is currently happening
	public int currentActionStart = 0; //time in ticks since init

	public WiajScene(WiajScript script) {
		this.script = script;
	}

	public WiajScene add(WiajAction action) {

		if(this.currentAction == null)
			this.currentAction = action; //set first action

		this.actions.add(action);
		return this;
	}

	/** does the once per 50ms tick routine */
	public void tick() {

		if(this.currentAction == null) return;

		this.currentAction.act(script.world, this);

		int duration = this.currentAction.getDuration();

		if(this.currentActionStart + duration <= script.ticksElapsed) { //choose next action

			this.actionNumber++;
			this.currentActionStart = script.ticksElapsed;

			if(actionNumber < this.actions.size()) {
				this.currentAction = this.actions.get(actionNumber);
				tick();

			} else {
				this.currentAction = null;
			}
		}
	}

	public void reset() {
		for(WiajAction a : this.actions) {
			if(a instanceof com.hbm_m.wiaj.action.ActionCreateActor ca) {
				ca.reset();
			}
		}
		this.currentAction = this.actions.get(0);
		this.actionNumber = 0;
		this.currentActionStart = script.ticksElapsed;
	}
}
