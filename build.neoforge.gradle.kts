import java.io.BufferedOutputStream
import java.io.DataOutputStream
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

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

// ── Nucleus mesh compiler: .obj -> .nmesh ──────────────────────────────────────
// Оффлайн-компиляция Wavefront .obj из assets-корней в бинарный формат .nmesh,
// который читает NMeshData.read (MAGIC = 0x4E4D5348 'NMSH' big-endian, VERSION = 1).
// Сварка вершин 1:1 как в рантайм-фолбэке: точное сравнение сырых битов всех
// 8 float (x,y,z,nx,ny,nz,u,v); нормали из vn нормализуются, отсутствующие
// считаются per-face (cross product) и применяются ко всей грани; полигоны
// триангулируются веером (0, i, i+1). Тангенты считаются методом Lengyel по
// сваренному треугольному супу каждого sub-mesh (по bucket object/group/material
// в порядке первого появления) и пакуются в 4 signed byte (как at_tangent).
// Вершины без тангент-вклада получают плейсхолдер 0x7F00007F (как в рантайме).
val nucleusAssetsRoots = listOf(
    rootProject.file("src/main/resources/assets"),
    rootProject.file("src/generated/resources/assets"),
)

fun triangulatePolygon(verts: Array<FloatArray>): IntArray {
    val n = verts.size
    if (n < 3) return IntArray(0)
    if (n == 3) return intArrayOf(0, 1, 2)

    var nx = 0.0; var ny = 0.0; var nz = 0.0
    for (i in 0 until n) {
        val j = (i + 1) % n
        val vi = verts[i]; val vj = verts[j]
        nx += (vi[1] - vj[1]).toDouble() * (vi[2] + vj[2]).toDouble()
        ny += (vi[2] - vj[2]).toDouble() * (vi[0] + vj[0]).toDouble()
        nz += (vi[0] - vj[0]).toDouble() * (vi[1] + vj[1]).toDouble()
    }
    val lenSq = nx * nx + ny * ny + nz * nz
    if (lenSq < 1e-12) {
        val fan = IntArray((n - 2) * 3)
        var idx = 0
        for (i in 1 until n - 1) {
            fan[idx++] = 0; fan[idx++] = i; fan[idx++] = i + 1
        }
        return fan
    }

    val absX = Math.abs(nx); val absY = Math.abs(ny); val absZ = Math.abs(nz)
    val u = DoubleArray(n); val v = DoubleArray(n)
    if (absZ >= absX && absZ >= absY) {
        for (i in 0 until n) { u[i] = verts[i][0].toDouble(); v[i] = verts[i][1].toDouble() }
    } else if (absY >= absX && absY >= absZ) {
        for (i in 0 until n) { u[i] = verts[i][0].toDouble(); v[i] = verts[i][2].toDouble() }
    } else {
        for (i in 0 until n) { u[i] = verts[i][1].toDouble(); v[i] = verts[i][2].toDouble() }
    }

    var area2D = 0.0
    for (i in 0 until n) {
        val j = (i + 1) % n
        area2D += (u[i] * v[j] - u[j] * v[i])
    }
    if (Math.abs(area2D) < 1e-12) {
        val fan = IntArray((n - 2) * 3)
        var idx = 0
        for (i in 1 until n - 1) {
            fan[idx++] = 0; fan[idx++] = i; fan[idx++] = i + 1
        }
        return fan
    }
    val areaSign = if (area2D > 0) 1.0 else -1.0

    fun cross2D(ax: Double, ay: Double, bx: Double, by: Double, cx: Double, cy: Double): Double {
        return (bx - ax) * (cy - ay) - (by - ay) * (cx - ax)
    }

    fun pointInTriangle(px: Double, py: Double, ax: Double, ay: Double, bx: Double, by: Double, cx: Double, cy: Double): Boolean {
        val s1 = cross2D(ax, ay, bx, by, px, py) * areaSign
        val s2 = cross2D(bx, by, cx, cy, px, py) * areaSign
        val s3 = cross2D(cx, cy, ax, ay, px, py) * areaSign
        return s1 >= -1e-7 && s2 >= -1e-7 && s3 >= -1e-7
    }

    val active = IntArray(n) { it }
    var m = n
    val out = IntArray((n - 2) * 3)
    var triCursor = 0

    while (m > 3) {
        var earIdx = -1
        for (k in 0 until m) {
            val prev = active[(k - 1 + m) % m]
            val curr = active[k]
            val next = active[(k + 1) % m]

            val c = cross2D(u[prev], v[prev], u[curr], v[curr], u[next], v[next])
            if (c * areaSign <= 1e-7) continue

            var ear = true
            for (j in 0 until m) {
                val p = active[j]
                if (p == prev || p == curr || p == next) continue
                if (pointInTriangle(u[p], v[p], u[prev], v[prev], u[curr], v[curr], u[next], v[next])) {
                    ear = false
                    break
                }
            }
            if (ear) {
                earIdx = k
                break
            }
        }

        if (earIdx < 0) {
            var maxCross = -Double.MAX_VALUE
            var bestK = 0
            for (k in 0 until m) {
                val prev = active[(k - 1 + m) % m]
                val curr = active[k]
                val next = active[(k + 1) % m]
                val sc = cross2D(u[prev], v[prev], u[curr], v[curr], u[next], v[next]) * areaSign
                if (sc > maxCross) {
                    maxCross = sc
                    bestK = k
                }
            }
            earIdx = bestK
        }

        val prev = active[(earIdx - 1 + m) % m]
        val curr = active[earIdx]
        val next = active[(earIdx + 1) % m]
        out[triCursor++] = prev
        out[triCursor++] = curr
        out[triCursor++] = next

        for (i in earIdx until m - 1) {
            active[i] = active[i + 1]
        }
        m--
    }

    out[triCursor++] = active[0]
    out[triCursor++] = active[1]
    out[triCursor++] = active[2]
    return out
}

