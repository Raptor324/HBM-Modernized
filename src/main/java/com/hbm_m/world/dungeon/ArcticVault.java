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
 * 1:1 {@code com.hbm.world.dungeon.ArcticVault} (Original-Weltgen-Klasse, zeilengetreu auf die Port-Hilfen {@code L}/{@code MB}/{@code VB} umgesetzt).
 */
public class ArcticVault {
	
	public void trySpawn(LevelAccessor world, int x, int y, int z) {
		
		y--;

		
		if(L.getFloatTemperature(world, x, y, z) < 0.2 && L.getBlock(world, x, y, z).getMaterial() == LMaterial.rock) {
			build(world, x, y, z);
		}
		
	}
	
	private void build(LevelAccessor world, int x, int y, int z) {

		List<MetaBlock> brick = Arrays.asList(new MetaBlock[] {new MetaBlock(VB.stonebrick), new MetaBlock(VB.stonebrick, 2)});
		List<MetaBlock> web = Arrays.asList(new MetaBlock[] {new MetaBlock(VB.air), new MetaBlock(VB.air), new MetaBlock(VB.web)});
		List<MetaBlock> crates = Arrays.asList(new MetaBlock[] {new MetaBlock(MB.crate), new MetaBlock(MB.crate_metal), new MetaBlock(MB.crate_ammo), new MetaBlock(MB.crate_can), new MetaBlock(MB.crate_jungle)});

		DungeonToolbox.generateBox(world, x - 5, y, z - 5, 11, 1, 11, brick);
		DungeonToolbox.generateBox(world, x - 5, y + 6, z - 5, 11, 1, 11, brick);
		DungeonToolbox.generateBox(world, x - 5, y + 1, z - 5, 11, 5, 1, brick);
		DungeonToolbox.generateBox(world, x - 5, y + 1, z + 5, 11, 5, 1, brick);
		DungeonToolbox.generateBox(world, x - 5, y + 1, z - 5, 1, 5, 11, brick);
		DungeonToolbox.generateBox(world, x + 5, y + 1, z - 5, 1, 5, 11, brick);
		DungeonToolbox.generateBox(world, x - 4, y + 1, z - 4, 9, 3, 9, VB.air);
		DungeonToolbox.generateBox(world, x - 4, y + 1, z - 4, 9, 1, 9, new MetaBlock(VB.snow_layer));
		DungeonToolbox.generateBox(world, x - 2, y + 1, z - 2, 5, 2, 1, new MetaBlock(MB.tape_recorder, 3));
		DungeonToolbox.generateBox(world, x - 2, y + 3, z - 2, 5, 1, 1, new MetaBlock(VB.snow_layer));
		DungeonToolbox.generateBox(world, x - 2, y + 1, z + 2, 5, 2, 1, new MetaBlock(MB.tape_recorder, 2));
		DungeonToolbox.generateBox(world, x - 2, y + 3, z + 2, 5, 1, 1, new MetaBlock(VB.snow_layer));
		DungeonToolbox.generateBox(world, x - 4, y + 4, z - 4, 9, 2, 9, web);
		
		for(int i = 0; i < 15; i++) {
			int ix = x - 4 + L.rand(world).nextInt(10);
			int iz = z - 4 + L.rand(world).nextInt(10);
			
			if(L.getBlock(world, ix, y + 1, iz) == VB.snow_layer) {
				
				if(i == 0) {
					L.setBlock(world, ix, y + 1, iz, MB.bobblehead, L.rand(world).nextInt(16), 3);
					net.minecraft.world.level.block.entity.BlockEntity bobble = L.getTileEntity(world, ix, y + 1, iz);
					
					if(bobble instanceof com.hbm_m.blockentity.decorations.TrinketBlockEntity trinket) {
						// BobbleType: Index 0 ist NONE
						trinket.setType(L.rand(world).nextInt(com.hbm_m.block.decorations.TrinketTypes.BobbleType.values().length - 1) + 1);
					}
					
				} else {
					MetaBlock b = DungeonToolbox.getRandom(crates, L.rand(world));
					L.setBlock(world, ix, y + 1, iz, b.block, b.meta, 2);
					L.setBlock(world, ix, y + 2, iz, VB.snow_layer);
				}
			}
		}
		
		int iy = L.getHeightValue(world, x, z);

		if(L.getBlock(world, x, iy - 1, z).canPlaceTorchOnTop(world, x, iy - 1, z)) {
			L.setBlock(world, x, iy, z, MB.tape_recorder);
		}
		
		if(GeneralConfig.enableDebugMode)
			MainRegistry.LOGGER.info("[Debug] Successfully spawned arctic code vault at " + x + " " + y + " " + z);
	}
}
