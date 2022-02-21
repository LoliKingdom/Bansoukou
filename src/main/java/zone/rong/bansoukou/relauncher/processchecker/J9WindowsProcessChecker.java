package zone.rong.bansoukou.relauncher.processchecker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class J9WindowsProcessChecker implements ProcessChecker {

    private ProcessBuilder builder;

    @Override
    public boolean checkExistence(String pid) throws IOException {
        if (builder == null) {
            builder = new ProcessBuilder();
            builder.command(System.getenv("windir") + "\\system32\\tasklist.exe", "/NH", "/FI", "\"PID eq " + pid + "\"");
        }
        Process process = builder.start();
        BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        boolean terminate = false;
        while ((line = input.readLine()) != null) {
            terminate = !line.startsWith("java");
        }
        input.close();
        return !terminate;
    }

}
