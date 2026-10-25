package com.hbm_m.world.gen.component;

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
import com.hbm_m.world.gen.component.Component.*;
import com.hbm_m.world.gen.legacy.L;
import com.hbm_m.world.gen.legacy.LB;
import com.hbm_m.world.gen.legacy.LMaterial;
import com.hbm_m.world.gen.legacy.MB;
import com.hbm_m.world.gen.legacy.MetaBlock;
import com.hbm_m.world.gen.legacy.VB;
import com.hbm_m.world.generator.DungeonToolbox;

import net.minecraft.world.Container;
import net.minecraft.world.level.LevelAccessor;
import com.hbm_m.world.gen.ProceduralStructureStart;
import com.hbm_m.world.gen.ProceduralStructureStart.ProceduralComponent;
import com.hbm_m.world.gen.ProceduralStructureStart.Weight;
import com.hbm_m.util.LootGenerator;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;





/* Described as "Civilian", as that's the overarching connection between all of these structures. Unlike the ruins, there's not enough to
 * compartmentalize even further. Just in general many of the structures I consider lower-quality (except for the sandstone houses; those are actually pretty nice).
 */
/**
 * 1:1 {@code com.hbm.world.gen.component.CivilianFeatures} (Original-Weltgen-Klasse, zeilengetreu auf die Port-Hilfen {@code L}/{@code MB}/{@code VB} umgesetzt).
 */
public class CivilianFeatures {

	public static void registerComponents() {
					}

	/** Sandstone Ruin 1 */
	public static class NTMHouse1 extends Component {

		private boolean hasPlacedChest;

		private static Sandstone RandomSandstone = new Sandstone();

		public NTMHouse1(CompoundTag nbt) {
			super(ComponentTypes.NTMHouse1.get(), nbt);
			readLegacy(nbt);
		}

		/** Constructor for this feature; takes coordinates for bounding box */
		public NTMHouse1(Random rand, int minX, int minZ) {
			super(ComponentTypes.NTMHouse1.get(), rand, minX, 64, minZ, 9, 4, 6);
			this.hasPlacedChest = false;
		}

		@Override
		protected void writeLegacy(CompoundTag nbt) {
			super.writeLegacy(nbt);
			nbt.putBoolean("hasChest", this.hasPlacedChest);
		}

		@Override
		protected void readLegacy(CompoundTag nbt) {
			super.readLegacy(nbt);
			this.hasPlacedChest = nbt.getBoolean("hasChest");
		}

		/**
		 * Generates structures.
		 */
		@Override
		public boolean addComponentParts(LevelAccessor world, Random rand, BoundingBox box) {
			/*
			 * Places block at current position. Dependent on coordinate mode, i.e. will allow for random rotation, so use this instead of setBlock!
			 * this.placeBlockAtCurrentPosition(world, block, minX, metadata, x, y, z, box);
			 * Fills an area with air, self-explanatory. Use to clear interiors of unwanted blocks.
			 * this.fillWithAir(world, box, minX, minY, minZ, maxX, maxY, maxZ);
			 * Fills an area with blocks, self-explanatory.
			 * this.fillWithBlocks(world, box, minX, minY, minZ, maxX, maxY, maxZ, blockToPlace, blockToReplace, alwaysReplace);
			 * Fills an area with metadata blocks, self-explanatory.
			 * this.fillWithMetadataBlocks(world, box, minX, minY, minZ, maxX, maxY, maxZ, blockToPlace, blockPlaceMeta, blockToReplace, replaceBlockMeta, alwaysReplace);
			 * Fills an area with randomized blocks, self-explanatory.
			 * this.fillWithRandomizedBlocks(world, box, minX, minY, minZ, maxX, maxY, maxZ, alwaysReplace, rand, StructurePiece.blockSelector);
			 * (BlockSelector is basically a list of blocks that can be randomly picked, except that it can actually be weighted)
			 * Replaces any air or water blocks with this block down. Useful for foundations
			 * this.func_151554_b(world, block, metadata, x, startAtY, z, box
			 * Fills an area with blocks randomly - look into randLimit?
			 * this.randomlyFillWithBlocks(world, box, rand, randLimit, minX, minY, minZ, maxX, maxY, maxZ, blockToPlace, blockToReplace, alwaysReplace);
			 */

			//System.out.println(this.coordBaseMode);
			if(!this.setAverageHeight(world, box, this.boundingBox.minY())) {
				return false;
			}
			//System.out.println("" + this.boundingBox.minX() + ", " + this.boundingBox.minY() + ", " + this.boundingBox.minZ());

			placeFoundationUnderneath(world, VB.sandstone, 0, 0, 0, 9, 6, -1, box);

			//Walls
			this.fillWithRandomizedBlocks(world, box, 0, 0, 0, 9, 0, 0, false, rand, RandomSandstone); //Back Wall
			this.fillWithRandomizedBlocks(world, box, 0, 1, 0, 1, 1, 0, false, rand, RandomSandstone);
			this.placeBlockAtCurrentPosition(world, VB.fence, 0, 2, 1, 0, box);
			this.fillWithRandomizedBlocks(world, box, 3, 1, 0, 5, 1, 0, false, rand, RandomSandstone);
			this.placeBlockAtCurrentPosition(world, VB.fence, 0, 6, 1, 0, box);
			this.placeBlockAtCurrentPosition(world, VB.fence, 0, 7, 1, 0, box);
			this.fillWithRandomizedBlocks(world, box, 9 - 1, 1, 0, 9, 1, 0, false, rand, RandomSandstone);
			this.fillWithRandomizedBlocks(world, box, 0, 2, 0, 9 - 2, 2, 0, false, rand, RandomSandstone);
			this.fillWithRandomizedBlocks(world, box, 0, 0, 0, 0, 1, 6, false, rand, RandomSandstone); //Left Wall
			this.placeBlockAtCurrentPosition(world, VB.stone_slab, 1, 0, 2, 1, box);
			this.fillWithMetadataBlocks(world, box, 0, 2, 3, 0, 2, 6, VB.stone_slab, 1, VB.air, 0, false);
			this.fillWithRandomizedBlocks(world, box, 1, 0, 6, 1, 1, 6, false, rand, RandomSandstone); //Front Wall
			this.fillWithRandomizedBlocks(world, box, 3, 0, 6, 9, 1, 6, false, rand, RandomSandstone);
			this.fillWithRandomizedBlocks(world, box, 1, 2, 6, 3, 2, 6, false, rand, RandomSandstone);
			this.fillWithMetadataBlocks(world, box, 4, 2, 6, 5, 2, 6, VB.stone_slab, 1, VB.air, 0, false);
			this.placeBlockAtCurrentPosition(world, VB.stone_slab, 1, 9 - 2, 2, 6, box);
			this.fillWithRandomizedBlocks(world, box, 9, 0, 0, 9, 0, 6, false, rand, RandomSandstone); //Right Wall
			this.randomlyFillWithBlocks(world, box, rand, 0.65F, 9, 1, 1, 9, 1, 6 - 1, VB.sand, VB.air, false);

			this.fillWithRandomizedBlocks(world, box, 4, 0, 1, 4, 1, 3, false, rand, RandomSandstone);
			this.placeBlockAtCurrentPosition(world, MB.reinforced_sand, 0, 4, 0, 4, box);

			//Loot/Sand
			this.placeBlockAtCurrentPosition(world, MB.crate_weapon, 0, 1, 0, 1, box);
			if(!this.hasPlacedChest)
				this.hasPlacedChest = this.generateStructureChestContents(world, box, rand, 3, 0, 1, ItemPool.getPool(ItemPoolsLegacy.POOL_GENERIC), rand.nextInt(2) + 8);
			this.fillWithBlocks(world, box, 5, 0, 1, 6, 0, 1, MB.crate, VB.air, false);
			this.placeBlockAtCurrentPosition(world, VB.sand, 0, 7, 0, 1, box);
			if(rand.nextFloat() <= 0.25)
				 this.placeBlockAtCurrentPosition(world, MB.crate_metal, 0, 9 - 1, 0, 1, box);
			this.randomlyFillWithBlocks(world, box, rand, 0.25F, 1, 0, 2, 3, 0, 6 - 1, VB.sand, VB.air, false);
			this.randomlyFillWithBlocks(world, box, rand, 0.25F, 5, 0, 2, 9 - 1, 0, 6 - 1, VB.sand, VB.air, false);

			return true;
		}

	}

	public static class NTMHouse2 extends Component {

		private static Sandstone RandomSandstone = new Sandstone();

		private boolean[] hasPlacedLoot = new boolean[2];

		public NTMHouse2(CompoundTag nbt) {
			super(ComponentTypes.NTMHouse2.get(), nbt);
			readLegacy(nbt);
		}

		public NTMHouse2(Random rand, int minX, int minZ) {
			super(ComponentTypes.NTMHouse2.get(), rand, minX, 64, minZ, 15, 5, 9);
			this.hasPlacedLoot[0] = false;
			this.hasPlacedLoot[1] = false;
		}

		@Override
		protected void writeLegacy(CompoundTag nbt) {
			super.writeLegacy(nbt);
			nbt.putBoolean("hasLoot1", this.hasPlacedLoot[0]);
			nbt.putBoolean("hasLoot2", this.hasPlacedLoot[1]);
		}

		@Override
		protected void readLegacy(CompoundTag nbt) {
			super.readLegacy(nbt);
			this.hasPlacedLoot[0] = nbt.getBoolean("hasLoot1");
			this.hasPlacedLoot[1] = nbt.getBoolean("hasLoot2");
		}

