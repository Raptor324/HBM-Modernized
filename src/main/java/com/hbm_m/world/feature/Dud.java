package com.hbm_m.world.feature;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import com.hbm_m.config.GeneralConfig;
import com.hbm_m.itempool.ItemPool;
import com.hbm_m.itempool.ItemPoolsComponent;
import com.hbm_m.itempool.ItemPoolsLegacy;
import com.hbm_m.itempool.ItemPoolsPile;
import com.hbm_m.itempool.ItemPoolsSingle;
import com.hbm_m.main.MainRegistry;
import com.hbm_m.util.ForgeDirection;
import com.hbm_m.util.Vec3NT;
import com.hbm_m.world.gen.legacy.L;
import com.hbm_m.world.gen.legacy.LB;
import com.hbm_m.world.gen.legacy.LMaterial;
import com.hbm_m.world.gen.legacy.MB;
import com.hbm_m.world.gen.legacy.MetaBlock;
import com.hbm_m.world.gen.legacy.VB;
import com.hbm_m.world.generator.DungeonToolbox;

import net.minecraft.world.Container;
import net.minecraft.world.level.LevelAccessor;





/**
 * 1:1 {@code com.hbm.world.feature.Dud} (Original-Weltgen-Klasse, zeilengetreu auf die Port-Hilfen {@code L}/{@code MB}/{@code VB} umgesetzt).
 */
public class Dud {
	
	protected LB[] GetValidSpawnBlocks() {
		return new LB[] {
			VB.grass,
			VB.dirt,
			VB.stone,
			VB.sand,
			VB.sandstone,
		};
	}

	public boolean LocationIsValidSpawn(LevelAccessor world, int x, int y, int z) {

		LB checkBlock = L.getBlock(world, x, y - 1, z);
		LB blockAbove = L.getBlock(world, x, y, z);
		LB blockBelow = L.getBlock(world, x, y - 2, z);

		for(LB i : GetValidSpawnBlocks()) {
			if(blockAbove != VB.air) {
				return false;
			}
			if(checkBlock == i) {
				return true;
			} else if(checkBlock == VB.snow_layer && blockBelow == i) {
				return true;
			} else if(checkBlock.getMaterial() == LMaterial.plants && blockBelow == i) {
				return true;
			}
		}
		return false;
	}

		public boolean generate(LevelAccessor world, Random rand, int x, int y, int z) {
		int i = rand.nextInt(1);

		if(i == 0) {
			generate_r0(world, rand, x, y, z);
		}

		return true;

	}

	public boolean generate_r0(LevelAccessor world, Random rand, int x, int y, int z) {
		if(!LocationIsValidSpawn(world, x, y, z))
			return false;

		L.setBlock(world, x, y, z, MB.crashed_balefire, rand.nextInt(4) /* EnumDudType: BALEFIRE, CONVENTIONAL, NUKE, SALTED */, 3);

		if(GeneralConfig.enableDebugMode)
			System.out.print("[Debug] Successfully spawned dud at " + x + " " + y + " " + z + "\n");
		return true;

	}
}
