import org.gradle.kotlin.dsl.runtimeOnly

plugins {
	id("mod-platform")
	id("net.neoforged.moddev.legacyforge")
}

platform {
	loader = "forge"
	dependencies {
		required("minecraft") {
			forgeVersionRange = "[${prop("deps.minecraft")}]"
		}
		required("forge") {
			// Минимальная версия Forge: 47.3.31. Более старые версии Forge не смогут загрузить мод.
			forgeVersionRange = "[47.3.31,)"
		}
		optional("architectury") {
			slug("architectury-api")
			forgeVersionRange = "[${prop("deps.architectury")},)"
		}
	}
}

legacyForge {
	version = "${property("deps.minecraft")}-${property("deps.forge")}"
	if (hasProperty("deps.parchment")) parchment {
		val parchmentProp = property("deps.parchment") as String
		val parts = parchmentProp.split(":")
		mappingsVersion = parts[1]
		minecraftVersion = parts[0]
	}
	validateAccessTransformers = true
	accessTransformers.from(rootProject.file("src/main/resources/aw/${stonecutter.current.version}.cfg"))

	// ── UTF-8 для дев-логов ─────────────────────────────────────────────
	// Сгенерированный MDG clientLog4j2.xml не задаёт charset в PatternLayout —
	// файловые логи пишутся в платформенной Cp1251 (кириллица выглядит «broken»
	// при чтении как UTF-8). -Dfile.encoding через jvmArguments не спасает, если
	// запуск идёт мимо gradle-таска (IDE-конфиг со старыми аргументами), поэтому
	// патчим САМ конфиг: idempotent, выполняется перед каждым запуском. Файл
	// один и тот же для любого способа запуска (path зашит в launch-аргументах).
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
			println("patchDevLogCharset: charset=UTF-8 added to ${f}")
		}
	}
	tasks.matching { it.name in setOf("runClient", "runServer", "runGameTestServer", "runData") }.configureEach {
		doFirst { patchDevLogCharset() }
	}

	runs {
		register("client") {
			client()
			gameDirectory = file("run/")
			ideName = "Forge Client (${stonecutter.active?.version})"
			programArgument("--username=Dev")
			// user.language=en: локаль по умолчанию даёт русские месяцы в паттерне
			// даты log4j; в Cp1251-файле они читаются как «ёхэЄ» — лог ломается.
			// AllowEnhancedClassRedefinition: DCEVM в JetBrains Runtime — hot swap не только
			// тел методов, но и новых методов/полей/иерархии классов. Работает только при
			// запуске на JBR (Gradle JVM → jbr-17.0.11-dcevm), на обычном Hotspot флаг
			// игнорируется с предупреждением.
			jvmArguments.addAll("-Xmx4G", "-Xms2G", "-XX:+UnlockDiagnosticVMOptions", "-XX:+DebugNonSafepoints",
				"-XX:+IgnoreUnrecognizedVMOptions",
				"-XX:+AllowEnhancedClassRedefinition",
				"-Dfile.encoding=UTF-8", "-Dconsole.encoding=UTF-8",
				"-Duser.language=en", "-Duser.country=US")
		}
		register("server") {
			server()
			gameDirectory = file("run/")
			ideName = "Forge Server (${stonecutter.active?.version})"
			programArgument("--nogui")
			jvmArguments.addAll("-Xmx4G", "-Xms2G", "-Dfile.encoding=UTF-8",
				"-Duser.language=en", "-Duser.country=US")
		}
		// GameTest-сервер: headless-прогон всех @GameTest без GUI.
		register("gameTestServer") {
			server()
			// Выделенный gameDir: мир "Test Level" (flat) пересоздаётся каждый прогон,
			// чтобы условия тестов были идентичны на forge и neoforge и не зависели
			// от состояния общего run/ (клиент/сервер оставляют там свой мир с рельефом —
			// в нём арена оказывается под землёй, sky-тесты ломаются).
			gameDirectory = file("runGameTest/")
			ideName = "Forge GameTest (${stonecutter.active?.version})"
			systemProperty("forge.gameTestServer", "true")
			systemProperty("forge.enableGameTest", "true")
			jvmArguments.addAll("-Xmx12G", "-Xms2G", "-Dfile.encoding=UTF-8", "-Dconsole.encoding=UTF-8",
				"-Duser.language=en", "-Duser.country=US",
				// Точечный прогон одного класса: все тесты зарегистрированы в неймспейс hbm_m,
				// а тяжёлый интеграционный турбины - в tf_integration (см. TurbofanIntegrationGameTest).
				// Оверрайд неймспейса: -PgameTestNamespace=hbm_m (NukeProfileGameTest и др.)
				"-Dforge.enabledGameTestNamespaces=${project.findProperty("gameTestNamespace") ?: "tf_integration"}")
		}

		register("data") {
			data()
			gameDirectory = file("run/")
			ideName = "Forge Data (${stonecutter.active?.version})"
			jvmArguments.addAll("-Xmx4G", "-Xms2G", "-Dfile.encoding=UTF-8", "-Dconsole.encoding=UTF-8")
			val datagenOutput = rootProject.file("src/generated/resources")
			val existingResources = rootProject.file("src/main/resources")
			programArgument("--mod")
			programArgument(prop("mod.id"))
			programArgument("--all")
			programArgument("--output")
			programArgument(datagenOutput.absolutePath)
			programArgument("--existing")
			programArgument(existingResources.absolutePath)
		}
	}

	mods {
		register(prop("mod.id")) {
			sourceSet(sourceSets["main"])
		}
	}
}

