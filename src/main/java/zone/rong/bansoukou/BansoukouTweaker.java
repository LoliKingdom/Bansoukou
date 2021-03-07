package zone.rong.bansoukou;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URI;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.ZipException;

public class BansoukouTweaker implements ITweaker {

    public static final Logger LOGGER = LogManager.getLogger("Bansoukou");

    public BansoukouTweaker() throws IOException {
        LOGGER.info("Ikimasu!");
        File bansoukouRoot = new File(Launch.minecraftHome, "bansoukou");
        if (bansoukouRoot.mkdir()) {
            LOGGER.info("No bansoukou found. Perhaps it is the first load. Continuing with mod loading.");
            return;
        }
        File bansoukouFolder = new File(bansoukouRoot, "patches");
        if (bansoukouFolder.mkdir()) {
            LOGGER.info("No patches found. Continuing with mod loading.");
            return;
        }
        File[] patchRoot = bansoukouFolder.listFiles();
        if (patchRoot == null) {
            LOGGER.info("No patches found. Continuing with mod loading.");
            return;
        }
        File mods = new File(Launch.minecraftHome, "mods");
        final Map<File, File> jars = new Object2ObjectOpenHashMap<>(patchRoot.length);
        for (File file : patchRoot) {
            jars.put(file, patch(mods, file));
        }
        for (Map.Entry<File, File> entry : jars.entrySet()) {
            File root = entry.getKey();
            File zip = entry.getValue();
            try (FileSystem fs = FileSystems.newFileSystem(zip.toPath(), null)) {
                try (Stream<Path> walk = Files.walk(root.toPath())) {
                    walk.map(Path::toFile)
                            .filter(f -> !f.isDirectory() || f.toString().endsWith(".DELETION"))
                            .forEach(f -> {
                                Path currentPath;
                                if (f.isDirectory()) {
                                    String fileUri = f.toURI().toString();
                                    fileUri = fileUri.substring(0, fileUri.lastIndexOf('.'));
                                    currentPath = fs.getPath(root.toURI().relativize(URI.create(fileUri)).toString());
                                } else {
                                    currentPath = fs.getPath(root.toURI().relativize(f.toURI()).toString());
                                }
                                try {
                                    work(f, currentPath);
                                } catch (IOException e) {
                                    if (e instanceof NoSuchFileException) {
                                        try {
                                            Files.createDirectories(currentPath);
                                            work(f, currentPath);
                                        } catch (IOException e2) {
                                            e2.printStackTrace();
                                        }
                                    } else {
                                        e.printStackTrace();
                                    }
                                }
                            });
                    Path meta$inf = fs.getPath("/META-INF");
                    if (Files.exists(meta$inf)) {
                        Files.walk(meta$inf, 1).filter(p -> p.toString().endsWith(".SF")).forEach(p -> {
                            try {
                                LOGGER.info("Wiping signature file from {}, as we have tampered with the file.", zip);
                                Files.delete(p);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        });
                    }
                }
            }
        }
        // LOGGER.info("Hooking diversions...");
        // ModContainerFactory.instance().registerContainerType(Type.getType(Mod.class), BansoukouModContainer.class);
    }

    private File patch(File mods, File patchFile) throws IOException {
        File modFile = new File(mods, patchFile.getName().concat(".jar"));
        if (Files.exists(modFile.toPath()) || Files.exists((modFile = new File(mods, patchFile.getName().concat("-patched.jar"))).toPath())) {
            try (RandomAccessFile raf = new RandomAccessFile(modFile, "r")) {
                int start = raf.readInt();
                if (start == 0x504B0304 || start == 0x504B0506 || start == 0x504B0708) {
                    LOGGER.info("{} was found in the mods folder. Copying to modify the copied cache.", modFile.getName());
                } else {
                    throw LOGGER.throwing(new ZipException(modFile.getName() + " -> exists in the mods folder but isn't a valid jar file! Report to the pack author!"));
                }
            }
            Path path = modFile.toPath();
            File mockFile = new File(mods, patchFile.getName().concat(".jar") + ".disabled"); // Curseforge workaround
            Files.copy(path, mockFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            File newFile = new File(mods, patchFile.getName() + "-patched.jar");
            Files.move(path, newFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return newFile;
        } else {
            throw LOGGER.throwing(new FileNotFoundException(modFile.getName() + " -> does not exist in mods folder! Perhaps the mod's name has changed? Report to the pack author!"));
        }
        /*
        String fileName = file.getName().concat(".jar");
        File pathFile = new File(mods, fileName);
        Path path = pathFile.toPath();
        if (Files.exists(path)) {
            try (RandomAccessFile raf = new RandomAccessFile(pathFile, "r")) {
                int start = raf.readInt();
                if (start == 0x504B0304 || start == 0x504B0506 || start == 0x504B0708) {
                    LOGGER.info("{} was found in the mods folder. Copying to modify the copied cache.", fileName);
                } else {
                    throw LOGGER.throwing(new ZipException(fileName + " -> exists in the mods folder but isn't a valid jar file! Report to the pack author!"));
                }
            }
            File mockFile = new File(mods, fileName + ".disabled"); // Curseforge workaround
            Files.copy(path, mockFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            File newFile = new File(mods, file.getName() + "-patched.jar");
            Files.move(path, newFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            // jars.put(file, newFile);
            return newFile;
        } else {
            throw LOGGER.throwing(new FileNotFoundException(fileName + " -> does not exist in mods folder! Perhaps the mod's name has changed? Report to the pack author!"));
        }
         */
    }

    private void work(File f, Path currentPath) throws IOException {
        if (f.isDirectory()) {
            LOGGER.warn("Removing {} folder and anything under it...", currentPath);
            Files.walkFileTree(currentPath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }
                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } else if (f.length() <= 0) {
            LOGGER.warn("Removing {}...", currentPath);
            Files.deleteIfExists(currentPath);
        } else {
            LOGGER.warn("Patching {}...", currentPath);
            Files.copy(f.toPath(), currentPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @Override
    public void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile) { }

    @Override
    public void injectIntoClassLoader(LaunchClassLoader classLoader) { }

    @Override
    public String getLaunchTarget() {
        return null;
    }

    @Override
    public String[] getLaunchArguments() {
        return new String[0];
    }

}
