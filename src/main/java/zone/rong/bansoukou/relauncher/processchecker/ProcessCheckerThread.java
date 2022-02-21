package zone.rong.bansoukou.relauncher.processchecker;

import org.apache.commons.lang3.SystemUtils;

import java.io.IOException;

public class ProcessCheckerThread extends Thread {

    private final String parentPid;
    private final ProcessChecker maintainer;

    private boolean running;

    public ProcessCheckerThread(String parentPid) {
        super();
        setName("Bansoukou/ProcessMaintainerThread");
        this.parentPid = parentPid;
        if (SystemUtils.IS_JAVA_1_8) {
            this.maintainer = SystemUtils.IS_OS_WINDOWS ? new J8WindowsProcessChecker() : new J8POSIXProcessChecker();
        } else {
            this.maintainer = new J9ProcessChecker();
        }
        this.running = true;
    }

    @Override
    public void run() {
        while (this.running) {
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            try {
                if (!maintainer.checkExistence(parentPid)) {
                    System.out.println("Minecraft is killed unexpectedly! Scheduling actual shutdown.");
                    this.running = false;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println("Exiting...");
        // QualifiedExit.exit(0);
    }

}