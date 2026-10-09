// neo-pendant: keins - reines Entwickler-Werkzeug fuer den 1.20.1-Forge-Dev-Client
//? if forge {
package com.hbm_m.client.dev;

import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.joml.Matrix4f;

import com.hbm_m.lib.RefStrings;
import com.hbm_m.main.MainRegistry;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexSorting;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Entwickler-Werkzeug (nur mit {@code -Dhbm.iconExport=true}, sonst ohne jede Wirkung): zeichnet nach dem Laden im
 * Hauptmenue fuer jedes Item des Mods (alle Creative-Tabs inkl. NBT-Varianten, dazu Items ohne Tab) das GUI-Icon
 * genau wie im Inventar ({@code GuiGraphics.renderItem} + {@code renderItemDecorations}) in einen Offscreen-Puffer:
 * 64x64 GUI-Einheiten, Slot 16x16 mittig (24..40), Skalierung 4 -> 256x256-PNG. Ein zweites Bild folgt fruehestens
 * 0,5 s spaeter (Animationspruefung). Danach beendet sich das Spiel.
 *
 * <p>Optionen: {@code -Dhbm.iconExportDir=<pfad>} (Standard {@code icon_export} im Laufverzeichnis),
 * {@code -Dhbm.iconExportFilter=<regex auf die Item-ID>}. Ausgabe: {@code <idx>_<id>[_v<n>]_f1.png}/{@code _f2.png}
 * und {@code manifest.tsv}.</p>
 */
