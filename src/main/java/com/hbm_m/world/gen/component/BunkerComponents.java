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





/**
 * 1:1 {@code com.hbm.world.gen.component.BunkerComponents} (Original-Weltgen-Klasse, zeilengetreu auf die Port-Hilfen {@code L}/{@code MB}/{@code VB} umgesetzt).
 */
public class BunkerComponents {
	
	public static class BunkerStart extends ProceduralStructureStart {
		
		/** Port: Fabrik fuer die SpawnCondition (Original: Konstruktor mit World). */
		public static List<StructurePiece> create(com.hbm_m.world.gen.nbt.SpawnCondition.StartContext ctx) {
			return new BunkerStart(ctx.rand(), ctx.chunkX(), ctx.chunkZ()).components;
		}
		
		public BunkerStart(Random rand, int chunkX, int chunkZ) {
			super(chunkX, chunkZ);
			
			this.sizeLimit = 7 + rand.nextInt(6);
			this.distanceLimit = 40;
			
			final int x = chunkX * 16 + 8;
			final int z = chunkZ * 16 + 8;
			
			Weight[] weights = new Weight[] {
				new Weight(6, 3, Corridor::findValidPlacement),
				new Weight(5, 4, BedroomL::findValidPlacement),
				new Weight(10, 3, FunJunction::findValidPlacement),
				new Weight(5, 2, BathroomL::findValidPlacement),
				new Weight(7, 2, Laboratory::findValidPlacement),
				new Weight(5, 1, PowerRoom::findValidPlacement),
			};
			
			StructurePiece starter = new StartingHub(rand, x, z);
			
			buildStart(rand, starter, weights);
			
			this.markAvailableHeight(rand, 20);
		}
		
	}
	
	public static void registerComponents() {
																//TODO more rooms for more variety
	}
	
	//why are we still doing this?
	private static ConcreteBricks ConcreteBricks = new ConcreteBricks();
	
	public static class StartingHub extends Component implements ProceduralComponent {
		
		private boolean[] paths = new boolean[3];
		
		public StartingHub(CompoundTag nbt) {
			super(ComponentTypes.StartingHub.get(), nbt);
			readLegacy(nbt);
		}
		
		public StartingHub(Random rand, int x, int z) {
			super(ComponentTypes.StartingHub.get(), rand, x, 64, z, 7, 5, 7);
		}
		
		public StartingHub(int componentType, BoundingBox box, int coordMode) {
			super(ComponentTypes.StartingHub.get(), componentType);
			this.boundingBox = box;
			this.coordBaseMode = coordMode;
		}
		
		/** write to nbt */
		@Override
		protected void writeLegacy(CompoundTag nbt) {
			super.writeLegacy(nbt);
			for(int i = 0; i < paths.length; i++)
				nbt.putBoolean("p" + i, paths[i]);
		}
		
		/** read from nbt */
		@Override
		protected void readLegacy(CompoundTag nbt) {
			super.readLegacy(nbt);
			for(int i = 0; i < paths.length; i++)
				paths[i] = nbt.getBoolean("p" + i);
		}
		
		@Override
		public void buildComponent(ProceduralStructureStart start, Random rand) {
			paths[0] = this.getNextComponentEast(start, this, coordBaseMode, rand, 5, 1) != null;
			paths[1] = this.getNextComponentAntiNormal(start, this, coordBaseMode, rand, 4, 1) != null;
			paths[2] = this.getNextComponentWest(start, this, coordBaseMode, rand, 3, 1) != null;
		}
		
		@Override
		public boolean addComponentParts(LevelAccessor world, Random rand, BoundingBox box) {
			
			fillWithAir(world, box, 1, 1, 1, 6, 3, 6);
			//floor
			fillWithMetadataBlocks(world, box, 1, 0, 1, 6, 0, 6, MB.vinyl_tile, 1);
			//ceiling
			fillWithBlocks(world, box, 1, 4, 1, 6, 4, 6, MB.vinyl_tile);
			//upper shield
			fillWithBlocks(world, box, 1, 4, 4, 3, 4, 6, MB.reinforced_stone);
			fillWithBlocks(world, box, 0, 5, 0, 7, 5, 7, MB.reinforced_stone);
			//walls
			fillWithRandomizedBlocks(world, box, 0, 0, 0, 0, 4, 7, rand, ConcreteBricks);
			fillWithRandomizedBlocks(world, box, 1, 0, 7, 6, 4, 7, rand, ConcreteBricks);
			fillWithRandomizedBlocks(world, box, 7, 0, 0, 7, 4, 7, rand, ConcreteBricks);
			fillWithRandomizedBlocks(world, box, 1, 0, 0, 6, 4, 0, rand, ConcreteBricks);
			//meh, fix the area later
			final int hpos = Component.getAverageHeight(world, boundingBox, box, boundingBox.maxY()) - boundingBox.minY(); 
			//top hatch
			placeBlockAtCurrentPosition(world, MB.concrete_slab, 1, 0, hpos, 5, box);
			fillWithMetadataBlocks(world, box, 1, hpos, 4, 1, hpos, 6, MB.concrete_smooth_stairs, getStairMeta(0));
			placeBlockAtCurrentPosition(world, MB.concrete_slab, 1, 2, hpos, 3, box);
			placeBlockAtCurrentPosition(world, MB.concrete_smooth_stairs, getStairMeta(2), 2, hpos, 4, box);
			placeBlockAtCurrentPosition(world, MB.trapdoor_steel, getDecoModelMeta(8) >> 2, 2, hpos, 5, box);
			placeBlockAtCurrentPosition(world, MB.concrete_smooth_stairs, getStairMeta(3), 2, hpos, 6, box);
			placeBlockAtCurrentPosition(world, MB.concrete_slab, 1, 2, hpos, 7, box);
			fillWithMetadataBlocks(world, box, 3, hpos, 4, 3, hpos, 6, MB.concrete_smooth_stairs, getStairMeta(1));
			placeBlockAtCurrentPosition(world, MB.concrete_slab, 1, 4, hpos, 5, box);
			//tunnel downwards
			fillWithBlocks(world, box, 1, 6, 4, 1, hpos - 1, 6, MB.reinforced_stone);
			fillWithBlocks(world, box, 2, 1, 6, 2, hpos - 1, 6, MB.reinforced_stone);
			fillWithBlocks(world, box, 3, 6, 4, 3, hpos - 1, 6, MB.reinforced_stone);
			fillWithBlocks(world, box, 2, 6, 4, 2, hpos - 1, 4, MB.reinforced_stone);
			fillWithMetadataBlocks(world, box, 2, 1, 5, 2, hpos - 1, 5, MB.ladder_sturdy, getDecoMeta(2)); //double check meta
			
			/* DECO */
			//lamps
			placeBlockAtCurrentPosition(world, MB.reinforced_lamp_off, 0, 2, 5, 2, box);
			placeBlockAtCurrentPosition(world, MB.reinforced_lamp_off, 0, 5, 5, 2, box);
			placeBlockAtCurrentPosition(world, MB.reinforced_lamp_off, 0, 5, 5, 5, box);
			placeBlockAtCurrentPosition(world, MB.fan, 0, 2, 4, 2, box);
			placeBlockAtCurrentPosition(world, MB.fan, 0, 5, 4, 2, box);
			placeBlockAtCurrentPosition(world, MB.fan, 0, 5, 4, 5, box);
			//machine
			placeBlockAtCurrentPosition(world, MB.deco_tungsten, 0, 3, 1, 6, box);
			generateInvContents(world, box, rand, VB.chest, getDecoMeta(3), 4, 1, 6, ItemPool.getPool(ItemPoolsLegacy.POOL_ANTENNA)/*TODO change */, 5);
			placeBlockAtCurrentPosition(world, MB.deco_tungsten, 0, 5, 1, 6, box);
			fillWithMetadataBlocks(world, box, 3, 2, 6, 5, 2, 6, MB.concrete_smooth_stairs, getStairMeta(2) | 4);
			fillWithMetadataBlocks(world, box, 3, 3, 6, 5, 3, 6, MB.tape_recorder, getDecoMeta(2));
			//desk
			placeBlockAtCurrentPosition(world, MB.concrete_smooth_stairs, getStairMeta(1) | 4, 3, 1, 4, box);
			placeBlockAtCurrentPosition(world, MB.concrete_smooth_stairs, getStairMeta(3) | 4, 4, 1, 4, box);
			placeBlockAtCurrentPosition(world, MB.concrete_smooth_stairs, getStairMeta(0) | 4, 5, 1, 4, box);
			placeBlockAtCurrentPosition(world, MB.deco_computer, getDecoModelMeta(1), 4, 2, 4, box);
			//clear out entryways based on path
			if(paths[0]) fillWithAir(world, box, 7, 1, 2, 7, 2, 3);
			if(paths[1]) fillWithAir(world, box, 3, 1, 0, 4, 2, 0);
			if(paths[2]) fillWithAir(world, box, 0, 1, 2, 0, 2, 3);
			
			return true;
		}
	}
	