fun compileObjToNmesh(objFile: File, outFile: File) {
    fun normalize(x: Float, y: Float, z: Float): FloatArray {
        val len = Math.sqrt(x.toDouble() * x + y.toDouble() * y + z.toDouble() * z)
        if (len == 0.0) return floatArrayOf(0f, 0f, 0f)
        return floatArrayOf((x / len).toFloat(), (y / len).toFloat(), (z / len).toFloat())
    }

    fun resolveMtl(mtlRef: String): File? {
        if (mtlRef.contains(":")) {
            val parts = mtlRef.split(":", limit = 2)
            val ns = parts[0]
            val p = parts[1]
            for (r in nucleusAssetsRoots) {
                val f = File(r, "$ns/$p")
                if (f.isFile) return f
            }
        } else {
            val f = File(objFile.parentFile, mtlRef)
            if (f.isFile) return f
            for (r in nucleusAssetsRoots) {
                val relF = File(r, mtlRef)
                if (relF.isFile) return relF
            }
        }
        return null
    }

    // MTL-данные материала: путь map_Kd + диффузный (Kd) и фоновый (Ka) цвета.
    // Семантика Forge ObjMaterialLibrary.Material: Kd по умолчанию (1,1,1), Ka — (0,0,0).
    data class MtlInfo(val mapKd: String, val kd: List<Float>, val ka: List<Float>)
    val mtlMap = mutableMapOf<String, MtlInfo>()
    fun parseMtlColor(args: String): List<Float> {
        val p = args.split(Regex("\\s+"))
        fun f(i: Int): Float = if (i < p.size) (p[i].toFloatOrNull() ?: 0f) else 0f
        return listOf(f(0), f(1), f(2))
    }
    fun parseMtl(file: File) {
        var curMtl = ""
        var mapKd = ""
        var kd = listOf(1f, 1f, 1f)
        var ka = listOf(0f, 0f, 0f)
        file.forEachLine(Charsets.UTF_8) { raw ->
            val l = raw.trim()
            if (l.isEmpty() || l.startsWith("#")) return@forEachLine
            if (l.startsWith("newmtl ")) {
                if (curMtl.isNotEmpty()) mtlMap[curMtl] = MtlInfo(mapKd, kd, ka)
                curMtl = l.substring(7).trim()
                mapKd = ""
                kd = listOf(1f, 1f, 1f)
                ka = listOf(0f, 0f, 0f)
            } else if (l.startsWith("map_Kd ") && curMtl.isNotEmpty()) {
                mapKd = l.substring(7).trim()
            } else if (l.startsWith("Kd ") && curMtl.isNotEmpty()) {
                kd = parseMtlColor(l.substring(3).trim())
            } else if (l.startsWith("Ka ") && curMtl.isNotEmpty()) {
                ka = parseMtlColor(l.substring(3).trim())
            }
        }
        if (curMtl.isNotEmpty()) mtlMap[curMtl] = MtlInfo(mapKd, kd, ka)
    }

    // Pre-scan for companion .mtl
    objFile.useLines(Charsets.UTF_8) { lines ->
        for (raw in lines) {
            val l = raw.trim()
            if (l.startsWith("mtllib ")) {
                val ref = l.substring(7).trim()
                val f = resolveMtl(ref)
                if (f != null && f.isFile) parseMtl(f)
            }
        }
    }
    if (mtlMap.isEmpty()) {
        val fallback = File(objFile.parentFile, objFile.nameWithoutExtension + ".mtl")
        if (fallback.isFile) parseMtl(fallback)
    }

    data class BucketKey(val obj: String, val grp: String, val mat: String, val mapKd: String)

    val pos = mutableListOf<FloatArray>()
    val uv = mutableListOf<FloatArray>()
    val nrm = mutableListOf<FloatArray>()
    var objName = ""
    var grpName = ""
    var matName = ""
    var mapKdName = ""
    var facesSinceLastO = false

    val order = mutableListOf<BucketKey>()
    val bucketVerts = linkedMapOf<BucketKey, MutableList<FloatArray>>()
    val bucketWeld = linkedMapOf<BucketKey, HashMap<String, Int>>()
    val bucketTris = linkedMapOf<BucketKey, MutableList<IntArray>>()

    fun bucket(key: BucketKey): Triple<MutableList<FloatArray>, HashMap<String, Int>, MutableList<IntArray>> {
        if (!bucketVerts.containsKey(key)) {
            order.add(key)
            bucketVerts[key] = mutableListOf()
            bucketWeld[key] = HashMap()
            bucketTris[key] = mutableListOf()
        }
        return Triple(bucketVerts[key]!!, bucketWeld[key]!!, bucketTris[key]!!)
    }

    fun addFace(corners: List<Triple<Int, Int, Int>>) {
        if (corners.size < 3) return
        val p0 = pos[corners[0].first]
        val p1 = pos[corners[1].first]
        val p2 = pos[corners[2].first]
        val e1x = p1[0] - p0[0]; val e1y = p1[1] - p0[1]; val e1z = p1[2] - p0[2]
        val e2x = p2[0] - p0[0]; val e2y = p2[1] - p0[1]; val e2z = p2[2] - p0[2]
        val flat = normalize(e1y * e2z - e1z * e2y, e1z * e2x - e1x * e2z, e1x * e2y - e1y * e2x)
        val key = BucketKey(objName, grpName, matName, mapKdName)
        val (bv, bw, bt) = bucket(key)
        val idxs = IntArray(corners.size)
        for ((i, c) in corners.withIndex()) {
            val pp = pos[c.first]
            val tt = if (c.second >= 0 && c.second < uv.size) uv[c.second] else floatArrayOf(0f, 0f)
            val nn = if (c.third >= 0 && c.third < nrm.size) {
                val n = nrm[c.third]; normalize(n[0], n[1], n[2])
            } else flat
            val vtx = floatArrayOf(pp[0], pp[1], pp[2], nn[0], nn[1], nn[2], tt[0], tt[1])
            val k = vtx.joinToString(",") { f -> f.toRawBits().toString() }
            idxs[i] = bw.getOrPut(k) { bv.size.also { bv.add(vtx) } }
        }
        val faceVerts = Array(corners.size) { i -> pos[corners[i].first] }
        val triIndices = triangulatePolygon(faceVerts)
        for (i in 0 until triIndices.size step 3) {
            bt.add(intArrayOf(idxs[triIndices[i]], idxs[triIndices[i + 1]], idxs[triIndices[i + 2]]))
        }
    }

    objFile.useLines(Charsets.UTF_8) { lines ->
        for (raw in lines) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) continue
            val t = line.split(Regex("\\s+"))
            when (t[0]) {
                "v" -> pos.add(floatArrayOf(t[1].toFloat(), t[2].toFloat(), t[3].toFloat()))
                "vt" -> uv.add(floatArrayOf(t[1].toFloat(), t[2].toFloat()))
                "vn" -> nrm.add(normalize(t[1].toFloat(), t[2].toFloat(), t[3].toFloat()))
                "o" -> {
                    objName = if (t.size > 1) line.substring(2).trim() else ""
                    grpName = ""
                    facesSinceLastO = false
                }
                "g" -> {
                    if (facesSinceLastO) {
                        objName = ""
                        facesSinceLastO = false
                    }
                    grpName = if (t.size > 1) line.substring(2).trim() else ""
                }
                "usemtl" -> {
                    matName = if (t.size > 1) line.substring(7).trim() else ""
                    mapKdName = mtlMap[matName]?.mapKd ?: ""
                }
                "f" -> {
                    facesSinceLastO = true
                    if (t.size < 4) continue
                    fun idx(s: String, n: Int): Int {
                        if (s.isEmpty()) return -1
                        val v = s.toInt()
                        return if (v > 0) v - 1 else n + v
                    }
                    val corners = ArrayList<Triple<Int, Int, Int>>(t.size - 1)
                    for (i in 1 until t.size) {
                        val p = t[i].split("/")
                        corners.add(Triple(
                            idx(p[0], pos.size),
                            if (p.size > 1) idx(p[1], uv.size) else -1,
                            if (p.size > 2) idx(p[2], nrm.size) else -1,
                        ))
                    }
                    addFace(corners)
                }
            }
        }
    }

    val packedTangents = HashMap<BucketKey, IntArray>()
    for (key in order) {
        val bv = bucketVerts[key]!!
        val bt = bucketTris[key]!!
        val n = bv.size
        val tAcc = DoubleArray(n * 3)
        val sAcc = DoubleArray(n)
        for (tri in bt) {
            val a = bv[tri[0]]; val b = bv[tri[1]]; val c = bv[tri[2]]
            val du1 = (b[6] - a[6]).toDouble(); val dv1 = (b[7] - a[7]).toDouble()
            val du2 = (c[6] - a[6]).toDouble(); val dv2 = (c[7] - a[7]).toDouble()
            val det = du1 * dv2 - du2 * dv1
            if (Math.abs(det) < 1.0e-12) continue
            val e1x = (b[0] - a[0]).toDouble(); val e1y = (b[1] - a[1]).toDouble(); val e1z = (b[2] - a[2]).toDouble()
            val e2x = (c[0] - a[0]).toDouble(); val e2y = (c[1] - a[1]).toDouble(); val e2z = (c[2] - a[2]).toDouble()
            val sx = (dv2 * e1x - dv1 * e2x) / det
            val sy = (dv2 * e1y - dv1 * e2y) / det
            val sz = (dv2 * e1z - dv1 * e2z) / det
            for (i in 0..2) {
                val vi = tri[i]
                tAcc[vi * 3] += sx; tAcc[vi * 3 + 1] += sy; tAcc[vi * 3 + 2] += sz
                sAcc[vi] += if (det > 0) 1.0 else -1.0
            }
        }
        packedTangents[key] = IntArray(n) { vi ->
            if (sAcc[vi] == 0.0) return@IntArray 0x7F00007F
            val nx = bv[vi][3].toDouble(); val ny = bv[vi][4].toDouble(); val nz = bv[vi][5].toDouble()
            var tx = tAcc[vi * 3]; var ty = tAcc[vi * 3 + 1]; var tz = tAcc[vi * 3 + 2]
            val d = nx * tx + ny * ty + nz * tz
            tx -= nx * d; ty -= ny * d; tz -= nz * d
            val len = Math.sqrt(tx * tx + ty * ty + tz * tz)
            if (len < 1.0e-12) return@IntArray 0x7F00007F
            tx /= len; ty /= len; tz /= len
            val tw = if (sAcc[vi] > 0) 127 else -127
            fun b(v: Double): Int = Math.round(v * 127.0).toInt().coerceIn(-127, 127)
            (b(tx) and 0xFF) or ((b(ty) and 0xFF) shl 8) or ((b(tz) and 0xFF) shl 16) or ((tw and 0xFF) shl 24)
        }
    }

    outFile.parentFile.mkdirs()
    fun strBytes(s: String): ByteArray = s.toByteArray(Charsets.UTF_8)
    var totalBytes = 12
    for (key in order) {
        val bv = bucketVerts[key]!!
        val bt = bucketTris[key]!!
        totalBytes += 2 + strBytes(key.obj).size
        totalBytes += 2 + strBytes(key.grp).size
        totalBytes += 2 + strBytes(key.mat).size
        totalBytes += 2 + strBytes(key.mapKd).size
        totalBytes += 24 // Kd rgb + Ka rgb
        totalBytes += 24
        totalBytes += 8
        totalBytes += bv.size * 8 * 4
        totalBytes += bv.size * 4
        totalBytes += (bt.size * 3) * (if (bv.size <= 65535) 2 else 4)
    }

    val buf = ByteBuffer.allocate(totalBytes).order(ByteOrder.LITTLE_ENDIAN)
    buf.putInt(0x4E4D5348) // MAGIC 'NMSH'
    buf.putInt(3)          // VERSION 3 (Kd/Ka на sub-mesh)
    buf.putInt(order.size)

    fun writeStr(s: String) {
        val b = strBytes(s)
        buf.putShort(b.size.toShort())
        buf.put(b)
    }

    for (key in order) {
        val bv = bucketVerts[key]!!
        val bt = bucketTris[key]!!
        val mtl = mtlMap[key.mat]
        val kd = mtl?.kd ?: listOf(1f, 1f, 1f)
        val ka = mtl?.ka ?: listOf(0f, 0f, 0f)
        writeStr(key.obj)
        writeStr(key.grp)
        writeStr(key.mat)
        writeStr(key.mapKd)
        buf.putFloat(kd[0]); buf.putFloat(kd[1]); buf.putFloat(kd[2])
        buf.putFloat(ka[0]); buf.putFloat(ka[1]); buf.putFloat(ka[2])

        var minX = Float.MAX_VALUE; var minY = Float.MAX_VALUE; var minZ = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE; var maxY = -Float.MAX_VALUE; var maxZ = -Float.MAX_VALUE
        for (v in bv) {
            if (v[0] < minX) minX = v[0]; if (v[1] < minY) minY = v[1]; if (v[2] < minZ) minZ = v[2]
            if (v[0] > maxX) maxX = v[0]; if (v[1] > maxY) maxY = v[1]; if (v[2] > maxZ) maxZ = v[2]
        }
        if (bv.isEmpty()) { minX = 0f; minY = 0f; minZ = 0f; maxX = 0f; maxY = 0f; maxZ = 0f }
        buf.putFloat(minX); buf.putFloat(minY); buf.putFloat(minZ)
        buf.putFloat(maxX); buf.putFloat(maxY); buf.putFloat(maxZ)
        buf.putInt(bv.size)
        buf.putInt(bt.size * 3)

        val fb = buf.asFloatBuffer()
        for (v in bv) {
            fb.put(v)
        }
        buf.position(buf.position() + bv.size * 8 * 4)

        val packed = packedTangents[key]!!
        val ib = buf.asIntBuffer()
        ib.put(packed)
        buf.position(buf.position() + bv.size * 4)

        if (bv.size <= 65535) {
            val sb = buf.asShortBuffer()
            for (tri in bt) for (i in 0..2) sb.put(tri[i].toShort())
            buf.position(buf.position() + bt.size * 3 * 2)
        } else {
            val idxBuf = buf.asIntBuffer()
            for (tri in bt) for (i in 0..2) idxBuf.put(tri[i])
            buf.position(buf.position() + bt.size * 3 * 4)
        }
    }

    FileOutputStream(outFile).use { fos ->
        fos.write(buf.array())
    }
}
val nucleusNmeshOut = layout.buildDirectory.dir("generated/nmesh")

