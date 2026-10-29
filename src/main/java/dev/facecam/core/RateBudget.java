package dev.facecam.core;

/** Separate token buckets: packet size is charged even when a frame is rejected. */
public final class RateBudget {
    private long last;
    private double bytes, frames;
    public RateBudget(long now, int byteRate, int frameRate) { last=now; bytes=byteRate; frames=frameRate; }
    public boolean accept(long now, int size, int byteRate, int frameRate) {
        double elapsed = Math.max(0, Math.min(1, (now-last)/1e9)); last=now;
        bytes=Math.min(byteRate, bytes+elapsed*byteRate);
        frames=Math.min(2, frames+elapsed*frameRate);
        boolean ok=bytes>=size && frames>=1;
        bytes=Math.max(0,bytes-size);
        if(ok) frames--;
        return ok;
    }
}
