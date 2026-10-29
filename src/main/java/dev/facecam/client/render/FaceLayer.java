package dev.facecam.client.render;

import dev.facecam.client.*;
import dev.facecam.core.Topology;
import dev.facecam.core.FaceAppearance;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.PlayerModelPart;
import org.joml.Vector3f;

public final class FaceLayer extends RenderLayer<AbstractClientPlayer,PlayerModel<AbstractClientPlayer>> {
    private final Vector3f[] vertices=new Vector3f[468],normals=new Vector3f[468];
    private final Vector3f edgeA=new Vector3f(),edgeB=new Vector3f();
    public FaceLayer(RenderLayerParent<AbstractClientPlayer,PlayerModel<AbstractClientPlayer>> parent){super(parent);for(int i=0;i<468;i++){vertices[i]=new Vector3f();normals[i]=new Vector3f();}}
    @Override public void render(PoseStack poses,MultiBufferSource buffers,int light,AbstractClientPlayer player,float limbSwing,float limbAmount,float partialTick,float age,float yaw,float pitch) {
        if(player.isInvisible()||!getParentModel().head.visible)return;
        if(FaceClient.settings.hideReceived && !player.isLocalPlayer())return;
        var stream=FaceClient.streams.get(player.getUUID());long now=System.nanoTime();
        if(stream==null||!stream.ready())return;float fade=stream.alpha(now);if(fade<=0)return;
        // Outer skin is inflated 0.5 model units; vanilla helmet is inflated 1.0.
        // Keep EVERY vertex below the shell, including under transparent skin pixels.
        // This prevents a nose poking through from any viewing angle; depth test handles its opaque texels.
        boolean covered=!player.getItemBySlot(EquipmentSlot.HEAD).isEmpty()
            ||(player.isModelPartShown(PlayerModelPart.HAT)&&HeadCovering.opaqueFront(player.getSkin().texture()));
        float cap=covered?.40f:FaceAppearance.MAX_DEPTH*FaceAppearance.MAX_SIZE;
        float size=stream.size(now);
        poses.pushPose();getParentModel().head.translateAndRotate(poses);
        var pose=poses.last();VertexConsumer out=buffers.getBuffer(FaceRenderType.face(stream.texture()));
        var topology=Topology.INSTANCE;
        for(int i=0;i<468;i++){vertices[i].set(FaceAppearance.x(stream.coordinate(i,0,now),size)/16,FaceAppearance.y(stream.coordinate(i,1,now),size)/16,(-4.015f-Math.min(cap,FaceAppearance.depth(stream.coordinate(i,2,now),size)))/16);normals[i].zero();}
        for(int[] triangle:topology.triangles){Vector3f a=vertices[triangle[0]],b=vertices[triangle[1]],c=vertices[triangle[2]];Vector3f normal=edgeA.set(b).sub(a).cross(edgeB.set(c).sub(a));if(normal.z>0)normal.negate();for(int i:triangle)normals[i].add(normal);}
        for(Vector3f n:normals){if(n.lengthSquared()<1e-12)n.set(0,0,-1);else n.normalize();}
        for(int[] triangle:topology.triangles)for(int i:triangle) {
            var v=vertices[i];var n=normals[i];float[] uv=topology.uv[i];
            out.addVertex(pose,v.x,v.y,v.z).setColor(1f,1f,1f,fade).setUv(uv[0],uv[1])
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose,n.x,n.y,n.z);
        }
        poses.popPose();
    }
}