// Регенерация мира гейм-тестов: каждый прогон стартует со свежим ванильным
// террейном (кратер ядерного профиля NukeProfileGameTest не должен портить
// следующие тесты, а автосейв на кратеризованном мире душил прогон).
tasks.matching { it.name == "runGameTestServer" }.configureEach {
	doFirst { delete("runGameTest/world") }
}

// Forge в собранном jar применяет AT только из META-INF/accesstransformer.cfg.
// accessTransformers{} в legacyForge{} ModDevGradle использует лишь для дев-окружения,
// поэтому пакуем per-версию cfg сами, а сырые aw/* из ресурсов вычищаем.
tasks.processResources {
	from(rootProject.file("src/main/resources/aw/${stonecutter.current.version}.cfg")) {
		rename { "accesstransformer.cfg" }
		into("META-INF")
	}
	exclude("aw/")
}

mixin {
	add(sourceSets.main.get(), prop("mod.mixin_refmap"))
	config(prop("mod.mixin_config"))
}

repositories {
	mavenLocal()
	mavenCentral()
	maven("https://maven.minecraftforge.net/") { name = "MinecraftForge" }
	maven("https://maven.parchmentmc.org") { name = "ParchmentMC" }
	maven("https://maven.shedaniel.me/") { name = "Shedaniel" }
	strictMaven("https://api.modrinth.com/maven", "maven.modrinth") { name = "Modrinth" }
	strictMaven("https://cursemaven.com", "curse.maven") { name = "CurseForge" }
	strictMaven("https://maven.architectury.dev/", "dev.architectury") { name = "Architectury" }
	// Create compat (опционально): Create, Flywheel. См. com.hbm_m.compat.create.
	maven("https://maven.createmod.net") { name = "Create" }
	maven("https://maven.ithundxr.dev/mirror") { name = "Ithundxr Mirror" }
	// Curios API (опционально): слот лица для противогазов. См. com.hbm_m.compat.curios.
	maven("https://maven.theillusivec4.top/") { name = "Illusive Soul Works" }
	flatDir { dirs(rootProject.file("libs")) }
}

