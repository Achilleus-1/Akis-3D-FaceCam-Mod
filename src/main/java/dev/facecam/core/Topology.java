package dev.facecam.core;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Google's canonical topology is shared locally; mesh definitions are never sent. */
public final class Topology {
    public static final int[] OVAL={10,338,297,332,284,251,389,356,454,323,361,288,397,365,379,378,400,377,152,148,176,149,150,136,172,58,132,93,234,127,162,21,54,103,67,109};
    public static final Topology INSTANCE = load();
    public final float[][] canonical;
    public final int[][] triangles;
    public final float[][] uv;
    public final float[] edge;
    private Topology(float[][] c,int[][] t,float[][] uv) {
        canonical=c; triangles=t; this.uv=uv; edge=new float[468];
        // Distance from the canonical oval, rather than a rectangle, controls blend and depth.
        for(int i=0;i<468;i++) {
            double d=Double.POSITIVE_INFINITY;
            for(int j=0;j<OVAL.length;j++) {
                float[] a=uv[OVAL[j]],b=uv[OVAL[(j+1)%OVAL.length]],p=uv[i];
                double dx=b[0]-a[0],dy=b[1]-a[1];
                double k=Math.max(0,Math.min(1,((p[0]-a[0])*dx+(p[1]-a[1])*dy)/(dx*dx+dy*dy)));
                d=Math.min(d,Math.hypot(p[0]-a[0]-k*dx,p[1]-a[1]-k*dy));
            }
            edge[i]=(float)Math.min(1,d/.045);
        }
    }
    private static Topology load() {
        try(var in=Topology.class.getResourceAsStream("/facecam/models/canonical_face_model.obj")) {
            if(in==null)throw new IOException("Missing FaceCam canonical model");
            List<float[]> vertices=new ArrayList<>(),tex=new ArrayList<>(); List<int[]> tris=new ArrayList<>();
            float[][] uv=new float[468][2];
            for(String line:new String(in.readAllBytes(),StandardCharsets.UTF_8).split("\\R")) {
                String[] p=line.trim().split("\\s+");
                if(p[0].equals("v")) vertices.add(new float[]{Float.parseFloat(p[1]),Float.parseFloat(p[2]),Float.parseFloat(p[3])});
                if(p[0].equals("vt")) tex.add(new float[]{Float.parseFloat(p[1]),1-Float.parseFloat(p[2])});
                if(p[0].equals("f")) {
                    int[] tri=new int[3];
                    for(int i=0;i<3;i++){String[] v=p[i+1].split("/");tri[i]=Integer.parseInt(v[0])-1;uv[tri[i]]=tex.get(Integer.parseInt(v[1])-1);}
                    tris.add(tri);
                }
            }
            if(vertices.size()!=468)throw new IOException("Expected 468 landmarks");
            // The canonical surface leaves eye and mouth apertures open. Fill them with
            // tracked triangles to carry eyelid/eyeball and live teeth/mouth texture.
            for(int[] ring:new int[][]{
                {78,191,80,81,82,13,312,311,310,415,308,324,318,402,317,14,87,178,88,95},
                {33,7,163,144,145,153,154,155,133,173,157,158,159,160,161,246},
                {263,249,390,373,374,380,381,382,362,398,384,385,386,387,388,466}})
                for(int i=1;i<ring.length-1;i++)tris.add(new int[]{ring[0],ring[i],ring[i+1]});
            return new Topology(vertices.toArray(float[][]::new),tris.toArray(int[][]::new),uv);
        }catch(IOException e){throw new ExceptionInInitializerError(e);}
    }
}
