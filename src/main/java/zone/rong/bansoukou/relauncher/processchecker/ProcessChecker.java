package zone.rong.bansoukou.relauncher.processchecker;

import java.io.IOException;

public interface ProcessChecker {

    boolean checkExistence(String pid) throws IOException;

}
