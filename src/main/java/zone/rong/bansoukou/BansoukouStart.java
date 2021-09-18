package zone.rong.bansoukou;

import net.minecraft.launchwrapper.Launch;
import zone.rong.bansoukou.processcheck.ProcessMaintainerThread;

public class BansoukouStart {

    public static void main(String[] args) {
        System.setSecurityManager(new BansoukouRelaxedSecurityManager());
        try {
            new ProcessMaintainerThread(args[args.length - 1]).start();
            Launch.main(args);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
