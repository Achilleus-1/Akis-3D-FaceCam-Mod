package dev.facecam.diagnostic;

import dev.facecam.client.*;
import dev.facecam.core.*;
import dev.facecam.network.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import net.minecraft.world.entity.player.PlayerModelPart;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.DynamicTexture;

/** Opt-in development harness. Synthetic transport/render verification; never enables a webcam. */
public final class ClientVerification {
    private final String role=System.getProperty("facecam.verify.role");
    private final FaceFrame fixture;
    private boolean connecting;
    private long start, ticks, received;
    private boolean paused;
    private final Set<String> shots=new HashSet<>();
    public ClientVerification() {
        try(var in=new DataInputStream(Files.newInputStream(Path.of(System.getProperty("facecam.verify.fixture"))))){
            int n=in.readInt(),length=in.readInt();byte[] jpeg=in.readNBytes(length);short[] xyz=new short[468*3];for(int i=0;i<xyz.length;i++)xyz[i]=in.readShort();fixture=new FaceFrame(0,n,jpeg,xyz);
        }catch(IOException e){throw new IllegalStateException("Prepare nativeSmoke fixture first",e);}
    }
    @SubscribeEvent public void tick(ClientTickEvent.Post e) {
        var mc=Minecraft.getInstance();ticks++;
        if(!connecting&&mc.getOverlay()==null&&ticks>80){
            connecting=true;
            ConnectScreen.startConnecting(mc.screen,mc,ServerAddress.parseString("127.0.0.1:25585"),new ServerData("FaceCam verification","127.0.0.1:25585",ServerData.Type.OTHER),false,null);
        }
        if(mc.player==null||mc.level==null)return;
        if(mc.player.isDeadOrDying()){mc.player.respawn();mc.setScreen(null);return;}
        if(start==0)start=System.nanoTime();
        mc.options.pauseOnLostFocus=false;mc.options.renderDistance().set(4);mc.options.hideGui=true;
        Path control=mc.gameDirectory.toPath().resolve("verification-control.txt");
        if(Files.exists(control))try {
            String command=Files.readString(control).trim();Files.delete(control);
            switch(command) {
                case "pause" -> {paused=true;PacketDistributor.sendToServer(new FaceNetwork.Up(true,null));}
                case "resume" -> paused=false;
                case "hat-off" -> mc.options.toggleModelPart(PlayerModelPart.HAT,false);
                case "hat-on" -> mc.options.toggleModelPart(PlayerModelPart.HAT,true);
                case "settings" -> mc.setScreen(new FaceScreen(null));
                case "close-settings" -> mc.setScreen(null);
                case "preview" -> FaceClient.start(false);
                case "stop-camera" -> FaceClient.stop();
                case "size-small" -> FaceClient.settings.faceSize=.1f;
                case "size-large" -> FaceClient.settings.faceSize=4f;
                case "size-reset" -> FaceClient.settings.resetSize();
                case "hide-received" -> {FaceClient.settings.hideReceived=true;FaceClient.streams.clear();}
                case "show-received" -> FaceClient.settings.hideReceived=false;
                case "wide", "slim" -> {
                    for(var player:mc.level.players())if(!player.getUUID().equals(mc.player.getUUID())) {
                        var info=mc.getConnection().getPlayerInfo(player.getUUID());
                        if(info==null)continue;var skin=info.getSkin();
                        var field=info.getClass().getDeclaredField("skinLookup");field.setAccessible(true);
                        var model=command.equals("wide")?net.minecraft.client.resources.PlayerSkin.Model.WIDE:net.minecraft.client.resources.PlayerSkin.Model.SLIM;
                        var replacement=new net.minecraft.client.resources.PlayerSkin(skin.texture(),skin.textureUrl(),skin.capeTexture(),skin.elytraTexture(),model,skin.secure());
                        field.set(info,(java.util.function.Supplier<net.minecraft.client.resources.PlayerSkin>)()->replacement);
                    }
                }
                case "mask-skin", "clear-skin" -> {
                    for(var player:mc.level.players())if(!player.getUUID().equals(mc.player.getUUID())) {
                        var skin=player.getSkin().texture();
                        NativeImage image;
                        try(var input=mc.getResourceManager().getResource(skin).orElseThrow().open()){image=NativeImage.read(input);}
                        for(int y=8;y<16;y++)for(int x=40;x<48;x++)image.setPixelRGBA(x,y,command.equals("mask-skin")&&x<44?0xffff4000:0);
                        mc.getTextureManager().register(skin,new DynamicTexture(image));
                    }
                }
            }
        }catch(Exception ex){ex.printStackTrace();}
        if(role.equals("sender")) {
            // Send real server payloads at 10 FPS. Expression displacement is deliberately synthetic.
            if(ticks%2==0&&!paused) {
                short[] geometry=fixture.geometry().clone();
                float expression=(float)((Math.sin(ticks*.07)+1)*.12);
                for(int i:new int[]{14,17,84,85,86,87,88,178,317,318,402})geometry[i*3+1]-=(short)(expression*4096);
                PacketDistributor.sendToServer(new FaceNetwork.Up(false,new FaceFrame(System.currentTimeMillis(),fixture.resolution(),fixture.image(),geometry,FaceClient.settings.faceSize)));
            }
        } else {
            for(var player:mc.level.players())if(!player.getUUID().equals(mc.player.getUUID())) {
                var s=FaceClient.streams.get(player.getUUID());if(s!=null&&s.ready())received++;
            }
        }
        // Console can request a named screenshot by creating this file; all framebuffer work stays on the render thread.
        Path request=mc.gameDirectory.toPath().resolve("verification-shot.txt");
        if(Files.exists(request))try{
            String name=Files.readString(request).trim();Files.delete(request);
            if(shots.add(name))Screenshot.grab(mc.gameDirectory,"facecam-"+name+".png",mc.getMainRenderTarget(),msg->{});
            String model="";for(var player:mc.level.players())if(!player.getUUID().equals(mc.player.getUUID()))model=player.getSkin().model().toString();
            String appearance="";for(var player:mc.level.players())if(!player.getUUID().equals(mc.player.getUUID())){var stream=FaceClient.streams.get(player.getUUID());if(stream!=null&&stream.ready())appearance="remoteSize="+stream.size(System.nanoTime())+"\nnoseDepth="+stream.coordinate(1,2,System.nanoTime())+"\n";}
            Files.writeString(mc.gameDirectory.toPath().resolve("verification-metrics.txt"),"role="+role+"\nreceivedReadyTicks="+received+"\nplayers="+mc.level.players().size()+"\nremoteModel="+model+"\nconfigKey="+FaceClient.CONFIG.getTranslatedKeyMessage().getString()+"\nsharingKey="+FaceClient.TOGGLE.getTranslatedKeyMessage().getString()+"\n"+appearance+FaceClient.streams.stats());
        }catch(IOException ex){ex.printStackTrace();}
        Path stop=mc.gameDirectory.toPath().resolve("verification-stop.txt");
        if(Files.exists(stop))try{
            Files.delete(stop);PacketDistributor.sendToServer(new FaceNetwork.Up(true,null));
            Files.writeString(mc.gameDirectory.toPath().resolve("verification-metrics.txt"),"role="+role+"\nreceivedReadyTicks="+received+"\nplayers="+mc.level.players().size()+"\n");
            mc.stop();
        }catch(IOException ex){ex.printStackTrace();}
    }
}
