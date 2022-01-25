package zone.rong.bansoukou;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.fml.relauncher.CoreModManager;
import sun.misc.URLClassPath;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandler;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.AccessControlContext;
import java.util.*;
import java.util.jar.JarFile;
import java.util.stream.Stream;

public class JarReplacer {

	static JarReplacer instance = new JarReplacer();

	/* The resulting search path of Loaders */
	// ArrayList<URLClassPath.Loader> loaders = new ArrayList<URLClassPath.Loader>();

	/* Map of each URL opened to its corresponding Loader */
	// HashMap<String, URLClassPath.Loader> lmap = new HashMap<String, URLClassPath.Loader>();

	final Set<String> ignoredMods = new ObjectOpenHashSet<>();

	private final URLClassPath ucp;

	private final Field ucp$path, ucp$urls, ucp$loaders, ucp$lmap, ucp$jarHandler, ucp$acc;

	private Field jarLoader$jar, jarLoader$csu;
	private Constructor jarLoader$ctor;

	private JarReplacer() {
		URLClassPath init$ucp = null;
		Field init$ucp$path = null, init$ucp$urls = null, init$ucp$loaders = null;
		Field init$ucp$lmap = null, init$ucp$jarHandler = null, init$ucp$acc = null;
		try {
			Field ucp = URLClassLoader.class.getDeclaredField("ucp");
			ucp.setAccessible(true);
			init$ucp = (URLClassPath) ucp.get(Launch.classLoader);
			init$ucp$path = URLClassPath.class.getDeclaredField("path");
			init$ucp$path.setAccessible(true);
			init$ucp$urls = URLClassPath.class.getDeclaredField("urls");
			init$ucp$urls.setAccessible(true);
			init$ucp$loaders = URLClassPath.class.getDeclaredField("loaders");
			init$ucp$loaders.setAccessible(true);
			init$ucp$lmap = URLClassPath.class.getDeclaredField("lmap");
			init$ucp$lmap.setAccessible(true);
			init$ucp$jarHandler = URLClassPath.class.getDeclaredField("jarHandler");
			init$ucp$jarHandler.setAccessible(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			// This field doesn't exist in 8u51, its in URLClassLoader itself but we won't be needing it
			init$ucp$acc = URLClassPath.class.getDeclaredField("acc");
			init$ucp$acc.setAccessible(true);
		} catch (Exception ignored) { }
		ucp = init$ucp;
		ucp$path = init$ucp$path;
		ucp$urls = init$ucp$urls;
		ucp$loaders = init$ucp$loaders;
		ucp$lmap = init$ucp$lmap;
		ucp$jarHandler = init$ucp$jarHandler;
		ucp$acc = init$ucp$acc;
	}

	public void replaceJar(URL oldUrl, Path originalPath, URL newUrl) {
		try {
			ListIterator loadersIter = ((ArrayList) ucp$loaders.get(ucp)).listIterator();
			while (loadersIter.hasNext()) {
				Object loader = loadersIter.next();
				if (jarLoader$csu == null) {
					jarLoader$csu = loader.getClass().getDeclaredField("csu");
					jarLoader$csu.setAccessible(true);
				}
				if (oldUrl.equals(jarLoader$csu.get(loader))) {
					if (jarLoader$jar == null) {
						jarLoader$jar = loader.getClass().getDeclaredField("jar");
						jarLoader$jar.setAccessible(true);
					}
					Stack<URL> urls = (Stack<URL>) ucp$urls.get(ucp);
					int index = urls.indexOf(oldUrl);
					if (index != -1) {
						urls.set(index, newUrl);
					}
					ArrayList<URL> path = (ArrayList<URL>) ucp$path.get(ucp);
					index = path.indexOf(oldUrl);
					if (index != -1) {
						path.set(index, newUrl);
					}
					index = Launch.classLoader.getSources().indexOf(oldUrl);
					if (index != -1) {
						Launch.classLoader.getSources().set(index, newUrl);
					} else {
						throw new IllegalStateException();
					}
					String oldFileName = Paths.get(oldUrl.toURI()).getFileName().toString();
					String newFileName = Paths.get(newUrl.toURI()).getFileName().toString();
					ignoredMods.add(newFileName);
					index = CoreModManager.getIgnoredMods().indexOf(oldFileName);
					if (index != -1) {
						CoreModManager.getIgnoredMods().set(index, newFileName);
					}
					index = CoreModManager.getReparseableCoremods().indexOf(oldFileName);
					if (index != -1) {
						CoreModManager.getReparseableCoremods().set(index, newFileName);
					}
					((JarFile) jarLoader$jar.get(loader)).close(); // Close old loader
					HashMap lmap = (HashMap) ucp$lmap.get(ucp);
					Object newJarLoader = null;
					if (jarLoader$ctor == null) {
						try {
							jarLoader$ctor = loader.getClass().getDeclaredConstructor(URL.class, URLStreamHandler.class, HashMap.class, AccessControlContext.class);
							jarLoader$ctor.setAccessible(true);
							newJarLoader = jarLoader$ctor.newInstance(newUrl, ucp$jarHandler.get(ucp), lmap, ucp$acc.get(ucp)); // Create new loader
						} catch (Exception e) {
							jarLoader$ctor = loader.getClass().getDeclaredConstructor(URL.class, URLStreamHandler.class, HashMap.class); // Older versions doesn't need ACC
							jarLoader$ctor.setAccessible(true);
							newJarLoader = jarLoader$ctor.newInstance(newUrl, ucp$jarHandler.get(ucp), lmap); // Create new loader
						}
					}
					if (newJarLoader == null) {
						throw new IllegalStateException("Cannot instantiate replacement JarLoader.");
					}
					String key = (String) ((Stream<Map.Entry>) lmap.entrySet().stream()).filter(e -> e.getValue() == loader).findFirst().map(Map.Entry::getKey).get();
					lmap.replace(key, newUrl);
					loadersIter.set(newJarLoader);
					Files.delete(originalPath);
				}
			}
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

}
