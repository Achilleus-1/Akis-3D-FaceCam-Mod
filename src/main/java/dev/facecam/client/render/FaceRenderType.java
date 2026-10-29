package dev.facecam.client.render;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import java.util.*;

public final class FaceRenderType extends RenderType {
    private FaceRenderType(){super("facecam",DefaultVertexFormat.NEW_ENTITY,VertexFormat.Mode.TRIANGLES,65536,false,true,()->{},()->{});}
    // Bound by active textures; explicit removal accompanies stream teardown.
    private static final Map<ResourceLocation,RenderType> TYPES=new HashMap<>();
    public static RenderType face(ResourceLocation texture) {
        return TYPES.computeIfAbsent(texture,t->create("facecam_triangles",DefaultVertexFormat.NEW_ENTITY,VertexFormat.Mode.TRIANGLES,65536,false,false,
            CompositeState.builder().setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
                .setTextureState(new TextureStateShard(t,false,false)).setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                .setCullState(NO_CULL).setLightmapState(LIGHTMAP).setOverlayState(OVERLAY)
                .setDepthTestState(LEQUAL_DEPTH_TEST).setWriteMaskState(COLOR_DEPTH_WRITE).createCompositeState(true)));
    }
    public static void release(ResourceLocation texture){TYPES.remove(texture);}
}
