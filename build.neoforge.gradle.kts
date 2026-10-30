plugins {
	id("mod-platform")
	id("net.neoforged.moddev")
}

platform {
	loader = "neoforge"
	dependencies {
		required("minecraft") {
			forgeVersionRange = "[${prop("deps.minecraft")}]"
		}
		required("neoforge") {
			forgeVersionRange = "[1,)"
		}
		required("architectury") {
			slug("architectury-api")
			forgeVersionRange = "[${prop("deps.architectury")},)"
		}
		// Create — опциональная совместимость (рендер блоков на поездах + двери на контрапшенах).
		// Зеркало optional-зависимости из src/main/resources/META-INF/mods.toml.
		optional("create") {
			slug("create")
			forgeVersionRange = "[6.0.0,6.1.0)"
		}
	}
}

neoForge {
	version = property("deps.neoforge") as String
	accessTransformers.from(rootProject.file("src/main/resources/aw/${stonecutter.current.version}.cfg"))
	validateAccessTransformers = true

	if (hasProperty("deps.parchment")) parchment {
		val (mc, ver) = (property("deps.parchment") as String).split(':')
		mappingsVersion = ver
		minecraftVersion = mc
	}

	// ── UTF-8 для дев-логов ─────────────────────────────────────────────
	// Сгенерированный MDG log4j2-конфиг не задаёт charset в PatternLayout —
	// файловые логи пишутся в платформенной Cp1251. Патчим конфиг idempotent-
	// но перед каждым запуском (файл общий для любого способа запуска).
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
	tasks.matching { it.name in setOf("runClient", "runServer", "runGameTestServer") }.configureEach {
		doFirst { patchDevLogCharset() }
	}

	runs {
		register("client") {
			client()
			gameDirectory = file("run/")
			ideName = "NeoForge Client (${stonecutter.current.version})"
			programArgument("--username=Dev")
			// UTF-8 для логов + ASCII-локаль (иначе log4j пишет даты «сен.» в
			// платформенной Cp1251 — debug.log открывается как ломаный UTF-8).
			// AllowEnhancedClassRedefinition: DCEVM в JetBrains Runtime — hot swap
			// новых методов/полей, не только тел методов. См. тот же флаг в forge-конфиге.
			jvmArguments.addAll("-Xmx4G", "-Xms2G", "-XX:+UnlockDiagnosticVMOptions", "-XX:+DebugNonSafepoints",
				"-XX:+IgnoreUnrecognizedVMOptions",
				"-XX:+AllowEnhancedClassRedefinition",
				"-Dfile.encoding=UTF-8", "-Dconsole.encoding=UTF-8",
				"-Duser.language=en", "-Duser.country=US")
		}
		register("server") {
			server()
			gameDirectory = file("run/")
			ideName = "NeoForge Server (${stonecutter.current.version})"
			jvmArguments.addAll("-Xmx4G", "-Xms2G", "-Dfile.encoding=UTF-8",
				"-Duser.language=en", "-Duser.country=US")
		}
		// GameTest-сервер: headless-прогон всех @GameTest без GUI.
		// NeoForge 1.21.1 НЕ поддерживает аргумент CLI --gametest (это Forge-only).
		// Вместо этого пропатченный Main.main() читает СИСТЕМНОЕ СВОЙСТВО JVM
		// "neoforge.gameTestServer" и при true запускает GameTestServer.create(...)
		// "neoforge.enableGameTest" дополнительно активирует регистрацию тестов в dev-среде.
		// Шаблоны (empty3x3x3/empty5x5x5) — ванильные; RegisterGameTestsEvent регистрирует
		// классы test-методов (тесты берутся из GameTestRegistry.getAllTestFunctions()).
		// Запуск: ./gradlew :1.21.1-neoforge:runGameTestServer
		register("gameTestServer") {
			server()
			// Выделенный gameDir: мир "Test Level" (flat) пересоздаётся каждый прогон —
			// идентичные условия арен на forge и neoforge (см. комментарий в build.forge.gradle.kts).
			gameDirectory = file("runGameTest/")
			ideName = "NeoForge GameTest (${stonecutter.active?.version})"
			systemProperty("neoforge.gameTestServer", "true")
			systemProperty("neoforge.enableGameTest", "true")
				jvmArguments.addAll("-Xmx12G", "-Xms2G", "-Dfile.encoding=UTF-8",
					"-Duser.language=en", "-Duser.country=US",
					// Точечный прогон одного класса: все тесты зарегистрированы в неймспейс hbm_m,
					// а тяжёлый интеграционный турбины - в tf_integration (см. TurbofanIntegrationGameTest).
					// Оверрайд неймспейса: -PgameTestNamespace=hbm_m (NukeProfileGameTest и др.)
					"-Dneoforge.enabledGameTestNamespaces=${project.findProperty("gameTestNamespace") ?: "tf_integration"}")
			// Оверрайды NukeProfileGameTest: -PnukeStrength=300 -PnukeBudget=15
			if (project.hasProperty("nukeStrength")) systemProperty("hbm.nukeProfileStrength", project.property("nukeStrength") as String)
			if (project.hasProperty("nukeBudget")) systemProperty("hbm.nukeProfileBudgetMinutes", project.property("nukeBudget") as String)
		}
	}

	// Регенерация мира гейм-тестов: каждый прогон стартует со свежим ванильным
	// террейном (кратер ядерного профиля NukeProfileGameTest не должен портить
	// следующие тесты, а автосейв на кратеризованном мире душил прогон).
	tasks.matching { it.name == "runGameTestServer" }.configureEach {
		doFirst { delete("runGameTest/world") }
	}

	mods {
		register(property("mod.id") as String) {
			sourceSet(sourceSets["main"])
		}
	}
	sourceSets["main"].resources.srcDir(rootProject.file("src/generated/resources"))
}

