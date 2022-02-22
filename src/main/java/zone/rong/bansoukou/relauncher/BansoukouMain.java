package zone.rong.bansoukou.relauncher;

import zone.rong.bansoukou.relauncher.processchecker.ProcessCheckerThread;

public class BansoukouMain {

    public static void main(String[] args) {
        System.setSecurityManager(new BansoukouRelaxedSecurityManager());
        try {
            new ProcessCheckerThread(args[args.length - 1]).start();
            // Launch.main(args);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}