		@Override
		public boolean addComponentParts(LevelAccessor world, Random rand, BoundingBox box) {

			//System.out.print(this.coordBaseMode);
			if(!this.setAverageHeight(world, box, this.boundingBox.minY())) {
				return false;
			}
			//System.out.println("" + this.boundingBox.minX() + ", " + this.boundingBox.minY() + ", " + this.boundingBox.minZ());

			placeFoundationUnderneath(world, VB.sandstone, 0, 0, 0, 6, 15, -1, box);
			placeFoundationUnderneath(world, VB.sandstone, 0, 9, 0, 15, 9, -1, box);

			this.fillWithAir(world, box, 1, 0, 1, 5, 5, 9 - 1);

			//House 1
			this.fillWithRandomizedBlocks(world, box, 0, 0, 0, 6, 1, 0, false, rand, RandomSandstone); //Back Wall
			this.fillWithRandomizedBlocks(world, box, 0, 2, 0, 1, 2, 0, false, rand, RandomSandstone);
			this.placeBlockAtCurrentPosition(world, VB.fence, 0, 2, 2, 0, box);
			this.fillWithRandomizedBlocks(world, box, 3, 2, 0, 3, 2, 0, false, rand, RandomSandstone);
			this.placeBlockAtCurrentPosition(world, VB.fence, 0, 4, 2, 0, box);
			this.fillWithRandomizedBlocks(world, box, 5, 2, 0, 6, 2, 0, false, rand, RandomSandstone);
			this.fillWithRandomizedBlocks(world, box, 0, 3, 0, 6, 3, 0, false, rand, RandomSandstone);
			this.fillWithRandomizedBlocks(world, box, 0, 0, 1, 0, 3, 9, false, rand, RandomSandstone); //Left Wall
			this.fillWithRandomizedBlocks(world, box, 1, 0, 9, 6, 1, 9, false, rand, RandomSandstone); //Front Wall
			this.fillWithRandomizedBlocks(world, box, 1, 2, 9, 1, 2, 9, false, rand, RandomSandstone);
			this.fillWithBlocks(world, box, 2, 2, 9, 4, 2, 9, VB.fence, VB.air, false);
			this.fillWithRandomizedBlocks(world, box, 5, 2, 9, 6, 2, 9, false, rand, RandomSandstone);
			this.fillWithRandomizedBlocks(world, box, 1, 3, 9, 6, 3, 9, false, rand, RandomSandstone);
			this.fillWithRandomizedBlocks(world, box, 6, 0, 9 - 1, 6, 3, 9 - 1, false, rand, RandomSandstone); //Right Wall
			this.fillWithRandomizedBlocks(world, box, 6, 0, 9 - 2, 6, 0, 9 - 2, false, rand, RandomSandstone);
			this.fillWithRandomizedBlocks(world, box, 6, 3, 9 - 2, 6, 3, 9 - 2, false, rand, RandomSandstone);
			this.fillWithRandomizedBlocks(world, box, 6, 0, 1, 6, 3, 9 - 3, false, rand, RandomSandstone);

			this.fillWithBlocks(world, box, 1, 0, 1, 5, 0, 9 - 1, VB.sandstone, VB.air, false); //Floor
			//this.fillWithRandomizedBlocks(world, box, 1, 5 - 1, 0, 5, 5 - 1, 9, false, rand, RandomSandstone); //Ceiling
			this.fillWithBlocks(world, box, 1, 5 - 1, 0, 5, 5 - 1, 9, VB.sandstone, VB.air, false);
			this.fillWithMetadataBlocks(world, box, 0, 5 - 1, 0, 0, 5 - 1, 9, VB.stone_slab, 1, VB.air, 0, false); //Roof
			this.fillWithMetadataBlocks(world, box, 6, 5 - 1, 0, 6, 5 - 1, 9, VB.stone_slab, 1, VB.air, 0, false);
			this.fillWithMetadataBlocks(world, box, 2, 5, 0, 4, 5, 0, VB.stone_slab, 1, VB.air, 0, false);
			this.fillWithMetadataBlocks(world, box, 3, 5, 1, 3, 5, 2, VB.stone_slab, 1, VB.air, 0, false);
			this.fillWithMetadataBlocks(world, box, 3, 5, 4, 3, 5, 6, VB.stone_slab, 1, VB.air, 0, false);
			this.placeBlockAtCurrentPosition(world, VB.stone_slab, 1, 3, 5, 9 - 1, box);
			this.fillWithMetadataBlocks(world, box, 2, 5, 9, 4, 5, 9, VB.stone_slab, 1, VB.air, 0, false);

			//House 2
			this.fillWithRandomizedBlocks(world, box, 15 - 6, 0, 0, 15, 0, 0, false, rand, RandomSandstone); //Back Wall
			this.fillWithRandomizedBlocks(world, box, 15 - 6, 1, 0, 15 - 2, 1, 0, false, rand, RandomSandstone);
			this.fillWithRandomizedBlocks(world, box, 15 - 6, 2, 0, 15 - 6, 2, 0, false, rand, RandomSandstone);
			this.placeBlockAtCurrentPosition(world, VB.stone_slab, 1, 15 - 6, 2, 0, box);
			this.placeBlockAtCurrentPosition(world, VB.stone_slab, 1, 15 - 3, 2, 0, box);
			this.fillWithRandomizedBlocks(world, box, 15 - 6, 0, 1, 15 - 6, 3, 1, false, rand, RandomSandstone); //Left Wall
			this.fillWithRandomizedBlocks(world, box, 15 - 6, 0, 2, 15 - 6, 0, 2, false, rand, RandomSandstone);
			this.fillWithRandomizedBlocks(world, box, 15 - 6, 3, 2, 15 - 6, 3, 9 - 1, false, rand, RandomSandstone);
			this.placeBlockAtCurrentPosition(world, VB.stone_slab, 1, 15 - 6, 5 - 1, 2, box);
			this.fillWithMetadataBlocks(world, box, 15 - 6, 5 - 1, 4, 15 - 6, 5 - 1, 9 - 2, VB.stone_slab, 1, VB.air, 0, false);
			this.fillWithRandomizedBlocks(world, box, 15 - 6, 0, 3, 15 - 6, 1, 9, false, rand, RandomSandstone);
			this.fillWithRandomizedBlocks(world, box, 15 - 6, 0, 2, 15 - 6, 0, 2, false, rand, RandomSandstone);
			this.fillWithRandomizedBlocks(world, box, 15 - 6, 2, 3, 15 - 6, 2, 3, false, rand, RandomSandstone);
			this.placeBlockAtCurrentPosition(world, VB.fence, 0, 15 - 6, 2, 4, box);
			this.fillWithRandomizedBlocks(world, box, 15 - 6, 2, 5, 15 - 6, 2, 5, false, rand, RandomSandstone);
			this.fillWithBlocks(world, box, 15 - 6, 2, 9 - 3, 15 - 6, 2, 9 - 2, VB.fence, VB.air, false);
			this.fillWithRandomizedBlocks(world, box, 15 - 6, 2, 9 - 1, 15 - 6, 2, 9, false, rand, RandomSandstone);
			this.fillWithRandomizedBlocks(world, box, 15 - 5, 0, 9, 15, 1, 9, false, rand, RandomSandstone); //Front Wall
			this.fillWithRandomizedBlocks(world, box, 15 - 5, 2, 9, 15 - 5, 2, 9, false, rand, RandomSandstone);
			this.fillWithRandomizedBlocks(world, box, 15 - 1, 2, 9, 15, 2, 9, false, rand, RandomSandstone);
			this.fillWithRandomizedBlocks(world, box, 15, 0, 1, 15, 0, 9 - 1, false, rand, RandomSandstone); //Right Wall
			this.fillWithRandomizedBlocks(world, box, 15, 1, 3, 15, 1, 3, false, rand, RandomSandstone);
			this.fillWithMetadataBlocks(world, box, 15, 1, 4, 15, 1, 5, VB.stone_slab, 1, VB.air, 0, false);
			this.fillWithRandomizedBlocks(world, box, 15, 1, 9 - 1, 15, 1, 9 - 3, false, rand, RandomSandstone);
			this.placeBlockAtCurrentPosition(world, VB.stone_slab, 1, 15, 1, 9 - 1, box);

			this.fillWithBlocks(world, box, 15 - 5, 0, 1, 15 - 1, 0, 9 - 1, VB.sandstone, VB.air, false); //Floor

			//Loot & Decorations
			//House 1
			int eastMeta = this.getDecoMeta(4);
			this.placeBlockAtCurrentPosition(world, MB.machine_boiler_off, 4, 1, 1, 1, box);
			this.fillWithBlocks(world, box, 1, 2, 1, 1, 3, 1, MB.deco_pipe_quad_rusted, VB.air, false);
			this.placeBlockAtCurrentPosition(world, MB.deco_pipe_rim_rusted, 0, 1, 5, 1, box);
			this.placeBlockAtCurrentPosition(world, MB.crate, 0, 2, 1, 3, box);
			this.placeBlockAtCurrentPosition(world, MB.crate_can, 0, 1, 1, 9 - 4, box);
			if(!hasPlacedLoot[0]) {
				this.placeBlockAtCurrentPosition(world, VB.chest, this.getMetadataWithOffset(VB.chest, 3), 1, 1, 9 - 2, box);
				ItemPool.generateChestContents(rand, ItemPool.getPool(ItemPoolsComponent.POOL_MACHINE_PARTS), L.getTileEntity(world, this.getXWithOffset(1, 9 - 2),
						this.getYWithOffset(1), this.getZWithOffset(1, 9 - 2)), 10);
				this.hasPlacedLoot[0] = true;
			}
			this.fillWithBlocks(world, box, 4, 1, 9 - 1, 5, 1, 9 - 1, MB.crate, VB.air, false);
			this.fillWithMetadataBlocks(world, box, 5, 1, 4, 5, 3, 4, MB.steel_scaffold, eastMeta < 4 ? 0 : 8, VB.air, 0, false);
			this.fillWithMetadataBlocks(world, box, 5, 1, 6, 5, 3, 6, MB.steel_scaffold, eastMeta < 4 ? 0 : 8, VB.air, 0, false);
			this.placeBlockAtCurrentPosition(world, MB.steel_grate, 7, 5, 1, 5, box);
			this.placeBlockAtCurrentPosition(world, MB.crate_weapon, 0, 5, 2, 5, box);

			//House 2
			if(!hasPlacedLoot[1]) {
				this.placeBlockAtCurrentPosition(world, VB.chest, this.getMetadataWithOffset(VB.chest, 3), 15 - 5, 1, 1, box);
				ItemPool.generateChestContents(rand, ItemPool.getPool(ItemPoolsLegacy.POOL_ANTENNA), L.getTileEntity(world, this.getXWithOffset(15 - 5, 1),
						this.getYWithOffset(1), this.getZWithOffset(15 - 5, 1)), 10);
				this.hasPlacedLoot[1] = true;
			}
			this.placeBlockAtCurrentPosition(world, MB.bobblehead, rand.nextInt(16), 15 - 5, 1, 4, box);
			com.hbm_m.blockentity.decorations.TrinketBlockEntity bobble = L.getTileEntity(world, this.getXWithOffset(15 - 5, 4), this.getYWithOffset(1), this.getZWithOffset(15 - 5, 4)) instanceof com.hbm_m.blockentity.decorations.TrinketBlockEntity t ? t : null;

			if(bobble != null) {
				bobble.setType(rand.nextInt(com.hbm_m.block.decorations.TrinketTypes.BobbleType.values().length - 1) + 1);
				bobble.setChanged();
			}

			this.randomlyFillWithBlocks(world, box, rand, 0.25F, 15 - 4, 1, 1, 15 - 1, 1, 9 - 1, VB.sand, VB.air, false);

			return true;
		}
	}

	public static class NTMLab1 extends Component {

		private static ConcreteBricks RandomConcreteBricks = new ConcreteBricks();
		private static LabTiles RandomLabTiles = new LabTiles();

		private boolean[] hasPlacedLoot = new boolean[2];

