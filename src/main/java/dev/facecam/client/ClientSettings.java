package dev.facecam.client;

import com.google.gson.GsonBuilder;
import net.neoforged.fml.loading.FMLPaths;
import java.nio.file.*;
import dev.facecam.core.FaceAppearance;

public final class ClientSettings {
    public volatile int camera=0, resolution=128, fps=15;
    public volatile float depth=FaceAppearance.DEFAULT_DEPTH, smoothing=.45f, faceSize=FaceAppearance.DEFAULT_SIZE;
    public int appearanceVersion=0;
    public transient boolean migrateLegacyKeys=true;
    public volatile boolean hideReceived=false;
    private static final Path FILE=FMLPaths.CONFIGDIR.get().resolve("facecam-client.json");
    public static ClientSettings load() {
        try { var s=new GsonBuilder().create().fromJson(Files.readString(FILE),ClientSettings.class);if(s!=null){s.migrateLegacyKeys=s.appearanceVersion<1;if(s.appearanceVersion<1&&Math.abs(s.depth-.85f)<.0001f)s.depth=FaceAppearance.DEFAULT_DEPTH;s.appearanceVersion=1;s.validate();s.save();return s;} }catch(Exception ignored){}
        var s=new ClientSettings();s.appearanceVersion=1;return s;
    }
    private void validate(){camera=Math.max(0,Math.min(15,camera));if(!dev.facecam.core.FaceFrame.resolutionValid(resolution))resolution=128;fps=Math.max(1,Math.min(30,fps));depth=Float.isFinite(depth)?Math.max(0,Math.min(FaceAppearance.MAX_DEPTH,depth)):FaceAppearance.DEFAULT_DEPTH;faceSize=Float.isFinite(faceSize)?Math.max(FaceAppearance.MIN_SIZE,Math.min(FaceAppearance.MAX_SIZE,faceSize)):FaceAppearance.DEFAULT_SIZE;smoothing=Float.isFinite(smoothing)?Math.max(0,Math.min(.85f,smoothing)):.45f;}
    public void resetSize(){faceSize=FaceAppearance.DEFAULT_SIZE;save();}
    public void save(){try {validate();Files.createDirectories(FILE.getParent());Files.writeString(FILE,new GsonBuilder().setPrettyPrinting().create().toJson(this));}catch(Exception e){FaceClient.status="Could not save settings: "+e.getMessage();}}
}
