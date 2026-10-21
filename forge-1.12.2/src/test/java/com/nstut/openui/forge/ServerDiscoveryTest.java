package com.nstut.openui.forge;
import java.net.URL;
import java.net.URLClassLoader;
import org.junit.Test;
import net.minecraftforge.fml.common.Mod;
import static org.junit.Assert.*;
/** Class-discovery check, not a replacement for booting a Forge dedicated server. */
public class ServerDiscoveryTest {
    @Test public void modContainerIsPresentOnServerWithoutLoadingClientClasses() throws Exception {
        URL[] urls={OpenUiMcForge.class.getProtectionDomain().getCodeSource().getLocation(),Mod.class.getProtectionDomain().getCodeSource().getLocation()};
        try(URLClassLoader isolated=new URLClassLoader(urls,null) {
            @Override protected Class<?> loadClass(String name,boolean resolve) throws ClassNotFoundException {
                if(name.startsWith("net.minecraft.client.")||name.startsWith("com.nstut.openui.minecraft."))throw new AssertionError("Client class loaded during server discovery: "+name);
                return super.loadClass(name,resolve);
            }
        }) {
            Class<?> entry=Class.forName("com.nstut.openui.forge.OpenUiMcForge",true,isolated);
            assertNotNull(entry.newInstance());
            java.lang.annotation.Annotation mod=entry.getAnnotations()[0];
            assertEquals("openui_mc",mod.annotationType().getMethod("modid").invoke(mod));
            assertEquals(false,mod.annotationType().getMethod("clientSideOnly").invoke(mod));
        }
    }
}
