package dev.facecam.client.capture;

import ai.onnxruntime.*;
import dev.facecam.core.*;
import java.awt.image.BufferedImage;
import java.nio.FloatBuffer;
import java.util.*;

/** Java desktop ONNX inference; no Python, MediaPipe Java API, or cloud service. */
public final class FaceTracker implements AutoCloseable {
    private final OrtEnvironment env;
    private final OrtSession detector,mesh;
    private final float[][] anchors=new float[896][2];
    private final float[] detectorPixels=new float[3*128*128],meshPixels=new float[3*192*192];
    public record Result(float[][] screen,float[][] stabilized){}
    public FaceTracker() throws Exception {
        env=OrtEnvironment.getEnvironment();
        try(var options=new OrtSession.SessionOptions()) {
            options.setIntraOpNumThreads(2);options.setInterOpNumThreads(1);
            detector=env.createSession(resource("face_detection_short_range.onnx"),options);
            try { mesh=env.createSession(resource("face_mesh_Nx3x192x192.onnx"),options); }catch(Exception e){detector.close();throw e;}
        }
        int i=0;
        for(int cells:new int[]{16,8})for(int y=0;y<cells;y++)for(int x=0;x<cells;x++)for(int k=0;k<(cells==16?2:6);k++)anchors[i++]=new float[]{(x+.5f)/cells,(y+.5f)/cells};
    }
    private static byte[] resource(String name)throws Exception{
        try(var s=FaceTracker.class.getResourceAsStream("/facecam/models/"+name)){if(s==null)throw new IllegalStateException("Missing bundled face model: "+name);return s.readAllBytes();}
    }
    private float[][] run(OrtSession session,float[] pixels,int size)throws OrtException {
        try(var input=OnnxTensor.createTensor(env,FloatBuffer.wrap(pixels),new long[]{1,3,size,size});var output=session.run(Map.of(session.getInputNames().iterator().next(),input))) {
            float[][] values=new float[output.size()][];
            for(int i=0;i<values.length;i++){FloatBuffer b=((OnnxTensor)output.get(i)).getFloatBuffer();values[i]=new float[b.remaining()];b.get(values[i]);}
            return values;
        }
    }
    public Result track(BufferedImage im)throws OrtException {
        float longSide=Math.max(im.getWidth(),im.getHeight());
        float scale=128/longSide,padx=(128-im.getWidth()*scale)/2,pady=(128-im.getHeight()*scale)/2;
        float[] input=pixels(im,128,1/scale,0,-padx/scale,0,1/scale,-pady/scale,true,detectorPixels);
        float[][] raw=run(detector,input,128);
        List<float[]> candidates=new ArrayList<>();
        for(int i=0;i<896;i++) {
            float probability=sigmoid(raw[1][i]);if(probability<.75f)continue;
            float[] d=new float[17];d[0]=probability;
            for(int k=0;k<16;k++){float v=raw[0][i*16+k]/128;d[k+1]=v+(k==2||k==3?0:anchors[i][k%2]);}
            candidates.add(d);
        }
        if(candidates.isEmpty())return null;
        candidates.sort((a,b)->Float.compare(b[0],a[0]));
        float[] best=candidates.getFirst(),blend=new float[17];float weight=0;
        for(float[] d:candidates)if(iou(best,d)>.3f){weight+=d[0];for(int k=1;k<17;k++)blend[k]+=d[k]*d[0];}
        for(int k=1;k<17;k++)blend[k]/=weight;
        float cx=(blend[1]*128-padx)/scale,cy=(blend[2]*128-pady)/scale;
        float side=Math.max(blend[3],blend[4])*longSide*1.5f;
        if(side<60||side>longSide*1.8f)return null;
        float angle=(float)Math.atan2(blend[8]-blend[6],blend[7]-blend[5]);
        float cos=(float)Math.cos(angle),sin=(float)Math.sin(angle);
        float step=side/192;
        float[][] predicted=run(mesh,pixels(im,192,cos*step,-sin*step,cx-side*.5f*(cos-sin),sin*step,cos*step,cy-side*.5f*(sin+cos),false,meshPixels),192);
        if(sigmoid(predicted[1][0])<.90f)return null;
        float[][] screen=new float[468][3],points=new float[468][3];
        for(int i=0;i<468;i++) {
            float dx=(predicted[0][i*3]/192-.5f)*side,dy=(predicted[0][i*3+1]/192-.5f)*side;
            float x=cx+cos*dx-sin*dy,y=cy+sin*dx+cos*dy,z=predicted[0][i*3+2]/192*side;
            if(!Float.isFinite(x)||!Float.isFinite(y)||!Float.isFinite(z))return null;
            screen[i]=new float[]{x,y,z};points[i]=new float[]{x,-y,-z};
        }
        for(int i:Topology.OVAL)if(screen[i][0]<2||screen[i][1]<2||screen[i][0]>im.getWidth()-3||screen[i][1]>im.getHeight()-3)return null;
        float[][] stabilized=PoseStabilizer.align(points);
        // Large residuals imply a profile, bad crop, or strongly corrupted tracking. Hold/fade instead of stretching.
        double residual=0;for(int i:new int[]{10,33,133,234,263,362,454})for(int k=0;k<3;k++){double d=stabilized[i][k]-Topology.INSTANCE.canonical[i][k];residual+=d*d;}
        if(residual>100)return null;
        return new Result(screen,stabilized);
    }
    private static float sigmoid(float x){return (float)(1/(1+Math.exp(-Math.max(-80,Math.min(80,x)))));}
    private static float iou(float[] a,float[] b){float dx=Math.max(0,Math.min(a[1]+a[3]/2,b[1]+b[3]/2)-Math.max(a[1]-a[3]/2,b[1]-b[3]/2));float dy=Math.max(0,Math.min(a[2]+a[4]/2,b[2]+b[4]/2)-Math.max(a[2]-a[4]/2,b[2]-b[4]/2));float area=dx*dy;return area/Math.max(1e-9f,a[3]*a[4]+b[3]*b[4]-area);}
    private static float[] pixels(BufferedImage im,int n,float a,float b,float tx,float c,float d,float ty,boolean signed,float[] out) {
        for(int y=0;y<n;y++)for(int x=0;x<n;x++) {
            float sx=a*(x+.5f)+b*(y+.5f)+tx,sy=c*(x+.5f)+d*(y+.5f)+ty;
            int ix=(int)Math.floor(sx),iy=(int)Math.floor(sy);float fx=sx-ix,fy=sy-iy;
            for(int channel=0;channel<3;channel++){
                int shift=16-channel*8;float p=sample(im,ix,iy,shift),q=sample(im,ix+1,iy,shift),r=sample(im,ix,iy+1,shift),s=sample(im,ix+1,iy+1,shift);
                float v=((p*(1-fx)+q*fx)*(1-fy)+(r*(1-fx)+s*fx)*fy)/255f;
                out[channel*n*n+y*n+x]=signed?v*2-1:v;
            }
        }
        return out;
    }
    private static int sample(BufferedImage im,int x,int y,int shift){return x>=0&&y>=0&&x<im.getWidth()&&y<im.getHeight()?(im.getRGB(x,y)>>shift)&255:0;}
    public void close() {try{mesh.close();}catch(Exception ignored){}try{detector.close();}catch(Exception ignored){}}
}
