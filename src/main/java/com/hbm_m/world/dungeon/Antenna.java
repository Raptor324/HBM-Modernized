//Schematic to java Structure by jajo_11 | inspired by "MITHION'S .SCHEMATIC TO JAVA CONVERTINGTOOL"

package com.hbm_m.world.dungeon;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import com.hbm_m.config.GeneralConfig;
import com.hbm_m.itempool.ItemPool;
import com.hbm_m.main.MainRegistry;
import com.hbm_m.itempool.ItemPoolsComponent;
import com.hbm_m.itempool.ItemPoolsLegacy;
import com.hbm_m.itempool.ItemPoolsSingle;
import com.hbm_m.itempool.ItemPoolsPile;
import com.hbm_m.world.gen.legacy.L;
import com.hbm_m.world.gen.legacy.LB;
import com.hbm_m.world.gen.legacy.LMaterial;
import com.hbm_m.world.gen.legacy.MB;
import com.hbm_m.world.gen.legacy.MetaBlock;
import com.hbm_m.world.generator.DungeonToolbox;
import com.hbm_m.util.Vec3NT;
import com.hbm_m.world.gen.legacy.VB;

import net.minecraft.world.Container;
import net.minecraft.world.level.LevelAccessor;




/**
 * 1:1 {@code com.hbm.world.dungeon.Antenna} (Original-Weltgen-Klasse, zeilengetreu auf die Port-Hilfen {@code L}/{@code MB}/{@code VB} umgesetzt).
 */
public class Antenna {
	protected LB[] GetValidSpawnBlocks()
	{
		return new LB[]
		{
			VB.grass,
			VB.dirt,
			VB.stone,
			VB.sand,
		};
	}

	public boolean LocationIsValidSpawn(LevelAccessor world, int x, int y, int z)
 {

		LB checkBlock = L.getBlock(world, x, y - 1, z);
		LB blockAbove = L.getBlock(world, x, y , z);
		LB blockBelow = L.getBlock(world, x, y - 2, z);

		for (LB i : GetValidSpawnBlocks())
		{
			if (blockAbove != VB.air)
			{
				return false;
			}
			if (checkBlock == i)
			{
				return true;
			}
			else if (checkBlock == VB.snow_layer && blockBelow == i)
			{
				return true;
			}
			else if (checkBlock.getMaterial() == LMaterial.plants && blockBelow == i)
			{
				return true;
			}
		}
		return false;
	}

		public boolean generate(LevelAccessor world, Random rand, int x, int y, int z)
	{
		int i = rand.nextInt(1);

		if(i == 0)
		{
		    generate_r0(world, rand, x, y, z);
		}

       return true;

	}

