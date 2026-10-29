package dev.facecam.client.capture;

import dev.facecam.client.*;
import dev.facecam.core.*;
import org.bytedeco.opencv.opencv_videoio.VideoCapture;
import org.bytedeco.opencv.opencv_core.Mat;
import static org.bytedeco.opencv.global.opencv_videoio.*;
import java.awt.image.*;
import java.io.*;
import java.util.concurrent.atomic.*;
import javax.imageio.*;
import javax.imageio.stream.MemoryCacheImageOutputStream;

public final class CameraWorker implements AutoCloseable {
    private static final AtomicLong FRAME_IDS=new AtomicLong(System.currentTimeMillis());
    public record Preview(FaceFrame frame,BufferedImage image){}
    public final AtomicReference<Preview> latest=new AtomicReference<>();
    private final AtomicBoolean running=new AtomicBoolean(true);
    private final Thread thread;
    public volatile String status="Opening camera…";
    public CameraWorker(int camera) {
        thread=new Thread(()->run(camera),"FaceCam-capture");thread.setDaemon(true);thread.start();
    }
    private void run(int camera) {
        // Native classes are resolved only after explicit Start / Preview on a Windows client.
        try(VideoCapture capture=new VideoCapture();Mat frame=new Mat();FaceTracker tracker=new FaceTracker()) {
            if(!running.get())return;
            if(!capture.open(camera,CAP_DSHOW))throw new IOException("Camera "+camera+" unavailable: check Windows camera privacy, other camera apps, or choose another camera.");
            capture.set(CAP_PROP_FRAME_WIDTH,640);capture.set(CAP_PROP_FRAME_HEIGHT,480);capture.set(CAP_PROP_FPS,30);capture.set(CAP_PROP_BUFFERSIZE,1);
            var stabilizer=new PoseStabilizer();int failures=0;long lastValid=0;
            BufferedImage cameraImage=null;
            while(running.get()) {
                long started=System.nanoTime();
                if(!capture.read(frame)||frame.empty()){if(++failures>5)throw new IOException("Camera stopped delivering frames; unplugged, denied, or in use.");continue;}
                failures=0;
                if(frame.channels()!=3)throw new IOException("Unsupported camera pixel format (expected BGR).");
                if(cameraImage==null||cameraImage.getWidth()!=frame.cols()||cameraImage.getHeight()!=frame.rows())cameraImage=new BufferedImage(frame.cols(),frame.rows(),BufferedImage.TYPE_3BYTE_BGR);
                byte[] data=((DataBufferByte)cameraImage.getRaster().getDataBuffer()).getData();
                // DirectShow BGR frame rows can have padding.
                int rowBytes=frame.cols()*3;for(int y=0;y<frame.rows();y++)frame.ptr(y).get(data,y*rowBytes,rowBytes);
                var result=tracker.track(cameraImage);
                if(result==null) {
                    status="Face not tracked — face camera in good light";
                    if(System.nanoTime()-lastValid>350_000_000L)latest.set(null);
                } else {
                    lastValid=System.nanoTime();
                    ClientSettings settings=FaceClient.settings;
                    int resolution=settings.resolution;
                    BufferedImage atlas=FaceRaster.atlas(cameraImage,result.screen(),resolution);
                    byte[] jpeg=jpeg(atlas,Math.min(FaceFrame.MAX_IMAGE,FaceClient.maxImage));
                    if(jpeg!=null) {
                        FaceFrame face=new FaceFrame(FRAME_IDS.updateAndGet(last->Math.max(last+1,System.currentTimeMillis())),resolution,jpeg,stabilizer.geometry(result.stabilized(),settings.depth,settings.smoothing),settings.faceSize);
                        if(running.get()&&face.valid())latest.set(new Preview(face,atlas));
                        status="Face tracked · "+jpeg.length/1024+" KiB/frame · local inference";
                    }else{latest.set(null);status="Frame exceeds server image limit; lower resolution";}
                }
                int fps=Math.min(FaceClient.settings.fps,FaceClient.maxFps);
                Preview p=latest.get();long bandwidth=p==null?0:(long)(p.frame.wireBytes()*1e9/FaceClient.maxBandwidth);
                long period=Math.max(1_000_000_000L/Math.max(1,fps),bandwidth);
                long wait=period-(System.nanoTime()-started);if(wait>0)Thread.sleep(wait/1_000_000,(int)(wait%1_000_000));
            }
            capture.release();
        } catch(InterruptedException e){Thread.currentThread().interrupt();}
        catch(Throwable e){status=(e instanceof UnsatisfiedLinkError||e instanceof NoClassDefFoundError)?"Native components failed to load. Use Windows x64 Java 21; install Microsoft VC++ 2015–2022 x64 runtime if missing. "+e.getMessage():"Camera/inference error: "+e.getClass().getSimpleName()+": "+e.getMessage();}
        finally {latest.set(null);running.set(false);}
    }
    public static byte[] jpeg(BufferedImage image,int max)throws IOException {
        var writer=ImageIO.getImageWritersByFormatName("jpeg").next();
        try {
            for(float quality:new float[]{.72f,.5f,.3f,.15f}){
                var bytes=new ByteArrayOutputStream();
                try(var out=new MemoryCacheImageOutputStream(bytes)){
                    writer.setOutput(out);var params=writer.getDefaultWriteParam();params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);params.setCompressionQuality(quality);writer.write(null,new IIOImage(image,null,null),params);
                }
                if(bytes.size()<=max)return bytes.toByteArray();
            }
            return null;
        }finally{writer.dispose();}
    }
    public boolean alive(){return running.get();}
    public boolean terminated(){return !thread.isAlive();}
    public void close(){running.set(false);latest.set(null);thread.interrupt();}
}
