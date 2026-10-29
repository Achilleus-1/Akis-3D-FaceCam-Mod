package dev.facecam.client;

import dev.facecam.FaceCam;
import dev.facecam.client.capture.CameraWorker;
import dev.facecam.client.render.FaceLayer;
import dev.facecam.core.FaceFrame;
import dev.facecam.network.FaceNetwork;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.network.chat.Component;
import com.mojang.blaze3d.platform.InputConstants;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

@Mod(value=FaceCam.ID,dist=Dist.CLIENT)
public final class FaceClient {
    public static ClientSettings settings;
    public static final RemoteStreams streams=new RemoteStreams();
    public static final KeyMapping TOGGLE=new KeyMapping("key.facecam.toggle",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_PERIOD,"key.categories.facecam");
    public static final KeyMapping CONFIG=new KeyMapping("key.facecam.config",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_COMMA,"key.categories.facecam");
    public static volatile int maxFps=15,maxImage=23000,maxBandwidth=180000;
    public static volatile String status="Camera off · not sharing";
    public static CameraWorker camera;
    private static CameraWorker retiring;
    public static boolean sharing;
    private static long lastFrame=-1,lastSent;
    private static boolean hadConnection;
    public FaceClient(IEventBus bus,ModContainer container) {
        settings=ClientSettings.load();
        bus.addListener((RegisterKeyMappingsEvent e)->{e.register(TOGGLE);e.register(CONFIG);});
        bus.addListener((EntityRenderersEvent.AddLayers e)->{for(var skin:e.getSkins()){PlayerRenderer renderer=e.getSkin(skin);if(renderer!=null)renderer.addLayer(new FaceLayer(renderer));}});
        container.registerExtensionPoint(IConfigScreenFactory.class,(mc,parent)->new FaceScreen(parent));
        FaceNetwork.receiver=streams::receive;
        FaceNetwork.limitsReceiver=p->{maxFps=Math.max(1,Math.min(30,p.fps()));maxImage=Math.max(1024,Math.min(23000,p.imageBytes()));maxBandwidth=Math.max(4096,Math.min(1000000,p.bytesSecond()));};
        NeoForge.EVENT_BUS.register(this);
        if(System.getProperty("facecam.verify.role")!=null)NeoForge.EVENT_BUS.register(new dev.facecam.diagnostic.ClientVerification());
        Runtime.getRuntime().addShutdownHook(new Thread(()->{if(camera!=null)camera.close();},"FaceCam-shutdown"));
    }
    public static void start(boolean share) {
        if(!System.getProperty("os.name").startsWith("Windows")||!System.getProperty("os.arch").equals("amd64")){status="Capture requires Windows x64 and Java 21";return;}
        if(retiring!=null&&!retiring.terminated()){status="Camera is closing; try again in a moment";return;}retiring=null;
        if(sharing&&!share&&Minecraft.getInstance().getConnection()!=null)PacketDistributor.sendToServer(new FaceNetwork.Up(true,null));
        if(camera==null||!camera.alive()){if(camera!=null)camera.close();camera=new CameraWorker(settings.camera);lastFrame=-1;}
        sharing=share;status=share?"Sharing enabled · opening camera":"Local preview only · opening camera";
    }
    public static void stop() {
        if(sharing&&Minecraft.getInstance().getConnection()!=null)PacketDistributor.sendToServer(new FaceNetwork.Up(true,null));
        sharing=false;lastFrame=-1;if(camera!=null){camera.close();retiring=camera;camera=null;}
        status="Camera off · not sharing";
        if(Minecraft.getInstance().player!=null)streams.remove(Minecraft.getInstance().player.getUUID());
    }
    @SubscribeEvent public void tick(ClientTickEvent.Post event) {
        var mc=Minecraft.getInstance();boolean connected=mc.getConnection()!=null&&mc.player!=null;
        if(settings.migrateLegacyKeys){
            settings.migrateLegacyKeys=false;
            if(CONFIG.matches(GLFW.GLFW_KEY_F7,0))CONFIG.setKey(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_COMMA));
            if(TOGGLE.matches(GLFW.GLFW_KEY_F8,0))TOGGLE.setKey(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_PERIOD));
            KeyMapping.resetMapping();mc.options.save();settings.save();
        }
        if(hadConnection&&!connected){stop();streams.clear();maxFps=15;maxImage=23000;maxBandwidth=180000;}hadConnection=connected;
        while(CONFIG.consumeClick())mc.setScreen(new FaceScreen(mc.screen));
        while(TOGGLE.consumeClick()) {
            if(sharing)stop();else if(connected)start(true);else status="Join a world before sharing";
            if(mc.player!=null)mc.player.displayClientMessage(Component.literal(status),true);
        }
        if(camera!=null) {
            status=camera.status;
            if(!camera.alive()){String error=status;stop();status=error;}
            else if(sharing&&connected) {
                var preview=camera.latest.get();
                if(preview!=null&&preview.frame().id()!=lastFrame) {
                    long now=System.nanoTime();FaceFrame f=preview.frame();long interval=Math.max(1_000_000_000L/Math.min(settings.fps,maxFps),(long)(f.wireBytes()*1e9/maxBandwidth));
                    if(now-lastSent>=interval&&mc.getConnection().getConnection().channel().isWritable()){PacketDistributor.sendToServer(new FaceNetwork.Up(false,f));lastFrame=f.id();lastSent=now;}
                }
            }
        }
        streams.tick();
    }
    @SubscribeEvent public void hud(RenderGuiEvent.Post e) {
        if(sharing)e.getGuiGraphics().drawString(Minecraft.getInstance().font,"Aki's 3D FaceCam Mod · SHARING · "+TOGGLE.getTranslatedKeyMessage().getString()+" stop",6,6,0xff80ef9f,true);
    }
}