	public static class Corridor extends Component implements ProceduralComponent {
		
		private boolean path;
		private int[] decorations = new int[2];
		
		public Corridor(CompoundTag nbt) {
			super(ComponentTypes.Corridor.get(), nbt);
			readLegacy(nbt);
		}
		
		public Corridor(int componentType, BoundingBox box, int coordMode, Random rand) {
			super(ComponentTypes.Corridor.get(), componentType);
			this.boundingBox = box;
			this.coordBaseMode = coordMode;
			
			decorations[0] = rand.nextInt(6);
			decorations[1] = rand.nextInt(6);
		}
		
		/** write to nbt */
		@Override
		protected void writeLegacy(CompoundTag nbt) {
			super.writeLegacy(nbt);
			nbt.putBoolean("p", path);
			nbt.putIntArray("d", decorations);
		}
		
		/** read from nbt */
		@Override
		protected void readLegacy(CompoundTag nbt) {
			super.readLegacy(nbt);
			path = nbt.getBoolean("p");
			decorations = nbt.getIntArray("d");
		}
		
		@Override
		public void buildComponent(ProceduralStructureStart start, Random rand) {
			path = this.getNextComponentNormal(start, this, coordBaseMode, rand, 3, 1) != null;
		}
		
		@Override
		public boolean addComponentParts(LevelAccessor world, Random rand, BoundingBox box) {
			
			fillWithAir(world, box, 1, 1, 1, 4, 3, 5);
			//floor
			fillWithMetadataBlocks(world, box, 1, 0, 1, 4, 0, 5, MB.vinyl_tile, 1);
			//ceiling
			fillWithBlocks(world, box, 1, 4, 1, 4, 4, 5, MB.vinyl_tile);
			//upper shield
			fillWithBlocks(world, box, 0, 5, 0, 5, 5, 6, MB.reinforced_stone);
			//walls
			fillWithRandomizedBlocks(world, box, 0, 0, 0, 0, 4, 6, rand, ConcreteBricks);
			fillWithRandomizedBlocks(world, box, 1, 0, 6, 4, 4, 6, rand, ConcreteBricks);
			fillWithRandomizedBlocks(world, box, 5, 0, 0, 5, 4, 6, rand, ConcreteBricks);
			fillWithRandomizedBlocks(world, box, 1, 0, 0, 4, 4, 0, rand, ConcreteBricks);
			
			//TODO different deco types? maybe plants or vending machines?
			//save it to nbt either way
			/* DECO */
			//lamps
			fillWithBlocks(world, box, 2, 5, 3, 3, 5, 3, MB.reinforced_lamp_off);
			fillWithBlocks(world, box, 2, 4, 3, 3, 4, 3, MB.fan);
			//deco misc
			final int stairMetaW = getStairMeta(0);
			final int stairMetaE = getStairMeta(1);
			final int stairMetaN = getStairMeta(2);
			final int stairMetaS = getStairMeta(3);
			final int decoMetaE = getDecoMeta(4);
			final int decoMetaW = getDecoMeta(5);
			
			for(int i = 0; i <= 1; i++) {
				final int x = 1 + i * 3;
				switch (decorations[i]) {
					default: //table w/ chairs
						placeBlockAtCurrentPosition(world, VB.oak_stairs, stairMetaS, x, 1, 2, box);
						placeBlockAtCurrentPosition(world, VB.oak_stairs, stairMetaN, x, 1, 4, box);
						placeBlockAtCurrentPosition(world, VB.fence, 0, x, 1, 3, box);
						placeBlockAtCurrentPosition(world, VB.wooden_pressure_plate, 1, x, 2, 3, box);
						break;
					case 1://desk w/ computer
						placeBlockAtCurrentPosition(world, MB.concrete_smooth_stairs, stairMetaS | 4, x, 1, 2, box);
						placeBlockAtCurrentPosition(world, VB.oak_stairs, stairMetaN, x, 1, 4, box);
						placeBlockAtCurrentPosition(world, MB.deco_computer, getDecoModelMeta(1), x, 2, 2, box);
						break;
					case 2: //couch
						placeBlockAtCurrentPosition(world, VB.oak_stairs, stairMetaS, x, 1, 2, box);
						placeBlockAtCurrentPosition(world, VB.oak_stairs, i < 1 ? stairMetaE : stairMetaW, x, 1, 3, box);
						placeBlockAtCurrentPosition(world, VB.oak_stairs, stairMetaN, x, 1, 4, box);
						break;
					case 3:
						placeBlockAtCurrentPosition(world, MB.concrete_smooth_stairs, stairMetaS | 4, x, 1, 2, box);
						placeBlockAtCurrentPosition(world, MB.concrete_smooth_stairs, (i < 1 ? stairMetaE : stairMetaW) | 4, x, 1, 3, box);
						placeBlockAtCurrentPosition(world, MB.concrete_smooth_stairs, stairMetaN | 4, x, 1, 4, box);
						placeBlockAtCurrentPosition(world, VB.flower_pot, 0, x, 2, 2, box);
						break;
					case 4:
						fillWithBlocks(world, box, x, 1, 1, x, 3, 1, MB.deco_tungsten);
						placeBlockAtCurrentPosition(world, MB.deco_tungsten, 0, x, 1, 3, box);
						fillWithMetadataBlocks(world, box, x, 3, 2, x, 3, 4, MB.concrete_smooth_stairs, i < 1 ? stairMetaE : stairMetaW);
						fillWithBlocks(world, box, x, 1, 5, x, 3, 5, MB.deco_tungsten);
						fillWithMetadataBlocks(world, box, x, 1, 2, x, 2, 2, MB.tape_recorder, i < 1 ? decoMetaW : decoMetaE); //don't ask me
						fillWithMetadataBlocks(world, box, x, 1, 4, x, 2, 4, MB.tape_recorder, i < 1 ? decoMetaW : decoMetaE);
						placeBlockAtCurrentPosition(world, MB.deco_computer, i < 1 ? getDecoModelMeta(3) : getDecoModelMeta(2), x, 2, 3, box);
						break;
					case 5:
						placeBlockAtCurrentPosition(world, VB.fence, 0, x, 1, 1, box);
						placeBlockAtCurrentPosition(world, VB.wooden_pressure_plate, 0, x, 2, 1, box);
						placeBlockAtCurrentPosition(world, MB.concrete_smooth_stairs, stairMetaS | 4, x, 1, 3, box);
						placeBlockAtCurrentPosition(world, MB.concrete_smooth_stairs, stairMetaN | 4, x, 1, 4, box);
						placeBlockAtCurrentPosition(world, MB.radiorec, i < 1 ? decoMetaE : decoMetaW, x, 2, 3, box);
						break;
				}
			}
			//doors
			placeDoor(world, box, MB.door_bunker, 1, true, rand.nextBoolean(), 2, 1, 0);
			placeDoor(world, box, MB.door_bunker, 1, false, rand.nextBoolean(), 3, 1, 0);
			if(path) fillWithAir(world, box, 2, 1, 6, 3, 2, 6);
			
			return true;
		}
		
