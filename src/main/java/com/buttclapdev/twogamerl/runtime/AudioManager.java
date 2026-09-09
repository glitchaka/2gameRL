package com.buttclapdev.twogamerl.runtime;

import com.buttclapdev.twogamerl.model.GameProject;
import com.buttclapdev.twogamerl.model.GameProject.SoundAsset;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.util.*;
import java.util.function.Consumer;

/** Runtime WAV audio mixer for 2gameRL. Uses only the JDK sampled-audio API. */
public final class AudioManager implements AutoCloseable {
    private final GameProject project;
    private final Consumer<String> logger;
    private final Map<String,List<Clip>> effects=new LinkedHashMap<>();
    private Clip music;
    private String musicKey="";

    public AudioManager(GameProject project,Consumer<String> logger){this.project=Objects.requireNonNull(project);this.logger=logger==null?System.out::println:logger;}

    public synchronized void playSound(String ref,double volume){SoundAsset asset=project.sound(ref);if(asset==null){logger.accept("[Audio] No existe el sonido '"+ref+"'.");return;}Clip clip=open(asset);if(clip==null)return;applyVolume(clip,clamp(volume)*clamp(asset.volume));effects.computeIfAbsent(asset.key,k->new ArrayList<>()).add(clip);clip.addLineListener(e->{if(e.getType()==LineEvent.Type.STOP&&!clip.isRunning()){synchronized(AudioManager.this){List<Clip>list=effects.get(asset.key);if(list!=null){list.remove(clip);if(list.isEmpty())effects.remove(asset.key);}clip.close();}}});clip.setFramePosition(0);if(asset.loop)clip.loop(Clip.LOOP_CONTINUOUSLY);else clip.start();}
    public synchronized void stopSound(String ref){SoundAsset asset=project.sound(ref);String key=asset==null?ref:asset.key;List<Clip>list=effects.remove(key);if(list!=null)for(Clip c:list)closeClip(c);}
    public synchronized void playMusic(String ref,double volume){SoundAsset asset=project.sound(ref);if(asset==null){logger.accept("[Audio] No existe la música '"+ref+"'.");return;}stopMusic();Clip clip=open(asset);if(clip==null)return;music=clip;musicKey=asset.key;applyVolume(clip,clamp(volume)*clamp(asset.volume));clip.setFramePosition(0);if(asset.loop)clip.loop(Clip.LOOP_CONTINUOUSLY);else clip.start();}
    public synchronized void stopMusic(){if(music!=null)closeClip(music);music=null;musicKey="";}
    public synchronized void stopEffects(){for(List<Clip>list:effects.values())for(Clip c:list)closeClip(c);effects.clear();}
    public synchronized boolean isMusicPlaying(){return music!=null&&music.isOpen()&&music.isRunning();}
    public synchronized String musicKey(){return musicKey;}
    @Override public synchronized void close(){stopEffects();stopMusic();}

    private Clip open(SoundAsset asset){if(asset.data==null||asset.data.length==0){logger.accept("[Audio] '"+asset.name+"' no contiene datos WAV.");return null;}try(ByteArrayInputStream raw=new ByteArrayInputStream(asset.data);AudioInputStream input=AudioSystem.getAudioInputStream(raw)){AudioFormat source=input.getFormat();AudioFormat decoded=source;if(source.getEncoding()!=AudioFormat.Encoding.PCM_SIGNED||source.getSampleSizeInBits()!=16){decoded=new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,source.getSampleRate(),16,source.getChannels(),source.getChannels()*2,source.getSampleRate(),false);}try(AudioInputStream pcm=decoded.matches(source)?input:AudioSystem.getAudioInputStream(decoded,input)){Clip clip=AudioSystem.getClip();clip.open(pcm);return clip;}}catch(Exception ex){logger.accept("[Audio] No se pudo reproducir '"+asset.name+"': "+ex.getMessage());return null;}}
    private static void applyVolume(Clip clip,double linear){if(!clip.isControlSupported(FloatControl.Type.MASTER_GAIN))return;FloatControl gain=(FloatControl)clip.getControl(FloatControl.Type.MASTER_GAIN);float db=linear<=0?gain.getMinimum():(float)(20.0*Math.log10(linear));gain.setValue(Math.max(gain.getMinimum(),Math.min(gain.getMaximum(),db)));}
    private static double clamp(double v){return Math.max(0,Math.min(1,v));}
    private static void closeClip(Clip c){try{c.stop();c.flush();c.close();}catch(Exception ignored){}}
}
