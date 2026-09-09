package com.buttclapdev.twogamerl.runtime;

import com.buttclapdev.twogamerl.model.GameProject;
import com.buttclapdev.twogamerl.model.GameProject.*;
import com.buttclapdev.twogamerl.model.ResourceRef;

/** Runtime-only normalization for 2.2.2. Never applied to the editable project. */
public final class RuntimeProject222 {
    private static final String FIXED_CAMERA="__2rl_fixed_camera";
    private RuntimeProject222(){}

    public static void prepare(GameProject project){
        if(project==null)return;ResourceRef.installRuntimeAliases(project);
        for(Level level:project.getLevels().values()){
            boolean explicitCamera=level.entities.stream().anyMatch(e->{ComponentDef c=e.component("Camera2D");return c!=null&&c.bool("enabled",true);});
            if(explicitCamera||level.cameraTarget!=null&&!level.cameraTarget.isBlank())continue;
            EntityDef anchor=new EntityDef(FIXED_CAMERA,"Cámara fija",level.width/2.0,level.height/2.0);anchor.width=.01;anchor.height=.01;anchor.enabled=false;level.entities.add(anchor);level.cameraTarget=FIXED_CAMERA;
        }
    }
}
