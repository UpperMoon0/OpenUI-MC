package com.nstut.openui.layout;
/** Integer positions and matching square hit targets for radial controls. */
public final class RadialLayout {
    private RadialLayout() {}
    public static int x(int index,int count,int radius) { return (int)(Math.cos(angle(index,count))*radius); }
    public static int y(int index,int count,int radius) { return (int)(Math.sin(angle(index,count))*radius); }
    private static double angle(int index,int count) { if(count<=0 || index<0 || index>=count) throw new IllegalArgumentException("Invalid radial index"); return -Math.PI/2+index*(Math.PI*2/count); }
    public static int hitIndex(int count,int radius,int size,double dx,double dy) {
        for(int i=0;i<count;i++) { int x=x(i,count,radius)-size/2,y=y(i,count,radius)-size/2; if(dx>=x && dx<x+size && dy>=y && dy<y+size) return i; }
        return -1;
    }
}
