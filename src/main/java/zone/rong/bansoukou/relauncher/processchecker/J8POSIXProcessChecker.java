package zone.rong.bansoukou.relauncher.processchecker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class J8POSIXProcessChecker implements ProcessChecker {

    @Override
    public boolean checkExistence(String pid) throws IOException {
        Process process = Runtime.getRuntime().exec("ps -p " + pid);
        BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while ((line = input.readLine()) != null) {
            if (line.trim().equals(pid)) {
                input.close();
                return true;
            }
        }
        input.close();
        return false;
    }

}
