package dev.facecam.diagnostic;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import java.nio.file.*;
import java.io.IOException;

/** Development-only command mailbox, enabled by an explicit JVM property. */
public final class ServerVerification {
    private int ticks;
    @SubscribeEvent public void tick(ServerTickEvent.Post e) {
        if(++ticks%10!=0)return;
        Path commands=Path.of("verification-commands.txt");
        if(!Files.exists(commands))return;
        try {
            var lines=Files.readAllLines(commands);Files.delete(commands);
            for(String command:lines){if(command.isBlank())continue;if(command.equals("stop")){e.getServer().halt(false);return;}e.getServer().getCommands().performPrefixedCommand(e.getServer().createCommandSourceStack(),command);}
        }catch(IOException ex){ex.printStackTrace();}
    }
}
