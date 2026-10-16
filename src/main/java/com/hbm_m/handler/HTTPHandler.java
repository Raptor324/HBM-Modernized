package com.hbm_m.handler;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.main.MainRegistry;
import dev.architectury.platform.Platform;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class HTTPHandler {

	public static final String MODRINTH_PROJECT_SLUG = "hbms-nuclear-tech-modernized";
	public static final String CURSEFORGE_PROJECT_SLUG = "hbms-nuclear-tech-modernized";

	public static final String BOOSTY_URL = "https://boosty.to/hbmmodernized";
	public static final String SUPPORT_PAGE_URL =
			"https://github.com/Raptor324/HBM-Modernized/blob/main/SUPPORT.md";

	public static List<String> capsule = new ArrayList<>();
	public static List<String> tipOfTheDay = new ArrayList<>();

	public static VersionUpdate modrinthUpdate;
	public static VersionUpdate curseforgeUpdate;

	public static void loadStats() {

		Thread versionChecker = new Thread(() -> {
			try {
				loadVersion();
			} catch (IOException e) {
				MainRegistry.LOGGER.warn("Version checker failed!", e);
			}
			try {
				loadSoyuz();
				loadTips();
			} catch (IOException e) {
				MainRegistry.LOGGER.warn("Version checker failed!", e);
			}
		}, "NTM-Version-Checker");

		versionChecker.setDaemon(true);
		versionChecker.start();
	}

	public static boolean hasAnyUpdate() {
		return modrinthUpdate != null || curseforgeUpdate != null;
	}

	private static void loadVersion() throws IOException {
		String currentVersion = normalizeVersion(Platform.getMod(RefStrings.MODID).getVersion());
		try {
			loadModrinthVersion(currentVersion);
		} catch (IOException e) {
			MainRegistry.LOGGER.warn("Modrinth version check failed!", e);
		}
	}

	private static void loadModrinthVersion(String currentVersion) throws IOException {
		String mcVersion = Platform.getMinecraftVersion();
		String loader = modrinthLoader();

		String query = "game_versions=" + encodeJsonArray(mcVersion) + "&loaders=" + encodeJsonArray(loader);
		URL api = URI.create("https://api.modrinth.com/v2/project/" + MODRINTH_PROJECT_SLUG + "/version?" + query).toURL();

		MainRegistry.LOGGER.info("Searching for new versions on Modrinth ({} / {})...", mcVersion, loader);

		String body = readResponse(api);
		JsonArray versions = JsonParser.parseString(body).getAsJsonArray();
		if (versions.isEmpty()) {
			MainRegistry.LOGGER.info("No Modrinth versions found for {} / {}.", mcVersion, loader);
			return;
		}

		JsonObject latest = versions.get(0).getAsJsonObject();
		String remote = latest.get("version_number").getAsString();
		String versionId = latest.get("id").getAsString();
		String url = "https://modrinth.com/mod/" + MODRINTH_PROJECT_SLUG + "/version/" + versionId;

		if (isRemoteVersionNewer(remote, currentVersion)) {
			modrinthUpdate = new VersionUpdate(remote, url);
			curseforgeUpdate = new VersionUpdate(remote, curseforgeFilesUrl(mcVersion));
			MainRegistry.LOGGER.info("Modrinth update available: {} (current: {})", remote, currentVersion);
		} else {
			MainRegistry.LOGGER.info("Modrinth is up to date (latest: {}, current: {}).", remote, currentVersion);
		}
	}

	/** Ссылка на страницу файлов CF без API — зеркало релиза с Modrinth. */
	private static String curseforgeFilesUrl(String mcVersion) {
		return "https://www.curseforge.com/minecraft/mc-mods/" + CURSEFORGE_PROJECT_SLUG
				+ "/files/all?page=1&pageSize=20&version=" + urlEncode(mcVersion);
	}

	private static String modrinthLoader() {
		//? if neoforge {
		/*return "neoforge";
		*///?} else if forge {
		return "forge";
		//?} else {
		/*return "fabric";
		*///?}
	}

	private static String encodeJsonArray(String value) {
		return urlEncode("[\"" + value + "\"]");
	}

	private static String urlEncode(String value) {
		return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8);
	}

	static String normalizeVersion(String version) {
		int plus = version.indexOf('+');
		if (plus >= 0) {
			version = version.substring(0, plus);
		}
		return version.trim();
	}

	static boolean isRemoteVersionNewer(String remote, String current) {
		if (remote.isEmpty() || current.isEmpty()) {
			return false;
		}
		if (remote.equals(current)) {
			return false;
		}
		return compareVersionTokens(remote) > compareVersionTokens(current);
	}

	private static long compareVersionTokens(String version) {
		String[] mainAndSuffix = version.split("-", 2);
		String[] parts = mainAndSuffix[0].split("\\.");
		long score = 0;
		long multiplier = 1_000_000L;
		for (String part : parts) {
			score += parseNumericPrefix(part) * multiplier;
			multiplier /= 1000;
			if (multiplier < 1) {
				break;
			}
		}
		return score;
	}

	private static long parseNumericPrefix(String part) {
		int end = 0;
		while (end < part.length() && Character.isDigit(part.charAt(end))) {
			end++;
		}
		if (end == 0) {
			return 0;
		}
		try {
			return Long.parseLong(part.substring(0, end));
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static String readResponse(URL url) throws IOException {
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestProperty("User-Agent", "HBM-Modernized/" + Platform.getMod(RefStrings.MODID).getVersion());
		connection.setRequestProperty("Accept", "application/json");
		connection.setConnectTimeout(10_000);
		connection.setReadTimeout(10_000);

		int code = connection.getResponseCode();
		java.io.InputStream stream = code >= 200 && code < 300 ? connection.getInputStream() : connection.getErrorStream();
		if (stream == null) {
			connection.disconnect();
			throw new IOException("HTTP " + code + " from " + url);
		}

		try (BufferedReader in = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
			StringBuilder sb = new StringBuilder();
			String line;
			while ((line = in.readLine()) != null) {
				sb.append(line);
			}
			if (code < 200 || code >= 300) {
				throw new IOException("HTTP " + code + " from " + url + ": " + sb);
			}
			return sb.toString();
		} finally {
			connection.disconnect();
		}
	}

	private static void loadSoyuz() throws IOException {

		URL github = URI.create("https://gist.githubusercontent.com/HbmMods/a1cad71d00b6915945a43961d0037a43/raw/soyuz_holo").toURL();
		BufferedReader in = new BufferedReader(new InputStreamReader(github.openStream(), StandardCharsets.UTF_8));

		String line;
		while ((line = in.readLine()) != null) {
			capsule.add(line);
		}
		in.close();
	}

	private static void loadTips() throws IOException {

		URL github = URI.create("https://gist.githubusercontent.com/HbmMods/a03c66ba160184e12f43de826b30c096/raw/tip_of_the_day").toURL();
		BufferedReader in = new BufferedReader(new InputStreamReader(github.openStream(), StandardCharsets.UTF_8));

		String line;
		while ((line = in.readLine()) != null) {
			tipOfTheDay.add(line);
		}
		in.close();
	}

	public static final class VersionUpdate {
		public final String versionNumber;
		public final String downloadUrl;

		public VersionUpdate(String versionNumber, String downloadUrl) {
			this.versionNumber = versionNumber;
			this.downloadUrl = downloadUrl;
		}
	}
}