		public static StructurePiece findValidPlacement(List<StructurePiece> components, Random rand, int x, int y, int z, int coordMode, int type) {
			BoundingBox box = ProceduralStructureStart.getComponentToAddBoundingBox(x, y, z, -3, -1, 0, 6, 6, 7, coordMode);
			return box.minY() > 10 && Component.findIntersecting(components, box) == null ? new Corridor(type, box, coordMode, rand) : null;
		}
	}
	
	public static class BedroomL extends Component implements ProceduralComponent {
		
		private boolean path;
		
		public BedroomL(CompoundTag nbt) {
			super(ComponentTypes.BedroomL.get(), nbt);
			readLegacy(nbt);
		}
		
		public BedroomL(int componentType, BoundingBox box, int coordMode) {
			super(ComponentTypes.BedroomL.get(), componentType);
			this.boundingBox = box;
			this.coordBaseMode = coordMode;
		}
		
		/** write to nbt */
		@Override
		protected void writeLegacy(CompoundTag nbt) {
			super.writeLegacy(nbt);
			nbt.putBoolean("p", path);
		}
		
		/** read from nbt */
		@Override
		protected void readLegacy(CompoundTag nbt) {
			super.readLegacy(nbt);
			path = nbt.getBoolean("p");
		}
		
		@Override
		public void buildComponent(ProceduralStructureStart start, Random rand) {
			path = this.getNextComponentWest(start, this, coordBaseMode, rand, 9, 1) != null;
		}
		
		@Override
		public boolean addComponentParts(LevelAccessor world, Random rand, BoundingBox box) {
			
			fillWithAir(world, box, 4, 1, 1, 8, 3, 4);
			fillWithAir(world, box, 1, 1, 5, 8, 3, 9);
			//floor
			fillWithMetadataBlocks(world, box, 4, 0, 1, 8, 0, 4, MB.vinyl_tile, 1);
			fillWithMetadataBlocks(world, box, 1, 0, 5, 8, 0, 9, MB.vinyl_tile, 1);
			//ceiling
			fillWithBlocks(world, box, 4, 4, 1, 8, 4, 4, MB.vinyl_tile);
			fillWithBlocks(world, box, 1, 4, 5, 8, 4, 9, MB.vinyl_tile);
			//upper shield
			fillWithBlocks(world, box, 3, 5, 0, 9, 5, 3, MB.reinforced_stone);
			fillWithBlocks(world, box, 0, 5, 4, 9, 5, 10, MB.reinforced_stone);
			//walls
			fillWithRandomizedBlocks(world, box, 0, 0, 4, 0, 4, 10, rand, ConcreteBricks);
			fillWithRandomizedBlocks(world, box, 1, 0, 10, 8, 4, 10, rand, ConcreteBricks);
			fillWithRandomizedBlocks(world, box, 9, 0, 0, 9, 4, 10, rand, ConcreteBricks);
			fillWithRandomizedBlocks(world, box, 4, 0, 0, 8, 4, 0, rand, ConcreteBricks);
			fillWithRandomizedBlocks(world, box, 3, 0, 0, 3, 4, 4, rand, ConcreteBricks);
			fillWithRandomizedBlocks(world, box, 1, 0, 4, 2, 4, 4, rand, ConcreteBricks);
			
			/* DECO */
			//lamps
			placeBlockAtCurrentPosition(world, MB.reinforced_lamp_off, 0, 3, 5, 7, box);
			placeBlockAtCurrentPosition(world, MB.reinforced_lamp_off, 0, 6, 5, 7, box);
			placeBlockAtCurrentPosition(world, MB.reinforced_lamp_off, 0, 6, 5, 3, box);
			placeBlockAtCurrentPosition(world, MB.fan, 0, 3, 4, 7, box);
			placeBlockAtCurrentPosition(world, MB.fan, 0, 6, 4, 7, box);
			placeBlockAtCurrentPosition(world, MB.fan, 0, 6, 4, 3, box);
			//Beds w/ table
			final int stairMetaW = getStairMeta(0);
			final int stairMetaE = getStairMeta(1);
			final int stairMetaN = getStairMeta(2);
			final int stairMetaS = getStairMeta(3);
			placeBed(world, box, 1, 5, 1, 1);
			placeBed(world, box, 1, 5, 1, 3);
			placeBed(world, box, 2, 3, 1, 6);
			placeBed(world, box, 2, 1, 1, 6);
			placeBlockAtCurrentPosition(world, MB.concrete_smooth_stairs, stairMetaE | 4, 4, 1, 2, box);
			placeBlockAtCurrentPosition(world, MB.concrete_smooth_stairs, stairMetaE | 4, 4, 1, 4, box);
			placeBlockAtCurrentPosition(world, MB.concrete_smooth_stairs, stairMetaS | 4, 4, 1, 5, box);
			placeBlockAtCurrentPosition(world, MB.concrete_smooth_stairs, stairMetaS | 4, 2, 1, 5, box);
			placeBlockAtCurrentPosition(world, MB.radiorec, getDecoMeta(4), 4, 2, 4, box);
			//table w/ microwave
			placeBlockAtCurrentPosition(world, MB.concrete_smooth_stairs, stairMetaS | 4, 8, 1, 3, box);
			placeBlockAtCurrentPosition(world, MB.concrete_smooth_stairs, stairMetaN | 4, 8, 1, 4, box);
			placeBlockAtCurrentPosition(world, VB.noteblock, 0, 8, 1, 5, box);
			placeBlockAtCurrentPosition(world, MB.machine_microwave, getDecoMeta(4), 8, 2, 4, box);
			//desk w/ computer
			placeBlockAtCurrentPosition(world, MB.concrete_smooth_stairs, stairMetaW | 4, 6, 1, 9, box);
			placeBlockAtCurrentPosition(world, MB.concrete_smooth_stairs, stairMetaN | 4, 5, 1, 9, box);
			placeBlockAtCurrentPosition(world, MB.concrete_smooth_stairs, stairMetaE | 4, 4, 1, 9, box);
			placeBlockAtCurrentPosition(world, VB.oak_stairs, stairMetaS, 5, 1, 8, box);
			placeBlockAtCurrentPosition(world, MB.deco_computer, getDecoModelMeta(0), 5, 2, 9, box);
			generateInvContents(world, box, rand, MB.filing_cabinet, getDecoModelMeta(0), 3, 1, 9, ItemPool.getPool(ItemPoolsComponent.POOL_FILING_CABINET), 5);
			//lockers
			generateInvContents(world, box, rand, VB.chest, getDecoMeta(4), 8, 1, 7, ItemPool.getPool(ItemPoolsComponent.POOL_VAULT_LOCKERS), 3);
			generateInvContents(world, box, rand, VB.chest, getDecoMeta(4), 8, 2, 7, ItemPool.getPool(ItemPoolsComponent.POOL_VAULT_LOCKERS), 5);
			fillWithBlocks(world, box, 8, 1, 8, 8, 2, 8, MB.deco_tungsten);
			generateInvContents(world, box, rand, VB.chest, getDecoMeta(4), 8, 1, 9, ItemPool.getPool(ItemPoolsComponent.POOL_VAULT_LOCKERS), 4);
			generateInvContents(world, box, rand, VB.chest, getDecoMeta(4), 8, 2, 9, ItemPool.getPool(ItemPoolsComponent.POOL_VAULT_LOCKERS), 5);
			fillWithMetadataBlocks(world, box, 8, 3, 7, 8, 3, 9, VB.trapdoor, getDecoModelMeta(2) >> 2);
			//doors
			placeDoor(world, box, MB.door_bunker, 1, true, rand.nextBoolean(), 7, 1, 0);
			placeDoor(world, box, MB.door_bunker, 1, false, rand.nextBoolean(), 8, 1, 0);
			if(path) fillWithAir(world, box, 0, 1, 8, 0, 2, 9);
			
			return true;
		}
		
