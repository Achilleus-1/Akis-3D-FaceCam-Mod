package dev.facecam.client.render;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import java.util.LinkedHashMap;

/** Bounded render-thread skin inspection; transparent hat fronts need no depth cap. */
public final class HeadCovering {
    private record Entry(int textureId,long checked,boolean opaque){}
    private static final LinkedHashMap<ResourceLocation,Entry> CACHE=new LinkedHashMap<>(64,.75f,true);
    public static boolean opaqueFront(ResourceLocation skin) {
        RenderSystem.assertOnRenderThread();
        int id=Minecraft.getInstance().getTextureManager().getTexture(skin).getId();long now=System.nanoTime();
        Entry cached=CACHE.get(skin);
        if(cached!=null&&cached.textureId==id&&now-cached.checked<5_000_000_000L)return cached.opaque;
        boolean opaque=true;
        int active=GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        RenderSystem.activeTexture(GL13.GL_TEXTURE0);
        int bound=GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        try {
            RenderSystem.bindTexture(id);
            int width=GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D,0,GL11.GL_TEXTURE_WIDTH);
            int height=GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D,0,GL11.GL_TEXTURE_HEIGHT);
            if(width>=64&&width<=1024&&(height==width||height==width/2)) {
                try(var image=new NativeImage(width,height,false)) {
                    image.downloadTexture(0,false);opaque=false;
                    int scale=width/64;
                    outer:for(int y=8*scale;y<16*scale;y++)for(int x=40*scale;x<48*scale;x++)
                        if((image.getPixelRGBA(x,y)>>>24)>25){opaque=true;break outer;}
                }
            }
        }catch(RuntimeException ignored){opaque=true;}
        finally {RenderSystem.bindTexture(bound);RenderSystem.activeTexture(active);}
        CACHE.put(skin,new Entry(id,now,opaque));
        while(CACHE.size()>64)CACHE.remove(CACHE.keySet().iterator().next());
        return opaque;
    }
    private HeadCovering(){}
}
