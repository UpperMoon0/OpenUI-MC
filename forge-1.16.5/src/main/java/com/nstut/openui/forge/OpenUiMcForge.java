package com.nstut.openui.forge;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.network.FMLNetworkConstants;
import org.apache.commons.lang3.tuple.Pair;
/** Client UI is optional on the remote server. No client class is initialized by the mod entrypoint. */
@Mod("openui_mc")
public final class OpenUiMcForge {
    public OpenUiMcForge() {
        ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.DISPLAYTEST,
                () -> Pair.of(() -> FMLNetworkConstants.IGNORESERVERONLY, (version, remote) -> true));
    }
}
