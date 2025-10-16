package zone.rong.bansoukou;

import net.minecraftforge.fml.relauncher.FMLInjectionData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class Bansoukou {

    public static final File HOME = (File) FMLInjectionData.data()[6];
    public static final Logger LOGGER = LogManager.getLogger(Tags.MOD_NAME);

    private static final Path HOME_PATH = HOME.toPath();
    public static final Path BANSOUKOU_DIRECTORY = HOME_PATH.resolve(Tags.MOD_ID);
    public static final Path CACHE_BANSOUKOU_DIRECTORY = HOME_PATH.resolve("cache").resolve(Tags.MOD_ID);

    private static final Path MOD_DIRECTORY = HOME_PATH.resolve("mods");

    public static Map<Path, Path> init() {
        if (!Files.exists(BANSOUKOU_DIRECTORY) || !Files.isDirectory(BANSOUKOU_DIRECTORY)) {
            LOGGER.warn("Bansoukou folder not found, skipping.");
            return Collections.emptyMap();
        } else {
            return new Bansoukou().run();
        }
    }

    private static boolean needsPatching(Path patchJar, Path cacheJar) throws IOException {
        if (!Files.exists(cacheJar)) {
            return true;
        }
        return Files.getLastModifiedTime(patchJar).compareTo(Files.getLastModifiedTime(cacheJar)) > 0;
    }

    private static void patchJar(Path originalJar, Path patchJar, Path cacheJar) throws IOException {
        Files.copy(originalJar, cacheJar, StandardCopyOption.REPLACE_EXISTING);

        // Remove signature-related files
        try (FileSystem cacheFileSystem = FileSystems.newFileSystem(cacheJar, null)) {
            Path metaInf = cacheFileSystem.getPath("/META-INF");
            if (Files.exists(metaInf) && Files.isDirectory(metaInf)) {
                try (Stream<Path> walk = Files.list(metaInf)) {
                    walk.filter(path -> {
                        String name = path.getFileName().toString().toLowerCase();
                        return name.endsWith(".sf") || name.endsWith(".rsa") || name.endsWith(".dsa");
                    }).forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            throw new RuntimeException("Failed to delete " + path);
                        }
                    });
                }
            }
        }

        // Patch over files
        try (ZipFile patchZipFile = new ZipFile(patchJar.toFile());
             FileSystem jarFs = FileSystems.newFileSystem(cacheJar, null)) {
            Enumeration<? extends ZipEntry> entries = patchZipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (!entry.isDirectory()) {
                    Path targetPath = jarFs.getPath(entry.getName());
                    if (targetPath.getParent() != null) {
                        Files.createDirectories(targetPath.getParent());
                    }
                    Files.copy(patchZipFile.getInputStream(entry), targetPath, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private Bansoukou() { }

    public Map<Path, Path> run() {
        try {
            Files.createDirectories(CACHE_BANSOUKOU_DIRECTORY);
        } catch (IOException e) {
            throw new RuntimeException("Unable to create Bansoukou's cache directory!", e);
        }

        Map<Path, Path> patch = new HashMap<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(BANSOUKOU_DIRECTORY)) {
            for (Path patchFile : stream) {
                if (Files.isDirectory(patchFile)) {
                    LOGGER.error("{} is a directory! Unlike Bansoukou v4 and before, your patches should be zipped up in a jar file. If the mod you are patching is a zip file, ", patchFile);
                    continue;
                }
                String patchName = patchFile.getFileName().toString();
                if (!(patchName.endsWith(".jar") || patchName.endsWith("zip"))) {
                    LOGGER.error("{} is not a .jar or .zip file, skipping.", patchName);
                    continue;
                }
                Path originalJar = MOD_DIRECTORY.resolve(patchName);
                Path cachedJar = CACHE_BANSOUKOU_DIRECTORY.resolve(patchName);
                if (needsPatching(patchFile, cachedJar)) {
                    patchJar(originalJar, patchFile, cachedJar);
                    LOGGER.info("Patching and caching {}", patchName);
                } else {
                    LOGGER.info("{} is up to date, patching not needed.", patchName);
                }
                patch.put(originalJar.toAbsolutePath(), cachedJar);
            }
        } catch (IOException e) {
            throw new RuntimeException("Unable to gather bansoukou patches", e);
        }

        return patch;
    }

}