dependencies {
	annotationProcessor("org.spongepowered:mixin:${libs.versions.mixin.get()}:processor")

	implementation(libs.moulberry.mixinconstraints)
	jarJar(libs.moulberry.mixinconstraints)

	"modImplementation"("dev.architectury:architectury-forge:${prop("deps.architectury")}")
	jarJar("dev.architectury:architectury-forge:${prop("deps.architectury")}")

	"modCompileOnly"("curse.maven:jei-238222:${prop("deps.jei")}")
	"modRuntimeOnly"("curse.maven:jei-238222:${prop("deps.jei")}")

	// Curios (опционально): API для компиляции, сам мод — в рантайм для тестов.
	"modCompileOnly"("top.theillusivec4.curios:curios-forge:5.14.1+1.20.1:api")
	"modRuntimeOnly"("top.theillusivec4.curios:curios-forge:5.14.1+1.20.1")

	"modCompileOnly"("com.simibubi.create:create-1.20.1:${prop("deps.create")}:slim") {
		isTransitive = false
	}
	// Sable (экосистема Create Aeronautics, существует только на 1.21.1+):
	// compileOnly для валидации строковых таргетов миксинов (полностью классы
	// не резолвятся: jar собран под 1.21.1). На 1.20.1 классы Sable в рантайме
	// отсутствуют -> миксины не применяются (no-op, мод не падает).
	compileOnly("maven.modrinth:sable:2.0.5+mc1.21.1")
	"modCompileOnly"("dev.engine-room.flywheel:flywheel-forge-api-1.20.1:${prop("deps.flywheel")}")
	// Embeddium падает в data-режиме (Minecraft.getInstance()==null при конструировании),
	// поэтому для runData запускаемся с -PnoClientMods, отключающим клиентские моды.
	if (!project.hasProperty("noClientMods")) {
		"modRuntimeOnly"("curse.maven:embeddium-908741:5681725")
		"modRuntimeOnly"("curse.maven:oculus-581495:6020952")
	}
	"modRuntimeOnly"("curse.maven:modernfix-790626:7515215")
	"modRuntimeOnly"("curse.maven:smooth-boot-reloaded-633412:5016280")
	
	"modRuntimeOnly"("curse.maven:screenshot-to-clipboard-326950:3643026")
	// "modRuntimeOnly"("maven.modrinth:cwoL6CqY:3PEwIAxS") // Item Transforms Helper
	// "modRuntimeOnly"("maven.modrinth:f3zK7pP5:8gUY8UiV") // Tick Freeze
	"modRuntimeOnly"("maven.modrinth:spark:1.10.53-forge")
	"modRuntimeOnly"("maven.modrinth:VYRu7qmG:QtSVNyjm") // Observable -  profiles (tile) entities and shows you what's taking up tick time and where.
	"modRuntimeOnly"("maven.modrinth:ordsPcFz:Zsh14XeQ") // Kotlin For Forge
	// "modRuntimeOnly"("curse.maven:konkrete-410295:5028413")
	// Distant Horizons: compileOnly для официального API (DhApiBeforeApplyShaderRenderEvent
	// и пр.) в com.hbm_m.client.compat.dh.DhRenderBridge. Класс моста грузится только при
	// установленном DH, поэтому отсутствие зависимости в рантайме безопасно.
	"modCompileOnly"("maven.modrinth:distanthorizons:3.3.2-1.20.1") // 3.3.2-1.20.1 (latest 2026-09-21)
	// "modRuntimeOnly"("maven.modrinth:distanthorizons:3.2.0-b-1.20.1")

	// "modRuntimeOnly"("curse.maven:xaeros-world-map-317780:7598469")
    // "modRuntimeOnly"("curse.maven:xaeros-minimap-263420:7598586")
	// Jade (WAILA fork): плагин com.hbm_m.compat.jade. compileOnly берёт ТОТ ЖЕ файл, что и
	// рантайм, чтобы API плагина гарантированно совпадал с тем, под чем запускаемся. Классы
	// плагина грузит только сканер @WailaPlugin самого Jade — без Jade они не загружаются.
	"modCompileOnly"("curse.maven:jade-324717:6855440")
	"modRuntimeOnly"("curse.maven:jade-324717:6855440")
}

sourceSets {
	main {
		resources.srcDir(rootProject.file("src/generated/resources"))
	}
}

tasks.named("createMinecraftArtifacts") {
	dependsOn(tasks.named("stonecutterGenerate"))
}

// Датаген и GameTest'ы нужны только в dev-ранах (runData / gameTestServer работают
// из classes-директории, не из jar) — в продакшен-jar они не попадают.
// reobfJar (публикуемый артефакт) строится поверх этого jar и наследует исключения.
tasks.named<Jar>("jar") {
	exclude("com/hbm_m/datagen/**", "com/hbm_m/test/**")
}

