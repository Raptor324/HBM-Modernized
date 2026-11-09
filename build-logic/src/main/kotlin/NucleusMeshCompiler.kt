// file: build-logic/src/main/kotlin/NucleusMeshCompiler.kt
import org.gradle.api.Project
import org.gradle.kotlin.dsl.named
import org.gradle.language.jvm.tasks.ProcessResources
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

private fun triangulatePolygon(verts: Array<FloatArray>): IntArray {
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

	fun cross2D(ax: Double, ay: Double, bx: Double, by: Double, cx: Double, cy: Double): Double =
		(bx - ax) * (cy - ay) - (by - ay) * (cx - ax)

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

private fun compileObjToNmesh(objFile: File, outFile: File, nucleusAssetsRoots: List<File>) {
	fun normalize(x: Float, y: Float, z: Float): FloatArray {
		val len = Math.sqrt(x.toDouble() * x + y.toDouble() * y + z.toDouble() * z)
		if (len == 0.0) return floatArrayOf(0f, 0f, 0f)
		return floatArrayOf((x / len).toFloat(), (y / len).toFloat(), (z / len).toFloat())
	}

	fun resolveMtl(mtlRef: String): File? {
		if (mtlRef.contains(":")) {
			val parts = mtlRef.split(":", limit = 2)
			for (r in nucleusAssetsRoots) {
				val f = File(r, "${parts[0]}/${parts[1]}")
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
			when {
				l.startsWith("newmtl ") -> {
					if (curMtl.isNotEmpty()) mtlMap[curMtl] = MtlInfo(mapKd, kd, ka)
					curMtl = l.substring(7).trim()
					mapKd = ""
					kd = listOf(1f, 1f, 1f)
					ka = listOf(0f, 0f, 0f)
				}
				l.startsWith("map_Kd ") && curMtl.isNotEmpty() -> mapKd = l.substring(7).trim()
				l.startsWith("Kd ") && curMtl.isNotEmpty() -> kd = parseMtlColor(l.substring(3).trim())
				l.startsWith("Ka ") && curMtl.isNotEmpty() -> ka = parseMtlColor(l.substring(3).trim())
			}
		}
		if (curMtl.isNotEmpty()) mtlMap[curMtl] = MtlInfo(mapKd, kd, ka)
	}

	objFile.useLines(Charsets.UTF_8) { lines ->
		for (raw in lines) {
			val l = raw.trim()
			if (l.startsWith("mtllib ")) {
				val f = resolveMtl(l.substring(7).trim())
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
			val vtx = floatArrayOf(
				if (pp[0] == 0.0f) 0.0f else pp[0],
				if (pp[1] == 0.0f) 0.0f else pp[1],
				if (pp[2] == 0.0f) 0.0f else pp[2],
				if (nn[0] == 0.0f) 0.0f else nn[0],
				if (nn[1] == 0.0f) 0.0f else nn[1],
				if (nn[2] == 0.0f) 0.0f else nn[2],
				if (tt[0] == 0.0f) 0.0f else tt[0],
				if (tt[1] == 0.0f) 0.0f else tt[1]
			)
			val k = vtx.joinToString(",") { f -> (if (f == 0.0f) 0.0f else f).toRawBits().toString() }
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
		val tan1 = DoubleArray(n * 3)
		val tan2 = DoubleArray(n * 3)
		for (tri in bt) {
			val a = bv[tri[0]]; val b = bv[tri[1]]; val c = bv[tri[2]]
			val du1 = (b[6] - a[6]).toDouble()
			val dv1 = -(b[7] - a[7]).toDouble()
			val du2 = (c[6] - a[6]).toDouble()
			val dv2 = -(c[7] - a[7]).toDouble()
			val det = du1 * dv2 - du2 * dv1
			if (Math.abs(det) < 1.0e-12) continue
			val r = 1.0 / det
			val e1x = (b[0] - a[0]).toDouble(); val e1y = (b[1] - a[1]).toDouble(); val e1z = (b[2] - a[2]).toDouble()
			val e2x = (c[0] - a[0]).toDouble(); val e2y = (c[1] - a[1]).toDouble(); val e2z = (c[2] - a[2]).toDouble()
			val flat = normalize(e1y.toFloat() * e2z.toFloat() - e1z.toFloat() * e2y.toFloat(),
				e1z.toFloat() * e2x.toFloat() - e1x.toFloat() * e2z.toFloat(),
				e1x.toFloat() * e2y.toFloat() - e1y.toFloat() * e2x.toFloat())
			val sx = (dv2 * e1x - dv1 * e2x) * r
			val sy = (dv2 * e1y - dv1 * e2y) * r
			val sz = (dv2 * e1z - dv1 * e2z) * r
			val tx = (du1 * e2x - du2 * e1x) * r
			val ty = (du1 * e2y - du2 * e1y) * r
			val tz = (du1 * e2z - du2 * e1z) * r
			for (i in 0..2) {
				val vi = tri[i]
				val tOff = vi * 3
				tan1[tOff] += sx; tan1[tOff + 1] += sy; tan1[tOff + 2] += sz
				tan2[tOff] += tx; tan2[tOff + 1] += ty; tan2[tOff + 2] += tz
			}
		}
		packedTangents[key] = IntArray(n) { vi ->
			val nx = bv[vi][3].toDouble(); val ny = bv[vi][4].toDouble(); val nz = bv[vi][5].toDouble()
			val tOff = vi * 3
			var tx = tan1[tOff]; var ty = tan1[tOff + 1]; var tz = tan1[tOff + 2]
			val d = nx * tx + ny * ty + nz * tz
			tx -= nx * d; ty -= ny * d; tz -= nz * d
			val len = Math.sqrt(tx * tx + ty * ty + tz * tz)
			if (len < 1.0e-12) {
				tx = 1.0; ty = 0.0; tz = 0.0
			} else {
				tx /= len; ty /= len; tz /= len
			}
			val cx = ny * tz - nz * ty
			val cy = nz * tx - nx * tz
			val cz = nx * ty - ny * tx
			val dot = cx * tan2[tOff] + cy * tan2[tOff + 1] + cz * tan2[tOff + 2]
			val tw = if (dot < 0.0) -127 else 127
			fun b(v: Double): Int = Math.round(Math.max(-1.0, Math.min(1.0, v)) * 127.0).toInt().coerceIn(-127, 127)
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
		totalBytes += 24 + 24 + 8
		totalBytes += bv.size * 8 * 4 + bv.size * 4
		totalBytes += (bt.size * 3) * (if (bv.size <= 65535) 2 else 4)
	}

	val buf = ByteBuffer.allocate(totalBytes).order(ByteOrder.LITTLE_ENDIAN)
	buf.putInt(0x4E4D5348)
	buf.putInt(3)
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
		writeStr(key.obj); writeStr(key.grp); writeStr(key.mat); writeStr(key.mapKd)
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
		for (v in bv) fb.put(v)
		buf.position(buf.position() + bv.size * 8 * 4)

		val packed = packedTangents[key]!!
		buf.asIntBuffer().put(packed)
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

	FileOutputStream(outFile).use { it.write(buf.array()) }
}

private fun nmeshVersionMatches(f: File): Boolean {
	if (f.length() < 8) return false
	return RandomAccessFile(f, "r").use { raf ->
		val header = ByteArray(8)
		raf.readFully(header)
		ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN).getInt(4) == 3
	}
}

fun Project.registerNucleusMeshCompiler(mcVersion: String) {
	val nucleusAssetsRoots = listOf(
		rootProject.file("src/main/resources/assets"),
		rootProject.file("src/generated/resources/assets"),
	)
	val nucleusNmeshOut = layout.buildDirectory.dir("generated/nmesh")

	val compileMeshesTask = tasks.register("compileNucleusMeshes") {
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
				val rootNorm = root.parentFile.absolutePath.replace('\\', '/')
				root.walkTopDown().filter { it.isFile && it.extension == "obj" }.forEach { obj ->
					val rel = obj.absolutePath.replace('\\', '/').removePrefix("$rootNorm/")
					val out = File(outDir, rel.removeSuffix(".obj") + ".nmesh")
					val mtlFile = File(obj.parentFile, obj.nameWithoutExtension + ".mtl")
					val mtlTime = if (mtlFile.isFile) mtlFile.lastModified() else 0L
					val srcTime = maxOf(obj.lastModified(), mtlTime)
					if (out.exists() && out.lastModified() >= srcTime && nmeshVersionMatches(out)) {
						upToDate++
						return@forEach
					}
					compileObjToNmesh(obj, out, nucleusAssetsRoots)
					compiled++
				}
			}
			if (outDir.isDirectory) {
				val objRoots = nucleusAssetsRoots.filter { it.isDirectory }.map { it.parentFile.absolutePath.replace('\\', '/') }
				outDir.walkTopDown().filter { it.isFile && it.extension == "nmesh" }.forEach { f ->
					val relNmesh = f.absolutePath.replace('\\', '/').removePrefix(outDir.absolutePath.replace('\\', '/') + "/")
					val relObj = relNmesh.removeSuffix(".nmesh") + ".obj"
					if (objRoots.none { File("$it/$relObj").isFile }) f.delete()
				}
			}
			println("compileNucleusMeshes: compiled $compiled, up-to-date $upToDate -> $outDir")
		}
	}

	tasks.named<ProcessResources>("processResources") {
		from(rootProject.file("src/main/resources/aw/$mcVersion.cfg")) {
			rename { "accesstransformer.cfg" }
			into("META-INF")
		}
		exclude("aw/")
		dependsOn(compileMeshesTask)
		from(nucleusNmeshOut)
		exclude("**/*.obj", "**/*.mtl")
	}
}
