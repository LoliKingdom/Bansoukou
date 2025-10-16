package zone.rong.bansoukou.cleanroom;

import zone.rong.bansoukou.Bansoukou;

import java.nio.file.Path;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

public class CleanBansoukou {

    public static boolean ran = false;

    private static Map<Path, Path> bansoukou;

    // Reflectively called from Cleanroom's LibraryManager
    public static void bansoukou(List<Path> list) {
        if (!ran) {
            ran = true;
            bansoukou = Bansoukou.init();
        }
        if (!bansoukou.isEmpty()) {
            ListIterator<Path> iterator = list.listIterator();
            while (iterator.hasNext()) {
                Path replacement = bansoukou.get(iterator.next().toAbsolutePath());
                if (replacement != null) {
                    iterator.set(replacement);
                }
            }
        }
    }

}
