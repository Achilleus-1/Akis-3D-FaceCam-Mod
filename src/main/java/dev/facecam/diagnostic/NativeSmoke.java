package dev.facecam.diagnostic;

import dev.facecam.client.capture.FaceTracker;
import dev.facecam.core.*;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.opencv.opencv_videoio.VideoCapture;
import org.bytedeco.opencv.opencv_core.Mat;
import static org.bytedeco.opencv.global.opencv_videoio.*;
import java.awt.image.*;
import java.nio.file.*;
import javax.imageio.ImageIO;
import java.io.DataOutputStream;

/** Explicit developer command; never runs during normal mod initialization. */
public final class NativeSmoke {
    public static void main(String[] args)throws Exception {
        Loader.load(org.bytedeco.opencv.global.opencv_videoio.class);
        System.out.println("NATIVE_OPENCV_OK");
        try(var tracker=new FaceTracker()) {
            System.out.println("ONNX_SESSIONS_OK");
            if(args.length>0&&!args[0].equals("--camera")) {
                BufferedImage input=ImageIO.read(Path.of(args[0]).toFile());
                check(tracker,input,"fixture");
            }
            if(args.length>0&&args[0].equals("--camera")) {
                int index=args.length>1?Integer.parseInt(args[1]):0;
                try(var camera=new VideoCapture();var frame=new Mat()) {
                    if(!camera.open(index,CAP_DSHOW)){System.out.println("NO_CAMERA_AVAILABLE index="+index);return;}
                    camera.set(CAP_PROP_FRAME_WIDTH,640);camera.set(CAP_PROP_FRAME_HEIGHT,480);
                    for(int i=0;i<10;i++)if(camera.read(frame)&&!frame.empty()) {
                        BufferedImage im=new BufferedImage(frame.cols(),frame.rows(),BufferedImage.TYPE_3BYTE_BGR);
                        byte[] pixels=((DataBufferByte)im.getRaster().getDataBuffer()).getData();for(int y=0;y<frame.rows();y++)frame.ptr(y).get(pixels,y*frame.cols()*3,frame.cols()*3);
                        if(check(tracker,im,"camera"))return;
                    }
                    System.out.println("CAMERA_OPEN_BUT_NO_VALID_FACE");
                }
            }
        }
    }
    private static boolean check(FaceTracker tracker,BufferedImage input,String kind)throws Exception {
        long start=System.nanoTime();var r=tracker.track(input);long elapsed=(System.nanoTime()-start)/1_000_000;
        if(r==null){System.out.println("NO_VALID_FACE kind="+kind+" elapsedMs="+elapsed);return false;}
        short[] xyz=new PoseStabilizer().geometry(r.stabilized(),FaceAppearance.DEFAULT_DEPTH,.45f);
        var atlas=FaceRaster.atlas(input,r.screen(),128);
        Path out=Path.of("build/verification/"+kind+"-masked.png");Files.createDirectories(out.getParent());ImageIO.write(atlas,"png",out.toFile());
        if(kind.equals("fixture")) {
            var bytes=new java.io.ByteArrayOutputStream();ImageIO.write(atlas,"jpg",bytes);
            try(var stream=new DataOutputStream(Files.newOutputStream(out.getParent().resolve("reference.frame")))){stream.writeInt(128);stream.writeInt(bytes.size());stream.write(bytes.toByteArray());for(short s:xyz)stream.writeShort(s);}
        }
        System.out.println("FACE_OK kind="+kind+" landmarks="+r.screen().length+" elapsedMs="+elapsed+" noseDepth="+xyz[5]/4096f+" atlas="+out);
        return true;
    }
}
