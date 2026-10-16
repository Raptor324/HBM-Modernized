// package com.hbm_m.client.render.implementations;

// import com.hbm_m.blockentity.machines.TestBlockEntity;
// import com.hbm_m.client.model.TestBakedModel;
// import com.hbm_m.client.render.AbstractPartBasedRenderer;
// import com.hbm_m.client.render.MeshRenderCache;
// import com.hbm_m.client.render.SingleMeshVboRenderer;
// import com.mojang.blaze3d.vertex.PoseStack;

// import net.minecraft.client.Minecraft;
// import net.minecraft.client.renderer.MultiBufferSource;
// import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
// import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
// import net.minecraft.client.resources.model.BakedModel;
// import com.hbm_m.main.MainRegistry;

// //? if forge {
// @net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
// //?} elif fabric {
// /*@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
// *///?} elif neoforge {
// /*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
// *///?}
// public class TestBlockRenderer implements BlockEntityRenderer<TestBlockEntity> {

//     private static final String CACHE_KEY = "test_block:" + TestBakedModel.PART_DEFAULT;

//     public TestBlockRenderer(BlockEntityRendererProvider.Context ctx) {}

//     @Override
//     public void render(TestBlockEntity blockEntity, float partialTick, PoseStack poseStack,
//                        MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
//         BakedModel raw = Minecraft.getInstance().getBlockRenderer()
//             .getBlockModel(blockEntity.getBlockState());
//         raw = AbstractPartBasedRenderer.unwrapFabricForwardingModels(raw);
//         if (!(raw instanceof TestBakedModel model)) {
//             return;
//         }

//         BakedModel part = model.getPart(TestBakedModel.PART_DEFAULT);
//         if (part == null) {
//             MainRegistry.LOGGER.error("ЧАСТЬ 'default' НЕ НАЙДЕНА В МОДЕЛИ! Доступные части: {}", String.join(", ", model.getPartNames()));
//             return;
//         }

//         // Статический меш: компилируется один раз, кэшируется в MeshRenderCache (LRU),
//         // при reload/disconnect чистится вместе со всеми остальными через clearAll().
//         SingleMeshVboRenderer renderer = MeshRenderCache.getOrCreateRenderer(CACHE_KEY, part);
//         if (renderer != null) {
//             renderer.render(poseStack, packedLight, blockEntity.getBlockPos(), blockEntity, bufferSource);
//         }
//     }
// }