// NeoForge в собранном jar применяет AT только из META-INF/accesstransformer.cfg.
// accessTransformers{} в neoForge{} ModDevGradle использует лишь для дев-окружения,
// поэтому пакуем per-версию cfg сами, а сырые aw/* из ресурсов вычищаем.
tasks.processResources {
	from(rootProject.file("src/main/resources/aw/${stonecutter.current.version}.cfg")) {
		rename { "accesstransformer.cfg" }
		into("META-INF")
	}
	exclude("aw/")
}

repositories {
	mavenCentral()
	strictMaven("https://api.modrinth.com/maven", "maven.modrinth") { name = "Modrinth" }
	strictMaven("https://maven.architectury.dev/", "dev.architectury") { name = "Architectury" }
	maven("https://maven.parchmentmc.org") { name = "ParchmentMC" }
	maven("https://maven.createmod.net") { name = "CreateMod" }
	strictMaven("https://cursemaven.com", "curse.maven") { name = "CurseForge" }
	// Curios API (опционально): слот лица для противогазов. См. com.hbm_m.compat.curios.
	maven("https://maven.theillusivec4.top/") { name = "Illusive Soul Works" }
	maven("https://maven.caffeinemc.net/releases") { name = "CaffeineMC" }
	// Sable Companion (dev.ryanhcode) — API-типы для Sable-миксинов, compileOnly
	maven("https://maven.ryanhcode.dev/releases") { name = "RyanHCode" }

}

