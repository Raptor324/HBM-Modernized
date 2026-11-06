package com.hbm_m.wiaj;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.FluidState;

/**
 * A hastily put together implementation of BlockAndTintGetter in order to render things in the demo screen...
 * It can handle blocks, and not a whole lot else.
 * <p>
 * Port of {@code com.hbm.wiaj.WorldInAJar} (1.7.10): the {@code Block[][][]/short[][][]} pair
 * collapses into a single {@code BlockState[][][]} in modern Minecraft, and the tile map keeps
 * fake entities placed and read by the scripted scenes via {@code ActionSetTile} (the original
 * used dummy tiles to make cables/ducts connect inside the jar - the modern ducts carry
 * connections in the BlockState instead, and {@code Dummies} fills the remaining visual-connection
 * role; four scenes currently place twelve tiles).
 */
public class VirtualWorld implements BlockAndTintGetter {

	public int sizeX;
	public int sizeY;
	public int sizeZ;

	public int lightlevel = 15;

	private BlockState[][][] states;
	private Map<BlockPos, BlockEntity> tiles;

	public VirtualWorld(int x, int y, int z) {
		this.sizeX = x;
		this.sizeY = y;
		this.sizeZ = z;

		this.states = new BlockState[x][y][z];
		this.tiles = new HashMap<>();
	}

	public void nuke() {

		this.states = new BlockState[sizeX][sizeY][sizeZ];
		this.tiles.clear();
	}

	public BlockState getBlock(int x, int y, int z) {
		return this.getBlockState(new BlockPos(x, y, z));
	}

	public BlockState getBlock(BlockPos pos) {
		return this.getBlockState(pos);
	}

	public void setBlock(int x, int y, int z, BlockState state) {
		if(x < 0 || x >= sizeX || y < 0 || y >= sizeY || z < 0 || z >= sizeZ)
			return;

		this.states[x][y][z] = state;
	}

	@Override
	public BlockState getBlockState(BlockPos pos) {
		if(pos.getX() < 0 || pos.getX() >= sizeX || pos.getY() < 0 || pos.getY() >= sizeY || pos.getZ() < 0 || pos.getZ() >= sizeZ)
			return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();

		BlockState state = this.states[pos.getX()][pos.getY()][pos.getZ()];
		return state != null ? state : net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
	}

	@Override
	public FluidState getFluidState(BlockPos pos) {
		// mandated BlockAndTintGetter override: feeds shouldRenderBackwardUpFace and the fluid
		// renderer's getFlow. Out-of-bounds/AIR still yields Fluids.EMPTY, so no other caller
		// changes behavior.
		return this.getBlockState(pos).getFluidState();
	}

	@Override
	public BlockEntity getBlockEntity(BlockPos pos) {
		return this.tiles.get(pos);
	}

	public void setTileEntity(int x, int y, int z, BlockEntity tile) {
		if(x < 0 || x >= sizeX || y < 0 || y >= sizeY || z < 0 || z >= sizeZ)
			return;

		this.tiles.put(new BlockPos(x, y, z), tile);
	}

	//always render fullbright, if the situation requires it we could add a very rudimentary system that
	//darkens blocks if there is a solid one above (the original WorldInAJar.lightlevel = 15 equivalent)
	@Override
	public int getBrightness(LightLayer type, BlockPos pos) {
		return this.lightlevel;
	}

	@Override
	public int getLightEmission(BlockPos pos) {
		return this.lightlevel;
	}

	@Override
	public float getShade(Direction direction, boolean shade) {
		// vanilla Level.getShade
		if(!shade) return 1.0F;
		return switch (direction) {
			case DOWN -> 0.5F;
			case UP -> 1.0F;
			case NORTH, SOUTH -> 0.8F;
			case WEST, EAST -> 0.6F;
		};
	}

	@Override
	public int getBlockTint(BlockPos pos, net.minecraft.world.level.ColorResolver resolver) {
		if (resolver == net.minecraft.client.renderer.BiomeColors.GRASS_COLOR_RESOLVER) {
			return 0x91BD59;
		}
		if (resolver == net.minecraft.client.renderer.BiomeColors.FOLIAGE_COLOR_RESOLVER) {
			return 0x77AB2F;
		}
		if (resolver == net.minecraft.client.renderer.BiomeColors.WATER_COLOR_RESOLVER) {
			return 0x3F76E4;
		}
		return -1;
	}

	private net.minecraft.world.level.lighting.LevelLightEngine cachedLightEngine;

	@Override
	public net.minecraft.world.level.lighting.LevelLightEngine getLightEngine() {
		// delegate to the real client level (cached once captured); the demo screen only
		// opens in-world and all actual brightness queries hit the fullbright
		// getBrightness() override above - this is only a contract-safe fallback
		if(this.cachedLightEngine == null) {
			var mc = net.minecraft.client.Minecraft.getInstance();
			if(mc.level != null) this.cachedLightEngine = mc.level.getLightEngine();
		}
		return this.cachedLightEngine;
	}

	public boolean isAirBlock(int x, int y, int z) {
		return this.getBlock(x, y, z).isAir();
	}

	@Override
	public int getHeight() {
		return this.sizeY;
	}

	@Override
	public int getMinBuildHeight() {
		return 0;
	}
}
