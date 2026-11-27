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
	// 26.x: entfernte Vanilla-Methoden (z. B. Block#onRemove) als Interface-Default zurückholen,
	// siehe com.hbm_m.platform.legacy.LegacyBlockRemoval.
	val interfaceData = rootProject.file("src/main/resources/aw/${stonecutter.current.version}.interfaces.json")
	if (interfaceData.exists()) interfaceInjectionData.from(interfaceData)
	validateAccessTransformers = true

	if (hasProperty("deps.parchment")) parchment {
		val (mc, ver) = (property("deps.parchment") as String).split(':')
		mappingsVersion = ver
		minecraftVersion = mc
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

	mods {
		register(property("mod.id") as String) {
			sourceSet(sourceSets["main"])
		}
	}
	sourceSets["main"].resources.srcDir(rootProject.file("src/generated/resources"))
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
	// 26.x: Create/Flywheel/Sable/Distant Horizons gibt es (noch) nicht -> nur 1.21.1.
	val is1211 = mcVer == "1.21.1"
	if (is1211) {
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
		"compileOnly"("dev.ryanhcode.sable-companion:sable-companion-common-1.21.1:1.6.0") {
			isTransitive = false
		}

		// Рантайм-зависимости для ручного тестирования интеграции с Create
		// Aeronautics / Sable в runClient (только 1.21.1 - на других версиях
		// этих модов нет). Create 6.x тащит Flywheel/Ponder внутри себя (jarJar).
		// noClientMods (runData / gameTestServer) их выключает: на геймтест-сервере
		// Sable в отсутствие игроков всё равно тикает свою физику и зависает
		// навечно в нативном степе Rapier (SubLevelPhysicsSystem.tickPipelinePhysics
		// -> Rapier3D.step, CPU 100% на Server thread, см. thread dump 2026-10-01).
		if (!project.hasProperty("noClientMods")) {
			"runtimeOnly"("maven.modrinth:create:6.0.10+mc1.21.1")
			"runtimeOnly"("maven.modrinth:sable:2.0.5+mc1.21.1")
			"runtimeOnly"("maven.modrinth:create-aeronautics:1.3.1+mc1.21.1") // bundled: simulated + offroad внутри
		}
		// В NeoForge артефакт называется flywheel-neoforge-api
		"compileOnly"("dev.engine-room.flywheel:flywheel-neoforge-api-$mcVer:${prop("deps.flywheel")}")
		// Distant Horizons: compileOnly для официального API (см. DhRenderBridge).
		// Класс моста грузится только при установленном DH.
		"compileOnly"("maven.modrinth:distanthorizons:3.3.3-1.21.1") // 3.3.3-1.21.1
		if (!project.hasProperty("noClientMods")) {
			"runtimeOnly"("maven.modrinth:distanthorizons:3.3.3-1.21.1")
		}
	}

	"compileOnly"("maven.modrinth:u6dRKJwZ:${prop("deps.jei")}")
	"runtimeOnly"("maven.modrinth:u6dRKJwZ:${prop("deps.jei")}")

	// Jade (WAILA fork): плагин com.hbm_m.compat.jade (см. комментарий в build.forge.gradle.kts).
	// Классы плагина грузит только сканер @WailaPlugin самого Jade — без Jade в рантайме
	// они никогда не загружаются, поэтому compileOnly-зависимость в прод безопасна.
	if (is1211) {
		"compileOnly"("maven.modrinth:jade:15.10.6+neoforge")
		"runtimeOnly"("maven.modrinth:jade:15.10.6+neoforge")
	} else {
		"compileOnly"("maven.modrinth:jade:26.3.5+neoforge")
	}

	// Curios (опционально): API для компиляции, сам мод — в рантайм для тестов.
	if (is1211) {
		"compileOnly"("top.theillusivec4.curios:curios-neoforge:9.5.1+1.21.1:api")
		"runtimeOnly"("top.theillusivec4.curios:curios-neoforge:9.5.1+1.21.1")
		"runtimeOnly"("maven.modrinth:l6YH9Als:v5qtqRQi") // spark
		"runtimeOnly"("maven.modrinth:1bokaNcj:JXvcT1hp") // xaeros minimap
		"runtimeOnly"("maven.modrinth:NcUtCpym:fOv9QzLO") // xaeros world map
	}
}

tasks.named("createMinecraftArtifacts") {
	dependsOn(tasks.named("stonecutterGenerate"))
}