/** true, если существующий .nmesh записан в текущей версии формата NMeshData.VERSION. */
fun nmeshVersionMatches(f: File): Boolean {
    if (f.length() < 8) return false
    val raf = RandomAccessFile(f, "r")
    try {
        val header = ByteArray(8)
        raf.readFully(header)
        return ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN).getInt(4) == 3
    } catch (e: Exception) {
        return false
    } finally {
        raf.close()
    }
}

tasks.register("compileNucleusMeshes") {
    group = "build"
    description = "Compiles assets/**/*.obj into binary .nmesh (NMeshData format) under build/generated/nmesh"
    nucleusAssetsRoots.filter { it.isDirectory }.forEach { inputs.dir(it) }
    outputs.dir(nucleusNmeshOut)
    doLast {
        val outDir = nucleusNmeshOut.get().asFile
        var compiled = 0
        var upToDate = 0
        for (root in nucleusAssetsRoots) {
            if (!root.isDirectory) continue
            // rel считается от resources-корня, чтобы в выводе сохранился префикс assets/
            val rootNorm = root.parentFile.absolutePath.replace('\\', '/')
            root.walkTopDown().filter { it.isFile && it.extension == "obj" }.forEach { obj ->
                val rel = obj.absolutePath.replace('\\', '/').removePrefix("$rootNorm/")
                val out = File(outDir, rel.removeSuffix(".obj") + ".nmesh")
                // Грубая инкрементальность: пропуск если .nmesh новее .obj (Gradle up-to-date
                // скипает весь таск по inputs/outputs; здесь per-file фолбэк).
                if (out.exists() && out.lastModified() >= obj.lastModified() && nmeshVersionMatches(out)) {
                    upToDate++
                    return@forEach
                }
                compileObjToNmesh(obj, out)
                compiled++
            }
        }
        // Чистим осиротевшие .nmesh (от удалённых .obj), чтобы они не попали в jar.
        if (outDir.isDirectory) {
            val objRoots = nucleusAssetsRoots.filter { it.isDirectory }.map { it.parentFile.absolutePath.replace('\\', '/') }
            outDir.walkTopDown().filter { it.isFile && it.extension == "nmesh" }.forEach { f ->
                val relNmesh = f.absolutePath.replace('\\', '/').removePrefix(outDir.absolutePath.replace('\\', '/') + "/")
                val relObj = relNmesh.removeSuffix(".nmesh") + ".obj"
                val stillExists = objRoots.any { File("$it/$relObj").isFile }
                if (!stillExists) f.delete()
            }
        }
        println("compileNucleusMeshes: compiled $compiled, up-to-date $upToDate -> ${outDir}")
    }
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
	// Скомпилированные .nmesh вместо сырых .obj/.mtl (те в jar не нужны).
	dependsOn("compileNucleusMeshes")
	from(nucleusNmeshOut)
	exclude("**/*.obj", "**/*.mtl")
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
	val jarFile = File(dir, "hbm-auto-hotswap-agent-v12.jar")
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
		        s_inst = inst;
			        scan(roots, false);
		        Thread watcher = new Thread(() -> loop(roots, inst), "HBM-AutoHotSwap");
		        watcher.setDaemon(true);
		        watcher.start();
		        System.out.println("[HBM-HotSwap v6] Autonomous SHA-256 watcher started: " + roots);
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

			    private static Object findMemberByReturn(Object owner, String returnSimpleName, String debugTag) {
			        // Walk the WHOLE class hierarchy: at runtime Minecraft uses ForgeGui (a Gui
			        // subclass), and getDeclaredMethods/getDeclaredFields see only per-class members.
			        for (Class<?> cls = owner.getClass(); cls != null && cls != Object.class; cls = cls.getSuperclass()) {
			        // Methods first: zero-arg methods whose return type matches. Each member is
			        // guarded individually - one unresolvable type anywhere in the class must not
			        // abort the whole scan before the wanted member is reached.
			        java.lang.reflect.Method[] methods = null;
			        try {
			            methods = cls.getDeclaredMethods();
			        } catch (Throwable t) {
			            System.out.println("[HBM-HotSwap] postChat: " + debugTag + ": getDeclaredMethods failed on " + cls.getName() + ": " + t);
			        }
			        if (methods != null) {
			            for (java.lang.reflect.Method m : methods) {
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
			        // Then fields of the wanted type (getChat may be absent under SRG names).
			        try {
			            for (java.lang.reflect.Field f : cls.getDeclaredFields()) {
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
			        }
			        StringBuilder sb = new StringBuilder();
			        try {
			            for (Class<?> c = owner.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
			                for (java.lang.reflect.Method m : c.getDeclaredMethods()) {
			                    if (m.getParameterCount() == 0 && m.getReturnType() != void.class) {
			                        sb.append(m.getName()).append("->").append(m.getReturnType().getSimpleName()).append(" ");
			                    }
			                }
			            }
			        } catch (Throwable ignored) {}
			        System.out.println("[HBM-HotSwap] postChat: " + debugTag + ": no member of type " + returnSimpleName
			                + " on " + owner.getClass().getName() + "; no-arg methods: " + sb);
			        return null;
			    }

			    private static Instrumentation s_inst;

			    private static void sendChatLine(Object chat, Class<?> component, String text) {
			        try {
			            Object msg = component.getMethod("literal", String.class).invoke(null, text);
			            for (java.lang.reflect.Method m : chat.getClass().getMethods()) {
			                if (m.getParameterCount() == 1
			                        && m.getParameterTypes()[0].getName().equals(component.getName())) {
			                    m.invoke(chat, msg);
			                    return;
			                }
			            }
			            System.out.println("[HBM-HotSwap] postChat: addMessage(Component) not found");
			        } catch (Throwable t) {
			            System.out.println("[HBM-HotSwap] postChat line failed: " + t);
			        }
			    }

			    private static void postChat(String text) {
			        try {
			            if (s_inst == null) {
			                System.out.println("[HBM-HotSwap] postChat: no instrumentation, skipping");
			                return;
			            }
			            Class<?> mcClass = null;
			            for (Class<?> c : s_inst.getAllLoadedClasses()) {
			                if (c.getName().equals("net.minecraft.client.Minecraft")) {
			                    mcClass = c;
			                    break;
			                }
			            }
			            if (mcClass == null) {
			                System.out.println("[HBM-HotSwap] postChat: Minecraft class is not loaded yet");
			                return;
			            }
			            Object mc = null;
			            Throwable lastErr = null;
			            for (java.lang.reflect.Method m : mcClass.getDeclaredMethods()) {
			                if (java.lang.reflect.Modifier.isStatic(m.getModifiers())
			                        && m.getParameterCount() == 0 && m.getReturnType() == mcClass) {
			                    try {
			                        m.setAccessible(true);
			                        mc = m.invoke(null);
			                    } catch (Throwable t) {
			                        lastErr = t;
			                    }
			                    if (mc != null) break;
			                }
			            }
			            if (mc == null) {
			                System.out.println("[HBM-HotSwap] postChat: no working Minecraft accessor, loader="
			                        + mcClass.getClassLoader() + (lastErr != null ? ", last error: " + lastErr : ""));
			                return;
			            }
			            final Object mcRef = mc;
			            Runnable task = () -> {
			                try {
			                    Object gui = findMemberByReturn(mcRef, "Gui", "Gui");
			                    if (gui == null) {
			                        System.out.println("[HBM-HotSwap] postChat: Gui accessor not found");
			                        return;
			                    }
			                    Object chat = findMemberByReturn(gui, "ChatComponent", "chat");
			                    if (chat == null) {
			                        System.out.println("[HBM-HotSwap] postChat: ChatComponent accessor not found");
			                        return;
			                    }
			                    Class<?> component = Class.forName("net.minecraft.network.chat.Component",
			                            true, chat.getClass().getClassLoader());
			                    sendChatLine(chat, component, text);
			                } catch (Throwable t) {
			                    System.out.println("[HBM-HotSwap] postChat task failed: " + t);
			                }
			            };
			            for (java.lang.reflect.Method m : mcClass.getMethods()) {
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