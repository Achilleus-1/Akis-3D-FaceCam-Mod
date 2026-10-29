package dev.facecam.client;

import dev.facecam.core.*;
import dev.facecam.network.FaceNetwork;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import javax.imageio.*;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public final class RemoteStreams {
    public static final int MAX_STREAMS=64;
    private final Map<UUID,Slot> streams=new LinkedHashMap<>();
    private final ThreadPoolExecutor decoder=new ThreadPoolExecutor(1,1,0,TimeUnit.SECONDS,new ArrayBlockingQueue<>(MAX_STREAMS),r->{var t=new Thread(r,"FaceCam-decode");t.setDaemon(true);return t;},new ThreadPoolExecutor.AbortPolicy());
    public static final class Slot {
        final AtomicReference<FaceFrame> pending=new AtomicReference<>();
        final AtomicReference<Decoded> decoded=new AtomicReference<>();
        final AtomicBoolean busy=new AtomicBoolean();
        volatile boolean live=true;
        long lastId=-1, seen=System.nanoTime(), displayed;
        FaceFrame current,previous;
        DynamicTexture texture;
        ResourceLocation location;
        int size;
        public float alpha(long now){return Math.max(0,Math.min(1,(1_300_000_000L-(now-seen))/850_000_000f));}
        public float coordinate(int i,int k,long now){float next=current.coordinate(i,k);if(previous==null)return next;float a=Math.min(1,(now-displayed)/65_000_000f);return previous.coordinate(i,k)*(1-a)+next*a;}
        public ResourceLocation texture(){return location;}
        public float size(long now){float next=current.size();if(previous==null)return next;float a=Math.min(1,(now-displayed)/65_000_000f);return previous.size()*(1-a)+next*a;}
        public boolean ready(){return texture!=null&&current!=null;}
    }
    private record Decoded(FaceFrame frame,int[] pixels){}
    public void receive(FaceNetwork.Down p) {
        if(p.stopped()){remove(p.player());return;}
        if(FaceClient.settings.hideReceived && (Minecraft.getInstance().player==null||!p.player().equals(Minecraft.getInstance().player.getUUID())))return;
        Slot slot=streams.get(p.player());
        if(slot==null){if(streams.size()>=MAX_STREAMS)return;slot=new Slot();streams.put(p.player(),slot);}
        if(p.frame().id()<=slot.lastId)return;
        slot.lastId=p.frame().id();slot.pending.set(p.frame());schedule(slot);
    }
    private void schedule(Slot s) {
        if(!s.busy.compareAndSet(false,true))return;
        try {decoder.execute(()->{
            try {
                FaceFrame frame=s.pending.getAndSet(null);
                if(frame!=null&&s.live){Decoded d=new Decoded(frame,FaceImageCodec.decode(frame));if(s.live)s.decoded.set(d);}
            }catch(Exception ignored){}
            finally{s.busy.set(false);if(s.live&&s.pending.get()!=null)schedule(s);}
        });}catch(RejectedExecutionException e){s.busy.set(false);}
    }
    public static float[] mask(int n){return FaceImageCodec.mask(n);}
    public void tick() {
        long now=System.nanoTime();var mc=Minecraft.getInstance();
        for(UUID id:new ArrayList<>(streams.keySet())) {
            Slot s=streams.get(id);
            if(now-s.seen>1_500_000_000L||mc.level==null){remove(id);continue;}
            Decoded d=s.decoded.getAndSet(null);if(d==null)continue;
            int n=d.frame.resolution();
            if(s.texture==null||s.size!=n){release(s);s.texture=new DynamicTexture(n,n,true);s.location=ResourceLocation.fromNamespaceAndPath("facecam","streams/"+id);mc.getTextureManager().register(s.location,s.texture);s.size=n;}
            NativeImage image=s.texture.getPixels();if(image==null)continue;
            for(int y=0;y<n;y++)for(int x=0;x<n;x++)image.setPixelRGBA(x,y,d.pixels[y*n+x]);
            s.texture.upload();s.previous=s.current;s.current=d.frame;s.displayed=now;s.seen=now;
        }
    }
    public Slot get(UUID id){return streams.get(id);}
    public String stats(){int textures=0,pending=0;for(Slot s:streams.values()){if(s.texture!=null)textures++;if(s.pending.get()!=null)pending++;}return "streams="+streams.size()+"\ntextures="+textures+"\npendingFrames="+pending+"\ndecoderQueue="+decoder.getQueue().size()+"\n";}
    private void release(Slot s){if(s.location!=null){dev.facecam.client.render.FaceRenderType.release(s.location);Minecraft.getInstance().getTextureManager().release(s.location);}s.texture=null;s.location=null;}
    public void remove(UUID id){Slot s=streams.remove(id);if(s!=null){s.live=false;s.pending.set(null);s.decoded.set(null);release(s);}}
    public void clear(){for(UUID id:new ArrayList<>(streams.keySet()))remove(id);decoder.getQueue().clear();}
}
