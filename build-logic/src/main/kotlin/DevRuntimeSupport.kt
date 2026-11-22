// file: build-logic/src/main/kotlin/DevRuntimeSupport.kt
import org.gradle.api.Project
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.jvm.toolchain.JvmVendorSpec
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.the
import org.gradle.kotlin.dsl.withGroovyBuilder
import org.gradle.kotlin.dsl.withType
import java.io.File
import javax.tools.ToolProvider

fun Project.registerDevRuntimeSupport(mcVersion: String) {
	// 1. UTF-8 патчер для clientLog4j2.xml
	fun patchDevLogCharset() {
		val f = file("build/moddev/clientLog4j2.xml")
		if (!f.exists()) return
		var text = f.readText()
		val cleaned = text.replace("charset=\"UTF-8\" charset=\"UTF-8\"", "charset=\"UTF-8\"")
		if (cleaned != text) {
			f.writeText(cleaned)
			text = cleaned
		}
		if (text.contains("charset=\"UTF-8\"")) return
		val patched = Regex("<PatternLayout(?![^>]*\\bcharset=)").replace(text, "<PatternLayout charset=\"UTF-8\"")
		if (patched != text) {
			f.writeText(patched)
			println("patchDevLogCharset: charset=UTF-8 added to $f")
		}
	}

	tasks.matching { it.name in setOf("runClient", "runServer", "runGameTestServer", "runData") }.configureEach {
		doFirst { patchDevLogCharset() }
	}

	// 2. Очистка мира перед runGameTestServer
	tasks.matching { it.name == "runGameTestServer" }.configureEach {
		doFirst {
			file("runGameTest").mkdirs()
			delete(file("runGameTest/Test Level"))
			delete(file("runGameTest/world"))
			file("runGameTest/eula.txt").writeText("eula=true")
		}
	}

	// 3. Автономный HotSwap v14 (SHA-256 + защита от бага G1GC/DCEVM при изменении инстанс-полей)
	val javaToolchains = extensions.getByType(JavaToolchainService::class.java)
	val sourceSets = the<SourceSetContainer>()

	tasks.withType<JavaExec>().matching { it.name == "runClient" }.configureEach {
		val targetJava = if (mcVersion == "1.20.1") 17 else 21
		val jbrLauncher = javaToolchains.launcherFor {
			languageVersion.set(JavaLanguageVersion.of(targetJava))
			vendor.set(JvmVendorSpec.JETBRAINS)
		}
		javaLauncher.set(provider {
			runCatching { jbrLauncher.get() }.getOrElse {
				logger.warn("⚠️ [HotSwap] JBR $targetJava не найдена, откат на стандартную JDK (хотсвап тел методов будет работать, но без добавления новых полей/методов).")
				javaToolchains.launcherFor {
					languageVersion.set(JavaLanguageVersion.of(targetJava))
				}.get()
			}
		})

		doFirst {
			// 1. Mixin Agent для хотсвапа миксинов
			val mixinJar = classpath.files.firstOrNull { file ->
				file.name.endsWith(".jar") && (
					file.name.startsWith("sponge-mixin-") ||
						file.name.matches(Regex("mixin-\\d+.*\\.jar"))
					)
			}
			if (mixinJar != null) {
				jvmArgs("-javaagent:${mixinJar.absolutePath}")
			}

			// 2. Автономный SHA-256 вотчер классов
			val watcherJar = buildAutoHotswapAgentJar()
			val classesDirs = sourceSets["main"].output.classesDirs.files
				.joinToString(File.pathSeparator) { it.absolutePath }
			jvmArgs("-javaagent:${watcherJar.absolutePath}=$classesDirs")
		}
	}
}

