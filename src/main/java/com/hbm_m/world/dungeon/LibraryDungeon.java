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
 * 1:1 {@code com.hbm.world.dungeon.LibraryDungeon} (Original-Weltgen-Klasse, zeilengetreu auf die Port-Hilfen {@code L}/{@code MB}/{@code VB} umgesetzt).
 */
public class LibraryDungeon {

	public boolean LocationIsValidSpawn(LevelAccessor world, int x, int y, int z)
 {

		LB blockAbove = L.getBlock(world, x, y  + 8, z);
		LB blockBelow = L.getBlock(world, x, y - 1, z);
		
		if(blockAbove.getMaterial().isSolid() && blockBelow.getMaterial().isSolid() && y - 1 > 4)
		{
			return true;
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
		if(!LocationIsValidSpawn(world, x, y, z) || !LocationIsValidSpawn(world, x + 8, y, z) || !LocationIsValidSpawn(world, x + 8, y, z + 10) || !LocationIsValidSpawn(world, x, y, z + 10))
		{
			return false;
		}

		L.setBlock(world, x + 0, y + 0, z + 0, VB.stonebrick, 3, 3);
		L.setBlock(world, x + 1, y + 0, z + 0, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 2, y + 0, z + 0, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 3, y + 0, z + 0, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 4, y + 0, z + 0, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 5, y + 0, z + 0, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 6, y + 0, z + 0, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 7, y + 0, z + 0, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 8, y + 0, z + 0, VB.stonebrick, 3, 3);
		L.setBlock(world, x + 0, y + 0, z + 1, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 1, y + 0, z + 1, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 2, y + 0, z + 1, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 3, y + 0, z + 1, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 4, y + 0, z + 1, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 5, y + 0, z + 1, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 6, y + 0, z + 1, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 7, y + 0, z + 1, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 8, y + 0, z + 1, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 0, y + 0, z + 2, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 1, y + 0, z + 2, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 2, y + 0, z + 2, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 3, y + 0, z + 2, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 4, y + 0, z + 2, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 5, y + 0, z + 2, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 6, y + 0, z + 2, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 7, y + 0, z + 2, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 8, y + 0, z + 2, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 0, y + 0, z + 3, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 1, y + 0, z + 3, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 2, y + 0, z + 3, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 3, y + 0, z + 3, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 4, y + 0, z + 3, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 5, y + 0, z + 3, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 6, y + 0, z + 3, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 7, y + 0, z + 3, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 8, y + 0, z + 3, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 0, y + 0, z + 4, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 1, y + 0, z + 4, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 2, y + 0, z + 4, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 3, y + 0, z + 4, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 4, y + 0, z + 4, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 5, y + 0, z + 4, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 6, y + 0, z + 4, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 7, y + 0, z + 4, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 8, y + 0, z + 4, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 0, y + 0, z + 5, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 1, y + 0, z + 5, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 2, y + 0, z + 5, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 3, y + 0, z + 5, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 4, y + 0, z + 5, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 5, y + 0, z + 5, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 6, y + 0, z + 5, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 7, y + 0, z + 5, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 8, y + 0, z + 5, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 0, y + 0, z + 6, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 1, y + 0, z + 6, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 2, y + 0, z + 6, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 3, y + 0, z + 6, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 4, y + 0, z + 6, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 5, y + 0, z + 6, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 6, y + 0, z + 6, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 7, y + 0, z + 6, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 8, y + 0, z + 6, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 0, y + 0, z + 7, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 1, y + 0, z + 7, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 2, y + 0, z + 7, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 3, y + 0, z + 7, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 4, y + 0, z + 7, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 5, y + 0, z + 7, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 6, y + 0, z + 7, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 7, y + 0, z + 7, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 8, y + 0, z + 7, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 0, y + 0, z + 8, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 1, y + 0, z + 8, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 2, y + 0, z + 8, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 3, y + 0, z + 8, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 4, y + 0, z + 8, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 5, y + 0, z + 8, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 6, y + 0, z + 8, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 7, y + 0, z + 8, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 8, y + 0, z + 8, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 0, y + 0, z + 9, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 1, y + 0, z + 9, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 2, y + 0, z + 9, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 3, y + 0, z + 9, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 4, y + 0, z + 9, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 5, y + 0, z + 9, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 6, y + 0, z + 9, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 7, y + 0, z + 9, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 8, y + 0, z + 9, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 0, y + 0, z + 10, VB.stonebrick, 3, 3);
		L.setBlock(world, x + 1, y + 0, z + 10, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 2, y + 0, z + 10, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 3, y + 0, z + 10, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 4, y + 0, z + 10, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 5, y + 0, z + 10, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 6, y + 0, z + 10, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 7, y + 0, z + 10, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 8, y + 0, z + 10, VB.stonebrick, 3, 3);
		L.setBlock(world, x + 0, y + 1, z + 0, VB.stonebrick, 3, 3);
		L.setBlock(world, x + 1, y + 1, z + 0, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 2, y + 1, z + 0, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 3, y + 1, z + 0, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 4, y + 1, z + 0, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 5, y + 1, z + 0, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 6, y + 1, z + 0, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 7, y + 1, z + 0, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 8, y + 1, z + 0, VB.stonebrick, 3, 3);
		L.setBlock(world, x + 0, y + 1, z + 1, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 1, y + 1, z + 1, getShelf(rand), 0, 3);
		L.setBlock(world, x + 2, y + 1, z + 1, VB.air, 0, 3);
		L.setBlock(world, x + 3, y + 1, z + 1, VB.air, 0, 3);
		L.setBlock(world, x + 4, y + 1, z + 1, VB.air, 0, 3);
		L.setBlock(world, x + 5, y + 1, z + 1, VB.air, 0, 3);
		L.setBlock(world, x + 6, y + 1, z + 1, VB.air, 0, 3);
		L.setBlock(world, x + 7, y + 1, z + 1, VB.air, 0, 3);
		L.setBlock(world, x + 8, y + 1, z + 1, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 0, y + 1, z + 2, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 1, y + 1, z + 2, getShelf(rand), 0, 3);
		L.setBlock(world, x + 2, y + 1, z + 2, VB.air, 0, 3);
		L.setBlock(world, x + 3, y + 1, z + 2, VB.air, 0, 3);
		L.setBlock(world, x + 4, y + 1, z + 2, VB.air, 0, 3);
		L.setBlock(world, x + 5, y + 1, z + 2, VB.air, 0, 3);

		L.setBlock(world, x + 6, y + 1, z + 2, VB.mob_spawner, 0, 2);
        net.minecraft.world.level.block.entity.BlockEntity tileentitymobspawner = L.getTileEntity(world, x + 6, y + 1, z + 2);

        if (tileentitymobspawner != null)
        {
            L.setSpawnerEntity(tileentitymobspawner, this.pickMobSpawner(rand));
        }
        else
        {
            System.err.println("Failed to fetch mob spawner entity at (" + (x + 6) + ", " + (y + 1) + ", " + (z + 2) + ")");
        }
		L.setBlock(world, x + 7, y + 1, z + 2, VB.air, 0, 3);
		L.setBlock(world, x + 8, y + 1, z + 2, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 0, y + 1, z + 3, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 1, y + 1, z + 3, getShelf(rand), 0, 3);
		L.setBlock(world, x + 2, y + 1, z + 3, VB.air, 0, 3);
		L.setBlock(world, x + 3, y + 1, z + 3, VB.air, 0, 3);
		L.setBlock(world, x + 4, y + 1, z + 3, VB.fence, 0, 3);
		L.setBlock(world, x + 5, y + 1, z + 3, VB.air, 0, 3);
		L.setBlock(world, x + 6, y + 1, z + 3, VB.air, 0, 3);
		L.setBlock(world, x + 7, y + 1, z + 3, VB.air, 0, 3);
		L.setBlock(world, x + 8, y + 1, z + 3, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 0, y + 1, z + 4, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 1, y + 1, z + 4, getShelf(rand), 0, 3);
		L.setBlock(world, x + 2, y + 1, z + 4, VB.air, 0, 3);
		L.setBlock(world, x + 3, y + 1, z + 4, VB.air, 0, 3);
		L.setBlock(world, x + 4, y + 1, z + 4, VB.air, 0, 3);
		L.setBlock(world, x + 5, y + 1, z + 4, VB.air, 0, 3);
		L.setBlock(world, x + 6, y + 1, z + 4, VB.air, 0, 3);
		L.setBlock(world, x + 7, y + 1, z + 4, VB.air, 0, 3);
		L.setBlock(world, x + 8, y + 1, z + 4, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 0, y + 1, z + 5, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 1, y + 1, z + 5, VB.chest, 5, 3);
        Object tileentitychest = L.getTileEntity(world, x + 1, y + 1, z + 5);

        if (tileentitychest != null)
        {
            L.setLootTable(tileentitychest, "minecraft:chests/simple_dungeon", rand);
        }
		L.setBlock(world, x + 2, y + 1, z + 5, VB.air, 0, 3);
		L.setBlock(world, x + 3, y + 1, z + 5, VB.air, 0, 3);
		L.setBlock(world, x + 4, y + 1, z + 5, VB.air, 0, 3);
		L.setBlock(world, x + 5, y + 1, z + 5, VB.air, 0, 3);
		L.setBlock(world, x + 6, y + 1, z + 5, VB.air, 0, 3);
		L.setBlock(world, x + 7, y + 1, z + 5, VB.air, 0, 3);
		L.setBlock(world, x + 8, y + 1, z + 5, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 0, y + 1, z + 6, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 1, y + 1, z + 6, getShelf(rand), 0, 3);
		L.setBlock(world, x + 2, y + 1, z + 6, VB.air, 0, 3);
		L.setBlock(world, x + 3, y + 1, z + 6, VB.air, 0, 3);
		L.setBlock(world, x + 4, y + 1, z + 6, VB.fence, 0, 3);
		L.setBlock(world, x + 5, y + 1, z + 6, VB.air, 0, 3);
		L.setBlock(world, x + 6, y + 1, z + 6, VB.air, 0, 3);
		L.setBlock(world, x + 7, y + 1, z + 6, VB.air, 0, 3);
		L.setBlock(world, x + 8, y + 1, z + 6, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 0, y + 1, z + 7, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 1, y + 1, z + 7, getShelf(rand), 0, 3);
		L.setBlock(world, x + 2, y + 1, z + 7, VB.air, 0, 3);
		L.setBlock(world, x + 3, y + 1, z + 7, VB.air, 0, 3);
		L.setBlock(world, x + 4, y + 1, z + 7, VB.air, 0, 3);
		L.setBlock(world, x + 5, y + 1, z + 7, VB.air, 0, 3);
		L.setBlock(world, x + 6, y + 1, z + 7, VB.air, 0, 3);
		L.setBlock(world, x + 7, y + 1, z + 7, VB.air, 0, 3);
		L.setBlock(world, x + 8, y + 1, z + 7, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 0, y + 1, z + 8, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 1, y + 1, z + 8, VB.chest, 5, 3);
        Object tileentitychest1 = L.getTileEntity(world, x + 1, y + 1, z + 8);

        if (tileentitychest1 != null)
        {
            L.setLootTable(tileentitychest1, "minecraft:chests/simple_dungeon", rand);
        }
		L.setBlock(world, x + 2, y + 1, z + 8, VB.air, 0, 3);
		L.setBlock(world, x + 3, y + 1, z + 8, VB.air, 0, 3);
		L.setBlock(world, x + 4, y + 1, z + 8, VB.air, 0, 3);
		L.setBlock(world, x + 5, y + 1, z + 8, VB.air, 0, 3);
		L.setBlock(world, x + 6, y + 1, z + 8, VB.bedrock, 0, 3);

		L.setBlock(world, x + 6, y + 1, z + 8, VB.mob_spawner, 0, 2);
        net.minecraft.world.level.block.entity.BlockEntity tileentitymobspawner1 = L.getTileEntity(world, x + 6, y + 1, z + 8);

        if (tileentitymobspawner1 != null)
        {
            L.setSpawnerEntity(tileentitymobspawner1, this.pickMobSpawner(rand));
        }
        else
        {
            System.err.println("Failed to fetch mob spawner entity at (" + (x + 6) + ", " + (y + 1) + ", " + (z + 8) + ")");
        }
		L.setBlock(world, x + 7, y + 1, z + 8, VB.air, 0, 3);
		L.setBlock(world, x + 8, y + 1, z + 8, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 0, y + 1, z + 9, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 1, y + 1, z + 9, getShelf(rand), 0, 3);
		L.setBlock(world, x + 2, y + 1, z + 9, VB.air, 0, 3);
		L.setBlock(world, x + 3, y + 1, z + 9, VB.air, 0, 3);
		L.setBlock(world, x + 4, y + 1, z + 9, VB.air, 0, 3);
		L.setBlock(world, x + 5, y + 1, z + 9, VB.air, 0, 3);
		L.setBlock(world, x + 6, y + 1, z + 9, VB.air, 0, 3);
		L.setBlock(world, x + 7, y + 1, z + 9, VB.air, 0, 3);
		L.setBlock(world, x + 8, y + 1, z + 9, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 0, y + 1, z + 10, VB.stonebrick, 3, 3);
		L.setBlock(world, x + 1, y + 1, z + 10, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 2, y + 1, z + 10, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 3, y + 1, z + 10, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 4, y + 1, z + 10, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 5, y + 1, z + 10, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 6, y + 1, z + 10, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 7, y + 1, z + 10, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 8, y + 1, z + 10, VB.stonebrick, 3, 3);
		L.setBlock(world, x + 0, y + 2, z + 0, VB.stonebrick, 3, 3);
		L.setBlock(world, x + 1, y + 2, z + 0, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 2, y + 2, z + 0, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 3, y + 2, z + 0, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 4, y + 2, z + 0, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 5, y + 2, z + 0, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 6, y + 2, z + 0, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 7, y + 2, z + 0, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 8, y + 2, z + 0, VB.stonebrick, 3, 3);
		L.setBlock(world, x + 0, y + 2, z + 1, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 1, y + 2, z + 1, getShelf(rand), 0, 3);
		L.setBlock(world, x + 2, y + 2, z + 1, VB.air, 0, 3);
		L.setBlock(world, x + 3, y + 2, z + 1, VB.air, 0, 3);
		L.setBlock(world, x + 4, y + 2, z + 1, VB.air, 0, 3);
		L.setBlock(world, x + 5, y + 2, z + 1, VB.air, 0, 3);
		L.setBlock(world, x + 6, y + 2, z + 1, VB.air, 0, 3);
		L.setBlock(world, x + 7, y + 2, z + 1, VB.air, 0, 3);
		L.setBlock(world, x + 8, y + 2, z + 1, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 0, y + 2, z + 2, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 1, y + 2, z + 2, getShelf(rand), 0, 3);
		L.setBlock(world, x + 2, y + 2, z + 2, VB.air, 0, 3);
		L.setBlock(world, x + 3, y + 2, z + 2, VB.air, 0, 3);
		L.setBlock(world, x + 4, y + 2, z + 2, VB.air, 0, 3);
		L.setBlock(world, x + 5, y + 2, z + 2, VB.air, 0, 3);
		L.setBlock(world, x + 6, y + 2, z + 2, VB.air, 0, 3);
		L.setBlock(world, x + 7, y + 2, z + 2, VB.air, 0, 3);
		L.setBlock(world, x + 8, y + 2, z + 2, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 0, y + 2, z + 3, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 1, y + 2, z + 3, VB.chest, 5, 3);
        Object tileentitychest2 = L.getTileEntity(world, x + 1, y + 2, z + 3);

        if (tileentitychest2 != null)
        {
            L.setLootTable(tileentitychest2, "minecraft:chests/simple_dungeon", rand);
        }
		L.setBlock(world, x + 2, y + 2, z + 3, VB.air, 0, 3);
		L.setBlock(world, x + 3, y + 2, z + 3, VB.air, 0, 3);
		L.setBlock(world, x + 4, y + 2, z + 3, VB.fence, 0, 3);
		L.setBlock(world, x + 5, y + 2, z + 3, VB.air, 0, 3);
		L.setBlock(world, x + 6, y + 2, z + 3, VB.air, 0, 3);
		L.setBlock(world, x + 7, y + 2, z + 3, VB.air, 0, 3);
		L.setBlock(world, x + 8, y + 2, z + 3, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 0, y + 2, z + 4, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 1, y + 2, z + 4, getShelf(rand), 0, 3);
		L.setBlock(world, x + 2, y + 2, z + 4, VB.air, 0, 3);
		L.setBlock(world, x + 3, y + 2, z + 4, VB.air, 0, 3);
		L.setBlock(world, x + 4, y + 2, z + 4, VB.air, 0, 3);
		L.setBlock(world, x + 5, y + 2, z + 4, VB.air, 0, 3);
		L.setBlock(world, x + 6, y + 2, z + 4, VB.air, 0, 3);
		L.setBlock(world, x + 7, y + 2, z + 4, VB.air, 0, 3);
		L.setBlock(world, x + 8, y + 2, z + 4, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 0, y + 2, z + 5, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 1, y + 2, z + 5, VB.wooden_slab, 8, 3);
		L.setBlock(world, x + 2, y + 2, z + 5, VB.air, 0, 3);
		L.setBlock(world, x + 3, y + 2, z + 5, VB.air, 0, 3);
		L.setBlock(world, x + 4, y + 2, z + 5, VB.air, 0, 3);
		L.setBlock(world, x + 5, y + 2, z + 5, VB.air, 0, 3);
		L.setBlock(world, x + 6, y + 2, z + 5, VB.air, 0, 3);
		L.setBlock(world, x + 7, y + 2, z + 5, VB.air, 0, 3);
		L.setBlock(world, x + 8, y + 2, z + 5, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 0, y + 2, z + 6, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 1, y + 2, z + 6, getShelf(rand), 0, 3);
		L.setBlock(world, x + 2, y + 2, z + 6, VB.air, 0, 3);
		L.setBlock(world, x + 3, y + 2, z + 6, VB.air, 0, 3);
		L.setBlock(world, x + 4, y + 2, z + 6, VB.fence, 0, 3);
		L.setBlock(world, x + 5, y + 2, z + 6, VB.air, 0, 3);
		L.setBlock(world, x + 6, y + 2, z + 6, VB.air, 0, 3);
		L.setBlock(world, x + 7, y + 2, z + 6, VB.air, 0, 3);
		L.setBlock(world, x + 8, y + 2, z + 6, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 0, y + 2, z + 7, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 1, y + 2, z + 7, getShelf(rand), 0, 3);
		L.setBlock(world, x + 2, y + 2, z + 7, VB.air, 0, 3);
		L.setBlock(world, x + 3, y + 2, z + 7, VB.air, 0, 3);
		L.setBlock(world, x + 4, y + 2, z + 7, VB.air, 0, 3);
		L.setBlock(world, x + 5, y + 2, z + 7, VB.air, 0, 3);
		L.setBlock(world, x + 6, y + 2, z + 7, VB.air, 0, 3);
		L.setBlock(world, x + 7, y + 2, z + 7, VB.air, 0, 3);
		L.setBlock(world, x + 8, y + 2, z + 7, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 0, y + 2, z + 8, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 1, y + 2, z + 8, VB.wooden_slab, 8, 3);
		L.setBlock(world, x + 2, y + 2, z + 8, VB.air, 0, 3);
		L.setBlock(world, x + 3, y + 2, z + 8, VB.air, 0, 3);
		L.setBlock(world, x + 4, y + 2, z + 8, VB.air, 0, 3);
		L.setBlock(world, x + 5, y + 2, z + 8, VB.air, 0, 3);
		L.setBlock(world, x + 6, y + 2, z + 8, VB.air, 0, 3);
		L.setBlock(world, x + 7, y + 2, z + 8, VB.air, 0, 3);
		L.setBlock(world, x + 8, y + 2, z + 8, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 0, y + 2, z + 9, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 1, y + 2, z + 9, getShelf(rand), 0, 3);
		L.setBlock(world, x + 2, y + 2, z + 9, VB.air, 0, 3);
		L.setBlock(world, x + 3, y + 2, z + 9, VB.air, 0, 3);
		L.setBlock(world, x + 4, y + 2, z + 9, VB.air, 0, 3);
		L.setBlock(world, x + 5, y + 2, z + 9, VB.air, 0, 3);
		L.setBlock(world, x + 6, y + 2, z + 9, VB.air, 0, 3);
		L.setBlock(world, x + 7, y + 2, z + 9, VB.air, 0, 3);
		L.setBlock(world, x + 8, y + 2, z + 9, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 0, y + 2, z + 10, VB.stonebrick, 3, 3);
		L.setBlock(world, x + 1, y + 2, z + 10, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 2, y + 2, z + 10, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 3, y + 2, z + 10, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 4, y + 2, z + 10, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 5, y + 2, z + 10, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 6, y + 2, z + 10, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 7, y + 2, z + 10, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 8, y + 2, z + 10, VB.stonebrick, 3, 3);
		L.setBlock(world, x + 0, y + 3, z + 0, VB.stonebrick, 3, 3);
		L.setBlock(world, x + 1, y + 3, z + 0, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 2, y + 3, z + 0, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 3, y + 3, z + 0, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 4, y + 3, z + 0, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 5, y + 3, z + 0, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 6, y + 3, z + 0, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 7, y + 3, z + 0, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 8, y + 3, z + 0, VB.stonebrick, 3, 3);
		L.setBlock(world, x + 0, y + 3, z + 1, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 1, y + 3, z + 1, VB.air, 0, 3);
		L.setBlock(world, x + 2, y + 3, z + 1, VB.air, 0, 3);
		L.setBlock(world, x + 3, y + 3, z + 1, VB.air, 0, 3);
		L.setBlock(world, x + 4, y + 3, z + 1, VB.air, 0, 3);
		L.setBlock(world, x + 5, y + 3, z + 1, VB.air, 0, 3);
		L.setBlock(world, x + 6, y + 3, z + 1, VB.air, 0, 3);
		L.setBlock(world, x + 7, y + 3, z + 1, VB.air, 0, 3);
		L.setBlock(world, x + 8, y + 3, z + 1, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 0, y + 3, z + 2, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 1, y + 3, z + 2, VB.air, 0, 3);
		L.setBlock(world, x + 2, y + 3, z + 2, VB.air, 0, 3);
		L.setBlock(world, x + 3, y + 3, z + 2, VB.air, 0, 3);
		L.setBlock(world, x + 5, y + 3, z + 2, VB.air, 0, 3);
		L.setBlock(world, x + 6, y + 3, z + 2, VB.air, 0, 3);
		L.setBlock(world, x + 7, y + 3, z + 2, VB.air, 0, 3);
		L.setBlock(world, x + 8, y + 3, z + 2, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 0, y + 3, z + 3, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 1, y + 3, z + 3, VB.wooden_slab, 8, 3);
		L.setBlock(world, x + 2, y + 3, z + 3, VB.wooden_slab, 8, 3);
		L.setBlock(world, x + 3, y + 3, z + 3, VB.wooden_slab, 8, 3);
		L.setBlock(world, x + 4, y + 3, z + 3, VB.double_wooden_slab, 0, 3);
		L.setBlock(world, x + 6, y + 3, z + 3, VB.air, 0, 3);
		L.setBlock(world, x + 7, y + 3, z + 3, VB.air, 0, 3);
		L.setBlock(world, x + 8, y + 3, z + 3, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 0, y + 3, z + 4, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 1, y + 3, z + 4, VB.wooden_slab, 8, 3);
		L.setBlock(world, x + 2, y + 3, z + 4, VB.wooden_slab, 8, 3);
		L.setBlock(world, x + 3, y + 3, z + 4, VB.wooden_slab, 8, 3);
		L.setBlock(world, x + 4, y + 3, z + 4, VB.wooden_slab, 8, 3);
		L.setBlock(world, x + 5, y + 3, z + 4, VB.air, 0, 3);
		L.setBlock(world, x + 6, y + 3, z + 4, VB.air, 0, 3);
		L.setBlock(world, x + 7, y + 3, z + 4, VB.air, 0, 3);
		L.setBlock(world, x + 8, y + 3, z + 4, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 0, y + 3, z + 5, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 1, y + 3, z + 5, VB.wooden_slab, 8, 3);
		L.setBlock(world, x + 2, y + 3, z + 5, VB.wooden_slab, 8, 3);
		L.setBlock(world, x + 3, y + 3, z + 5, VB.wooden_slab, 8, 3);
		L.setBlock(world, x + 4, y + 3, z + 5, VB.wooden_slab, 8, 3);
		L.setBlock(world, x + 5, y + 3, z + 5, VB.air, 0, 3);
		L.setBlock(world, x + 6, y + 3, z + 5, VB.air, 0, 3);
		L.setBlock(world, x + 7, y + 3, z + 5, VB.air, 0, 3);
		L.setBlock(world, x + 8, y + 3, z + 5, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 0, y + 3, z + 6, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 1, y + 3, z + 6, VB.wooden_slab, 8, 3);
		L.setBlock(world, x + 2, y + 3, z + 6, VB.wooden_slab, 8, 3);
		L.setBlock(world, x + 3, y + 3, z + 6, VB.wooden_slab, 8, 3);
		L.setBlock(world, x + 4, y + 3, z + 6, VB.double_wooden_slab, 0, 3);
		L.setBlock(world, x + 6, y + 3, z + 6, VB.air, 0, 3);
		L.setBlock(world, x + 7, y + 3, z + 6, VB.air, 0, 3);
		L.setBlock(world, x + 8, y + 3, z + 6, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 0, y + 3, z + 7, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 1, y + 3, z + 7, VB.wooden_slab, 8, 3);
		L.setBlock(world, x + 2, y + 3, z + 7, VB.wooden_slab, 8, 3);
		L.setBlock(world, x + 3, y + 3, z + 7, VB.wooden_slab, 8, 3);
		L.setBlock(world, x + 4, y + 3, z + 7, VB.wooden_slab, 8, 3);
		L.setBlock(world, x + 5, y + 3, z + 7, VB.air, 0, 3);
		L.setBlock(world, x + 6, y + 3, z + 7, VB.air, 0, 3);
		L.setBlock(world, x + 7, y + 3, z + 7, VB.air, 0, 3);
		L.setBlock(world, x + 8, y + 3, z + 7, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 0, y + 3, z + 8, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 1, y + 3, z + 8, VB.wooden_slab, 8, 3);
		L.setBlock(world, x + 2, y + 3, z + 8, VB.wooden_slab, 8, 3);
		L.setBlock(world, x + 3, y + 3, z + 8, VB.wooden_slab, 8, 3);
		L.setBlock(world, x + 4, y + 3, z + 8, VB.wooden_slab, 8, 3);
		L.setBlock(world, x + 5, y + 3, z + 8, VB.air, 0, 3);
		L.setBlock(world, x + 6, y + 3, z + 8, VB.air, 0, 3);
		L.setBlock(world, x + 7, y + 3, z + 8, VB.air, 0, 3);
		L.setBlock(world, x + 8, y + 3, z + 8, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 0, y + 3, z + 9, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 1, y + 3, z + 9, VB.wooden_slab, 8, 3);
		L.setBlock(world, x + 2, y + 3, z + 9, VB.wooden_slab, 8, 3);
		L.setBlock(world, x + 3, y + 3, z + 9, VB.wooden_slab, 8, 3);
		L.setBlock(world, x + 4, y + 3, z + 9, VB.wooden_slab, 8, 3);
		L.setBlock(world, x + 5, y + 3, z + 9, VB.air, 0, 3);
		L.setBlock(world, x + 6, y + 3, z + 9, VB.air, 0, 3);
		L.setBlock(world, x + 7, y + 3, z + 9, VB.air, 0, 3);
		L.setBlock(world, x + 8, y + 3, z + 9, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 0, y + 3, z + 10, VB.stonebrick, 3, 3);
		L.setBlock(world, x + 1, y + 3, z + 10, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 2, y + 3, z + 10, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 3, y + 3, z + 10, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 4, y + 3, z + 10, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 5, y + 3, z + 10, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 6, y + 3, z + 10, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 7, y + 3, z + 10, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 8, y + 3, z + 10, VB.stonebrick, 3, 3);
		L.setBlock(world, x + 0, y + 4, z + 0, VB.stonebrick, 3, 3);
		L.setBlock(world, x + 1, y + 4, z + 0, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 2, y + 4, z + 0, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 3, y + 4, z + 0, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 4, y + 4, z + 0, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 5, y + 4, z + 0, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 6, y + 4, z + 0, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 7, y + 4, z + 0, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 8, y + 4, z + 0, VB.stonebrick, 3, 3);
		L.setBlock(world, x + 0, y + 4, z + 1, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 1, y + 4, z + 1, VB.air, 0, 3);
		L.setBlock(world, x + 2, y + 4, z + 1, VB.air, 0, 3);
		L.setBlock(world, x + 3, y + 4, z + 1, VB.air, 0, 3);
		L.setBlock(world, x + 4, y + 4, z + 1, VB.air, 0, 3);
		L.setBlock(world, x + 5, y + 4, z + 1, VB.air, 0, 3);
		L.setBlock(world, x + 6, y + 4, z + 1, VB.air, 0, 3);
		L.setBlock(world, x + 7, y + 4, z + 1, VB.air, 0, 3);
		L.setBlock(world, x + 8, y + 4, z + 1, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 0, y + 4, z + 2, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 1, y + 4, z + 2, VB.air, 0, 3);
		L.setBlock(world, x + 2, y + 4, z + 2, VB.air, 0, 3);
		L.setBlock(world, x + 3, y + 4, z + 2, VB.air, 0, 3);
		L.setBlock(world, x + 4, y + 4, z + 2, VB.air, 0, 3);
		L.setBlock(world, x + 5, y + 4, z + 2, VB.air, 0, 3);
		L.setBlock(world, x + 6, y + 4, z + 2, VB.air, 0, 3);
		L.setBlock(world, x + 7, y + 4, z + 2, VB.air, 0, 3);
		L.setBlock(world, x + 8, y + 4, z + 2, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 0, y + 4, z + 3, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 1, y + 4, z + 3, VB.air, 0, 3);
		L.setBlock(world, x + 2, y + 4, z + 3, VB.air, 0, 3);
		L.setBlock(world, x + 3, y + 4, z + 3, VB.air, 0, 3);
		L.setBlock(world, x + 4, y + 4, z + 3, VB.fence, 0, 3);
		L.setBlock(world, x + 5, y + 4, z + 3, VB.air, 0, 3);
		L.setBlock(world, x + 6, y + 4, z + 3, VB.air, 0, 3);
		L.setBlock(world, x + 7, y + 4, z + 3, VB.air, 0, 3);
		L.setBlock(world, x + 8, y + 4, z + 3, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 0, y + 4, z + 4, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 1, y + 4, z + 4, getShelf(rand), 0, 3);
		L.setBlock(world, x + 2, y + 4, z + 4, VB.air, 0, 3);
		L.setBlock(world, x + 3, y + 4, z + 4, VB.air, 0, 3);
		L.setBlock(world, x + 4, y + 4, z + 4, VB.air, 0, 3);
		L.setBlock(world, x + 5, y + 4, z + 4, VB.air, 0, 3);
		L.setBlock(world, x + 6, y + 4, z + 4, VB.air, 0, 3);
		L.setBlock(world, x + 7, y + 4, z + 4, VB.air, 0, 3);
		L.setBlock(world, x + 8, y + 4, z + 4, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 0, y + 4, z + 5, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 1, y + 4, z + 5, VB.chest, 5, 3);
        Object tileentitychest3 = L.getTileEntity(world, x + 1, y + 4, z + 5);

        if (tileentitychest3 != null)
        {
            L.setLootTable(tileentitychest3, "minecraft:chests/simple_dungeon", rand);
        }
		L.setBlock(world, x + 2, y + 4, z + 5, VB.air, 0, 3);
		L.setBlock(world, x + 3, y + 4, z + 5, VB.bedrock, 0, 3);

		L.setBlock(world, x + 3, y + 4, z + 5, VB.mob_spawner, 0, 2);
        net.minecraft.world.level.block.entity.BlockEntity tileentitymobspawner2 = L.getTileEntity(world, x + 3, y + 4, z + 5);

        if (tileentitymobspawner2 != null)
        {
            L.setSpawnerEntity(tileentitymobspawner2, this.pickMobSpawner(rand));
        }
        else
        {
            System.err.println("Failed to fetch mob spawner entity at (" + (x + 3) + ", " + (y + 4) + ", " + (z + 5) + ")");
        }
		L.setBlock(world, x + 4, y + 4, z + 5, VB.air, 0, 3);
		L.setBlock(world, x + 5, y + 4, z + 5, VB.air, 0, 3);
		L.setBlock(world, x + 6, y + 4, z + 5, VB.air, 0, 3);
		L.setBlock(world, x + 7, y + 4, z + 5, VB.air, 0, 3);
		L.setBlock(world, x + 8, y + 4, z + 5, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 0, y + 4, z + 6, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 1, y + 4, z + 6, getShelf(rand), 0, 3);
		L.setBlock(world, x + 2, y + 4, z + 6, VB.air, 0, 3);
		L.setBlock(world, x + 3, y + 4, z + 6, VB.air, 0, 3);
		L.setBlock(world, x + 4, y + 4, z + 6, VB.fence, 0, 3);
		L.setBlock(world, x + 5, y + 4, z + 6, VB.air, 0, 3);
		L.setBlock(world, x + 6, y + 4, z + 6, VB.air, 0, 3);
		L.setBlock(world, x + 7, y + 4, z + 6, VB.air, 0, 3);
		L.setBlock(world, x + 8, y + 4, z + 6, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 0, y + 4, z + 7, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 1, y + 4, z + 7, getShelf(rand), 0, 3);
		L.setBlock(world, x + 2, y + 4, z + 7, VB.air, 0, 3);
		L.setBlock(world, x + 3, y + 4, z + 7, VB.air, 0, 3);
		L.setBlock(world, x + 4, y + 4, z + 7, VB.air, 0, 3);
		L.setBlock(world, x + 5, y + 4, z + 7, VB.air, 0, 3);
		L.setBlock(world, x + 6, y + 4, z + 7, VB.air, 0, 3);
		L.setBlock(world, x + 7, y + 4, z + 7, VB.air, 0, 3);
		L.setBlock(world, x + 8, y + 4, z + 7, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 0, y + 4, z + 8, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 1, y + 4, z + 8, getShelf(rand), 0, 3);
		L.setBlock(world, x + 2, y + 4, z + 8, VB.air, 0, 3);
		L.setBlock(world, x + 3, y + 4, z + 8, VB.air, 0, 3);
		L.setBlock(world, x + 4, y + 4, z + 8, VB.air, 0, 3);
		L.setBlock(world, x + 5, y + 4, z + 8, VB.air, 0, 3);
		L.setBlock(world, x + 6, y + 4, z + 8, VB.air, 0, 3);
		L.setBlock(world, x + 7, y + 4, z + 8, VB.air, 0, 3);
		L.setBlock(world, x + 8, y + 4, z + 8, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 0, y + 4, z + 9, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 1, y + 4, z + 9, getShelf(rand), 0, 3);
		L.setBlock(world, x + 2, y + 4, z + 9, VB.chest, 2, 3);
        Object tileentitychest4 = L.getTileEntity(world, x + 2, y + 4, z + 9);

        if (tileentitychest4 != null)
        {
            L.setLootTable(tileentitychest4, "minecraft:chests/simple_dungeon", rand);
        }
		L.setBlock(world, x + 3, y + 4, z + 9, getShelf(rand), 0, 3);
		L.setBlock(world, x + 4, y + 4, z + 9, VB.air, 0, 3);
		L.setBlock(world, x + 5, y + 4, z + 9, VB.air, 0, 3);
		L.setBlock(world, x + 6, y + 4, z + 9, VB.air, 0, 3);
		L.setBlock(world, x + 7, y + 4, z + 9, VB.air, 0, 3);
		L.setBlock(world, x + 8, y + 4, z + 9, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 0, y + 4, z + 10, VB.stonebrick, 3, 3);
		L.setBlock(world, x + 1, y + 4, z + 10, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 2, y + 4, z + 10, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 3, y + 4, z + 10, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 4, y + 4, z + 10, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 5, y + 4, z + 10, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 6, y + 4, z + 10, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 7, y + 4, z + 10, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 8, y + 4, z + 10, VB.stonebrick, 3, 3);
		L.setBlock(world, x + 0, y + 5, z + 0, VB.stonebrick, 3, 3);
		L.setBlock(world, x + 1, y + 5, z + 0, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 2, y + 5, z + 0, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 3, y + 5, z + 0, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 4, y + 5, z + 0, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 5, y + 5, z + 0, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 6, y + 5, z + 0, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 7, y + 5, z + 0, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 8, y + 5, z + 0, VB.stonebrick, 3, 3);
		L.setBlock(world, x + 0, y + 5, z + 1, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 1, y + 5, z + 1, VB.air, 0, 3);
		L.setBlock(world, x + 2, y + 5, z + 1, VB.air, 0, 3);
		L.setBlock(world, x + 3, y + 5, z + 1, VB.air, 0, 3);
		L.setBlock(world, x + 4, y + 5, z + 1, VB.wooden_slab, 8, 3);
		L.setBlock(world, x + 5, y + 5, z + 1, VB.wooden_slab, 8, 3);
		L.setBlock(world, x + 6, y + 5, z + 1, VB.wooden_slab, 8, 3);
		L.setBlock(world, x + 7, y + 5, z + 1, VB.wooden_slab, 8, 3);
		L.setBlock(world, x + 8, y + 5, z + 1, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 0, y + 5, z + 2, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 1, y + 5, z + 2, VB.air, 0, 3);
		L.setBlock(world, x + 2, y + 5, z + 2, VB.air, 0, 3);
		L.setBlock(world, x + 3, y + 5, z + 2, VB.air, 0, 3);
		L.setBlock(world, x + 4, y + 5, z + 2, VB.wooden_slab, 8, 3);
		L.setBlock(world, x + 5, y + 5, z + 2, VB.wooden_slab, 8, 3);
		L.setBlock(world, x + 6, y + 5, z + 2, VB.wooden_slab, 8, 3);
		L.setBlock(world, x + 7, y + 5, z + 2, VB.wooden_slab, 8, 3);
		L.setBlock(world, x + 8, y + 5, z + 2, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 0, y + 5, z + 3, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 1, y + 5, z + 3, VB.air, 0, 3);
		L.setBlock(world, x + 2, y + 5, z + 3, VB.air, 0, 3);
		L.setBlock(world, x + 4, y + 5, z + 3, VB.double_wooden_slab, 0, 3);
		L.setBlock(world, x + 5, y + 5, z + 3, VB.wooden_slab, 8, 3);
		L.setBlock(world, x + 6, y + 5, z + 3, VB.wooden_slab, 8, 3);
		L.setBlock(world, x + 7, y + 5, z + 3, VB.wooden_slab, 8, 3);
		L.setBlock(world, x + 8, y + 5, z + 3, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 0, y + 5, z + 4, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 1, y + 5, z + 4, getShelf(rand), 0, 3);
		L.setBlock(world, x + 2, y + 5, z + 4, VB.air, 0, 3);
		L.setBlock(world, x + 3, y + 5, z + 4, VB.air, 0, 3);
		L.setBlock(world, x + 4, y + 5, z + 4, VB.wooden_slab, 8, 3);
		L.setBlock(world, x + 5, y + 5, z + 4, VB.wooden_slab, 8, 3);
		L.setBlock(world, x + 6, y + 5, z + 4, VB.wooden_slab, 8, 3);
		L.setBlock(world, x + 7, y + 5, z + 4, VB.wooden_slab, 8, 3);
		L.setBlock(world, x + 8, y + 5, z + 4, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 0, y + 5, z + 5, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 1, y + 5, z + 5, VB.wooden_slab, 8, 3);
		L.setBlock(world, x + 2, y + 5, z + 5, VB.air, 0, 3);
		L.setBlock(world, x + 3, y + 5, z + 5, VB.air, 0, 3);
		L.setBlock(world, x + 4, y + 5, z + 5, VB.wooden_slab, 8, 3);
		L.setBlock(world, x + 5, y + 5, z + 5, VB.wooden_slab, 8, 3);
		L.setBlock(world, x + 6, y + 5, z + 5, VB.wooden_slab, 8, 3);
		L.setBlock(world, x + 7, y + 5, z + 5, VB.wooden_slab, 8, 3);
		L.setBlock(world, x + 8, y + 5, z + 5, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 0, y + 5, z + 6, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 1, y + 5, z + 6, getShelf(rand), 0, 3);
		L.setBlock(world, x + 2, y + 5, z + 6, VB.air, 0, 3);
		L.setBlock(world, x + 4, y + 5, z + 6, VB.double_wooden_slab, 0, 3);
		L.setBlock(world, x + 5, y + 5, z + 6, VB.wooden_slab, 8, 3);
		L.setBlock(world, x + 6, y + 5, z + 6, VB.wooden_slab, 8, 3);
		L.setBlock(world, x + 7, y + 5, z + 6, VB.wooden_slab, 8, 3);
		L.setBlock(world, x + 8, y + 5, z + 6, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 0, y + 5, z + 7, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 1, y + 5, z + 7, VB.chest, 5, 3);
        Object tileentitychest5 = L.getTileEntity(world, x + 1, y + 5, z + 7);

        if (tileentitychest5 != null)
        {
            L.setLootTable(tileentitychest5, "minecraft:chests/simple_dungeon", rand);
        }
		L.setBlock(world, x + 2, y + 5, z + 7, VB.air, 0, 3);
		L.setBlock(world, x + 3, y + 5, z + 7, VB.air, 0, 3);
		L.setBlock(world, x + 5, y + 5, z + 7, VB.air, 0, 3);
		L.setBlock(world, x + 6, y + 5, z + 7, VB.air, 0, 3);
		L.setBlock(world, x + 7, y + 5, z + 7, VB.air, 0, 3);
		L.setBlock(world, x + 8, y + 5, z + 7, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 0, y + 5, z + 8, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 1, y + 5, z + 8, getShelf(rand), 0, 3);
		L.setBlock(world, x + 2, y + 5, z + 8, VB.air, 0, 3);
		L.setBlock(world, x + 3, y + 5, z + 8, VB.air, 0, 3);
		L.setBlock(world, x + 4, y + 5, z + 8, VB.air, 0, 3);
		L.setBlock(world, x + 5, y + 5, z + 8, VB.air, 0, 3);
		L.setBlock(world, x + 6, y + 5, z + 8, VB.air, 0, 3);
		L.setBlock(world, x + 7, y + 5, z + 8, VB.air, 0, 3);
		L.setBlock(world, x + 8, y + 5, z + 8, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 0, y + 5, z + 9, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 1, y + 5, z + 9, getShelf(rand), 0, 3);
		L.setBlock(world, x + 2, y + 5, z + 9, VB.wooden_slab, 8, 3);
		L.setBlock(world, x + 3, y + 5, z + 9, getShelf(rand), 0, 3);
		L.setBlock(world, x + 4, y + 5, z + 9, VB.air, 0, 3);
		L.setBlock(world, x + 5, y + 5, z + 9, VB.air, 0, 3);
		L.setBlock(world, x + 6, y + 5, z + 9, VB.air, 0, 3);
		L.setBlock(world, x + 7, y + 5, z + 9, VB.air, 0, 3);
		L.setBlock(world, x + 8, y + 5, z + 9, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 0, y + 5, z + 10, VB.stonebrick, 3, 3);
		L.setBlock(world, x + 1, y + 5, z + 10, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 2, y + 5, z + 10, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 3, y + 5, z + 10, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 4, y + 5, z + 10, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 5, y + 5, z + 10, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 6, y + 5, z + 10, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 7, y + 5, z + 10, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 8, y + 5, z + 10, VB.stonebrick, 3, 3);
		L.setBlock(world, x + 0, y + 6, z + 0, VB.stonebrick, 3, 3);
		L.setBlock(world, x + 1, y + 6, z + 0, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 2, y + 6, z + 0, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 3, y + 6, z + 0, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 4, y + 6, z + 0, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 5, y + 6, z + 0, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 6, y + 6, z + 0, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 7, y + 6, z + 0, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 8, y + 6, z + 0, VB.stonebrick, 3, 3);
		L.setBlock(world, x + 0, y + 6, z + 1, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 1, y + 6, z + 1, VB.air, 0, 3);
		L.setBlock(world, x + 2, y + 6, z + 1, VB.air, 0, 3);
		L.setBlock(world, x + 3, y + 6, z + 1, VB.air, 0, 3);
		L.setBlock(world, x + 4, y + 6, z + 1, VB.air, 0, 3);
		L.setBlock(world, x + 5, y + 6, z + 1, getShelf(rand), 0, 3);
		L.setBlock(world, x + 6, y + 6, z + 1, VB.chest, 3, 3);
        Object tileentitychest6 = L.getTileEntity(world, x + 6, y + 6, z + 1);

        if (tileentitychest6 != null)
        {
            L.setLootTable(tileentitychest6, "minecraft:chests/simple_dungeon", rand);
        }
		L.setBlock(world, x + 7, y + 6, z + 1, getShelf(rand), 0, 3);
		L.setBlock(world, x + 8, y + 6, z + 1, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 0, y + 6, z + 2, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 1, y + 6, z + 2, VB.air, 0, 3);
		L.setBlock(world, x + 2, y + 6, z + 2, VB.air, 0, 3);
		L.setBlock(world, x + 3, y + 6, z + 2, VB.air, 0, 3);
		L.setBlock(world, x + 4, y + 6, z + 2, VB.air, 0, 3);
		L.setBlock(world, x + 5, y + 6, z + 2, VB.air, 0, 3);
		L.setBlock(world, x + 6, y + 6, z + 2, VB.air, 0, 3);
		L.setBlock(world, x + 7, y + 6, z + 2, getShelf(rand), 0, 3);
		L.setBlock(world, x + 8, y + 6, z + 2, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 0, y + 6, z + 3, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 1, y + 6, z + 3, VB.air, 0, 3);
		L.setBlock(world, x + 2, y + 6, z + 3, VB.air, 0, 3);
		L.setBlock(world, x + 3, y + 6, z + 3, VB.air, 0, 3);
		L.setBlock(world, x + 4, y + 6, z + 3, VB.fence, 0, 3);
		L.setBlock(world, x + 5, y + 6, z + 3, VB.air, 0, 3);
		L.setBlock(world, x + 6, y + 6, z + 3, VB.air, 0, 3);
		L.setBlock(world, x + 7, y + 6, z + 3, getShelf(rand), 0, 3);
		L.setBlock(world, x + 8, y + 6, z + 3, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 0, y + 6, z + 4, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 1, y + 6, z + 4, VB.air, 0, 3);
		L.setBlock(world, x + 2, y + 6, z + 4, VB.air, 0, 3);
		L.setBlock(world, x + 3, y + 6, z + 4, VB.air, 0, 3);
		L.setBlock(world, x + 4, y + 6, z + 4, VB.air, 0, 3);
		L.setBlock(world, x + 5, y + 6, z + 4, VB.bedrock, 0, 3);

		L.setBlock(world, x + 5, y + 6, z + 4, VB.mob_spawner, 0, 2);
        net.minecraft.world.level.block.entity.BlockEntity tileentitymobspawner3 = L.getTileEntity(world, x + 5, y + 6, z + 4);

        if (tileentitymobspawner3 != null)
        {
            L.setSpawnerEntity(tileentitymobspawner3, this.pickMobSpawner(rand));
        }
        else
        {
            System.err.println("Failed to fetch mob spawner entity at (" + (x + 5) + ", " + (y + 6) + ", " + (z + 4) + ")");
        }
		L.setBlock(world, x + 6, y + 6, z + 4, VB.air, 0, 3);
		L.setBlock(world, x + 7, y + 6, z + 4, VB.chest, 4, 3);
        Object tileentitychest7 = L.getTileEntity(world, x + 7, y + 6, z + 4);

        if (tileentitychest7 != null)
        {
            L.setLootTable(tileentitychest7, "minecraft:chests/simple_dungeon", rand);
        }
		L.setBlock(world, x + 8, y + 6, z + 4, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 0, y + 6, z + 5, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 1, y + 6, z + 5, VB.air, 0, 3);
		L.setBlock(world, x + 2, y + 6, z + 5, VB.air, 0, 3);
		L.setBlock(world, x + 3, y + 6, z + 5, VB.air, 0, 3);
		L.setBlock(world, x + 4, y + 6, z + 5, VB.air, 0, 3);
		L.setBlock(world, x + 5, y + 6, z + 5, VB.air, 0, 3);
		L.setBlock(world, x + 6, y + 6, z + 5, VB.air, 0, 3);
		L.setBlock(world, x + 7, y + 6, z + 5, getShelf(rand), 0, 3);
		L.setBlock(world, x + 8, y + 6, z + 5, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 0, y + 6, z + 6, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 1, y + 6, z + 6, VB.air, 0, 3);
		L.setBlock(world, x + 2, y + 6, z + 6, VB.air, 0, 3);
		L.setBlock(world, x + 3, y + 6, z + 6, VB.air, 0, 3);
		L.setBlock(world, x + 4, y + 6, z + 6, VB.fence, 0, 3);
		L.setBlock(world, x + 5, y + 6, z + 6, VB.air, 0, 3);
		L.setBlock(world, x + 6, y + 6, z + 6, VB.air, 0, 3);
		L.setBlock(world, x + 7, y + 6, z + 6, VB.air, 0, 3);
		L.setBlock(world, x + 8, y + 6, z + 6, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 0, y + 6, z + 7, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 1, y + 6, z + 7, VB.air, 0, 3);
		L.setBlock(world, x + 2, y + 6, z + 7, VB.air, 0, 3);
		L.setBlock(world, x + 3, y + 6, z + 7, VB.air, 0, 3);
		L.setBlock(world, x + 4, y + 6, z + 7, VB.air, 0, 3);
		L.setBlock(world, x + 5, y + 6, z + 7, VB.air, 0, 3);
		L.setBlock(world, x + 6, y + 6, z + 7, VB.air, 0, 3);
		L.setBlock(world, x + 7, y + 6, z + 7, VB.air, 0, 3);
		L.setBlock(world, x + 8, y + 6, z + 7, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 0, y + 6, z + 8, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 1, y + 6, z + 8, VB.air, 0, 3);
		L.setBlock(world, x + 2, y + 6, z + 8, VB.air, 0, 3);
		L.setBlock(world, x + 3, y + 6, z + 8, VB.air, 0, 3);
		L.setBlock(world, x + 4, y + 6, z + 8, VB.air, 0, 3);
		L.setBlock(world, x + 5, y + 6, z + 8, VB.air, 0, 3);
		L.setBlock(world, x + 6, y + 6, z + 8, VB.air, 0, 3);
		L.setBlock(world, x + 7, y + 6, z + 8, VB.air, 0, 3);
		L.setBlock(world, x + 8, y + 6, z + 8, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 0, y + 6, z + 9, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 1, y + 6, z + 9, VB.air, 0, 3);
		L.setBlock(world, x + 2, y + 6, z + 9, VB.air, 0, 3);
		L.setBlock(world, x + 3, y + 6, z + 9, VB.air, 0, 3);
		L.setBlock(world, x + 4, y + 6, z + 9, VB.air, 0, 3);
		L.setBlock(world, x + 5, y + 6, z + 9, VB.air, 0, 3);
		L.setBlock(world, x + 6, y + 6, z + 9, VB.air, 0, 3);
		L.setBlock(world, x + 7, y + 6, z + 9, VB.air, 0, 3);
		L.setBlock(world, x + 8, y + 6, z + 9, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 0, y + 6, z + 10, VB.stonebrick, 3, 3);
		L.setBlock(world, x + 1, y + 6, z + 10, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 2, y + 6, z + 10, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 3, y + 6, z + 10, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 4, y + 6, z + 10, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 5, y + 6, z + 10, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 6, y + 6, z + 10, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 7, y + 6, z + 10, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 8, y + 6, z + 10, VB.stonebrick, 3, 3);
		L.setBlock(world, x + 0, y + 7, z + 0, VB.stonebrick, 3, 3);
		L.setBlock(world, x + 1, y + 7, z + 0, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 2, y + 7, z + 0, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 3, y + 7, z + 0, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 4, y + 7, z + 0, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 5, y + 7, z + 0, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 6, y + 7, z + 0, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 7, y + 7, z + 0, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 8, y + 7, z + 0, VB.stonebrick, 3, 3);
		L.setBlock(world, x + 0, y + 7, z + 1, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 1, y + 7, z + 1, VB.air, 0, 3);
		L.setBlock(world, x + 2, y + 7, z + 1, VB.air, 0, 3);
		L.setBlock(world, x + 3, y + 7, z + 1, VB.air, 0, 3);
		L.setBlock(world, x + 4, y + 7, z + 1, VB.air, 0, 3);
		L.setBlock(world, x + 5, y + 7, z + 1, getShelf(rand), 0, 3);
		L.setBlock(world, x + 6, y + 7, z + 1, VB.wooden_slab, 8, 3);
		L.setBlock(world, x + 7, y + 7, z + 1, getShelf(rand), 0, 3);
		L.setBlock(world, x + 8, y + 7, z + 1, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 0, y + 7, z + 2, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 1, y + 7, z + 2, VB.air, 0, 3);
		L.setBlock(world, x + 2, y + 7, z + 2, VB.air, 0, 3);
		L.setBlock(world, x + 3, y + 7, z + 2, VB.air, 0, 3);
		L.setBlock(world, x + 4, y + 7, z + 2, VB.air, 0, 3);
		L.setBlock(world, x + 5, y + 7, z + 2, VB.air, 0, 3);
		L.setBlock(world, x + 6, y + 7, z + 2, VB.air, 0, 3);
		L.setBlock(world, x + 7, y + 7, z + 2, getShelf(rand), 0, 3);
		L.setBlock(world, x + 8, y + 7, z + 2, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 0, y + 7, z + 3, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 1, y + 7, z + 3, VB.air, 0, 3);
		L.setBlock(world, x + 2, y + 7, z + 3, VB.air, 0, 3);
		L.setBlock(world, x + 3, y + 7, z + 3, VB.air, 0, 3);
		L.setBlock(world, x + 4, y + 7, z + 3, VB.fence, 0, 3);
		L.setBlock(world, x + 5, y + 7, z + 3, VB.air, 0, 3);
		L.setBlock(world, x + 6, y + 7, z + 3, VB.air, 0, 3);
		L.setBlock(world, x + 7, y + 7, z + 3, getShelf(rand), 0, 3);
		L.setBlock(world, x + 8, y + 7, z + 3, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 0, y + 7, z + 4, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 1, y + 7, z + 4, VB.air, 0, 3);
		L.setBlock(world, x + 2, y + 7, z + 4, VB.air, 0, 3);
		L.setBlock(world, x + 3, y + 7, z + 4, VB.air, 0, 3);
		L.setBlock(world, x + 4, y + 7, z + 4, VB.air, 0, 3);
		L.setBlock(world, x + 5, y + 7, z + 4, VB.air, 0, 3);
		L.setBlock(world, x + 6, y + 7, z + 4, VB.air, 0, 3);
		L.setBlock(world, x + 7, y + 7, z + 4, VB.wooden_slab, 8, 3);
		L.setBlock(world, x + 8, y + 7, z + 4, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 0, y + 7, z + 5, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 1, y + 7, z + 5, VB.air, 0, 3);
		L.setBlock(world, x + 2, y + 7, z + 5, VB.air, 0, 3);
		L.setBlock(world, x + 3, y + 7, z + 5, VB.air, 0, 3);
		L.setBlock(world, x + 4, y + 7, z + 5, VB.air, 0, 3);
		L.setBlock(world, x + 5, y + 7, z + 5, VB.air, 0, 3);
		L.setBlock(world, x + 6, y + 7, z + 5, VB.air, 0, 3);
		L.setBlock(world, x + 7, y + 7, z + 5, getShelf(rand), 0, 3);
		L.setBlock(world, x + 8, y + 7, z + 5, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 0, y + 7, z + 6, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 1, y + 7, z + 6, VB.air, 0, 3);
		L.setBlock(world, x + 2, y + 7, z + 6, VB.air, 0, 3);
		L.setBlock(world, x + 3, y + 7, z + 6, VB.air, 0, 3);
		L.setBlock(world, x + 4, y + 7, z + 6, VB.fence, 0, 3);
		L.setBlock(world, x + 5, y + 7, z + 6, VB.air, 0, 3);
		L.setBlock(world, x + 6, y + 7, z + 6, VB.air, 0, 3);
		L.setBlock(world, x + 7, y + 7, z + 6, VB.air, 0, 3);
		L.setBlock(world, x + 8, y + 7, z + 6, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 0, y + 7, z + 7, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 1, y + 7, z + 7, VB.air, 0, 3);
		L.setBlock(world, x + 2, y + 7, z + 7, VB.air, 0, 3);
		L.setBlock(world, x + 3, y + 7, z + 7, VB.air, 0, 3);
		L.setBlock(world, x + 4, y + 7, z + 7, VB.air, 0, 3);
		L.setBlock(world, x + 5, y + 7, z + 7, VB.air, 0, 3);
		L.setBlock(world, x + 6, y + 7, z + 7, VB.air, 0, 3);
		L.setBlock(world, x + 7, y + 7, z + 7, VB.air, 0, 3);
		L.setBlock(world, x + 8, y + 7, z + 7, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 0, y + 7, z + 8, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 1, y + 7, z + 8, VB.air, 0, 3);
		L.setBlock(world, x + 2, y + 7, z + 8, VB.air, 0, 3);
		L.setBlock(world, x + 3, y + 7, z + 8, VB.air, 0, 3);
		L.setBlock(world, x + 4, y + 7, z + 8, VB.air, 0, 3);
		L.setBlock(world, x + 5, y + 7, z + 8, VB.air, 0, 3);
		L.setBlock(world, x + 6, y + 7, z + 8, VB.air, 0, 3);
		L.setBlock(world, x + 7, y + 7, z + 8, VB.air, 0, 3);
		L.setBlock(world, x + 8, y + 7, z + 8, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 0, y + 7, z + 9, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 1, y + 7, z + 9, VB.air, 0, 3);
		L.setBlock(world, x + 2, y + 7, z + 9, VB.air, 0, 3);
		L.setBlock(world, x + 3, y + 7, z + 9, VB.air, 0, 3);
		L.setBlock(world, x + 4, y + 7, z + 9, VB.air, 0, 3);
		L.setBlock(world, x + 5, y + 7, z + 9, VB.air, 0, 3);
		L.setBlock(world, x + 6, y + 7, z + 9, VB.air, 0, 3);
		L.setBlock(world, x + 7, y + 7, z + 9, VB.air, 0, 3);
		L.setBlock(world, x + 8, y + 7, z + 9, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 0, y + 7, z + 10, VB.stonebrick, 3, 3);
		L.setBlock(world, x + 1, y + 7, z + 10, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 2, y + 7, z + 10, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 3, y + 7, z + 10, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 4, y + 7, z + 10, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 5, y + 7, z + 10, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 6, y + 7, z + 10, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 7, y + 7, z + 10, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 8, y + 7, z + 10, VB.stonebrick, 3, 3);
		L.setBlock(world, x + 0, y + 8, z + 0, VB.stonebrick, 3, 3);
		L.setBlock(world, x + 1, y + 8, z + 0, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 2, y + 8, z + 0, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 3, y + 8, z + 0, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 4, y + 8, z + 0, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 5, y + 8, z + 0, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 6, y + 8, z + 0, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 7, y + 8, z + 0, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 8, y + 8, z + 0, VB.stonebrick, 3, 3);
		L.setBlock(world, x + 0, y + 8, z + 1, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 1, y + 8, z + 1, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 2, y + 8, z + 1, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 3, y + 8, z + 1, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 4, y + 8, z + 1, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 5, y + 8, z + 1, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 6, y + 8, z + 1, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 7, y + 8, z + 1, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 8, y + 8, z + 1, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 0, y + 8, z + 2, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 1, y + 8, z + 2, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 2, y + 8, z + 2, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 3, y + 8, z + 2, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 4, y + 8, z + 2, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 5, y + 8, z + 2, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 6, y + 8, z + 2, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 7, y + 8, z + 2, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 8, y + 8, z + 2, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 0, y + 8, z + 3, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 1, y + 8, z + 3, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 2, y + 8, z + 3, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 3, y + 8, z + 3, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 4, y + 8, z + 3, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 5, y + 8, z + 3, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 6, y + 8, z + 3, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 7, y + 8, z + 3, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 8, y + 8, z + 3, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 0, y + 8, z + 4, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 1, y + 8, z + 4, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 2, y + 8, z + 4, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 3, y + 8, z + 4, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 4, y + 8, z + 4, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 5, y + 8, z + 4, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 6, y + 8, z + 4, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 7, y + 8, z + 4, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 8, y + 8, z + 4, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 0, y + 8, z + 5, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 1, y + 8, z + 5, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 2, y + 8, z + 5, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 3, y + 8, z + 5, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 4, y + 8, z + 5, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 5, y + 8, z + 5, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 6, y + 8, z + 5, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 7, y + 8, z + 5, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 8, y + 8, z + 5, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 0, y + 8, z + 6, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 1, y + 8, z + 6, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 2, y + 8, z + 6, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 3, y + 8, z + 6, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 4, y + 8, z + 6, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 5, y + 8, z + 6, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 6, y + 8, z + 6, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 7, y + 8, z + 6, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 8, y + 8, z + 6, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 0, y + 8, z + 7, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 1, y + 8, z + 7, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 2, y + 8, z + 7, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 3, y + 8, z + 7, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 4, y + 8, z + 7, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 5, y + 8, z + 7, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 6, y + 8, z + 7, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 7, y + 8, z + 7, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 8, y + 8, z + 7, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 0, y + 8, z + 8, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 1, y + 8, z + 8, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 2, y + 8, z + 8, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 3, y + 8, z + 8, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 4, y + 8, z + 8, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 5, y + 8, z + 8, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 6, y + 8, z + 8, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 7, y + 8, z + 8, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 8, y + 8, z + 8, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 0, y + 8, z + 9, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 1, y + 8, z + 9, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 2, y + 8, z + 9, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 3, y + 8, z + 9, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 4, y + 8, z + 9, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 5, y + 8, z + 9, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 6, y + 8, z + 9, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 7, y + 8, z + 9, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 8, y + 8, z + 9, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 0, y + 8, z + 10, VB.stonebrick, 3, 3);
		L.setBlock(world, x + 1, y + 8, z + 10, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 2, y + 8, z + 10, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 3, y + 8, z + 10, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 4, y + 8, z + 10, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 5, y + 8, z + 10, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 6, y + 8, z + 10, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 7, y + 8, z + 10, VB.stonebrick, getBrick(rand), 3);
		L.setBlock(world, x + 8, y + 8, z + 10, VB.stonebrick, 3, 3);

		generate_r02_last(world, rand, x, y, z);
		return true;

	}
	public boolean generate_r02_last(LevelAccessor world, Random rand, int x, int y, int z)
	{

		/*L.setBlock(world, x + 4, y + 3, z + 2, VB.torch, 4, 3);
		L.setBlock(world, x + 5, y + 3, z + 3, VB.torch, 1, 3);
		L.setBlock(world, x + 5, y + 3, z + 6, VB.torch, 1, 3);
		L.setBlock(world, x + 3, y + 5, z + 3, VB.torch, 2, 3);
		L.setBlock(world, x + 3, y + 5, z + 6, VB.torch, 2, 3);
		L.setBlock(world, x + 4, y + 5, z + 7, VB.torch, 3, 3);*/
		L.setBlock(world, x + 4, y + 3, z + 2, VB.air, 0, 3);
		L.setBlock(world, x + 5, y + 3, z + 3, VB.air, 0, 3);
		L.setBlock(world, x + 5, y + 3, z + 6, VB.air, 0, 3);
		L.setBlock(world, x + 3, y + 5, z + 3, VB.air, 0, 3);
		L.setBlock(world, x + 3, y + 5, z + 6, VB.air, 0, 3);
		L.setBlock(world, x + 4, y + 5, z + 7, VB.air, 0, 3);
		if(GeneralConfig.enableDebugMode)
			System.out.print("[Debug] Successfully spawned library at " + x + " " + y +" " + z + "\n");
		return true;

	}
	public int getBrick(Random rand) {
		return rand.nextInt(3);
		
	}
	public LB getShelf(Random rand) {
		int i = rand.nextInt(2);
		if(i == 0)
		{
			return VB.planks;
		}
		return VB.bookshelf;
	}
    private String pickMobSpawner(Random p_76543_1_)
    {
        return L.getRandomDungeonMob(p_76543_1_);
    }

}