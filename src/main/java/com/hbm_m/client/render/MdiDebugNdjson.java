package com.hbm_m.client.render;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * NDJSON debug ingest for MDI investigation (session 92ec77). Do not log secrets.
 */
public final class MdiDebugNdjson {

    private static final String SESSION = "92ec77";
    private static volatile Path cachedPath;

    private static Path logFile() {
        Path c = cachedPath;
        if (c != null) return c;
        synchronized (MdiDebugNdjson.class) {
            if (cachedPath != null) return cachedPath;
            Path p = Path.of(System.getProperty("user.dir", ".")).normalize();
            Path found = p.resolve("debug-92ec77.log");
            for (int i = 0; i < 12 && p != null; i++) {
                if (Files.exists(p.resolve("settings.gradle.kts"))
                        || Files.exists(p.resolve("gradlew.bat"))) {
                    found = p.resolve("debug-92ec77.log");
                    break;
                }
                p = p.getParent();
            }
            cachedPath = found;
            return cachedPath;
        }
    }

    // #region agent log
    public static void log(String hypothesisId, String location, String message, String jsonDataObjectBody) {
        try {
            long ts = System.currentTimeMillis();
            String data = (jsonDataObjectBody == null || jsonDataObjectBody.isEmpty()) ? "{}" : jsonDataObjectBody;
            String line = "{\"sessionId\":\"" + SESSION + "\",\"hypothesisId\":\"" + esc(hypothesisId)
                    + "\",\"location\":\"" + esc(location) + "\",\"message\":\"" + esc(message)
                    + "\",\"data\":" + data + ",\"timestamp\":" + ts + "}\n";
            Files.writeString(logFile(), line, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Throwable ignored) {
        }
    }
    // #endregion agent log

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private MdiDebugNdjson() {}
}
