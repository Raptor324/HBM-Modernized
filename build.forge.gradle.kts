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
	"modRuntimeOnly"("maven.modrinth:spark:1.10.53-forge")
	"modRuntimeOnly"("maven.modrinth:VYRu7qmG:QtSVNyjm") // Observable - profiles (tile) entities and shows you what's taking up tick time and where.
	"modRuntimeOnly"("maven.modrinth:ordsPcFz:Zsh14XeQ") // Kotlin For Forge
	// Distant Horizons: compileOnly для официального API (DhApiBeforeApplyShaderRenderEvent
	// и пр.) в com.hbm_m.client.compat.dh.DhRenderBridge. Класс моста грузится только при
	// установленном DH, поэтому отсутствие зависимости в рантайме безопасно.
	"modCompileOnly"("maven.modrinth:distanthorizons:3.3.3-1.20.1") // 3.3.3-1.20.1

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
	test {
		compileClasspath += configurations["compileClasspath"]
		runtimeClasspath += configurations["runtimeClasspath"]
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

stonecutter {
}
