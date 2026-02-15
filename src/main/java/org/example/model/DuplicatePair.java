package org.example.model;

import java.io.File;

/**
 * Para duplikatów: plik źródłowy i jego odpowiednik w folderze głównym.
 */
public record DuplicatePair(BackupFile sourceFile, File masterFile) {

    public String getSourcePath() { return sourceFile.getPath(); }
    public String getMasterPath() { return masterFile.getAbsolutePath(); }
    public String getHash() { return sourceFile.getHash(); }

    // Aliasy dla kompatybilności z istniejącym kodem GUI
    public BackupFile getSourceFile() { return sourceFile; }
    public File getMasterFile() { return masterFile; }
}