tasks.withType<JavaCompile>().configureEach {
	options.encoding = "UTF-8"
	// See build.neoforge.gradle.kts: the forked compiler worker needs a real heap.
	options.forkOptions.memoryMaximumSize = "3g"
}

tasks.withType<Test>().configureEach {
	failOnNoDiscoveredTests = false
}

tasks.register<JavaExec>("runMilestone1RemedTest") {
	dependsOn(tasks.named("compileTestJava"))
	classpath = sourceSets["test"].runtimeClasspath + configurations["compileClasspath"]
	mainClass.set("com.hbm_m.test.Milestone1RemediationTest")
	jvmArgs("-ea")
}

tasks.register<JavaExec>("runSpatialLightingTest") {
	dependsOn(tasks.named("compileTestJava"))
	classpath = sourceSets["test"].runtimeClasspath + configurations["compileClasspath"]
	mainClass.set("com.hbm_m.test.MultiblockSpatialLightingTest")
	jvmArgs("-ea")
}

stonecutter {
}

// Мир "Test Level" пересоздаётся каждый прогон gameTestServer — идентичные
// условия арен на forge и neoforge (fresh flat-мир, открытый скай на y≈-60).
tasks.matching { it.name == "runGameTestServer" }.configureEach {
	doFirst {
		file("runGameTest").mkdirs()
		// Гейтест-сервер хранит мир в runGameTest/world (level-name из server.properties),
		// а НЕ в "Test Level" - форс-загруженные чанки арен сохраняются в level.dat, и
		// неубитый мир копит их от упавших прогонов, пока сервер не начнёт грузить
		// сотни тысяч чанков на старте и не умрёт по OOM.
		delete(file("runGameTest/Test Level"))
		delete(file("runGameTest/world"))
		file("runGameTest/eula.txt").writeText("eula=true")
	}
}