		public NTMLab1(CompoundTag nbt) {
			super(ComponentTypes.NTMLab1.get(), nbt);
			readLegacy(nbt);
		}

		/** Constructor for this feature; takes coordinates for bounding box */
		public NTMLab1(Random rand, int minX, int minZ) {
			super(ComponentTypes.NTMLab1.get(), rand, minX, 64, minZ, 9, 4, 7);
			this.hasPlacedLoot[0] = false;
			this.hasPlacedLoot[1] = false;
		}

		@Override
		protected void writeLegacy(CompoundTag nbt) {
			super.writeLegacy(nbt);
			nbt.putBoolean("hasLoot1", this.hasPlacedLoot[0]);
			nbt.putBoolean("hasLoot2", this.hasPlacedLoot[1]);
		}

		@Override
		protected void readLegacy(CompoundTag nbt) {
			super.readLegacy(nbt);
			this.hasPlacedLoot[0] = nbt.getBoolean("hasLoot1");
			this.hasPlacedLoot[1] = nbt.getBoolean("hasLoot2");
		}

		@Override
		public boolean addComponentParts(LevelAccessor world, Random rand, BoundingBox box) {

			//System.out.println(this.coordBaseMode);
			if(!this.setAverageHeight(world, box, this.boundingBox.minY())) {
				return false;
			}
			//System.out.println("" + this.boundingBox.minX() + ", " + this.boundingBox.minY() + ", " + this.boundingBox.minZ());

			placeFoundationUnderneath(world, VB.stonebrick, 0, 0, 0, 9, 7 - 2, -1, box);
			placeFoundationUnderneath(world, VB.stonebrick, 0, 3, 6, 9, 7, -1, box);

			if(this.getBlockAtCurrentPosition(world, 2, 0, 7 - 1, box).getMaterial().isReplaceable()
					|| this.getBlockAtCurrentPosition(world, 2, 0, 7 - 1, box) == VB.air) {
				placeFoundationUnderneath(world, VB.stonebrick, 0, 2, 7 - 1, 2, 7 - 1, -1, box);
				this.placeBlockAtCurrentPosition(world, VB.stone_brick_stairs, getStairMeta(0), 2, 0, 7 - 1, box);
			}

			this.fillWithAir(world, box, 1, 0, 1, 9 - 1, 4, 4);
			this.fillWithAir(world, box, 4, 0, 4, 9 - 1, 4, 7 - 1);
			this.fillWithAir(world, box, 3, 1, 7 - 1, 3, 2, 7 - 1);

			int pillarMeta = this.getPillarMeta(8);

			//Pillars
			this.fillWithBlocks(world, box, 0, 0, 0, 0, 3, 0, MB.concrete_pillar, VB.air, false);
			this.fillWithBlocks(world, box, 9, 0, 0, 9, 3, 0, MB.concrete_pillar, VB.air, false);
			this.fillWithMetadataBlocks(world, box, 0, 0, 1, 0, 0, 4, MB.concrete_pillar, pillarMeta, VB.air, 0, false);
			this.fillWithMetadataBlocks(world, box, 9, 0, 1, 9, 0, 7 - 1, MB.concrete_pillar, pillarMeta, VB.air, 0, false);
			this.fillWithBlocks(world, box, 0, 0, 7 - 2, 0, 3, 7 - 2, MB.concrete_pillar, VB.air, false);
			this.fillWithBlocks(world, box, 3, 0, 7 - 2, 3, 3, 7 - 2, MB.concrete_pillar, VB.air, false);
			this.fillWithBlocks(world, box, 3, 0, 7, 3, 3, 7, MB.concrete_pillar, VB.air, false);
			this.fillWithBlocks(world, box, 9, 0, 7, 9, 3, 7, MB.concrete_pillar, VB.air, false);

			//Walls
			this.fillWithRandomizedBlocks(world, box, 1, 0, 0, 9 - 1, 4 - 1, 0, false, rand, RandomConcreteBricks); //Back Wall
			this.fillWithRandomizedBlocks(world, box, 0, 4, 0, 9, 4, 0, false, rand, RandomConcreteBricks);
			this.fillWithRandomizedBlocks(world, box, 0, 1, 1, 0, 4 - 1, 4, false, rand, RandomConcreteBricks); //Left Wall
			this.fillWithRandomizedBlocks(world, box, 0, 4, 0, 0, 4, 7 - 2, false, rand, RandomConcreteBricks);
			this.fillWithRandomizedBlocks(world, box, 1, 0, 7 - 2, 2, 4, 7 - 2, false, rand, RandomConcreteBricks); //Front Wall Pt. 1
			this.placeBlockAtCurrentPosition(world, MB.brick_concrete_broken, 0, 3, 4, 7 - 2, box);
			this.fillWithRandomizedBlocks(world, box, 3, 4 - 1, 7 - 1, 3, 4, 7 - 1, false, rand, RandomConcreteBricks);
			this.fillWithRandomizedBlocks(world, box, 4, 0, 7, 9 - 1, 1, 7, false, rand, RandomConcreteBricks); //Front Wall Pt. 2
			this.fillWithRandomizedBlocks(world, box, 4, 2, 7, 4, 3, 7, false, rand, RandomConcreteBricks);
			this.fillWithRandomizedBlocks(world, box, 9 - 1, 2, 7, 9 - 1, 3, 7, false, rand, RandomConcreteBricks);
			this.randomlyFillWithBlocks(world, box, rand, 0.75F, 5, 2, 7, 9 - 2, 3, 7, VB.glass_pane, VB.air, false);
			this.fillWithRandomizedBlocks(world, box, 3, 4, 7, 9, 4, 7, false, rand, RandomConcreteBricks);
			this.fillWithRandomizedBlocks(world, box, 9, 1, 1, 9, 4, 7 - 1, false, rand, RandomConcreteBricks); //Right Wall

			//Floor & Ceiling
			this.fillWithRandomizedBlocks(world, box, 1, 0, 1, 9 - 1, 0, 4, false, rand, RandomLabTiles); //Floor
			this.fillWithRandomizedBlocks(world, box, 4, 0, 7 - 2, 9 - 1, 0, 7 - 1, false, rand, RandomLabTiles);
			this.placeBlockAtCurrentPosition(world, MB.tile_lab_cracked, 0, 3, 0, 7 - 1, box);

			this.fillWithBlocks(world, box, 1, 4 - 1, 1, 1, 4, 4, MB.reinforced_glass, VB.air, false); //Ceiling
			this.fillWithBlocks(world, box, 2, 4, 1, 9 - 1, 4, 4, MB.brick_light, VB.air, false);
			this.fillWithBlocks(world, box, 4, 4, 7 - 2, 9 - 1, 4, 7 - 1, MB.brick_light, VB.air, false);

			//Decorations & Loot
			this.fillWithMetadataBlocks(world, box, 1, 1, 1, 1, 1, 4, VB.dirt, 2, VB.air, 0, false);
			int westDecoMeta = this.getDecoMeta(5);
			this.fillWithMetadataBlocks(world, box, 2, 1, 1, 2, 1, 4, MB.steel_wall, westDecoMeta, VB.air, 0, false);
			this.fillWithMetadataBlocks(world, box, 2, 4 - 1, 1, 2, 4 - 1, 4, MB.steel_wall, westDecoMeta, VB.air, 0, false);
			for(byte i = 0; i < 4; i++) {
				this.placeBlockAtCurrentPosition(world, MB.plant_flower, i, 1, 2, 1 + i, box);
			}

			int doorMeta = this.getMetadataWithOffset(VB.wooden_door, 2);
			this.placeBlockAtCurrentPosition(world, MB.door_office, doorMeta, 3, 1, 7 - 1, box);
			L.placeDoorBlock(world, this.getXWithOffset(3, 7 - 1), this.getYWithOffset(1), this.getZWithOffset(3, 7 - 1), doorMeta, MB.door_office);

			int northDecoMeta = this.getDecoMeta(3);
			this.fillWithMetadataBlocks(world, box, 5, 4 - 1, 1, 9 - 1, 4 - 1, 1, MB.steel_scaffold, westDecoMeta < 4 ? 0 : 8, VB.air, 0, false);
			this.fillWithMetadataBlocks(world, box, 5, 4 - 1, 2, 9 - 1, 4 - 1, 2, MB.steel_wall, northDecoMeta, VB.air, 0, false);
			this.placeBlockAtCurrentPosition(world, MB.machine_electric_furnace_off, northDecoMeta, 5, 1, 1, box);
			this.placeBlockAtCurrentPosition(world, MB.machine_microwave, northDecoMeta, 5, 2, 1, box);
			this.placeBlockAtCurrentPosition(world, MB.deco_titanium, 0, 6, 1, 1, box);
			this.placeBlockAtCurrentPosition(world, MB.machine_shredder, 0, 9 - 2, 1, 1, box);
			this.placeBlockAtCurrentPosition(world, MB.deco_titanium, 0, 9 - 1, 1, 1, box);
			this.fillWithBlocks(world, box, 5, 1, 3, 9 - 1, 1, 3, MB.deco_titanium, VB.air, false);
			if(!hasPlacedLoot[0]) {
				this.placeBlockAtCurrentPosition(world, MB.deco_loot, 0, 6, 2, 3, box);
				LootGenerator.lootMedicine(world, this.getXWithOffset(6, 3), this.getYWithOffset(2), this.getZWithOffset(6, 3));
				this.hasPlacedLoot[0] = true;
			}

			this.placeBlockAtCurrentPosition(world, MB.crate_can, 0, 9 - 1, 1, 7 - 2, box);
			if(!hasPlacedLoot[1]) {
				this.hasPlacedLoot[1] = this.generateInvContents(world, box, rand, MB.crate_iron, 9 - 1, 1, 7 - 1, ItemPool.getPool(ItemPoolsLegacy.POOL_GENERIC), 8);
			}

			return true;
		}
	}

	public static class NTMLab2 extends Component {

		private static SuperConcrete RandomSuperConcrete = new SuperConcrete();
		private static ConcreteBricks RandomConcreteBricks = new ConcreteBricks();
		private static LabTiles RandomLabTiles = new LabTiles();

		private boolean[] hasPlacedLoot = new boolean[2];

		public NTMLab2(CompoundTag nbt) {
			super(ComponentTypes.NTMLab2.get(), nbt);
			readLegacy(nbt);
		}

		public NTMLab2(Random rand, int minX, int minZ) {
			super(ComponentTypes.NTMLab2.get(), rand, minX, 64, minZ, 12, 11, 8);
			this.hasPlacedLoot[0] = false;
			this.hasPlacedLoot[1] = false;
		}

		@Override
		protected void writeLegacy(CompoundTag nbt) {
			super.writeLegacy(nbt);
			nbt.putBoolean("hasLoot1", this.hasPlacedLoot[0]);
			nbt.putBoolean("hasLoot2", this.hasPlacedLoot[1]);
		}

