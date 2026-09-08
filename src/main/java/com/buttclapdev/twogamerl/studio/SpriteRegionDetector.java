package com.buttclapdev.twogamerl.studio;

import java.awt.image.BufferedImage;
import java.util.*;

final class SpriteRegionDetector {
    record Box(int x,int y,int width,int height,int pixels) {
        int right(){return x+width-1;} int bottom(){return y+height-1;}
    }

    private SpriteRegionDetector() {}

    static List<Box> detect(BufferedImage image,int alphaThreshold,int colorTolerance,int minPixels,int mergeGap,int padding) {
        if(image==null)return List.of();
        int w=image.getWidth(),h=image.getHeight();
        boolean useAlpha=hasUsefulTransparency(image,alphaThreshold);
        int background=image.getRGB(0,0);
        boolean[] foreground=new boolean[w*h];
        for(int y=0;y<h;y++)for(int x=0;x<w;x++)foreground[y*w+x]=isForeground(image.getRGB(x,y),background,useAlpha,alphaThreshold,colorTolerance);
        boolean[] seen=new boolean[w*h];
        ArrayList<Box> boxes=new ArrayList<>();
        int[] qx=new int[w*h],qy=new int[w*h];
        for(int sy=0;sy<h;sy++)for(int sx=0;sx<w;sx++){
            int si=sy*w+sx;if(!foreground[si]||seen[si])continue;
            int head=0,tail=0,minX=sx,maxX=sx,minY=sy,maxY=sy,count=0;qx[tail]=sx;qy[tail++]=sy;seen[si]=true;
            while(head<tail){int x=qx[head],y=qy[head++];count++;minX=Math.min(minX,x);maxX=Math.max(maxX,x);minY=Math.min(minY,y);maxY=Math.max(maxY,y);
                for(int oy=-1;oy<=1;oy++)for(int ox=-1;ox<=1;ox++){if(ox==0&&oy==0)continue;int nx=x+ox,ny=y+oy;if(nx<0||ny<0||nx>=w||ny>=h)continue;int ni=ny*w+nx;if(foreground[ni]&&!seen[ni]){seen[ni]=true;qx[tail]=nx;qy[tail++]=ny;}}
            }
            if(count>=Math.max(1,minPixels))boxes.add(new Box(minX,minY,maxX-minX+1,maxY-minY+1,count));
        }
        boxes=merge(boxes,Math.max(0,mergeGap));
        ArrayList<Box> padded=new ArrayList<>();
        for(Box b:boxes){int x=Math.max(0,b.x-padding),y=Math.max(0,b.y-padding),r=Math.min(w-1,b.right()+padding),bot=Math.min(h-1,b.bottom()+padding);padded.add(new Box(x,y,r-x+1,bot-y+1,b.pixels));}
        padded.sort(Comparator.comparingInt(Box::y).thenComparingInt(Box::x));
        return padded;
    }

    private static boolean hasUsefulTransparency(BufferedImage image,int threshold){
        if(!image.getColorModel().hasAlpha())return false;int w=image.getWidth(),h=image.getHeight();
        for(int y=0;y<h;y+=Math.max(1,h/64))for(int x=0;x<w;x+=Math.max(1,w/64))if(((image.getRGB(x,y)>>>24)&255)<=threshold)return true;
        return false;
    }

    private static boolean isForeground(int argb,int bg,boolean alpha,int alphaThreshold,int tolerance){
        if(alpha)return ((argb>>>24)&255)>alphaThreshold;
        int r=(argb>>>16)&255,g=(argb>>>8)&255,b=argb&255,br=(bg>>>16)&255,bgG=(bg>>>8)&255,bb=bg&255;
        int dist=Math.max(Math.abs(r-br),Math.max(Math.abs(g-bgG),Math.abs(b-bb)));
        return dist>Math.max(0,tolerance);
    }

    private static ArrayList<Box> merge(List<Box> input,int gap){
        ArrayList<Box> boxes=new ArrayList<>(input);boolean changed=true;
        while(changed){changed=false;outer:for(int i=0;i<boxes.size();i++)for(int j=i+1;j<boxes.size();j++)if(near(boxes.get(i),boxes.get(j),gap)){Box a=boxes.get(i),b=boxes.remove(j);int x=Math.min(a.x,b.x),y=Math.min(a.y,b.y),r=Math.max(a.right(),b.right()),bot=Math.max(a.bottom(),b.bottom());boxes.set(i,new Box(x,y,r-x+1,bot-y+1,a.pixels+b.pixels));changed=true;break outer;}}
        return boxes;
    }

    private static boolean near(Box a,Box b,int gap){
        int dx=Math.max(0,Math.max(a.x-b.right()-1,b.x-a.right()-1));
        int dy=Math.max(0,Math.max(a.y-b.bottom()-1,b.y-a.bottom()-1));
        boolean projectedOverlapX=a.x<=b.right()+gap&&b.x<=a.right()+gap;
        boolean projectedOverlapY=a.y<=b.bottom()+gap&&b.y<=a.bottom()+gap;
        return (dx<=gap&&projectedOverlapY)||(dy<=gap&&projectedOverlapX)||(dx<=gap&&dy<=gap);
    }
}
