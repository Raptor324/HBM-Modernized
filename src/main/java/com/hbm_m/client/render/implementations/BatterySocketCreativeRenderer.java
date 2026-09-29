package com.hbm_m.client.render.implementations;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import com.hbm_m.block.machines.MachineBatterySocketBlock;
import com.hbm_m.blockentity.machines.BatterySocketBlockEntity;
import com.hbm_m.client.model.BatteryPackBakedModel;
import com.hbm_m.client.render.RenderDistanceHelper;
import com.hbm_m.item.fekal_electric.ItemBatteryPack;
import com.hbm_m.item.fekal_electric.ItemCreativeBattery;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.platform.PlatformHooks;
import com.hbm_m.platform.RenderHooks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

//? if < 1.21.1 {
@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
//?} else {
/*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
*///?}

/**
 * World renderer for the battery socket (parity with the original BER): the body of an inserted
 * battery pack is drawn from the tier's item model (the OBJ Battery/Capacitor parts with their
 * native texture), the creative battery is a spinning figure with lightning bolts, other
 * energy items are a slowly rotating item on an "arm". The body is not in the chunk mesh,
 * so it appears right after insertion without a section rebuild.
 */
public class BatterySocketCreativeRenderer implements com.hbm_m.client.render.HbmBerBounds<BatterySocketBlockEntity> {

    private static final ResourceLocation MOD_SKIN =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/entity/battery_socket/creative_avatar.png");

    private static final ResourceLocation STEVE =
            ResourceLocation.withDefaultNamespace("textures/entity/player/wide/steve.png");

    /** Per-tier body quad cache: Socket-model parts remapped onto the tier's sprite. */
    private static final Map<ResourceLocation, List<net.minecraft.client.renderer.block.model.BakedQuad>> BODY_QUADS =
            new ConcurrentHashMap<>();

    private static final float ITEM_DEGREES_PER_TICK = 2.5f;

    private final PlayerModel<?> playerModel;

    public BatterySocketCreativeRenderer(BlockEntityRendererProvider.Context ctx) {
        this.playerModel = new PlayerModel<>(ctx.getModelSet().bakeLayer(ModelLayers.PLAYER), false);
    }

    @Override
    public void render(BatterySocketBlockEntity be, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        ItemStack stack = be.getItemHandler().getStackInSlot(0);
        if (stack.isEmpty()) return;

        Level level = be.getLevel();
        if (level == null) return;

        if (stack.getItem() instanceof ItemCreativeBattery) {
            renderFigure(level, be.getBlockPos(), partialTicks, poseStack, buffer);
            return;
        }

        poseStack.pushPose();
        if (stack.getItem() instanceof ItemBatteryPack pack) {
            renderPackBody(level, be.getBlockPos(), poseStack, buffer, packedLight, packedOverlay, stack, pack);
        } else {
            renderSpinningItem(level, partialTicks, poseStack, buffer, packedLight, packedOverlay, stack);
        }
        poseStack.popPose();
    }

    /** Pack body: Socket-model parts (Battery/Capacitor) with the tier's sprite, rotated by FACING. */
    private void renderPackBody(Level level, BlockPos pos, PoseStack poseStack, MultiBufferSource buffer,
            int packedLight, int packedOverlay, ItemStack stack, ItemBatteryPack pack) {
        List<net.minecraft.client.renderer.block.model.BakedQuad> quads = bodyQuads(stack, pack);
        if (quads.isEmpty()) return;

        int rotationY = rotationYForFacing(level, pos);
        poseStack.translate(0.5, 0, 0.5);
        // Rotation direction matches the baked model's transformQuadsByFacing.
        poseStack.mulPose(Axis.YN.rotationDegrees(rotationY));
        poseStack.translate(-0.5, 0, -0.5);

        VertexConsumer vc = buffer.getBuffer(RenderType.cutout());
        var pose = poseStack.last();
        RandomSource rand = RandomSource.create();
        for (net.minecraft.client.renderer.block.model.BakedQuad quad : quads) {
            RenderHooks.putBulkData(vc, pose, quad, 1f, 1f, 1f, 1f, packedLight, packedOverlay, true);
        }
    }