@Mod.EventBusSubscriber(modid = RefStrings.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class IconExportHook {

    private static final boolean ENABLED = Boolean.getBoolean("hbm.iconExport")
            || "true".equalsIgnoreCase(System.getenv("HBM_ICON_EXPORT"));
    private static final int CANVAS = 64;
    private static final int SCALE = 4;
    private static final int PX = CANVAS * SCALE;
    private static final int SLOT = 24;
    private static final int PER_FRAME = 40;

    private record Entry(int idx, String id, int variant, ItemStack stack, String tab, String name) {}

    private static int phase;          // 0 warten, 1 Bild 1, 2 Pause, 3 Bild 2, 4 fertig
    private static int titleTicks;
    private static int cursor;
    private static long pass1Start;
    private static List<Entry> entries;
    private static Map<Integer, String[]> info;
    private static Path outDir;
    private static TextureTarget target;

    private IconExportHook() {}

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (!ENABLED || event.phase != TickEvent.Phase.END || phase >= 4) return;
        Minecraft mc = Minecraft.getInstance();
        try {
            switch (phase) {
                case 0 -> {
                    if (mc.getOverlay() == null && mc.screen instanceof TitleScreen && ++titleTicks > 20) {
                        collect(mc);
                        pass1Start = System.currentTimeMillis();
                        phase = 1;
                    }
                }
                case 1 -> {
                    if (renderBatch(mc, 1)) { cursor = 0; phase = 2; }
                }
                case 2 -> {
                    if (System.currentTimeMillis() - pass1Start >= 500) phase = 3;
                }
                case 3 -> {
                    if (renderBatch(mc, 2)) {
                        writeManifest();
                        phase = 4;
                        MainRegistry.LOGGER.info("[HBM IconExport] fertig: {} Icons nach {}", entries.size(), outDir);
                        mc.stop();
                    }
                }
                default -> { }
            }
        } catch (Throwable t) {
            MainRegistry.LOGGER.error("[HBM IconExport] abgebrochen", t);
            phase = 4;
            mc.stop();
        }
    }

    private static void collect(Minecraft mc) throws Exception {
        outDir = Paths.get(System.getProperty("hbm.iconExportDir", "icon_export")).toAbsolutePath();
        Files.createDirectories(outDir);
        String f = System.getProperty("hbm.iconExportFilter");
        Pattern filter = f == null || f.isEmpty() ? null : Pattern.compile(f);

        CreativeModeTab.ItemDisplayParameters params = new CreativeModeTab.ItemDisplayParameters(
                FeatureFlags.REGISTRY.allFlags(), true, VanillaRegistries.createLookup());
        Map<String, Entry> seen = new LinkedHashMap<>();
        Map<String, Integer> variants = new LinkedHashMap<>();
        List<Entry> list = new ArrayList<>();
        for (CreativeModeTab tab : BuiltInRegistries.CREATIVE_MODE_TAB) {
            ResourceLocation tabId = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab);
            try {
                tab.buildContents(params);
            } catch (Throwable t) {
                MainRegistry.LOGGER.error("[HBM IconExport] Tab {} nicht aufbaubar", tabId, t);
                continue;
            }
            for (ItemStack s : tab.getDisplayItems()) {
                add(s, String.valueOf(tabId), filter, seen, variants, list);
            }
        }
        for (Item item : BuiltInRegistries.ITEM) {
            add(new ItemStack(item), "-", filter, seen, variants, list);
        }
        entries = list;
        info = new LinkedHashMap<>();
        target = new TextureTarget(PX, PX, true, Minecraft.ON_OSX);
        target.setClearColor(0F, 0F, 0F, 0F);
        MainRegistry.LOGGER.info("[HBM IconExport] {} Icons -> {}", list.size(), outDir);
    }

    private static void add(ItemStack s, String tab, Pattern filter, Map<String, Entry> seen,
                            Map<String, Integer> variants, List<Entry> list) {
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(s.getItem());
        // Vanilla-Kontrollen fuer die Animationspruefung (animierte Texturen: Seelaterne, Magmablock)
        boolean control = key.equals(ResourceLocation.withDefaultNamespace("sea_lantern")) || key.equals(ResourceLocation.withDefaultNamespace("magma_block"));
        if (!RefStrings.MODID.equals(key.getNamespace()) && !control) return;
        String id = key.getPath();
        if (filter != null && !filter.matcher(id).find()) return;
        String dedupe = id + "|" + (s.getTag() == null ? "" : s.getTag().toString());
        if (tab.equals("-") ? variants.containsKey(id) : seen.containsKey(dedupe)) return;
        int v = variants.merge(id, 1, Integer::sum) - 1;
        String name = String.format("%05d_%s%s", list.size(), id, v == 0 ? "" : "_v" + v);
        Entry e = new Entry(list.size(), id, v, s.copy(), tab, name);
        seen.put(dedupe, e);
        list.add(e);
    }

    /** @return true, wenn der Durchgang fertig ist */
    private static boolean renderBatch(Minecraft mc, int frame) throws Exception {
        int end = Math.min(entries.size(), cursor + PER_FRAME);
        Matrix4f oldProj = new Matrix4f(RenderSystem.getProjectionMatrix());
        VertexSorting oldSort = RenderSystem.getVertexSorting();
        PoseStack mv = RenderSystem.getModelViewStack();
        mv.pushPose();
        try {
            RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0F, CANVAS, CANVAS, 0F, 1000F, 21000F), VertexSorting.ORTHOGRAPHIC_Z);
            for (; cursor < end; cursor++) {
                Entry e = entries.get(cursor);
                String err = renderOne(mc, e, frame);
                String[] row = info.computeIfAbsent(e.idx, k -> new String[] {"", "", "", "", "", "", ""});
                if (frame == 1) describe(mc, e, row);
                row[frame == 1 ? 5 : 6] = err;
            }
        } finally {
            mv.popPose();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.setProjectionMatrix(oldProj, oldSort);
            mc.getMainRenderTarget().bindWrite(true);
        }
        return cursor >= entries.size();
    }

    private static String renderOne(Minecraft mc, Entry e, int frame) {
        String err = "";
        target.setClearColor(0F, 0F, 0F, 0F);
        target.clear(Minecraft.ON_OSX);
        target.bindWrite(true);
        PoseStack mv = RenderSystem.getModelViewStack();
        mv.setIdentity();
        mv.translate(0F, 0F, -11000F);
        RenderSystem.applyModelViewMatrix();
        Lighting.setupFor3DItems();
        RenderSystem.enableDepthTest();
        try {
            GuiGraphics g = new GuiGraphics(mc, mc.renderBuffers().bufferSource());
            g.renderItem(e.stack, SLOT, SLOT);
            g.renderItemDecorations(mc.font, e.stack, SLOT, SLOT);
            g.flush();
        } catch (Throwable t) {
            Throwable c = t;
            while (c.getCause() != null && c.getCause() != c) c = c.getCause();
            err = (c.getClass().getSimpleName() + ": " + c.getMessage()).replace('\t', ' ').replace('\n', ' ');
            recover(mc);
        }
        try (NativeImage img = new NativeImage(PX, PX, false)) {
            RenderSystem.bindTexture(target.getColorTextureId());
            img.downloadTexture(0, false);
            img.flipY();
            img.writeToFile(outDir.resolve(e.name + "_f" + frame + ".png"));
        } catch (Throwable t) {
            err = err + " | PNG: " + t;
        }
        return err;
    }

    private static void recover(Minecraft mc) {
        try { mc.renderBuffers().bufferSource().endBatch(); } catch (Throwable ignored) { }
        try {
            BufferBuilder b = Tesselator.getInstance().getBuilder();
            if (b.building()) b.end().release();
            b.discard();
        } catch (Throwable ignored) { }
    }

    /** Modellklasse, Sonderrenderer, fehlendes Modell, fehlende Texturen. */
    private static void describe(Minecraft mc, Entry e, String[] row) {
        try {
            BakedModel m = mc.getItemRenderer().getModel(e.stack, null, null, 0);
            row[0] = m.getClass().getName();
            row[1] = String.valueOf(m.isCustomRenderer());
            row[2] = String.valueOf(m == mc.getModelManager().getMissingModel());
            int missing = 0, total = 0;
            if (!m.isCustomRenderer()) {
                RandomSource rnd = RandomSource.create(42L);
                List<BakedQuad> quads = new ArrayList<>(m.getQuads(null, null, rnd));
                for (Direction d : Direction.values()) quads.addAll(m.getQuads(null, d, rnd));
                for (BakedQuad q : quads) {
                    total++;
                    if (MissingTextureAtlasSprite.getLocation().equals(q.getSprite().contents().name())) missing++;
                }
                if (MissingTextureAtlasSprite.getLocation().equals(m.getParticleIcon().contents().name()) && total == 0) missing++;
            }
            row[3] = missing + "/" + total;
            row[4] = IClientItemExtensions.of(e.stack).getCustomRenderer().getClass().getName();
        } catch (Throwable t) {
            row[4] = "describe: " + t;
        }
    }

    private static void writeManifest() throws Exception {
        try (BufferedWriter w = Files.newBufferedWriter(outDir.resolve("manifest.tsv"), StandardCharsets.UTF_8)) {
            w.write("idx\tname\tid\tvariant\ttab\tnbt\tmodel\tcustom\tmissing_model\tmissing_tex\tbewlr\terr1\terr2\n");
            for (Entry e : entries) {
                String[] r = info.getOrDefault(e.idx, new String[7]);
                w.write(e.idx + "\t" + e.name + "\t" + e.id + "\t" + e.variant + "\t" + e.tab + "\t"
                        + (e.stack.getTag() == null ? "" : e.stack.getTag().toString().replace('\t', ' '))
                        + "\t" + r[0] + "\t" + r[1] + "\t" + r[2] + "\t" + r[3] + "\t" + r[4] + "\t" + r[5] + "\t" + r[6] + "\n");
            }
        }
    }
}
//?}
