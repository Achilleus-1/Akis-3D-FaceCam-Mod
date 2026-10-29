package dev.facecam.network;

import dev.facecam.FaceCam;
import dev.facecam.core.FaceFrame;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import java.util.UUID;
import java.util.function.Consumer;

public final class FaceNetwork {
    // Client installs these consumers from its Dist.CLIENT entry point. Common code never links Minecraft client classes.
    public static Consumer<Down> receiver = p -> {};
    public static Consumer<Limits> limitsReceiver = p -> {};
    static FaceFrame read(RegistryFriendlyByteBuf b) {
        long id=b.readVarLong(); int n=b.readUnsignedByte();float size=b.readUnsignedShort()/100f;
        byte[] jpeg=b.readByteArray(FaceFrame.MAX_IMAGE);
        short[] xyz=new short[FaceFrame.VERTICES*3];
        for(int i=0;i<xyz.length;i++) xyz[i]=b.readShort();
        FaceFrame f=new FaceFrame(id,n,jpeg,xyz,size);
        if(!f.valid()) throw new IllegalArgumentException("Invalid FaceCam frame");
        return f;
    }
    static void write(RegistryFriendlyByteBuf b, FaceFrame f) {
        b.writeVarLong(f.id()); b.writeByte(f.resolution());b.writeShort(Math.round(f.size()*100)); b.writeByteArray(f.image());
        for(short s:f.geometry()) b.writeShort(s);
    }
    public record Up(boolean stopped, FaceFrame frame) implements CustomPacketPayload {
        public static final Type<Up> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(FaceCam.ID,"up"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Up> CODEC=StreamCodec.of((b,p)->{b.writeBoolean(p.stopped); if(!p.stopped)write(b,p.frame);}, b->{boolean s=b.readBoolean();return new Up(s,s?null:read(b));});
        public Type<Up> type(){return TYPE;}
    }
    public record Down(UUID player, boolean stopped, FaceFrame frame) implements CustomPacketPayload {
        public static final Type<Down> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(FaceCam.ID,"down"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Down> CODEC=StreamCodec.of((b,p)->{b.writeUUID(p.player);b.writeBoolean(p.stopped);if(!p.stopped)write(b,p.frame);},b->{UUID u=b.readUUID();boolean s=b.readBoolean();return new Down(u,s,s?null:read(b));});
        public Type<Down> type(){return TYPE;}
    }
    public record Limits(int fps,int imageBytes,int bytesSecond) implements CustomPacketPayload {
        public static final Type<Limits> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(FaceCam.ID,"limits"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Limits> CODEC=StreamCodec.of((b,p)->{b.writeVarInt(p.fps);b.writeVarInt(p.imageBytes);b.writeVarInt(p.bytesSecond);},b->new Limits(b.readVarInt(),b.readVarInt(),b.readVarInt()));
        public Type<Limits> type(){return TYPE;}
    }
    public static void register(RegisterPayloadHandlersEvent e) {
        var r=e.registrar("2");
        r.playToServer(Up.TYPE,Up.CODEC,ServerRelay::receive);
        r.playToClient(Down.TYPE,Down.CODEC,(p,c)->receiver.accept(p));
        r.playToClient(Limits.TYPE,Limits.CODEC,(p,c)->limitsReceiver.accept(p));
    }
}