    private int rotationYForFacing(Level level, BlockPos pos) {
        if (level.getBlockState(pos).hasProperty(MachineBatterySocketBlock.FACING)) {
            return switch (level.getBlockState(pos).getValue(MachineBatterySocketBlock.FACING)) {
                case SOUTH -> 180;
                case WEST -> 270;
                case EAST -> 90;
                default -> 0;
            };
        }
        return 0;
    }

    /**
     * Pack body quads: the socket's baked Socket model (the same geometry the chunk mesh
     * used to draw) with UVs remapped onto the tier's sprite from the block atlas (sprites
     * are stitched via the standard textures map in the packs' item JSONs). Cached per item id.
     */
    private static List<net.minecraft.client.renderer.block.model.BakedQuad> bodyQuads(ItemStack stack, ItemBatteryPack pack) {
        ResourceLocation itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (itemId == null) return List.of();
        return BODY_QUADS.computeIfAbsent(itemId, id -> {
            Minecraft mc = Minecraft.getInstance();
            BlockState socketState = com.hbm_m.block.ModBlocks.MACHINE_BATTERY_SOCKET.get().defaultBlockState();
            BakedModel socketModel = mc.getBlockRenderer().getBlockModel(socketState);
            if (!(socketModel instanceof com.hbm_m.client.model.MachineBatterySocketBakedModel multipart)) {
                com.hbm_m.main.MainRegistry.LOGGER.warn("[BatterySocket] Body model not found for {}", id);
                return List.of();
            }
            BakedModel part = multipart.getPartModels().get(pack.tier.isCapacitor() ? "Capacitor" : "Battery");
            if (part == null) return List.of();

            ResourceLocation texLoc = ResourceLocation.fromNamespaceAndPath(
                    RefStrings.MODID, "block/machine/" + pack.tier.tex);
            TextureAtlasSprite sprite = mc.getTextureAtlas(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS).apply(texLoc);

            RandomSource rand = RandomSource.create();
            List<net.minecraft.client.renderer.block.model.BakedQuad> out = new ArrayList<>();
            for (Direction d : Direction.values()) {
                remapInto(out, com.hbm_m.platform.RenderHooks.getPartQuads(part, null, d, rand), sprite);
            }
            remapInto(out, com.hbm_m.platform.RenderHooks.getPartQuads(part, null, null, rand), sprite);
            return out;
        });
    }

    private static void remapInto(List<net.minecraft.client.renderer.block.model.BakedQuad> out,
            List<net.minecraft.client.renderer.block.model.BakedQuad> source, TextureAtlasSprite sprite) {
        for (net.minecraft.client.renderer.block.model.BakedQuad quad : source) {
            out.add(retextureQuad(quad, sprite));
        }
    }

    /** Affine remap of a quad's UVs from its source sprite to the tier's sprite. */
    private static net.minecraft.client.renderer.block.model.BakedQuad retextureQuad(
            net.minecraft.client.renderer.block.model.BakedQuad original, TextureAtlasSprite sprite) {
        TextureAtlasSprite oldSprite = original.getSprite();
        if (oldSprite == null || oldSprite == sprite) return original;
        float oldUDiff = oldSprite.getU1() - oldSprite.getU0();
        float oldVDiff = oldSprite.getV1() - oldSprite.getV0();
        if (oldUDiff == 0 || oldVDiff == 0) return original;
        int[] oldData = original.getVertices();
        int[] newData = oldData.clone();
        int stride = oldData.length / 4;
        for (int i = 0; i < 4; i++) {
            int offset = i * stride;
            float u = Float.intBitsToFloat(oldData[offset + 4]);
            float v = Float.intBitsToFloat(oldData[offset + 5]);
            float normU = (u - oldSprite.getU0()) / oldUDiff;
            float normV = (v - oldSprite.getV0()) / oldVDiff;
            newData[offset + 4] = Float.floatToIntBits(sprite.getU0() + normU * (sprite.getU1() - sprite.getU0()));
            newData[offset + 5] = Float.floatToIntBits(sprite.getV0() + normV * (sprite.getV1() - sprite.getV0()));
        }
        return new net.minecraft.client.renderer.block.model.BakedQuad(
                newData, original.getTintIndex(), original.getDirection(), sprite, original.isShade());
    }

