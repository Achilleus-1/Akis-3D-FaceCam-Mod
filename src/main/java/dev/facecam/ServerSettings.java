package dev.facecam;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ServerSettings {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue DISTANCE, FPS, FRAME_BYTES, BYTES_SECOND, VIEWERS;
    static {
        var b = new ModConfigSpec.Builder();
        DISTANCE = b.comment("Only relay to players tracking the sender within this distance (blocks).").defineInRange("sharingDistance", 48, 8, 192);
        FPS = b.defineInRange("maxFramesPerSecond", 15, 1, 30);
        FRAME_BYTES = b.comment("Entire packet remains below the 32 KiB serverbound limit.").defineInRange("maxImageBytes", 23000, 1024, 23000);
        BYTES_SECOND = b.defineInRange("maxBytesPerSecondPerSender", 180000, 4096, 1000000);
        VIEWERS = b.comment("Bounds relay fanout and server bandwidth.").defineInRange("maxViewersPerSender", 16, 1, 64);
        SPEC = b.build();
    }
    private ServerSettings() {}
}
