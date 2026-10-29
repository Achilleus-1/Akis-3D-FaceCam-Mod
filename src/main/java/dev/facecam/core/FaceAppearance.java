package dev.facecam.core;

/** Shared appearance bounds. Size scales the surface about the head-face center. */
public final class FaceAppearance {
    public static final float DEFAULT_SIZE=1f, MIN_SIZE=.1f, MAX_SIZE=4f;
    public static final float DEFAULT_DEPTH=1.75f, MAX_DEPTH=6f;
    public static boolean validSize(float size){return Float.isFinite(size)&&size>=MIN_SIZE&&size<=MAX_SIZE;}
    public static float x(float x,float size){return x*size;}
    public static float y(float y,float size){return -4+(y+4)*size;}
    public static float depth(float depth,float size){return depth*size;}
    private FaceAppearance(){}
}
