// neo-pendant: keins - reines Entwickler-Werkzeug fuer den 1.20.1-Forge-Dev-Client
//? if forge {
package com.hbm_m.client.dev;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

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
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Entwickler-Werkzeug (nur mit {@code -Dhbm.iconExport=true}, sonst ohne jede Wirkung): zeichnet fuer jedes Item des
 * Mods (alle Creative-Tabs inkl. NBT-Varianten, dazu Items ohne Tab) das GUI-Icon genau wie im Inventar
 * ({@code GuiGraphics.renderItem} + {@code renderItemDecorations}) in einen Offscreen-Puffer: 64x64 GUI-Einheiten,
 * Slot 16x16 mittig (24..40), Skalierung 4 -> 256x256-PNG. Ein zweites Bild folgt fruehestens 0,5 s spaeter
 * (Animationspruefung), ein drittes ({@code _bg}) mit undurchsichtigem Inventar-Hintergrund bei z=0
 * (zeigt Icons, die im echten Inventar hinter dem GUI verschwinden). Danach beendet sich das Spiel.
 *
 * <p>Optionen: {@code -Dhbm.iconExportDir=<pfad>} (Standard {@code icon_export} im Laufverzeichnis),
 * {@code -Dhbm.iconExportFilter=<regex auf die Item-ID>}, {@code -Dhbm.iconExportWorld=true} (vorher automatisch eine
 * Superflat-Kreativwelt anlegen und betreten, Export erst in der geladenen Welt - viele Item-Renderer greifen auf
 * Level/Spieler zu). Ausgabe: {@code <idx>_<id>[_v<n>]_f1.png}/{@code _f2.png}/{@code _bg.png}, {@code manifest.tsv}
 * (inkl. GL-Zustandslecks je Item) und {@code log_errors.tsv} (alle WARN/ERROR-Logzeilen und STDERR, mit dem gerade
 * gezeichneten Item bzw. {@code <laden>}).</p>
 */
