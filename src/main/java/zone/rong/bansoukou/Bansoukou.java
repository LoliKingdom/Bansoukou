package zone.rong.bansoukou;

import net.minecraftforge.fml.relauncher.FMLInjectionData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.util.*;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class Bansoukou {

    public static final Logger LOGGER = LogManager.getLogger(Tags.MOD_NAME);

    public static final File HOME;
    public static final Path BANSOUKOU_DIRECTORY;
    public static final Path CACHE_BANSOUKOU_DIRECTORY;
    private static final Path MOD_DIRECTORY;

    static {
        File home = new File(".");
        try {
            Object[] data = FMLInjectionData.data();
            if (data[6] != null) {
                home = (File) data[6];
            }
        } catch (Throwable ignored) { }
        HOME = home;
        Path homePath = home.toPath();
        BANSOUKOU_DIRECTORY = homePath.resolve(Tags.MOD_ID);
        CACHE_BANSOUKOU_DIRECTORY = homePath.resolve("cache").resolve(Tags.MOD_ID);
        MOD_DIRECTORY = homePath.resolve("mods");
    }

    public static Map<Path, Path> init() {
        if (!Files.exists(BANSOUKOU_DIRECTORY) || !Files.isDirectory(BANSOUKOU_DIRECTORY)) {
            LOGGER.warn("Bansoukou folder not found, skipping.");
            return Collections.emptyMap();
        } else {
            return new Bansoukou().run();
        }
    }

    // A cached jar that is truncated, empty or otherwise unreadable must be rebuilt, no matter how recent it is.
    static boolean isReadableZip(Path jar) {
        try (ZipFile ignored = new ZipFile(jar.toFile())) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    static boolean needsPatching(Path patchSource, Path cacheJar) throws IOException {
        if (!Files.exists(cacheJar)) {
            return true;
        }
        if (!isReadableZip(cacheJar)) {
            LOGGER.warn("Cached jar {} is corrupted, rebuilding it.", cacheJar);
            return true;
        }
        FileTime cacheTime = Files.getLastModifiedTime(cacheJar);
        if (Files.isDirectory(patchSource)) {
            try (Stream<Path> walk = Files.walk(patchSource)) {
                return walk.filter(Files::isRegularFile).anyMatch(path -> {
                    try {
                        return Files.getLastModifiedTime(path).compareTo(cacheTime) > 0;
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to read modified time of " + path, e);
                    }
                });
            }
        }
        return Files.getLastModifiedTime(patchSource).compareTo(cacheTime) > 0;
    }

    private static String resolveModFileName(String bareName) {
        String jarName = bareName + ".jar";
        if (Files.exists(MOD_DIRECTORY.resolve(jarName))) {
            return jarName;
        }
        String zipName = bareName + ".zip";
        if (Files.exists(MOD_DIRECTORY.resolve(zipName))) {
            return zipName;
        }
        return null;
    }

    static void patchJar(Path originalJar, Path patchSource, Path cacheJar) throws IOException {
        Path workJar = cacheJar.resolveSibling(cacheJar.getFileName() + ".tmp");
        try {
            Files.copy(originalJar, workJar, StandardCopyOption.REPLACE_EXISTING);

            try (FileSystem jarFs = FileSystems.newFileSystem(workJar, null)) {
                // Remove signature-related files
                Path metaInf = jarFs.getPath("/META-INF");
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

                // Patch over files
                if (Files.isDirectory(patchSource)) {
                    patchFromDirectory(patchSource, jarFs);
                } else {
                    patchFromZip(patchSource, jarFs);
                }
            }

            try {
                Files.move(workJar, cacheJar, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(workJar, cacheJar, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(workJar);
        }
    }

    @FunctionalInterface
    interface IOSupplier {

        InputStream get() throws IOException;

    }

    // Applies one patch entry to the cached jar: empty entry deletes the target, otherwise overwrites it.
    private static void applyPatchEntry(FileSystem jarFs, String entryName, boolean empty, IOSupplier content) throws IOException {
        Path targetPath = jarFs.getPath(entryName);
        if (targetPath.getParent() != null) {
            Files.createDirectories(targetPath.getParent());
        }
        if (empty) {
            LOGGER.debug("Deleting {} as it is empty in the patch.", targetPath);
            Files.deleteIfExists(targetPath);
        } else {
            LOGGER.debug("Patching {}.", targetPath);
            try (InputStream in = content.get()) {
                Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private static void patchFromZip(Path patchJar, FileSystem jarFs) throws IOException {
        try (ZipFile patchZipFile = new ZipFile(patchJar.toFile())) {
            Enumeration<? extends ZipEntry> entries = patchZipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (!entry.isDirectory()) {
                    applyPatchEntry(jarFs, entry.getName(), entry.getSize() == 0L, () -> patchZipFile.getInputStream(entry));
                }
            }
        }
    }

    private static void patchFromDirectory(Path patchDir, FileSystem jarFs) throws IOException {
        try (Stream<Path> walk = Files.walk(patchDir)) {
            Iterator<Path> sources = walk.filter(Files::isRegularFile).iterator();
            while (sources.hasNext()) {
                Path source = sources.next();
                String relative = patchDir.relativize(source).toString().replace(File.separatorChar, '/');
                applyPatchEntry(jarFs, relative, Files.size(source) == 0L, () -> Files.newInputStream(source));
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
                String fileName = patchFile.getFileName().toString();
                String patchName; // target mod file name, with extension
                if (Files.isDirectory(patchFile)) {
                    patchName = resolveModFileName(fileName);
                    if (patchName == null) {
                        LOGGER.error("No .jar/.zip in mods/ matches patch directory {}, skipping.", fileName);
                        continue;
                    }
                } else if (fileName.endsWith(".jar") || fileName.endsWith(".zip")) {
                    patchName = fileName;
                } else {
                    LOGGER.error("{} is not a .jar/.zip file or directory named after the target mod, skipping.", fileName);
                    continue;
                }
                Path originalJar = MOD_DIRECTORY.resolve(patchName);
                if (!Files.isRegularFile(originalJar)) {
                    LOGGER.error("{} does not exist in mods/, skipping patch {}.", patchName, fileName);
                    continue;
                }
                Path cachedJar = CACHE_BANSOUKOU_DIRECTORY.resolve(patchName);
                if (needsPatching(patchFile, cachedJar)) {
                    patchJar(originalJar, patchFile, cachedJar);
                    LOGGER.info("Patching and caching {}", patchName);
                } else {
                    LOGGER.info("{} is up to date, patching not needed.", patchName);
                }
                patch.put(originalJar.toAbsolutePath().normalize(), cachedJar);
            }
        } catch (IOException e) {
            throw new RuntimeException("Unable to gather bansoukou patches", e);
        }

        return patch;
    }

}
