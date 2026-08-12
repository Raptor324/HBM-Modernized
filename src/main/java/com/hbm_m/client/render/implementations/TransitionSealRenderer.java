package com.hbm_m.client.render.implementations;

import java.util.List;

import com.hbm_m.block.machines.TransitionSealBlock;
import com.hbm_m.blockentity.machines.TransitionSealBlockEntity;
import com.hbm_m.client.loader.dae.DaeAnimation;
import com.hbm_m.client.loader.dae.DaeModel;
import com.hbm_m.client.loader.dae.DaeNode;
import com.hbm_m.client.loader.dae.DaeQuadBaker;
import com.hbm_m.client.render.MeshRenderCache;
import com.hbm_m.client.render.SingleMeshVboRenderer;
import com.hbm_m.client.render.shader.IrisRenderBatch;
import com.hbm_m.client.render.shader.ShaderCompatibilityDetector;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.main.MainRegistry;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

/**
 * BER for the transition seal blast door multiblock. The 26 wide x 24 tall DAE scene
 * ({@code models/block/doors/transition_seal.dae}) is baked once per node into the
 * {@link SingleMeshVboRenderer block_lit} pipeline: node meshes are converted to
 * block-atlas {@link BakedQuad}s (triangle soup -> degenerate quads), which feed both
 * the VBO path and the Iris companion-mesh path. Per frame the 24 second "animation"
 * clip is sampled at {@code TransitionSealBlockEntity.getAnimationTime()} and each
 * node's animated local matrix is pushed onto the pose stack - the rest-pose geometry
 * is never re-uploaded. Mirrors the NEO {@code RenderTransitionSeal} / {@code
 * DaeModelRenderer} transform chain exactly (no Z_UP -> Y_UP conversion, the DAE node
 * matrices are authored in Minecraft space).
 */

//? if forge {
@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
//?} elif fabric {
/*@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
*///?} elif neoforge {
/*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
*///?}
public class TransitionSealRenderer implements BlockEntityRenderer<TransitionSealBlockEntity> {

    private static final String CLIP_NAME = "animation";
    private static final ResourceLocation MODEL_ID = RefStrings.resourceLocation("models/block/doors/transition_seal");
    private static final ResourceLocation SEAL_TEX = RefStrings.resourceLocation("block/doors/transition_seal");

    private static DaeModel model;
    private static boolean modelFailed;
    private static ResourceLocation resolvedModelFile;
    private static ResourceLocation resolvedTexture;

    public TransitionSealRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public boolean shouldRenderOffScreen(TransitionSealBlockEntity be) {
        return true;
    }

