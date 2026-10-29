package dev.facecam.core;

import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.io.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Bounded, graphics-independent JPEG decode and canonical alpha reconstruction. */
public final class FaceImageCodec {
    public static int[] decode(FaceFrame frame)throws IOException {
        byte[] bytes=frame.image();
        if(bytes.length<3||bytes.length>FaceFrame.MAX_IMAGE||(bytes[0]&255)!=255||(bytes[1]&255)!=216)throw new IOException("Expected bounded JPEG");
        try(var input=new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))){
            var readers=ImageIO.getImageReaders(input);if(!readers.hasNext())throw new IOException("Invalid JPEG");var reader=readers.next();
            try {reader.setInput(input,true,true);int n=frame.resolution();if(!FaceFrame.resolutionValid(n)||reader.getWidth(0)!=n||reader.getHeight(0)!=n)throw new IOException("Wrong image dimensions");var image=reader.read(0);int[] rgba=new int[n*n];float[] mask=mask(n);
                for(int y=0;y<n;y++)for(int x=0;x<n;x++){int rgb=image.getRGB(x,y);int alpha=(int)(mask[y*n+x]*255);rgba[y*n+x]=(alpha<<24)|((rgb&255)<<16)|(rgb&0xff00)|((rgb>>16)&255);}
                return rgba;
            }finally{reader.dispose();}
        }
    }
    private static final Map<Integer,float[]> MASKS=new ConcurrentHashMap<>();
    public static float[] mask(int n){if(!FaceFrame.resolutionValid(n))throw new IllegalArgumentException("Unsupported atlas size");return MASKS.computeIfAbsent(n,FaceImageCodec::makeMask);}
    private static float[] makeMask(int n) {
        var t=Topology.INSTANCE;float[] mask=new float[n*n];
        for(int[] tri:t.triangles) {
            float[] a=t.uv[tri[0]],b=t.uv[tri[1]],c=t.uv[tri[2]];
            float den=(b[1]-c[1])*(a[0]-c[0])+(c[0]-b[0])*(a[1]-c[1]);if(Math.abs(den)<1e-9)continue;
            for(int y=Math.max(0,(int)(Math.min(a[1],Math.min(b[1],c[1]))*n));y<=Math.min(n-1,(int)(Math.max(a[1],Math.max(b[1],c[1]))*n));y++)
                for(int x=Math.max(0,(int)(Math.min(a[0],Math.min(b[0],c[0]))*n));x<=Math.min(n-1,(int)(Math.max(a[0],Math.max(b[0],c[0]))*n));x++){
                    float u=(x+.5f)/n,v=(y+.5f)/n;float wa=((b[1]-c[1])*(u-c[0])+(c[0]-b[0])*(v-c[1]))/den,wb=((c[1]-a[1])*(u-c[0])+(a[0]-c[0])*(v-c[1]))/den,wc=1-wa-wb;
                    if(wa>=0&&wb>=0&&wc>=0){float e=wa*t.edge[tri[0]]+wb*t.edge[tri[1]]+wc*t.edge[tri[2]];float f=Math.max(0,Math.min(1,(e-.2f)/.8f));mask[y*n+x]=f*f*(3-2*f);}
                }
        }
        return mask;
    }
    private FaceImageCodec(){}
}