		public static StructurePiece findValidPlacement(List<StructurePiece> components, Random rand, int x, int y, int z, int coordMode, int type) {
			BoundingBox box = ProceduralStructureStart.getComponentToAddBoundingBox(x, y, z, -8, -1, 0, 10, 6, 11, coordMode);
			return box.minY() > 10 && Component.findIntersecting(components, box) == null ? new BedroomL(type, box, coordMode) : null;
		}
	}
	
	public static class FunJunction extends Component implements ProceduralComponent {
		
		private boolean[] paths = new boolean[2];
		
		public FunJunction(CompoundTag nbt) {
			super(ComponentTypes.FunJunction.get(), nbt);
			readLegacy(nbt);
		}
		
		public FunJunction(int componentType, BoundingBox box, int coordMode) {
			super(ComponentTypes.FunJunction.get(), componentType);
			this.boundingBox = box;
			this.coordBaseMode = coordMode;
		}
		
		/** write to nbt */
		@Override
		protected void writeLegacy(CompoundTag nbt) {
			super.writeLegacy(nbt);
			for(int i = 0; i < paths.length; i++)
				nbt.putBoolean("p" + i, paths[i]);
		}
		
		/** read from nbt */
		@Override
		protected void readLegacy(CompoundTag nbt) {
			super.readLegacy(nbt);
			for(int i = 0; i < paths.length; i++)
				paths[i] = nbt.getBoolean("p" + i);
		}
		
		@Override
		public void buildComponent(ProceduralStructureStart start, Random rand) {
			paths[0] = this.getNextComponentEast(start, this, coordBaseMode, rand, 6, 1) != null;
			paths[1] = this.getNextComponentNormal(start, this, coordBaseMode, rand, 5, 1) != null;
		}
		
		@Override
		public boolean addComponentParts(LevelAccessor world, Random rand, BoundingBox box) {
			
			fillWithAir(world, box, 1, 1, 1, 6, 3, 10);
			//floor
			fillWithMetadataBlocks(world, box, 1, 0, 1, 6, 0, 10, MB.vinyl_tile, 1);
			//ceiling
			fillWithBlocks(world, box, 1, 4, 1, 6, 4, 10, MB.vinyl_tile);
			//upper shield
			fillWithBlocks(world, box, 0, 5, 0, 7, 5, 11, MB.reinforced_stone);
			//walls
			fillWithRandomizedBlocks(world, box, 0, 0, 0, 0, 4, 11, rand, ConcreteBricks);
			fillWithRandomizedBlocks(world, box, 1, 0, 11, 6, 4, 11, rand, ConcreteBricks);
			fillWithRandomizedBlocks(world, box, 7, 0, 0, 7, 4, 11, rand, ConcreteBricks);
			fillWithRandomizedBlocks(world, box, 1, 0, 0, 6, 4, 0, rand, ConcreteBricks);
			
			/* DECO */
			//lamps
			placeBlockAtCurrentPosition(world, MB.reinforced_lamp_off, 0, 2, 5, 3, box);
			fillWithBlocks(world, box, 5, 5, 5, 5, 5, 6, MB.reinforced_lamp_off);
			placeBlockAtCurrentPosition(world, MB.reinforced_lamp_off, 0, 2, 5, 8, box);
			placeBlockAtCurrentPosition(world, MB.fan, 0, 2, 4, 3, box);
			fillWithBlocks(world, box, 5, 4, 5, 5, 4, 6, MB.fan);
			placeBlockAtCurrentPosition(world, MB.fan, 0, 2, 4, 8, box);
			//couches w/ tables
			final int stairMetaW = getStairMeta(0);
			final int stairMetaE = getStairMeta(1);
			final int stairMetaN = getStairMeta(2);
			final int stairMetaS = getStairMeta(3);
			placeBlockAtCurrentPosition(world, VB.oak_stairs, stairMetaE, 1, 1, 1, box);
			placeBlockAtCurrentPosition(world, VB.oak_stairs, stairMetaS, 2, 1, 1, box);
			placeBlockAtCurrentPosition(world, VB.oak_stairs, stairMetaW, 3, 1, 1, box);
			placeBlockAtCurrentPosition(world, VB.oak_stairs, stairMetaS, 1, 1, 4, box);
			placeBlockAtCurrentPosition(world, VB.oak_stairs, stairMetaE, 1, 1, 5, box);
			fillWithMetadataBlocks(world, box, 1, 1, 6, 2, 1, 6, VB.oak_stairs, stairMetaN);
			placeBlockAtCurrentPosition(world, VB.oak_stairs, stairMetaW, 3, 1, 6, box);
			placeBlockAtCurrentPosition(world, VB.fence, 0, 1, 1, 3, box);
			placeBlockAtCurrentPosition(world, VB.wooden_pressure_plate, 0, 1, 2, 3, box);
			placeBlockAtCurrentPosition(world, VB.fence, 0, 3, 1, 4, box);
			placeBlockAtCurrentPosition(world, VB.wooden_pressure_plate, 0, 3, 2, 4, box);
			//table & chest
			placeBlockAtCurrentPosition(world, VB.fence, 0, 6, 1, 2, box);
			placeBlockAtCurrentPosition(world, VB.wooden_pressure_plate, 0, 6, 2, 2, box);
			generateInvContents(world, box, rand, VB.chest, getDecoMeta(4), 6, 1, 3, ItemPool.getPool(ItemPoolsComponent.POOL_VAULT_LOCKERS), 8);
			//desk w/ computer + bobblehead
			placeBlockAtCurrentPosition(world, MB.concrete_smooth_stairs, stairMetaS | 4, 1, 1, 8, box);
			placeBlockAtCurrentPosition(world, MB.concrete_smooth_stairs, stairMetaE | 4, 1, 1, 9, box);
			placeBlockAtCurrentPosition(world, MB.concrete_smooth_stairs, stairMetaN | 4, 1, 1, 10, box);
			placeBlockAtCurrentPosition(world, VB.oak_stairs, stairMetaS, 2, 1, 8, box);
			placeBlockAtCurrentPosition(world, MB.deco_computer, getDecoModelMeta(3), 1, 2, 9, box);
			if(rand.nextBoolean()) placeRandomBobble(world, box, rand, 1, 2, 8);
			//jukebox
			fillWithBlocks(world, box, 6, 1, 8, 6, 2, 8, VB.noteblock);
			placeBlockAtCurrentPosition(world, MB.deco_tungsten, 0, 6, 1, 9, box);
			placeBlockAtCurrentPosition(world, MB.tape_recorder, getDecoMeta(4), 6, 2, 9, box);
			fillWithBlocks(world, box, 6, 3, 8, 6, 3, 9, MB.concrete_slab);
			placeLever(world, box, 2, rand.nextBoolean(), 5, 1, 9);
			//doors
			placeDoor(world, box, MB.door_bunker, 1, true, rand.nextBoolean(), 4, 1, 0);
			placeDoor(world, box, MB.door_bunker, 1, false, rand.nextBoolean(), 5, 1, 0);
			if(paths[0]) fillWithAir(world, box, 7, 1, 5, 7, 2, 6);
			if(paths[1]) fillWithAir(world, box, 4, 1, 11, 5, 2, 11);
			
			return true;
		}
		
