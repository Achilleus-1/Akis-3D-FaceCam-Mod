package dev.facecam.core;

/** One atomic image/geometry update. Wire geometry is 468 xyz shorts, model-unit / 4096. */
public record FaceFrame(long id, int resolution, byte[] image, short[] geometry, float size) {
    public FaceFrame(long id,int resolution,byte[] image,short[] geometry){this(id,resolution,image,geometry,FaceAppearance.DEFAULT_SIZE);}
    public static final int VERTICES = 468;
    public static final int MAX_IMAGE = 23000;
    public static boolean resolutionValid(int n) { return n == 64 || n == 128 || n == 192; }
    public boolean valid() {
        if (id < 0 || !resolutionValid(resolution) || !FaceAppearance.validSize(size) || image.length < 8 || image.length > MAX_IMAGE || geometry.length != VERTICES * 3) return false;
        for (int i = 0; i < VERTICES; i++) {
            if (Math.abs((int)geometry[i*3]) > 15565 || geometry[i*3+1] < -31130 || geometry[i*3+1] > -1024 || geometry[i*3+2] < 0 || geometry[i*3+2] > 24576) return false;
        }
        return true;
    }
    public float coordinate(int i, int axis) { return geometry[i*3+axis] / 4096f; }
    public int wireBytes() { return image.length + geometry.length * 2 + 34; }
}
