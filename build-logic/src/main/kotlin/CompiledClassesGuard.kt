import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.kotlin.dsl.*
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileLock
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.locks.ReentrantLock
import javax.tools.ToolProvider

private val CLASS_TOKEN = Regex("""(?<![.\w])(?:@interface|class|interface|enum|record)\s+(\w+)|(\{)|(\})""")
private val PACKAGE_DECLARATION = Regex("""(?m)^\s*package\s+([\w.]+)""")

// ── 1. Межпроцессный OS-мьютекс для защиты от одновременной компиляции 2 агентами ──
// Использует ядерный FileChannel.lock(): работает между разными демонами Gradle,
// терминалами и агентами. При kill -9 ОС сама мгновенно освобождает лок.
abstract class CompileMutexService : BuildService<CompileMutexService.Params>, AutoCloseable {
	interface Params : BuildServiceParameters {
		var lockFile: File
		var compilingMarkerFiles: List<File>
	}

	private val jvmLock = ReentrantLock()
	private var raf: RandomAccessFile? = null
	private var fileLock: FileLock? = null

	@Synchronized
	fun acquire(logger: org.gradle.api.logging.Logger) {
		if (fileLock != null) return
		jvmLock.lock()
		try {
			val lf = parameters.lockFile
			lf.parentFile.mkdirs()
			val r = RandomAccessFile(lf, "rw")
			val ch = r.channel
			var lock = ch.tryLock()
			if (lock == null) {
				logger.lifecycle("⏳ [BuildMutex] Другой агент сейчас компилирует проект. Ожидание очереди (${lf.name})...")
				val t0 = System.currentTimeMillis()
				lock = ch.lock()
				logger.lifecycle("▶️ [BuildMutex] Очередь дошла за ${System.currentTimeMillis() - t0} мс, начинаем сборку.")
			}
			raf = r
			fileLock = lock
			// Ставим маркер .compiling, чтобы HbmAutoHotswapAgent в живом клиенте не читал полусобранные классы
			for (marker in parameters.compilingMarkerFiles) {
				runCatching {
					marker.parentFile?.mkdirs()
					marker.writeText(ProcessHandle.current().pid().toString())
				}
			}
		} catch (t: Throwable) {
			jvmLock.unlock()
			throw t
		}
	}

	@Synchronized
	fun release() {
		for (marker in parameters.compilingMarkerFiles) {
			runCatching { marker.delete() }
		}
		try {
			runCatching { fileLock?.release() }
			runCatching { raf?.close() }
		} finally {
			fileLock = null
			raf = null
			if (jvmLock.isHeldByCurrentThread) {
				jvmLock.unlock()
			}
		}
	}

	override fun close() {
		release()
	}
}

/** Вырезает комментарии и строковые/символьные литералы для парсинга объявлений типов. */
private fun stripNonCode(src: String): String {
	val sb = StringBuilder(src.length)
	var i = 0
	val n = src.length
	while (i < n) {
		val c = src[i]
		when {
			c == '/' && i + 1 < n && src[i + 1] == '/' -> {
				while (i < n && src[i] != '\n') i++
				sb.append(' ')
			}
			c == '/' && i + 1 < n && src[i + 1] == '*' -> {
				i += 2
				while (i + 1 < n && !(src[i] == '*' && src[i + 1] == '/')) i++
				i = minOf(i + 2, n)
				sb.append(' ')
			}
			c == '"' && src.startsWith("\"\"\"", i) -> {
				i += 3
				while (i + 2 < n && !src.startsWith("\"\"\"", i)) i += if (src[i] == '\\') 2 else 1
				i = minOf(i + 3, n)
				sb.append(' ')
			}
			c == '"' || c == '\'' -> {
				i++
				while (i < n && src[i] != c) i += if (src[i] == '\\') 2 else 1
				i = minOf(i + 1, n)
				sb.append(' ')
			}
			else -> {
				sb.append(c)
				i++
			}
		}
	}
	return sb.toString()
}

private fun expectedClassFiles(stripped: String): List<Pair<String, Boolean>> {
	val pkg = PACKAGE_DECLARATION.find(stripped)?.groupValues?.get(1)?.replace('.', '/')
	val result = mutableListOf<Pair<String, Boolean>>()
	val braceStack = ArrayDeque<String?>()
	var pendingDecl: String? = null
	for (m in CLASS_TOKEN.findAll(stripped)) {
		when {
			m.groups[1] != null -> {
				val chain = braceStack.filterNotNull()
				val simple = (if (chain.isEmpty()) "" else chain.joinToString("$") + "$") + m.groupValues[1]
				result += (if (pkg.isNullOrEmpty()) "" else "$pkg/") + simple to chain.isEmpty()
				pendingDecl = m.groupValues[1]
			}
			m.groups[2] != null -> {
				braceStack.addLast(pendingDecl)
				pendingDecl = null
			}
			else -> braceStack.removeLastOrNull()
		}
	}
	return result
}

