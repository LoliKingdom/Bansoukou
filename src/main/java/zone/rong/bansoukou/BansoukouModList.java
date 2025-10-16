package zone.rong.bansoukou;

import net.minecraftforge.fml.relauncher.libraries.ModList;
import net.minecraftforge.fml.relauncher.libraries.Repository;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.Map;

class BansoukouModList extends ModList {

    static void replace(Map<Path, Path> patches) {
        try {
            Repository memory = Repository.get("MEMORY");
            BansoukouRepository replace = new BansoukouRepository(memory, memory.getFile("dummy").getParentFile(), patches);

            Field repository$cache = Repository.class.getDeclaredField("cache");
            repository$cache.setAccessible(true);

            Map<String, Repository> repo = (Map<String, Repository>) repository$cache.get(null);
            repo.put("MEMORY", replace);

            Field modlist$cache = ModList.class.getDeclaredField("cache");
            modlist$cache.setAccessible(true);

            Map<String, ModList> modList = (Map<String, ModList>) modlist$cache.get(null);
            modList.put("MEMORY", new BansoukouModList(replace));
        } catch (Throwable t) {
            throw new RuntimeException("Cannot replace inner MEMORY repository", t);
        }
    }

    BansoukouModList(Repository repo) {
        super(repo);
    }

    @Override
    public void save() { }

    @Override
    public String getName() {
        return "MEMORY";
    }

}
