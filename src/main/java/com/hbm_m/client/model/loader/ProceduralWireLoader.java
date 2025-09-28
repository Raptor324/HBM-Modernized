package com.hbm_m.client.model.loader;

// Загрузчик модели для процедурных проводов.
// Он просто возвращает новый экземпляр ProceduralWireGeometry, так как вся геометрия генерируется программно.
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.hbm_m.client.model.ProceduralWireGeometry;
import net.minecraftforge.client.model.geometry.IGeometryLoader;

public class ProceduralWireLoader implements IGeometryLoader<ProceduralWireGeometry> {
    @Override
    public ProceduralWireGeometry read(JsonObject jsonObject, JsonDeserializationContext context) {
        return new ProceduralWireGeometry();
    }
}