@Mod.EventBusSubscriber(modid = RefStrings.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class IconExportHook {

    private static final boolean ENABLED = Boolean.getBoolean("hbm.iconExport")
            || "true".equalsIgnoreCase(System.getenv("HBM_ICON_EXPORT"));
    private static final boolean WORLD = Boolean.getBoolean("hbm.iconExportWorld")
            || "true".equalsIgnoreCase(System.getenv("HBM_ICON_EXPORT_WORLD"));
    private static final int CANVAS = 64;
    private static final int SCALE = 4;
    private static final int PX = CANVAS * SCALE;
    private static final int SLOT = 24;
    private static final int PER_FRAME = 40;
    private static final int BG = 0xFF8B8B8B;

    private record Entry(int idx, String id, int variant, ItemStack stack, String tab, String name) {}

    private static int phase;          // 0 warten, 1 Bild 1, 2 Pause, 3 Bild 2, 4 fertig; 10 Welt laedt
    private static int titleTicks;
    private static int worldTicks;
    private static int cursor;
    private static long pass1Start;
    private static List<Entry> entries;
    private static Map<Integer, String[]> info;
    private static Path outDir;
    private static TextureTarget target;

    /** gerade gezeichnetes Item fuer die Log-Zuordnung */
    private static volatile String current = "<laden>";
    private static final List<String> LOG_LINES = new ArrayList<>();

    static {
        if (ENABLED) installAppender();
    }

    private IconExportHook() {}

    private static void installAppender() {
        try {
            LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
            AbstractAppender app = new AbstractAppender("HbmIconExport", null, null, true) {
                @Override
                public void append(LogEvent e) {
                    String logger = String.valueOf(e.getLoggerName());
                    boolean err = e.getLevel().isMoreSpecificThan(Level.WARN);
                    if (!err && !logger.contains("STDERR")) return;
                    String msg = e.getMessage() == null ? "" : e.getMessage().getFormattedMessage();
                    Throwable t = e.getThrown();
                    String thr = "";
                    if (t != null) {
                        Throwable c = t;
                        while (c.getCause() != null && c.getCause() != c) c = c.getCause();
                        StringBuilder sb = new StringBuilder(c.getClass().getName() + ": " + c.getMessage());
                        StackTraceElement[] st = c.getStackTrace();
                        for (int i = 0; i < Math.min(6, st.length); i++) sb.append(" @ ").append(st[i]);
                        thr = sb.toString();
                    }
                    String line = current + "\t" + e.getLevel() + "\t" + logger + "\t" + clean(msg) + "\t" + clean(thr);
                    synchronized (LOG_LINES) { if (LOG_LINES.size() < 200000) LOG_LINES.add(line); }
                }
            };
            app.start();
            ctx.getConfiguration().addAppender(app);
            ctx.getConfiguration().getRootLogger().addAppender(app, Level.ALL, null);
            ctx.getConfiguration().getLoggers().values().forEach(l -> {
                if (!l.isAdditive()) l.addAppender(app, Level.ALL, null);
            });
            ctx.updateLoggers();
        } catch (Throwable t) {
            MainRegistry.LOGGER.error("[HBM IconExport] Log-Appender nicht installierbar", t);
        }
    }

    private static String clean(String s) {
        s = s.replace('\t', ' ').replace('\r', ' ').replace("\n", " | ");
        return s.length() > 1500 ? s.substring(0, 1500) : s;
    }

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (!ENABLED || event.phase != TickEvent.Phase.END || phase == 4) return;
        Minecraft mc = Minecraft.getInstance();
        try {
            switch (phase) {
                case 0 -> {
                    if (mc.getOverlay() == null && mc.screen instanceof TitleScreen && ++titleTicks > 20) {
                        if (WORLD) {
                            startWorld(mc);
                            phase = 10;
                        } else {
                            begin(mc);
                        }
                    }
                }
                case 10 -> {
                    if (mc.level != null && mc.player != null && mc.getOverlay() == null) {
                        if (mc.screen != null && !(mc.screen instanceof PauseScreen)) {
                            worldTicks = 0;
                        } else if (++worldTicks > 200) {
                            if (mc.screen != null) mc.setScreen(null);
                            begin(mc);
                        }
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
                        current = "<ende>";
                        writeManifest();
                        phase = 4;
                        MainRegistry.LOGGER.info("[HBM IconExport] fertig: {} Icons nach {}", entries.size(), outDir);
                        writeLog();
                        mc.stop();
                    }
                }
                default -> { }
            }
        } catch (Throwable t) {
            MainRegistry.LOGGER.error("[HBM IconExport] abgebrochen", t);
            phase = 4;
            try { writeLog(); } catch (Throwable ignored) { }
            mc.stop();
        }
    }

    private static void begin(Minecraft mc) throws Exception {
        collect(mc);
        writeLog();
        pass1Start = System.currentTimeMillis();
        phase = 1;
    }

    /** Superflat-Kreativwelt anlegen (alte Testwelten vorher loeschen) */
    private static void startWorld(Minecraft mc) {
        mc.options.pauseOnLostFocus = false;
        Path saves = mc.gameDirectory.toPath().resolve("saves");
        if (Files.isDirectory(saves)) {
            try (Stream<Path> s = Files.list(saves)) {
                s.filter(p -> p.getFileName().toString().startsWith("hbm_icontest")).forEach(IconExportHook::deleteTree);
            } catch (IOException ignored) { }
        }
        String level = "hbm_icontest_" + System.currentTimeMillis();
        GameRules rules = new GameRules();
        rules.getRule(GameRules.RULE_DAYLIGHT).set(false, null);
        rules.getRule(GameRules.RULE_WEATHER_CYCLE).set(false, null);
        rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, null);
        LevelSettings settings = new LevelSettings(level, GameType.CREATIVE, false, Difficulty.PEACEFUL, true, rules,
                WorldDataConfiguration.DEFAULT);
        MainRegistry.LOGGER.info("[HBM IconExport] lege Testwelt {} an", level);
        mc.createWorldOpenFlows().createFreshLevel(level, settings, new WorldOptions(0L, false, false),
                ra -> ra.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());
    }

    private static void deleteTree(Path p) {
        try (Stream<Path> s = Files.walk(p)) {
            s.sorted(Comparator.reverseOrder()).forEach(q -> { try { Files.delete(q); } catch (IOException ignored) { } });
        } catch (IOException ignored) { }
    }

    private static void collect(Minecraft mc) throws Exception {
        outDir = Paths.get(System.getProperty("hbm.iconExportDir", "icon_export")).toAbsolutePath();
        Files.createDirectories(outDir);
        String f = System.getProperty("hbm.iconExportFilter");
        Pattern filter = f == null || f.isEmpty() ? null : Pattern.compile(f);

        HolderLookup.Provider lookup = mc.level != null ? mc.level.registryAccess() : VanillaRegistries.createLookup();
        CreativeModeTab.ItemDisplayParameters params = new CreativeModeTab.ItemDisplayParameters(
                FeatureFlags.REGISTRY.allFlags(), true, lookup);
        Map<String, Entry> seen = new LinkedHashMap<>();
        Map<String, Integer> variants = new LinkedHashMap<>();
        List<Entry> list = new ArrayList<>();
        for (CreativeModeTab tab : BuiltInRegistries.CREATIVE_MODE_TAB) {
            ResourceLocation tabId = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab);
            try {
                current = "<tab " + tabId + ">";
                tab.buildContents(params);
            } catch (Throwable t) {
                MainRegistry.LOGGER.error("[HBM IconExport] Tab {} nicht aufbaubar", tabId, t);
                continue;
            }
            for (ItemStack s : tab.getDisplayItems()) {
                add(s, String.valueOf(tabId), filter, seen, variants, list);
            }
        }
        current = "<export>";
        for (Item item : BuiltInRegistries.ITEM) {
            add(new ItemStack(item), "-", filter, seen, variants, list);
        }
        entries = list;
        info = new LinkedHashMap<>();
        target = new TextureTarget(PX, PX, true, Minecraft.ON_OSX);
        target.setClearColor(0F, 0F, 0F, 0F);
        MainRegistry.LOGGER.info("[HBM IconExport] {} Icons -> {} (Welt: {})", list.size(), outDir, mc.level != null);
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
            // Vanilla-Aufwaermzeichnung: GL-Zustand wie nach einem normalen Inventar-Icon (Basis fuer die Leckpruefung)
            renderOne(mc, new Entry(-1, "stone", 0, new ItemStack(net.minecraft.world.item.Items.STONE), "-", "<warmup>"), 0, false, false);
            for (; cursor < end; cursor++) {
                Entry e = entries.get(cursor);
                current = e.name;
                String[] row = info.computeIfAbsent(e.idx, k -> new String[] {"", "", "", "", "", "", "", ""});
                if (frame == 1) describe(mc, e, row);
                String before = glState();
                String err = renderOne(mc, e, frame, false, true);
                String after = glState();
                if (!before.equals(after)) {
                    if (frame == 1) row[7] = before + " -> " + after;
                    restoreGl();
                }
                if (frame == 1) {
                    String eb = renderOne(mc, e, 0, true, true);
                    if (!eb.isEmpty() && err.isEmpty()) err = "bg: " + eb;
                    restoreGl();
                }
                row[frame == 1 ? 5 : 6] = err;
            }
        } finally {
            current = "<export>";
            mv.popPose();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.setProjectionMatrix(oldProj, oldSort);
            mc.getMainRenderTarget().bindWrite(true);
        }
        return cursor >= entries.size();
    }

    /** GL-Zustand, den Item-Renderer haeufig verstellen (unsichtbare Folge-Icons im echten Inventar) */
    private static String glState() {
        float[] c = RenderSystem.getShaderColor();
        java.nio.ByteBuffer bb = org.lwjgl.BufferUtils.createByteBuffer(16);
        GL11.glGetBooleanv(GL11.GL_COLOR_WRITEMASK, bb);
        String cm = "" + (bb.get(0) != 0) + (bb.get(1) != 0) + (bb.get(2) != 0) + (bb.get(3) != 0);
        return "col=" + c[0] + "," + c[1] + "," + c[2] + "," + c[3]
                + " dmask=" + GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK)
                + " dtest=" + GL11.glIsEnabled(GL11.GL_DEPTH_TEST)
                + " dfunc=" + GL11.glGetInteger(GL11.GL_DEPTH_FUNC)
                + " blend=" + GL11.glIsEnabled(GL11.GL_BLEND)
                + " cull=" + GL11.glIsEnabled(GL11.GL_CULL_FACE)
                + " scissor=" + GL11.glIsEnabled(GL11.GL_SCISSOR_TEST)
                + " cmask=" + cm
                + " poly=" + GL11.glIsEnabled(GL11.GL_POLYGON_OFFSET_FILL);
    }

    private static void restoreGl() {
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        GL11.glDepthMask(true);
        RenderSystem.depthMask(true);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        RenderSystem.enableDepthTest();
        GL11.glDepthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        RenderSystem.disableScissor();
        GL11.glColorMask(true, true, true, true);
        RenderSystem.colorMask(true, true, true, true);
        GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
        RenderSystem.disablePolygonOffset();
        GL11.glEnable(GL11.GL_CULL_FACE);
        RenderSystem.enableCull();
    }

    private static String renderOne(Minecraft mc, Entry e, int frame, boolean background, boolean save) {
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
            if (background) {
                g.fill(0, 0, CANVAS, CANVAS, BG);
                g.flush();
            }
            g.renderItem(e.stack, SLOT, SLOT);
            g.renderItemDecorations(mc.font, e.stack, SLOT, SLOT);
            g.flush();
        } catch (Throwable t) {
            Throwable c = t;
            while (c.getCause() != null && c.getCause() != c) c = c.getCause();
            err = (c.getClass().getSimpleName() + ": " + c.getMessage()).replace('\t', ' ').replace('\n', ' ');
            MainRegistry.LOGGER.error("[HBM IconExport] Exception rendering item {}", e.name, t);
            recover(mc);
        }
        if (!save) return err;
        try (NativeImage img = new NativeImage(PX, PX, false)) {
            RenderSystem.bindTexture(target.getColorTextureId());
            img.downloadTexture(0, false);
            img.flipY();
            img.writeToFile(outDir.resolve(e.name + (background ? "_bg" : "_f" + frame) + ".png"));
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
            BakedModel m = mc.getItemRenderer().getModel(e.stack, mc.level, mc.player, 0);
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
            w.write("idx\tname\tid\tvariant\ttab\tnbt\tmodel\tcustom\tmissing_model\tmissing_tex\tbewlr\terr1\terr2\tglleak\n");
            for (Entry e : entries) {
                String[] r = info.getOrDefault(e.idx, new String[8]);
                w.write(e.idx + "\t" + e.name + "\t" + e.id + "\t" + e.variant + "\t" + e.tab + "\t"
                        + (e.stack.getTag() == null ? "" : e.stack.getTag().toString().replace('\t', ' '))
                        + "\t" + r[0] + "\t" + r[1] + "\t" + r[2] + "\t" + r[3] + "\t" + r[4] + "\t" + r[5] + "\t" + r[6]
                        + "\t" + (r[7] == null ? "" : r[7]) + "\n");
            }
        }
    }

    private static void writeLog() throws Exception {
        if (outDir == null) return;
        List<String> copy;
        synchronized (LOG_LINES) { copy = new ArrayList<>(LOG_LINES); }
        try (BufferedWriter w = Files.newBufferedWriter(outDir.resolve("log_errors.tsv"), StandardCharsets.UTF_8)) {
            w.write("context\tlevel\tlogger\tmessage\tthrowable\n");
            for (String s : copy) w.write(s + "\n");
        }
    }
}
//?}
