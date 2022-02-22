package zone.rong.bansoukou.relauncher;

import net.minecraftforge.fml.relauncher.FMLSecurityManager;

import java.security.Permission;

public class BansoukouRelaxedSecurityManager extends SecurityManager {

    @Override
    public void checkPermission(Permission perm) {
        if (perm.getName() == null) {
            return;
        }
        if (perm.getName().startsWith("exitVM")) {
            Class<?>[] classContexts = getClassContext();
            String callingClass = classContexts.length > 4 ? classContexts[4].getName() : "none";
            String callingParent = classContexts.length > 5 ? classContexts[5].getName() : "none";
            // FML is allowed to call system exit and the Minecraft applet (from the quit button)
            if (!(callingClass.startsWith("net.minecraftforge.fml.")
                    || "net.minecraft.server.dedicated.ServerHangWatchdog$1".equals(callingClass)
                    || "net.minecraft.server.dedicated.ServerHangWatchdog".equals(callingClass)
                    || ("net.minecraft.client.Minecraft".equals(callingClass) && "net.minecraft.client.Minecraft".equals(callingParent))
                    || ("net.minecraft.server.dedicated.DedicatedServer".equals(callingClass) && "net.minecraft.server.MinecraftServer".equals(callingParent)))
            ) {
                throw new FMLSecurityManager.ExitTrappedException();
            }
        } /* else if ("setSecurityManager".equals(permName)) {
            throw new SecurityException("Cannot replace the FML security manager");
        } */
    }

    @Override
    public void checkPermission(Permission perm, Object context) {
        this.checkPermission(perm);
    }

}
