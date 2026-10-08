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
 * 1:1 {@code com.hbm.world.dungeon.AncientTomb} (Original-Weltgen-Klasse, zeilengetreu auf die Port-Hilfen {@code L}/{@code MB}/{@code VB} umgesetzt).
 */
public class AncientTomb {

	
	public void build(LevelAccessor world, Random rand, int x, int y, int z) {

		List<MetaBlock> concrete = Arrays.asList(new MetaBlock[] {
				new MetaBlock(MB.brick_concrete),
				new MetaBlock(MB.brick_concrete_broken),
				new MetaBlock(MB.brick_concrete_cracked)});
		
		y = 20;
		
		/// PYRAMID Y LOCATION ///
		int yOff = Math.max(L.getHeightValue(world, x, z), 35) - 5;
		
		int pySize = 15;
		
		/// PRINT PYRAMID ///
		for(int iy = pySize; iy > 0; iy--) {
			
			int range = (pySize - iy);
			
			for(int ix = -range; ix <= range; ix++) {
				for(int iz = -range; iz <= range; iz++) {
					
					if((ix <= -range + 1 || ix >= range - 1) && (iz <= -range + 1 || iz >= range - 1)) {
						L.setBlock(world, x + ix, yOff + iy, z + iz, MB.reinforced_stone);
						continue;
					}
					
					if(iy == 1) {
						L.setBlock(world, x + ix, yOff + iy, z + iz, MB.concrete_smooth);
						continue;
					}
					
					if((ix <= -range + 1 || ix >= range - 1) || (iz <= -range + 1 || iz >= range - 1)) {
						L.setBlock(world, x + ix, yOff + iy, z + iz, MB.concrete_smooth);
						continue;
					}
					
					L.setBlockToAir(world, x + ix, yOff + iy, z + iz);
				}
			}
		}
		
		DungeonToolbox.generateBox(world, x - 2, yOff + 2, z - 2, 5, 4, 5, concrete);
		L.setBlock(world, x + 2, yOff + 3, z, MB.brick_concrete_marked);
		L.setBlock(world, x - 2, yOff + 3, z, MB.brick_concrete_marked);
		L.setBlock(world, x, yOff + 3, z + 2, MB.brick_concrete_marked);
		L.setBlock(world, x, yOff + 3, z - 2, MB.brick_concrete_marked);

		DungeonToolbox.generateBox(world, x + 5, yOff + 2, z + 5, 1, 7, 1, MB.concrete_pillar);
		DungeonToolbox.generateBox(world, x + 5, yOff + 2, z - 5, 1, 7, 1, MB.concrete_pillar);
		DungeonToolbox.generateBox(world, x - 5, yOff + 2, z - 5, 1, 7, 1, MB.concrete_pillar);
		DungeonToolbox.generateBox(world, x - 5, yOff + 2, z + 5, 1, 7, 1, MB.concrete_pillar);
		
		/// PRINT SPIKES ///
		int spikeCount = 36 + rand.nextInt(15);
		
		Vec3NT vec = Vec3NT.createVectorHelper(20, 0, 0);
		float rot = (float)Math.toRadians(360F / spikeCount);
		
		for(int i = 0; i < spikeCount; i++) {
			
			vec.rotateAroundY(rot);
			
			double variance = 1D + rand.nextDouble() * 0.4D;

			int ix = (int) (x + vec.xCoord * variance);
			int iz = (int) (z + vec.zCoord * variance);
			int iy = L.getHeightValue(world, ix, iz) - 3;
			
			for(int j = iy; j < iy + 7; j++) {
				L.setBlock(world, ix, j, iz, MB.deco_steel, 0, 2);
			}
		}
		
		/// GENERATE TUNNEL ///
		Vec3NT sVec = Vec3NT.createVectorHelper(10, 0, 0);
		float sRot = (float) Math.toRadians(360F / 32F);

		for(int i = y - 1; i < yOff + 2; i++) {

			int ix = (int) Math.floor(sVec.xCoord);
			int iz = (int) Math.floor(sVec.zCoord);
			
			int h = i < yOff ? 3 : 2;
			
			if(i > 40)
				DungeonToolbox.generateBox(world, x + ix - 1, i, z + iz - 1, 3, h, 3, VB.air);
			else
				DungeonToolbox.generateBox(world, x + ix - 1, i, z + iz - 1, 3, h, 3, MB.gas_radon_tomb);

			for(int dx = x + ix - 2; dx < x + ix + 3; dx++) {
				for(int dy = i - 1; dy < i + 4; dy++) {
					for(int dz = z + iz - 2; dz < z + iz + 3; dz++) {

						//if(dy >= yOff + 2)
						//	continue;

						LB b = L.getBlock(world, dx, dy, dz);

						if(b != VB.air && b != MB.gas_radon_tomb && b != MB.concrete && b != MB.concrete_smooth && b != MB.brick_concrete && b != MB.brick_concrete_cracked && b != MB.brick_concrete_broken) {

							MetaBlock meta = DungeonToolbox.getRandom(concrete, rand);
							L.setBlock(world, dx, dy, dz, meta.block, meta.meta, 3);
						}
					}
				}
			}

			sVec.rotateAroundY(sRot);
		}
		
		for(int dx = x + 4; dx < x + 8; dx++) {
			for(int dy = y - 1; dy < y + 4; dy++) {
				for(int dz = z - 2; dz < z + 3; dz++) {
					
					LB b = L.getBlock(world, dx, dy, dz);
					
					if(b != VB.air && b != MB.gas_radon_tomb && b != MB.concrete &&
							b != MB.concrete_smooth && b != MB.brick_concrete &&
							b != MB.brick_concrete_cracked && b != MB.brick_concrete_broken) {
						
						MetaBlock meta = DungeonToolbox.getRandom(concrete, rand);
						L.setBlock(world, dx, dy, dz, meta.block, meta.meta, 3);
					}
				}
			}
		}
		
		/// PRINT TOMB CHAMBER ///
		int size = 5;
		int cladding = size - 1;
		int core = size -2;

		int dimOuter = size * 2 + 1;
		int dimInner = cladding * 2 + 1;
		int dimCore = core * 2 + 1;

		DungeonToolbox.generateBox(world, x - size, y - size, z - size, 1, dimOuter, dimOuter, concrete);
		DungeonToolbox.generateBox(world, x - size, y - size, z - size, dimOuter, 1, dimOuter, concrete);
		DungeonToolbox.generateBox(world, x - size, y - size, z - size, dimOuter, dimOuter, 1, concrete);
		DungeonToolbox.generateBox(world, x + size, y - size, z - size, 1, dimOuter, dimOuter, concrete);
		DungeonToolbox.generateBox(world, x - size, y + size, z - size, dimOuter, 1, dimOuter, concrete);
		DungeonToolbox.generateBox(world, x - size, y - size, z + size, dimOuter, dimOuter, 1, concrete);
		
		DungeonToolbox.generateBox(world, x - cladding, y - cladding, z - cladding, 1, dimInner, dimInner, MB.brick_obsidian);
		DungeonToolbox.generateBox(world, x - cladding, y - cladding, z - cladding, dimInner, 1, dimInner, MB.brick_obsidian);
		DungeonToolbox.generateBox(world, x - cladding, y - cladding, z - cladding, dimInner, dimInner, 1, MB.brick_obsidian);
		DungeonToolbox.generateBox(world, x + cladding, y - cladding, z - cladding, 1, dimInner, dimInner, MB.brick_obsidian);
		DungeonToolbox.generateBox(world, x - cladding, y + cladding, z - cladding, dimInner, 1, dimInner, MB.brick_obsidian);
		DungeonToolbox.generateBox(world, x - cladding, y - cladding, z + cladding, dimInner, dimInner, 1, MB.brick_obsidian);

		DungeonToolbox.generateBox(world, x - core, y - core, z - core, dimCore, dimCore, dimCore, MB.ancient_scrap);
		
		/// PRINT ACCESS ///
		DungeonToolbox.generateBox(world, x + 6, y - 2, z - 1, 2, 1, 3, concrete);
		DungeonToolbox.generateBox(world, x + 4, y - 1, z - 1, 5, 3, 3, MB.gas_radon_tomb);
		DungeonToolbox.generateBox(world, x + 6, y + 2, z - 1, 2, 1, 3, concrete);
	}
}
