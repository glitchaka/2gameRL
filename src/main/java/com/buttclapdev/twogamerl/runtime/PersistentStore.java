package com.buttclapdev.twogamerl.runtime;

import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.function.Consumer;

final class PersistentStore implements AutoCloseable {
    private final Map<String,String> fallback=new HashMap<>();
    private Connection connection;
    private int slot=1;
    private final Consumer<String> logger;

    PersistentStore(String gameTitle,Consumer<String> logger){
        this.logger=logger==null?s->{}:logger;
        try{
            Class.forName("org.sqlite.JDBC");
            String local=System.getenv("LOCALAPPDATA");
            Path root=(local==null||local.isBlank()?Paths.get(System.getProperty("user.home","."),".2gameRL"):Paths.get(local,"2gameRL"));
            Path dir=root.resolve(safeName(gameTitle));Files.createDirectories(dir);Path db=dir.resolve("save.db");
            connection=DriverManager.getConnection("jdbc:sqlite:"+db.toAbsolutePath());
            try(Statement st=connection.createStatement()){st.executeUpdate("CREATE TABLE IF NOT EXISTS save_values (slot INTEGER NOT NULL, key TEXT NOT NULL, value TEXT NOT NULL, PRIMARY KEY(slot,key))");}
        }catch(Exception ex){connection=null;this.logger.accept("[Save] SQLite no disponible; se usará memoria durante esta ejecución: "+ex.getMessage());}
    }

    int slot(){return slot;}void slot(int value){slot=Math.max(1,value);}
    String get(String key){if(key==null||key.isBlank())return"";if(connection==null)return fallback.getOrDefault(slot+"\0"+key,"0");try(PreparedStatement ps=connection.prepareStatement("SELECT value FROM save_values WHERE slot=? AND key=?")){ps.setInt(1,slot);ps.setString(2,key);try(ResultSet rs=ps.executeQuery()){return rs.next()?rs.getString(1):"0";}}catch(SQLException ex){logger.accept("[Save] "+ex.getMessage());return"0";}}
    void set(String key,String value){if(key==null||key.isBlank())return;String v=value==null?"":value;if(connection==null){fallback.put(slot+"\0"+key,v);return;}try(PreparedStatement ps=connection.prepareStatement("INSERT INTO save_values(slot,key,value) VALUES(?,?,?) ON CONFLICT(slot,key) DO UPDATE SET value=excluded.value")){ps.setInt(1,slot);ps.setString(2,key);ps.setString(3,v);ps.executeUpdate();}catch(SQLException ex){logger.accept("[Save] "+ex.getMessage());}}
    void clearSlot(){if(connection==null){String prefix=slot+"\0";fallback.keySet().removeIf(k->k.startsWith(prefix));return;}try(PreparedStatement ps=connection.prepareStatement("DELETE FROM save_values WHERE slot=?")){ps.setInt(1,slot);ps.executeUpdate();}catch(SQLException ex){logger.accept("[Save] "+ex.getMessage());}}
    @Override public void close(){if(connection!=null)try{connection.close();}catch(SQLException ignored){}}
    private static String safeName(String raw){String s=raw==null?"game":raw.trim().replaceAll("[\\\\/:*?\"<>|]+","_").replaceAll("\\s+"," ");return s.isBlank()?"game":s;}
}