	public boolean generate_r0(LevelAccessor world, Random rand, int x, int y, int z)
	{
		if(!LocationIsValidSpawn(world, x + 1, y, z + 1))
		{
			return false;
		}

		L.setBlock(world, x + 0, y + 0, z + 0, VB.air, 0, 3);
		L.setBlock(world, x + 1, y + 0, z + 0, MB.steel_poles, 2, 3);
		L.setBlock(world, x + 2, y + 0, z + 0, VB.air, 0, 3);
		L.setBlock(world, x + 0, y + 0, z + 1, MB.steel_poles, 4, 3);
		L.setBlock(world, x + 1, y + 0, z + 1, MB.deco_steel, 0, 3);
		L.setBlock(world, x + 2, y + 0, z + 1, MB.tape_recorder, 5, 3);
		L.setBlock(world, x + 0, y + 0, z + 2, VB.air, 0, 3);
		L.setBlock(world, x + 1, y + 0, z + 2, MB.steel_poles, 3, 3);
		L.setBlock(world, x + 2, y + 0, z + 2, VB.chest, 0, 3);
		L.setBlockMetadataWithNotify(world, x + 2, y + 0, z + 2, 5, 3);
        ItemPool.generateChestContents(rand, ItemPool.getPool(ItemPoolsLegacy.POOL_ANTENNA), L.getTileEntity(world, x + 2, y, z + 2), 8);
		L.setBlock(world, x + 0, y + 1, z + 0, VB.air, 0, 3);
		L.setBlock(world, x + 1, y + 1, z + 0, MB.steel_poles, 2, 3);
		L.setBlock(world, x + 2, y + 1, z + 0, VB.air, 0, 3);
		L.setBlock(world, x + 0, y + 1, z + 1, MB.steel_poles, 4, 3);
		L.setBlock(world, x + 1, y + 1, z + 1, MB.deco_steel, 0, 3);
		L.setBlock(world, x + 2, y + 1, z + 1, MB.tape_recorder, 5, 3);
		L.setBlock(world, x + 0, y + 1, z + 2, VB.air, 0, 3);
		L.setBlock(world, x + 1, y + 1, z + 2, MB.steel_poles, 3, 3);
		L.setBlock(world, x + 2, y + 1, z + 2, VB.air, 0, 3);
		L.setBlock(world, x + 0, y + 2, z + 0, VB.air, 0, 3);
		L.setBlock(world, x + 1, y + 2, z + 0, MB.deco_steel, 0, 3);
		L.setBlock(world, x + 2, y + 2, z + 0, VB.air, 0, 3);
		L.setBlock(world, x + 0, y + 2, z + 1, MB.deco_steel, 0, 3);
		L.setBlock(world, x + 1, y + 2, z + 1, MB.deco_steel, 0, 3);
		L.setBlock(world, x + 2, y + 2, z + 1, MB.deco_steel, 0, 3);
		L.setBlock(world, x + 0, y + 2, z + 2, VB.air, 0, 3);
		L.setBlock(world, x + 1, y + 2, z + 2, MB.deco_steel, 0, 3);
		L.setBlock(world, x + 2, y + 2, z + 2, VB.air, 0, 3);
		L.setBlock(world, x + 0, y + 3, z + 0, VB.air, 0, 3);
		L.setBlock(world, x + 1, y + 3, z + 0, VB.air, 0, 3);
		L.setBlock(world, x + 2, y + 3, z + 0, VB.air, 0, 3);
		L.setBlock(world, x + 0, y + 3, z + 1, VB.air, 0, 3);
		L.setBlock(world, x + 1, y + 3, z + 1, MB.steel_poles, 4, 3);
		L.setBlock(world, x + 2, y + 3, z + 1, VB.air, 0, 3);
		L.setBlock(world, x + 0, y + 3, z + 2, VB.air, 0, 3);
		L.setBlock(world, x + 1, y + 3, z + 2, VB.air, 0, 3);
		L.setBlock(world, x + 2, y + 3, z + 2, VB.air, 0, 3);
		L.setBlock(world, x + 0, y + 4, z + 0, VB.air, 0, 3);
		L.setBlock(world, x + 1, y + 4, z + 0, VB.air, 0, 3);
		L.setBlock(world, x + 2, y + 4, z + 0, VB.air, 0, 3);
		L.setBlock(world, x + 0, y + 4, z + 1, VB.air, 0, 3);
		L.setBlock(world, x + 1, y + 4, z + 1, MB.steel_poles, 4, 3);
		L.setBlock(world, x + 2, y + 4, z + 1, VB.air, 0, 3);
		L.setBlock(world, x + 0, y + 4, z + 2, VB.air, 0, 3);
		L.setBlock(world, x + 1, y + 4, z + 2, VB.air, 0, 3);
		L.setBlock(world, x + 2, y + 4, z + 2, VB.air, 0, 3);
		L.setBlock(world, x + 0, y + 5, z + 0, VB.air, 0, 3);
		L.setBlock(world, x + 1, y + 5, z + 0, VB.air, 0, 3);
		L.setBlock(world, x + 2, y + 5, z + 0, VB.air, 0, 3);
		L.setBlock(world, x + 0, y + 5, z + 1, VB.air, 0, 3);
		L.setBlock(world, x + 1, y + 5, z + 1, MB.steel_poles, 4, 3);
		L.setBlock(world, x + 2, y + 5, z + 1, VB.air, 0, 3);
		L.setBlock(world, x + 0, y + 5, z + 2, VB.air, 0, 3);
		L.setBlock(world, x + 1, y + 5, z + 2, VB.air, 0, 3);
		L.setBlock(world, x + 2, y + 5, z + 2, VB.air, 0, 3);
		L.setBlock(world, x + 0, y + 6, z + 0, VB.air, 0, 3);
		L.setBlock(world, x + 1, y + 6, z + 0, VB.air, 0, 3);
		L.setBlock(world, x + 2, y + 6, z + 0, VB.air, 0, 3);
		L.setBlock(world, x + 0, y + 6, z + 1, VB.air, 0, 3);
		L.setBlock(world, x + 1, y + 6, z + 1, MB.steel_poles, 4, 3);
		L.setBlock(world, x + 2, y + 6, z + 1, VB.air, 0, 3);
		L.setBlock(world, x + 0, y + 6, z + 2, VB.air, 0, 3);
		L.setBlock(world, x + 1, y + 6, z + 2, VB.air, 0, 3);
		L.setBlock(world, x + 2, y + 6, z + 2, VB.air, 0, 3);
		L.setBlock(world, x + 0, y + 7, z + 0, VB.air, 0, 3);
		L.setBlock(world, x + 1, y + 7, z + 0, VB.air, 0, 3);
		L.setBlock(world, x + 2, y + 7, z + 0, VB.air, 0, 3);
		L.setBlock(world, x + 0, y + 7, z + 1, VB.air, 0, 3);
		L.setBlock(world, x + 1, y + 7, z + 1, MB.steel_poles, 4, 3);
		L.setBlock(world, x + 2, y + 7, z + 1, VB.air, 0, 3);
		L.setBlock(world, x + 0, y + 7, z + 2, VB.air, 0, 3);
		L.setBlock(world, x + 1, y + 7, z + 2, VB.air, 0, 3);
		L.setBlock(world, x + 2, y + 7, z + 2, VB.air, 0, 3);
		L.setBlock(world, x + 0, y + 8, z + 0, VB.air, 0, 3);
		L.setBlock(world, x + 1, y + 8, z + 0, VB.air, 0, 3);
		L.setBlock(world, x + 2, y + 8, z + 0, VB.air, 0, 3);
		L.setBlock(world, x + 0, y + 8, z + 1, VB.air, 0, 3);
		L.setBlock(world, x + 1, y + 8, z + 1, MB.steel_poles, 4, 3);
		L.setBlock(world, x + 2, y + 8, z + 1, VB.air, 0, 3);
		L.setBlock(world, x + 0, y + 8, z + 2, VB.air, 0, 3);
		L.setBlock(world, x + 1, y + 8, z + 2, VB.air, 0, 3);
		L.setBlock(world, x + 2, y + 8, z + 2, VB.air, 0, 3);
		L.setBlock(world, x + 0, y + 9, z + 0, VB.air, 0, 3);
		L.setBlock(world, x + 1, y + 9, z + 0, VB.air, 0, 3);
		L.setBlock(world, x + 2, y + 9, z + 0, VB.air, 0, 3);
		L.setBlock(world, x + 0, y + 9, z + 1, VB.air, 0, 3);
		L.setBlock(world, x + 1, y + 9, z + 1, MB.steel_poles, 4, 3);
		L.setBlock(world, x + 2, y + 9, z + 1, VB.air, 0, 3);
		L.setBlock(world, x + 0, y + 9, z + 2, VB.air, 0, 3);
		L.setBlock(world, x + 1, y + 9, z + 2, VB.air, 0, 3);
		L.setBlock(world, x + 2, y + 9, z + 2, VB.air, 0, 3);
		L.setBlock(world, x + 0, y + 10, z + 0, VB.air, 0, 3);
		L.setBlock(world, x + 1, y + 10, z + 0, VB.air, 0, 3);
		L.setBlock(world, x + 2, y + 10, z + 0, VB.air, 0, 3);
		L.setBlock(world, x + 0, y + 10, z + 1, VB.air, 0, 3);
		L.setBlock(world, x + 1, y + 10, z + 1, MB.steel_poles, 4, 3);
		L.setBlock(world, x + 2, y + 10, z + 1, VB.air, 0, 3);
		L.setBlock(world, x + 0, y + 10, z + 2, VB.air, 0, 3);
		L.setBlock(world, x + 1, y + 10, z + 2, VB.air, 0, 3);
		L.setBlock(world, x + 2, y + 10, z + 2, VB.air, 0, 3);
		L.setBlock(world, x + 0, y + 11, z + 0, VB.air, 0, 3);
		L.setBlock(world, x + 1, y + 11, z + 0, VB.air, 0, 3);
		L.setBlock(world, x + 2, y + 11, z + 0, VB.air, 0, 3);
		L.setBlock(world, x + 0, y + 11, z + 1, VB.air, 0, 3);
		L.setBlock(world, x + 1, y + 11, z + 1, MB.steel_poles, 4, 3);
		L.setBlock(world, x + 2, y + 11, z + 1, VB.air, 0, 3);
		L.setBlock(world, x + 0, y + 11, z + 2, VB.air, 0, 3);
		L.setBlock(world, x + 1, y + 11, z + 2, VB.air, 0, 3);
		L.setBlock(world, x + 2, y + 11, z + 2, VB.air, 0, 3);
		L.setBlock(world, x + 0, y + 12, z + 0, VB.air, 0, 3);
		L.setBlock(world, x + 1, y + 12, z + 0, VB.air, 0, 3);
		L.setBlock(world, x + 2, y + 12, z + 0, VB.air, 0, 3);
		L.setBlock(world, x + 0, y + 12, z + 1, VB.air, 0, 3);
		L.setBlock(world, x + 1, y + 12, z + 1, MB.steel_poles, 4, 3);
		L.setBlock(world, x + 2, y + 12, z + 1, VB.air, 0, 3);
		L.setBlock(world, x + 0, y + 12, z + 2, VB.air, 0, 3);
		L.setBlock(world, x + 1, y + 12, z + 2, VB.air, 0, 3);
		L.setBlock(world, x + 2, y + 12, z + 2, VB.air, 0, 3);
		L.setBlock(world, x + 0, y + 13, z + 0, VB.air, 0, 3);
		L.setBlock(world, x + 1, y + 13, z + 0, VB.air, 0, 3);
		L.setBlock(world, x + 2, y + 13, z + 0, VB.air, 0, 3);
		L.setBlock(world, x + 0, y + 13, z + 1, VB.air, 0, 3);
		L.setBlock(world, x + 1, y + 13, z + 1, MB.pole_satellite_receiver, 3, 3);
		L.setBlock(world, x + 2, y + 13, z + 1, VB.air, 0, 3);
		L.setBlock(world, x + 0, y + 13, z + 2, VB.air, 0, 3);
		L.setBlock(world, x + 1, y + 13, z + 2, VB.air, 0, 3);
		L.setBlock(world, x + 2, y + 13, z + 2, VB.air, 0, 3);
		L.setBlock(world, x + 0, y + 14, z + 0, VB.air, 0, 3);
		L.setBlock(world, x + 1, y + 14, z + 0, VB.air, 0, 3);
		L.setBlock(world, x + 2, y + 14, z + 0, VB.air, 0, 3);
		L.setBlock(world, x + 0, y + 14, z + 1, VB.air, 0, 3);
		L.setBlock(world, x + 1, y + 14, z + 1, MB.steel_poles, 4, 3);
		L.setBlock(world, x + 2, y + 14, z + 1, VB.air, 0, 3);
		L.setBlock(world, x + 0, y + 14, z + 2, VB.air, 0, 3);
		L.setBlock(world, x + 1, y + 14, z + 2, VB.air, 0, 3);
		L.setBlock(world, x + 2, y + 14, z + 2, VB.air, 0, 3);
		L.setBlock(world, x + 0, y + 15, z + 0, VB.air, 0, 3);
		L.setBlock(world, x + 1, y + 15, z + 0, VB.air, 0, 3);
		L.setBlock(world, x + 2, y + 15, z + 0, VB.air, 0, 3);
		L.setBlock(world, x + 0, y + 15, z + 1, VB.air, 0, 3);
		L.setBlock(world, x + 1, y + 15, z + 1, MB.steel_poles, 4, 3);
		L.setBlock(world, x + 2, y + 15, z + 1, VB.air, 0, 3);
		L.setBlock(world, x + 0, y + 15, z + 2, VB.air, 0, 3);
		L.setBlock(world, x + 1, y + 15, z + 2, VB.air, 0, 3);
		L.setBlock(world, x + 2, y + 15, z + 2, VB.air, 0, 3);
		L.setBlock(world, x + 0, y + 16, z + 0, VB.air, 0, 3);
		L.setBlock(world, x + 1, y + 16, z + 0, VB.air, 0, 3);
		L.setBlock(world, x + 2, y + 16, z + 0, VB.air, 0, 3);
		L.setBlock(world, x + 0, y + 16, z + 1, VB.air, 0, 3);
		L.setBlock(world, x + 1, y + 16, z + 1, MB.steel_poles, 4, 3);
		L.setBlock(world, x + 2, y + 16, z + 1, VB.air, 0, 3);
		L.setBlock(world, x + 0, y + 16, z + 2, VB.air, 0, 3);
		L.setBlock(world, x + 1, y + 16, z + 2, VB.air, 0, 3);
		L.setBlock(world, x + 2, y + 16, z + 2, VB.air, 0, 3);
		L.setBlock(world, x + 0, y + 17, z + 0, VB.air, 0, 3);
		L.setBlock(world, x + 1, y + 17, z + 0, VB.air, 0, 3);
		L.setBlock(world, x + 2, y + 17, z + 0, VB.air, 0, 3);
		L.setBlock(world, x + 0, y + 17, z + 1, VB.air, 0, 3);
		L.setBlock(world, x + 1, y + 17, z + 1, MB.pole_satellite_receiver, 2, 3);
		L.setBlock(world, x + 2, y + 17, z + 1, VB.air, 0, 3);
		L.setBlock(world, x + 0, y + 17, z + 2, VB.air, 0, 3);
		L.setBlock(world, x + 1, y + 17, z + 2, VB.air, 0, 3);
		L.setBlock(world, x + 2, y + 17, z + 2, VB.air, 0, 3);
		L.setBlock(world, x + 0, y + 18, z + 0, VB.air, 0, 3);
		L.setBlock(world, x + 1, y + 18, z + 0, VB.air, 0, 3);
		L.setBlock(world, x + 2, y + 18, z + 0, VB.air, 0, 3);
		L.setBlock(world, x + 0, y + 18, z + 1, VB.air, 0, 3);
		L.setBlock(world, x + 1, y + 18, z + 1, MB.pole_satellite_receiver, 4, 3);
		L.setBlock(world, x + 2, y + 18, z + 1, VB.air, 0, 3);
		L.setBlock(world, x + 0, y + 18, z + 2, VB.air, 0, 3);
		L.setBlock(world, x + 1, y + 18, z + 2, VB.air, 0, 3);
		L.setBlock(world, x + 2, y + 18, z + 2, VB.air, 0, 3);
		L.setBlock(world, x + 0, y + 19, z + 0, VB.air, 0, 3);
		L.setBlock(world, x + 1, y + 19, z + 0, VB.air, 0, 3);
		L.setBlock(world, x + 2, y + 19, z + 0, VB.air, 0, 3);
		L.setBlock(world, x + 0, y + 19, z + 1, VB.air, 0, 3);
		L.setBlock(world, x + 1, y + 19, z + 1, MB.steel_poles, 4, 3);
		L.setBlock(world, x + 2, y + 19, z + 1, VB.air, 0, 3);
		L.setBlock(world, x + 0, y + 19, z + 2, VB.air, 0, 3);
		L.setBlock(world, x + 1, y + 19, z + 2, VB.air, 0, 3);
		L.setBlock(world, x + 2, y + 19, z + 2, VB.air, 0, 3);
		L.setBlock(world, x + 0, y + 20, z + 0, VB.air, 0, 3);
		L.setBlock(world, x + 1, y + 20, z + 0, VB.air, 0, 3);
		L.setBlock(world, x + 2, y + 20, z + 0, VB.air, 0, 3);
		L.setBlock(world, x + 0, y + 20, z + 1, VB.air, 0, 3);
		L.setBlock(world, x + 1, y + 20, z + 1, MB.pole_top, 4, 3);
		L.setBlock(world, x + 2, y + 20, z + 1, VB.air, 0, 3);
		L.setBlock(world, x + 0, y + 20, z + 2, VB.air, 0, 3);
		L.setBlock(world, x + 1, y + 20, z + 2, VB.air, 0, 3);
		L.setBlock(world, x + 2, y + 20, z + 2, VB.air, 0, 3);
		if(GeneralConfig.enableDebugMode)
			System.out.print("[Debug] Successfully spawned antenna at " + x + " " + y +" " + z + "\n");
		return true;

	}

}