package com.buttclapdev.twogamerl;

import com.buttclapdev.twogamerl.io.ProjectIO;
import com.buttclapdev.twogamerl.model.AdvancedTileResolver;
import com.buttclapdev.twogamerl.model.GameProject;
import com.buttclapdev.twogamerl.model.GameProject.*;
import com.buttclapdev.twogamerl.model.ResourceIntegrity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class RcStressTest {
    @TempDir Path temp;

    @Test void oneHundredRandomizedRcProjectsSurviveRoundTrip() throws Exception {
        SplittableRandom r=new SplittableRandom(23001);
        for(int iteration=0;iteration<100;iteration++){
            GameProject p=GameProject.createDefault();p.setTitle("stress-"+iteration);Level level=p.getLevels().get("level-1");level.resize(8+r.nextInt(20),8+r.nextInt(20));TileLayer layer=level.baseLayer();
            for(TileDef tile:p.getTiles().values()){tile.autotileMode=r.nextBoolean()?TileAutotileMode.FOUR_WAY:TileAutotileMode.EIGHT_WAY;tile.connectTag=r.nextBoolean()?"ground":"";tile.variants.add(new TileVariant("placeholder-player.png",.1+r.nextDouble()*5));tile.autotileAssets.put(r.nextInt(16),"placeholder-player.png");}
            for(int y=0;y<level.height;y++)for(int x=0;x<level.width;x++)if(r.nextDouble()<.65)layer.set(x,y,r.nextInt(2));
            AnimationClip clip=new AnimationClip("clip","Clip");clip.fps=r.nextBoolean()?0:1+r.nextInt(60);clip.reverse=r.nextBoolean();clip.loop=r.nextBoolean();for(int f=0;f<1+r.nextInt(12);f++){AnimationFrame frame=new AnimationFrame("placeholder-player.png",.01+r.nextDouble());if(r.nextBoolean())frame.shapes.add(new FrameShape("s"+f,r.nextBoolean()?FrameShapeRole.HITBOX:FrameShapeRole.HURTBOX,r.nextDouble(),r.nextDouble(),.1+r.nextDouble(),.1+r.nextDouble()));clip.frames.add(frame);}p.getAnimationClips().put(clip.key,clip);
            AnimatorController ac=new AnimatorController("controller","Controller");AnimatorParameter param=new AnimatorParameter("speed",AnimatorParameterType.FLOAT,Double.toString(r.nextDouble()*2));ac.parameters.put(param.name,param);AnimatorState state=new AnimatorState("Move",clip.key);state.blendTree.type=BlendTreeType.ONE_D;state.blendTree.parameterX="speed";for(int b=0;b<4;b++)state.blendTree.children.add(new BlendChild(clip.key,b,0));ac.states.put(state.name,state);ac.defaultState=state.name;p.getAnimatorControllers().put(ac.key,ac);
            UISkinAsset skin=new UISkinAsset("skin","Skin");skin.normalAssetKey="placeholder-player.png";skin.radius=r.nextDouble()*20;p.getUiSkins().put(skin.key,skin);
            StampPattern stamp=new StampPattern("stamp","Stamp",1+r.nextInt(8),1+r.nextInt(8));for(int y=0;y<stamp.height;y++)for(int x=0;x<stamp.width;x++)stamp.set(x,y,r.nextInt(3)-1);p.getStampPatterns().put(stamp.key,stamp);
            Path path=temp.resolve("stress-"+iteration+".2grl");ProjectIO.save(p,path);GameProject q=ProjectIO.load(path);assertEquals(p.getTitle(),q.getTitle());assertEquals(level.width,q.getLevels().get("level-1").width);assertEquals(clip.frames.size(),q.getAnimationClips().get("clip").frames.size());assertEquals(stamp.width,q.getStampPatterns().get("stamp").width);assertTrue(ResourceIntegrity.brokenAssetReferences(q).stream().noneMatch(s->s.contains("placeholder-player.png")));
        }
    }

    @Test void largeAutotileGridIsStableAcrossRepeatedResolution(){GameProject p=GameProject.createDefault();Level l=p.getLevels().get("level-1");l.resize(256,256);TileLayer layer=l.baseLayer();TileDef t=p.getTiles().get(0);t.autotileMode=TileAutotileMode.EIGHT_WAY;t.variants.add(new TileVariant("a",1));t.variants.add(new TileVariant("b",2));for(int mask=0;mask<256;mask+=17)t.autotileAssets.put(mask,"rule-"+mask);for(int y=0;y<256;y++)for(int x=0;x<256;x++)layer.set(x,y,(x+y)%7==0?1:0);long hash1=0,hash2=0;for(int pass=0;pass<2;pass++){long h=1125899906842597L;for(int y=0;y<256;y++)for(int x=0;x<256;x++){int id=layer.get(x,y);TileDef tile=p.getTiles().get(id);String asset=AdvancedTileResolver.assetFor(p,l,layer,x,y,tile);h=31*h+asset.hashCode();h=31*h+AdvancedTileResolver.mask(p,layer,x,y,tile);}if(pass==0)hash1=h;else hash2=h;}assertEquals(hash1,hash2);}
}
