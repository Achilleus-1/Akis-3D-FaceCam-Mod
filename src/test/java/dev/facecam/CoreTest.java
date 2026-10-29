package dev.facecam;

import dev.facecam.core.*;
import dev.facecam.client.capture.CameraWorker;
import org.junit.jupiter.api.Test;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;

class CoreTest {
    @Test void topologyHasDepthAndValidTriangles(){
        var t=Topology.INSTANCE;assertEquals(468,t.canonical.length);assertTrue(t.triangles.length>850);
        for(int[] tri:t.triangles)for(int i:tri)assertTrue(i>=0&&i<468);
        short[] xyz=new PoseStabilizer().geometry(copy(t.canonical),.85f,.45f);
        FaceFrame f=new FaceFrame(1,128,new byte[8],xyz);assertTrue(f.valid());
        assertTrue(f.coordinate(1,2)>.5,"Nose must have actual geometric depth");
        for(int feature:new int[]{50,9,0})assertTrue(f.coordinate(feature,2)>.1,"Cheek, brow and lips need geometric depth: "+feature);
        for(int i:Topology.OVAL)assertEquals(0,f.coordinate(i,2),1e-4,"Boundary must attach to skin");
        short[] flat=new PoseStabilizer().geometry(copy(t.canonical),0,0);for(int i=0;i<468;i++)assertEquals(0,flat[i*3+2]);
    }
    @Test void removesRigidTranslationRotationScale(){
        float[][] canonical=Topology.INSTANCE.canonical,points=copy(canonical);
        double ax=.2,ay=.35,az=-.3;
        for(float[] v:points){double x=v[0],y=v[1],z=v[2];double x1=x*Math.cos(ay)+z*Math.sin(ay),z1=-x*Math.sin(ay)+z*Math.cos(ay);double y2=y*Math.cos(ax)-z1*Math.sin(ax),z2=y*Math.sin(ax)+z1*Math.cos(ax);v[0]=(float)((x1*Math.cos(az)-y2*Math.sin(az))*13+220);v[1]=(float)((x1*Math.sin(az)+y2*Math.cos(az))*13-140);v[2]=(float)(z2*13+90);}
        float[][] aligned=PoseStabilizer.align(points);
        double error=0;for(int i=0;i<468;i++)for(int k=0;k<3;k++)error=Math.max(error,Math.abs(aligned[i][k]-canonical[i][k]));
        assertTrue(error<.03,"Rigid pose residual: "+error);
    }
    @Test void expressionSurvivesPoseRemoval(){
        float[][] points=copy(Topology.INSTANCE.canonical);points[14][1]-=1.2f;points[159][1]-=.3f;
        float[][] aligned=PoseStabilizer.align(points);
        assertEquals(Topology.INSTANCE.canonical[14][1]-1.2f,aligned[14][1],.02);
        assertEquals(Topology.INSTANCE.canonical[159][1]-.3f,aligned[159][1],.02);
    }
    @Test void canonicalAtlasExcludesRoomPixels() {
        BufferedImage camera=new BufferedImage(320,320,BufferedImage.TYPE_INT_RGB);
        for(int y=0;y<320;y++)for(int x=0;x<320;x++)camera.setRGB(x,y,0xff00ff);
        // All sampled mesh triangles are inside the green center; magenta represents room/background.
        for(int y=80;y<240;y++)for(int x=80;x<240;x++)camera.setRGB(x,y,0x30bd50);
        float[][] points=new float[468][3];for(int i=0;i<468;i++)points[i]=new float[]{100+Topology.INSTANCE.uv[i][0]*120,100+Topology.INSTANCE.uv[i][1]*120,0};
        BufferedImage atlas=FaceRaster.atlas(camera,points,128);
        for(int y=0;y<128;y++)for(int x=0;x<128;x++){int rgb=atlas.getRGB(x,y)&0xffffff;assertNotEquals(0xff00ff,rgb);assertTrue(rgb==0||rgb==0x30bd50);}
        float[] mask=FaceImageCodec.mask(128);assertEquals(0,mask[0]);assertTrue(Arrays.stream(toDouble(mask)).max().orElse(0)>.9);
    }
    @Test void tokenBudgetBoundsFramesAndBytes(){
        var rate=new RateBudget(0,10000,15);assertTrue(rate.accept(0,1000,10000,15));assertTrue(rate.accept(0,1000,10000,15));assertFalse(rate.accept(0,1000,10000,15));
        assertTrue(rate.accept(1_000_000_000L,9000,10000,15));assertFalse(rate.accept(1_000_000_000L,9000,10000,15));
    }
    @Test void rejectsUnboundedGeometryAndFrames(){
        short[] xyz=new PoseStabilizer().geometry(copy(Topology.INSTANCE.canonical),.8f,0);
        assertFalse(new FaceFrame(1,256,new byte[20],xyz).valid());
        assertFalse(new FaceFrame(1,128,new byte[23001],xyz).valid());
        assertFalse(new FaceFrame(1,128,new byte[20],new short[2000]).valid());
        xyz[0]=Short.MAX_VALUE;assertFalse(new FaceFrame(1,128,new byte[20],xyz).valid());
    }
    @Test void compressionRespectsServerboundPacketLimit()throws Exception {
        BufferedImage im=new BufferedImage(192,192,BufferedImage.TYPE_INT_RGB);var random=new java.util.Random(17);
        for(int y=0;y<192;y++)for(int x=0;x<192;x++)im.setRGB(x,y,random.nextInt());
        byte[] jpeg=CameraWorker.jpeg(im,23000);assertNotNull(jpeg);assertTrue(jpeg.length+468*6+32<32768);
    }
    @Test void receiverRejectsWrongDimensionsBeforePixelDecode()throws Exception {
        var image=new BufferedImage(16,16,BufferedImage.TYPE_INT_RGB);
        byte[] bytes=CameraWorker.jpeg(image,23000);
        var error=assertThrows(java.io.IOException.class,()->FaceImageCodec.decode(new FaceFrame(1,128,bytes,new short[468*3])));
        assertEquals("Wrong image dimensions",error.getMessage());
    }
    @Test void sizePreservesHeadCenterAndSupportsExtremes() {
        assertEquals(-4,FaceAppearance.y(-4,4));
        assertEquals(3,FaceAppearance.x(3,1));assertEquals(-7,FaceAppearance.y(-7,1));
        assertEquals(.3f,FaceAppearance.x(3,.1f),1e-6);assertEquals(12,FaceAppearance.x(3,4));
        assertEquals(.175f,FaceAppearance.depth(1.75f,.1f),1e-6);assertEquals(7,FaceAppearance.depth(1.75f,4));
        short[] xyz=new PoseStabilizer().geometry(copy(Topology.INSTANCE.canonical),FaceAppearance.DEFAULT_DEPTH,0);
        for(float size:new float[]{.1f,1f,4f})assertTrue(new FaceFrame(1,128,new byte[8],xyz,size).valid());
        for(float size:new float[]{0,4.01f,Float.NaN,Float.POSITIVE_INFINITY})assertFalse(new FaceFrame(1,128,new byte[8],xyz,size).valid());
    }
    @Test void strongerDepthStaysBoundedAndBoundaryAttached() {
        float[][] c=Topology.INSTANCE.canonical;
        FaceFrame original=new FaceFrame(1,128,new byte[8],new PoseStabilizer().geometry(copy(c),.85f,0));
        FaceFrame deeper=new FaceFrame(2,128,new byte[8],new PoseStabilizer().geometry(copy(c),FaceAppearance.DEFAULT_DEPTH,0));
        assertTrue(deeper.valid());assertTrue(deeper.coordinate(1,2)>original.coordinate(1,2)*2);
        FaceFrame maximum=new FaceFrame(3,128,new byte[8],new PoseStabilizer().geometry(copy(c),6,0));assertTrue(maximum.valid());
        for(int i:Topology.OVAL)assertEquals(0,maximum.coordinate(i,2));
        short[] invalid=maximum.geometry().clone();invalid[5]=24577;assertFalse(new FaceFrame(4,128,new byte[8],invalid).valid());
    }
    private static float[][] copy(float[][] p){return Arrays.stream(p).map(float[]::clone).toArray(float[][]::new);}
    private static double[] toDouble(float[] p){double[] d=new double[p.length];for(int i=0;i<p.length;i++)d[i]=p[i];return d;}
}
