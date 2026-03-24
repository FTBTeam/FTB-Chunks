package dev.ftb.mods.ftbchunks.neoforge.integration;

import java.nio.file.Path;

// TODO when Backups3 is ported
public class FTBBackups3Integration {
    public static final Path LOCAL_DIR = Path.of("local", "ftbchunks");

    public static void init() {
//        NeoForge.EVENT_BUS.addListener(FTBBackups3Integration::addBackupPath);
    }

//    private static void addBackupPath(BackupEvent.Pre event) {
//        event.add(LOCAL_DIR);
//
//        FTBChunks.LOGGER.info("added {} to FTB Backups 3 path list", LOCAL_DIR);
//    }
}