private fun Project.buildAutoHotswapAgentJar(): File {
	val dir = layout.buildDirectory.dir("hotswap-agent").get().asFile
	val jarFile = File(dir, "hbm-auto-hotswap-agent-v14.jar")
	if (jarFile.exists()) return jarFile

	val srcDir = File(dir, "src").apply { mkdirs() }
	val classesDir = File(dir, "classes").apply { deleteRecursively(); mkdirs() }
	val javaFile = File(srcDir, "HbmAutoHotswapAgent.java")
	javaFile.writeText(HOTSWAP_AGENT_JAVA_SOURCE)

	val compiler = ToolProvider.getSystemJavaCompiler()
		?: error("SystemJavaCompiler not found (run Gradle on a JDK, not JRE)")
	val rc = compiler.run(null, null, null, "--release", "17", "-d", classesDir.absolutePath, javaFile.absolutePath)
	if (rc != 0) error("Failed to compile HbmAutoHotswapAgent")

	val manifestFile = File(dir, "MANIFEST.MF")
	manifestFile.writeText(
		"Manifest-Version: 1.0\n" +
			"Premain-Class: HbmAutoHotswapAgent\n" +
			"Can-Redefine-Classes: true\n" +
			"Can-Retransform-Classes: true\n\n"
	)
	ant.withGroovyBuilder {
		"jar"("destfile" to jarFile, "basedir" to classesDir, "manifest" to manifestFile)
	}
	return jarFile
}

