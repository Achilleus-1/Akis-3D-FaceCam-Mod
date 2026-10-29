package dev.facecam.core;

import java.awt.image.BufferedImage;

/** Rasterize only face triangles into a canonical UV atlas. No raw camera crop is sent. */
public final class FaceRaster {
    public static BufferedImage atlas(BufferedImage camera,float[][] screen,int size) {
        var t=Topology.INSTANCE;
        BufferedImage out=new BufferedImage(size,size,BufferedImage.TYPE_INT_RGB);
        for(int[] tri:t.triangles) {
            float[] a=t.uv[tri[0]],b=t.uv[tri[1]],c=t.uv[tri[2]];
            float den=(b[1]-c[1])*(a[0]-c[0])+(c[0]-b[0])*(a[1]-c[1]);
            if(Math.abs(den)<1e-9)continue;
            int x0=Math.max(0,(int)(Math.min(a[0],Math.min(b[0],c[0]))*size));
            int x1=Math.min(size-1,(int)(Math.max(a[0],Math.max(b[0],c[0]))*size));
            int y0=Math.max(0,(int)(Math.min(a[1],Math.min(b[1],c[1]))*size));
            int y1=Math.min(size-1,(int)(Math.max(a[1],Math.max(b[1],c[1]))*size));
            for(int y=y0;y<=y1;y++)for(int x=x0;x<=x1;x++) {
                float u=(x+.5f)/size,v=(y+.5f)/size;
                float wa=((b[1]-c[1])*(u-c[0])+(c[0]-b[0])*(v-c[1]))/den;
                float wb=((c[1]-a[1])*(u-c[0])+(a[0]-c[0])*(v-c[1]))/den,wc=1-wa-wb;
                if(wa<0||wb<0||wc<0)continue;
                // Shrink the outer polygon in image space so bilinear samples do not cross into background.
                float edge=wa*t.edge[tri[0]]+wb*t.edge[tri[1]]+wc*t.edge[tri[2]];
                if(edge<.15f)continue;
                float sx=wa*screen[tri[0]][0]+wb*screen[tri[1]][0]+wc*screen[tri[2]][0];
                float sy=wa*screen[tri[0]][1]+wb*screen[tri[1]][1]+wc*screen[tri[2]][1];
                if(sx>=1&&sy>=1&&sx<camera.getWidth()-1&&sy<camera.getHeight()-1)out.setRGB(x,y,sample(camera,sx,sy));
            }
        }
        return out;
    }
    private static int sample(BufferedImage im,float x,float y) {
        int ix=(int)x,iy=(int)y;float fx=x-ix,fy=y-iy;int rgb=0;
        for(int shift=0;shift<=16;shift+=8){float a=(im.getRGB(ix,iy)>>shift)&255,b=(im.getRGB(ix+1,iy)>>shift)&255,c=(im.getRGB(ix,iy+1)>>shift)&255,d=(im.getRGB(ix+1,iy+1)>>shift)&255;rgb|=Math.round((a*(1-fx)+b*fx)*(1-fy)+(c*(1-fx)+d*fx)*fy)<<shift;}
        return rgb;
    }
    private FaceRaster(){}
}
