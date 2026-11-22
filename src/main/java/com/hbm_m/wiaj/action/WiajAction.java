package com.hbm_m.wiaj.action;

import com.hbm_m.wiaj.VirtualWorld;
import com.hbm_m.wiaj.WiajScene;

public interface WiajAction {

	/** Time taken by this action in ticks */
	int getDuration();
	/** Perform this action */
	void act(VirtualWorld world, WiajScene scene);
}
