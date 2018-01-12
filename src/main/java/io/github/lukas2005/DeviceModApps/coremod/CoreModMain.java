package io.github.lukas2005.DeviceModApps.coremod;

import io.github.lukas2005.DeviceModApps.OSValidator;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.relauncher.FMLInjectionData;
import net.minecraftforge.fml.relauncher.IFMLCallHook;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Map;

@IFMLLoadingPlugin.MCVersion(Loader.MC_VERSION)
public class CoreModMain implements IFMLLoadingPlugin, IFMLCallHook {

    public static File minecraftDir;

    public static String urlBase = "https://github.com/lukas2005/Device-Mod-Apps/blob/master/lib/jxbrowser/lib/";

    public CoreModMain() {
        if (minecraftDir != null)
            return;

        minecraftDir = (File) FMLInjectionData.data()[6];
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Nullable
    @Override
    public String getSetupClass() {
        return CoreModMain.class.getName();
    }

    @Override
    public void injectData(Map<String, Object> data) {
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }

    @Override
    public String[] getASMTransformerClass() {
        return null;
    }

    @Override
    public Void call() {
        if ((Boolean) Launch.blackboard.get("fml.deobfuscatedEnvironment")) return null;
        try {
            LaunchClassLoader loader = (LaunchClassLoader) MinecraftServer.class.getClassLoader();

            String nativeName = "jxbrowser-%s-6.18.jar?raw=true";

            if (OSValidator.isWindows()) {
                nativeName = String.format(nativeName, "win32");
            } else if (OSValidator.isMac()) {
                nativeName = String.format(nativeName, "mac");
            } else if (OSValidator.isUnix()) {
                nativeName = String.format(nativeName, "linux64");
            } else {
                System.err.println("----------------ERROR UNKNOWN OS: NO NATIVE BINARY AVAILABLE----------------");
                throw new Exception();
            }

            URL nativesUrl = new URL(urlBase + nativeName);
            File natives = new File(Paths.get(minecraftDir.getAbsolutePath(), "natives", nativeName).toString());

            if (!natives.exists()) {
                natives.getParentFile().mkdirs();
                natives.createNewFile();

                InputStream is = nativesUrl.openStream();
                OutputStream os = new FileOutputStream(natives);

                byte[] b = new byte[2048];
                int length;

                while ((length = is.read(b)) != -1) {
                    os.write(b, 0, length);
                    //Gui.drawRect(0, 0, is.available(), 10, new Color(255, 0, 0).getRGB());
                    //RenderUtil.drawStringClipped("Downlaoding natives... " + is.available(), 0, 10, 100, new Color(255, 0, 0).getRGB(), false);
                }
                is.close();
                os.close();
            }

            loader.addURL(natives.toURI().toURL());
        } catch (Exception e) {
            System.err.println("----------------ERROR DURING DOWNLOAD OF NATIVES----------------");
            e.printStackTrace();
        }
        return null;
    }
}