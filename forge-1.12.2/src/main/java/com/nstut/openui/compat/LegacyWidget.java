package com.nstut.openui.compat;
/** Native widgets remain owned by OpenUI's clip-aware event router, not GuiScreen's button list. */
public abstract class LegacyWidget { public boolean active=true,visible=true; public int x,y,width,height; }
