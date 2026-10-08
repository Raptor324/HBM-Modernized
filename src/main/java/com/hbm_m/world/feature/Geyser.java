//Schematic to java Structure by jajo_11 | inspired by "MITHION'S .SCHEMATIC TO JAVA CONVERTINGTOOL"

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
 * 1:1 {@code com.hbm.world.feature.Geyser} (Original-Weltgen-Klasse, zeilengetreu auf die Port-Hilfen {@code L}/{@code MB}/{@code VB} umgesetzt).
 */
public class Geyser {

	public boolean generate(LevelAccessor world, Random rand, int x, int y, int z) {
		int i = rand.nextInt(1);

		if (i == 0) {
			generate_r0(world, rand, x, y, z);
		}

		return true;

	}

	public boolean generate_r0(LevelAccessor world, Random rand, int x, int y, int z) {

		x -= 2;
		y -= 11;
		z -= 2;
		L.setBlock(world, x + 1, y + 5, z + 0, VB.stone, 0, 3);
		L.setBlock(world, x + 2, y + 5, z + 0, VB.stone, 0, 3);
		L.setBlock(world, x + 3, y + 5, z + 0, VB.stone, 0, 3);
		L.setBlock(world, x + 0, y + 5, z + 1, VB.stone, 0, 3);
		L.setBlock(world, x + 1, y + 5, z + 1, VB.stone, 0, 3);
		L.setBlock(world, x + 2, y + 5, z + 1, VB.stone, 0, 3);
		L.setBlock(world, x + 3, y + 5, z + 1, VB.stone, 0, 3);
		L.setBlock(world, x + 4, y + 5, z + 1, VB.stone, 0, 3);
		L.setBlock(world, x + 0, y + 5, z + 2, VB.stone, 0, 3);
		L.setBlock(world, x + 1, y + 5, z + 2, VB.stone, 0, 3);
		L.setBlock(world, x + 2, y + 5, z + 2, VB.stone, 0, 3);
		L.setBlock(world, x + 3, y + 5, z + 2, VB.stone, 0, 3);
		L.setBlock(world, x + 4, y + 5, z + 2, VB.stone, 0, 3);
		L.setBlock(world, x + 0, y + 5, z + 3, VB.stone, 0, 3);
		L.setBlock(world, x + 1, y + 5, z + 3, VB.stone, 0, 3);
		L.setBlock(world, x + 2, y + 5, z + 3, VB.stone, 0, 3);
		L.setBlock(world, x + 3, y + 5, z + 3, VB.stone, 0, 3);
		L.setBlock(world, x + 4, y + 5, z + 3, VB.stone, 0, 3);
		L.setBlock(world, x + 1, y + 5, z + 4, VB.stone, 0, 3);
		L.setBlock(world, x + 2, y + 5, z + 4, VB.stone, 0, 3);
		L.setBlock(world, x + 3, y + 5, z + 4, VB.stone, 0, 3);
		L.setBlock(world, x + 1, y + 6, z + 0, VB.stone, 0, 3);
		L.setBlock(world, x + 2, y + 6, z + 0, VB.stone, 0, 3);
		L.setBlock(world, x + 3, y + 6, z + 0, VB.stone, 0, 3);
		L.setBlock(world, x + 0, y + 6, z + 1, VB.stone, 0, 3);
		L.setBlock(world, x + 1, y + 6, z + 1, VB.stone, 0, 3);
		L.setBlock(world, x + 2, y + 6, z + 1, VB.stone, 0, 3);
		L.setBlock(world, x + 3, y + 6, z + 1, VB.stone, 0, 3);
		L.setBlock(world, x + 4, y + 6, z + 1, VB.stone, 0, 3);
		L.setBlock(world, x + 0, y + 6, z + 2, VB.stone, 0, 3);
		L.setBlock(world, x + 1, y + 6, z + 2, VB.stone, 0, 3);
		L.setBlock(world, x + 2, y + 6, z + 2, VB.stone, 0, 3);
		L.setBlock(world, x + 3, y + 6, z + 2, VB.stone, 0, 3);
		L.setBlock(world, x + 4, y + 6, z + 2, VB.stone, 0, 3);
		L.setBlock(world, x + 0, y + 6, z + 3, VB.stone, 0, 3);
		L.setBlock(world, x + 1, y + 6, z + 3, VB.stone, 0, 3);
		L.setBlock(world, x + 2, y + 6, z + 3, VB.stone, 0, 3);
		L.setBlock(world, x + 3, y + 6, z + 3, VB.stone, 0, 3);
		L.setBlock(world, x + 4, y + 6, z + 3, VB.stone, 0, 3);
		L.setBlock(world, x + 1, y + 6, z + 4, VB.stone, 0, 3);
		L.setBlock(world, x + 2, y + 6, z + 4, VB.stone, 0, 3);
		L.setBlock(world, x + 3, y + 6, z + 4, VB.stone, 0, 3);
		L.setBlock(world, x + 1, y + 7, z + 0, VB.stone, 0, 3);
		L.setBlock(world, x + 2, y + 7, z + 0, VB.stone, 0, 3);
		L.setBlock(world, x + 3, y + 7, z + 0, VB.stone, 0, 3);
		L.setBlock(world, x + 0, y + 7, z + 1, VB.stone, 0, 3);
		L.setBlock(world, x + 1, y + 7, z + 1, VB.water, 0, 3);
		L.setBlock(world, x + 2, y + 7, z + 1, MB.block_yellowcake, 0, 3);
		L.setBlock(world, x + 3, y + 7, z + 1, VB.water, 0, 3);
		L.setBlock(world, x + 4, y + 7, z + 1, VB.stone, 0, 3);
		L.setBlock(world, x + 0, y + 7, z + 2, VB.stone, 0, 3);
		L.setBlock(world, x + 1, y + 7, z + 2, MB.block_yellowcake, 0, 3);
		L.setBlock(world, x + 2, y + 7, z + 2, VB.water, 0, 3);
		L.setBlock(world, x + 3, y + 7, z + 2, VB.water, 0, 3);
		L.setBlock(world, x + 4, y + 7, z + 2, VB.stone, 0, 3);
		L.setBlock(world, x + 0, y + 7, z + 3, VB.stone, 0, 3);
		L.setBlock(world, x + 1, y + 7, z + 3, VB.water, 0, 3);
		L.setBlock(world, x + 2, y + 7, z + 3, MB.block_yellowcake, 0, 3);
		L.setBlock(world, x + 3, y + 7, z + 3, MB.block_yellowcake, 0, 3);
		L.setBlock(world, x + 4, y + 7, z + 3, VB.stone, 0, 3);
		L.setBlock(world, x + 1, y + 7, z + 4, VB.stone, 0, 3);
		L.setBlock(world, x + 2, y + 7, z + 4, VB.stone, 0, 3);
		L.setBlock(world, x + 3, y + 7, z + 4, VB.stone, 0, 3);
		L.setBlock(world, x + 1, y + 8, z + 0, VB.stone, 0, 3);
		L.setBlock(world, x + 2, y + 8, z + 0, VB.stone, 0, 3);
		L.setBlock(world, x + 3, y + 8, z + 0, VB.stone, 0, 3);
		L.setBlock(world, x + 0, y + 8, z + 1, VB.stone, 0, 3);
		L.setBlock(world, x + 1, y + 8, z + 1, VB.air, 0, 3);
		L.setBlock(world, x + 2, y + 8, z + 1, VB.air, 0, 3);
		L.setBlock(world, x + 3, y + 8, z + 1, VB.air, 0, 3);
		L.setBlock(world, x + 4, y + 8, z + 1, VB.stone, 0, 3);
		L.setBlock(world, x + 0, y + 8, z + 2, VB.stone, 0, 3);
		L.setBlock(world, x + 1, y + 8, z + 2, VB.air, 0, 3);
		L.setBlock(world, x + 2, y + 8, z + 2, VB.air, 0, 3);
		L.setBlock(world, x + 3, y + 8, z + 2, VB.air, 0, 3);
		L.setBlock(world, x + 4, y + 8, z + 2, VB.stone, 0, 3);
		L.setBlock(world, x + 0, y + 8, z + 3, VB.stone, 0, 3);
		L.setBlock(world, x + 1, y + 8, z + 3, VB.air, 0, 3);
		L.setBlock(world, x + 2, y + 8, z + 3, VB.air, 0, 3);
		L.setBlock(world, x + 3, y + 8, z + 3, VB.air, 0, 3);
		L.setBlock(world, x + 4, y + 8, z + 3, VB.stone, 0, 3);
		L.setBlock(world, x + 1, y + 8, z + 4, VB.stone, 0, 3);
		L.setBlock(world, x + 2, y + 8, z + 4, VB.stone, 0, 3);
		L.setBlock(world, x + 3, y + 8, z + 4, VB.stone, 0, 3);
		L.setBlock(world, x + 1, y + 9, z + 0, VB.stone, 0, 3);
		L.setBlock(world, x + 2, y + 9, z + 0, VB.stone, 0, 3);
		L.setBlock(world, x + 3, y + 9, z + 0, VB.stone, 0, 3);
		L.setBlock(world, x + 0, y + 9, z + 1, VB.stone, 0, 3);
		L.setBlock(world, x + 1, y + 9, z + 1, VB.stone, 0, 3);
		L.setBlock(world, x + 3, y + 9, z + 1, VB.stone, 0, 3);
		L.setBlock(world, x + 4, y + 9, z + 1, VB.stone, 0, 3);
		L.setBlock(world, x + 0, y + 9, z + 2, VB.stone, 0, 3);
		L.setBlock(world, x + 1, y + 9, z + 2, VB.air, 0, 3);
		L.setBlock(world, x + 2, y + 9, z + 2, VB.air, 0, 3);
		L.setBlock(world, x + 3, y + 9, z + 2, VB.air, 0, 3);
		L.setBlock(world, x + 4, y + 9, z + 2, VB.stone, 0, 3);
		L.setBlock(world, x + 0, y + 9, z + 3, VB.stone, 0, 3);
		L.setBlock(world, x + 1, y + 9, z + 3, VB.stone, 0, 3);
		L.setBlock(world, x + 3, y + 9, z + 3, VB.stone, 0, 3);
		L.setBlock(world, x + 4, y + 9, z + 3, VB.stone, 0, 3);
		L.setBlock(world, x + 1, y + 9, z + 4, VB.stone, 0, 3);
		L.setBlock(world, x + 2, y + 9, z + 4, VB.stone, 0, 3);
		L.setBlock(world, x + 3, y + 9, z + 4, VB.stone, 0, 3);
		L.setBlock(world, x + 1, y + 10, z + 0, VB.grass, 0, 3);
		L.setBlock(world, x + 2, y + 10, z + 0, VB.grass, 0, 3);
		L.setBlock(world, x + 3, y + 10, z + 0, VB.grass, 0, 3);
		L.setBlock(world, x + 0, y + 10, z + 1, VB.grass, 0, 3);
		L.setBlock(world, x + 1, y + 10, z + 1, VB.gravel, 0, 3);
		L.setBlock(world, x + 2, y + 10, z + 1, VB.stone, 0, 3);
		L.setBlock(world, x + 3, y + 10, z + 1, VB.grass, 0, 3);
		L.setBlock(world, x + 4, y + 10, z + 1, VB.stone, 0, 3);
		L.setBlock(world, x + 0, y + 10, z + 2, VB.stone, 0, 3);
		L.setBlock(world, x + 1, y + 10, z + 2, VB.grass, 0, 3);
		L.setBlock(world, x + 2, y + 10, z + 2, MB.geysir_chlorine, 0, 3);
		L.setBlock(world, x + 3, y + 10, z + 2, VB.grass, 0, 3);
		L.setBlock(world, x + 4, y + 10, z + 2, VB.gravel, 0, 3);
		L.setBlock(world, x + 0, y + 10, z + 3, VB.grass, 0, 3);
		L.setBlock(world, x + 1, y + 10, z + 3, VB.stone, 0, 3);
		L.setBlock(world, x + 2, y + 10, z + 3, VB.grass, 0, 3);
		L.setBlock(world, x + 3, y + 10, z + 3, VB.gravel, 0, 3);
		L.setBlock(world, x + 4, y + 10, z + 3, VB.grass, 0, 3);
		L.setBlock(world, x + 1, y + 10, z + 4, VB.grass, 0, 3);
		L.setBlock(world, x + 2, y + 10, z + 4, VB.grass, 0, 3);
		L.setBlock(world, x + 3, y + 10, z + 4, VB.grass, 0, 3);
		return true;

	}

}