dependencies {
	implementation(libs.moulberry.mixinconstraints)
	jarJar(libs.moulberry.mixinconstraints)

	// Architectury API — implementation + jarJar (встраивается в итоговый jar, как на Forge).
	implementation("dev.architectury:architectury-neoforge:${prop("deps.architectury")}")
	jarJar("dev.architectury:architectury-neoforge:${prop("deps.architectury")}")

	val mcVer = stonecutter.current.version
	"compileOnly"("com.simibubi.create:create-$mcVer:${prop("deps.create")}:slim") {
		isTransitive = false
	}
	// Sable (экосистема Create Aeronautics): compileOnly только для валидации
	// строковых таргетов миксинов (@Mixin(targets = "...")) на этапе компиляции.
	// В рантайм не пакуется; в отсутствие Sable миксины просто не применяются.
	"compileOnly"("maven.modrinth:sable:2.0.5+mc1.21.1")
	// The modrinth artifact does not expose BoundingBox3ic - it lives in sable-companion,
	// which SubLevelAssembleExpansionMixin has to name in its handler signature.
	// compileOnly: nothing is packed into the mod.
	if (stonecutter.current.version == "1.21.1") {
		"compileOnly"("dev.ryanhcode.sable-companion:sable-companion-common-1.21.1:1.6.0") {
			isTransitive = false
		}
	}

	// Рантайм-зависимости для ручного тестирования интеграции с Create
	// Aeronautics / Sable в runClient (только 1.21.1 - на других версиях
	// этих модов нет). Create 6.x тащит Flywheel/Ponder внутри себя (jarJar).
	// noClientMods (runData / gameTestServer) их выключает: на геймтест-сервере
	// Sable в отсутствие игроков всё равно тикает свою физику и зависает
	// навечно в нативном степе Rapier (SubLevelPhysicsSystem.tickPipelinePhysics
	// -> Rapier3D.step, CPU 100% на Server thread, см. thread dump 2026-10-01).
	if (stonecutter.current.version == "1.21.1" && !project.hasProperty("noClientMods")) {
		"runtimeOnly"("maven.modrinth:create:6.0.10+mc1.21.1")
		"runtimeOnly"("maven.modrinth:sable:2.0.5+mc1.21.1")
		"runtimeOnly"("maven.modrinth:create-aeronautics:1.3.1+mc1.21.1") // bundled: simulated + offroad внутри
	}
	// В NeoForge артефакт называется flywheel-neoforge-api
	"compileOnly"("dev.engine-room.flywheel:flywheel-neoforge-api-$mcVer:${prop("deps.flywheel")}")
	// Distant Horizons: compileOnly для официального API (см. DhRenderBridge).
	// Класс моста грузится только при установленном DH.
	"compileOnly"("maven.modrinth:distanthorizons:3.3.2-1.21.1") // 3.3.2-1.21.1 (latest 2026-09-21)
	// "runtimeOnly"("maven.modrinth:distanthorizons:3.2.0-b-1.21.1")

	"compileOnly"("maven.modrinth:u6dRKJwZ:${prop("deps.jei")}")
	"runtimeOnly"("maven.modrinth:u6dRKJwZ:${prop("deps.jei")}")

	// Jade (WAILA fork): плагин com.hbm_m.compat.jade (см. комментарий в build.forge.gradle.kts).
	// Классы плагина грузит только сканер @WailaPlugin самого Jade — без Jade в рантайме
	// они никогда не загружаются, поэтому compileOnly-зависимость в прод безопасна.
	"compileOnly"("maven.modrinth:jade:15.10.6+neoforge")
	"runtimeOnly"("maven.modrinth:jade:15.10.6+neoforge")

	// Curios (опционально): API для компиляции, сам мод — в рантайм для тестов.
	"compileOnly"("top.theillusivec4.curios:curios-neoforge:9.5.1+1.21.1:api")
	"runtimeOnly"("top.theillusivec4.curios:curios-neoforge:9.5.1+1.21.1")
	"runtimeOnly"("maven.modrinth:l6YH9Als:v5qtqRQi") // spark
	"runtimeOnly"("maven.modrinth:1bokaNcj:JXvcT1hp") // xaeros minimap
	"runtimeOnly"("maven.modrinth:NcUtCpym:fOv9QzLO") // xaeros world map
	
}

tasks.named("createMinecraftArtifacts") {
	dependsOn(tasks.named("stonecutterGenerate"))
}

