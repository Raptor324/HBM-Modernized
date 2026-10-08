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
 * 1:1 {@code com.hbm.world.dungeon.Barrel} (Original-Weltgen-Klasse, zeilengetreu auf die Port-Hilfen {@code L}/{@code MB}/{@code VB} umgesetzt).
 */
public class Barrel {
	
	protected LB[] GetValidSpawnBlocks() {
		
		return new LB[] {
				VB.grass,
				VB.dirt,
				VB.sand,
				VB.stone,
				VB.sandstone
			};
	}

	public boolean LocationIsValidSpawn(LevelAccessor world, int x, int y, int z) {

		LB checkBlock = L.getBlock(world, x, y - 1, z);
		LB blockAbove = L.getBlock(world, x, y, z);
		LB blockBelow = L.getBlock(world, x, y - 2, z);

		for (LB i : GetValidSpawnBlocks()) {
			if (blockAbove != VB.air) {
				return false;
			}
			if (checkBlock == i) {
				return true;
			} else if (checkBlock == VB.snow_layer && blockBelow == i) {
				return true;
			} else if (checkBlock.getMaterial() == LMaterial.plants && blockBelow == i) {
				return true;
			}
		}
		return false;
	}

	public boolean generate(LevelAccessor world, Random rand, int x, int y, int z) {
		int i = rand.nextInt(1);

		if (i == 0) {
			generate_r0(world, rand, x, y, z);
		}

		return true;

	}
	
	LB Block1 = MB.reinforced_brick;
	LB Block2 = MB.sellafield_slaked;
	LB Block3 = MB.brick_concrete;
	LB sellafield = MB.sellafield;
	LB Block10 = MB.deco_lead;
	LB Block11 = MB.reinforced_glass;
	LB Block12 = MB.toxic_block;

