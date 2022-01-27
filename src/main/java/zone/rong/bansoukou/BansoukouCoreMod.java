package zone.rong.bansoukou;

import com.google.common.eventbus.EventBus;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraftforge.common.ForgeVersion;
import net.minecraftforge.fml.common.DummyModContainer;
import net.minecraftforge.fml.common.LoadController;
import net.minecraftforge.fml.common.ModMetadata;
import net.minecraftforge.fml.relauncher.CoreModManager;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import net.minecraftforge.fml.relauncher.libraries.LibraryManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sun.misc.URLClassPath;

import javax.annotation.Nullable;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Stream;
import java.util.zip.ZipException;

@IFMLLoadingPlugin.Name("-Bansoukou-")
@IFMLLoadingPlugin.MCVersion(ForgeVersion.mcVersion)
@IFMLLoadingPlugin.SortingIndex(Integer.MIN_VALUE)
public class BansoukouCoreMod implements IFMLLoadingPlugin {

    public static final Logger LOGGER = LogManager.getLogger("Bansoukou");

    static Map<String, Path> scheduledDeletion = new Object2ObjectOpenHashMap<>();
    static Map<String, String> patchedStrings = new Object2ObjectOpenHashMap<>();
    static Map<URL, URL> patchedUrls = new Object2ObjectOpenHashMap<>();

