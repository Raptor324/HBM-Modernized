package com.hbm_m.wiaj.action;

import com.hbm_m.wiaj.VirtualWorld;
import com.hbm_m.wiaj.WiajScene;

import net.minecraft.world.level.block.entity.BlockEntity;

public class ActionSetTile implements WiajAction {

	int x;
	int y;
	int z;
	BlockEntity tile;

	public ActionSetTile(int x, int y, int z, BlockEntity tile) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.tile = tile;
	}

	@Override
	public int getDuration() {
		return 0;
	}

	@Override
	public void act(VirtualWorld world, WiajScene scene) {
		world.setTileEntity(x, y, z, tile);
	}
}