// ── Автономный HotSwap v2 (с фильтрацией по SHA-256 байткода против пересборок Stonecutter) ──
fun buildAutoHotswapAgentJar(): File {
	val dir = layout.buildDirectory.dir("hotswap-agent").get().asFile
	val jarFile = File(dir, "hbm-auto-hotswap-agent-v2.jar")
	if (jarFile.exists()) return jarFile

	val srcDir = File(dir, "src").apply { mkdirs() }
	val classesDir = File(dir, "classes").apply { deleteRecursively(); mkdirs() }
	val javaFile = File(srcDir, "HbmAutoHotswapAgent.java")
	javaFile.writeText("""
		import java.io.File;
		import java.lang.instrument.ClassDefinition;
		import java.lang.instrument.Instrumentation;
		import java.nio.file.*;
		import java.security.MessageDigest;
		import java.util.*;
		import java.util.stream.Stream;

		public class HbmAutoHotswapAgent {
		    private static final Map<Path, Long> timestamps = new HashMap<>();
		    private static final Map<Path, byte[]> bytecodeHashes = new HashMap<>();

		    public static void premain(String agentArgs, Instrumentation inst) {
		        if (agentArgs == null || agentArgs.isEmpty()) return;
		        List<Path> roots = new ArrayList<>();
		        for (String part : agentArgs.split(File.pathSeparator)) {
		            if (!part.isBlank()) roots.add(Paths.get(part));
		        }
		        scan(roots, false);
		        Thread watcher = new Thread(() -> loop(roots, inst), "HBM-AutoHotSwap");
		        watcher.setDaemon(true);
		        watcher.start();
		        System.out.println("⚡ [HBM-HotSwap v2] Автономный SHA-256 вотчер запущен: " + roots);
		    }

		    private static byte[] sha256(byte[] data) {
		        try {
		            return MessageDigest.getInstance("SHA-256").digest(data);
		        } catch (Exception e) {
		            return data;
		        }
		    }

		    private static Map<Path, byte[]> scan(List<Path> roots, boolean collectChanged) {
		        Map<Path, byte[]> changedBytes = new LinkedHashMap<>();
		        for (Path root : roots) {
		            if (!Files.isDirectory(root)) continue;
		            try (Stream<Path> stream = Files.walk(root)) {
		                stream.filter(p -> p.toString().endsWith(".class") && Files.isRegularFile(p)).forEach(p -> {
		                    try {
		                        long mod = Files.getLastModifiedTime(p).toMillis();
		                        Long prevMod = timestamps.get(p);
		                        if (prevMod == null || mod > prevMod) {
		                            byte[] bytes = Files.readAllBytes(p);
		                            if (bytes.length == 0) return;
		                            byte[] hash = sha256(bytes);
		                            byte[] prevHash = bytecodeHashes.put(p, hash);
		                            timestamps.put(p, mod);
		                            // Перезагружаем ТОЛЬКО если реально изменился байткод класса!
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

		    private static void loop(List<Path> roots, Instrumentation inst) {
		        while (true) {
		            try {
		                Thread.sleep(500);
		                Map<Path, byte[]> changed = new LinkedHashMap<>(scan(roots, true));
		                if (changed.isEmpty()) continue;
		                Thread.sleep(250); // Ждем завершения записи javac
		                changed.putAll(scan(roots, true));

		                Map<String, List<Class<?>>> loadedByName = new HashMap<>();
		                for (Class<?> c : inst.getAllLoadedClasses()) {
		                    if (inst.isModifiableClass(c)) {
		                        loadedByName.computeIfAbsent(c.getName(), k -> new ArrayList<>()).add(c);
		                    }
		                }

		                List<ClassDefinition> defs = new ArrayList<>();
		                List<String> names = new ArrayList<>();
		                for (Map.Entry<Path, byte[]> entry : changed.entrySet()) {
		                    Path file = entry.getKey();
		                    byte[] bytes = entry.getValue();
		                    for (Path root : roots) {
		                        if (file.startsWith(root)) {
		                            String rel = root.relativize(file).toString().replace('\\', '.').replace('/', '.');
		                            String className = rel.substring(0, rel.length() - 6);
		                            List<Class<?>> targets = loadedByName.get(className);
		                            if (targets != null && !targets.isEmpty()) {
		                                for (Class<?> target : targets) {
		                                    defs.add(new ClassDefinition(target, bytes));
		                                }
		                                if (!names.contains(className)) names.add(className);
		                            }
		                            break;
		                        }
		                    }
		                }

		                if (!defs.isEmpty()) {
		                    long t0 = System.currentTimeMillis();
		                    try {
		                        inst.redefineClasses(defs.toArray(new ClassDefinition[0]));
		                        long dt = System.currentTimeMillis() - t0;
		                        System.out.println("⚡ [HBM-HotSwap] Перезагружено реально измененных классов: " + names.size() + " за " + dt + " мс -> " + names);
		                    } catch (Throwable batchErr) {
		                        for (ClassDefinition def : defs) {
		                            try {
		                                inst.redefineClasses(def);
		                                System.out.println("⚡ [HBM-HotSwap] Перезагружен: " + def.getDefinitionClass().getName());
		                            } catch (Throwable singleErr) {
		                                System.err.println("❌ [HBM-HotSwap] Ошибка замены " + def.getDefinitionClass().getName() + ": " + singleErr);
		                            }
		                        }
		                    }
		                }
		            } catch (Throwable t) {
		                t.printStackTrace();
		            }
		        }
		    }
		}
	""".trimIndent())

	val compiler = javax.tools.ToolProvider.getSystemJavaCompiler()
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

tasks.withType<JavaExec>().matching { it.name == "runClient" }.configureEach {
	val targetJava = if (stonecutter.current.version == "1.20.1") 17 else 21
	// Пытаемся взять JBR (с DCEVM); если у разработчика её нет и автоскачивание выключено —
	// оставляем дефолтную JDK, чтобы runClient всегда запускался в 100% случаев.
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

		// 2. Автономный SHA-256 вотчер классов (работает на любой JDK!)
		val watcherJar = buildAutoHotswapAgentJar()
		val classesDirs = sourceSets["main"].output.classesDirs.files
			.joinToString(File.pathSeparator) { it.absolutePath }
		jvmArgs("-javaagent:${watcherJar.absolutePath}=$classesDirs")
	}
}