// 1.21+ переименовала папки дата-паков из множественного числа в единственное
// (recipes → recipe, tags/blocks → tags/block и т.д.), ItemStack-кодек сменил
// ключ "item" на "id" (у варочных рецептов result стал объектом вместо строки),
// а конвенциональные теги Forge переехали из неймспейса forge: в c:.
// Датаген — 1.20.1-only и пишет во всём старом формате, поэтому нормализуем
// ресурсы на выходе processResources, не трогая датаген.
tasks.named<ProcessResources>("processResources") {
	doLast {
		val dataDir = File(destinationDir, "data")
		if (!dataDir.isDirectory) return@doLast

		fun moveInto(source: File, target: File) {
			target.mkdirs()
			source.listFiles()!!.forEach { child ->
				val dest = File(target, child.name)
				if (dest.exists()) {
					if (child.isDirectory) moveInto(child, dest) else child.delete()
				} else {
					child.renameTo(dest)
				}
			}
			source.deleteRecursively()
		}

		// Переименование переименованных в 1.21 директорий (merge при коллизии).
		val dirRenames = mapOf(
			"recipes" to "recipe", "advancements" to "advancement",
			"loot_tables" to "loot_table", "structures" to "structure",
		)
		val tagRenames = mapOf(
			"blocks" to "block", "items" to "item", "entity_types" to "entity_type",
			"fluids" to "fluid", "game_events" to "game_event",
		)

		// Biome-модификаторы на NeoForge 1.21+ читаются из neoforge/biome_modifier
		// с типом neoforge:add_features; датаген (1.20.1) пишет
		// forge/biome_modifier + forge:add_features — без ремапа руды не спавнятся.
		dataDir.listFiles()!!.filter { it.isDirectory }.forEach { nsDir ->
			val bm = File(nsDir, "forge/biome_modifier")
			if (bm.isDirectory) {
				val target = File(nsDir, "neoforge/biome_modifier")
				moveInto(bm, target)
				target.walkTopDown().filter { it.isFile && it.extension == "json" }.forEach { f ->
					f.writeText(f.readText()
						.replace("\"forge:add_features\"", "\"neoforge:add_features\"")
						.replace("\"forge:add_spawns\"", "\"neoforge:add_spawns\"")
						// Карверы: свой forge-тип hbm_m:add_carvers на NeoForge 1.21.1 не
						// зарегистрирован — ремапим на встроенный neoforge:add_carvers
						// (формат полей biomes/carvers/step идентичен).
						.replace("\"hbm_m:add_carvers\"", "\"neoforge:add_carvers\""))
				}
			}
		}

		dataDir.listFiles()!!.filter { it.isDirectory }.forEach { nsDir ->
			// Теги forge: → c: (конвенциональные теги на NeoForge 1.21+ живут в c:).
			if (nsDir.name == "forge") {
				moveInto(nsDir, File(dataDir, "c"))
				// После переезда в c: теги тоже надо привести к единственному числу
				// (1.21 переименовала tags/items → tags/item и т.д.).
				val cTags = File(File(dataDir, "c"), "tags")
				if (cTags.isDirectory) {
					for ((old, new) in tagRenames) {
						val plural = File(cTags, old)
						if (plural.isDirectory) moveInto(plural, File(cTags, new))
					}
				}
			}
			for ((old, new) in dirRenames) {
				val plural = File(nsDir, old)
				if (plural.isDirectory) moveInto(plural, File(nsDir, new))
			}
			val tags = File(nsDir, "tags")
			if (tags.isDirectory) {
				for ((old, new) in tagRenames) {
					val plural = File(tags, old)
					if (plural.isDirectory) moveInto(plural, File(tags, new))
				}
			}
		}

		// The block above moves the tag files, but references inside JSON stay "forge:".
		// Unremapped, recipes match an empty tag and silently stop crafting.
		// The leading quote in the pattern keeps "neoforge:add_features" intact.
		// glass is the only tag whose name also changed: NeoForge calls it c:glass_blocks.
		dataDir.walkTopDown().filter { it.isFile && it.extension == "json" }.forEach { file ->
			val text = file.readText()
			if (!text.contains("forge:")) return@forEach
			val out = text.replace("\"#forge:", "\"#c:").replace("\"forge:", "\"c:")
				.replace("\"c:glass\"", "\"c:glass_blocks\"").replace("\"#c:glass\"", "\"#c:glass_blocks\"")
			// "neoforge:" also matches the guard above, so only write when something actually changed.
			if (out != text) file.writeText(out)
		}

		// Remap silk-touch условия в loot-таблицах: датаген (1.20.1) пишет
		// match_tool-предикат с полем "enchantments" внутри ItemPredicate,
		// а на 1.21.1 это поле удалено — проверки зачарований переехали в
		// ItemSubPredicates ("predicates"."minecraft:enchantments", а внутри
		// каждой записи зачарование теперь "enchantments", не "enchantment").
		// Без ремапа silk-ветка рудных таблиц никогда не срабатывает.
		val lootRoots = dataDir.listFiles()!!.mapNotNull { File(it, "loot_table").takeIf(File::isDirectory) }
		lootRoots.forEach { root ->
			root.walkTopDown().filter { it.isFile && it.extension == "json" }.forEach { file ->
				val text = file.readText()
				if (!text.contains("minecraft:match_tool")) return@forEach
				val tree = groovy.json.JsonSlurper().parseText(text)
				@Suppress("UNCHECKED_CAST")
				fun fixPredicate(predicate: MutableMap<String, Any>) {
					val ench = predicate.remove("enchantments") as? List<Any> ?: return
					val converted = ench.map { e ->
						val m = e as MutableMap<String, Any>
						val out = linkedMapOf<String, Any>()
						m["enchantment"]?.let { out["enchantments"] = it }
						m["levels"]?.let { out["levels"] = it }
						out
					}
					predicate["predicates"] = linkedMapOf("minecraft:enchantments" to converted)
				}
				@Suppress("UNCHECKED_CAST")
				fun walk(node: Any?) {
					when (node) {
						is MutableMap<*, *> -> {
							if (node["condition"] == "minecraft:match_tool") {
								(node["predicate"] as? MutableMap<String, Any>)?.let { fixPredicate(it) }
							}
							node.values.forEach { walk(it) }
						}
						is MutableList<*> -> node.forEach { walk(it) }
					}
				}
				walk(tree)
				file.writeText(groovy.json.JsonOutput.prettyPrint(groovy.json.JsonOutput.toJson(tree)))
			}
		}

		// Нормализация ванильных рецептов (minecraft:*) в формат 1.21.x:
		// "result": {"item": X} → {"id": X};  "result": "X" → {"id": X}.
		// Кастомные рецепты (hbm_m:*) не трогаем — их нормализует RecipeHooks.
		val recipeRoots = dataDir.listFiles()!!.mapNotNull { File(it, "recipe").takeIf(File::isDirectory) }
		val slurper = groovy.json.JsonSlurper()
		recipeRoots.forEach { root ->
			root.walkTopDown().filter { it.isFile && it.extension == "json" }.forEach { file ->
				val tree = slurper.parse(file) as? Map<*, *> ?: return@forEach
				if ((tree["type"] as? String)?.startsWith("minecraft:") != true) return@forEach
				val result = tree["result"]
				val normalized: Any? = when (result) {
					is String -> mapOf("id" to result)
					is Map<*, *> ->
						if (result.containsKey("item") && !result.containsKey("id"))
							result.entries.associate { (k, v) -> if (k == "item") "id" to v else k to v }
						else null
					else -> null
				}
				if (normalized != null) {
					val copy = tree.toMutableMap()
					copy["result"] = normalized
					file.writeText(groovy.json.JsonOutput.prettyPrint(groovy.json.JsonOutput.toJson(copy)))
				}
			}
		}

		// Лут-таблицы батарей используют minecraft:copy_nbt, удалённый в 1.20.5+.
		// На 1.21.1 состояние батареи переносится кодом
		// (MachineBatteryBlock#playerWillDestroy → MachineBatteryBlockEntity#saveToItemStack),
		// поэтому таблицы просто исключаем из сборки.
		// Конденсаторы несут заряд тем же механизмом (MachineCapacitorBlock#playerWillDestroy) -
		// их таблицы тоже исключаем. У TE-less capacitor_bus таблица остаётся (обычный dropSelf).
		listOf("loot_table", "loot_tables").forEach { dirName ->
			File(File(dataDir, "hbm_m"), dirName).walkTopDown()
				// machine_fensu is a MachineBatteryBlock too - its name just does not start with
				// machine_battery, so it kept its loot table and dropped a second copy of itself.
				.filter { it.isFile && (it.name.startsWith("machine_battery") || it.name == "machine_fensu.json"
						|| (it.name.startsWith("capacitor_") && it.name != "capacitor_bus.json")) }
				.forEach { it.delete() }
		}

		// minecraft:grass переименован в short_grass в 1.20.3+ (датаген 1.20.1 пишет старое имя).
		dataDir.walkTopDown()
			.filter { it.isFile && it.extension == "json" }.forEach { file ->
				val text = file.readText()
				if (text.contains("\"minecraft:grass\"")) {
					file.writeText(text.replace("\"minecraft:grass\"", "\"minecraft:short_grass\""))
				}
			}
	}
}

sourceSets {
	main {
		java {
			exclude("com/hbm_m/datagen/**")
		}
	}
}

// GameTest'ы нужны только в dev-ранах (gameTestServer работает из classes-директории,
// не из jar) — в продакшен-jar они не попадают.
tasks.named<Jar>("jar") {
	exclude("com/hbm_m/test/**")
}

tasks.withType<JavaCompile>().configureEach {
	options.compilerArgs.addAll(listOf("-Xmaxerrs", "10000"))
	options.encoding = "UTF-8"
	// The forked compiler worker defaults to -Xmx512m, which the mixin annotation
	// processor can exhaust on large merges (javac OOM "Java heap space").
	options.forkOptions.memoryMaximumSize = "3g"
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