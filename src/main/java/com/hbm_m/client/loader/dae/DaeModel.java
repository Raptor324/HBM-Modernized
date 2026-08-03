package com.hbm_m.client.loader.dae;

import com.hbm_m.lib.RefStrings;
import net.minecraft.resources.ResourceLocation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A COLLADA (.dae) model parsed from a resource: geometry, the visual scene node tree,
 * material -> texture resolution and animation clips. Static data only; the vertex
 * buffers are baked by {@link DaeModelRenderer}. Parsing is deliberately lenient since
 * the files are hand made for the mod.
 */
public class DaeModel {

    public final ResourceLocation resource;
    public final List<DaeNode> sceneRoots = new ArrayList<>();
    public final Map<String, DaeMesh> meshes = new HashMap<>();
    public final Map<String, ResourceLocation> textures = new HashMap<>();
    public final Map<String, DaeAnimation> animations = new HashMap<>();

    public float scale = 1.0F;

    /** All parsed models, so the reload listener can re-bake them. */
    public static final Collection<DaeModel> allModels = Collections.synchronizedCollection(new ArrayList<>());

    public DaeModel(String path) {
        this(RefStrings.resourceLocation(path));
    }

    public DaeModel(ResourceLocation resource) {
        this.resource = resource;
    }

    public static DaeModel load(ResourceLocation resource) {
        DaeModel model = new DaeModel(resource);
        model.parseResource();
        allModels.add(model);
        return model;
    }

    public void reload() {
        sceneRoots.clear();
        meshes.clear();
        textures.clear();
        animations.clear();
        parseResource();
    }

    private void parseResource() {
        try (InputStream stream = MinecraftResourceResolver.open(resource)) {
            parse(stream);
        } catch(IOException e) {
            throw new DaeModelFormatException("Failed to read DAE model " + resource, e);
        }
    }

    public void clear() {
        sceneRoots.clear();
        meshes.clear();
        textures.clear();
        animations.clear();
        allModels.remove(this);
    }

    /** Computes smooth normals for meshes that lack normal data. */
    public static float[] computeNormals(DaeMesh mesh) {
        float[] positions = mesh.positions;
        if(positions == null) return new float[0];
        int count = positions.length / 3;
        float[] normals = new float[positions.length];

        for(int[] tri : mesh.tris) {
            float ax = positions[tri[0] * 3], ay = positions[tri[0] * 3 + 1], az = positions[tri[0] * 3 + 2];
            float bx = positions[tri[3] * 3], by = positions[tri[3] * 3 + 1], bz = positions[tri[3] * 3 + 2];
            float cx = positions[tri[6] * 3], cy = positions[tri[6] * 3 + 1], cz = positions[tri[6] * 3 + 2];

            float ux = bx - ax, uy = by - ay, uz = bz - az;
            float vx = cx - ax, vy = cy - ay, vz = cz - az;
            float nx = uy * vz - uz * vy;
            float ny = uz * vx - ux * vz;
            float nz = ux * vy - uy * vx;

            for(int i = 0; i < 3; i++) {
                int idx = tri[i * 3] * 3;
                normals[idx] += nx;
                normals[idx + 1] += ny;
                normals[idx + 2] += nz;
            }
        }

        for(int i = 0; i < count; i++) {
            float x = normals[i * 3], y = normals[i * 3 + 1], z = normals[i * 3 + 2];
            float len = (float) Math.sqrt(x * x + y * y + z * z);
            if(len > 1e-6F) {
                normals[i * 3] = x / len;
                normals[i * 3 + 1] = y / len;
                normals[i * 3 + 2] = z / len;
            } else {
                normals[i * 3 + 1] = 1F;
            }
        }
        return normals;
    }

    private void parse(InputStream stream) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            Document doc = factory.newDocumentBuilder().parse(stream);

            Element root = doc.getDocumentElement();
            if(root == null || !"COLLADA".equals(root.getNodeName())) {
                throw new DaeModelFormatException("Not a COLLADA document: " + resource);
            }

