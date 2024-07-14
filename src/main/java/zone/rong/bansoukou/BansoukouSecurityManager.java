package zone.rong.bansoukou;

import net.minecraftforge.fml.relauncher.CoreModManager;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.Permission;

public class BansoukouSecurityManager extends SecurityManager {

    private static final IOException CHEAT_EXCEPTION = new CheatIOException("Bansoukou: Premature Exit Please Ignore");
    private static final SecurityManager DEFAULT_SECURITY_MANAGER = System.getSecurityManager();

    private static Field system$security;

    static void replace() {
        try {
            if (system$security == null) {
                Method getDeclaredFields0 = Class.class.getDeclaredMethod("getDeclaredFields0", boolean.class);
                getDeclaredFields0.setAccessible(true);
                Field[] fields = (Field[]) getDeclaredFields0.invoke(System.class, false);
                for (Field field : fields) {
                    if ("security".equals(field.getName())) {
                        system$security = field;
                        break;
                    }
                }
            }

            system$security.setAccessible(true);
            system$security.set(null, new BansoukouSecurityManager());
        } catch (Throwable t) {
            throw new RuntimeException("Cannot replace SecurityManager", t);
        }
    }

    @Override
    public void checkPermission(Permission perm) { }

    @Override
    public void checkRead(String file) {
        Class[] context = this.getClassContext();
        if (context[2] == CoreModManager.class && context[3] == CoreModManager.class && new Exception().getStackTrace()[2].getMethodName().equals("discoverCoreMods")) {
            Bansoukou.UNSAFE.throwException(CHEAT_EXCEPTION);
            System.setSecurityManager(DEFAULT_SECURITY_MANAGER);
        }
    }

    private static class CheatIOException extends IOException {

        private CheatIOException(String message) {
            super(message);
        }

        @Override
        public synchronized Throwable fillInStackTrace() {
            return this;
        }

    }

}
