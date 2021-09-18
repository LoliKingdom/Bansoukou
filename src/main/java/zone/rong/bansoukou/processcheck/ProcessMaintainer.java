package zone.rong.bansoukou.processcheck;

import java.io.IOException;

public interface ProcessMaintainer {

    boolean checkExistence(String pid) throws IOException;

}
