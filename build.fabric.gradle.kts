plugins {
	id("mod-platform")
	id("fabric-loom")
}

platform {
	loader = "fabric"
	dependencies {
		required("minecraft") {
			versionRange = prop("deps.minecraft")
		}
		required("architectury") {
			slug("architectury-api")
			versionRange = ">=${prop("deps.architectury")}"
		}
		required("fabric-api") {
			slug("fabric-api")
			versionRange = ">=${prop("deps.fabric-api")}"
		}
		required("fabricloader") {
			versionRange = ">=${libs.fabric.loader.get().version}"
		}
		optional("modmenu") {}
	}
}

loom {
	accessWidenerPath = rootProject.file("src/main/resources/aw/${stonecutter.current.version}.accesswidener")
	runs.named("client") {
		client()
		ideConfigGenerated(true)
		runDir = "run/"
		environment = "client"
		programArgs("--username=Dev")
		configName = "Fabric Client"
	}
	runs.named("server") {
		server()
		ideConfigGenerated(true)
		runDir = "run/"
		environment = "server"
		configName = "Fabric Server"
	}
}

fabricApi {
	configureDataGeneration {
		outputDirectory = rootProject.file("src/generated/resources")
		client = true
	}
}

repositories {
	mavenCentral()
	strictMaven("https://maven.terraformersmc.com/", "com.terraformersmc") { name = "TerraformersMC" }
	strictMaven("https://api.modrinth.com/maven", "maven.modrinth") { name = "Modrinth" }
	strictMaven("https://maven.architectury.dev/", "dev.architectury") { name = "Architectury" }
	strictMaven("https://cursemaven.com/","curse.maven") { name = "CurseMaven" }


	maven("https://maven.parchmentmc.org") { name = "ParchmentMC" }

	// CCA releases for 1.20.x live under dev.onyxstudios.cardinal-components-api on Ladysnake maven.
	maven("https://maven.ladysnake.org/releases") { name = "Ladysnake" }

	// TeamReborn Energy API is published to FabricMC maven.
	maven("https://maven.fabricmc.net") { name = "FabricMC Maven" }

	// Compile-only Forge API to keep shared sources compiling on Fabric.
	maven("https://maven.minecraftforge.net") { name = "Forge Maven" }
}

dependencies {
	minecraft("com.mojang:minecraft:${prop("deps.minecraft")}")
	mappings(
		loom.layered {
			officialMojangMappings()
			if (hasProperty("deps.parchment")) parchment("org.parchmentmc.data:parchment-${prop("deps.parchment")}@zip")
		})
	modImplementation(libs.fabric.loader)
	implementation(libs.moulberry.mixinconstraints)
	include(libs.moulberry.mixinconstraints)
	modImplementation("net.fabricmc.fabric-api:fabric-api:${prop("deps.fabric-api")}")
	modImplementation("dev.architectury:architectury-fabric:${prop("deps.architectury")}")
	modLocalRuntime("com.terraformersmc:modmenu:${prop("deps.modmenu")}")

	// Fabric compat: Chunk radiation via CCA (bundled)
	modImplementation("dev.onyxstudios.cardinal-components-api:cardinal-components-base:5.2.3")
	modImplementation("dev.onyxstudios.cardinal-components-api:cardinal-components-chunk:5.2.3")
	include("dev.onyxstudios.cardinal-components-api:cardinal-components-base:5.2.3")
	include("dev.onyxstudios.cardinal-components-api:cardinal-components-chunk:5.2.3")

	// Fabric compat: external energy via TeamReborn Energy API (bundled)
	modApi("teamreborn:energy:3.0.0")
	include("teamreborn:energy:3.0.0")
	include("curse.maven:cloth-config-348521:5729104")
	modApi("curse.maven:cloth-config-348521:5729104")
	// ---- Compile-only shims for shared (Forge-origin) sources ----
	// These MUST NOT end up in the Fabric runtime jar.
	// compileOnly("net.minecraftforge:forge:1.20.1-47.4.20:universal")
	// compileOnly("net.minecraftforge:fmlloader:1.20.1-47.4.20")
	// compileOnly("net.minecraftforge:fmlcore:1.20.1-47.4.20")
	// compileOnly("net.minecraftforge:javafmllanguage:1.20.1-47.4.20")
	// compileOnly("net.minecraftforge:lowcodelanguage:1.20.1-47.4.20")
	// compileOnly("net.minecraftforge:mclanguage:1.20.1-47.4.20")
	// compileOnly("com.google.code.findbugs:jsr305:3.0.2")
}

tasks.withType<JavaCompile>().configureEach {
	options.compilerArgs.addAll(listOf("-Xmaxerrs", "10000"))
}

sourceSets {
	main {
		java {
			exclude("com/hbm_m/datagen/**")
		}
	}
}

stonecutter {
	val isModern = current.parsed >= "1.21.11"

	replacements.regex(isModern) {
		replace("\\bResourceLocation\\b", "Identifier")
		reversePattern.set("\\bIdentifier\\b")
		reverseValue.set("ResourceLocation")
	}

	replacements.regex(isModern) {
		replace("\\blocation\\(\\)", "identifier()")
		reversePattern.set("\\bidentifier\\(\\)")
		reverseValue.set("location()")
	}

	replacements.regex(isModern) {
		replace("net\\.minecraft\\.resources\\.ResourceLocation", "net.minecraft.util.Identifier")
		reversePattern.set("net\\.minecraft\\.util\\.Identifier")
		reverseValue.set("net.minecraft.resources.ResourceLocation")
	}
}
