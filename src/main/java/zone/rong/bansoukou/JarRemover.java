package zone.rong.bansoukou;

import net.minecraft.launchwrapper.Launch;
import sun.misc.URLClassPath;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.jar.JarFile;

public class JarRemover {

	static JarRemover instance = new JarRemover();

	/* The resulting search path of Loaders */
	// ArrayList<URLClassPath.Loader> loaders = new ArrayList<URLClassPath.Loader>();

	/* Map of each URL opened to its corresponding Loader */
	// HashMap<String, URLClassPath.Loader> lmap = new HashMap<String, URLClassPath.Loader>();

	private final URLClassPath urlClassPath;

	private final Field urlClassPath$path, urlClassPath$urls, urlClassPath$loaders, urlClassPath$lmap;

	private Field jarLoader$jar, jarLoader$csu;

	private JarRemover() {
		URLClassPath init$urlClassPath = null;
		Field init$urlClassPath$path = null, init$urlClassPath$urls = null, init$urlClassPath$loaders = null, init$urlClassPath$lmap = null;
		try {
			Field ucp = URLClassLoader.class.getDeclaredField("ucp");
			ucp.setAccessible(true);
			init$urlClassPath = (URLClassPath) ucp.get(Launch.classLoader);
			init$urlClassPath$path = URLClassPath.class.getDeclaredField("path");
			init$urlClassPath$path.setAccessible(true);
			init$urlClassPath$urls = URLClassPath.class.getDeclaredField("urls");
			init$urlClassPath$urls.setAccessible(true);
			init$urlClassPath$loaders = URLClassPath.class.getDeclaredField("loaders");
			init$urlClassPath$loaders.setAccessible(true);
			init$urlClassPath$lmap = URLClassPath.class.getDeclaredField("lmap");
			init$urlClassPath$lmap.setAccessible(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
		urlClassPath = init$urlClassPath;
		urlClassPath$path = init$urlClassPath$path;
		urlClassPath$urls = init$urlClassPath$urls;
		urlClassPath$loaders = init$urlClassPath$loaders;
		urlClassPath$lmap = init$urlClassPath$lmap;
	}

	public boolean removeJar(URL url) {
		try {
			Iterator loadersIter = ((ArrayList) urlClassPath$loaders.get(urlClassPath)).iterator();
			while (loadersIter.hasNext()) {
				Object loader = loadersIter.next();
				if (jarLoader$csu == null) {
					jarLoader$csu = loader.getClass().getDeclaredField("csu");
					jarLoader$csu.setAccessible(true);
				}
				if (url.equals(jarLoader$csu.get(loader))) {
					if (jarLoader$jar == null) {
						jarLoader$jar = loader.getClass().getDeclaredField("jar");
						jarLoader$jar.setAccessible(true);
					}
					((JarFile) jarLoader$jar.get(loader)).close();
					loadersIter.remove();
					((HashMap) urlClassPath$lmap.get(urlClassPath)).values().removeIf(v -> v == loader);
					return true;
				}
			}
		} catch (ReflectiveOperationException | IOException e) {
			e.printStackTrace();
		}
		return false;
	}

}