		@Override
		protected void readLegacy(CompoundTag nbt) {
			super.readLegacy(nbt);
			this.hasPlacedLoot[0] = nbt.getBoolean("hasLoot1");
			this.hasPlacedLoot[1] = nbt.getBoolean("hasLoot2");
		}

		/** Port: Versatz nach der Hoehenmittelung beim Zusammenbau statt bei jedem Chunk-Bau. */
		@Override
		public void computeHeight(net.minecraft.world.level.levelgen.structure.Structure.GenerationContext ctx) {
			super.computeHeight(ctx);
			this.boundingBox.move(0, -7, 0);
		}

		@Override
		public boolean addComponentParts(LevelAccessor world, Random rand, BoundingBox box) {

			//System.out.println(this.coordBaseMode);
			if(!this.setAverageHeight(world, box, this.boundingBox.minY())) {
				return false;
			}
			//System.out.println("" + this.boundingBox.minX() + ", " + this.boundingBox.minY() + ", " + this.boundingBox.minZ());

			placeFoundationUnderneath(world, VB.stonebrick, 0, 0, 0, 12, 8 - 2, 6, box);
			placeFoundationUnderneath(world, VB.stonebrick, 0, 0, 7, 6, 8, 6, box);

			if(this.getBlockAtCurrentPosition(world, 12 - 3, 11 - 4, 7, box).getMaterial().isReplaceable()
					|| this.getBlockAtCurrentPosition(world, 12 - 3, 11 - 4, 7, box) == VB.air) {
				int stairMeta = this.getMetadataWithOffset(VB.stone_brick_stairs, 2);
				placeFoundationUnderneath(world, VB.stonebrick, 0, 12 - 3, 7, 12 - 2, 7, 11 - 4, box);
				this.fillWithMetadataBlocks(world, box, 12 - 3, 11 - 4, 7, 12 - 2, 11 - 4, 7, VB.stone_brick_stairs, stairMeta, VB.air, 0, false);
			}


			this.fillWithAir(world, box, 1, 11 - 4, 1, 12 - 1, 11, 8 - 3);
			this.fillWithAir(world, box, 1, 11 - 4, 8 - 2, 5, 11, 8 - 1);
			this.fillWithAir(world, box, 12 - 3, 11 - 3, 8 - 2, 12 - 2, 11 - 2, 8 - 2);
			this.fillWithAir(world, box, 5, 5, 1, 6, 6, 2);
			this.fillWithAir(world, box, 2, 0, 2, 12 - 2, 3, 8 - 2);

			//Walls
			this.fillWithRandomizedBlocks(world, box, 0, 11 - 4, 0, 12, 11, 0, false, rand, RandomSuperConcrete); //Back Wall
			this.fillWithRandomizedBlocks(world, box, 0, 11 - 4, 0, 0, 11, 8, false, rand, RandomSuperConcrete); //Left Wall
			this.fillWithRandomizedBlocks(world, box, 1, 11 - 4, 8, 5, 11 - 4, 8, false, rand, RandomSuperConcrete); //Front Wall pt. 1
			this.fillWithBlocks(world, box, 1, 11 - 3, 8, 1, 11 - 1, 8, MB.reinforced_glass, VB.air, false);
			this.fillWithRandomizedBlocks(world, box, 2, 11 - 4, 8, 2, 11 - 1, 8, false, rand, RandomSuperConcrete);
			this.fillWithBlocks(world, box, 3, 11 - 3, 8, 3, 11 - 1, 8, MB.reinforced_glass, VB.air, false);
			this.fillWithRandomizedBlocks(world, box, 4, 11 - 4, 8, 4, 11 - 1, 8, false, rand, RandomSuperConcrete);
			this.fillWithBlocks(world, box, 5, 11 - 3, 8, 5, 11 - 1, 8, MB.reinforced_glass, VB.air, false);
			this.fillWithRandomizedBlocks(world, box, 1, 11, 8, 5, 11, 8, false, rand, RandomSuperConcrete);
			this.fillWithRandomizedBlocks(world, box, 6, 11 - 4, 8 - 1, 6, 11, 8, false, rand, RandomSuperConcrete); //Front Wall pt. 2
			this.fillWithRandomizedBlocks(world, box, 6, 11 - 4, 8 - 2, 7, 11 - 2, 8 - 2, false, rand, RandomSuperConcrete); //Front Wall pt. 3
			this.fillWithBlocks(world, box, 6, 11 - 1, 8 - 2, 7, 11 - 1, 8 - 2, MB.concrete_super_broken, VB.air, false);
			this.fillWithRandomizedBlocks(world, box, 12 - 4, 11 - 4, 8 - 2, 12, 11 - 4, 8 - 2, false, rand, RandomSuperConcrete);
			this.fillWithRandomizedBlocks(world, box, 12 - 4, 11 - 3, 8 - 2, 12 - 4, 11, 8 - 2, false, rand, RandomSuperConcrete);
			this.fillWithRandomizedBlocks(world, box, 12 - 3, 11 - 1, 8 - 2, 12 - 2, 11, 8 - 2, false, rand, RandomSuperConcrete);
			this.fillWithRandomizedBlocks(world, box, 12 - 1, 11 - 4, 8 - 2, 12, 11, 8 - 2, false, rand, RandomSuperConcrete);
			this.fillWithRandomizedBlocks(world, box, 12, 11 - 4, 1, 12, 11 - 4, 8 - 3, false, rand, RandomSuperConcrete); //Right Wall
			this.fillWithBlocks(world, box, 12, 11 - 3, 8 - 3, 12, 11 - 1, 8 - 3, MB.reinforced_glass, VB.air, false);
			this.fillWithRandomizedBlocks(world, box, 12, 11 - 3, 4, 12, 11 - 1, 4, false, rand, RandomSuperConcrete);
			this.fillWithBlocks(world, box, 12, 11 - 3, 3, 12, 11 - 1, 3, MB.reinforced_glass, VB.air, false);
			this.fillWithRandomizedBlocks(world, box, 12, 11 - 3, 2, 12, 11 - 1, 2, false, rand, RandomSuperConcrete);
			this.fillWithBlocks(world, box, 12, 11 - 3, 1, 12, 11 - 1, 1, MB.reinforced_glass, VB.air, false);
			this.fillWithRandomizedBlocks(world, box, 12, 11, 1, 12, 11, 8 - 3, false, rand, RandomSuperConcrete);

			this.fillWithBlocks(world, box, 1, 0, 1, 12 - 1, 3, 1, MB.reinforced_stone, VB.air, false); //Back Wall
			this.fillWithBlocks(world, box, 1, 0, 2, 1, 3, 8 - 2, MB.reinforced_stone, VB.air, false); //Left Wall
			this.fillWithBlocks(world, box, 1, 0, 8 - 1, 12 - 1, 3, 8 - 1, MB.reinforced_stone, VB.air, false); //Front Wall
			this.fillWithBlocks(world, box, 12 - 1, 0, 2, 12 - 1, 3, 8 - 2, MB.reinforced_stone, VB.air, false); // Right Wall
			this.fillWithBlocks(world, box, 6, 0, 3, 6, 3, 8 - 2, MB.reinforced_stone, VB.air, false); //Internal Wall

			//Floors & Ceiling
			this.fillWithRandomizedBlocks(world, box, 1, 11 - 4, 1, 3, 11 - 4, 8 - 1, false, rand, RandomLabTiles); //Left Floor
			this.fillWithRandomizedBlocks(world, box, 4, 11 - 4, 8 - 2, 5, 11 - 4, 8 - 1, false, rand, RandomLabTiles);
			this.fillWithRandomizedBlocks(world, box, 12 - 4, 11 - 4, 1, 12 - 1, 11 - 4, 8 - 3, false, rand, RandomLabTiles); //Right Floor
			this.fillWithRandomizedBlocks(world, box, 12 - 3, 11 - 4, 8 - 2, 12 - 2, 11 - 4, 8 - 2, false, rand, RandomLabTiles);
			this.fillWithBlocks(world, box, 4, 11 - 4, 1, 7, 11 - 4, 1, MB.tile_lab_broken, VB.air, false); //Center Floor (Pain)
			this.placeBlockAtCurrentPosition(world, MB.tile_lab_broken, 0, 4, 11 - 4, 2, box);
			this.fillWithBlocks(world, box, 4, 11 - 4, 3, 4, 11 - 4, 5, MB.tile_lab_cracked, VB.air, false);
			this.placeBlockAtCurrentPosition(world, MB.tile_lab_broken, 0, 5, 11 - 4, 3, box);
			this.fillWithBlocks(world, box, 5, 11 - 4, 4, 5, 11 - 4, 5, MB.tile_lab_cracked, VB.air, false);
			this.placeBlockAtCurrentPosition(world, MB.tile_lab_broken, 0, 6, 11 - 4, 4, box);
			this.placeBlockAtCurrentPosition(world, MB.tile_lab_cracked, 0, 6, 11 - 4, 5, box);
			this.fillWithBlocks(world, box, 7, 11 - 4, 2, 7, 11 - 4, 3, MB.tile_lab_broken, VB.air, false);
			this.fillWithBlocks(world, box, 7, 11 - 4, 4, 7, 11 - 4, 5, MB.tile_lab_cracked, VB.air, false);

			this.fillWithBlocks(world, box, 1, 11, 1, 2, 11, 8 - 1, MB.brick_light, VB.air, false); //Left Ceiling
			this.fillWithBlocks(world, box, 3, 11, 8 - 2, 4, 11, 8 - 1, MB.brick_light, VB.air, false);
			this.fillWithBlocks(world, box, 12 - 3, 11, 1, 12 - 1, 11, 8 - 3, MB.brick_light, VB.air, false); //Right Ceiling
			this.fillWithBlocks(world, box, 3, 11, 1, 8, 11, 1, MB.waste_planks, VB.air, false); //Center Ceiling (Pain)
			this.fillWithBlocks(world, box, 3, 11, 2, 4, 11, 2, MB.waste_planks, VB.air, false);
			this.fillWithBlocks(world, box, 7, 11, 2, 8, 11, 2, MB.waste_planks, VB.air, false);
			this.fillWithBlocks(world, box, 3, 11, 3, 3, 11, 5, MB.waste_planks, VB.air, false);
			this.fillWithBlocks(world, box, 4, 11, 4, 4, 11, 5, MB.waste_planks, VB.air, false);
			this.fillWithBlocks(world, box, 5, 11, 6, 5, 11, 8 - 1, MB.waste_planks, VB.air, false);
			this.fillWithBlocks(world, box, 8, 11, 3, 8, 11, 5, MB.waste_planks, VB.air, false);

			this.fillWithRandomizedBlocks(world, box, 2, 0, 2, 5, 0, 8 - 2, false, rand, RandomLabTiles); //Floor
			this.fillWithRandomizedBlocks(world, box, 6, 0, 2, 6, 0, 3, false, rand, RandomLabTiles);
			this.fillWithRandomizedBlocks(world, box, 7, 0, 2, 12 - 2, 0, 8 - 2, false, rand, RandomLabTiles);

			this.fillWithRandomizedBlocks(world, box, 1, 4, 1, 12 - 1, 4, 8 - 1, false, rand, RandomConcreteBricks); //Ceiling

			//Decorations & Loot
			int eastMeta = this.getDecoMeta(4);
			int westMeta = this.getDecoMeta(5);
			int northMeta = this.getDecoMeta(3);
			int southMeta = this.getDecoMeta(2);
			this.placeBlockAtCurrentPosition(world, MB.crashed_balefire, southMeta, 6, 11 - 2, 3, box);

			int doorMeta = this.getMetadataWithOffset(VB.wooden_door, 1);
			this.placeBlockAtCurrentPosition(world, MB.door_office, doorMeta, 12 - 3, 11 - 3, 8 - 2, box);
			L.placeDoorBlock(world, this.getXWithOffset(12 - 3, 8 - 2), this.getYWithOffset(11 - 3), this.getZWithOffset(12 - 3, 8 - 2),
					doorMeta, MB.door_office);
			this.placeBlockAtCurrentPosition(world, MB.door_office, doorMeta, 12 - 2, 11 - 3, 8 - 2, box);
			L.placeDoorBlock(world, this.getXWithOffset(12 - 2, 8 - 2), this.getYWithOffset(11 - 3), this.getZWithOffset(12 - 2, 8 - 2),
					doorMeta, MB.door_office);

			this.fillWithBlocks(world, box, 1, 11 - 3, 1, 1, 11 - 1, 1, MB.deco_steel, VB.air, false);
			this.fillWithMetadataBlocks(world, box, 1, 11 - 3, 2, 1, 11 - 2, 3, MB.steel_grate, 7, VB.air, 0, false);
			this.placeBlockAtCurrentPosition(world, MB.tape_recorder, westMeta, 1, 11 - 1, 2, box);
			this.placeBlockAtCurrentPosition(world, MB.steel_beam, 0, 1, 11 - 1, 3, box);
			this.fillWithBlocks(world, box, 1, 11 - 3, 6, 1, 11 - 1, 6, MB.deco_pipe_framed_rusted, VB.air, false);

			this.fillWithMetadataBlocks(world, box, 12 - 4, 11 - 3, 1, 12 - 4, 11 - 1, 1, MB.steel_wall, eastMeta, VB.air, 0, false);
			this.fillWithMetadataBlocks(world, box, 12 - 3, 11 - 1, 1, 12 - 2, 11 - 1, 1, MB.steel_grate, 0, VB.air, 0, false);
			this.fillWithMetadataBlocks(world, box, 12 - 3, 11 - 2, 1, 12 - 2, 11 - 2, 1, MB.tape_recorder, northMeta, VB.air, 0, false);
			this.fillWithBlocks(world, box, 12 - 3, 11 - 3, 1, 12 - 2, 11 - 3, 1, MB.deco_steel, VB.air, false);
			this.fillWithMetadataBlocks(world, box, 12 - 1, 11 - 3, 1, 12 - 1, 11 - 1, 1, MB.steel_wall, westMeta, VB.air, 0, false);

			this.fillWithMetadataBlocks(world, box, 2, 1, 2, 2, 1, 8 - 2, MB.steel_grate, 7, VB.air, 0, false);
			this.placeBlockAtCurrentPosition(world, MB.vitrified_barrel, 0, 2, 2, 2, box);
			this.fillWithMetadataBlocks(world, box, 3, 1, 2, 3, 3, 2, MB.steel_wall, westMeta, VB.air, 0, false);
			this.fillWithMetadataBlocks(world, box, 3, 1, 4, 3, 3, 4, MB.steel_wall, westMeta, VB.air, 0, false);
			this.fillWithMetadataBlocks(world, box, 3, 1, 8 - 2, 3, 3, 8 - 2, MB.steel_wall, westMeta, VB.air, 0, false);
			this.placeBlockAtCurrentPosition(world, MB.crate, 0, 4, 1, 8 - 2, box);
			this.placeBlockAtCurrentPosition(world, MB.crate_lead, 0, 4, 2, 8 - 2, box);
			if(!hasPlacedLoot[0]) {
				this.hasPlacedLoot[0] = this.generateInvContents(world, box, rand, MB.crate_iron, 5, 1, 8 - 2, ItemPool.getPool(ItemPoolsComponent.POOL_NUKE_FUEL), 10);
			}
			this.fillWithBlocks(world, box, 4, 1, 8 - 3, 5, 1, 8 - 3, MB.crate_lead, VB.air, false);

			this.fillWithBlocks(world, box, 12 - 5, 1, 8 - 2, 12 - 5, 3, 8 - 2, MB.deco_steel, VB.air, false);;
			this.fillWithMetadataBlocks(world, box, 12 - 4, 1, 8 - 2, 12 - 2, 1, 8 - 2, MB.steel_grate, 7, VB.air, 0, false);
			this.fillWithMetadataBlocks(world, box, 12 - 4, 2, 8 - 2, 12 - 3, 2, 8 - 2, MB.tape_recorder, southMeta, VB.air, 0, false);
			this.placeBlockAtCurrentPosition(world, MB.steel_beam, 0, 12 - 2, 2, 8 - 2, box);
			this.fillWithBlocks(world, box, 12 - 4, 3, 8 - 2, 12 - 2, 3, 8 - 2, MB.steel_roof, VB.air, false);
			if(!hasPlacedLoot[1]) {
				this.hasPlacedLoot[1] = this.generateInvContents(world, box, rand, MB.crate_iron, 12 - 2, 1, 3, ItemPool.getPool(ItemPoolsLegacy.POOL_NUKE_TRASH), 9);
				if(rand.nextInt(2) == 0)
					generateLoreBook(world, box, 12 - 2, 1, 3, 1, com.hbm_m.item.special.ItemBookLore.generateOfficeBook(net.minecraft.util.RandomSource.create(rand.nextLong())));
			}

			return true;
		}
	}