	public boolean generate_r0(LevelAccessor world, Random rand, int x, int y, int z) {
		if (!LocationIsValidSpawn(world, x, y, z) || !LocationIsValidSpawn(world, x + 4, y, z)
				|| !LocationIsValidSpawn(world, x + 4, y, z + 6) || !LocationIsValidSpawn(world, x, y, z + 6)) {
			return false;
		}

		L.setBlock(world, x + 1, y + -1, z + 0, Block1, 0, 3);
		L.setBlock(world, x + 2, y + -1, z + 0, Block1, 0, 3);
		L.setBlock(world, x + 3, y + -1, z + 0, Block1, 0, 3);
		L.setBlock(world, x + 0, y + -1, z + 1, Block1, 0, 3);
		L.setBlock(world, x + 1, y + -1, z + 1, Block1, 0, 3);
		L.setBlock(world, x + 2, y + -1, z + 1, Block1, 0, 3);
		L.setBlock(world, x + 3, y + -1, z + 1, Block1, 0, 3);
		L.setBlock(world, x + 4, y + -1, z + 1, Block1, 0, 3);
		L.setBlock(world, x + 0, y + -1, z + 2, Block1, 0, 3);
		L.setBlock(world, x + 1, y + -1, z + 2, Block1, 0, 3);
		L.setBlock(world, x + 2, y + -1, z + 2, Block1, 0, 3);
		L.setBlock(world, x + 3, y + -1, z + 2, Block1, 0, 3);
		L.setBlock(world, x + 4, y + -1, z + 2, Block1, 0, 3);
		L.setBlock(world, x + 0, y + -1, z + 3, Block1, 0, 3);
		L.setBlock(world, x + 1, y + -1, z + 3, Block1, 0, 3);
		L.setBlock(world, x + 2, y + -1, z + 3, Block1, 0, 3);
		L.setBlock(world, x + 3, y + -1, z + 3, Block1, 0, 3);
		L.setBlock(world, x + 4, y + -1, z + 3, Block1, 0, 3);
		L.setBlock(world, x + 1, y + -1, z + 4, Block1, 0, 3);
		L.setBlock(world, x + 2, y + -1, z + 4, Block1, 0, 3);
		L.setBlock(world, x + 3, y + -1, z + 4, Block1, 0, 3);
		L.setBlock(world, x + 1, y + -1, z + 5, Block1, 0, 3);
		L.setBlock(world, x + 2, y + -1, z + 5, Block1, 0, 3);
		L.setBlock(world, x + 3, y + -1, z + 5, Block1, 0, 3);
		L.setBlock(world, x + 1, y + -1, z + 6, Block1, 0, 3);
		L.setBlock(world, x + 2, y + -1, z + 6, Block1, 0, 3);
		L.setBlock(world, x + 3, y + -1, z + 6, Block1, 0, 3);
		L.setBlock(world, x + 1, y + 0, z + 0, Block2, 0, 3);
		L.setBlock(world, x + 2, y + 0, z + 0, L.getRandomConcrete(), 0, 3);
		L.setBlock(world, x + 3, y + 0, z + 0, Block2, 0, 3);
		L.setBlock(world, x + 0, y + 0, z + 1, L.getRandomConcrete(), 0, 3);
		L.setBlock(world, x + 1, y + 0, z + 1, sellafield, 3, 3);
		L.setBlock(world, x + 2, y + 0, z + 1, sellafield, 4, 3);
		L.setBlock(world, x + 3, y + 0, z + 1, sellafield, 3, 3);
		L.setBlock(world, x + 4, y + 0, z + 1, L.getRandomConcrete(), 0, 3);
		L.setBlock(world, x + 0, y + 0, z + 2, L.getRandomConcrete(), 0, 3);
		L.setBlock(world, x + 1, y + 0, z + 2, sellafield, 4, 3);
		L.setBlock(world, x + 2, y + 0, z + 2, sellafield, 5, 3);
		
		// Original: radius = 2.5 fuer ein TileEntitySellafield - wirkungslos, denn ModBlocks.sellafield ist BlockSellafield
		// ohne Blockentitaet (sellafield_core ist entfernt, MainRegistry.ignoreMappings). Daher hier nichts zu tun.
		
		L.setBlock(world, x + 3, y + 0, z + 2, sellafield, 4, 3);
		L.setBlock(world, x + 4, y + 0, z + 2, Block2, 0, 3);
		L.setBlock(world, x + 0, y + 0, z + 3, Block2, 0, 3);
		L.setBlock(world, x + 1, y + 0, z + 3, sellafield, 4, 3);
		L.setBlock(world, x + 2, y + 0, z + 3, sellafield, 3, 3);
		L.setBlock(world, x + 3, y + 0, z + 3, sellafield, 4, 3);
		L.setBlock(world, x + 4, y + 0, z + 3, Block2, 0, 3);
		L.setBlock(world, x + 1, y + 0, z + 4, Block2, 0, 3);
		L.setBlock(world, x + 2, y + 0, z + 4, L.getRandomConcrete(), 0, 3);
		L.setBlock(world, x + 3, y + 0, z + 4, L.getRandomConcrete(), 0, 3);
		L.setBlock(world, x + 1, y + 1, z + 0, Block2, 0, 3);
		L.setBlock(world, x + 2, y + 1, z + 0, L.getRandomConcrete(), 0, 3);
		L.setBlock(world, x + 3, y + 1, z + 0, Block2, 0, 3);
		L.setBlock(world, x + 0, y + 1, z + 1, L.getRandomConcrete(), 0, 3);
		L.setBlock(world, x + 1, y + 1, z + 1, sellafield, 2, 3);
		L.setBlock(world, x + 2, y + 1, z + 1, sellafield, 3, 3);
		L.setBlock(world, x + 3, y + 1, z + 1, sellafield, 3, 3);
		L.setBlock(world, x + 4, y + 1, z + 1, Block2, 0, 3);
		L.setBlock(world, x + 0, y + 1, z + 2, L.getRandomConcrete(), 0, 3);
		L.setBlock(world, x + 1, y + 1, z + 2, sellafield, 3, 3);
		
		/*L.setBlock(world, x + 2, y + 1, z + 2, VB.chest, 3, 3);

		if(L.getBlock(world, x + 2, y + 1, z + 2) == VB.chest)
		{
			ItemPool.generateChestContents(rand, HbmChestContents.getLoot(3), L.getTileEntity(world, x + 2, y + 1, z + 2), 16);
		}*/

		L.setBlock(world, x + 2, y + 1, z + 2, MB.crate_steel, 0, 3);

		if(L.getBlock(world, x + 2, y + 1, z + 2) == MB.crate_steel)
		{
			ItemPool.generateChestContents(rand, ItemPool.getPool(ItemPoolsLegacy.POOL_EXPENSIVE), L.getTileEntity(world, x + 2, y + 1, z + 2), 16);
		}
		
		L.setBlock(world, x + 3, y + 1, z + 2, sellafield, 3, 3);
		L.setBlock(world, x + 4, y + 1, z + 2, L.getRandomConcrete(), 0, 3);
		L.setBlock(world, x + 0, y + 1, z + 3, L.getRandomConcrete(), 0, 3);
		L.setBlock(world, x + 1, y + 1, z + 3, sellafield, 3, 3);
		L.setBlock(world, x + 2, y + 1, z + 3, sellafield, 2, 3);
		L.setBlock(world, x + 3, y + 1, z + 3, sellafield, 3, 3);
		L.setBlock(world, x + 4, y + 1, z + 3, Block2, 0, 3);
		L.setBlock(world, x + 1, y + 1, z + 4, Block2, 0, 3);
		L.setBlock(world, x + 2, y + 1, z + 4, L.getRandomConcrete(), 0, 3);
		L.setBlock(world, x + 3, y + 1, z + 4, L.getRandomConcrete(), 0, 3);
		L.setBlock(world, x + 1, y + 2, z + 0, L.getRandomConcrete(), 0, 3);
		L.setBlock(world, x + 2, y + 2, z + 0, Block2, 0, 3);
		L.setBlock(world, x + 3, y + 2, z + 0, L.getRandomConcrete(), 0, 3);
		L.setBlock(world, x + 0, y + 2, z + 1, Block2, 0, 3);
		L.setBlock(world, x + 1, y + 2, z + 1, sellafield, 1, 3);
		L.setBlock(world, x + 2, y + 2, z + 1, sellafield, 2, 3);
		L.setBlock(world, x + 3, y + 2, z + 1, sellafield, 2, 3);
		L.setBlock(world, x + 4, y + 2, z + 1, L.getRandomConcrete(), 0, 3);
		L.setBlock(world, x + 0, y + 2, z + 2, L.getRandomConcrete(), 0, 3);
		L.setBlock(world, x + 1, y + 2, z + 2, sellafield, 2, 3);
		L.setBlock(world, x + 2, y + 2, z + 2, sellafield, 4, 3);
		L.setBlock(world, x + 3, y + 2, z + 2, sellafield, 2, 3);
		L.setBlock(world, x + 4, y + 2, z + 2, L.getRandomConcrete(), 0, 3);
		L.setBlock(world, x + 0, y + 2, z + 3, L.getRandomConcrete(), 0, 3);
		L.setBlock(world, x + 1, y + 2, z + 3, sellafield, 2, 3);
		L.setBlock(world, x + 2, y + 2, z + 3, sellafield, 1, 3);
		L.setBlock(world, x + 3, y + 2, z + 3, sellafield, 2, 3);
		L.setBlock(world, x + 4, y + 2, z + 3, L.getRandomConcrete(), 0, 3);
		L.setBlock(world, x + 1, y + 2, z + 4, L.getRandomConcrete(), 0, 3);
		L.setBlock(world, x + 2, y + 2, z + 4, L.getRandomConcrete(), 0, 3);
		L.setBlock(world, x + 3, y + 2, z + 4, L.getRandomConcrete(), 0, 3);
		L.setBlock(world, x + 1, y + 3, z + 0, L.getRandomConcrete(), 0, 3);
		L.setBlock(world, x + 2, y + 3, z + 0, L.getRandomConcrete(), 0, 3);
		L.setBlock(world, x + 3, y + 3, z + 0, L.getRandomConcrete(), 0, 3);
		L.setBlock(world, x + 0, y + 3, z + 1, L.getRandomConcrete(), 0, 3);
		L.setBlock(world, x + 1, y + 3, z + 1, sellafield, 1, 3);
		L.setBlock(world, x + 2, y + 3, z + 1, sellafield, 1, 3);
		L.setBlock(world, x + 3, y + 3, z + 1, sellafield, 1, 3);
		L.setBlock(world, x + 4, y + 3, z + 1, Block2, 0, 3);
		L.setBlock(world, x + 0, y + 3, z + 2, L.getRandomConcrete(), 0, 3);
		L.setBlock(world, x + 1, y + 3, z + 2, sellafield, 1, 3);
		L.setBlock(world, x + 2, y + 3, z + 2, sellafield, 3, 3);
		L.setBlock(world, x + 3, y + 3, z + 2, sellafield, 1, 3);
		L.setBlock(world, x + 4, y + 3, z + 2, L.getRandomConcrete(), 0, 3);
		L.setBlock(world, x + 0, y + 3, z + 3, Block2, 0, 3);
		L.setBlock(world, x + 1, y + 3, z + 3, sellafield, 1, 3);
		L.setBlock(world, x + 2, y + 3, z + 3, sellafield, 0, 3);
		L.setBlock(world, x + 3, y + 3, z + 3, sellafield, 1, 3);
		L.setBlock(world, x + 4, y + 3, z + 3, L.getRandomConcrete(), 0, 3);
		L.setBlock(world, x + 1, y + 3, z + 4, L.getRandomConcrete(), 0, 3);
		L.setBlock(world, x + 2, y + 3, z + 4, L.getRandomConcrete(), 0, 3);
		L.setBlock(world, x + 3, y + 3, z + 4, L.getRandomConcrete(), 0, 3);
		L.setBlock(world, x + 1, y + 4, z + 0, L.getRandomConcrete(), 0, 3);
		L.setBlock(world, x + 2, y + 4, z + 0, L.getRandomConcrete(), 0, 3);
		L.setBlock(world, x + 3, y + 4, z + 0, L.getRandomConcrete(), 0, 3);
		L.setBlock(world, x + 0, y + 4, z + 1, L.getRandomConcrete(), 0, 3);
		L.setBlock(world, x + 1, y + 4, z + 1, sellafield, 0, 3);
		L.setBlock(world, x + 2, y + 4, z + 1, sellafield, 1, 3);
		L.setBlock(world, x + 3, y + 4, z + 1, sellafield, 0, 3);
		L.setBlock(world, x + 4, y + 4, z + 1, L.getRandomConcrete(), 0, 3);
		L.setBlock(world, x + 0, y + 4, z + 2, L.getRandomConcrete(), 0, 3);
		L.setBlock(world, x + 1, y + 4, z + 2, sellafield, 0, 3);
		L.setBlock(world, x + 2, y + 4, z + 2, sellafield, 2, 3);
		L.setBlock(world, x + 3, y + 4, z + 2, sellafield, 1, 3);
		L.setBlock(world, x + 4, y + 4, z + 2, Block2, 0, 3);
		L.setBlock(world, x + 0, y + 4, z + 3, L.getRandomConcrete(), 0, 3);
		L.setBlock(world, x + 1, y + 4, z + 3, sellafield, 1, 3);
		L.setBlock(world, x + 2, y + 4, z + 3, sellafield, 0, 3);
		L.setBlock(world, x + 3, y + 4, z + 3, sellafield, 0, 3);
		L.setBlock(world, x + 4, y + 4, z + 3, Block2, 0, 3);
		L.setBlock(world, x + 1, y + 4, z + 4, L.getRandomConcrete(), 0, 3);
		L.setBlock(world, x + 2, y + 4, z + 4, L.getRandomConcrete(), 0, 3);
		L.setBlock(world, x + 3, y + 4, z + 4, Block2, 0, 3);
		L.setBlock(world, x + 1, y + 5, z + 0, L.getRandomConcrete(), 0, 3);
		L.setBlock(world, x + 2, y + 5, z + 0, L.getRandomConcrete(), 0, 3);
		L.setBlock(world, x + 3, y + 5, z + 0, L.getRandomConcrete(), 0, 3);
		L.setBlock(world, x + 0, y + 5, z + 1, L.getRandomConcrete(), 0, 3);
		L.setBlock(world, x + 1, y + 5, z + 1, sellafield, 0, 3);
		L.setBlock(world, x + 2, y + 5, z + 1, sellafield, 0, 3);
		L.setBlock(world, x + 3, y + 5, z + 1, sellafield, 0, 3);
		L.setBlock(world, x + 4, y + 5, z + 1, Block2, 0, 3);
		L.setBlock(world, x + 0, y + 5, z + 2, L.getRandomConcrete(), 0, 3);
		L.setBlock(world, x + 1, y + 5, z + 2, Block12, 0, 3);
		L.setBlock(world, x + 2, y + 5, z + 2, sellafield, 1, 3);
		L.setBlock(world, x + 3, y + 5, z + 2, Block12, 0, 3);
		L.setBlock(world, x + 4, y + 5, z + 2, VB.air, 0, 3);
		L.setBlock(world, x + 0, y + 5, z + 3, L.getRandomConcrete(), 0, 3);
		L.setBlock(world, x + 1, y + 5, z + 3, sellafield, 0, 3);
		L.setBlock(world, x + 2, y + 5, z + 3, sellafield, 0, 3);
		L.setBlock(world, x + 3, y + 5, z + 3, Block12, 0, 3);
		L.setBlock(world, x + 4, y + 5, z + 3, Block2, 0, 3);
		L.setBlock(world, x + 1, y + 5, z + 4, L.getRandomConcrete(), 0, 3);
		L.setBlock(world, x + 2, y + 5, z + 4, L.getRandomConcrete(), 0, 3);
		L.setBlock(world, x + 3, y + 5, z + 4, L.getRandomConcrete(), 0, 3);
		L.setBlock(world, x + 1, y + 6, z + 0, Block10, 0, 3);
		L.setBlock(world, x + 2, y + 6, z + 0, Block10, 0, 3);
		L.setBlock(world, x + 3, y + 6, z + 0, Block10, 0, 3);
		L.setBlock(world, x + 0, y + 6, z + 1, Block10, 0, 3);
		L.setBlock(world, x + 1, y + 6, z + 1, Block12, 0, 3);
		L.setBlock(world, x + 2, y + 6, z + 1, Block12, 0, 3);
		L.setBlock(world, x + 3, y + 6, z + 1, Block12, 0, 3);
		L.setBlock(world, x + 4, y + 6, z + 1, Block10, 0, 3);
		L.setBlock(world, x + 0, y + 6, z + 2, Block10, 0, 3);
		L.setBlock(world, x + 1, y + 6, z + 2, Block12, 0, 3);
		L.setBlock(world, x + 2, y + 6, z + 2, sellafield, 0, 3);
		L.setBlock(world, x + 3, y + 6, z + 2, Block12, 0, 3);
		L.setBlock(world, x + 4, y + 6, z + 2, Block10, 0, 3);
		L.setBlock(world, x + 0, y + 6, z + 3, Block10, 0, 3);
		L.setBlock(world, x + 1, y + 6, z + 3, Block12, 0, 3);
		L.setBlock(world, x + 2, y + 6, z + 3, Block12, 0, 3);
		L.setBlock(world, x + 3, y + 6, z + 3, Block12, 0, 3);
		L.setBlock(world, x + 4, y + 6, z + 3, Block10, 0, 3);
		L.setBlock(world, x + 1, y + 6, z + 4, Block10, 0, 3);
		L.setBlock(world, x + 2, y + 6, z + 4, Block10, 0, 3);
		L.setBlock(world, x + 3, y + 6, z + 4, Block10, 0, 3);
		L.setBlock(world, x + 1, y + 7, z + 0, L.getRandomConcrete(), 0, 3);
		L.setBlock(world, x + 2, y + 7, z + 0, L.getRandomConcrete(), 0, 3);
		L.setBlock(world, x + 3, y + 7, z + 0, L.getRandomConcrete(), 0, 3);
		L.setBlock(world, x + 0, y + 7, z + 1, L.getRandomConcrete(), 0, 3);
		L.setBlock(world, x + 1, y + 7, z + 1, Block12, 0, 3);
		L.setBlock(world, x + 2, y + 7, z + 1, Block12, 0, 3);
		L.setBlock(world, x + 3, y + 7, z + 1, Block12, 0, 3);
		L.setBlock(world, x + 4, y + 7, z + 1, Block2, 0, 3);
		L.setBlock(world, x + 0, y + 7, z + 2, L.getRandomConcrete(), 0, 3);
		L.setBlock(world, x + 1, y + 7, z + 2, Block12, 0, 3);
		L.setBlock(world, x + 2, y + 7, z + 2, Block12, 0, 3);
		L.setBlock(world, x + 3, y + 7, z + 2, Block12, 0, 3);
		L.setBlock(world, x + 4, y + 7, z + 2, Block2, 0, 3);
		L.setBlock(world, x + 0, y + 7, z + 3, L.getRandomConcrete(), 0, 3);
		L.setBlock(world, x + 1, y + 7, z + 3, Block12, 0, 3);
		L.setBlock(world, x + 2, y + 7, z + 3, Block12, 0, 3);
		L.setBlock(world, x + 3, y + 7, z + 3, Block12, 0, 3);
		L.setBlock(world, x + 4, y + 7, z + 3, L.getRandomConcrete(), 0, 3);
		L.setBlock(world, x + 1, y + 7, z + 4, L.getRandomConcrete(), 0, 3);
		L.setBlock(world, x + 2, y + 7, z + 4, L.getRandomConcrete(), 0, 3);
		L.setBlock(world, x + 3, y + 7, z + 4, L.getRandomConcrete(), 0, 3);
		L.setBlock(world, x + 1, y + 8, z + 0, Block10, 0, 3);
		L.setBlock(world, x + 2, y + 8, z + 0, Block10, 0, 3);
		L.setBlock(world, x + 3, y + 8, z + 0, Block10, 0, 3);
		L.setBlock(world, x + 0, y + 8, z + 1, Block10, 0, 3);
		L.setBlock(world, x + 1, y + 8, z + 1, Block12, 0, 3);
		L.setBlock(world, x + 2, y + 8, z + 1, Block12, 0, 3);
		L.setBlock(world, x + 3, y + 8, z + 1, Block12, 0, 3);
		L.setBlock(world, x + 4, y + 8, z + 1, Block10, 0, 3);
		L.setBlock(world, x + 0, y + 8, z + 2, Block10, 0, 3);
		L.setBlock(world, x + 1, y + 8, z + 2, Block12, 0, 3);
		L.setBlock(world, x + 2, y + 8, z + 2, Block12, 0, 3);
		L.setBlock(world, x + 3, y + 8, z + 2, Block12, 0, 3);
		L.setBlock(world, x + 4, y + 8, z + 2, Block10, 0, 3);
		L.setBlock(world, x + 0, y + 8, z + 3, Block10, 0, 3);
		L.setBlock(world, x + 1, y + 8, z + 3, Block12, 0, 3);
		L.setBlock(world, x + 2, y + 8, z + 3, Block12, 0, 3);
		L.setBlock(world, x + 3, y + 8, z + 3, Block12, 0, 3);
		L.setBlock(world, x + 4, y + 8, z + 3, Block10, 0, 3);
		L.setBlock(world, x + 1, y + 8, z + 4, Block10, 0, 3);
		L.setBlock(world, x + 2, y + 8, z + 4, Block10, 0, 3);
		L.setBlock(world, x + 3, y + 8, z + 4, Block10, 0, 3);
		L.setBlock(world, x + 1, y + 9, z + 0, L.getRandomConcrete(), 0, 3);
		L.setBlock(world, x + 2, y + 9, z + 0, L.getRandomConcrete(), 0, 3);
		L.setBlock(world, x + 3, y + 9, z + 0, L.getRandomConcrete(), 0, 3);
		L.setBlock(world, x + 0, y + 9, z + 1, L.getRandomConcrete(), 0, 3);
		L.setBlock(world, x + 4, y + 9, z + 1, L.getRandomConcrete(), 0, 3);
		L.setBlock(world, x + 0, y + 9, z + 2, L.getRandomConcrete(), 0, 3);
		L.setBlock(world, x + 4, y + 9, z + 2, L.getRandomConcrete(), 0, 3);
		L.setBlock(world, x + 0, y + 9, z + 3, L.getRandomConcrete(), 0, 3);
		L.setBlock(world, x + 4, y + 9, z + 3, L.getRandomConcrete(), 0, 3);
		L.setBlock(world, x + 1, y + 9, z + 4, L.getRandomConcrete(), 0, 3);
		L.setBlock(world, x + 2, y + 9, z + 4, L.getRandomConcrete(), 0, 3);
		L.setBlock(world, x + 3, y + 9, z + 4, L.getRandomConcrete(), 0, 3);
		L.setBlock(world, x + 1, y + 10, z + 0, L.getRandomConcrete(), 0, 3);
		L.setBlock(world, x + 2, y + 10, z + 0, L.getRandomConcrete(), 0, 3);
		L.setBlock(world, x + 3, y + 10, z + 0, L.getRandomConcrete(), 0, 3);
		L.setBlock(world, x + 0, y + 10, z + 1, L.getRandomConcrete(), 0, 3);
		L.setBlock(world, x + 4, y + 10, z + 1, L.getRandomConcrete(), 0, 3);
		L.setBlock(world, x + 0, y + 10, z + 2, L.getRandomConcrete(), 0, 3);
		L.setBlock(world, x + 4, y + 10, z + 2, L.getRandomConcrete(), 0, 3);
		L.setBlock(world, x + 0, y + 10, z + 3, L.getRandomConcrete(), 0, 3);
		L.setBlock(world, x + 4, y + 10, z + 3, L.getRandomConcrete(), 0, 3);
		L.setBlock(world, x + 1, y + 10, z + 4, L.getRandomConcrete(), 0, 3);
		//L.setBlock(world, x + 2, y + 10, z + 4, VB.iron_door, 2, 3);
		L.setBlock(world, x + 3, y + 10, z + 4, L.getRandomConcrete(), 0, 3);
		L.setBlock(world, x + 1, y + 11, z + 0, L.getRandomConcrete(), 0, 3);
		L.setBlock(world, x + 2, y + 11, z + 0, L.getRandomConcrete(), 0, 3);
		L.setBlock(world, x + 3, y + 11, z + 0, L.getRandomConcrete(), 0, 3);
		L.setBlock(world, x + 0, y + 11, z + 1, Block11, 0, 3);
		L.setBlock(world, x + 0, y + 11, z + 2, Block11, 0, 3);
		L.setBlock(world, x + 4, y + 11, z + 2, Block11, 0, 3);
		L.setBlock(world, x + 0, y + 11, z + 3, Block11, 0, 3);
		L.setBlock(world, x + 4, y + 11, z + 3, Block11, 0, 3);
		L.setBlock(world, x + 1, y + 11, z + 4, L.getRandomConcrete(), 0, 3);
		//L.setBlock(world, x + 2, y + 11, z + 4, VB.iron_door, 8, 3);
        L.placeDoorBlock(world, x + 2, y + 10, z + 4, 2, VB.iron_door);
		L.setBlock(world, x + 3, y + 11, z + 4, L.getRandomConcrete(), 0, 3);
		L.setBlock(world, x + 1, y + 12, z + 0, L.getRandomConcrete(), 0, 3);
		L.setBlock(world, x + 2, y + 12, z + 0, L.getRandomConcrete(), 0, 3);
		L.setBlock(world, x + 3, y + 12, z + 0, L.getRandomConcrete(), 0, 3);
		L.setBlock(world, x + 0, y + 12, z + 1, L.getRandomConcrete(), 0, 3);
		L.setBlock(world, x + 4, y + 12, z + 1, L.getRandomConcrete(), 0, 3);
		L.setBlock(world, x + 0, y + 12, z + 2, L.getRandomConcrete(), 0, 3);
		L.setBlock(world, x + 4, y + 12, z + 2, L.getRandomConcrete(), 0, 3);
		L.setBlock(world, x + 0, y + 12, z + 3, L.getRandomConcrete(), 0, 3);
		L.setBlock(world, x + 4, y + 12, z + 3, L.getRandomConcrete(), 0, 3);
		L.setBlock(world, x + 1, y + 12, z + 4, L.getRandomConcrete(), 0, 3);
		L.setBlock(world, x + 2, y + 12, z + 4, L.getRandomConcrete(), 0, 3);
		L.setBlock(world, x + 3, y + 12, z + 4, L.getRandomConcrete(), 0, 3);
		L.setBlock(world, x + 1, y + 13, z + 0, Block1, 0, 3);
		L.setBlock(world, x + 2, y + 13, z + 0, Block1, 0, 3);
		L.setBlock(world, x + 3, y + 13, z + 0, Block1, 0, 3);
		L.setBlock(world, x + 0, y + 13, z + 1, Block1, 0, 3);
		L.setBlock(world, x + 1, y + 13, z + 1, Block10, 0, 3);
		L.setBlock(world, x + 2, y + 13, z + 1, Block10, 0, 3);
		L.setBlock(world, x + 3, y + 13, z + 1, Block10, 0, 3);
		L.setBlock(world, x + 4, y + 13, z + 1, Block1, 0, 3);
		L.setBlock(world, x + 0, y + 13, z + 2, Block1, 0, 3);
		L.setBlock(world, x + 1, y + 13, z + 2, Block10, 0, 3);
		L.setBlock(world, x + 2, y + 13, z + 2, Block10, 0, 3);
		L.setBlock(world, x + 4, y + 13, z + 2, Block1, 0, 3);
		L.setBlock(world, x + 0, y + 13, z + 3, Block1, 0, 3);
		L.setBlock(world, x + 1, y + 13, z + 3, Block10, 0, 3);
		L.setBlock(world, x + 2, y + 13, z + 3, Block10, 0, 3);
		L.setBlock(world, x + 1, y + 13, z + 4, Block1, 0, 3);
		L.setBlock(world, x + 2, y + 13, z + 4, Block1, 0, 3);
		L.setBlock(world, x + 3, y + 13, z + 4, Block1, 0, 3);

		generate_r02_last(world, rand, x, y, z);
		return true;

	}

	public boolean generate_r02_last(LevelAccessor world, Random rand, int x, int y, int z) {

		L.setBlock(world, x + 2, y + 0, z + 5, VB.ladder, 3, 3);
		L.setBlock(world, x + 2, y + 1, z + 5, VB.ladder, 3, 3);
		L.setBlock(world, x + 2, y + 2, z + 5, VB.ladder, 3, 3);
		L.setBlock(world, x + 2, y + 3, z + 5, VB.ladder, 3, 3);
		L.setBlock(world, x + 2, y + 4, z + 5, VB.ladder, 3, 3);
		L.setBlock(world, x + 2, y + 5, z + 5, VB.ladder, 3, 3);
		L.setBlock(world, x + 2, y + 6, z + 5, VB.ladder, 3, 3);
		L.setBlock(world, x + 2, y + 7, z + 5, VB.ladder, 3, 3);
		L.setBlock(world, x + 2, y + 8, z + 5, VB.ladder, 3, 3);
		L.setBlock(world, x + 2, y + 9, z + 5, VB.ladder, 3, 3);
		
		if(GeneralConfig.enableDebugMode)
			System.out.print("[Debug] Successfully spawned waste tank at " + x + " " + y +" " + z + "\n");
		
		return true;

	}

}