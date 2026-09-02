// package com.hbm_m.client.model;

// import java.util.List;
// import java.util.Map;

// import net.minecraft.client.renderer.block.model.BakedQuad;
// import net.minecraft.client.renderer.block.model.ItemOverrides;
// import net.minecraft.client.renderer.block.model.ItemTransforms;
// import net.minecraft.client.resources.model.BakedModel;
// import net.minecraft.core.Direction;
// import net.minecraft.resources.ResourceLocation;
// import net.minecraft.util.RandomSource;
// import net.minecraft.world.level.block.state.BlockState;
// import org.jetbrains.annotations.Nullable;

// /**
//  * Тестовая модель (~2.5 млн полигонов): ВСЯ геометрия живёт в VBO (BER + {@link com.hbm_m.client.render.MeshRenderCache}).
//  * Квады не отдаются ни для item, ни для чанк-меша — иначе клиент захлебнётся.
//  */
// public class TestBakedModel extends AbstractMultipartBakedModel implements AbstractMultipartBakedModel.PartNamesProvider {

//     /** Единственная часть: OBJ содержит только группы {@code g default}, без {@code o}-объектов. */
//     public static final String PART_DEFAULT = "default";

//     private final String[] partNames;
//     private final ResourceLocation modelId;

//     public TestBakedModel(Map<String, BakedModel> parts, ItemTransforms transforms, ResourceLocation modelId) {
//         super(parts, transforms);
//         this.partNames = parts.keySet().toArray(new String[0]);
//         this.modelId = modelId;
//     }

//     public ResourceLocation getModelId() {
//         return modelId;
//     }

//     @Override
//     public String[] getPartNames() {
//         return partNames;
//     }

//     @Override
//     protected boolean shouldSkipWorldRendering(@Nullable BlockState state) {
//         return true;
//     }

//     @Override
//     public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
//         return List.of();
//     }

//     //? if forge {
//     @Override
//     public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand,
//                                     net.minecraftforge.client.model.data.ModelData data,
//                                     @Nullable net.minecraft.client.renderer.RenderType renderType) {
//         return List.of();
//     }
//     //?}

//     @Override
//     public ItemOverrides getOverrides() {
//         return ItemOverrides.EMPTY;
//     }
// }
