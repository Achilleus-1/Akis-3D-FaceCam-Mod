package dev.facecam.core;

/** Weighted rigid similarity fit (Horn quaternion): remove pose, preserve nonrigid expression residuals. */
public final class PoseStabilizer {
    private static final int[] ANCHORS={6,10,33,133,168,197,234,263,362,454,127,356};
    public static float[][] align(float[][] measured) {
        float[][] canonical=Topology.INSTANCE.canonical;
        double[] a=new double[3],b=new double[3];
        for(int i:ANCHORS)for(int k=0;k<3;k++){a[k]+=measured[i][k]/ANCHORS.length;b[k]+=canonical[i][k]/ANCHORS.length;}
        double[][] s=new double[3][3]; double aa=0,bb=0;
        for(int i:ANCHORS)for(int j=0;j<3;j++) {
            double x=measured[i][j]-a[j]; aa+=x*x;
            double y=canonical[i][j]-b[j];bb+=y*y;
            for(int k=0;k<3;k++)s[j][k]+=x*(canonical[i][k]-b[k]);
        }
        if(aa<1e-8)throw new IllegalArgumentException("Degenerate face");
        double trace=s[0][0]+s[1][1]+s[2][2];
        double[][] n={
            {trace,s[1][2]-s[2][1],s[2][0]-s[0][2],s[0][1]-s[1][0]},
            {s[1][2]-s[2][1],s[0][0]-s[1][1]-s[2][2],s[0][1]+s[1][0],s[2][0]+s[0][2]},
            {s[2][0]-s[0][2],s[0][1]+s[1][0],-s[0][0]+s[1][1]-s[2][2],s[1][2]+s[2][1]},
            {s[0][1]-s[1][0],s[2][0]+s[0][2],s[1][2]+s[2][1],-s[0][0]-s[1][1]+s[2][2]}};
        double shift=0;for(double[] row:n)for(double x:row)shift+=Math.abs(x);
        double[] q={1,.001,.002,.003};
        for(int iter=0;iter<80;iter++) {
            double[] next=new double[4]; double length=0;
            for(int j=0;j<4;j++){next[j]=shift*q[j];for(int k=0;k<4;k++)next[j]+=n[j][k]*q[k];length+=next[j]*next[j];}
            length=Math.sqrt(length);for(int j=0;j<4;j++)q[j]=next[j]/length;
        }
        double w=q[0],x=q[1],y=q[2],z=q[3];
        double[][] r={{1-2*(y*y+z*z),2*(x*y-z*w),2*(x*z+y*w)}, {2*(x*y+z*w),1-2*(x*x+z*z),2*(y*z-x*w)}, {2*(x*z-y*w),2*(y*z+x*w),1-2*(x*x+y*y)}};
        double scale=Math.sqrt(bb/aa); float[][] out=new float[468][3];
        for(int i=0;i<468;i++)for(int j=0;j<3;j++){
            double v=b[j];for(int k=0;k<3;k++)v+=r[j][k]*(measured[i][k]-a[k])*scale;out[i][j]=(float)v;
        }
        return out;
    }
    private float[][] previous;
    public short[] geometry(float[][] canonicalPose,float depth,float smoothing) {
        float[][] c=Topology.INSTANCE.canonical;
        float minY=Float.MAX_VALUE,maxY=-Float.MAX_VALUE;
        for(float[] v:c){minY=Math.min(minY,v[1]);maxY=Math.max(maxY,v[1]);}
        float scale=7.1f/(maxY-minY),center=(minY+maxY)/2;
        if(previous==null)previous=canonicalPose;
        float alpha=Math.max(.15f,1-smoothing);
        short[] out=new short[468*3];
        for(int i=0;i<468;i++) {
            for(int k=0;k<3;k++)previous[i][k]+=alpha*(canonicalPose[i][k]-previous[i][k]);
            out[i*3]=(short)Math.round(clamp(previous[i][0]*scale,-3.7f,3.7f)*4096);
            out[i*3+1]=(short)Math.round(clamp(-4-(previous[i][1]-center)*scale,-7.6f,-.4f)*4096);
            float shape=clamp((previous[i][2]-1.4f)/6.2f,0,1);
            float edge=Topology.INSTANCE.edge[i]; edge=edge*edge*(3-2*edge);
            out[i*3+2]=(short)Math.round(clamp(shape*depth*edge,0,FaceAppearance.MAX_DEPTH)*4096);
        }
        return out;
    }
    private static float clamp(float x,float a,float b){return Math.max(a,Math.min(b,x));}
}
