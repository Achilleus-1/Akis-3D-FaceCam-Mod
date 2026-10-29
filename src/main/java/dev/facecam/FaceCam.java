package dev.facecam;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import dev.facecam.network.FaceNetwork;
import dev.facecam.network.ServerRelay;

@Mod(FaceCam.ID)
public final class FaceCam {
    public static final String ID = "facecam";
    public FaceCam(IEventBus bus, ModContainer container) {
        container.registerConfig(ModConfig.Type.SERVER, ServerSettings.SPEC);
        bus.addListener(FaceNetwork::register);
        NeoForge.EVENT_BUS.register(new ServerRelay());
        if(Boolean.getBoolean("facecam.verify.server"))NeoForge.EVENT_BUS.register(new dev.facecam.diagnostic.ServerVerification());
    }
}