private fun hasLocalClassVariant(expected: String, classFiles: Set<String>): Boolean {
	val slash = expected.lastIndexOf('/')
	val dir = if (slash < 0) "" else expected.substring(0, slash + 1)
	val simple = expected.substring(slash + 1)
	val outer = simple.substringBefore('$')
	val name = simple.substringAfterLast('$')
	val re = Regex("""^${Regex.escape(outer)}\$[\d]*${Regex.escape(name)}$""")
	return classFiles.any {
		it.startsWith(dir) && it.substring(dir.length).matches(re)
	}
}

/**
 * ── 2. Парсер Constant Pool байткода (.class) ──
 * Извлекает все ссылки CONSTANT_Class_info (включая синтетические switch-таблицы Outer$1,
 * анонимные классы, вложенные классы из InnerClasses/NestMembers и ссылки на другие классы мода).
 * Возвращает null, если .class-файл битый или обрезан при гонке записи.
 */
private fun parseConstantPoolModRefs(bytes: ByteArray, modPrefix: String = "com/hbm_m/"): Set<String>? {
	if (bytes.size < 24) return null
	val buf = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)
	if (buf.int != -0x35014542) return null // 0xCAFEBABE
	buf.short // minor
	buf.short // major
	val cpCount = buf.short.toInt() and 0xFFFF
	if (cpCount < 1) return null

	val utf8 = arrayOfNulls<String>(cpCount)
	val classNameIndices = IntArray(cpCount)
	var classRefCount = 0

	var i = 1
	try {
		while (i < cpCount) {
			when (buf.get().toInt() and 0xFF) {
				1 -> { // CONSTANT_Utf8
					val len = buf.short.toInt() and 0xFFFF
					val pos = buf.position()
					buf.position(pos + len)
					utf8[i] = String(bytes, pos, len, Charsets.UTF_8)
				}
				3, 4 -> buf.position(buf.position() + 4) // Integer, Float
				5, 6 -> { // Long, Double занимают 2 слота в Constant Pool (JVMS §4.4.5)
					buf.position(buf.position() + 8)
					i++
				}
				7 -> { // CONSTANT_Class
					classNameIndices[classRefCount++] = buf.short.toInt() and 0xFFFF
				}
				8, 16, 19, 20 -> buf.position(buf.position() + 2)
				9, 10, 11, 12, 17, 18 -> buf.position(buf.position() + 4)
				15 -> buf.position(buf.position() + 3)
				else -> return null // Неизвестный тег -> файл повреждён
			}
			i++
		}
	} catch (_: Exception) {
		return null // Файл обрезан (недописан javac)
	}

	val refs = mutableSetOf<String>()
	for (k in 0 until classRefCount) {
		val idx = classNameIndices[k]
		val raw = if (idx in 1 until cpCount) utf8[idx] else null
		if (raw == null) continue
		val clean = if (raw.startsWith("[")) {
			val l = raw.indexOf('L')
			if (l >= 0 && raw.endsWith(";")) raw.substring(l + 1, raw.length - 1) else continue
		} else {
			raw
		}
		if (clean.startsWith(modPrefix)) {
			refs += clean
		}
	}
	return refs
}

private fun analyzeCompiledClasses(sources: Collection<File>, classDirs: FileCollection): List<String> {
	val classFileMap = sortedMapOf<String, File>()
	classDirs.forEach { dir ->
		if (dir.isDirectory) dir.walkTopDown()
			.filter { it.isFile && it.name.endsWith(".class") }
			.forEach { f ->
				val rel = dir.toPath().relativize(f.toPath()).toString().replace('\\', '/').removeSuffix(".class")
				classFileMap[rel] = f
			}
	}
	val classFiles = classFileMap.keys

	val sourceTopLevels = mutableSetOf<String>()
	val problems = mutableListOf<String>()

	// Проверка 1: Все объявленные в .java классы присутствуют на диске
	for (src in sources) {
		if (!src.exists()) continue
		for ((expected, isTop) in expectedClassFiles(stripNonCode(src.readText()))) {
			if (isTop) sourceTopLevels += expected
			if (expected in classFiles) continue
			if (!isTop && hasLocalClassVariant(expected, classFiles)) continue
			problems += "${src.invariantSeparatorsPath}: missing $expected.class"
		}
	}

	// Проверка 2: Целостность байткода (0xCAFEBABE) и ВСЕ ссылки из Constant Pool
	// Ловит 100% пропавших анонимных классов ($1, $2), синтетических switch-map'ов и внутренних классов!
	for ((className, file) in classFileMap) {
		val bytes = runCatching { file.readBytes() }.getOrNull()
		if (bytes == null || bytes.isEmpty()) {
			problems += "$className.class: empty or unreadable file"
			continue
		}
		val refs = parseConstantPoolModRefs(bytes)
		if (refs == null) {
			problems += "$className.class: corrupted/truncated bytecode (invalid Constant Pool)"
			continue
		}
		for (ref in refs) {
			if (ref !in classFiles) {
				problems += "$className.class references missing $ref.class (Constant Pool link broken -> guaranteed NoClassDefFoundError)"
			}
		}
	}

	// Проверка 3: Осиротевшие .class-файлы от старых сборок
	for (cf in classFiles) {
		val top = cf.substringBefore('$')
		if (top == "package-info" || top == "module-info") continue
		if (top !in sourceTopLevels) problems += "$cf.class: class without a source file (left over from an old build?)"
	}
	return problems
}