		public static StructurePiece findValidPlacement(List<StructurePiece> components, Random rand, int x, int y, int z, int coordMode, int type) {
			BoundingBox box = ProceduralStructureStart.getComponentToAddBoundingBox(x, y, z, -5, -1, 0, 8, 6, 12, coordMode);
			return box.minY() > 10 && Component.findIntersecting(components, box) == null ? new FunJunction(type, box, coordMode) : null;
		}
	}
	
	public static class BathroomL extends Component implements ProceduralComponent {
		
		private boolean path;
		
		public BathroomL(CompoundTag nbt) {
			super(ComponentTypes.BathroomL.get(), nbt);
			readLegacy(nbt);
		}
		
		public BathroomL(int componentType, BoundingBox box, int coordMode) {
			super(ComponentTypes.BathroomL.get(), componentType);
			this.boundingBox = box;
			this.coordBaseMode = coordMode;
		}
		
		/** write to nbt */
		@Override
		protected void writeLegacy(CompoundTag nbt) {
			super.writeLegacy(nbt);
			nbt.putBoolean("p", path);
		}
		
		/** read from nbt */
		@Override
		protected void readLegacy(CompoundTag nbt) {
			super.readLegacy(nbt);
			path = nbt.getBoolean("p");
		}
		
		@Override
		public void buildComponent(ProceduralStructureStart start, Random rand) {
			path = this.getNextComponentEast(start, this, coordBaseMode, rand, 3, 1) != null;
		}
		
		@Override
		public boolean addComponentParts(LevelAccessor world, Random rand, BoundingBox box) {
			
			fillWithAir(world, box, 1, 1, 1, 7, 3, 9);
			//floor
			fillWithMetadataBlocks(world, box, 1, 0, 1, 7, 0, 9, MB.vinyl_tile, 1);
			//ceiling
			fillWithBlocks(world, box, 1, 4, 1, 7, 4, 9, MB.vinyl_tile);
			//upper shield
			fillWithBlocks(world, box, 0, 5, 0, 8, 5, 10, MB.reinforced_stone);
			//walls
			fillWithRandomizedBlocks(world, box, 0, 0, 0, 0, 4, 10, rand, ConcreteBricks);
			fillWithRandomizedBlocks(world, box, 1, 0, 10, 7, 4, 10, rand, ConcreteBricks);
			fillWithRandomizedBlocks(world, box, 8, 0, 0, 8, 4, 10, rand, ConcreteBricks);
			fillWithRandomizedBlocks(world, box, 1, 0, 0, 7, 4, 0, rand, ConcreteBricks);
			
			/* DECO */
			//lamps
			placeBlockAtCurrentPosition(world, MB.reinforced_lamp_off, 0, 2, 5, 3, box);
			placeBlockAtCurrentPosition(world, MB.reinforced_lamp_off, 0, 2, 5, 7, box);
			placeBlockAtCurrentPosition(world, MB.reinforced_lamp_off, 0, 5, 5, 7, box);
			placeBlockAtCurrentPosition(world, MB.reinforced_lamp_off, 0, 5, 5, 3, box);
			placeBlockAtCurrentPosition(world, MB.fan, 0, 2, 4, 3, box);
			placeBlockAtCurrentPosition(world, MB.fan, 0, 2, 4, 7, box);
			placeBlockAtCurrentPosition(world, MB.fan, 0, 5, 4, 7, box);
			placeBlockAtCurrentPosition(world, MB.fan, 0, 5, 4, 3, box);
			//sinks
			for(int i = 2; i <= 8; i += 2) {
				placeBlockAtCurrentPosition(world, VB.cauldron, rand.nextInt(4), 1, 1, i, box);
				placeBlockAtCurrentPosition(world, MB.concrete_slab, 8, 1, 1, i + 1, box);
				placeBlockAtCurrentPosition(world, VB.tripwire_hook, getTripwireMeta(3), 1, 2, i, box);
			}
			//hand-dryers (industrial-strength)
			placeBlockAtCurrentPosition(world, MB.steel_beam, 3, 4, 1, 9, box);
			placeBlockAtCurrentPosition(world, MB.fan, getDecoMeta(2), 4, 2, 9, box);
			placeBlockAtCurrentPosition(world, VB.stone_button, getButtonMeta(2), 3, 2, 9, box); //TODO button meta
			placeBlockAtCurrentPosition(world, MB.steel_beam, 3, 6, 1, 9, box);
			placeBlockAtCurrentPosition(world, MB.fan, getDecoMeta(2), 6, 2, 9, box);
			placeBlockAtCurrentPosition(world, VB.stone_button, getButtonMeta(1), 7, 2, 9, box);
			//stalls w/ toilets
			for(int i = 1; i <= 5; i += 2) {
				placeDoor(world, box, MB.door_metal, 0, false, rand.nextBoolean(), 5, 1, i);
				fillWithMetadataBlocks(world, box, 5, 1, i + 1, 5, 2, i + 1, MB.steel_corner, getDecoMeta(2));
				fillWithMetadataBlocks(world, box, 6, 1, i + 1, 7, 2, i + 1, MB.steel_wall, getDecoMeta(2));
				placeBlockAtCurrentPosition(world, MB.deco_pipe_rim, 0, 7, 1, i, box);
				placeBlockAtCurrentPosition(world, VB.trapdoor, getDecoModelMeta(2) >> 2, 7, 2, i, box);
			}
			//doors
			placeDoor(world, box, MB.door_bunker, 1, true, rand.nextBoolean(), 2, 1, 0);
			placeDoor(world, box, MB.door_bunker, 1, false, rand.nextBoolean(), 3, 1, 0);
			if(path) fillWithAir(world, box, 8, 1, 7, 8, 2, 8);
			
			return true;
		}
		
		public static StructurePiece findValidPlacement(List<StructurePiece> components, Random rand, int x, int y, int z, int coordMode, int type) {
			BoundingBox box = ProceduralStructureStart.getComponentToAddBoundingBox(x, y, z, -3, -1, 0, 9, 6, 11, coordMode);
			return box.minY() > 10 && Component.findIntersecting(components, box) == null ? new BathroomL(type, box, coordMode) : null;
		}
	}
	
	public static class Laboratory extends Component implements ProceduralComponent {
		
		private boolean[] paths = new boolean[2];
		
		public Laboratory(CompoundTag nbt) {
			super(ComponentTypes.Laboratory.get(), nbt);
			readLegacy(nbt);
		}
		
		public Laboratory(int componentType, BoundingBox box, int coordMode) {
			super(ComponentTypes.Laboratory.get(), componentType);
			this.boundingBox = box;
			this.coordBaseMode = coordMode;
		}
		
		/** write to nbt */
		@Override
		protected void writeLegacy(CompoundTag nbt) {
			super.writeLegacy(nbt);
			for(int i = 0; i < paths.length; i++)
				nbt.putBoolean("p" + i, paths[i]);
		}
		
		/** read from nbt */
		@Override
		protected void readLegacy(CompoundTag nbt) {
			super.readLegacy(nbt);
			for(int i = 0; i < paths.length; i++)
				paths[i] = nbt.getBoolean("p" + i);
		}
		
		@Override
		public void buildComponent(ProceduralStructureStart start, Random rand) {
			paths[0] = this.getNextComponentWest(start, this, coordBaseMode, rand, 3, 1) != null;
			paths[1] = this.getNextComponentNormal(start, this, coordBaseMode, rand, 6, 1) != null;
		}
		