    /** Other energy item: slow rotation on the "arm" inside the socket. */
    private void renderSpinningItem(Level level, float partialTicks, PoseStack poseStack,
            MultiBufferSource buffer, int packedLight, int packedOverlay, ItemStack stack) {
        float spin = (level.getGameTime() % 3600) * ITEM_DEGREES_PER_TICK + partialTicks * ITEM_DEGREES_PER_TICK;
        poseStack.translate(0.5, 0.85, 0.5);
        poseStack.mulPose(Axis.YN.rotationDegrees(spin));
        poseStack.scale(0.6f, 0.6f, 0.6f);
        Minecraft.getInstance().getItemRenderer().renderStatic(
                stack, ItemDisplayContext.GROUND, packedLight, packedOverlay,
                poseStack, buffer, level, (int) level.getGameTime());
    }

    private void renderFigure(Level level, BlockPos pos, float partialTicks, PoseStack poseStack, MultiBufferSource buffer) {
        poseStack.pushPose();
        poseStack.translate(0.5, 0.15, 0.5);
        float spin = (level.getGameTime() + partialTicks) * 25f;
        poseStack.mulPose(Axis.YP.rotationDegrees(spin));
        poseStack.scale(0.45f, 0.45f, 0.45f);
        poseStack.translate(0, 1.65, 0);

        ResourceLocation skin = MOD_SKIN;
        if (Minecraft.getInstance().getResourceManager().getResource(skin).isEmpty()) {
            skin = STEVE;
        }

        int light = LevelRenderer.getLightColor(level, pos.above());
        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(skin));
        this.playerModel.young = false;
        this.playerModel.setAllVisible(true);
        RenderHooks.renderModelToBuffer(this.playerModel, poseStack, vc, light, OverlayTexture.NO_OVERLAY);

        renderJaggedBolts(level, poseStack, buffer, pos);

        poseStack.popPose();
    }

    /** Jagged polylines toward corners (client-only decoration). */
    private static void renderJaggedBolts(Level level, PoseStack poseStack, MultiBufferSource buffer, BlockPos pos) {
        Random rand = new Random(level.getGameTime() / 5 + pos.asLong());
        VertexConsumer lines = buffer.getBuffer(RenderType.lines());
        float w = 0.4375f;
        for (int i = -1; i <= 1; i += 2) {
            for (int j = -1; j <= 1; j += 2) {
                if (rand.nextInt(4) != 0) continue;
                poseStack.pushPose();
                poseStack.translate(0, 0.75, 0);
                drawPolyline(poseStack, lines, w * i, 1.1875f, w * j, rand.nextInt(4096));
                poseStack.popPose();
            }
        }
    }

    private static void drawPolyline(PoseStack poseStack, VertexConsumer lines, float tx, float ty, float tz, int seed) {
        Random r = new Random(seed);
        float ox = 0f, oy = 0.5f, oz = 0f;
        int segments = 10;
        for (int s = 0; s < segments; s++) {
            float t = (s + 1) / (float) segments;
            float nx = tx * t + (r.nextFloat() - 0.5f) * 0.1f;
            float ny = oy + ty * t + (r.nextFloat() - 0.5f) * 0.08f;
            float nz = tz * t + (r.nextFloat() - 0.5f) * 0.1f;
            double minX = Math.min(ox, nx) - 0.012;
            double minY = Math.min(oy, ny) - 0.012;
            double minZ = Math.min(oz, nz) - 0.012;
            double maxX = Math.max(ox, nx) + 0.012;
            double maxY = Math.max(oy, ny) + 0.012;
            double maxZ = Math.max(oz, nz) + 0.012;
            LevelRenderer.renderLineBox(poseStack, lines, minX, minY, minZ, maxX, maxY, maxZ, 0.35f, 0.35f, 0.95f, 0.9f);
            ox = nx;
            oy = ny;
            oz = nz;
        }
    }

    @Override
    public boolean shouldRenderOffScreen(BatterySocketBlockEntity be) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return RenderDistanceHelper.getStaticViewDistanceBlocks();
    }
}