/**
 * ── 3. Автоматическое самолечение (Auto-Heal) без обнуления живого build/classes ──
 * Компилирует все исходники начисто во временную staging-папку и атомарно синхронизирует
 * .class-файлы в основную директорию, чтобы запущенный runClient не падал во время починки.
 */
private fun autoHealCompilation(project: Project, compileTask: JavaCompile, sources: Set<File>): Boolean {
	val compiler = ToolProvider.getSystemJavaCompiler() ?: return false
	val stagingDir = project.layout.buildDirectory.dir("tmp/auto-heal-classes").get().asFile
	stagingDir.deleteRecursively()
	stagingDir.mkdirs()

	val targetDir = compileTask.destinationDirectory.get().asFile
	targetDir.mkdirs()

	val args = mutableListOf<String>()
	args += listOf("-encoding", "UTF-8", "-d", stagingDir.absolutePath)

	val release = compileTask.options.release.orNull
	if (release != null) {
		args += listOf("--release", release.toString())
	} else {
		args += listOf("-source", compileTask.sourceCompatibility, "-target", compileTask.targetCompatibility)
	}

	val cp = compileTask.classpath.files.filter { it.exists() }.joinToString(File.pathSeparator) { it.absolutePath }
	if (cp.isNotEmpty()) args += listOf("-classpath", cp)

	// Отключаем только генерацию миксин-рефмапа во время аварийной докомпиляции классов (сам рефмап уже собран),
	// либо используем proc:none, чтобы javac мгновенно и без падений восстановил все .class-файлы.
	args += "-proc:none"
	args += sources.filter { it.exists() && it.extension == "java" }.map { it.absolutePath }

	val argFile = project.layout.buildDirectory.file("tmp/auto-heal-javac.args").get().asFile
	argFile.parentFile.mkdirs()
	argFile.writeText(args.joinToString("\n") { "\"${it.replace("\\", "\\\\").replace("\"", "\\\"")}\"" })

	val rc = compiler.run(null, null, null, "@${argFile.absolutePath}")
	if (rc != 0) return false

	// Атомарно копируем свежескомпилированные .class поверх targetDir (без предварительного deleteRecursively!)
	val newRelPaths = mutableSetOf<String>()
	stagingDir.walkTopDown().filter { it.isFile && it.name.endsWith(".class") }.forEach { srcFile ->
		val rel = stagingDir.toPath().relativize(srcFile.toPath()).toString()
		newRelPaths += rel
		val dstFile = File(targetDir, rel)
		dstFile.parentFile.mkdirs()
		val tmpFile = File(targetDir, "$rel.tmp")
		Files.copy(srcFile.toPath(), tmpFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
		Files.move(tmpFile.toPath(), dstFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
	}

	// Удаляем только реально осиротевшие .class-файлы, которых нет в новой компиляции
	targetDir.walkTopDown().filter { it.isFile && it.name.endsWith(".class") }.forEach { existing ->
		val rel = targetDir.toPath().relativize(existing.toPath()).toString()
		if (rel !in newRelPaths) existing.delete()
	}

	// Сбрасываем битый инкрементальный стейт Gradle, чтобы следующий инкрементальный билд шёл с чистого листа
	project.file("build/tmp/compileJava").deleteRecursively()
	project.file("build/kspCaches").deleteRecursively()
	stagingDir.deleteRecursively()
	return true
}

private fun describeProblems(problems: List<String>, repairCommand: String): String {
	val shown = problems.take(30).joinToString("\n  ")
	return buildString {
		appendLine("Build is incomplete: ${problems.size} mismatches between sources/bytecode and compilation output:")
		append("  ").appendLine(shown)
		if (problems.size > 30) appendLine("  …and ${problems.size - 30} more")
		append("Repair and rebuild with a single command: ").append(repairCommand)
	}
}

fun Project.registerCompiledClassesGuard(jarTaskName: String) {
	val mainSourceSet = the<SourceSetContainer>().named("main")
	val sources = mainSourceSet.map { it.java }
	val classDirs = mainSourceSet.map { it.output.classesDirs }
	val repairCommand = "./gradlew $path:repairCompiledClasses"

	// Глобальный лок-файл на весь проект (или сабпроект), сериализующий параллельные сборки от разных агентов
	val mutexProvider = gradle.sharedServices.registerIfAbsent("compileMutex_${path.replace(':', '_')}", CompileMutexService::class.java) {
		parameters.lockFile = rootProject.file(".gradle/hbm-compile-${project.name}.lock")
		parameters.compilingMarkerFiles = listOf(
			layout.buildDirectory.file("classes/.compiling").get().asFile,
			layout.buildDirectory.file("classes/java/main/.compiling").get().asFile
		)
	}

	// Захватываем межпроцессный лок перед генерацией кода Stonecutter/KSP/Java
	tasks.matching { it.name in setOf("stonecutterGenerate", "kspKotlin", "compileKotlin", "compileJava") }.configureEach {
		usesService(mutexProvider)
		doFirst {
			mutexProvider.get().acquire(logger)
		}
	}

	val verify = tasks.register("verifyCompiledClasses") {
		group = "verification"
		description = "Cross-checks compilation output & Constant Pool against sources and auto-heals missing classes"
		dependsOn(mainSourceSet.map { it.classesTaskName })
		usesService(mutexProvider)

		// НИКОГДА не доверяем VFS-кэшу Gradle для этой задачи (проверка занимает ~40 мс, но даёт 100% гарантию)
		outputs.upToDateWhen { false }

		doLast {
			try {
				var problems = analyzeCompiledClasses(sources.get().files, classDirs.get())
				if (problems.isNotEmpty()) {
					logger.warn("⚠️ [CompiledClassesGuard] Обнаружено ${problems.size} пропавших/битых классов после сборки! Запуск автоматического самолечения (Auto-Heal)...")
					problems.take(10).forEach { logger.warn("   • $it") }

					val compileJavaTask = tasks.named<JavaCompile>("compileJava").get()
					val healed = autoHealCompilation(project, compileJavaTask, sources.get().files)
					if (healed) {
						problems = analyzeCompiledClasses(sources.get().files, classDirs.get())
					}
					if (problems.isNotEmpty()) {
						throw GradleException(describeProblems(problems, repairCommand))
					}
					logger.lifecycle("✅ [CompiledClassesGuard] Auto-Heal успешно восстановил 100% классов и ссылок Constant Pool!")
				}
			} finally {
				// Освобождаем лок сразу после проверки, чтобы запущенный следом runClient НЕ держал лок часами
				mutexProvider.get().release()
			}
		}
	}

	tasks.named<JavaCompile>("compileJava").configure {
		outputs.cacheIf { false }
		// Если до старта compileJava классы уже битые, говорим Gradle что таск НЕ UP-TO-DATE
		outputs.upToDateWhen {
			analyzeCompiledClasses(sources.get().files, classDirs.get()).isEmpty()
		}
		// Любой запуск compileJava (даже фоновый от ИИ-агента) ОБЯЗАН завершиться верификацией и снятием лока
		finalizedBy(verify)
	}

	tasks.matching { it.name == "compileKotlin" || it.name == "classes" }.configureEach {
		outputs.cacheIf { false }
		finalizedBy(verify)
	}

	tasks.register("repairCompiledClasses") {
		group = "build"
		description = "Forces clean rebuild of all compiled classes"
		usesService(mutexProvider)
		doLast {
			mutexProvider.get().acquire(logger)
			val compileJavaTask = tasks.named<JavaCompile>("compileJava").get()
			logger.lifecycle("repairCompiledClasses: performing atomic clean compilation...")
			if (!autoHealCompilation(project, compileJavaTask, sources.get().files)) {
				classDirs.get().forEach { it.deleteRecursively() }
				file("build/tmp/compileJava").deleteRecursively()
				file("build/tmp/compileKotlin").deleteRecursively()
				file("build/kspCaches").deleteRecursively()
			}
		}
		finalizedBy("compileJava", "compileKotlin", verify)
	}

	tasks.matching { it.name == jarTaskName }.configureEach { dependsOn(verify) }
	tasks.matching { it.name.startsWith("run") }.configureEach { dependsOn(verify) }
}