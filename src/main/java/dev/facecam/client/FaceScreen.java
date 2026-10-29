package dev.facecam.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import java.util.List;
import dev.facecam.core.FaceAppearance;

public final class FaceScreen extends Screen {
    private final Screen parent;
    private DynamicTexture preview;
    private ResourceLocation previewId;
    private long previewFrame=-1;
    private int previewSize,left,right;
    public FaceScreen(Screen parent){super(Component.literal("Aki's 3D FaceCam Mod"));this.parent=parent;}
    private Button button(int x,int y,int w,String text,Runnable action){Button b=Button.builder(Component.literal(text),p->action.run()).bounds(x,y,w,20).build();addRenderableWidget(b);return b;}
    @Override protected void init() {
        int w=Math.min(410,width-20);left=(width-w)/2;right=left+130;int controls=w-130;
        button(left,36,w/2-3,"Camera "+FaceClient.settings.camera+" (click to select)",()->{FaceClient.stop();FaceClient.settings.camera=(FaceClient.settings.camera+1)%8;FaceClient.settings.save();rebuildWidgets();});
        button(left+w/2+3,36,w/2-3,"Video: "+FaceClient.settings.resolution+" × "+FaceClient.settings.resolution,()->{int n=FaceClient.settings.resolution;FaceClient.settings.resolution=n==64?128:n==128?192:64;FaceClient.settings.save();rebuildWidgets();});
        button(right,60,controls/2-2,"Local preview",()->FaceClient.start(false));
        button(right+controls/2+2,60,controls/2-2,"Start sharing",()->{if(minecraft.getConnection()!=null)FaceClient.start(true);else FaceClient.status="Join a world before sharing";});
        button(right,82,controls,"Stop and release camera",FaceClient::stop);
        addRenderableWidget(new SettingSlider(right,104,controls,true));
        addRenderableWidget(new SizeSlider(right,126,controls-74));
        button(right+controls-70,126,70,"Reset size",()->{FaceClient.settings.resetSize();rebuildWidgets();});
        addRenderableWidget(new SettingSlider(right,148,controls,false));
        button(right,170,controls/2-2,"FPS: "+FaceClient.settings.fps,()->{int n=FaceClient.settings.fps;FaceClient.settings.fps=n==5?10:n==10?15:n==15?20:n==20?30:5;FaceClient.settings.save();rebuildWidgets();});
        button(right+controls/2+2,170,controls/2-2,FaceClient.settings.hideReceived?"Faces: hidden":"Faces: visible",()->{FaceClient.settings.hideReceived=!FaceClient.settings.hideReceived;FaceClient.settings.save();FaceClient.streams.clear();rebuildWidgets();});
        button(width/2-60,height-26,120,"Done",this::onClose);
    }
    private static final class SettingSlider extends AbstractSliderButton {
        private final boolean depth;
        SettingSlider(int x,int y,int w,boolean depth){super(x,y,w,20,Component.empty(),depth?FaceClient.settings.depth/FaceAppearance.MAX_DEPTH:FaceClient.settings.smoothing/.85);this.depth=depth;updateMessage();}
        protected void updateMessage(){setMessage(Component.literal(depth?String.format(java.util.Locale.ROOT,"Face depth: %.2f units",value*FaceAppearance.MAX_DEPTH):String.format(java.util.Locale.ROOT,"Smoothing: %.0f%%",value*85)));}
        protected void applyValue(){if(depth)FaceClient.settings.depth=(float)value*FaceAppearance.MAX_DEPTH;else FaceClient.settings.smoothing=(float)value*.85f;FaceClient.settings.save();}
    }
    private static final class SizeSlider extends AbstractSliderButton {
        SizeSlider(int x,int y,int w){super(x,y,w,20,Component.empty(),(FaceClient.settings.faceSize-FaceAppearance.MIN_SIZE)/(FaceAppearance.MAX_SIZE-FaceAppearance.MIN_SIZE));updateMessage();}
        protected void updateMessage(){setMessage(Component.literal("Face size: "+Math.round((FaceAppearance.MIN_SIZE+value*(FaceAppearance.MAX_SIZE-FaceAppearance.MIN_SIZE))*100)+"%"));}
        protected void applyValue(){FaceClient.settings.faceSize=Math.round((FaceAppearance.MIN_SIZE+(float)value*(FaceAppearance.MAX_SIZE-FaceAppearance.MIN_SIZE))*100)/100f;FaceClient.settings.save();}
    }
    @Override public void tick() {
        var camera=FaceClient.camera;if(camera==null)return;var latest=camera.latest.get();if(latest==null||latest.frame().id()==previewFrame)return;
        int n=latest.image().getWidth();
        if(preview==null||previewSize!=n){releasePreview();preview=new DynamicTexture(n,n,true);previewId=ResourceLocation.fromNamespaceAndPath("facecam","preview");minecraft.getTextureManager().register(previewId,preview);previewSize=n;}
        var image=preview.getPixels();float[] mask=RemoteStreams.mask(n);
        if(image!=null){for(int y=0;y<n;y++)for(int x=0;x<n;x++){int rgb=latest.image().getRGB(x,y);image.setPixelRGBA(x,y,((int)(mask[y*n+x]*255)<<24)|((rgb&255)<<16)|(rgb&0xff00)|((rgb>>16)&255));}preview.upload();}
        previewFrame=latest.frame().id();
    }
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partial) {
        // NeoForge 21.1.259's Screen.render already draws and blurs the background.
        super.render(g,mouseX,mouseY,partial);
        g.drawCenteredString(font,getTitle(),width/2,8,0xffffff);
        g.drawCenteredString(font,FaceClient.CONFIG.getTranslatedKeyMessage().getString()+" settings · "+FaceClient.TOGGLE.getTranslatedKeyMessage().getString()+" sharing",width/2,22,0xc0c0c0);
        g.fill(left,64,left+120,184,0xff20272d);
        boolean fresh=FaceClient.camera!=null&&FaceClient.camera.latest.get()!=null;
        if(previewId!=null&&fresh)g.blit(previewId,left+12,76,0,0,96,96,96,96);
        else g.drawCenteredString(font,"Camera off / no face",left+60,114,0xc0c0c0);
        g.drawString(font,FaceClient.sharing?"SHARING TO SERVER":"LOCAL / CAMERA OFF",left,188,FaceClient.sharing?0x80ef9f:0xffffff,false);
        List<net.minecraft.util.FormattedCharSequence> lines=font.split(Component.literal(FaceClient.status),width-24);
        // Status is visible even with small GUI sizes; longer errors get a hover tooltip.
        if(!lines.isEmpty())g.drawString(font,lines.getFirst(),12,height-39,0xe5c579,false);
        if(mouseY>=height-44&&mouseY<height-27)g.renderTooltip(font,font.split(Component.literal(FaceClient.status),Math.min(360,width-30)),mouseX,mouseY);
    }
    private void releasePreview(){if(previewId!=null)minecraft.getTextureManager().release(previewId);preview=null;previewId=null;previewFrame=-1;}
    @Override public void removed(){releasePreview();if(!FaceClient.sharing)FaceClient.stop();}
    @Override public void onClose(){minecraft.setScreen(parent);}
    @Override public boolean isPauseScreen(){return false;}
}
