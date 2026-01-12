package org.example.service;

import org.example.model.BackupFile;
import java.io.File;

/**
 * Para duplikatów: plik źródłowy i jego odpowiednik w folderze głównym.
 */
public class DuplicatePair {

    private final BackupFile sourceFile;
    private final File masterFile;

    public DuplicatePair(BackupFile sourceFile, File masterFile) {
        this.sourceFile = sourceFile;
        this.masterFile = masterFile;
    }

    public BackupFile getSourceFile() { return sourceFile; }
    public File getMasterFile() { return masterFile; }
    public String getSourcePath() { return sourceFile.getPath(); }
    public String getMasterPath() { return masterFile.getAbsolutePath(); }
    public String getHash() { return sourceFile.getHash(); }
}
