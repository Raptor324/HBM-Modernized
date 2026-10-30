package com.hbm_m.client.weapon;

import java.util.HashMap;
import java.util.Map;

import com.hbm_m.client.handler.GunLivingRenderTracker;
import com.hbm_m.client.weapon.ItemRenderWeaponBase.ItemRenderType;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Bruecke von 1.20-Itemdarstellung zu den 1.7.10-{@link ItemRenderWeaponBase}-Renderern (Original
 * {@code MinecraftForgeClient.registerItemRenderer} in {@code GunFactoryClient}). Die Item-Modelle sind
 * {@code builtin/entity} mit Einheits-Anzeigetransformationen; die Original-Transformationen (Hand, Inventar,
 * Boden) werden hier nachgestellt. Erstperson zeichnet {@link GunClientHooks} ueber das RenderHand-Ereignis.
 */
public class GunItemRenderer extends BlockEntityWithoutLevelRenderer {

    public static final GunItemRenderer INSTANCE = new GunItemRenderer();

    /** Original {@code MinecraftForgeClient.registerItemRenderer(item, renderer)}. */
    public static final Map<Item, ItemRenderWeaponBase> RENDERERS = new HashMap<>();

    public static void register(Item item, ItemRenderWeaponBase renderer) {
        RENDERERS.put(item, renderer);
    }

    public static ItemRenderWeaponBase get(ItemStack stack) {
        return stack.isEmpty() ? null : RENDERERS.get(stack.getItem());
    }

    private GunItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext ctx, PoseStack ps, MultiBufferSource buffers, int light, int overlay) {
        ItemRenderWeaponBase renderer = get(stack);
        if (renderer == null) return;
        boolean firstPerson = ctx == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND || ctx == ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
        if (firstPerson && renderer.customFirstPerson()) return;

        ps.pushPose();
        try {
            ps.translate(0.5, 0.5, 0.5); // ItemRenderer verschiebt vor dem BEWLR um -0.5

            GunGL.begin(ps, buffers, light, overlay);
            ItemRenderWeaponBase.interp = com.hbm_m.platform.RenderHooks.getPartialTickRaw();

            switch (ctx) {
                case GUI -> {
                    // 1.7.10-Inventar: Pixelraum 0..16 mit Y nach unten
                    ps.translate(-0.5, 0.5, 0);
                    com.hbm_m.client.render.item.OrigInventoryTransform.scaleSafe(ps, 1F / 16F, -1F / 16F, 1F / 16F); // vJ: Normalen
                    renderer.renderItem(ItemRenderType.INVENTORY, stack);
                }
                case THIRD_PERSON_RIGHT_HAND, THIRD_PERSON_LEFT_HAND -> {
                    // zurueck in den 1.7.10-Armraum: ItemInHandLayer dreht -90 X, 180 Y und verschiebt (1/16, 1/8, -10/16)
                    boolean left = ctx == ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
                    ps.translate((left ? 1 : -1) / 16F, -0.125F, 0.625F);
                    ps.mulPose(Axis.YP.rotationDegrees(-180));
                    ps.mulPose(Axis.XP.rotationDegrees(90));
                    ps.translate(-0.0625F, 0.4375F, 0.0625F);
                    if (renderer.isFull3D()) {
                        // RenderBiped.renderEquippedItems (1.7.10), isFull3D-Zweig (z.B. RedstoneSword)
                        ps.translate(0F, 0.1875F, 0F);
                        ps.scale(0.625F, -0.625F, 0.625F);
                        ps.mulPose(Axis.XP.rotationDegrees(-100));
                        ps.mulPose(Axis.YP.rotationDegrees(45));
                    } else {
                        // RenderBiped.renderEquippedItems (1.7.10), flacher Zweig
                        ps.translate(0.25F, 0.1875F, -0.1875F);
                        ps.scale(0.375F, 0.375F, 0.375F);
                        ps.mulPose(Axis.ZP.rotationDegrees(60));
                        ps.mulPose(Axis.XP.rotationDegrees(-90));
                        ps.mulPose(Axis.ZP.rotationDegrees(20));
                    }
                    // Forge 1.7.10 ForgeHooksClient.renderEquippedItem-Basismatrix (beide Haende, DEF-04)
                    ps.translate(0F, -0.3F, 0F);
                    ps.scale(1.5F, 1.5F, 1.5F);
                    ps.mulPose(Axis.YP.rotationDegrees(50));
                    ps.mulPose(Axis.ZP.rotationDegrees(335));
                    ps.translate(-0.9375F, -0.0625F, 0F);
                    // Halter der dargestellten Waffe (BEWLR hat keinen Entity-Parameter), sonst LocalPlayer
                    LivingEntity holder = GunLivingRenderTracker.current();
                    if (holder == null) holder = Minecraft.getInstance().player;
                    boolean isAkimbo = renderer.isAkimbo(holder);
                    if (left && isAkimbo) {
                        GunGL.pushMatrix();
                        renderer.setupThirdPersonAkimbo(stack);
                        renderer.renderEquippedAkimbo(stack, holder);
                        GunGL.popMatrix();
                    } else {
                        renderer.renderItem(ItemRenderType.EQUIPPED, stack, null, holder);
                    }
                }
                case FIRST_PERSON_RIGHT_HAND, FIRST_PERSON_LEFT_HAND -> {
                    // 1.7.10 ItemRenderer.renderItemInFirstPerson fuer IItemRenderer ohne Render-Helfer:
                    // Rest der Basismatrix (translate 0.56/-0.52/-0.72 + Equip-Progress macht die Vanilla-Hand),
                    // hier bleiben rotate 45 Y und scale 0.4 aus 1.7.10 uebrig (1.20 macht beides nicht)
                    ps.mulPose(Axis.YP.rotationDegrees(45));
                    ps.scale(0.4F, 0.4F, 0.4F);
                    renderer.renderItem(ItemRenderType.EQUIPPED_FIRST_PERSON, stack, null, Minecraft.getInstance().player);
                }
                case GROUND, FIXED, HEAD, NONE -> {
                    ps.scale(0.5F, 0.5F, 0.5F);
                    renderer.renderItem(ItemRenderType.ENTITY, stack);
                }
                default -> { }
            }
        } finally {
            GunGL.end();
            ps.popPose();
        }
    }
}
