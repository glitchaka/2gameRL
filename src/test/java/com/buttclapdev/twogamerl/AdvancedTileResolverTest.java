package com.buttclapdev.twogamerl;

import com.buttclapdev.twogamerl.model.AdvancedTileResolver;
import com.buttclapdev.twogamerl.model.GameProject;
import com.buttclapdev.twogamerl.model.GameProject.*;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class AdvancedTileResolverTest {
    @Test void fourWayAndEightWayMasksAreExact(){
        GameProject p=GameProject.createDefault();Level l=p.getLevels().get("level-1");l.resize(5,5);TileLayer layer=l.baseLayer();TileDef t=p.getTiles().get(0);t.autotileMode=TileAutotileMode.EIGHT_WAY;
        layer.set(2,2,0);layer.set(2,1,0);layer.set(3,2,0);layer.set(2,3,0);layer.set(1,2,0);layer.set(3,1,0);layer.set(3,3,0);layer.set(1,3,0);layer.set(1,1,0);
        assertEquals(255,AdvancedTileResolver.mask(p,layer,2,2,t));
        t.autotileMode=TileAutotileMode.FOUR_WAY;assertEquals(15,AdvancedTileResolver.mask(p,layer,2,2,t));
    }

    @Test void connectTagJoinsDifferentTileIds(){
        GameProject p=GameProject.createDefault();Level l=p.getLevels().get("level-1");l.resize(3,3);TileLayer layer=l.baseLayer();TileDef a=p.getTiles().get(0),b=p.getTiles().get(1);a.connectTag="terrain";b.tags.add("terrain");a.autotileMode=TileAutotileMode.FOUR_WAY;layer.set(1,1,a.id);layer.set(1,0,b.id);assertEquals(1,AdvancedTileResolver.mask(p,layer,1,1,a));
    }

    @Test void weightedVariantIsDeterministicPerCellAndUsesConfiguredAssets(){
        GameProject p=GameProject.createDefault();Level l=p.getLevels().get("level-1");TileLayer layer=l.baseLayer();TileDef t=p.getTiles().get(0);t.variants.add(new TileVariant("a",1));t.variants.add(new TileVariant("b",3));Set<String>seen=new HashSet<>();for(int y=0;y<16;y++)for(int x=0;x<24;x++){String first=AdvancedTileResolver.assetFor(p,l,layer,x,y,t);String again=AdvancedTileResolver.assetFor(p,l,layer,x,y,t);assertEquals(first,again);assertTrue(first.equals("a")||first.equals("b"));seen.add(first);}assertEquals(Set.of("a","b"),seen);
    }

    @Test void exactAutotileRuleWinsOverVariants(){
        GameProject p=GameProject.createDefault();Level l=p.getLevels().get("level-1");l.resize(3,3);TileLayer layer=l.baseLayer();TileDef t=p.getTiles().get(0);t.autotileMode=TileAutotileMode.FOUR_WAY;t.autotileAssets.put(0,"isolated");t.variants.add(new TileVariant("variant",1));layer.set(1,1,t.id);assertEquals("isolated",AdvancedTileResolver.assetFor(p,l,layer,1,1,t));
    }
}
