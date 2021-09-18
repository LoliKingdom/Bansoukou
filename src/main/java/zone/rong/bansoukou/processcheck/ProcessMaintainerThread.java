package zone.rong.bansoukou.processcheck;

import net.minecraftforge.fml.exit.ExitOnSpawningNewProcess;
import org.apache.commons.lang3.SystemUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public class ProcessMaintainerThread extends Thread {

    private final String parentPid;
    private final ProcessMaintainer maintainer;
    private final Logger logger;

    private boolean running;

    public ProcessMaintainerThread(String parentPid) {
        super();
        setName("Bansoukou/ProcessMaintainerThread");
        this.parentPid = parentPid;
        if (SystemUtils.IS_JAVA_1_8) {
            this.maintainer = SystemUtils.IS_OS_WINDOWS ? new J8WindowsProcessMaintainer() : new J8MiscProcessMaintainer();
        } else {
            this.maintainer = new J9ProcessMaintainer();
        }
        this.running = true;
        this.logger = LogManager.getLogger("ProcessMaintainerThread");
    }

    @Override
    public void run() {
        while (running) {
            try {
                // Try polling every 2 seconds
                if (System.currentTimeMillis() % 2000 == 0 && !maintainer.checkExistence(parentPid)) {
                    this.logger.info("Minecraft is killed unexpectedly! Scheduling actual shutdown.");
                    this.running = false;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        this.logger.info("Exiting...");
        ExitOnSpawningNewProcess.exit(0);
    }

}
