package com.nstut.openui.compat;
import net.minecraft.util.ResourceLocation;
public final class LegacyResources {
    private LegacyResources() { }
    public static ResourceLocation tryParse(String id) {
        if(id==null)return null;String[] parts=id.split(":",-1);
        if(parts.length>2)return null;String namespace=parts.length==2?parts[0]:"minecraft",path=parts[parts.length-1];
        if(!namespace.matches("[a-z0-9_.-]+")||!path.matches("[a-z0-9/._-]+"))return null;
        return new ResourceLocation(namespace,path);
    }
}