		@Override
		public boolean addComponentParts(LevelAccessor world, Random rand, BoundingBox box) {
			
			fillWithAir(world, box, 1, 1, 1, 7, 3, 11);
			//floor
			fillWithMetadataBlocks(world, box, 1, 0, 1, 7, 0, 11, MB.vinyl_tile, 1);
			//ceiling
			fillWithBlocks(world, box, 1, 4, 1, 7, 4, 11, MB.vinyl_tile);
			//upper shield
			fillWithBlocks(world, box, 0, 5, 0, 8, 5, 12, MB.reinforced_stone);
			//walls
			fillWithBlocks(world, box, 0, 0, 0, 0, 4, 12, MB.brick_concrete);
			fillWithBlocks(world, box, 1, 0, 12, 7, 4, 12, MB.brick_concrete);
			fillWithBlocks(world, box, 8, 0, 0, 8, 4, 12, MB.brick_concrete);
			fillWithBlocks(world, box, 1, 0, 0, 7, 4, 0, MB.brick_concrete);
			
			/* DECO */
			//lamps
			for(int x = 3; x <= 5; x += 2) {
				for(int z = 3; z <= 9; z += 3) {
					placeBlockAtCurrentPosition(world, MB.reinforced_lamp_off, 0, x, 5, z, box);
					placeBlockAtCurrentPosition(world, MB.fan, 0, x, 4, z, box);
				}
			}
			//couch w/ table
			final int stairMetaW = getStairMeta(0);
			final int stairMetaE = getStairMeta(1);
			final int stairMetaN = getStairMeta(2);
			final int stairMetaS = getStairMeta(3);
			placeBlockAtCurrentPosition(world, VB.oak_stairs, stairMetaE, 1, 1, 1, box);
			placeBlockAtCurrentPosition(world, VB.oak_stairs, stairMetaS, 2, 1, 1, box);
			placeBlockAtCurrentPosition(world, VB.oak_stairs, stairMetaW, 3, 1, 1, box);
			placeBlockAtCurrentPosition(world, VB.fence, 0, 4, 1, 1, box);
			placeBlockAtCurrentPosition(world, VB.wooden_pressure_plate, 0, 4, 2, 1, box);
			//big ole wall machine
			final int decoMetaE = getDecoMeta(4);
			final int decoMetaW = getDecoMeta(5);
			final int decoModelMetaW = getDecoModelMeta(2);
			final int decoModelMetaE = getDecoModelMeta(3);
			fillWithBlocks(world, box, 1, 1, 5, 1, 3, 5, MB.deco_tungsten);
			placeBlockAtCurrentPosition(world, MB.deco_steel, 0, 1, 1, 6, box);
			placeBlockAtCurrentPosition(world, MB.deco_computer, decoModelMetaE, 1, 2, 6, box);
			placeBlockAtCurrentPosition(world, MB.tape_recorder, decoMetaW, 1, 3, 6, box);
			fillWithMetadataBlocks(world, box, 1, 1, 7, 1, 3, 7, MB.tape_recorder, decoMetaW);
			fillWithBlocks(world, box, 1, 1, 8, 1, 3, 8, MB.deco_tungsten);
			fillWithMetadataBlocks(world, box, 1, 1, 9, 1, 1, 10, MB.tape_recorder, decoMetaW);
			fillWithMetadataBlocks(world, box, 1, 2, 9, 1, 2, 10, MB.concrete_smooth_stairs, stairMetaE | 4);
			fillWithMetadataBlocks(world, box, 1, 3, 9, 1, 3, 10, MB.tape_recorder, decoMetaW);
			fillWithBlocks(world, box, 1, 1, 11, 1, 3, 11, MB.deco_tungsten);
			//desks w/ computers
			generateInvContents(world, box, rand, VB.chest, getDecoMeta(2), 3, 1, 4, ItemPool.getPool(ItemPoolsComponent.POOL_MACHINE_PARTS), 6);
			placeBlockAtCurrentPosition(world, MB.concrete_smooth_stairs, stairMetaS | 4, 3, 1, 5, box);
			fillWithMetadataBlocks(world, box, 4, 1, 5, 4, 1, 7, MB.concrete_smooth_stairs, stairMetaW | 4);
			placeBlockAtCurrentPosition(world, VB.oak_stairs, stairMetaN, 3, 1, 7, box);
			placeBlockAtCurrentPosition(world, MB.concrete_smooth_stairs, stairMetaS | 4, 3, 1, 9, box);
			fillWithMetadataBlocks(world, box, 4, 1, 9, 4, 1, 11, MB.concrete_smooth_stairs, stairMetaW | 4);
			placeBlockAtCurrentPosition(world, VB.oak_stairs, stairMetaN, 3, 1, 11, box);
			placeBlockAtCurrentPosition(world, VB.flower_pot, 0, 3, 2, 5, box);
			placeBlockAtCurrentPosition(world, MB.deco_computer, decoModelMetaW, 4, 2, 6, box);
			placeBlockAtCurrentPosition(world, MB.deco_computer, decoModelMetaW, 4, 2, 10, box);
			//lever wall machine
			placeBlockAtCurrentPosition(world, MB.concrete_smooth_stairs, stairMetaW | 4, 7, 1, 3, box);
			placeBlockAtCurrentPosition(world, MB.deco_red_copper, 0, 7, 2, 3, box);
			placeBlockAtCurrentPosition(world, MB.concrete_smooth_stairs, stairMetaW, 7, 3, 3, box);
			placeLever(world, box, 2, rand.nextBoolean(), 6, 2, 3);
			fillWithMetadataBlocks(world, box, 7, 1, 4, 7, 2, 4, MB.steel_poles, decoMetaE);
			placeBlockAtCurrentPosition(world, MB.deco_steel, 0, 7, 3, 4, box);
			placeBlockAtCurrentPosition(world, MB.deco_tungsten, 0, 7, 1, 5, box);
			placeBlockAtCurrentPosition(world, MB.tape_recorder, decoMetaE, 7, 1, 6, box);
			placeBlockAtCurrentPosition(world, MB.deco_tungsten, 0, 7, 1, 7, box);
			fillWithMetadataBlocks(world, box, 7, 2, 5, 7, 2, 7, MB.concrete_smooth_stairs, stairMetaW | 4);
			fillWithMetadataBlocks(world, box, 7, 3, 5, 7, 3, 7, MB.tape_recorder, decoMetaE);
			//table w/ chest
			placeBlockAtCurrentPosition(world, VB.fence, 0, 7, 1, 9, box);
			placeBlockAtCurrentPosition(world, VB.wooden_pressure_plate, 0, 7, 2, 9, box);
			generateInvContents(world, box, rand, VB.chest, getDecoMeta(4), 7, 1, 10, ItemPool.getPool(ItemPoolsComponent.POOL_VAULT_LAB), 8);
			//doors
			placeDoor(world, box, MB.door_bunker, 1, true, rand.nextBoolean(), 5, 1, 0);
			placeDoor(world, box, MB.door_bunker, 1, false, rand.nextBoolean(), 6, 1, 0);
			if(paths[0]) fillWithAir(world, box, 0, 1, 2, 0, 2, 3);
			if(paths[1]) fillWithAir(world, box, 5, 1, 12, 6, 2, 12);
			
			return true;
		}
		
		public static StructurePiece findValidPlacement(List<StructurePiece> components, Random rand, int x, int y, int z, int coordMode, int type) {
			BoundingBox box = ProceduralStructureStart.getComponentToAddBoundingBox(x, y, z, -6, -1, 0, 9, 6, 12, coordMode);
			return box.minY() > 10 && Component.findIntersecting(components, box) == null ? new Laboratory(type, box, coordMode) : null;
		}
	}
	
	public static class PowerRoom extends Component implements ProceduralComponent {
		
		private boolean path;
		
		private int powerType;
		
		public PowerRoom(CompoundTag nbt) {
			super(ComponentTypes.PowerRoom.get(), nbt);
			readLegacy(nbt);
		}
		
