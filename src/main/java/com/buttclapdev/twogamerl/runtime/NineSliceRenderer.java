package com.buttclapdev.twogamerl.runtime;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

/** Deterministic nine-slice renderer shared by runtime and Studio-facing UI helpers. */
public final class NineSliceRenderer {
    private NineSliceRenderer(){}

    public static void draw(GraphicsContext g,Image image,double sx,double sy,double sw,double sh,int left,int top,int right,int bottom,double dx,double dy,double dw,double dh){
        if(g==null||image==null||sw<=0||sh<=0||dw<=0||dh<=0)return;
        double sl=Math.max(0,Math.min(left,sw/2)),sr=Math.max(0,Math.min(right,sw-sl)),st=Math.max(0,Math.min(top,sh/2)),sb=Math.max(0,Math.min(bottom,sh-st));
        if(sl+sr<=0&&st+sb<=0){g.drawImage(image,sx,sy,sw,sh,dx,dy,dw,dh);return;}
        double dl=Math.min(sl,dw/2),dr=Math.min(sr,Math.max(0,dw-dl)),dt=Math.min(st,dh/2),db=Math.min(sb,Math.max(0,dh-dt));
        double[]xs={sx,sx+sl,sx+sw-sr,sx+sw},ys={sy,sy+st,sy+sh-sb,sy+sh};
        double[]xd={dx,dx+dl,dx+dw-dr,dx+dw},yd={dy,dy+dt,dy+dh-db,dy+dh};
        for(int row=0;row<3;row++)for(int col=0;col<3;col++){double srcW=xs[col+1]-xs[col],srcH=ys[row+1]-ys[row],dstW=xd[col+1]-xd[col],dstH=yd[row+1]-yd[row];if(srcW>0&&srcH>0&&dstW>0&&dstH>0)g.drawImage(image,xs[col],ys[row],srcW,srcH,xd[col],yd[row],dstW,dstH);}
    }
}
