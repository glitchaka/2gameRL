package com.buttclapdev.twogamerl.export;

import com.buttclapdev.twogamerl.io.ProjectIO;
import java.nio.file.Path;

public final class ExportCli {
    private ExportCli() {}
    public static void main(String[] args) throws Exception {
        if(args.length<2){System.err.println("Uso: ExportCli <proyecto.2grl> <carpeta-salida>");System.exit(2);}
        var result=NativeExporter.export(ProjectIO.load(Path.of(args[0])),Path.of(args[1]),System.out::println);
        if(!result.success()){System.err.println(result.message());System.exit(1);}
        System.out.println("Aplicación: "+result.application());System.out.println("ZIP: "+result.zip());
    }
}
