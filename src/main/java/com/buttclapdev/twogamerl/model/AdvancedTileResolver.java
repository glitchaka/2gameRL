package com.buttclapdev.twogamerl.model;

import com.buttclapdev.twogamerl.model.GameProject.*;
import java.util.*;

/** Deterministic visual resolution for weighted variants and 4/8-way autotiles. */
public final class AdvancedTileResolver {
    private AdvancedTileResolver(){}
    public static String assetFor(GameProject project,Level level,TileLayer layer,int x,int y,TileDef tile){
        if(project==null||level==null||layer==null||tile==null)return"";
        if(tile.autotileMode!=TileAutotileMode.NONE&&!tile.autotileAssets.isEmpty()){int mask=mask(project,layer,x,y,tile);String exact=tile.autotileAssets.get(mask);if(exact==null&&tile.autotileMode==TileAutotileMode.EIGHT_WAY)exact=tile.autotileAssets.get(mask&15);if(exact!=null&&!exact.isBlank())return exact;}
        if(!tile.variants.isEmpty()){double total=tile.variants.stream().mapToDouble(v->Math.max(0,v.weight)).sum();if(total>0){long seed=1469598103934665603L;seed=(seed^tile.id)*1099511628211L;seed=(seed^x)*1099511628211L;seed=(seed^y)*1099511628211L;seed=(seed^layer.id.hashCode())*1099511628211L;double pick=(new SplittableRandom(seed).nextDouble())*total;for(TileVariant v:tile.variants){pick-=Math.max(0,v.weight);if(pick<=0&&!v.assetKey.isBlank())return v.assetKey;}}}
        return tile.assetKey==null?"":tile.assetKey;
    }
    public static int mask(GameProject project,TileLayer layer,int x,int y,TileDef tile){int m=0;if(connects(project,layer,x,y-1,tile))m|=1;if(connects(project,layer,x+1,y,tile))m|=2;if(connects(project,layer,x,y+1,tile))m|=4;if(connects(project,layer,x-1,y,tile))m|=8;if(tile.autotileMode==TileAutotileMode.EIGHT_WAY){if(connects(project,layer,x+1,y-1,tile))m|=16;if(connects(project,layer,x+1,y+1,tile))m|=32;if(connects(project,layer,x-1,y+1,tile))m|=64;if(connects(project,layer,x-1,y-1,tile))m|=128;}return m;}
    private static boolean connects(GameProject p,TileLayer l,int x,int y,TileDef t){int id=l.get(x,y);if(id<0)return false;if(id==t.id)return true;TileDef other=p.getTiles().get(id);return other!=null&&!t.connectTag.isBlank()&&other.tags.contains(t.connectTag);}
}
