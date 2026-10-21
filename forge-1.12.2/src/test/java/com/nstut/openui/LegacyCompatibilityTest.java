package com.nstut.openui;
import com.nstut.openui.layout.Constraints;
import com.nstut.openui.layout.RadialLayout;
import com.nstut.openui.theme.Theme;
import com.nstut.openui.state.Signal;
import com.nstut.openui.state.Signals;
import com.nstut.openui.declarative.NodeIdentity;
import com.nstut.openui.compat.Java8;
import org.junit.Test;
import static org.junit.Assert.*;
public class LegacyCompatibilityTest {
    @Test public void recordsRetainValidationValueEqualityAndNestedStaticConstruction() {
        assertEquals(0,new Constraints(-1,2,-3,4).minWidth());
        assertEquals(Theme.dark(),Theme.dark()); assertEquals(Theme.dark().hashCode(),Theme.dark().hashCode());
        assertEquals(new NodeIdentity("node","key"),new NodeIdentity("node","key"));
        assertThrows(NullPointerException.class,()->new NodeIdentity(null,"key"));
        assertTrue(Theme.dark().toString().startsWith("Theme[colors="));
    }
    @Test public void reactiveStateRunsOnJava8AndCollectionSnapshotsStayImmutable() {
        Signal<Integer> value=Signals.of(1);java.util.concurrent.atomic.AtomicInteger observed=new java.util.concurrent.atomic.AtomicInteger();
        value.subscribe(observed::set);value.set(2);assertEquals(2,observed.get());
        java.util.List<String> source=new java.util.ArrayList<>();source.add("first");
        java.util.List<String> copy=Java8.listCopy(source);source.add("later");assertEquals(1,copy.size());
        assertThrows(UnsupportedOperationException.class,()->copy.add("no"));
        assertTrue(Java8.isBlank(" \t"));assertFalse(Java8.isBlank(" text "));
    }
    @Test public void radialCornersAreClickableAndCenterAndGapsAreInactive() {
        assertEquals(1,RadialLayout.hitIndex(8,54,36,22,-22));
        assertEquals(0,RadialLayout.hitIndex(8,54,36,0,-54));
        assertEquals(-1,RadialLayout.hitIndex(8,54,36,0,0));
        assertEquals(-1,RadialLayout.hitIndex(8,54,36,0,-30));
    }
}
