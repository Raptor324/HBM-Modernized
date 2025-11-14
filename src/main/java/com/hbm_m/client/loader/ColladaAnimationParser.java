package com.hbm_m.client.loader;

import org.w3c.dom.*;

import com.hbm_m.main.MainRegistry;

import javax.xml.parsers.*;
import java.io.InputStream;
import java.util.*;

public class ColladaAnimationParser {
    
    public static Map<String, List<AnimationChannel>> parse(InputStream daeStream) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(daeStream);
            
            Map<String, List<AnimationChannel>> result = new HashMap<>();
            
            // Парсим <library_animations>
            NodeList animList = doc.getElementsByTagName("animation");
            
            for (int i = 0; i < animList.getLength(); i++) {
                Element anim = (Element) animList.item(i);
                parseAnimation(anim, result);
            }
            
            return result;
            
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyMap();
        }
    }
    
    private static void parseAnimation(Element anim, Map<String, List<AnimationChannel>> result) {
        // Парсим <source> элементы
        Map<String, float[]> sources = new HashMap<>();
        NodeList sourceList = anim.getElementsByTagName("source");
        
        for (int i = 0; i < sourceList.getLength(); i++) {
            Element source = (Element) sourceList.item(i);
            String sourceId = source.getAttribute("id");
            
            NodeList floatArrays = source.getElementsByTagName("float_array");
            if (floatArrays.getLength() > 0) {
                Element floatArray = (Element) floatArrays.item(0);
                String[] values = floatArray.getTextContent().trim().split("\\s+");
                float[] floats = new float[values.length];
                for (int j = 0; j < values.length; j++) {
                    floats[j] = Float.parseFloat(values[j]);
                }
                sources.put(sourceId, floats);
            }
        }
        
        // Парсим <sampler>
        NodeList samplers = anim.getElementsByTagName("sampler");
        if (samplers.getLength() == 0) return;
        
        Element sampler = (Element) samplers.item(0);
        NodeList inputs = sampler.getElementsByTagName("input");
        
        String inputSource = null;
        String outputSource = null;
        
        for (int i = 0; i < inputs.getLength(); i++) {
            Element input = (Element) inputs.item(i);
            String semantic = input.getAttribute("semantic");
            String source = input.getAttribute("source").substring(1); // Убираем #
            
            if ("INPUT".equals(semantic)) {
                inputSource = source;
            } else if ("OUTPUT".equals(semantic)) {
                outputSource = source;
            }
        }
        
        // Парсим <channel>
        NodeList channels = anim.getElementsByTagName("channel");
        if (channels.getLength() == 0) return;
        
        Element channel = (Element) channels.item(0);
        String target = channel.getAttribute("target");
        
        // target формата: "ObjectName/transform" или "ObjectName/location.X"
        String[] targetParts = target.split("/");
        String objectName = targetParts[0];
        String property = targetParts.length > 1 ? targetParts[1] : "transform";
        
        float[] times = sources.get(inputSource);
        float[] values = sources.get(outputSource);
        
        if (times != null && values != null) {
            AnimationChannel animChannel = new AnimationChannel();
            animChannel.property = property;
            animChannel.times = times;
            animChannel.values = values;
            
            result.computeIfAbsent(objectName, k -> new ArrayList<>()).add(animChannel);
            MainRegistry.LOGGER.debug("Loaded animation channel: " + objectName + " -> " + property + 
            " (keyframes: " + times.length + ", duration: " + times[times.length - 1] + "s)");
        }
    }
    
    public static class AnimationChannel {
        public String property; // "location.X", "rotation_euler.Z", "transform"
        public float[] times; // Время в секундах
        public float[] values; // Значения
    
        // Линейная интерполяция
        public float getValue(float time) {
            if (times.length == 0) return 0;
            if (time <= times[0]) return values[0];
            if (time >= times[times.length - 1]) return values[values.length - 1];
    
            for (int i = 0; i < times.length - 1; i++) {
                if (time >= times[i] && time <= times[i + 1]) {
                    float alpha = (time - times[i]) / (times[i + 1] - times[i]);
                    return values[i] + alpha * (values[i + 1] - values[i]);
                }
            }
            return values[values.length - 1];
        }
    
        // Для матриц 4x4 (property == "transform")
        public float[] getMatrix(float time) {
            if (!property.equals("transform")) return null;
            
            // Найти ближайший keyframe
            int frameIndex = 0;
            for (int i = 0; i < times.length - 1; i++) {
                if (time >= times[i] && time <= times[i + 1]) {
                    // ИНТЕРПОЛЯЦИЯ МАТРИЦ - берем ближайший keyframe
                    float alpha = (time - times[i]) / (times[i + 1] - times[i]);
                    frameIndex = alpha < 0.5f ? i : i + 1;
                    break;
                }
            }
            if (time >= times[times.length - 1]) {
                frameIndex = times.length - 1;
            }
            
            // Извлечь матрицу 4x4 (16 float)
            float[] matrix = new float[16];
            int offset = frameIndex * 16;
            if (offset + 16 <= values.length) {
                System.arraycopy(values, offset, matrix, 0, 16);
            }
            return matrix;
        }
        
        // НОВЫЙ МЕТОД: Извлечь translation из матрицы 4x4
        public float[] getTranslationFromMatrix(float time) {
            float[] matrix = getMatrix(time);
            if (matrix == null) return new float[]{0, 0, 0};
            
            // В COLLADA матрицы column-major, translation в последней колонке
            return new float[]{
                matrix[12], // X
                matrix[13], // Y
                matrix[14]  // Z
            };
        }
    }    
}