private val HOTSWAP_AGENT_JAVA_SOURCE = """
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Stream;

public class HbmAutoHotswapAgent {
    private static final Map<Path, Long> timestamps = new HashMap<>();
    private static final Map<Path, byte[]> bytecodeHashes = new HashMap<>();
    private static final boolean ALLOW_SCHEMA_CHANGE =
            Boolean.getBoolean("hbm.hotswap.allowSchemaChange");
    private static Instrumentation s_inst;

    public static void premain(String agentArgs, Instrumentation inst) {
        if (agentArgs == null || agentArgs.isEmpty()) return;
        List<Path> roots = new ArrayList<>();
        for (String part : agentArgs.split(File.pathSeparator)) {
            if (!part.isBlank()) roots.add(Paths.get(part));
        }
        s_inst = inst;
        scan(roots, false);
        Thread watcher = new Thread(() -> loop(roots, inst), "HBM-AutoHotSwap");
        watcher.setDaemon(true);
        watcher.start();
        System.out.println("[HBM-HotSwap v14] Autonomous SHA-256 watcher started: " + roots);
    }

    private static byte[] sha256(byte[] data) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(data);
        } catch (Exception e) {
            return data;
        }
    }

    private static boolean isCompiling(List<Path> roots) {
        for (Path root : roots) {
            File rf = root.toFile();
            if (!rf.isDirectory()) continue;
            if (new File(rf, ".compiling").exists()) return true;
            File p1 = rf.getParentFile();
            if (p1 != null && new File(p1, ".compiling").exists()) return true;
            File p2 = p1 != null ? p1.getParentFile() : null;
            if (p2 != null && new File(p2, ".compiling").exists()) return true;
        }
        return false;
    }

    private static boolean hasModifiedFiles(List<Path> roots) {
        if (isCompiling(roots)) return false;
        for (Path root : roots) {
            if (!root.toFile().isDirectory()) continue;
            try (Stream<Path> stream = Files.walk(root)) {
                boolean found = stream
                        .filter(p -> p.toString().endsWith(".class"))
                        .anyMatch(p -> {
                            File f = p.toFile();
                            if (!f.isFile()) return false;
                            long mod = f.lastModified();
                            Long prevMod = timestamps.get(p);
                            return prevMod == null || mod > prevMod;
                        });
                if (found) return true;
            } catch (Exception ignored) {}
        }
        return false;
    }

    private static Map<Path, byte[]> scan(List<Path> roots, boolean collectChanged) {
        if (isCompiling(roots)) return Collections.emptyMap();
        Map<Path, byte[]> changedBytes = new LinkedHashMap<>();
        for (Path root : roots) {
            if (!root.toFile().isDirectory()) continue;
            try (Stream<Path> stream = Files.walk(root)) {
                stream.filter(p -> p.toString().endsWith(".class")).forEach(p -> {
                    try {
                        File f = p.toFile();
                        if (!f.isFile()) return;
                        long mod = f.lastModified();
                        Long prevMod = timestamps.get(p);
                        if (prevMod == null || mod > prevMod) {
                            byte[] bytes = Files.readAllBytes(p);
                            if (bytes.length < 10
                                    || (bytes[0] & 0xFF) != 0xCA
                                    || (bytes[1] & 0xFF) != 0xFE
                                    || (bytes[2] & 0xFF) != 0xBA
                                    || (bytes[3] & 0xFF) != 0xBE) {
                                return;
                            }
                            byte[] hash = sha256(bytes);
                            byte[] prevHash = bytecodeHashes.put(p, hash);
                            timestamps.put(p, mod);
                            if (collectChanged && (prevHash == null || !MessageDigest.isEqual(hash, prevHash))) {
                                changedBytes.put(p, bytes);
                            }
                        }
                    } catch (Exception ignored) {}
                });
            } catch (Exception ignored) {}
        }
        return changedBytes;
    }

    private static List<String> extractInstanceFieldsFromBytecode(byte[] bytes) throws Exception {
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes));
        if (in.readInt() != 0xCAFEBABE) throw new IllegalArgumentException("Bad magic");
        in.readUnsignedShort();
        in.readUnsignedShort();
        int cpCount = in.readUnsignedShort();
        String[] utf8 = new String[cpCount];
        for (int i = 1; i < cpCount; i++) {
            int tag = in.readUnsignedByte();
            switch (tag) {
                case 1 -> utf8[i] = in.readUTF();
                case 3, 4, 9, 10, 11, 12, 17, 18 -> in.skipBytes(4);
                case 5, 6 -> { in.skipBytes(8); i++; }
                case 7, 8, 16, 19, 20 -> in.skipBytes(2);
                case 15 -> in.skipBytes(3);
                default -> throw new IllegalArgumentException("Unknown CP tag: " + tag);
            }
        }
        in.readUnsignedShort();
        in.readUnsignedShort();
        in.readUnsignedShort();
        int ifaceCount = in.readUnsignedShort();
        in.skipBytes(ifaceCount * 2);
        int fieldsCount = in.readUnsignedShort();
        List<String> instanceFields = new ArrayList<>(fieldsCount);
        for (int i = 0; i < fieldsCount; i++) {
            int access = in.readUnsignedShort();
            int nameIdx = in.readUnsignedShort();
            int descIdx = in.readUnsignedShort();
            int attrCount = in.readUnsignedShort();
            for (int a = 0; a < attrCount; a++) {
                in.readUnsignedShort();
                int len = in.readInt();
                in.skipBytes(len);
            }
            if ((access & Modifier.STATIC) == 0) {
                instanceFields.add(utf8[nameIdx] + ":" + utf8[descIdx]);
            }
        }
        return instanceFields;
    }

    private static boolean hasInstanceFieldLayoutChanged(Class<?> target, byte[] newBytes) {
        if (ALLOW_SCHEMA_CHANGE) return false;
        try {
            List<String> newFields = extractInstanceFieldsFromBytecode(newBytes);
            List<String> oldFields = new ArrayList<>();
            for (Field f : target.getDeclaredFields()) {
                if (!Modifier.isStatic(f.getModifiers())) {
                    oldFields.add(f.getName() + ":" + f.getType().descriptorString());
                }
            }
            return !oldFields.equals(newFields);
        } catch (Throwable t) {
            return false;
        }
    }

    private static Object findMemberByReturn(Object owner, String returnSimpleName, String debugTag) {
        for (Class<?> cls = owner.getClass(); cls != null && cls != Object.class; cls = cls.getSuperclass()) {
            try {
                for (Field f : cls.getDeclaredFields()) {
                    String ft;
                    try {
                        ft = f.getType().getSimpleName();
                    } catch (Throwable t) {
                        continue;
                    }
                    if (ft.equals(returnSimpleName)) {
                        try {
                            f.setAccessible(true);
                            Object r = f.get(owner);
                            if (r != null) return r;
                        } catch (Throwable ignored) {}
                    }
                }
            } catch (Throwable t) {
                System.out.println("[HBM-HotSwap] postChat: " + debugTag + ": field scan failed on " + cls.getName() + ": " + t);
            }
            Method[] methods = null;
            try {
                methods = cls.getDeclaredMethods();
            } catch (Throwable t) {
                System.out.println("[HBM-HotSwap] postChat: " + debugTag + ": getDeclaredMethods failed on " + cls.getName() + ": " + t);
            }
            if (methods != null) {
                for (Method m : methods) {
                    if (m.getParameterCount() != 0) continue;
                    String rt;
                    try {
                        rt = m.getReturnType().getSimpleName();
                    } catch (Throwable t) {
                        continue;
                    }
                    if (rt.equals(returnSimpleName)) {
                        try {
                            m.setAccessible(true);
                            Object r = m.invoke(owner);
                            if (r != null) return r;
                        } catch (Throwable ignored) {}
                    }
                }
            }
        }
        return null;
    }

    private static void sendChatLine(Object chat, Class<?> component, String text) {
        try {
            Object msg = component.getMethod("literal", String.class).invoke(null, text);
            for (Method m : chat.getClass().getMethods()) {
                if (m.getParameterCount() == 1
                        && m.getParameterTypes()[0].getName().equals(component.getName())) {
                    m.invoke(chat, msg);
                    return;
                }
            }
        } catch (Throwable t) {
            System.out.println("[HBM-HotSwap] postChat line failed: " + t);
        }
    }

    private static void postChat(String text) {
        try {
            if (s_inst == null) return;
            Class<?> mcClass = null;
            for (Class<?> c : s_inst.getAllLoadedClasses()) {
                if (c.getName().equals("net.minecraft.client.Minecraft")) {
                    mcClass = c;
                    break;
                }
            }
            if (mcClass == null) return;
            Object mc = null;
            for (Method m : mcClass.getDeclaredMethods()) {
                if (Modifier.isStatic(m.getModifiers())
                        && m.getParameterCount() == 0 && m.getReturnType() == mcClass) {
                    try {
                        m.setAccessible(true);
                        mc = m.invoke(null);
                    } catch (Throwable ignored) {}
                    if (mc != null) break;
                }
            }
            if (mc == null) return;
            final Object mcRef = mc;
            Runnable task = () -> {
                try {
                    Object gui = findMemberByReturn(mcRef, "Gui", "Gui");
                    if (gui == null) return;
                    Object chat = findMemberByReturn(gui, "ChatComponent", "chat");
                    if (chat == null) return;
                    Class<?> component = Class.forName("net.minecraft.network.chat.Component",
                            true, chat.getClass().getClassLoader());
                    sendChatLine(chat, component, text);
                } catch (Throwable t) {
                    System.out.println("[HBM-HotSwap] postChat task failed: " + t);
                }
            };
            for (Method m : mcClass.getMethods()) {
                if (m.getName().equals("execute") && m.getParameterCount() == 1
                        && m.getParameterTypes()[0] == Runnable.class) {
                    m.invoke(mcRef, task);
                    return;
                }
            }
            task.run();
        } catch (Throwable t) {
            System.out.println("[HBM-HotSwap] postChat failed: " + t);
        }
    }

    private static void loop(List<Path> roots, Instrumentation inst) {
        while (true) {
            try {
                Thread.sleep(500);
                if (!hasModifiedFiles(roots)) continue;
                Thread.sleep(250);
                Map<Path, byte[]> changed = new LinkedHashMap<>(scan(roots, true));
                if (changed.isEmpty()) continue;

                Map<String, List<Class<?>>> loadedByName = new HashMap<>();
                for (Class<?> c : inst.getAllLoadedClasses()) {
                    if (inst.isModifiableClass(c)) {
                        loadedByName.computeIfAbsent(c.getName(), k -> new ArrayList<>()).add(c);
                    }
                }

                List<ClassDefinition> defs = new ArrayList<>();
                List<String> names = new ArrayList<>();
                List<String> skippedSchema = new ArrayList<>();
                for (Map.Entry<Path, byte[]> entry : changed.entrySet()) {
                    Path file = entry.getKey();
                    byte[] bytes = entry.getValue();
                    for (Path root : roots) {
                        if (file.startsWith(root)) {
                            String rel = root.relativize(file).toString().replace('\\', '.').replace('/', '.');
                            String className = rel.substring(0, rel.length() - 6);
                            List<Class<?>> targets = loadedByName.get(className);
                            if (targets != null && !targets.isEmpty()) {
                                boolean layoutChanged = false;
                                for (Class<?> target : targets) {
                                    if (hasInstanceFieldLayoutChanged(target, bytes)) {
                                        layoutChanged = true;
                                        break;
                                    }
                                }
                                if (layoutChanged) {
                                    if (!skippedSchema.contains(className)) skippedSchema.add(className);
                                } else {
                                    for (Class<?> target : targets) {
                                        defs.add(new ClassDefinition(target, bytes));
                                    }
                                    if (!names.contains(className)) names.add(className);
                                }
                            }
                            break;
                        }
                    }
                }

                if (!skippedSchema.isEmpty()) {
                    System.out.println("[HBM-HotSwap] Skipped classes with instance field changes (G1/DCEVM crash guard): " + skippedSchema);
                    StringBuilder warnMsg = new StringBuilder("\u00a76\u00a7l[HBM-HotSwap]\u00a7r \u00a7eSkipped (instance fields changed, restart required):");
                    for (String n : skippedSchema) warnMsg.append("\n  \u00a7c\u2022\u00a7r ").append(n);
                    postChat(warnMsg.toString());
                }

                if (!defs.isEmpty()) {
                    long t0 = System.currentTimeMillis();
                    try {
                        inst.redefineClasses(defs.toArray(new ClassDefinition[0]));
                        long dt = System.currentTimeMillis() - t0;
                        System.out.println("[HBM-HotSwap] Reloaded changed classes: " + names.size() + " in " + dt + " ms -> " + names);
                        StringBuilder chatMsg = new StringBuilder("\u00a76\u00a7l[HBM-HotSwap]\u00a7r \u00a7aReloaded \u00a7e"
                                + names.size() + " class(es)\u00a7a in \u00a7e" + dt + " ms\u00a7r");
                        for (String n : names) chatMsg.append("\n  \u00a77\u2022\u00a7r ").append(n);
                        postChat(chatMsg.toString());
                    } catch (Throwable batchErr) {
                        List<String> ok = new ArrayList<>();
                        for (ClassDefinition def : defs) {
                            try {
                                inst.redefineClasses(def);
                                ok.add(def.getDefinitionClass().getName());
                                System.out.println("[HBM-HotSwap] Reloaded: " + def.getDefinitionClass().getName());
                            } catch (Throwable singleErr) {
                                System.err.println("[HBM-HotSwap] Redefine failed for " + def.getDefinitionClass().getName() + ": " + singleErr);
                                postChat("\u00a7c[HBM-HotSwap] Redefine failed: " + def.getDefinitionClass().getName());
                            }
                        }
                        if (!ok.isEmpty()) {
                            StringBuilder chatMsg = new StringBuilder("\u00a76\u00a7l[HBM-HotSwap]\u00a7r \u00a7aReloaded \u00a7e"
                                    + ok.size() + " class(es)\u00a7r \u00a77(fallback mode)");
                            for (String n : ok) chatMsg.append("\n  \u00a77\u2022\u00a7r ").append(n);
                            postChat(chatMsg.toString());
                        }
                    }
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }
}
""".trimIndent()
