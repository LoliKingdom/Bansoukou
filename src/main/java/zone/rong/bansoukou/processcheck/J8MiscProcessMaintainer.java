package zone.rong.bansoukou.processcheck;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class J8MiscProcessMaintainer implements ProcessMaintainer {

    @Override
    public boolean checkExistence(String pid) throws IOException {
        Process p = Runtime.getRuntime().exec("pgrep java");
        BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line;
        boolean terminate = false;
        while ((line = input.readLine()) != null) {
            if (line.trim().equals(pid)) {
                terminate = true;
            }
        }
        input.close();
        return !terminate;
    }

}