	public static class RuralHouse1 extends Component {

		public RuralHouse1(CompoundTag nbt) {
			super(ComponentTypes.RuralHouse1.get(), nbt);
			readLegacy(nbt);
		}

		public RuralHouse1(Random rand, int minX, int minZ) {
			super(ComponentTypes.RuralHouse1.get(), rand, minX, 64, minZ, 14, 8, 14);
		}

		@Override
		public boolean addComponentParts(LevelAccessor world, Random rand, BoundingBox box) {

			if(!this.setAverageHeight(world, box, this.boundingBox.minY())) {
				return false;
			}

			//FillWithAir
			fillWithAir(world, box, 9, 1, 3, 12, 4, 8);
			fillWithAir(world, box, 5, 1, 2, 8, 3, 8);
			fillWithAir(world, box, 2, 1, 5, 4, 3, 8);
			fillWithAir(world, box, 2, 1, 10, 7, 3, 12);

			//Foundations
			fillWithBlocks(world, box, 1, 0, 4, 4, 0, 4, MB.concrete_colored_ext);
			fillWithBlocks(world, box, 4, 0, 2, 4, 0, 3, MB.concrete_colored_ext);
			fillWithBlocks(world, box, 4, 0, 1, 9, 0, 1, MB.concrete_colored_ext);
			fillWithBlocks(world, box, 9, 0, 2, 10, 0, 2, MB.concrete_colored_ext);
			placeBlockAtCurrentPosition(world, MB.concrete_colored_ext, 0, 12, 0, 2, box);
			fillWithBlocks(world, box, 13, 0, 2, 13, 0, 9, MB.concrete_colored_ext);
			fillWithBlocks(world, box, 5, 0, 9, 12, 0, 9, MB.concrete_colored_ext);
			fillWithBlocks(world, box, 2, 0, 9, 3, 0, 9, MB.concrete_colored_ext);
			placeBlockAtCurrentPosition(world, MB.concrete_colored_ext, 0, 8, 0, 10, box);
			fillWithBlocks(world, box, 8, 0, 12, 8, 0, 13, MB.concrete_colored_ext);
			fillWithBlocks(world, box, 1, 0, 13, 7, 0, 13, MB.concrete_colored_ext);
			fillWithBlocks(world, box, 1, 0, 5, 1, 0, 12, MB.concrete_colored_ext);
			placeFoundationUnderneath(world, MB.concrete_colored_ext, 0, 1, 10, 8, 13, -1, box);
			placeFoundationUnderneath(world, MB.concrete_colored_ext, 0, 1, 4, 3, 9, -1, box);
			placeFoundationUnderneath(world, MB.concrete_colored_ext, 0, 4, 1, 13, 9, -1, box);

			placeFoundationUnderneath(world, VB.log, 0, 2, 3, 2, 3, 0, box);
			placeFoundationUnderneath(world, VB.log, 0, 3, 2, 3, 2, 0, box);
			placeFoundationUnderneath(world, VB.log, 0, 3, 0, 3, 0, -1, box);
			placeFoundationUnderneath(world, VB.log, 0, 5, 0, 5, 0, 0, box);
			placeFoundationUnderneath(world, VB.log, 0, 8, 0, 8, 0, 0, box);
			placeFoundationUnderneath(world, VB.log, 0, 10, 0, 10, 0, -1, box);
			placeFoundationUnderneath(world, VB.log, 0, 14, 1, 14, 1, -1, box);
			placeFoundationUnderneath(world, VB.log, 0, 14, 3, 14, 3, -1, box);
			placeFoundationUnderneath(world, VB.log, 0, 14, 5, 14, 6, 0, box);
			placeFoundationUnderneath(world, VB.log, 0, 14, 8, 14, 8, -1, box);
			placeFoundationUnderneath(world, VB.log, 0, 14, 10, 14, 10, -1, box);
			placeFoundationUnderneath(world, VB.log, 0, 9, 14, 9, 14, -1, box);
			placeFoundationUnderneath(world, VB.log, 0, 7, 14, 7, 14, -1, box);
			placeFoundationUnderneath(world, VB.log, 0, 4, 14, 5, 14, 0, box);
			placeFoundationUnderneath(world, VB.log, 0, 2, 14, 2, 14, -1, box);
			placeFoundationUnderneath(world, VB.log, 0, 0, 14, 0, 14, -1, box);
			placeFoundationUnderneath(world, VB.log, 0, 0, 13, 0, 13, 0, box);
			placeFoundationUnderneath(world, VB.log, 0, 0, 11, 0, 11, 0, box);
			placeFoundationUnderneath(world, VB.log, 0, 0, 9, 0, 9, -1, box);
			placeFoundationUnderneath(world, VB.log, 0, 0, 6, 0, 7, 0, box);
			placeFoundationUnderneath(world, VB.log, 0, 0, 4, 0, 4, 0, box);
			placeFoundationUnderneath(world, VB.log, 0, 0, 3, 0, 4, -1, box);

			//Walls
			//North/Front
			fillWithBlocks(world, box, 1, 1, 4, 4, 4, 4, VB.brick_block);
			fillWithBlocks(world, box, 2, 5, 4, 7, 5, 4, VB.brick_block);
			placeBlockAtCurrentPosition(world, VB.brick_block, 0, 3, 6, 4, box);
			placeBlockAtCurrentPosition(world, VB.brick_block, 0, 6, 6, 4, box);
			fillWithBlocks(world, box, 4, 7, 4, 5, 7, 4, VB.brick_block);
			fillWithBlocks(world, box, 4, 1, 1, 4, 4, 3, VB.brick_block);
			fillWithBlocks(world, box, 5, 1, 1, 8, 1, 1, VB.brick_block);
			fillWithBlocks(world, box, 5, 4, 1, 8, 4, 1, VB.brick_block);
			fillWithBlocks(world, box, 9, 1, 1, 9, 4, 2, VB.brick_block);
			fillWithBlocks(world, box, 10, 1, 2, 10, 3, 2, VB.brick_block);
			fillWithBlocks(world, box, 12, 1, 2, 13, 3, 2, VB.brick_block);
			fillWithBlocks(world, box, 10, 4, 2, 13, 4, 2, VB.brick_block);
			fillWithBlocks(world, box, 9, 5, 2, 12, 5, 2, VB.brick_block);
			fillWithBlocks(world, box, 10, 6, 2, 11, 6, 2, VB.brick_block);
			//East/Left
			fillWithBlocks(world, box, 13, 1, 3, 13, 1, 8, VB.brick_block);
			fillWithBlocks(world, box, 13, 3, 3, 13, 4, 8, VB.brick_block);
			//South/Back
			fillWithBlocks(world, box, 13, 1, 9, 13, 4, 9, VB.brick_block);
			fillWithBlocks(world, box, 9, 1, 9, 12, 1, 9, VB.brick_block);
			fillWithBlocks(world, box, 9, 4, 9, 12, 5, 9, VB.brick_block);
			fillWithBlocks(world, box, 10, 6, 9, 11, 6, 9, VB.brick_block);
			fillWithBlocks(world, box, 8, 1, 9, 8, 4, 10, VB.brick_block);
			fillWithBlocks(world, box, 8, 1, 12, 8, 3, 13, VB.brick_block);
			fillWithBlocks(world, box, 8, 4, 11, 8, 4, 13, VB.brick_block);
			fillWithBlocks(world, box, 7, 1, 13, 7, 3, 13, VB.brick_block);
			fillWithBlocks(world, box, 3, 1, 13, 6, 1, 13, VB.brick_block);
			fillWithBlocks(world, box, 2, 4, 13, 7, 5, 13, VB.brick_block);
			placeBlockAtCurrentPosition(world, VB.brick_block, 0, 6, 6, 13, box);
			placeBlockAtCurrentPosition(world, VB.brick_block, 0, 3, 6, 13, box);
			fillWithBlocks(world, box, 4, 7, 13, 5, 7, 13, VB.brick_block);
			fillWithBlocks(world, box, 2, 1, 13, 2, 3, 13, VB.brick_block);
			//West/Right
			fillWithBlocks(world, box, 1, 1, 13, 1, 4, 13, VB.brick_block);
			fillWithBlocks(world, box, 1, 1, 5, 1, 1, 12, VB.brick_block);
			placeBlockAtCurrentPosition(world, VB.brick_block, 0, 1, 2, 9, box);
			fillWithBlocks(world, box, 1, 3, 5, 1, 3, 12, VB.brick_block);
			//Inside
			fillWithBlocks(world, box, 2, 1, 9, 3, 3, 9, VB.brick_block);
			fillWithBlocks(world, box, 5, 1, 9, 7, 3, 9, VB.brick_block);
			//Wood Paneling
			fillWithMetadataBlocks(world, box, 5, 2, 1, 5, 3, 1, VB.planks, 1);
			fillWithMetadataBlocks(world, box, 8, 2, 1, 8, 3, 1, VB.planks, 1);
			placeBlockAtCurrentPosition(world, VB.planks, 1, 11, 3, 2, box);
			fillWithMetadataBlocks(world, box, 13, 2, 3, 13, 2, 4, VB.planks, 1);
			fillWithMetadataBlocks(world, box, 13, 2, 7, 13, 2, 8, VB.planks, 1);
			fillWithMetadataBlocks(world, box, 12, 2, 9, 12, 3, 9, VB.planks, 1);
			fillWithMetadataBlocks(world, box, 9, 2, 9, 9, 3, 9, VB.planks, 1);
			placeBlockAtCurrentPosition(world, VB.planks, 1, 8, 3, 11, box);
			fillWithMetadataBlocks(world, box, 6, 2, 13, 6, 3, 13, VB.planks, 1);
			fillWithMetadataBlocks(world, box, 3, 2, 13, 3, 3, 13, VB.planks, 1);
			placeBlockAtCurrentPosition(world, VB.planks, 1, 1, 2, 12, box);
			placeBlockAtCurrentPosition(world, VB.planks, 1, 1, 2, 10, box);
			placeBlockAtCurrentPosition(world, VB.planks, 1, 1, 2, 8, box);
			placeBlockAtCurrentPosition(world, VB.planks, 1, 1, 2, 5, box);
			placeBlockAtCurrentPosition(world, VB.planks, 1, 4, 3, 9, box);
			//Wood Framing
			//North/Front
			int logW = this.getPillarMeta(4);
			int logN = this.getPillarMeta(8);

			fillWithBlocks(world, box, 0, 0, 3, 0, 3, 3, VB.log);
			fillWithMetadataBlocks(world, box, 1, 4, 3, 3, 4, 3, VB.log, logW);
			fillWithMetadataBlocks(world, box, 3, 4, 1, 3, 4, 2, VB.log, logN);
			placeBlockAtCurrentPosition(world, VB.wooden_slab, 9, 1, 3, 3, box);
			placeBlockAtCurrentPosition(world, VB.wooden_slab, 9, 3, 3, 1, box);
			fillWithMetadataBlocks(world, box, 1, 1, 3, 2, 1, 3, VB.wooden_slab, 1);
			fillWithMetadataBlocks(world, box, 3, 1, 1, 3, 1, 3, VB.wooden_slab, 1);
			fillWithBlocks(world, box, 3, 0, 0, 3, 3, 0, VB.log);
			fillWithMetadataBlocks(world, box, 4, 1, 0, 9, 1, 0, VB.wooden_slab, 1);
			placeBlockAtCurrentPosition(world, VB.wooden_slab, 9, 4, 3, 0, box);
			placeBlockAtCurrentPosition(world, VB.wooden_slab, 9, 9, 3, 0, box);
			fillWithBlocks(world, box, 10, 0, 0, 10, 3, 0, VB.log);
			fillWithMetadataBlocks(world, box, 10, 4, 1, 13, 4, 1, VB.log, logW);
			fillWithBlocks(world, box, 14, 0, 1, 14, 3, 1, VB.log);
			//East/Left
			fillWithBlocks(world, box, 14, 0, 3, 14, 3, 3, VB.log);
			fillWithBlocks(world, box, 14, 0, 8, 14, 3, 8, VB.log);
			fillWithBlocks(world, box, 14, 0, 10, 14, 3, 10, VB.log);
			placeBlockAtCurrentPosition(world, VB.wooden_slab, 1, 14, 1, 2, box);
			fillWithMetadataBlocks(world, box, 14, 1, 4, 14, 1, 7, VB.wooden_slab, 1);
			placeBlockAtCurrentPosition(world, VB.wooden_slab, 1, 14, 1, 9, box);
			placeBlockAtCurrentPosition(world, VB.wooden_slab, 9, 14, 3, 2, box);
			placeBlockAtCurrentPosition(world, VB.wooden_slab, 9, 14, 3, 4, box);
			placeBlockAtCurrentPosition(world, VB.wooden_slab, 9, 14, 3, 7, box);
			placeBlockAtCurrentPosition(world, VB.wooden_slab, 9, 14, 3, 9, box);
			//South/Back
			fillWithMetadataBlocks(world, box, 9, 4, 10, 13, 4, 10, VB.log, logW);
			placeBlockAtCurrentPosition(world, VB.wooden_slab, 9, 13, 3, 10, box);
			fillWithBlocks(world, box, 9, 0, 14, 9, 3, 14, VB.log);
			fillWithBlocks(world, box, 7, 0, 14, 7, 3, 14, VB.log);
			fillWithBlocks(world, box, 2, 0, 14, 2, 3, 14, VB.log);
			fillWithBlocks(world, box, 0, 0, 14, 0, 3, 14, VB.log);
			fillWithMetadataBlocks(world, box, 1, 4, 14, 8, 4, 14, VB.log, logW);
			placeBlockAtCurrentPosition(world, VB.wooden_slab, 1, 8, 1, 14, box);
			fillWithMetadataBlocks(world, box, 3, 1, 14, 6, 1, 14, VB.wooden_slab, 1);
			placeBlockAtCurrentPosition(world, VB.wooden_slab, 1, 1, 1, 14, box);
			placeBlockAtCurrentPosition(world, VB.wooden_slab, 9, 8, 3, 14, box);
			placeBlockAtCurrentPosition(world, VB.wooden_slab, 9, 1, 3, 14, box);
			//West/Right
			fillWithBlocks(world, box, 0, 0, 9, 0, 3, 9, VB.log);
			fillWithMetadataBlocks(world, box, 0, 1, 10, 0, 1, 13, VB.wooden_slab, 1);
			fillWithMetadataBlocks(world, box, 0, 1, 4, 0, 1, 8, VB.wooden_slab, 1);
			placeBlockAtCurrentPosition(world, VB.wooden_slab, 9, 0, 3, 13, box);
			placeBlockAtCurrentPosition(world, VB.wooden_slab, 9, 0, 3, 10, box);
			placeBlockAtCurrentPosition(world, VB.wooden_slab, 9, 0, 3, 8, box);
			placeBlockAtCurrentPosition(world, VB.wooden_slab, 9, 0, 3, 4, box);

			int stairW = this.getStairMeta(0);
			int stairE = this.getStairMeta(1);
			int stairN = this.getStairMeta(2);
			int stairS = this.getStairMeta(3);

			//Floor
			placeBlockAtCurrentPosition(world, VB.planks, 1, 11, 0, 2, box);
			fillWithMetadataBlocks(world, box, 9, 0, 3, 12, 0, 8, VB.planks, 1);
			fillWithMetadataBlocks(world, box, 5, 0, 2, 8, 0, 8, VB.planks, 1);
			fillWithMetadataBlocks(world, box, 2, 0, 5, 4, 0, 8, VB.planks, 1);
			placeBlockAtCurrentPosition(world, VB.planks, 1, 4, 0, 9, box);
			fillWithMetadataBlocks(world, box, 2, 0, 10, 7, 0, 12, VB.planks, 1);
			placeBlockAtCurrentPosition(world, VB.planks, 1, 8, 0, 11, box);
			fillWithBlocks(world, box, 13, 1, 0, 14, 1, 0, VB.fence);
			//Porches
			fillWithBlocks(world, box, 10, 0, 1, 13, 0, 1, VB.planks);
			fillWithMetadataBlocks(world, box, 11, 0, 0, 12, 0, 0, VB.spruce_stairs, stairN);
			fillWithMetadataBlocks(world, box, 13, 0, 0, 14, 0, 0, VB.planks, 1);
			fillWithBlocks(world, box, 12, 0, 10, 13, 0, 10, VB.planks);
			fillWithBlocks(world, box, 9, 0, 10, 11, 0, 11, VB.planks);
			fillWithBlocks(world, box, 9, 0, 12, 10, 0, 12, VB.planks);
			placeBlockAtCurrentPosition(world, VB.planks, 0, 9, 0, 13, box);
			for(int i = 0; i < 3; i++) {
				fillWithMetadataBlocks(world, box, 10 + i, 0, 13 - i, 11 + i, 0, 13 - i, VB.planks, 1);
				fillWithBlocks(world, box, 10 + i, 1, 13 - i, 11 + i, 1, 13 - i, VB.fence);
			}

			//Ceiling
			fillWithMetadataBlocks(world, box, 12, 4, 3, 12, 4, 8, VB.oak_stairs, stairW | 4);
			fillWithBlocks(world, box, 12, 5, 3, 12, 5, 8, VB.planks);
			fillWithBlocks(world, box, 10, 5, 3, 11, 6, 8, VB.planks);
			fillWithBlocks(world, box, 9, 5, 3, 9, 5, 8, VB.planks);
			fillWithMetadataBlocks(world, box, 9, 4, 3, 9, 4, 8, VB.oak_stairs, stairE | 4);
			fillWithBlocks(world, box, 8, 4, 5, 8, 4, 8, VB.planks);
			fillWithBlocks(world, box, 5, 4, 2, 8, 4, 4, VB.planks);
			fillWithBlocks(world, box, 1, 4, 5, 7, 4, 12, VB.planks);

			//Roofing
			//Framing
			placeBlockAtCurrentPosition(world, VB.spruce_stairs, stairW, 1, 5, 3, box);
			placeBlockAtCurrentPosition(world, VB.spruce_stairs, stairW, 2, 6, 3, box);
			placeBlockAtCurrentPosition(world, VB.spruce_stairs, stairE | 4, 3, 6, 3, box);
			placeBlockAtCurrentPosition(world, VB.spruce_stairs, stairW, 3, 7, 3, box);
			placeBlockAtCurrentPosition(world, VB.spruce_stairs, stairE | 4, 4, 7, 3, box);
			fillWithMetadataBlocks(world, box, 4, 8, 3, 5, 8, 3, VB.wooden_slab, 1);
			placeBlockAtCurrentPosition(world, VB.spruce_stairs, stairW | 4, 5, 7, 3, box);
			placeBlockAtCurrentPosition(world, VB.spruce_stairs, stairE, 6, 7, 3, box);
			placeBlockAtCurrentPosition(world, VB.spruce_stairs, stairW | 4, 6, 6, 3, box);
			placeBlockAtCurrentPosition(world, VB.spruce_stairs, stairE, 7, 6, 3, box);
			fillWithMetadataBlocks(world, box, 2, 5, 3, 3, 5, 3, VB.planks, 1);
			placeBlockAtCurrentPosition(world, VB.planks, 1, 3, 5, 2, box);
			placeBlockAtCurrentPosition(world, VB.wooden_slab, 1, 3, 5, 1, box);
			fillWithMetadataBlocks(world, box, 3, 4, 0, 14, 4, 0, VB.spruce_stairs, stairN);
			placeBlockAtCurrentPosition(world, VB.spruce_stairs, stairW, 8, 5, 1, box);
			placeBlockAtCurrentPosition(world, VB.planks, 1, 9, 5, 1, box);
			placeBlockAtCurrentPosition(world, VB.wooden_slab, 1, 10, 5, 1, box);
			placeBlockAtCurrentPosition(world, VB.spruce_stairs, stairW, 9, 6, 1, box);
			placeBlockAtCurrentPosition(world, VB.spruce_stairs, stairE | 4, 10, 6, 1, box);
			fillWithMetadataBlocks(world, box, 10, 7, 1, 11, 7, 1, VB.wooden_slab, 1);
			placeBlockAtCurrentPosition(world, VB.spruce_stairs, stairW | 4, 11, 6, 1, box);
			placeBlockAtCurrentPosition(world, VB.spruce_stairs, stairE, 12, 6, 1, box);
			placeBlockAtCurrentPosition(world, VB.spruce_stairs, stairW | 4, 12, 5, 1, box);
			placeBlockAtCurrentPosition(world, VB.spruce_stairs, stairE, 13, 5, 1, box);
			fillWithMetadataBlocks(world, box, 14, 4, 1, 14, 4, 10, VB.spruce_stairs, stairE);
			placeBlockAtCurrentPosition(world, VB.spruce_stairs, stairE, 13, 5, 10, box);
			placeBlockAtCurrentPosition(world, VB.spruce_stairs, stairW | 4, 12, 5, 10, box);
			placeBlockAtCurrentPosition(world, VB.spruce_stairs, stairE, 12, 6, 10, box);
			placeBlockAtCurrentPosition(world, VB.spruce_stairs, stairW | 4, 11, 6, 10, box);
			fillWithMetadataBlocks(world, box, 10, 7, 10, 11, 7, 10, VB.wooden_slab, 1);
			placeBlockAtCurrentPosition(world, VB.spruce_stairs, stairE | 4, 10, 6, 10, box);
			placeBlockAtCurrentPosition(world, VB.spruce_stairs, stairW, 9, 6, 10, box);
			placeBlockAtCurrentPosition(world, VB.spruce_stairs, stairE | 4, 9, 5, 10, box);
			fillWithMetadataBlocks(world, box, 9, 4, 11, 9, 4, 14, VB.spruce_stairs, stairE);
			placeBlockAtCurrentPosition(world, VB.spruce_stairs, stairE, 8, 5, 14, box);
			placeBlockAtCurrentPosition(world, VB.spruce_stairs, stairW | 4, 7, 5, 14, box);
			placeBlockAtCurrentPosition(world, VB.spruce_stairs, stairE, 7, 6, 14, box);
			placeBlockAtCurrentPosition(world, VB.spruce_stairs, stairW | 4, 6, 6, 14, box);
			placeBlockAtCurrentPosition(world, VB.spruce_stairs, stairE, 6, 7, 14, box);
			placeBlockAtCurrentPosition(world, VB.spruce_stairs, stairW | 4, 5, 7, 14, box);
			fillWithMetadataBlocks(world, box, 4, 8, 14, 5, 8, 14, VB.wooden_slab, 1);
			placeBlockAtCurrentPosition(world, VB.spruce_stairs, stairE | 4, 4, 7, 14, box);
			placeBlockAtCurrentPosition(world, VB.spruce_stairs, stairW, 3, 7, 14, box);
			placeBlockAtCurrentPosition(world, VB.spruce_stairs, stairE | 4, 3, 6, 14, box);
			placeBlockAtCurrentPosition(world, VB.spruce_stairs, stairW, 2, 6, 14, box);
			placeBlockAtCurrentPosition(world, VB.spruce_stairs, stairE | 4, 2, 5, 14, box);
			placeBlockAtCurrentPosition(world, VB.spruce_stairs, stairW, 1, 5, 14, box);
			fillWithMetadataBlocks(world, box, 0, 4, 3, 0, 4, 14, VB.spruce_stairs, stairW);
			//Beams
			for(int z = 6; z <= 11; z += 5) {
				for(int i = 0; i < 3; i++) {
					placeBlockAtCurrentPosition(world, VB.spruce_stairs, stairE | 4, 2 + i, 5 + i, z, box);
					placeBlockAtCurrentPosition(world, VB.spruce_stairs, stairW | 4, 7 - i, 5 + i, z, box);
				}
			}

			//Main (LEFT)
			BrokenStairs roofStairs = new BrokenStairs();
			BrokenBlocks roofBlocks = new BrokenBlocks();

			roofStairs.setMetadata(stairW);
			fillWithBlocks(world, box, 4, 5, 1, 7, 5, 1, VB.wooden_slab);
			fillWithRandomizedBlocks(world, box, 4, 5, 2, 7, 5, 3, rand, roofBlocks); //TODO separate into stair/slab/block block selectors
			fillWithRandomizedBlocks(world, box, 8, 5, 2, 8, 5, 10, rand, roofBlocks);
			fillWithRandomizedBlocks(world, box, 9, 6, 2, 9, 6, 9, rand, roofStairs);
			randomlyFillWithBlocks(world, box, rand, 0.8F, 10, 7, 2, 11, 7, 9, VB.wooden_slab);
			roofStairs.setMetadata(stairE);
			fillWithRandomizedBlocks(world, box, 12, 6, 2, 12, 6, 9, rand, roofStairs); //i should redo like most of this shit
			fillWithRandomizedBlocks(world, box, 13, 5, 2, 13, 5, 9, rand, roofStairs);
			//Main (RIGHT)
			fillWithRandomizedBlocks(world, box, 8, 5, 11, 8, 5, 13, rand, roofStairs);
			fillWithRandomizedBlocks(world, box, 7, 6, 4, 7, 6, 13, rand, roofStairs);
			fillWithRandomizedBlocks(world, box, 6, 7, 4, 6, 7, 7, rand, roofStairs);
			fillWithRandomizedBlocks(world, box, 6, 7, 11, 6, 7, 13, rand, roofStairs);
			roofStairs.setMetadata(stairW);
			fillWithBlocks(world, box, 4, 8, 4, 5, 8, 5, VB.wooden_slab);
			placeBlockAtCurrentPosition(world, VB.wooden_slab, 0, 5, 8, 6, box);
			placeBlockAtCurrentPosition(world, VB.wooden_slab, 0, 4, 8, 11, box);
			fillWithBlocks(world, box, 4, 8, 12, 5, 8, 13, VB.wooden_slab);
			fillWithRandomizedBlocks(world, box, 3, 7, 4, 3, 7, 6, rand, roofStairs);
			fillWithRandomizedBlocks(world, box, 3, 7, 10, 3, 7, 13, rand, roofStairs);
			fillWithRandomizedBlocks(world, box, 2, 6, 4, 2, 6, 13, rand, roofStairs);
			fillWithRandomizedBlocks(world, box, 1, 5, 4, 1, 5, 13, rand, roofStairs);

			//Deco
			int metaN = getDecoMeta(3);
			int metaE = getDecoMeta(4);

			//Webs
			randomlyFillWithBlocks(world, box, rand, 0.05F, 12, 3, 3, 12, 3, 8, VB.web);
			randomlyFillWithBlocks(world, box, rand, 0.05F, 10, 4, 3, 11, 4, 8, VB.web);
			randomlyFillWithBlocks(world, box, rand, 0.05F, 5, 3, 2, 8, 3, 2, VB.web);
			randomlyFillWithBlocks(world, box, rand, 0.05F, 5, 3, 3, 9, 3, 8, VB.web);
			randomlyFillWithBlocks(world, box, rand, 0.05F, 2, 3, 5, 4, 3, 8, VB.web);
			randomlyFillWithBlocks(world, box, rand, 0.05F, 2, 3, 10, 7, 3, 12, VB.web);
			//Doors
			placeDoor(world, box, VB.wooden_door, 1, false, false, 11, 1, 2);
			placeDoor(world, box, VB.wooden_door, 1, false, rand.nextBoolean(), 4, 1, 9);
			placeDoor(world, box, VB.wooden_door, 2, false, rand.nextBoolean(), 8, 1, 11);
			//Windows
			randomlyFillWithBlocks(world, box, rand, 0.5F, 6, 2, 1, 7, 3, 1, VB.glass_pane);
			randomlyFillWithBlocks(world, box, rand, 0.5F, 13, 2, 5, 13, 2, 6, VB.glass_pane);
			randomlyFillWithBlocks(world, box, rand, 0.5F, 10, 2, 9, 11, 3, 9, VB.glass_pane);
			randomlyFillWithBlocks(world, box, rand, 0.5F, 4, 2, 13, 5, 3, 13, VB.glass_pane);
			randomlyFillWithBlocks(world, box, rand, 0.5F, 1, 2, 11, 1, 2, 11, VB.glass_pane);
			randomlyFillWithBlocks(world, box, rand, 0.5F, 1, 2, 6, 1, 2, 7, VB.glass_pane);
			randomlyFillWithBlocks(world, box, rand, 0.5F, 4, 6, 4, 5, 6, 4, VB.glass_pane);
			randomlyFillWithBlocks(world, box, rand, 0.5F, 4, 6, 13, 5, 6, 13, VB.glass_pane);
			//Attic Access
			placeBlockAtCurrentPosition(world, VB.trapdoor, getDecoModelMeta(4) >> 2, 6, 4, 10, box);
			fillWithMetadataBlocks(world, box, 6, 2, 10, 6, 3, 10, VB.ladder, metaN);
			//Furniture
			placeBlockAtCurrentPosition(world, VB.oak_stairs, stairN | 4, 12, 1, 5, box); //tables
			placeBlockAtCurrentPosition(world, VB.wooden_slab, 8, 12, 1, 6, box);
			placeBlockAtCurrentPosition(world, VB.oak_stairs, stairS | 4, 12, 1, 7, box);
			fillWithMetadataBlocks(world, box, 9, 1, 4, 9, 1, 5, VB.dark_oak_stairs, stairE | 4);
			fillWithMetadataBlocks(world, box, 8, 1, 4, 8, 1, 5, VB.wooden_slab, 13);
			fillWithMetadataBlocks(world, box, 7, 1, 4, 7, 1, 5, VB.dark_oak_stairs, stairW | 4);
			placeBlockAtCurrentPosition(world, VB.dark_oak_stairs, stairS | 4, 8, 1, 2, box); //couch
			placeBlockAtCurrentPosition(world, VB.dark_oak_stairs, stairW, 7, 1, 2, box);
			placeBlockAtCurrentPosition(world, VB.dark_oak_stairs, stairS, 6, 1, 2, box);
			fillWithMetadataBlocks(world, box, 5, 1, 2, 5, 1, 3, VB.dark_oak_stairs, stairE);
			placeBlockAtCurrentPosition(world, VB.dark_oak_stairs, stairN, 5, 1, 4, box);
			placeBlockAtCurrentPosition(world, VB.oak_stairs, stairW, 10, 1, 5, box); //chairs
			placeBlockAtCurrentPosition(world, VB.oak_stairs, stairN, 8, 1, 6, box);
			placeBlockAtCurrentPosition(world, VB.oak_stairs, stairE, 9, 1, 8, box); //bookshelf
			placeBlockAtCurrentPosition(world, VB.oak_stairs, stairE | 4, 9, 2, 8, box);
			fillWithBlocks(world, box, 8, 1, 8, 8, 2, 8, VB.bookshelf);
			placeBlockAtCurrentPosition(world, VB.oak_stairs, stairW, 7, 1, 8, box);
			placeBlockAtCurrentPosition(world, VB.oak_stairs, stairW | 4, 7, 2, 8, box);
			fillWithMetadataBlocks(world, box, 7, 3, 8, 9, 3, 8, VB.wooden_slab, 1);
			placeBlockAtCurrentPosition(world, VB.double_stone_slab, 0, 4, 1, 5, box); //kitchen
			placeBlockAtCurrentPosition(world, rand.nextBoolean() ? MB.machine_electric_furnace_off : VB.furnace, metaN, 3, 1, 5, box); //idk why the meta is off between all these blocks and idc
			fillWithBlocks(world, box, 2, 1, 5, 2, 1, 6, VB.double_stone_slab);
			placeBlockAtCurrentPosition(world, VB.cauldron, 2, 2, 1, 7, box);
			placeBlockAtCurrentPosition(world, VB.double_stone_slab, 0, 2, 1, 8, box);
			placeBlockAtCurrentPosition(world, VB.double_stone_slab, 0, 4, 3, 5, box);
			placeBlockAtCurrentPosition(world, VB.redstone_lamp, 0, 3, 3, 5, box);
			placeBlockAtCurrentPosition(world, VB.double_stone_slab, 0, 2, 3, 5, box);
			placeBlockAtCurrentPosition(world, MB.steel_wall, metaN, 3, 3, 6, box);
			placeBlockAtCurrentPosition(world, MB.radiorec, getDecoMeta(2), 8, 2, 2, box);
			placeBlockAtCurrentPosition(world, VB.flower_pot, 0, 7, 2, 4, box);

			fillWithBlocks(world, box, 2, 1, 12, 3, 1, 12, VB.bookshelf); //bookshelf/desk
			placeBlockAtCurrentPosition(world, VB.oak_stairs, stairE | 4, 4, 1, 12, box);
			placeBlockAtCurrentPosition(world, VB.wooden_slab, 8, 5, 1, 12, box);
			placeBlockAtCurrentPosition(world, VB.oak_stairs, stairW | 4, 6, 1, 12, box);
			fillWithBlocks(world, box, 7, 1, 12, 7, 2, 12, VB.bookshelf);
			placeBlockAtCurrentPosition(world, VB.wooden_slab, 5, 5, 1, 11, box); //seat
			placeBed(world, box, 1, 3, 1, 10);
			placeBlockAtCurrentPosition(world, VB.flower_pot, 0, 4, 2, 12, box);
			placeBlockAtCurrentPosition(world, MB.deco_computer, getDecoModelMeta(0), 5, 2, 12, box);

			fillWithMetadataBlocks(world, box, 4, 5, 5, 5, 5, 5, VB.dark_oak_stairs, stairS | 4); //seat and desk
			placeBlockAtCurrentPosition(world, VB.wooden_slab, 1, 4, 5, 6, box);
			placeBlockAtCurrentPosition(world, MB.crate_can, 0, 7, 5, 7, box); //conserve crates
			placeBlockAtCurrentPosition(world, MB.crate_can, 0, 2, 5, 9, box);
			placeBlockAtCurrentPosition(world, MB.crate_can, 0, 3, 5, 11, box);
			if(rand.nextBoolean())
				placeBlockAtCurrentPosition(world, MB.machine_diesel, metaE, 7, 5, 9, box);
			placeBlockAtCurrentPosition(world, rand.nextBoolean() ? MB.crate_weapon : MB.crate, 0, 6, 5, 12, box);

			//inventories
			generateInvContents(world, box, rand, MB.filing_cabinet, getDecoModelMeta(2), 7, 1, 10, ItemPool.getPool(ItemPoolsComponent.POOL_OFFICE_TRASH), 4);
			generateInvContents(world, box, rand, VB.chest, metaE, 7, 5, 5, ItemPool.getPool(ItemPoolsLegacy.POOL_GENERIC), 8);
			//loot
			placeBlockAtCurrentPosition(world, MB.deco_loot, 0, 3, 2, 12, box);
			LootGenerator.lootBookLore(world, getXWithOffset(3, 12), getYWithOffset(2), getZWithOffset(3, 12), com.hbm_m.item.special.ItemBookLore.generateLabBook(net.minecraft.util.RandomSource.create(rand.nextLong()))); //TODO write more lore
			placeBlockAtCurrentPosition(world, MB.deco_loot, 0, 5, 6, 5, box);
			LootGenerator.lootMakeshiftGun(world, getXWithOffset(5, 5), getYWithOffset(6), getZWithOffset(5, 5));
			placeRandomBobble(world, box, rand, 5, 5, 12);

			return true;
		}