    public BansoukouCoreMod() throws IOException {
        LOGGER.info("Ikimasu!");
        File bansoukouRoot;
        if (Launch.minecraftHome == null) {
            bansoukouRoot = new File(".", "bansoukou");
        } else {
            bansoukouRoot = new File(Launch.minecraftHome, "bansoukou").getCanonicalFile();
        }
        LOGGER.info(bansoukouRoot.toURI());
        if (bansoukouRoot.mkdir()) {
            LOGGER.info("No bansoukou found. Perhaps it is the first load. Continuing with mod loading.");
            return;
        }
        File[] patchRoot = bansoukouRoot.listFiles();
        if (patchRoot == null) {
            LOGGER.info("No patches found. Continuing with mod loading.");
            return;
        }
        File mods;
        if (Launch.minecraftHome == null) {
            mods = new File(".", "mods");
        } else {
            mods = new File(Launch.minecraftHome, "mods").getCanonicalFile();
        }
        final Map<File, File> jars = new Object2ObjectOpenHashMap<>(patchRoot.length);
        for (File file : patchRoot) {
            try {
                File patch = getPatchFile(mods, file);
                if (patch != null) {
                    jars.put(file, patch);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (!jars.isEmpty()) {
            // Divert calls in different data structures
            try {
                // Setup Fields
                // LaunchClassLoader
                Field launchClassLoader$sources = LaunchClassLoader.class.getDeclaredField("sources");
                launchClassLoader$sources.setAccessible(true);
                // URLClassLoader
                Field ucp = URLClassLoader.class.getDeclaredField("ucp");
                ucp.setAccessible(true);
                // URLClassPath
                Field urlClassPath$path = URLClassPath.class.getDeclaredField("path");
                urlClassPath$path.setAccessible(true);
                Field urlClassPath$urls = URLClassPath.class.getDeclaredField("urls");
                urlClassPath$urls.setAccessible(true);
                // LibraryManager
                Field modFilenameFilterField = LibraryManager.class.getDeclaredField("MOD_FILENAME_FILTER");
                modFilenameFilterField.setAccessible(true);
                Field modifiersField = Field.class.getDeclaredField("modifiers");
                modifiersField.setAccessible(true);
                modifiersField.setInt(modFilenameFilterField, modFilenameFilterField.getModifiers() & ~Modifier.FINAL);
                // CoreModManager
                Field coreModManager$ignoredModFiles = CoreModManager.class.getDeclaredField("ignoredModFiles");
                coreModManager$ignoredModFiles.setAccessible(true);
                Field coreModManager$candidateModFiles = CoreModManager.class.getDeclaredField("candidateModFiles");
                coreModManager$candidateModFiles.setAccessible(true);
                // ModAccessTransformer
                // Field modAccessTransformer$embedded = ModAccessTransformer.class.getDeclaredField("embedded");
                // modAccessTransformer$embedded.setAccessible(true);

                // Set replacement data structures
                // LaunchClassLoader
                launchClassLoader$sources.set(Launch.classLoader, getRedirectedAddUrlsList(Launch.classLoader.getSources()));
                // URLClassPath
                URLClassPath urlClassPathInstance = (URLClassPath) ucp.get(Launch.classLoader);
                urlClassPath$path.set(urlClassPathInstance, getRedirectedAddUrlsList((List<URL>) urlClassPath$path.get(urlClassPathInstance)));
                urlClassPath$urls.set(urlClassPathInstance, getRedirectedAddUrlsStack((Stack<URL>) urlClassPath$urls.get(urlClassPathInstance)));
                // LibraryManager
                FilenameFilter newFilter = (dir, name) -> !patchedStrings.containsKey(name) && (name.endsWith(".jar") || name.endsWith(".zip"));
                modFilenameFilterField.set(null, newFilter);
                // CoreModManager
                coreModManager$ignoredModFiles.set(null, getRedirectedAddStringsList(CoreModManager.getIgnoredMods()));
                coreModManager$candidateModFiles.set(null, getRedirectedAddStringsList(CoreModManager.getReparseableCoremods()));
                // ModAccessTransformer
                /* TODO: we'll get back to this...
                modAccessTransformer$embedded.set(null, new HashMap<String, String>((Map<String, String>) modAccessTransformer$embedded.get(null)) {
                    @Override
                    public String put(String key, String value) {
                        System.out.println(key);
                        return super.put(key, value);
                    }
                });
                 */
            } catch (Throwable t) {
                t.printStackTrace();
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
                                        patch(f, currentPath);
                                    } catch (IOException e) {
                                        if (e instanceof NoSuchFileException) {
                                            try {
                                                Files.createDirectories(currentPath);
                                                patch(f, currentPath);
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
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private File getPatchFile(File mods, File patchFile) throws IOException {
        File modFile = new File(mods, patchFile.getName().concat(".jar"));
        if (Launch.minecraftHome != null) {
            modFile = modFile.getCanonicalFile();
        }
        if (Files.exists(modFile.toPath())) {
            try (RandomAccessFile raf = new RandomAccessFile(modFile, "r")) {
                int start = raf.readInt();
                if (start == 0x504B0304 || start == 0x504B0506 || start == 0x504B0708) {
                    LOGGER.warn("{} was found in the mods folder. Copying to modify the copied cache.", modFile.getName());
                } else {
                    throw LOGGER.throwing(new ZipException(modFile.getName() + " exists in the mods folder but isn't a valid jar/zip file!"));
                }
            }
            Path modFilePath = modFile.toPath();
            scheduledDeletion.put(patchFile.getName(), modFilePath);
            File newFile = new File(mods, patchFile.getName() + "-patched.jar").getCanonicalFile();
            Path newFilePath = newFile.toPath();
            Files.copy(modFilePath, newFilePath, StandardCopyOption.REPLACE_EXISTING);
            patchedStrings.put(modFilePath.getFileName().toString(), newFilePath.getFileName().toString());
            patchedUrls.put(modFile.toURI().toURL(), newFile.toURI().toURL());
            return newFile;
        } else {
            if (!new File(mods, patchFile.getName().concat(".disabled")).exists()) {
                throw LOGGER.throwing(new FileNotFoundException(modFile.getName() + " does not exist in mods folder!"));
            }
        }
        return null;
    }

    private void patch(File f, Path currentPath) throws IOException {
        if (f.isDirectory()) {
            LOGGER.warn("Removing {} folder and everything under it...", currentPath);
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

    private ArrayList<String> getRedirectedAddStringsList(List<String> existingList) {
        return new ArrayList<String>(existingList) {
            @Override
            public boolean add(String o) {
                String patched = patchedStrings.get(o);
                return patched == null ? super.add(o) : super.add(patched);
            }
        };
    }

    private ArrayList<URL> getRedirectedAddUrlsList(List<URL> existingList) {
        return new ArrayList<URL>(existingList) {
            @Override
            public boolean add(URL o) {
                URL patched = patchedUrls.get(o);
                return patched == null ? super.add(o) : super.add(patched);
            }
        };
    }

    private Stack<URL> getRedirectedAddUrlsStack(Stack<URL> existingStack) {
        Stack<URL> stack = new Stack<URL>() {
            @Override
            public void add(int index, URL o) {
                URL patched = patchedUrls.get(o);
                if (patched == null) {
                    super.add(index, o);
                } else {
                    super.add(index, patched);
                }
            }
        };
        stack.addAll(existingStack);
        return stack;
    }

    @Override
    public String[] getASMTransformerClass() {
        return new String[0];
    }

    @Override
    public String getModContainerClass() {
        return "zone.rong.bansoukou.BansoukouCoreMod$Container";
    }

    @Nullable
    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {
        try {
            File mods;
            if (Launch.minecraftHome == null) {
                mods = new File(".", "mods");
            } else {
                mods = new File(Launch.minecraftHome, "mods").getCanonicalFile();
            }
            scheduledDeletion.forEach((s, p) -> {
                try {
                    File disabledFile = new File(mods, s + ".disabled");
                    Files.move(p, disabledFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                        try {
                            File disabledFile = new File(mods, s + ".disabled");
                            Files.move(p, disabledFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                    }));
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }

    public static class Container extends DummyModContainer {

        public Container() {
            super(new ModMetadata());
            ModMetadata meta = this.getMetadata();
            meta.modId = "bansoukou";
            meta.name = "Bansoukou";
            meta.description = "A simple coremod that streamlines patching of mods.";
            meta.version = "4.3";
            meta.logoFile = "/icon.png";
            meta.authorList.add("Rongmario");
        }

        @Override
        public boolean registerBus(EventBus bus, LoadController controller) {
            bus.register(this);
            return true;
        }

    }

}
