package com.buttclapdev.twogamerl.runtime;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class PersistentStoreTest {
    @Test void valuesPersistAcrossInstances(){String game="2gameRL-test-"+UUID.randomUUID();PersistentStore first=new PersistentStore(game,s->{});first.set("nombre","Adarvio");first.set("monedas","7");first.close();PersistentStore second=new PersistentStore(game,s->{});assertEquals("Adarvio",second.get("nombre"));assertEquals("7",second.get("monedas"));second.close();}
}