		//i don't like this class
		public static class BrokenStairs extends Component.LegacySelector {
			//man.
			public void setMetadata(int meta) {
				this.meta = meta;
			}
			//mannnnnnnn.
			@Override
			public int getSelectedBlockMetaData() {
				return this.block.block() instanceof net.minecraft.world.level.block.StairBlock ? this.meta : 0;
			}

			@Override
			public void selectBlocks(RandomSource rand, int posX, int posY, int posZ, boolean notInterior) {
				float chance = rand.nextFloat();

				if(chance < 0.7)
					this.block = VB.oak_stairs;
				else if(chance < 0.97)
					this.block = VB.wooden_slab;
				else
					this.block = VB.air;
			}
		}

		//this fucking sucks. i am racist against the blockselector class
		public static class BrokenBlocks extends Component.LegacySelector {

			@Override
			public void selectBlocks(RandomSource rand, int posX, int posY, int posZ, boolean notInterior) {
				float chance = rand.nextFloat();

				if(chance < 0.6) {
					this.block = VB.planks;
					this.meta = 0;
				} else if(chance < 0.8) {
					this.block = VB.oak_stairs;
					this.meta = rand.nextInt(4);
				} else {
					this.block = VB.wooden_slab;
					this.meta = 0;
				}
			}
		}
	}
}
