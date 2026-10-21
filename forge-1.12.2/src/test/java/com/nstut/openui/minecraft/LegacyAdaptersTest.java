package com.nstut.openui.minecraft;
import com.nstut.openui.compat.LegacyResources;
import com.nstut.openui.minecraft.LegacyKeys;
import com.nstut.openui.minecraft.ContainerKeyPolicy;
import org.junit.Test;
import static org.junit.Assert.*;
public class LegacyAdaptersTest {
    @Test public void lwjgl2KeysPreserveFrameworkNavigationAndCommitContract() {
        assertEquals(256,LegacyKeys.translate(1,'\0'));assertEquals(257,LegacyKeys.translate(28,'\0'));
        assertEquals(258,LegacyKeys.translate(15,'\0'));assertEquals(263,LegacyKeys.translate(203,'\0'));
        assertEquals(262,LegacyKeys.translate(205,'\0'));assertEquals(335,LegacyKeys.translate(156,'\0'));
        assertEquals(65,LegacyKeys.translate(30,'a'));assertEquals(65,LegacyKeys.translate(30,'\0'));assertEquals(65,LegacyKeys.translate(30,'\1'));
        assertEquals(49,LegacyKeys.translate(2,'1'));assertEquals(49,LegacyKeys.translate(2,'!'));assertEquals(49,LegacyKeys.translate(2,'\0'));
        assertEquals(59,LegacyKeys.translate(39,':'));assertEquals(47,LegacyKeys.translate(53,'?'));assertEquals(96,LegacyKeys.translate(41,'~'));
        assertEquals(290,LegacyKeys.translate(59,'\0'));assertEquals(301,LegacyKeys.translate(88,'\0'));assertEquals(340,LegacyKeys.translate(42,'\0'));
        assertEquals(-1,LegacyKeys.translate(0,'a'));
    }
    @Test public void invalidTextureIdsAreRejectedBeforeBinding() {
        assertNull(LegacyResources.tryParse(null));assertNull(LegacyResources.tryParse("Upper:Case"));assertNull(LegacyResources.tryParse("a:b:c"));
        assertNull(LegacyResources.tryParse("a:"));assertEquals("minecraft:textures/gui/widgets.png",LegacyResources.tryParse("textures/gui/widgets.png").toString());
        assertEquals("openui_mc:icons/test.png",LegacyResources.tryParse("openui_mc:icons/test.png").toString());
    }
    @Test public void inventoryKeyCannotCloseWhileTextOrModalOwnsInput() {
        assertEquals(ContainerKeyPolicy.Route.OPEN_UI,ContainerKeyPolicy.route(true,true,false));
        assertEquals(ContainerKeyPolicy.Route.OPEN_UI,ContainerKeyPolicy.route(true,false,true));
        assertEquals(ContainerKeyPolicy.Route.VANILLA,ContainerKeyPolicy.route(true,false,false));
    }
}
