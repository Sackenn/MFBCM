# MFBCM - Multimedia File Backup Manager
## Complete Project Documentation

**Version:** 1.0.0  
**Last Updated:** December 21, 2025

---

## Table of Contents

1. [Overview](#1-overview)
2. [Architecture](#2-architecture)
3. [Package Structure](#3-package-structure)
4. [Core Components](#4-core-components)
5. [Data Flow Diagrams](#5-data-flow-diagrams)
6. [Service Layer Details](#6-service-layer-details)
7. [GUI Layer Details](#7-gui-layer-details)
8. [Configuration Management](#8-configuration-management)
9. [File Processing Pipeline](#9-file-processing-pipeline)
10. [Hash Storage System](#10-hash-storage-system)
11. [Key Algorithms](#11-key-algorithms)
12. [Error Handling](#12-error-handling)
13. [Threading Model](#13-threading-model)

---

## 1. Overview

### What is MFBCM?

MFBCM (Multimedia File Backup Manager) is a Java Swing desktop application designed to:

1. **Scan** source directories for multimedia files (images and videos)
2. **Detect duplicates** using fast xxHash3 hashing or metadata comparison
3. **Backup** unique files to a master backup location
4. **Sync** the master backup to multiple secondary locations
5. **Delete** selected files with confirmation

### Key Features

- **Multi-threaded processing** - Uses all available CPU cores for hashing
- **xxHash3 algorithm** - Ultra-fast non-cryptographic hashing
- **Smart duplicate detection** - Compares files by hash or by name+size
- **Persistent hash storage** - Avoids re-hashing unchanged files
- **Image preview** - Shows thumbnails on hover
- **Cross-platform** - Works on Windows, macOS, Linux

### Technology Stack

| Component | Technology |
|-----------|------------|
| Language | Java 21+ |
| GUI | Swing with FlatLaf Dark theme |
| Hashing | xxHash3 via Zero-Allocation-Hashing library |
| JSON | Jackson for hash storage |
| Build | Gradle |

---

## 2. Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         PRESENTATION LAYER                       │
│  ┌──────────────┐  ┌──────────────────┐  ┌──────────────────┐  │
│  │  MainWindow  │  │ DuplicateViewer  │  │  FileListPanel   │  │
│  │              │  │     Window       │  │                  │  │
│  └──────────────┘  └──────────────────┘  └──────────────────┘  │
│                              │                                   │
│                    ┌─────────┴─────────┐                        │
│                    │   UIConstants     │                        │
│                    │ ImagePreviewTooltip│                       │
│                    └───────────────────┘                        │
└─────────────────────────────────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────┐
│                          SERVICE LAYER                           │
│  ┌────────────┐ ┌────────────┐ ┌────────────┐ ┌──────────────┐ │
│  │FileScanner │ │BackupSvc   │ │SyncService │ │FileDeleteSvc │ │
│  └────────────┘ └────────────┘ └────────────┘ └──────────────┘ │
│  ┌─────────────────────┐  ┌─────────────────────────────────┐  │
│  │DuplicateDetectionSvc│  │     HashStorageService          │  │
│  └─────────────────────┘  └─────────────────────────────────┘  │
│  ┌───────────────────────────────────────────────────────────┐ │
│  │              ConfigurationPersistenceService              │ │
│  └───────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────┐
│                           MODEL LAYER                            │
│        ┌──────────────────────┐  ┌──────────────────┐          │
│        │  BackupConfiguration │  │    BackupFile    │          │
│        └──────────────────────┘  └──────────────────┘          │
└─────────────────────────────────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────┐
│                          UTILITY LAYER                           │
│    ┌──────────────────┐  ┌────────────────────────────────┐    │
│    │  FileUtilities   │  │  MultiThreadedHashCalculator   │    │
│    └──────────────────┘  └────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
```

### Design Patterns Used

| Pattern | Where Used | Purpose |
|---------|------------|---------|
| **MVC** | Overall architecture | Separates UI, business logic, and data |
| **Observer** | SwingWorker callbacks | Async progress updates to UI |
| **Factory** | UIConstants | Creates styled UI components |
| **Singleton** | ImagePreviewTooltip | Single popup window instance |
| **Strategy** | Skip hashing option | Different duplicate detection strategies |

---

## 3. Package Structure

```
org.example/
├── Main.java                          # Application entry point
│
├── gui/                               # Presentation layer
│   ├── MainWindow.java                # Main application window
│   ├── DuplicateViewerWindow.java     # Duplicate analysis dialog
│   ├── FileListPanel.java             # File table component
│   ├── UIConstants.java               # Styling and factory methods
│   └── ImagePreviewTooltip.java       # Image preview popup
│
├── model/                             # Data models
│   ├── BackupConfiguration.java       # App settings and paths
│   └── BackupFile.java                # File with metadata and status
│
├── service/                           # Business logic
│   ├── FileScanner.java               # Scans directories for files
│   ├── BackupService.java             # Copies files to master
│   ├── SyncService.java               # Syncs master to locations
│   ├── FileDeleteService.java         # Deletes selected files
│   ├── DuplicateDetectionService.java # Detects duplicate files
│   ├── HashStorageService.java        # Persistent hash cache
│   ├── ConfigurationPersistenceService.java # Saves/loads config
│   ├── DuplicateAnalysisResult.java   # Duplicate scan results
│   ├── DuplicatePair.java             # Source-master file pair
│   ├── SyncResult.java                # Sync operation results
│   ├── BackupProgress.java            # Progress record
│   └── SyncProgress.java              # Sync progress record
│
└── util/                              # Utilities
    ├── FileUtilities.java             # File helpers and formatting
    └── MultiThreadedHashCalculator.java # Multi-threaded hashing
```

---

## 4. Core Components

### 4.1 Main.java

**Purpose:** Application entry point

**Responsibilities:**
- Initialize FlatLaf dark theme
- Configure UI defaults (rounded corners, scrollbars)
- Launch MainWindow on Event Dispatch Thread

```java
public class Main {
    public static final String VERSION = "1.0.0";
    public static final String APP_NAME = "Multimedia File Backup Manager";
    
    public static void main(String[] args) {
        printStartupInfo();      // Log version and system info
        initializeLookAndFeel(); // Set up FlatLaf
        launchApplication();     // Create MainWindow on EDT
    }
}
```

### 4.2 BackupConfiguration (Model)

**Purpose:** Holds all application settings

**Key Fields:**
```java
private File masterBackupLocation;        // Primary backup destination
private List<File> sourceDirectories;     // Folders to scan
private List<File> syncLocations;         // Secondary backup locations
private boolean includeSubdirectories;    // Scan subdirectories
private boolean createDateFolders;        // Organize by date
private boolean skipHashing;              // Use metadata instead of hash
private int hashingThreadCount;           // Parallel processing threads
```

**Validation:**
- Only accepts existing directories
- Prevents duplicate entries
- Thread count bounded to 1 - 2×CPU cores

### 4.3 BackupFile (Model)

**Purpose:** Represents a file to be backed up with metadata

**Key Fields:**
```java
private final File sourceFile;           // The actual file
private final String hash;               // xxHash3 hash (or null)
private final long size;                 // File size in bytes
private final LocalDateTime lastModified; // Modification timestamp
private boolean selected;                // User selection for backup
private BackupStatus status;             // PENDING, IN_PROGRESS, COMPLETED, ERROR, DUPLICATE
private boolean existsInMaster;          // Found in master backup
```

**File Type Detection:**
```java
// Defined extension sets
VIDEO_EXTENSIONS = {"mp4", "avi", "mkv", "mov", ...}
IMAGE_EXTENSIONS = {"jpg", "jpeg", "png", "gif", ...}

public boolean isVideo() { return VIDEO_EXTENSIONS.contains(getFileExtension()); }
public boolean isImage() { return IMAGE_EXTENSIONS.contains(getFileExtension()); }
```

---

## 5. Data Flow Diagrams

### 5.1 Complete Application Data Flow

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              USER INTERACTION                                │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                    ┌─────────────────┼─────────────────┐
                    ▼                 ▼                 ▼
            ┌───────────┐     ┌───────────┐     ┌───────────┐
            │  Browse   │     │   Scan    │     │  Backup   │
            │  Folders  │     │  Files    │     │  Files    │
            └─────┬─────┘     └─────┬─────┘     └─────┬─────┘
                  │                 │                 │
                  ▼                 ▼                 ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                              MAIN WINDOW                                     │
│  ┌─────────────────────┐  ┌─────────────────────┐  ┌──────────────────────┐│
│  │ Configuration Panel │  │   FileListPanel     │  │   Progress Panel     ││
│  │ - Master location   │  │   - File table      │  │   - Scan progress    ││
│  │ - Source dirs       │  │   - Selection       │  │   - Backup progress  ││
│  │ - Sync locations    │  │   - Filters         │  │   - Sync progress    ││
│  │ - Options           │  │                     │  │                      ││
│  └─────────────────────┘  └─────────────────────┘  └──────────────────────┘│
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
         ┌────────────────────────────┼────────────────────────────┐
         ▼                            ▼                            ▼
┌─────────────────┐        ┌─────────────────────┐      ┌─────────────────────┐
│  FileScanner    │        │DuplicateDetection   │      │   BackupService     │
│                 │        │     Service         │      │                     │
│ Collects files  │        │                     │      │ Copies selected     │
│ from source     │───────▶│ Compares files to   │      │ files to master     │
│ directories     │        │ master backup       │      │ backup location     │
└────────┬────────┘        └──────────┬──────────┘      └──────────┬──────────┘
         │                            │                            │
         ▼                            ▼                            ▼
┌─────────────────┐        ┌─────────────────────┐      ┌─────────────────────┐
│  FileUtilities  │        │ HashStorageService  │      │   File System       │
│                 │        │                     │      │                     │
│ - Collect files │        │ - Load hashes       │      │ - Copy files        │
│ - Check types   │        │ - Calculate hashes  │      │ - Create folders    │
│ - Format sizes  │        │ - Store hashes      │      │ - Handle conflicts  │
└─────────────────┘        └──────────┬──────────┘      └─────────────────────┘
                                      │
                                      ▼
                           ┌─────────────────────┐
                           │MultiThreadedHash    │
                           │   Calculator        │
                           │                     │
                           │ - Thread pool       │
                           │ - xxHash3 hashing   │
                           │ - Progress callback │
                           └─────────────────────┘
```

### 5.2 Scan Operation Flow

```
User clicks "Scan for Files"
            │
            ▼
┌─────────────────────────────────────┐
│  MainWindow.startScan()             │
│  - Check if scan in progress        │
│  - Validate configuration           │
└──────────────┬──────────────────────┘
               │
               ▼
┌─────────────────────────────────────┐
│  DuplicateDetectionService          │
│  (extends SwingWorker)              │
│  - Runs in background thread        │
└──────────────┬──────────────────────┘
               │
    ┌──────────┴──────────┐
    ▼                     ▼
┌────────────┐      ┌────────────┐
│ skipHashing│      │   Normal   │
│   = true   │      │   Mode     │
└─────┬──────┘      └─────┬──────┘
      │                   │
      ▼                   ▼
┌────────────────┐  ┌────────────────┐
│scanWithMetadata│  │scanWithHashing │
│                │  │                │
│ Compare by:    │  │ Calculate      │
│ - filename     │  │ xxHash3 for    │
│ - file size    │  │ each file      │
└───────┬────────┘  └───────┬────────┘
        │                   │
        └─────────┬─────────┘
                  ▼
┌─────────────────────────────────────┐
│  analyzeDuplicates()                │
│                                     │
│  Categorize files:                  │
│  - duplicatesInMaster (in backup)   │
│  - duplicatesInSource (within src)  │
│  - newFiles (unique, ready to copy) │
└──────────────┬──────────────────────┘
               │
               ▼
┌─────────────────────────────────────┐
│  DuplicateAnalysisResult            │
│                                     │
│  - sourceFiles: List<BackupFile>    │
│  - duplicatesInMaster: List         │
│  - duplicatesInSource: List         │
│  - newFiles: List                   │
│  - duplicatePairs: List             │
│  - sourceDuplicateGroups: Map       │
└──────────────┬──────────────────────┘
               │
               ▼
┌─────────────────────────────────────┐
│  MainWindow.detectionCompleted()    │
│                                     │
│  - Update FileListPanel             │
│  - Enable View Duplicates button    │
│  - Show status message              │
└─────────────────────────────────────┘
```

### 5.3 Backup Operation Flow

```
User clicks "Start Backup"
            │
            ▼
┌─────────────────────────────────────┐
│  MainWindow.startBackup()           │
│  - Get selected files from panel    │
│  - Validate master location         │
└──────────────┬──────────────────────┘
               │
               ▼
┌─────────────────────────────────────┐
│  BackupService                      │
│  (extends SwingWorker)              │
└──────────────┬──────────────────────┘
               │
               ▼
         ┌─────────────┐
         │  For each   │◄──────────────────┐
         │    file     │                   │
         └──────┬──────┘                   │
                │                          │
                ▼                          │
┌─────────────────────────────────────┐    │
│  shouldSkipFile()?                  │    │
│  - Not selected?                    │    │
│  - Status == DUPLICATE?             │    │
└──────────────┬──────────────────────┘    │
               │                           │
         ┌─────┴─────┐                     │
         │   Skip?   │                     │
         └─────┬─────┘                     │
           No  │  Yes ─────────────────────┤
               ▼                           │
┌─────────────────────────────────────┐    │
│  calculateDestinationPath()         │    │
│                                     │    │
│  if (createDateFolders):            │    │
│    masterLocation/YYYY/MM/file      │    │
│  else:                              │    │
│    masterLocation/file              │    │
└──────────────┬──────────────────────┘    │
               │                           │
               ▼                           │
┌─────────────────────────────────────┐    │
│  copyFile()                         │    │
│                                     │    │
│  1. validateDiskSpace()             │    │
│  2. resolveNameConflict()           │    │
│     (adds _1, _2, etc if exists)    │    │
│  3. ensureParentDirectoryExists()   │    │
│  4. Files.copy() with attributes    │    │
│  5. Verify size matches             │    │
└──────────────┬──────────────────────┘    │
               │                           │
               ▼                           │
┌─────────────────────────────────────┐    │
│  Update file status                 │    │
│  - COMPLETED or ERROR               │    │
│  - Publish progress                 │────┘
└──────────────┬──────────────────────┘
               │
               ▼
┌─────────────────────────────────────┐
│  backupCompleted()                  │
│  - Report success/error counts      │
│  - Update UI                        │
└─────────────────────────────────────┘
```

### 5.4 Hash Calculation Flow

```
┌─────────────────────────────────────┐
│  List<File> filesToHash             │
└──────────────┬──────────────────────┘
               │
               ▼
┌─────────────────────────────────────┐
│  MultiThreadedHashCalculator        │
│                                     │
│  Thread pool with N threads         │
│  (N = hashingThreadCount)           │
└──────────────┬──────────────────────┘
               │
    ┌──────────┴──────────────────────────────────────────┐
    │          │          │          │          │         │
    ▼          ▼          ▼          ▼          ▼         ▼
┌────────┐┌────────┐┌────────┐┌────────┐┌────────┐┌────────┐
│Thread 1││Thread 2││Thread 3││Thread 4││Thread 5││Thread N│
└───┬────┘└───┬────┘└───┬────┘└───┬────┘└───┬────┘└───┬────┘
    │         │         │         │         │         │
    └─────────┴─────────┴────┬────┴─────────┴─────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────┐
│  calculateFileHash(file)                                │
│                                                         │
│  if (fileSize < 100MB):                                │
│    ┌─────────────────────────────────────────────────┐ │
│    │  hashSmallFile()                                │ │
│    │  - Read entire file into memory                 │ │
│    │  - Hash all bytes with xxHash3                  │ │
│    └─────────────────────────────────────────────────┘ │
│  else:                                                  │
│    ┌─────────────────────────────────────────────────┐ │
│    │  hashLargeFile() - CHUNKED SAMPLING             │ │
│    │                                                 │ │
│    │  File: [====================================]   │ │
│    │         ↑    ↑    ↑    ↑    ↑    ↑    ↑    ↑   │ │
│    │        10MB chunks sampled at even intervals    │ │
│    │                                                 │ │
│    │  - Read 10 chunks of 10MB each                  │ │
│    │  - Hash each chunk separately                   │ │
│    │  - Combine: hash = rotateLeft(hash) XOR chunk   │ │
│    └─────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────┘
               │
               ▼
┌─────────────────────────────────────┐
│  Return 16-character hex string     │
│  e.g., "a1b2c3d4e5f6a7b8"          │
└─────────────────────────────────────┘
```

---

## 6. Service Layer Details

### 6.1 FileScanner

**Purpose:** Scans source directories for multimedia files

**Key Methods:**
```java
protected List<BackupFile> doInBackground() {
    List<File> allFiles = collectAllFiles();  // Recursive directory scan
    
    if (configuration.isSkipHashing()) {
        return scanWithMetadata(allFiles);    // Compare by name+size
    } else {
        return scanWithHashing(allFiles);     // Calculate xxHash3
    }
}
```

**Callback Interface:**
```java
public interface ScanProgressCallback {
    void updateProgress(int current, int total, String currentFile);
    void scanCompleted(List<BackupFile> files);
    void scanFailed(String error);
}
```

### 6.2 DuplicateDetectionService

**Purpose:** Detects files that exist in master backup or are duplicates within source

**Two Detection Modes:**

1. **With Hashing (default):**
   - Calculate xxHash3 for each source file
   - Compare against stored hashes from master
   - Two files with same hash = duplicate

2. **Skip Hashing (fast mode):**
   - Compare by `filename + "|" + filesize`
   - Much faster but less accurate
   - May have false positives (same name+size, different content)

**Key Output:**
```java
class DuplicateAnalysisResult {
    List<BackupFile> sourceFiles;          // All scanned files
    List<BackupFile> duplicatesInMaster;   // Already in master
    List<BackupFile> duplicatesInSource;   // Duplicates within source
    List<BackupFile> newFiles;             // Unique, ready for backup
    List<DuplicatePair> duplicatePairs;    // Source-master pairs
    Map<String, List<BackupFile>> sourceDuplicateGroups; // Grouped by hash
}
```

### 6.3 BackupService

**Purpose:** Copies selected files to master backup location

**Features:**
- **Date folders:** Optional YYYY/MM organization
- **Conflict resolution:** Adds _1, _2, etc. for duplicates
- **Disk space validation:** Checks before copying
- **Attribute preservation:** Keeps modification time

### 6.4 SyncService

**Purpose:** Mirrors master backup to secondary locations

**Algorithm:**
1. Calculate total size to sync
2. For each sync location:
   - Walk master directory tree
   - Copy new/modified files
   - Delete files not in master
   - Remove empty directories

**Comparison Logic:**
```java
// File is identical if:
sourceAttrs.size() == targetAttrs.size() &&
sourceAttrs.lastModifiedTime().equals(targetAttrs.lastModifiedTime())
```

### 6.5 HashStorageService

**Purpose:** Persistent cache of file hashes in master backup

**Storage Format:** JSON file `.mfbcm_hashes.json` in master folder

```json
{
  "photos/vacation/beach.jpg": {
    "relativePath": "photos/vacation/beach.jpg",
    "hash": "a1b2c3d4e5f6a7b8",
    "lastModified": 1703145600000,
    "fileSize": 2458624
  }
}
```

**Hash Invalidation:**
- File modified time changed → re-hash
- File size changed → re-hash
- File deleted → remove from cache

### 6.6 FileDeleteService

**Purpose:** Safely deletes selected files

**Features:**
- Background thread processing
- Tracks success/failure for each file
- Cancellation support
- Reports detailed error reasons

---

## 7. GUI Layer Details

### 7.1 MainWindow

**Purpose:** Main application window, orchestrates all operations

**Layout:**
```
┌─────────────────────────────────────────────────────────────────┐
│                     Configuration Panel                          │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │ Master Backup Location: [                        ] [Browse] ││
│  ├─────────────────────────────────────────────────────────────┤│
│  │ Source Directories:     [List of directories]    [Add/Rem] ││
│  ├─────────────────────────────────────────────────────────────┤│
│  │ Sync Locations:         [List of locations]      [Add/Rem] ││
│  ├─────────────────────────────────────────────────────────────┤│
│  │ Options: ☑ Subdirs  ☐ Date folders  ☑ Duplicates  ☐ Skip   ││
│  ├─────────────────────────────────────────────────────────────┤│
│  │ [Scan] [Backup] [View Duplicates] [Rescan Master] [Sync]   ││
│  └─────────────────────────────────────────────────────────────┘│
├─────────────────────────────────────────────────────────────────┤
│                        FileListPanel                             │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │ ☑ Select All    Filter: [All ▼]                             ││
│  ├────┬──────────┬───────────────┬──────┬──────────┬────┬──────┤│
│  │ ☑  │ Name     │ Path          │ Size │ Modified │Type│Status││
│  ├────┼──────────┼───────────────┼──────┼──────────┼────┼──────┤│
│  │ ☑  │ photo.jpg│ C:\Photos\... │ 2.4MB│ 2025-... │Img │PEND  ││
│  │ ☐  │ video.mp4│ C:\Videos\... │150MB │ 2025-... │Vid │DUP   ││
│  └────┴──────────┴───────────────┴──────┴──────────┴────┴──────┘│
│  Showing 150 files | Selected: 75 (1.2 GB of 3.5 GB)            │
├─────────────────────────────────────────────────────────────────┤
│                       Progress Panel                             │
│  Scan:    [████████████████████░░░░░░░░] 80% - Processing...    │
│  Backup:  [░░░░░░░░░░░░░░░░░░░░░░░░░░░░]  0% - Ready            │
│  Sync:    [░░░░░░░░░░░░░░░░░░░░░░░░░░░░]  0% - Ready            │
└─────────────────────────────────────────────────────────────────┘
│ Status: Scanning 150 files...                                    │
└─────────────────────────────────────────────────────────────────┘
```

**Implemented Callbacks:**
- `FileScanner.ScanProgressCallback`
- `BackupService.BackupProgressCallback`
- `DuplicateDetectionService.DuplicateDetectionCallback`
- `SyncService.SyncProgressCallback`
- `FileDeleteService.DeleteProgressCallback`

### 7.2 FileListPanel

**Purpose:** Table displaying scanned files with selection

**Features:**
- Checkbox column for selection
- Filter dropdown (All, Images, Videos, Selected, Duplicates)
- Color-coded status column
- Double-click to toggle selection
- Image preview on hover

**Table Columns:**
| Column | Content | Width |
|--------|---------|-------|
| ☑ | Selection checkbox | 40 |
| Name | File name | 250 |
| Path | Full path | 350 |
| Size | Formatted size | 90 |
| Modified | Date time | 140 |
| Type | Image/Video | 80 |
| Status | PENDING/DUPLICATE/etc | 100 |

### 7.3 DuplicateViewerWindow

**Purpose:** Modal dialog showing detailed duplicate analysis

**Tabs:**
1. **Master Duplicates** - Files already in master backup
2. **Source Duplicates** - Duplicate files within source directories
3. **Summary** - Statistics and potential space savings

### 7.4 UIConstants

**Purpose:** Centralized styling and component factory

**Color Scheme (Dark Theme):**
```java
BG_PRIMARY    = #2D3139  // Main background
BG_SECONDARY  = #353A42  // Panel background
BG_INPUT      = #3C424D  // Input fields
TEXT_PRIMARY  = #C8C8C8  // Main text
TEXT_SECONDARY= #B4B4B4  // Secondary text
STATUS_SUCCESS= #27AE60  // Green
STATUS_ERROR  = #E74C3C  // Red
STATUS_WARNING= #E67E22  // Orange
```

### 7.5 ImagePreviewTooltip

**Purpose:** Shows image thumbnail on hover

**Implementation:**
- Uses `JWindow` popup (not HTML tooltip)
- Caches thumbnails in memory (max 100)
- Scales to max 300×300 pixels
- Displays filename, size, date

---

## 8. Configuration Management

### 8.1 Saved Settings

**File:** `config.properties` (in working directory)

**Contents:**
```properties
masterBackupLocation=C:/Backup/Master
sourceDirectories=C:/Photos|C:/Videos|D:/Camera
syncLocations=E:/Backup1|F:/Backup2
includeSubdirectories=true
createDateFolders=false
skipHashing=false
hashingThreadCount=8
lastSaved=1703145600000
```

### 8.2 ConfigurationPersistenceService

**Load on Startup:**
```java
BackupConfiguration config = configPersistenceService.loadConfiguration();
// Validates each directory still exists
// Ignores invalid/deleted paths
```

**Save on Change:**
```java
// Called whenever user changes a setting
configPersistenceService.saveConfiguration(configuration);
```

---

## 9. File Processing Pipeline

### 9.1 Multimedia File Detection

**Supported Extensions:**

| Category | Extensions |
|----------|------------|
| Images | jpg, jpeg, png, gif, bmp, tiff, tif, webp, svg, raw, cr2, nef, dng, arw, orf, rw2, pef, srw |
| Videos | mp4, avi, mkv, mov, wmv, flv, webm, m4v, 3gp, mpg, mpeg, m2v, mts, ts, vob, asf, rm, rmvb |

**Detection Code:**
```java
public static boolean isMultimediaFile(File file) {
    String extension = getExtension(file.getName());
    return MULTIMEDIA_EXTENSIONS.contains(extension);
}
```

### 9.2 Processing States

```
PENDING ─────────────────────────────────────────────────────────┐
   │                                                              │
   │ (Scan starts)                                                │
   ▼                                                              │
┌─────────────────────────────────────────────────────────────┐  │
│                    DURING SCAN                               │  │
│                                                              │  │
│  if (hash matches master)     → DUPLICATE (existsInMaster)  │  │
│  if (hash matches other src)  → DUPLICATE                   │  │
│  else                         → stays PENDING               │  │
└─────────────────────────────────────────────────────────────┘  │
   │                                                              │
   │ (Backup starts)                                              │
   ▼                                                              │
IN_PROGRESS ──┬─────────────────────────────────────────────────│
              │                                                   │
              │ (Copy completes)                                  │
              ▼                                                   │
         COMPLETED                                                │
              │                                                   │
              │ (Error occurs)                                    │
              ▼                                                   │
           ERROR ◄────────────────────────────────────────────────┘
```

---

## 10. Hash Storage System

### 10.1 Purpose

Avoid re-hashing files that haven't changed between scans.

### 10.2 Storage Location

File: `<masterBackupLocation>/.mfbcm_hashes.json`

### 10.3 Data Structure

```json
{
  "relative/path/to/file.jpg": {
    "relativePath": "relative/path/to/file.jpg",
    "hash": "a1b2c3d4e5f6a7b8",
    "lastModified": 1703145600000,
    "fileSize": 2458624
  }
}
```

### 10.4 Validation on Load

```java
private boolean isValidHashInfo(FileHashInfo info) {
    return info != null &&
           info.getHash() != null && !info.getHash().isEmpty() &&
           info.getRelativePath() != null && !info.getRelativePath().isEmpty() &&
           info.getFileSize() >= 0 && 
           info.getLastModified() > 0;
}
```

### 10.5 Hash Update Logic

```
For each file in master folder:
┌─────────────────────────────────────────────────────┐
│                                                     │
│  Has stored hash?                                   │
│       │                                             │
│   ┌───┴───┐                                         │
│  No      Yes                                        │
│   │       │                                         │
│   │       ▼                                         │
│   │   File modified?                                │
│   │   (lastModified or size changed)                │
│   │       │                                         │
│   │   ┌───┴───┐                                     │
│   │  No      Yes                                    │
│   │   │       │                                     │
│   │   │       └──────┐                              │
│   │   │              │                              │
│   │   ▼              ▼                              │
│   │  Use cached    Re-hash file                     │
│   │  hash          Update cache                     │
│   │                                                 │
│   └──────────────────┘                              │
│           │                                         │
│           ▼                                         │
│     Hash file                                       │
│     Add to cache                                    │
│                                                     │
└─────────────────────────────────────────────────────┘
```

---

## 11. Key Algorithms

### 11.1 xxHash3 Hashing

**Why xxHash3?**
- 10-100× faster than MD5/SHA
- Non-cryptographic (not for security)
- Perfect for duplicate detection
- Low collision probability

**Library:** `net.openhft:zero-allocation-hashing`

**Usage:**
```java
LongHashFunction hashFunction = LongHashFunction.xx3();
long hash = hashFunction.hashBytes(fileBytes);
return String.format("%016x", hash);  // 16-char hex string
```

### 11.2 Large File Chunked Hashing

**Problem:** Large files (>100MB) consume too much memory

**Solution:** Sample 10 chunks evenly distributed through file

```
File: [====================================] (2 GB)
       ↓    ↓    ↓    ↓    ↓    ↓    ↓    ↓
      10MB chunks at positions:
      0%, 10%, 20%, 30%, 40%, 50%, 60%, 70%, 80%, 90%

Hash combining:
hash = chunk0Hash
hash = rotateLeft(hash, 1) XOR chunk1Hash
hash = rotateLeft(hash, 1) XOR chunk2Hash
... and so on
```

**Trade-off:** Slightly less accurate but much faster and memory-efficient

### 11.3 Duplicate Detection

**With Hash:**
```java
// Two files are duplicates if they have the same hash
if (masterHashes.containsKey(sourceFile.getHash())) {
    sourceFile.setStatus(DUPLICATE);
    sourceFile.setExistsInMaster(true);
}
```

**Without Hash (Metadata):**
```java
// Compare by name + size
String key = fileName + "|" + fileSize;
if (masterMetadataMap.containsKey(key)) {
    // Likely duplicate (not guaranteed)
}
```

---

## 12. Error Handling

### 12.1 Exception Strategy

| Location | Handling |
|----------|----------|
| File operations | Try-catch with status update |
| Hash calculation | Return null, log error, continue |
| UI callbacks | SwingUtilities.invokeLater |
| Thread interruption | Restore interrupt flag |

### 12.2 Common Errors

| Error | Cause | Resolution |
|-------|-------|------------|
| Insufficient disk space | Full destination | Check before copy |
| Access denied | Permissions | Log and skip file |
| File in use | Locked by another process | Report in UI |
| Out of memory | Large file | Use chunked hashing |

### 12.3 Error Reporting

```java
// Files that fail are tracked
backupFile.setStatus(BackupFile.BackupStatus.ERROR);
progressCallback.fileCompleted(file, false, "Error message");
```

---

## 13. Threading Model

### 13.1 Thread Types

| Thread | Purpose | Created By |
|--------|---------|------------|
| EDT (Event Dispatch Thread) | UI updates | Swing |
| SwingWorker background | Long operations | Services |
| Hash calculator pool | Parallel hashing | ForkJoinPool or custom |

### 13.2 SwingWorker Pattern

All services extend `SwingWorker<Result, Progress>`:

```java
public class ExampleService extends SwingWorker<ResultType, ProgressType> {
    
    @Override
    protected ResultType doInBackground() {
        // Runs on background thread
        publish(progressUpdate);  // Send to process()
        return result;
    }
    
    @Override
    protected void process(List<ProgressType> chunks) {
        // Runs on EDT - safe for UI updates
        callback.updateProgress(...);
    }
    
    @Override
    protected void done() {
        // Runs on EDT after doInBackground completes
        try {
            ResultType result = get();
            callback.completed(result);
        } catch (CancellationException e) {
            callback.cancelled();
        } catch (Exception e) {
            callback.failed(e.getMessage());
        }
    }
}
```

### 13.3 Hash Calculator Threading

```java
// Default: Use common ForkJoinPool
if (threadCount == availableProcessors) {
    executor = ForkJoinPool.commonPool();
}

// Custom count: Create dedicated pool
else {
    executor = Executors.newFixedThreadPool(threadCount);
}
```

### 13.4 Thread Safety

**Concurrent Collections Used:**
- `ConcurrentHashMap` - Thread-safe hash storage
- `CopyOnWriteArrayList` - Thread-safe file lists
- `AtomicInteger` - Progress counters

**Synchronization:**
- UI updates always via `SwingUtilities.invokeLater()`
- Progress callbacks batched (every 100 files or 0.1%)

---

## Quick Reference

### Keyboard Shortcuts

| Action | Shortcut |
|--------|----------|
| Double-click file | Toggle selection |
| Select All checkbox | Select/deselect all visible |

### File Locations

| File | Location | Purpose |
|------|----------|---------|
| config.properties | Working directory | App settings |
| .mfbcm_hashes.json | Master backup folder | Hash cache |

### Status Colors

| Status | Color | Meaning |
|--------|-------|---------|
| PENDING | White | Ready for backup |
| IN_PROGRESS | Blue | Currently copying |
| COMPLETED | Green | Successfully backed up |
| ERROR | Red | Failed to backup |
| DUPLICATE | Orange | Already exists/duplicate |

---

*Documentation generated for MFBCM v1.0.0*

