package dev.facecam.network;

import dev.facecam.ServerSettings;
import dev.facecam.core.RateBudget;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import java.util.*;

public final class ServerRelay {
    private static final Map<UUID,State> STATES=new HashMap<>();
    private static final Map<UUID,Set<UUID>> WATCHERS=new HashMap<>();
    private static final class State {
        final RateBudget budget=new RateBudget(System.nanoTime(),ServerSettings.BYTES_SECOND.get(),ServerSettings.FPS.get());
        final Set<UUID> delivered=new HashSet<>();
        long id=-1, time; boolean active;
    }
    public static void receive(FaceNetwork.Up p, IPayloadContext c) {
        if(!(c.player() instanceof ServerPlayer sender)) return;
        UUID id=sender.getUUID();
        State s=STATES.computeIfAbsent(id,k->new State());
        if(p.stopped()) { stop(sender,s); return; }
        var f=p.frame(); long now=System.nanoTime();
        if(!f.valid() || f.id()<=s.id || f.image().length>ServerSettings.FRAME_BYTES.get() || !s.budget.accept(now,f.wireBytes(),ServerSettings.BYTES_SECOND.get(),ServerSettings.FPS.get())) return;
        s.id=f.id(); s.time=now; s.active=true;
        var eligible=eligible(sender);
        for(UUID old:new HashSet<>(s.delivered)) if(!eligible.contains(old)) sendStop(sender,old);
        var packet=new FaceNetwork.Down(id,false,f);
        for(UUID viewer:eligible) {
            ServerPlayer player=sender.server.getPlayerList().getPlayer(viewer);
            if(player!=null&&player.connection.getConnection().channel().isWritable()) PacketDistributor.sendToPlayer(player,packet);
        }
        s.delivered.clear(); s.delivered.addAll(eligible);
    }
    private static Set<UUID> eligible(ServerPlayer sender) {
        var out=new LinkedHashSet<UUID>(); out.add(sender.getUUID());
        int max=ServerSettings.VIEWERS.get(); double distance=ServerSettings.DISTANCE.get();
        WATCHERS.getOrDefault(sender.getUUID(),Set.of()).stream()
            .map(u->sender.server.getPlayerList().getPlayer(u)).filter(Objects::nonNull)
            .filter(p->p.serverLevel()==sender.serverLevel() && p.distanceToSqr(sender)<=distance*distance)
            .sorted(Comparator.comparingDouble(p->p.distanceToSqr(sender))).limit(max)
            .forEach(p->out.add(p.getUUID()));
        return out;
    }
    private static void sendStop(ServerPlayer sender,UUID viewer) {
        ServerPlayer p=sender.server.getPlayerList().getPlayer(viewer);
        if(p!=null) PacketDistributor.sendToPlayer(p,new FaceNetwork.Down(sender.getUUID(),true,null));
    }
    private static void stop(ServerPlayer sender,State s) {
        if(s.active) for(UUID viewer:s.delivered) sendStop(sender,viewer);
        s.active=false; s.delivered.clear();
    }
    @SubscribeEvent public void login(PlayerEvent.PlayerLoggedInEvent e) {
        if(e.getEntity() instanceof ServerPlayer p) PacketDistributor.sendToPlayer(p,new FaceNetwork.Limits(ServerSettings.FPS.get(),ServerSettings.FRAME_BYTES.get(),ServerSettings.BYTES_SECOND.get()));
    }
    @SubscribeEvent public void track(PlayerEvent.StartTracking e) {
        if(e.getEntity() instanceof ServerPlayer p && e.getTarget() instanceof ServerPlayer target) WATCHERS.computeIfAbsent(target.getUUID(),k->new HashSet<>()).add(p.getUUID());
    }
    @SubscribeEvent public void untrack(PlayerEvent.StopTracking e) {
        if(e.getEntity() instanceof ServerPlayer p && e.getTarget() instanceof ServerPlayer target) {
            WATCHERS.getOrDefault(target.getUUID(),new HashSet<>()).remove(p.getUUID());
            State s=STATES.get(target.getUUID());
            if(s!=null && s.delivered.remove(p.getUUID())) sendStop(target,p.getUUID());
        }
    }
    @SubscribeEvent public void logout(PlayerEvent.PlayerLoggedOutEvent e) {
        if(e.getEntity() instanceof ServerPlayer p) {
            State s=STATES.remove(p.getUUID()); if(s!=null)stop(p,s);
            WATCHERS.remove(p.getUUID()); WATCHERS.values().forEach(v->v.remove(p.getUUID()));
            STATES.values().forEach(v->v.delivered.remove(p.getUUID()));
        }
    }
    @SubscribeEvent public void tick(ServerTickEvent.Post e) {
        long now=System.nanoTime();
        for(var entry:STATES.entrySet()) {
            State s=entry.getValue(); ServerPlayer p=e.getServer().getPlayerList().getPlayer(entry.getKey());
            if(p!=null && s.active && now-s.time>1_500_000_000L) stop(p,s);
        }
    }
    @SubscribeEvent public void stopped(ServerStoppedEvent e) {STATES.clear(); WATCHERS.clear();}
}
