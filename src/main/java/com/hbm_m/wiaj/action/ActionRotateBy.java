package com.hbm_m.wiaj.action;

import com.hbm_m.wiaj.VirtualWorld;
import com.hbm_m.wiaj.WiajScene;

/**
 * Static action for rotating the scene around
 * To rotate the scene along with another action, create a special actor that rotates the scene
 * <p>
 * Port of {@code com.hbm.wiaj.actions.ActionRotateBy} (1.7.10).
 */
public class ActionRotateBy implements WiajAction {

	int time;
	double velYaw;
	double velPitch;

	public ActionRotateBy(double yaw, double pitch, int time) {
		this.velYaw = yaw / (time + 1);
		this.velPitch = pitch / (time + 1);
		this.time = time;
	}

	@Override
	public int getDuration() {
		return this.time;
	}

	@Override
	public void act(VirtualWorld world, WiajScene scene) {
		scene.script.rotationPitch += this.velPitch;
		scene.script.rotationYaw += this.velYaw;
	}
}
