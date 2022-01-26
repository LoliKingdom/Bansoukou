package zone.rong.bansoukou;

import com.google.common.eventbus.EventBus;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.common.ForgeVersion;
import net.minecraftforge.fml.common.DummyModContainer;
import net.minecraftforge.fml.common.LoadController;
import net.minecraftforge.fml.common.ModMetadata;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import net.minecraftforge.fml.relauncher.libraries.LibraryManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.io.*;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.ZipException;

@IFMLLoadingPlugin.Name("-Bansoukou-")
@IFMLLoadingPlugin.MCVersion(ForgeVersion.mcVersion)
@IFMLLoadingPlugin.SortingIndex(Integer.MIN_VALUE)
public class BansoukouCoreMod implements IFMLLoadingPlugin {

	public static final Logger LOGGER = LogManager.getLogger("Bansoukou");

	Map<URL, Path> queuedDeletion = new Object2ObjectOpenHashMap<>();
	Map<URL, URL> queuedAddition = new Object2ObjectOpenHashMap<>();

	public BansoukouCoreMod() throws IOException {
		LOGGER.info("Ikimasu!");
		File bansoukouRoot = new File(Launch.minecraftHome, "bansoukou");
		if (bansoukouRoot.mkdir()) {
			LOGGER.info("No bansoukou found. Perhaps it is the first load. Continuing with mod loading.");
			return;
		}
		File[] patchRoot = bansoukouRoot.listFiles();
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
			File newFile = new File(mods, patchFile.getName() + "-patched.jar");
			Files.copy(path, newFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
			File mockFile = new File(mods, patchFile.getName().concat(".jar") + ".disabled"); // CurseForge workaround
			if (!mockFile.exists()) {
				Files.copy(path, mockFile.toPath()); // Keep original jar for the remainder of the coremod loading process...
				URL oldUrl = modFile.toURI().toURL();
				queuedDeletion.put(oldUrl, path);
				queuedAddition.put(oldUrl, newFile.toURI().toURL());
			}
			return newFile;
		} else {
			throw LOGGER.throwing(new FileNotFoundException(patchFile.getName().concat(".jar") + " -> does not exist in mods folder! Perhaps the mod's name has changed? Report to the pack author!"));
		}
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
		if (!queuedDeletion.isEmpty()) {
			try {
				FilenameFilter newFilter = (dir, name) -> JarReplacer.instance.ignoredMods.contains(name) || name.endsWith(".jar") || name.endsWith(".zip");
				Field modFilenameFilterField = LibraryManager.class.getDeclaredField("MOD_FILENAME_FILTER");
				modFilenameFilterField.setAccessible(true);
				modFilenameFilterField.set(null, newFilter);
			} catch (NoSuchFieldException | IllegalAccessException e) {
				e.printStackTrace();
			}
			queuedDeletion.forEach((oldUrl, path) -> JarReplacer.instance.replaceJar(oldUrl, path, queuedAddition.get(oldUrl)));
		}
		queuedDeletion = null;
		queuedAddition = null;
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
			meta.version = "4.2.3";
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