            parseGeometries(root);
            Map<String, String> materialEffects = parseMaterials(root);
            Map<String, String> effectImages = parseEffects(root);
            Map<String, String> imagePaths = parseImages(root);
            buildTextureMap(root, materialEffects, effectImages, imagePaths);
            parseScene(root);
            parseAnimations(root);
        } catch(ParserConfigurationException | SAXException | IOException e) {
            throw new DaeModelFormatException("Malformed XML in " + resource, e);
        }
    }

    // ---------------------------------------------------------------- geometries

    private void parseGeometries(Element root) {
        for(Element library : children(root, "library_geometries")) {
            for(Element geo : children(library, "geometry")) {
                String id = geo.getAttribute("id");
                if(id.isEmpty()) continue;
                Element meshEl = firstChild(geo, "mesh");
                if(meshEl == null) continue;

                DaeMesh mesh = new DaeMesh();
                for(Element primitive : union(meshEl, "polylist", "triangles")) {
                    parsePrimitive(meshEl, primitive, mesh);
                }
                meshes.put(id, mesh);
            }
        }
    }

    private void parsePrimitive(Element meshEl, Element prim, DaeMesh mesh) {
        Map<String, Integer> semanticOffset = new HashMap<>();
        int maxOffset = 0;
        for(Element input : children(prim, "input")) {
            String semantic = input.getAttribute("semantic");
            int offset = parseInt(input.getAttribute("offset"), 0);
            semanticOffset.put(semantic, offset);
            maxOffset = Math.max(maxOffset, offset + 1);
        }

        // COLLADA primitives reference vertex positions through either a POSITION
        // input directly or a VERTEX input pointing at a <vertices> element.
        int posOffset;
        if(semanticOffset.containsKey("VERTEX")) posOffset = semanticOffset.get("VERTEX");
        else posOffset = semanticOffset.getOrDefault("POSITION", 0);
        int normalOffset = semanticOffset.getOrDefault("NORMAL", 1);
        int uvOffset = semanticOffset.getOrDefault("TEXCOORD", 2);
        boolean haveNormals = semanticOffset.containsKey("NORMAL");
        boolean haveUvs = semanticOffset.containsKey("TEXCOORD");

        float[] positions = readSource(meshEl, posOffset, prim);
        if(positions == null) return;

        // The mesh source arrays are taken from the first primitive; later primitives
        // usually share the same sources in hand-made files.
        if(mesh.positions == null) {
            mesh.positions = positions;
            mesh.normals = haveNormals ? readSource(meshEl, semanticOffset.get("NORMAL"), prim) : null;
            mesh.uvs = haveUvs ? readSource(meshEl, semanticOffset.get("TEXCOORD"), prim) : null;
        }

        int[] indices = parseIntArray(textOf(prim, "p"));
        if(indices == null || indices.length == 0) return;

        if(prim.getNodeName().equals("polylist")) {
            int[] vcount = parseIntArray(textOf(prim, "vcount"));
            if(vcount == null) return;

            int ptr = 0;
            for(int count : vcount) {
                int[] poly = new int[count * 3];
                for(int i = 0; i < count; i++) {
                    poly[i * 3] = indices[ptr + i * maxOffset + posOffset];
                    poly[i * 3 + 1] = haveNormals ? indices[ptr + i * maxOffset + normalOffset] : -1;
                    poly[i * 3 + 2] = haveUvs ? indices[ptr + i * maxOffset + uvOffset] : -1;
                }
                ptr += count * maxOffset;
                for(int i = 1; i < count - 1; i++) {
                    addTriangle(mesh.tris, poly, 0, i * 3, (i + 1) * 3);
                }
            }
        } else {
            int count = parseInt(prim.getAttribute("count"), indices.length / maxOffset / 3);
            for(int i = 0; i < count; i++) {
                int base = i * maxOffset * 3;
                int[] tri = new int[9];
                for(int j = 0; j < 3; j++) {
                    tri[j * 3] = indices[base + j * maxOffset + posOffset];
                    tri[j * 3 + 1] = haveNormals ? indices[base + j * maxOffset + normalOffset] : -1;
                    tri[j * 3 + 2] = haveUvs ? indices[base + j * maxOffset + uvOffset] : -1;
                }
                mesh.tris.add(tri);
            }
        }
    }

    /** Emits a fan triangle from the corner triples of a polygon. */
    private static void addTriangle(List<int[]> tris, int[] corners, int a, int b, int c) {
        tris.add(new int[] { corners[a], corners[a + 1], corners[a + 2],
                corners[b], corners[b + 1], corners[b + 2],
                corners[c], corners[c + 1], corners[c + 2] });
    }

    /** Reads the float_array referenced by the primitive input with the given offset. */
    private float[] readSource(Element meshEl, Integer offset, Element prim) {
        if(offset == null) return null;
        for(Element input : children(prim, "input")) {
            if(parseInt(input.getAttribute("offset"), 0) == offset) {
                return readFloatArray(meshEl, input.getAttribute("source"));
            }
        }
        return null;
    }

    // ---------------------------------------------------------------- materials

    private Map<String, String> parseMaterials(Element root) {
        Map<String, String> map = new HashMap<>();
        for(Element library : children(root, "library_materials")) {
            for(Element mat : children(library, "material")) {
                String id = mat.getAttribute("id");
                for(Element effect : children(mat, "instance_effect")) {
                    map.put(id, effect.getAttribute("url").replace("#", ""));
                }
            }
        }
        return map;
    }

    private Map<String, String> parseEffects(Element root) {
        Map<String, String> map = new HashMap<>();
        for(Element library : children(root, "library_effects")) {
            for(Element fx : children(library, "effect")) {
                String id = fx.getAttribute("id");
                for(Element technique : children(fx, "technique")) {
                    for(Element shade : children(technique, "phong", "lambert", "blinn", "constant")) {
                        for(Element diffuse : children(shade, "diffuse")) {
                            Element tex = firstChild(diffuse, "texture");
                            if(tex != null) {
                                String sampler = tex.getAttribute("texture");
                                map.put(id, sampler);
                                break;
                            }
                        }
                    }
                }
            }
        }
        return map;
    }

    private Map<String, String> parseImages(Element root) {
        Map<String, String> map = new HashMap<>();
        for(Element library : children(root, "library_images")) {
            for(Element image : children(library, "image")) {
                String id = image.getAttribute("id");
                String path = textOf(image, "init_from");
                map.put(id, path);
            }
        }
        return map;
    }

    /** Maps the geometry's material names to a texture resource location. */
    private void buildTextureMap(Element root, Map<String, String> materialEffects, Map<String, String> effectImages, Map<String, String> imagePaths) {
        Map<String, String> samplerImages = new HashMap<>();
        Map<String, String> surfaceImages = new HashMap<>();

        for(Element library : libraries(root, "library_effects")) {
            for(Element fx : children(library, "effect")) {
                for(Element technique : children(fx, "technique")) {
                    for(Element samplerEl : children(technique, "newparam")) {
                        Element surface = firstChild(samplerEl, "surface");
                        if(surface != null) {
                            surfaceImages.put(samplerEl.getAttribute("sid"), textOf(surface, "init_from"));
                        }
                        Element sampler2d = firstChild(samplerEl, "sampler2D");
                        if(sampler2d == null) continue;
                        String source = textOf(sampler2d, "source");
                        String samplerName = samplerEl.getAttribute("sid");
                        samplerImages.put(samplerName, source);
                    }
                }
            }
        }

        Map<String, String> effectTextures = new HashMap<>();
        for(String effectId : effectImages.keySet()) {
            String sampler = effectImages.get(effectId);
            String image = samplerImages.get(sampler);
            if(image == null) image = sampler;
            // Follow the surface sid down to the image id it initializes from.
            String imageId = surfaceImages.get(image);
            if(imageId != null) image = imageId;
            effectTextures.put(effectId, image);
        }

        for(Element library : libraries(root, "library_geometries")) {
            for(Element geo : children(library, "geometry")) {
                Element mesh = firstChild(geo, "mesh");
                if(mesh == null) continue;
                for(Element prim : union(mesh, "polylist", "triangles")) {
                    String materialName = prim.getAttribute("material");
                    if(materialName.isEmpty()) continue;
                    String effectId = materialEffects.get(materialName);
                    if(effectId == null) continue;
                    String image = effectTextures.get(effectId);
                    if(image == null) continue;
                    // Resolve the image id to its init_from path when available.
                    String path = imagePaths.get(image);
                    if(path == null) path = image;
                    textures.put(materialName, resolveTexture(path));
                }
            }
        }

        // Fall back to the first image in the file so even unbound materials render.
        if(textures.isEmpty()) {
            for(String path : imagePaths.values()) {
                textures.put("default", resolveTexture(path));
                break;
            }
        }
    }

    private ResourceLocation resolveTexture(String path) {
        if(path == null || path.isBlank()) {
            return RefStrings.resourceLocation("textures/models/missing");
        }
        if(path.startsWith("file://")) path = path.substring("file://".length());
        String clean = path.replace('\\', '/').trim();
        while(clean.startsWith("/")) clean = clean.substring(1);

        // COLLADA exports may reference textures relative to the mod's textures/ folder.
        if(clean.startsWith("textures/")) {
            clean = clean.substring("textures/".length());
        } else if(clean.startsWith("assets/")) {
            clean = clean.substring("assets/".length());
            int slash = clean.indexOf('/');
            if(slash >= 0) {
                clean = clean.substring(slash + 1);
            }
        }

        // Absolute filesystem paths carry no mod-relative info; use the file name.
        if(!clean.startsWith("textures/") && !clean.startsWith("block/") && !clean.startsWith("item/") && !clean.startsWith("entity/")) {
            int slash = clean.lastIndexOf('/');
            if(slash >= 0) clean = clean.substring(slash + 1);
            clean = "textures/models/" + clean;
        }

        int dot = clean.lastIndexOf('.');
        if(dot >= 0) clean = clean.substring(0, dot);
        return RefStrings.resourceLocation(clean);
    }

    // ---------------------------------------------------------------- scene

    private void parseScene(Element root) {
        for(Element library : children(root, "library_visual_scenes")) {
            for(Element scene : children(library, "visual_scene")) {
                for(Element nodeEl : children(scene, "node")) {
                    DaeNode node = parseNode(nodeEl);
                    if(node != null) sceneRoots.add(node);
                }
            }
        }
    }

    private DaeNode parseNode(Element nodeEl) {
        String name = nodeEl.getAttribute("name");
        if(name.isEmpty()) name = nodeEl.getAttribute("id");
        if(name.isEmpty()) name = "node";

        DaeNode node = new DaeNode(name);
        node.colladaId = nodeEl.getAttribute("id");

        for(Element child : children(nodeEl)) {
            switch(child.getNodeName()) {
                case "translate" -> {
                    String sid = child.getAttribute("sid");
                    node.transforms.add(new DaeTransform(DaeTransform.Type.TRANSLATE, sid, parseFloatArray(textOf(child))));
                }
                case "rotate" -> {
                    String sid = child.getAttribute("sid");
                    node.transforms.add(new DaeTransform(DaeTransform.Type.ROTATE, sid, parseFloatArray(textOf(child))));
                }
                case "scale" -> {
                    String sid = child.getAttribute("sid");
                    node.transforms.add(new DaeTransform(DaeTransform.Type.SCALE, sid, parseFloatArray(textOf(child))));
                }
                case "matrix" -> {
                    String sid = child.getAttribute("sid");
                    node.transforms.add(new DaeTransform(DaeTransform.Type.MATRIX, sid, parseFloatArray(textOf(child))));
                }
                case "instance_geometry" -> {
                    String url = child.getAttribute("url").replace("#", "");
                    DaeMesh mesh = meshes.get(url);
                    if(mesh != null) {
                        node.mesh = mesh;
                        String material = null;
                        Element instance = firstChild(child, "bind_material");
                        if(instance != null) {
                            Element technique = firstChild(instance, "technique_common");
                            if(technique != null) {
                                for(Element bind : children(technique, "instance_material")) {
                                    material = bind.getAttribute("target").replace("#", "");
                                }
                            }
                        }
                        if(material != null && textures.containsKey(material)) {
                            node.texture = textures.get(material);
                        } else {
                            node.texture = firstTexture();
                        }
                    }
                }
                case "node" -> {
                    DaeNode childNode = parseNode(child);
                    if(childNode != null) node.children.add(childNode);
                }
            }
        }
        return node;
    }

    private ResourceLocation firstTexture() {
        for(ResourceLocation tex : textures.values()) {
            return tex;
        }
        return null;
    }

    // ---------------------------------------------------------------- animations

    private void parseAnimations(Element root) {
        for(Element library : children(root, "library_animations")) {
            String clipName = library.getAttribute("id");
            if(clipName.isEmpty()) clipName = "animation";
            DaeAnimation clip = new DaeAnimation(clipName);

            Map<String, Element> samplerById = new HashMap<>();
            for(Element anim : children(library, "animation")) {
                collectSamplers(anim, samplerById);
            }
            for(Element anim : children(library, "animation")) {
                collectChannels(anim, root, samplerById, clip);
            }
            if(!clip.isEmpty()) {
                animations.put(clipName, clip);
            }
        }
    }

    private void collectSamplers(Element anim, Map<String, Element> samplerById) {
        for(Element sampler : children(anim, "sampler")) {
            samplerById.put(sampler.getAttribute("id"), sampler);
        }
        for(Element child : children(anim, "animation")) {
            collectSamplers(child, samplerById);
        }
    }

    private void collectChannels(Element anim, Element root, Map<String, Element> samplerById, DaeAnimation clip) {
        for(Element channel : children(anim, "channel")) {
            buildChannel(root, channel, samplerById, clip);
        }
        for(Element child : children(anim, "animation")) {
            collectChannels(child, root, samplerById, clip);
        }
    }

    private void buildChannel(Element root, Element channel, Map<String, Element> samplerById, DaeAnimation clip) {
        String target = channel.getAttribute("target");
        String samplerId = channel.getAttribute("source").replace("#", "");
        Element sampler = samplerById.get(samplerId);
        if(sampler == null || target == null || target.isEmpty()) return;

        int slash = target.indexOf('/');
        if(slash < 0) return;
        String nodeName = target.substring(0, slash);
        String property = target.substring(slash + 1);

        Element samplerInput = null;
        Element samplerOutput = null;
        Element samplerInterp = null;
        for(Element input : children(sampler, "input")) {
            switch(input.getAttribute("semantic")) {
                case "INPUT" -> samplerInput = input;
                case "OUTPUT" -> samplerOutput = input;
                case "INTERPOLATION" -> samplerInterp = input;
            }
        }
        if(samplerInput == null || samplerOutput == null) return;

        float[] times = readAnimationSource(root, samplerInput.getAttribute("source"));
        float[] values = readAnimationSource(root, samplerOutput.getAttribute("source"));
        String[] interps = samplerInterp != null ? readStringSource(root, samplerInterp.getAttribute("source")) : null;

        if(times == null || times.length == 0 || values == null) return;

        // Determine the per-keyframe stride of the output source.
        int stride;
        String suffix = property.substring(property.lastIndexOf('.') + 1);
        if(suffix.equals("ANGLE") || suffix.length() == 1 && "XYZ".indexOf(suffix) >= 0) {
            stride = 1;
        } else if(property.equals("matrix")) {
            stride = 16;
        } else {
            stride = values.length / times.length;
            if(stride < 1) stride = 1;
        }

        if(values.length < times.length * stride) return;

        float[][] rows = new float[times.length][stride];
        for(int i = 0; i < times.length; i++) {
            System.arraycopy(values, i * stride, rows[i], 0, stride);
        }
        boolean[] step = new boolean[times.length];
        for(int i = 0; i < times.length; i++) {
            if(interps != null && i < interps.length && !"LINEAR".equals(interps[i])) {
                step[i] = true;
            }
        }

        // Extract single-component curves for targeted axes.
        if(stride == 1) {
            float[][] flat = new float[times.length][1];
            for(int i = 0; i < times.length; i++) flat[i][0] = rows[i][0];
            clip.addChannel(nodeName, property, new DaeCurve(times, flat, step, 1));
        } else if(stride == 3) {
            String axis = suffix.length() == 1 && "XYZ".indexOf(suffix) >= 0 ? suffix : null;
            String baseProp = axis != null ? property.substring(0, property.length() - 2) : property;
            if(axis != null) {
                int comp = "XYZ".indexOf(axis);
                float[][] flat = new float[times.length][1];
                for(int i = 0; i < times.length; i++) flat[i][0] = rows[i][comp];
                clip.addChannel(nodeName, baseProp + "." + axis, new DaeCurve(times, flat, step, 1));
            } else {
                clip.addChannel(nodeName, property, new DaeCurve(times, rows, step, 3));
            }
        } else {
            clip.addChannel(nodeName, property, new DaeCurve(times, rows, step, stride));
        }
    }

    private float[] readAnimationSource(Element root, String sourceId) {
        Element source = findSource(root, sourceId);
        if(source == null) return null;
        return parseFloatArray(textOf(source, "float_array"));
    }

    private String[] readStringSource(Element root, String sourceId) {
        Element source = findSource(root, sourceId);
        if(source == null) return null;
        String[] words = textOf(source, "Name_array", "IDREF_array").trim().split("\\s+");
        List<String> result = new ArrayList<>();
        for(String word : words) {
            if(!word.isEmpty()) result.add(word);
        }
        return result.toArray(new String[0]);
    }

    private Element findSource(Element root, String sourceId) {
        if(sourceId.startsWith("#")) sourceId = sourceId.substring(1);
        if(sourceId.isEmpty()) return null;

        for(Element source : descendants(root, "source")) {
            if(sourceId.equals(source.getAttribute("id"))) return source;
        }
        return null;
    }

    // ---------------------------------------------------------------- helpers

    private static List<Element> libraries(Element root, String libraryName) {
        return children(root, libraryName);
    }

    private static List<Element> children(Element parent, String... names) {
        List<Element> result = new ArrayList<>();
        NodeList list = parent.getChildNodes();
        for(int i = 0; i < list.getLength(); i++) {
            Node n = list.item(i);
            if(n instanceof Element el) {
                for(String name : names) {
                    if(name.equals(el.getNodeName())) {
                        result.add(el);
                        break;
                    }
                }
            }
        }
        return result;
    }

    private static List<Element> children(Element parent) {
        List<Element> result = new ArrayList<>();
        NodeList list = parent.getChildNodes();
        for(int i = 0; i < list.getLength(); i++) {
            if(list.item(i) instanceof Element el) result.add(el);
        }
        return result;
    }

    private static Element firstChild(Element parent, String name) {
        for(Element el : children(parent, name)) return el;
        return null;
    }

    private static List<Element> union(Element parent, String... names) {
        return children(parent, names);
    }

    private static List<Element> descendants(Element root, String... tags) {
        List<Element> result = new ArrayList<>();
        collectDescendants(root, tags, result);
        return result;
    }

    private static void collectDescendants(Element parent, String[] tags, List<Element> out) {
        for(Element el : children(parent)) {
            for(String tag : tags) {
                if(tag.equals(el.getNodeName())) out.add(el);
            }
            collectDescendants(el, tags, out);
        }
    }

    private static String textOf(Element el) {
        return el == null ? "" : el.getTextContent().trim();
    }

    private static String textOf(Element parent, String... childNames) {
        for(String name : childNames) {
            Element child = firstChild(parent, name);
            if(child != null) return child.getTextContent().trim();
        }
        return "";
    }

    private static int parseInt(String text, int fallback) {
        try {
            return Integer.parseInt(text.trim());
        } catch(NumberFormatException e) {
            return fallback;
        }
    }

    private static int[] parseIntArray(String text) {
        String[] parts = text.trim().split("\\s+");
        if(parts.length == 1 && parts[0].isEmpty()) return null;
        int[] result = new int[parts.length];
        for(int i = 0; i < parts.length; i++) {
            try {
                result[i] = Integer.parseInt(parts[i]);
            } catch(NumberFormatException e) {
                return null;
            }
        }
        return result;
    }

    private static float[] parseFloatArray(String text) {
        String[] parts = text.trim().split("\\s+");
        if(parts.length == 1 && parts[0].isEmpty()) return new float[0];
        float[] result = new float[parts.length];
        for(int i = 0; i < parts.length; i++) {
            try {
                result[i] = Float.parseFloat(parts[i]);
            } catch(NumberFormatException e) {
                return new float[0];
            }
        }
        return result;
    }

    private static float[] readFloatArray(Element meshEl, String source) {
        if(source.startsWith("#")) source = source.substring(1);
        if(source.isEmpty()) return null;

        // A POSITION input may reference a <vertices> element instead of a <source>;
        // follow its POSITION input down to the actual source.
        for(Element vertices : descendants(meshEl, "vertices")) {
            if(source.equals(vertices.getAttribute("id"))) {
                Element posInput = firstChild(vertices, "input");
                if(posInput != null) {
                    return readFloatArray(meshEl, posInput.getAttribute("source"));
                }
            }
        }

        for(Element sourceEl : descendants(meshEl, "source")) {
            if(source.equals(sourceEl.getAttribute("id"))) {
                return parseFloatArray(textOf(sourceEl, "float_array"));
            }
        }
        return null;
    }

    public ResourceLocation getTexture() {
        return firstTexture();
    }
}
