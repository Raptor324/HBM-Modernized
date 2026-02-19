package com.hbm_m.client.loader;

import com.hbm_m.main.MainRegistry;
import org.joml.Matrix4f;
import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.InputStream;
import java.util.*;

public class ColladaAnimationParser {

    public static class Result {
        public final Map<String, List<AnimationChannel>> animations;
        public final Map<String, String> hierarchy; // child -> parent
        public final boolean zUp;
        public final float duration;

        public Result(Map<String, List<AnimationChannel>> animations, Map<String, String> hierarchy, boolean zUp, float duration) {
            this.animations = animations;
            this.hierarchy = hierarchy;
            this.zUp = zUp;
            this.duration = duration;
        }
    }

    // Public constant for DoorRenderer
    public static final Matrix4f Z_UP_TO_Y_UP = new Matrix4f(
            1, 0, 0, 0,
            0, 0, 1, 0,
            0, -1, 0, 0,
            0, 0, 0, 1
    );
    
    private static final Matrix4f CORRECTION_INV = new Matrix4f(Z_UP_TO_Y_UP).invert();

    public static Result parse(InputStream daeStream) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(daeStream);

            boolean zUp = parseUpAxis(doc);
            Map<String, List<AnimationChannel>> animations = new HashMap<>();
            Map<String, String> hierarchy = new HashMap<>();

            // 1. Parse Animations
            NodeList allAnims = doc.getElementsByTagName("animation");
            for (int i = 0; i < allAnims.getLength(); i++) {
                Element anim = (Element) allAnims.item(i);
                if (hasDirectChild(anim, "channel")) {
                    parseAnimation(anim, animations, zUp);
                }
            }

            // 2. Parse Hierarchy (Visual Scene) to support child bone rendering
            NodeList scenes = doc.getElementsByTagName("visual_scene");
            if (scenes.getLength() > 0) {
                parseHierarchy((Element) scenes.item(0), hierarchy);
            }

            // 3. Calculate max duration
            float maxTime = 0f;
            for (List<AnimationChannel> channels : animations.values()) {
                for (AnimationChannel ch : channels) {
                    if (ch.times != null && ch.times.length > 0) {
                        maxTime = Math.max(maxTime, ch.times[ch.times.length - 1]);
                    }
                }
            }

            return new Result(animations, hierarchy, zUp, maxTime);

        } catch (Exception e) {
            MainRegistry.LOGGER.error("DAE Parsing Error", e);
            return new Result(Collections.emptyMap(), Collections.emptyMap(), false, 0f);
        }
    }

    private static boolean parseUpAxis(Document doc) {
        NodeList assets = doc.getElementsByTagName("asset");
        if (assets.getLength() > 0) {
            NodeList upAxis = ((Element) assets.item(0)).getElementsByTagName("up_axis");
            if (upAxis.getLength() > 0) {
                return "Z_UP".equals(upAxis.item(0).getTextContent().trim());
            }
        }
        return false;
    }

    private static void parseHierarchy(Element node, Map<String, String> hierarchy) {
        String parentId = node.getAttribute("id");
        if (parentId == null || parentId.isEmpty()) parentId = node.getAttribute("name");

        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE && "node".equals(child.getNodeName())) {
                Element childElem = (Element) child;
                String childId = childElem.getAttribute("id");
                if (childId == null || childId.isEmpty()) childId = childElem.getAttribute("name");

                if (parentId != null && !parentId.isEmpty() && childId != null && !childId.isEmpty()) {
                    hierarchy.put(childId, parentId);
                }
                parseHierarchy(childElem, hierarchy);
            }
        }
    }

    private static boolean hasDirectChild(Element parent, String name) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i) instanceof Element && children.item(i).getNodeName().equals(name)) return true;
        }
        return false;
    }

    private static void parseAnimation(Element anim, Map<String, List<AnimationChannel>> result, boolean zUp) {
        Map<String, float[]> sources = new HashMap<>();
        NodeList sourceList = anim.getElementsByTagName("source");
        for (int i = 0; i < sourceList.getLength(); i++) {
            Element source = (Element) sourceList.item(i);
            String id = source.getAttribute("id");
            Element floatArray = (Element) source.getElementsByTagName("float_array").item(0);
            if (floatArray != null) {
                String[] raw = floatArray.getTextContent().trim().split("\\s+");
                float[] data = new float[raw.length];
                for (int k = 0; k < raw.length; k++) data[k] = Float.parseFloat(raw[k]);
                sources.put(id, data);
            }
        }

        Element sampler = (Element) anim.getElementsByTagName("sampler").item(0);
        if (sampler == null) return;
        String inputId = null, outputId = null;
        NodeList inputs = sampler.getElementsByTagName("input");
        for (int i = 0; i < inputs.getLength(); i++) {
            Element inp = (Element) inputs.item(i);
            String src = inp.getAttribute("source").substring(1);
            if ("INPUT".equals(inp.getAttribute("semantic"))) inputId = src;
            else if ("OUTPUT".equals(inp.getAttribute("semantic"))) outputId = src;
        }

        Element channel = (Element) anim.getElementsByTagName("channel").item(0);
        if (channel == null) return;
        String target = channel.getAttribute("target");
        String objectName = target.split("/")[0];

        if (inputId != null && outputId != null && sources.containsKey(inputId) && sources.containsKey(outputId)) {
            AnimationChannel ch = new AnimationChannel();
            ch.times = sources.get(inputId);
            ch.values = sources.get(outputId);
            ch.zUpSource = zUp;
            result.computeIfAbsent(objectName, k -> new ArrayList<>()).add(ch);
        }
    }

    public static class AnimationChannel {
        public float[] times;
        public float[] values;
        public boolean zUpSource;

        public Matrix4f getInterpolatedMatrix(float time) {
            if (values.length < 16) return new Matrix4f();

            int frame = 0;
            float factor = 0;
            if (time >= times[times.length - 1]) {
                frame = times.length - 1;
            } else {
                for (int i = 0; i < times.length - 1; i++) {
                    if (time >= times[i] && time < times[i + 1]) {
                        frame = i;
                        factor = (time - times[i]) / (times[i + 1] - times[i]);
                        break;
                    }
                }
            }

            float[] m1 = getMatrixData(frame);
            float[] m2 = getMatrixData(Math.min(frame + 1, times.length - 1));
            float[] res = new float[16];
            for (int i = 0; i < 16; i++) res[i] = m1[i] + (m2[i] - m1[i]) * factor;

            Matrix4f mat = new Matrix4f(
                res[0], res[4], res[8], res[12],
                res[1], res[5], res[9], res[13],
                res[2], res[6], res[10], res[14],
                res[3], res[7], res[11], res[15]
            );

            if (zUpSource) {
                // Fix Z-Up -> Y-Up directly in interpolation
                Matrix4f temp = new Matrix4f(Z_UP_TO_Y_UP);
                temp.mul(mat).mul(CORRECTION_INV);
                return temp;
            }
            return mat;
        }

        private float[] getMatrixData(int index) {
            float[] m = new float[16];
            System.arraycopy(values, index * 16, m, 0, 16);
            return m;
        }
    }
}