		public PowerRoom(int componentType, BoundingBox box, int coordMode, Random rand) {
			super(ComponentTypes.PowerRoom.get(), componentType);
			this.boundingBox = box;
			this.coordBaseMode = coordMode;
			
			float chance = rand.nextFloat();
			powerType = chance < 0.2 ? 2 : chance < 0.6 ? 1 : 0;
		}
		
		/** write to nbt */
		@Override
		protected void writeLegacy(CompoundTag nbt) {
			super.writeLegacy(nbt);
			nbt.putBoolean("p", path);
		}
		
		/** read from nbt */
		@Override
		protected void readLegacy(CompoundTag nbt) {
			super.readLegacy(nbt);
			path = nbt.getBoolean("p");
		}
		
		@Override
		public void buildComponent(ProceduralStructureStart start, Random rand) {
			path = this.getNextComponentEast(start, this, coordBaseMode, rand, 4, 1) != null;
		}
		
		@Override
		public boolean addComponentParts(LevelAccessor world, Random rand, BoundingBox box) {
			
			fillWithAir(world, box, 1, 1, 1, 10, 3, 10);
			//floor
			fillWithMetadataBlocks(world, box, 1, 0, 1, 10, 0, 10, MB.vinyl_tile, 1);
			//ceiling
			fillWithBlocks(world, box, 1, 4, 1, 10, 4, 10, MB.vinyl_tile);
			//upper shield
			fillWithBlocks(world, box, 0, 5, 0, 11, 5, 11, MB.reinforced_stone);
			//walls
			fillWithRandomizedBlocks(world, box, 0, 0, 0, 11, 4, 0, rand, ConcreteBricks);
			fillWithRandomizedBlocks(world, box, 0, 0, 1, 0, 4, 10, rand, ConcreteBricks);
			fillWithRandomizedBlocks(world, box, 0, 0, 11, 11, 4, 11, rand, ConcreteBricks);
			fillWithRandomizedBlocks(world, box, 11, 0, 1, 11, 4, 10, rand, ConcreteBricks);
			fillWithRandomizedBlocks(world, box, 5, 1, 1, 5, 3, 6, rand, ConcreteBricks);
			fillWithRandomizedBlocks(world, box, 6, 1, 6, 10, 3, 6, rand, ConcreteBricks);
			
			/* DECO */
			//lamps
			placeBlockAtCurrentPosition(world, MB.reinforced_lamp_off, 0, 3, 5, 2, box);
			placeBlockAtCurrentPosition(world, MB.reinforced_lamp_off, 0, 3, 5, 5, box);
			placeBlockAtCurrentPosition(world, MB.reinforced_lamp_off, 0, 3, 5, 8, box);
			placeBlockAtCurrentPosition(world, MB.reinforced_lamp_off, 0, 6, 5, 8, box);
			placeBlockAtCurrentPosition(world, MB.reinforced_lamp_off, 0, 9, 5, 8, box);
			placeBlockAtCurrentPosition(world, MB.fan, 0, 3, 4, 2, box);
			placeBlockAtCurrentPosition(world, MB.fan, 0, 3, 4, 5, box);
			placeBlockAtCurrentPosition(world, MB.fan, 0, 3, 4, 8, box);
			placeBlockAtCurrentPosition(world, MB.fan, 0, 6, 4, 8, box);
			placeBlockAtCurrentPosition(world, MB.fan, 0, 9, 4, 8, box);
			//power room stuff
			fillWithBlocks(world, box, 7, 2, 6, 9, 2, 6, MB.reinforced_glass);
			int decoMetaE = getDecoMeta(5);
			int decoMetaW = getDecoMeta(4);
			int decoMetaN = getDecoMeta(3);
			int decoMetaS = getDecoMeta(2);
			
			int stairMetaS = getStairMeta(3);
			int stairMetaN = getStairMeta(2);
			int stairMetaW = getStairMeta(1);
			int stairMetaE = getStairMeta(0);
			
			switch(this.powerType) {
			default:
				fillWithBlocks(world, box, 6, 1, 1, 6, 3, 1, MB.deco_pipe_framed_rusted);
				for(int i = 7; i <= 9; i += 2) {
					placeBlockAtCurrentPosition(world, MB.machine_electric_furnace_off, decoMetaN, i, 1, 1, box);
					placeBlockAtCurrentPosition(world, MB.steel_beam, 2, i, 2, 1, box);
					placeBlockAtCurrentPosition(world, MB.machine_electric_furnace_off, decoMetaN, i, 3, 1, box);
				}
				placeBlockAtCurrentPosition(world, MB.deco_red_copper, 0, 8, 1, 1, box);
				placeBlockAtCurrentPosition(world, MB.concrete_colored_ext, 5, 8, 2, 1, box);
				placeBlockAtCurrentPosition(world, MB.deco_red_copper, 0, 8, 3, 1, box);
				placeLever(world, box, 3, rand.nextBoolean(), 8, 2, 2);
				for(int i = 1; i <= 3; i += 2) {
					placeBlockAtCurrentPosition(world, MB.deco_steel, 0, 10, i, 1, box);
					fillWithMetadataBlocks(world, box, 10, i, 2, 10, i, 4, MB.deco_pipe_quad_rusted, getPillarMeta(8));
					placeBlockAtCurrentPosition(world, MB.deco_steel, 0, 10, i, 5, box);
				}
				placeBlockAtCurrentPosition(world, MB.deco_pipe_framed_rusted, 0, 10, 2, 1, box);
				placeBlockAtCurrentPosition(world, MB.fluid_duct_gauge, decoMetaW, 10, 2, 5, box);
				placeBlockAtCurrentPosition(world, MB.barrel_plastic, 0, 6, 1, 5, box);
				//chests
				generateInvContents(world, box, rand, VB.chest, decoMetaS, 7, 1, 5, ItemPool.getPool(ItemPoolsComponent.POOL_SOLID_FUEL), 5);
				generateInvContents(world, box, rand, VB.chest, decoMetaS, 9, 1, 5, ItemPool.getPool(ItemPoolsComponent.POOL_SOLID_FUEL), 6);
				break;
			case 1:
				placeBlockAtCurrentPosition(world, MB.concrete_colored_ext, 5, 6, 1, 1, box);
				placeBlockAtCurrentPosition(world, MB.cable_detector, 0, 6, 2, 1, box);
				placeBlockAtCurrentPosition(world, MB.concrete_colored_ext, 5, 6, 3, 1, box);
				placeLever(world, box, 3, false, 6, 2, 2);
				for(int i = 7; i <= 9; i += 2) {
					placeBlockAtCurrentPosition(world, MB.steel_scaffold, 8, i, 1, 1, box); //i'm not making another fucking meta method
					placeBlockAtCurrentPosition(world, MB.machine_diesel, decoMetaE, i, 2, 1, box);
				}
				placeBlockAtCurrentPosition(world, MB.deco_pipe_rim_rusted, getPillarMeta(4), 8, 2, 1, box);
				placeBlockAtCurrentPosition(world, MB.deco_pipe_rim_rusted, getPillarMeta(4), 8, 2, 1, box);
				fillWithMetadataBlocks(world, box, 7, 3, 1, 9, 3, 1, MB.concrete_smooth_stairs, stairMetaS);
				fillWithBlocks(world, box, 10, 1, 1, 10, 1, 3, MB.deco_steel);
				placeBlockAtCurrentPosition(world, MB.deco_red_copper, 0, 10, 2, 1, box);
				placeBlockAtCurrentPosition(world, MB.deco_steel, 0, 10, 3, 1, box);
				placeBlockAtCurrentPosition(world, MB.steel_grate, 7, 10, 2, 2, box);
				placeBlockAtCurrentPosition(world, MB.deco_computer, getDecoModelMeta(2), 10, 3, 2, box);
				fillWithMetadataBlocks(world, box, 10, 2, 3, 10, 3, 3, MB.tape_recorder, decoMetaW);
				fillWithMetadataBlocks(world, box, 9, 1, 2, 9, 1, 3, MB.steel_grate, 7);
				fillWithBlocks(world, box, 9, 1, 5, 10, 1, 5, MB.barrel_corroded);
				placeBlockAtCurrentPosition(world, MB.barrel_corroded, 0, 10, 2, 5, box);
				fillWithBlocks(world, box, 6, 1, 5, 6, 2, 5, MB.barrel_corroded);
				placeBlockAtCurrentPosition(world, MB.barrel_corroded, 0, 6, 1, 2, box);
				break;
			case 2:
				for(int i = 7; i <= 9; i += 2) {
					fillWithBlocks(world, box, i, 1, 2, i, 1, 4, MB.deco_lead);
					fillWithBlocks(world, box, i, 2, 2, i, 2, 4, MB.block_lead);
					fillWithBlocks(world, box, i, 3, 2, i, 3, 4, MB.deco_lead);
				}
				placeBlockAtCurrentPosition(world, MB.concrete_colored_ext, 5, 8, 1, 4, box);
				placeBlockAtCurrentPosition(world, VB.redstone_lamp, 0, 8, 2, 4, box);
				placeBlockAtCurrentPosition(world, MB.concrete_colored_ext, 5, 8, 3, 4, box);
				placeLever(world, box, 3, rand.nextBoolean(), 8, 2, 5);
				placeBlockAtCurrentPosition(world, MB.pwr_fuel, 0, 8, 1, 3, box);
				placeBlockAtCurrentPosition(world, MB.pwr_control, 0, 8, 2, 3, box);
				placeBlockAtCurrentPosition(world, MB.pwr_fuel, 0, 8, 3, 3, box);
				placeBlockAtCurrentPosition(world, MB.block_copper, 0, 8, 1, 2, box);
				placeBlockAtCurrentPosition(world, MB.block_lead, 0, 8, 2, 2, box);
				placeBlockAtCurrentPosition(world, MB.block_copper, 0, 8, 3, 2, box);
				placeBlockAtCurrentPosition(world, MB.pwr_channel, 0, 8, 1, 1, box);
				placeBlockAtCurrentPosition(world, MB.machine_turbine, 0, 8, 2, 1, box);
				placeBlockAtCurrentPosition(world, MB.pwr_channel, 0, 8, 3, 1, box);
				fillWithBlocks(world, box, 9, 1, 1, 9, 3, 1, MB.deco_steel);
				placeBlockAtCurrentPosition(world, MB.steel_grate, 7, 10, 1, 1, box);
				placeBlockAtCurrentPosition(world, MB.deco_computer, getDecoModelMeta(1), 10, 2, 1, box);
				placeBlockAtCurrentPosition(world, MB.tape_recorder, decoMetaN, 10, 3, 1, box);
				fillWithMetadataBlocks(world, box, 6, 1, 1, 7, 1, 1, MB.deco_pipe_quad_rusted, getPillarMeta(4));
				placeBlockAtCurrentPosition(world, MB.deco_pipe_quad_rusted, getPillarMeta(4), 7, 3, 1, box);
				placeBlockAtCurrentPosition(world, MB.fluid_duct_gauge, decoMetaN, 6, 3, 1, box);
				//chest
				generateInvContents(world, box, rand, VB.chest, decoMetaN, 6, 1, 2, ItemPool.getPool(ItemPoolsComponent.POOL_NUKE_FUEL), 8);
				break;
			}
			//transformer
			fillWithMetadataBlocks(world, box, 1, 1, 1, 1, 1, 5, MB.concrete_smooth_stairs, stairMetaW | 4);
			fillWithBlocks(world, box, 1, 1, 6, 1, 3, 6, MB.concrete_pillar);
			fillWithMetadataBlocks(world, box, 1, 3, 1, 1, 3, 5, MB.concrete_smooth_stairs, stairMetaW);
			placeBlockAtCurrentPosition(world, MB.machine_transformer, 0, 1, 2, 1, box);
			placeBlockAtCurrentPosition(world, MB.cable_diode, decoMetaN, 1, 2, 2, box);
			placeBlockAtCurrentPosition(world, MB.capacitor_copper, 0, 1, 2, 3, box);
			placeBlockAtCurrentPosition(world, MB.deco_red_copper, 0, 1, 2, 4, box);
			placeBlockAtCurrentPosition(world, MB.cable_switch, 0, 1, 2, 5, box);
			//machine
			for(int i = 1; i <= 5; i += 4) {
				placeBlockAtCurrentPosition(world, MB.deco_beryllium, 0, i, 1, 10, box);
				placeBlockAtCurrentPosition(world, MB.steel_scaffold, 0, i, 2, 10, box);
				placeBlockAtCurrentPosition(world, MB.deco_beryllium, 0, i, 3, 10, box);
			}
			placeBlockAtCurrentPosition(world, MB.steel_scaffold, 0, 2, 1, 10, box);
			placeBlockAtCurrentPosition(world, MB.deco_tungsten, 0, 3, 1, 10, box);
			placeBlockAtCurrentPosition(world, MB.steel_scaffold, 0, 4, 1, 10, box);
			placeBlockAtCurrentPosition(world, MB.tape_recorder, decoMetaS, 2, 2, 10, box);
			placeBlockAtCurrentPosition(world, MB.deco_computer, getDecoModelMeta(0), 3, 2, 10, box);
			placeBlockAtCurrentPosition(world, MB.tape_recorder, decoMetaS, 4, 2, 10, box);
			fillWithMetadataBlocks(world, box, 2, 3, 10, 4, 3, 10, MB.tape_recorder, decoMetaS);
			//desk
			fillWithMetadataBlocks(world, box, 8, 1, 10, 10, 1, 10, MB.concrete_smooth_stairs, stairMetaN | 4);
			placeBlockAtCurrentPosition(world, MB.concrete_smooth_stairs, stairMetaE | 4, 10, 1, 9, box);
			placeBlockAtCurrentPosition(world, VB.oak_stairs,stairMetaS, 9, 1, 9, box);
			placeBlockAtCurrentPosition(world, VB.flower_pot, 0, 8, 2, 10, box);
			placeBlockAtCurrentPosition(world, MB.deco_computer, getDecoModelMeta(0), 9, 2, 10, box);
			//loot
			generateInvContents(world, box, rand, VB.chest, decoMetaE, 1, 1, 7, ItemPool.getPool(ItemPoolsComponent.POOL_MACHINE_PARTS), 6);
			generateInvContents(world, box, rand, MB.filing_cabinet, getDecoModelMeta(0), 7, 1, 10, ItemPool.getPool(ItemPoolsComponent.POOL_FILING_CABINET), 4);
			//doors
			placeDoor(world, box, MB.door_bunker, 1, true, rand.nextBoolean(), 3, 1, 0);
			placeDoor(world, box, MB.door_bunker, 1, false, rand.nextBoolean(), 4, 1, 0);
			placeDoor(world, box, MB.door_bunker, 0, false, false, 5, 1, 3);
			if(path) fillWithAir(world, box, 11, 1, 7, 11, 2, 8);
			
			return true;
		}
		
		public static StructurePiece findValidPlacement(List<StructurePiece> components, Random rand, int x, int y, int z, int coordMode, int type) {
			BoundingBox box = ProceduralStructureStart.getComponentToAddBoundingBox(x, y, z, -4, -1, 0, 12, 6, 12, coordMode);
			return box.minY() > 10 && Component.findIntersecting(components, box) == null ? new PowerRoom(type, box, coordMode, rand) : null;
		}
	}
}
