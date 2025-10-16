package zone.rong.bansoukou;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import zone.rong.bansoukou.cleanroom.CleanBansoukou;

import javax.annotation.Nullable;
import java.io.File;
import java.util.Map;

@IFMLLoadingPlugin.Name(Tags.MOD_NAME)
public class BansoukouCoreMod implements IFMLLoadingPlugin {

    static File source;

    public BansoukouCoreMod() {
        if (CleanBansoukou.ran) {
            return;
        }

        if (!Bansoukou.init().isEmpty()) {
            BansoukouModList.replace();
            BansoukouSecurityManager.replace();
            BansoukouFMLTweaker.replace();
            BansoukouSecurityManager.replace();
        }
    }

    @Override
    public String[] getASMTransformerClass() {
        return null;
    }

    @Override
    public String getModContainerClass() {
        return "zone.rong.bansoukou.BansoukouMod";
    }

    @Nullable
    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {
        source = (File) data.get("coremodLocation") ;
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }

}