// Compat-Code für Mods ohne 26.x-Version (Create, Sable, Distant Horizons, Curios).
// Die Fassaden (CreateCompat, SableCompat, DhCompat, CuriosCompat) bleiben drin und
// verzweigen per //? if <26.1 nicht mehr in diese Klassen.
val mc26CompatExcludes = listOf(
	"com/hbm_m/client/compat/create/**",
	"com/hbm_m/client/compat/dh/DhRenderBridge.java",
	"com/hbm_m/compat/create/CreateDoorRegistrar.java",
	"com/hbm_m/compat/create/HbmDoor*.java",
	"com/hbm_m/compat/create/HbmMultiblockMovementChecks.java",
	"com/hbm_m/compat/curios/CuriosAccess.java",
	"com/hbm_m/compat/curios/CuriosForgeEvents.java",
	"com/hbm_m/compat/curios/CuriosNeoForgeEvents.java",
	"com/hbm_m/compat/curios/GasMaskCurio.java",
	"com/hbm_m/mixin/compat/**",
	"com/hbm_m/mixin/client/SuperGlueSelectionOutlineMixin.java",
	// Zielt auf BlockState#onRemove (gibt es in 26.x nicht); der Contraption-Schutz ist ohne Create/Sable
	// ohnehin unnötig. TODO(26.x): clearAllBlockEntities-Snapshot (CME-Schutz) separat nachziehen.
	"com/hbm_m/mixin/LevelChunkSilentRemovalMixin.java",
	// GameTests: 26.x hat ein neues, datengetriebenes GameTest-System -> später portieren.
	"com/hbm_m/test/**",
	// Modellsystem „nur NMesh" (26.x): die alten IGeometryLoader/BakedModel-Klassen entfallen,
	// alle hbm_m-Loader-IDs laufen über platform/legacy/client/model/NMeshModelLoader26.
	"com/hbm_m/client/loader/**",
	"com/hbm_m/client/model/*BakedModel.java",
	"com/hbm_m/client/model/MeteorSwordGlintModel.java",
	"com/hbm_m/client/render/weapon/WeaponMultipartBakedModel.java",
	"com/hbm_m/item/hazmat/render/HazmatArmorModelLoader.java",
	"com/hbm_m/powerarmor/render/*ArmorModelLoader.java",
	"com/hbm_m/powerarmor/render/*BakedModel.java",
	"com/hbm_m/item/hazmat/render/*BakedModel.java",
	"com/hbm_m/client/compat/itemtransformhelper/**",
)
val isMc26 = stonecutter.eval(stonecutter.current.version, ">=26.1")

sourceSets {
	main {
		java {
			exclude("com/hbm_m/datagen/**")
			if (isMc26) mc26CompatExcludes.forEach { exclude(it) }
			// Ersatz für die in NeoForge 26.x entfernten Handler-APIs (items/fluids.capability/energy).
			else exclude("com/hbm_m/platform/legacy/**")
		}
	}
}

// Ausgeschlossene Mixins auch aus der Mixin-Config entfernen (sonst "mixin class not found").
// Dazu die 26.x-Brücken-Mixins (mixins.hbm_m.mc26.json) als zweite [[mixins]]-Config anmelden.
if (!isMc26) tasks.named<ProcessResources>("processResources") {
	exclude("mixins.hbm_m.mc26.json", "aw/*.interfaces.json")
}
if (isMc26) tasks.named<ProcessResources>("processResources") {
	doLast {
		val toml = File(destinationDir, "META-INF/neoforge.mods.toml")
		if (toml.exists() && !toml.readText().contains("mixins.hbm_m.mc26.json")) {
			toml.appendText("\n[[mixins]]\nconfig = \"mixins.hbm_m.mc26.json\"\n")
		}
	}
	filesMatching("mixins.hbm_m.json") {
		filter { line ->
			if (line.contains("\"compat.create.") || line.contains("\"compat.sable.")
				|| line.contains("\"client.SuperGlueSelectionOutlineMixin\"")
				|| line.contains("\"LevelChunkSilentRemovalMixin\"")) "" else line
		}
	}
}

// GameTest'ы нужны только в dev-ранах (gameTestServer работает из classes-директории,
// не из jar) — в продакшен-jar они не попадают.
tasks.named<Jar>("jar") {
	exclude("com/hbm_m/test/**")
}

tasks.withType<JavaCompile>().configureEach {
	options.compilerArgs.addAll(listOf("-Xmaxerrs", if (stonecutter.eval(stonecutter.current.version, ">=26.1")) "60000" else "10000"))
	options.encoding = "UTF-8"
	// The forked compiler worker defaults to -Xmx512m, which the mixin annotation
	// processor can exhaust on large merges (javac OOM "Java heap space").
	options.forkOptions.memoryMaximumSize = "3g"
}

stonecutter {
}
