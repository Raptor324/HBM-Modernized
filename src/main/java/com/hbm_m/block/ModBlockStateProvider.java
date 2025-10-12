package com.hbm_m.block;

import com.hbm_m.main.MainRegistry;
import com.hbm_m.block.ModBlocks;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ConfiguredModel;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Function;

public class ModBlockStateProvider extends BlockStateProvider {
    public ModBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, MainRegistry.MOD_ID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        blockWithItem(ModBlocks.REINFORCED_STONE);
        stairsBlock(((StairBlock) ModBlocks.REINFORCED_STONE_STAIRS.get()), blockTexture(ModBlocks.REINFORCED_STONE.get()));
        slabBlock(((SlabBlock) ModBlocks.REINFORCED_STONE_SLAB.get()), blockTexture(ModBlocks.REINFORCED_STONE_SLAB.get()), blockTexture(ModBlocks.REINFORCED_STONE.get()));

        blockWithItem(ModBlocks.CONCRETE_HAZARD);
        stairsBlock(((StairBlock) ModBlocks.CONCRETE_HAZARD_STAIRS.get()), blockTexture(ModBlocks.CONCRETE_HAZARD.get()));
        slabBlock(((SlabBlock) ModBlocks.CONCRETE_HAZARD_SLAB.get()), blockTexture(ModBlocks.CONCRETE_HAZARD_SLAB.get()), blockTexture(ModBlocks.BRICK_CONCRETE.get()));

        blockWithItem(ModBlocks.BRICK_CONCRETE);
        stairsBlock(((StairBlock) ModBlocks.BRICK_CONCRETE.get()), blockTexture(ModBlocks.BRICK_CONCRETE.get()));
        slabBlock(((SlabBlock) ModBlocks.BRICK_CONCRETE_SLAB.get()), blockTexture(ModBlocks.BRICK_CONCRETE_SLAB.get()), blockTexture(ModBlocks.BRICK_CONCRETE.get()));

        blockWithItem(ModBlocks.BRICK_CONCRETE_BROKEN);
        stairsBlock(((StairBlock) ModBlocks.BRICK_CONCRETE_BROKEN.get()), blockTexture(ModBlocks.BRICK_CONCRETE_BROKEN.get()));
        slabBlock(((SlabBlock) ModBlocks.BRICK_CONCRETE_BROKEN_SLAB.get()), blockTexture(ModBlocks.BRICK_CONCRETE_BROKEN_SLAB.get()), blockTexture(ModBlocks.BRICK_CONCRETE_BROKEN.get()));

        blockWithItem(ModBlocks.BRICK_CONCRETE_CRACKED);
        stairsBlock(((StairBlock) ModBlocks.BRICK_CONCRETE_CRACKED.get()), blockTexture(ModBlocks.BRICK_CONCRETE_CRACKED.get()));
        slabBlock(((SlabBlock) ModBlocks.BRICK_CONCRETE_CRACKED_SLAB.get()), blockTexture(ModBlocks.BRICK_CONCRETE_CRACKED_SLAB.get()), blockTexture(ModBlocks.BRICK_CONCRETE_CRACKED.get()));

        blockWithItem(ModBlocks.BRICK_CONCRETE_MOSSY);
        stairsBlock(((StairBlock) ModBlocks.BRICK_CONCRETE_MOSSY.get()), blockTexture(ModBlocks.BRICK_CONCRETE_MOSSY.get()));
        slabBlock(((SlabBlock) ModBlocks.BRICK_CONCRETE_MOSSY_SLAB.get()), blockTexture(ModBlocks.BRICK_CONCRETE_MOSSY_SLAB.get()), blockTexture(ModBlocks.BRICK_CONCRETE_MOSSY.get()));






        simpleBlockWithItem(ModBlocks.SHREDDER.get(),
                new ModelFile.UncheckedModelFile(modLoc("block/shredder")));

    }

    private void blockItem(RegistryObject<Block> blockRegistryObject) {
        simpleBlockItem(blockRegistryObject.get(), new ModelFile.UncheckedModelFile(MainRegistry.MOD_ID +
                ":block/" + ForgeRegistries.BLOCKS.getKey(blockRegistryObject.get()).getPath()));
    }


    private void blockWithItem(RegistryObject<Block> blockRegistryObject) {
        simpleBlockWithItem(blockRegistryObject.get(), cubeAll(blockRegistryObject.get()));
    }
}