    @Override
    public void render(TransitionSealBlockEntity be, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        init();
        if (model == null) return;
        DaeAnimation clip = getClip();
        if (clip == null) return;

        Direction facing = be.getBlockState().getValue(TransitionSealBlock.FACING);

        poseStack.pushPose();
        poseStack.translate(0.5F, 0F, 0.5F);
        switch (facing) {
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(90F));
            case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(270F));
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(180F));
            default -> { }
        }
        poseStack.translate(0F, 0F, 0.5F);

        float time = be.getAnimationTime(partialTick);

        boolean iris = ShaderCompatibilityDetector.isExternalShaderActive();
        if (iris) {
            try (IrisRenderBatch batch = IrisRenderBatch.begin(
                    ShaderCompatibilityDetector.isRenderingShadowPass(), RenderSystem.getProjectionMatrix())) {
                renderNodes(model.sceneRoots, clip, time, poseStack, packedLight, be, bufferSource);
            }
        } else {
            renderNodes(model.sceneRoots, clip, time, poseStack, packedLight, be, bufferSource);
        }

        poseStack.popPose();
    }

    private static void renderNodes(List<DaeNode> nodes, DaeAnimation clip, float time,
                                    PoseStack poseStack, int packedLight,
                                    TransitionSealBlockEntity be, MultiBufferSource bufferSource) {
        for (DaeNode node : nodes) {
            poseStack.pushPose();
            //? if < 1.21.1 {
            poseStack.mulPoseMatrix(node.localMatrix(time, clip));
            //?} else {
            /*poseStack.mulPose(node.localMatrix(time, clip));
            *///?}
            if (node.mesh != null) {
                SingleMeshVboRenderer renderer = getRendererForNode(node);
                if (renderer != null) {
                    renderer.render(poseStack, packedLight, be.getBlockPos(), be, bufferSource);
                }
            }
            renderNodes(node.children, clip, time, poseStack, packedLight, be, bufferSource);
            poseStack.popPose();
        }
    }

    private static SingleMeshVboRenderer getRendererForNode(DaeNode node) {
        String key = "transition_seal:" + node.name;
        try {
            ResourceLocation texture = node.texture != null ? node.texture : SEAL_TEX;
            if (texture != null) {
                resolvedTexture = texture;
            }
            TextureAtlasSprite sprite = Minecraft.getInstance().getModelManager()
                    .getAtlas(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS)
                    .getSprite(texture);
            List<BakedQuad> quads = bakeNodeQuads(node, sprite);
            if (quads.isEmpty()) {
                return null;
            }
            return MeshRenderCache.getOrCreateRendererFromQuadList(key, quads);
        } catch (Exception e) {
            MainRegistry.LOGGER.error("TransitionSealRenderer: failed to bake node '{}'", node.name, e);
            return null;
        }
    }

    /**
     * Bakes the node's DAE mesh into block-atlas quads. The mesh stays in local space —
     * the per-frame animated pose is applied by the render loop's pose stack, exactly
     * like the NEO {@code RenderTransitionSeal} transform chain.
     */
    private static List<BakedQuad> bakeNodeQuads(DaeNode node, TextureAtlasSprite sprite) {
        return DaeQuadBaker.bakeNodeQuads(node.mesh, new org.joml.Matrix4f(), sprite);
    }

    private static void init() {
        if (model == null && !modelFailed) {
            try {
                model = DaeModel.load(MODEL_ID);
                resolvedModelFile = ResourceLocation.fromNamespaceAndPath(model.resource.getNamespace(), model.resource.getPath() + ".dae");
                resolvedTexture = findFirstTexture(model.sceneRoots);
                if (resolvedTexture == null && !model.textures.isEmpty()) {
                    resolvedTexture = model.textures.values().iterator().next();
                }
                if (resolvedTexture == null) {
                    resolvedTexture = SEAL_TEX;
                }
            } catch (Exception e) {
                modelFailed = true;
                MainRegistry.LOGGER.error("TransitionSealRenderer: failed to load DAE model", e);
            }
        }
    }

    public static String getDebugInfo() {
        init();
        StringBuilder sb = new StringBuilder("Transition seal debug\n");
        sb.append("Model file: ").append(resolvedModelFile != null ? resolvedModelFile : "<not loaded>");
        sb.append("\nTexture: ").append(resolvedTexture != null ? resolvedTexture : "<not loaded>");
        if (Minecraft.getInstance() != null && resolvedTexture != null) {
            try {
                TextureAtlasSprite sprite = Minecraft.getInstance().getModelManager()
                        .getAtlas(TextureAtlas.LOCATION_BLOCKS)
                        .getSprite(resolvedTexture);
                sb.append("\nSprite present: ").append(sprite != null);
                if (sprite != null) {
                    var contents = sprite.contents();
                    sb.append("\nSprite size: ").append(contents.width()).append("x").append(contents.height());
                    sb.append("\nSprite UV: ").append(sprite.getU0()).append("..").append(sprite.getU1())
                            .append(" / ").append(sprite.getV0()).append("..").append(sprite.getV1());
                }
            } catch (Exception e) {
                sb.append("\nSprite lookup failed: ").append(e.getClass().getSimpleName()).append(": ").append(e.getMessage());
            }
        }
        if (model != null) {
            sb.append("\nDAE resource: ").append(model.resource);
        }
        if (modelFailed) {
            sb.append("\nStatus: load failed");
        }
        return sb.toString();
    }

    private static ResourceLocation findFirstTexture(List<DaeNode> nodes) {
        for (DaeNode node : nodes) {
            if (node.texture != null) {
                return node.texture;
            }
            ResourceLocation found = findFirstTexture(node.children);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private static DaeAnimation getClip() {
        DaeAnimation clip = model.animations.get(CLIP_NAME);
        if (clip == null && !model.animations.isEmpty()) {
            clip = model.animations.values().iterator().next();
        }
        return clip;
    }
}
