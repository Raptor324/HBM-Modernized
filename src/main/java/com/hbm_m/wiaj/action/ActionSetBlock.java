package com.hbm_m.wiaj.action;

import com.hbm_m.wiaj.VirtualWorld;
import com.hbm_m.wiaj.WiajScene;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Simple action that places one block instantly with no delay
 * <p>
 * Port of {@code com.hbm.wiaj.actions.ActionSetBlock} (1.7.10): the block+meta pair
 * becomes a single {@link BlockState}.
 */
public class ActionSetBlock implements WiajAction {

	int x;
	int y;
	int z;
	BlockState state;

	public ActionSetBlock(int x, int y, int z, Block block) {
		this(x, y, z, block.defaultBlockState());
	}

	public ActionSetBlock(int x, int y, int z, BlockState state) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.state = state;
	}

	@Override
	public void act(VirtualWorld world, WiajScene scene) {
		world.setBlock(x, y, z, state);
	}

	@Override
	public int getDuration() {
		return 0;
	}
}
