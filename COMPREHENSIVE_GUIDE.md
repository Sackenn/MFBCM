# MFBCM - Comprehensive Project Guide

> **Multimedia File Backup Manager** - A high-performance Java desktop application for managing multimedia file backups with advanced duplicate detection.

## Table of Contents
1. [Project Overview](#1-project-overview)
2. [Getting Started](#2-getting-started)
3. [Complete Project Structure](#3-complete-project-structure)
4. [File-by-File Documentation](#4-file-by-file-documentation)
5. [Architecture & Design Patterns](#5-architecture--design-patterns)
6. [Data Flow Diagrams](#6-data-flow-diagrams)
7. [Configuration System](#7-configuration-system)
8. [GUI Components](#8-gui-components)
9. [Key Algorithms](#9-key-algorithms)
10. [Performance Optimizations](#10-performance-optimizations)
11. [Development Guide](#11-development-guide)
12. [Troubleshooting](#12-troubleshooting)
13. [Future Enhancements](#13-future-enhancements)

---

## 1. Project Overview

### What is MFBCM?
**Multimedia File Backup Manager (MFBCM)** is a high-performance Java desktop application designed to:
- Back up photos and videos to a master folder
- Detect duplicate files using ultra-fast hashing
- Organize files by date (optional)
- Maintain a hash database for quick comparisons

### Key Features
| Feature | Description |
|---------|-------------|
| **Modern Dark UI** | Professional FlatLaf dark theme |
| **Multi-threaded Hashing** | Uses all CPU cores for speed |
| **xxHash3 Algorithm** | Ultra-fast (22+ GB/s) file hashing |
| **Duplicate Detection** | Finds duplicates in sources and against master |
| **Date-based Organization** | Optional YYYY/MM folder structure |
| **Persistent Configuration** | Remembers settings between sessions |

### Technology Stack
- **Language**: Java 11+ (tested on Java 24)
- **Build Tool**: Gradle 8.x
- **UI Framework**: Swing + FlatLaf 3.5.2
- **JSON Library**: Jackson 2.18.2
- **Hashing**: Zero-Allocation Hashing 0.27ea1 (xxHash3)
- **File Utilities**: Apache Commons IO 2.18.0

---

## 2. Getting Started

### Prerequisites
- Java 11 or higher (Java 24 recommended)
- Windows/macOS/Linux
- 2GB+ RAM (4GB recommended for large libraries)

### Installation & Running

#### Method 1: Using Batch File (Windows - Recommended)
```cmd
run.bat
```

#### Method 2: Using Gradle
```cmd
.\gradlew clean build
.\gradlew run
```

#### Method 3: Direct JAR Execution
```cmd
java --enable-native-access=ALL-UNNAMED -jar build\libs\MFBCM-1.0-SNAPSHOT.jar
```

> **Note**: The `--enable-native-access=ALL-UNNAMED` flag is required for Java 17+ to suppress native access warnings from FlatLaf and xxHash3.

### First-Time Setup
1. **Set Master Backup Location**: Click "Browse..." and select your backup destination folder
2. **Add Source Directories**: Click "Add..." to add folders containing photos/videos
3. **Configure Options**:
   - ✅ Include subdirectories (scans nested folders)
   - ✅ Create date-based folders (organizes by YYYY/MM)
   - ✅ Detect duplicates (compares against master)
   - Hash threads: Number of CPU cores to use

---

## 3. Complete Project Structure

### Full Directory Tree
```
MFBCM/
├── 📄 build.gradle              # Gradle build configuration
├── 📄 config.properties         # Runtime settings (auto-generated)
├── 📄 COMPREHENSIVE_GUIDE.md    # This document
├── 📄 FIXES.md                  # Known issues and solutions
├── 📄 gradlew                   # Gradle wrapper (Unix)
├── 📄 gradlew.bat               # Gradle wrapper (Windows)
├── 📄 README.md                 # Quick start guide
├── 📄 run.bat                   # Windows launcher script
├── 📄 settings.gradle           # Gradle settings
│
├── 📁 build/                    # Build output (generated)
│   ├── 📁 classes/              # Compiled .class files
│   ├── 📁 distributions/        # ZIP/TAR distributions
│   ├── 📁 libs/                 # Built JAR file
│   └── 📁 scripts/              # Generated run scripts
│
├── 📁 gradle/wrapper/           # Gradle wrapper files
│   ├── 📄 gradle-wrapper.jar
│   └── 📄 gradle-wrapper.properties
│
└── 📁 src/
    ├── 📁 main/
    │   ├── 📁 java/org/example/
    │   │   │
    │   │   ├── 📄 Main.java                     # Application entry point
    │   │   │
    │   │   ├── 📁 gui/                          # User Interface Layer
    │   │   │   ├── 📄 MainWindow.java           # Main application window
    │   │   │   ├── 📄 FileListPanel.java        # File list table component
    │   │   │   └── 📄 DuplicateViewerWindow.java # Duplicate viewer dialog
    │   │   │
    │   │   ├── 📁 model/                        # Data Model Layer
    │   │   │   ├── 📄 BackupConfiguration.java  # App configuration
    │   │   │   └── 📄 BackupFile.java           # File metadata model
    │   │   │
    │   │   ├── 📁 service/                      # Business Logic Layer
    │   │   │   ├── 📄 FileScanner.java          # File discovery service
    │   │   │   ├── 📄 BackupService.java        # Backup operations
    │   │   │   ├── 📄 BackupProgress.java       # Backup progress DTO
    │   │   │   ├── 📄 HashStorageService.java   # Hash database manager
    │   │   │   ├── 📄 DuplicateDetectionService.java # Duplicate finder
    │   │   │   ├── 📄 DuplicateAnalysisResult.java   # Analysis result DTO
    │   │   │   ├── 📄 DuplicatePair.java        # Duplicate pair info
    │   │   │   ├── 📄 SyncService.java          # Sync operations
    │   │   │   ├── 📄 SyncProgress.java         # Sync progress DTO
    │   │   │   ├── 📄 SyncResult.java           # Sync result DTO
    │   │   │   └── 📄 ConfigurationPersistenceService.java # Config I/O
    │   │   │
    │   │   └── 📁 util/                         # Utility Layer
    │   │       ├── 📄 MultiThreadedHashCalculator.java # Parallel hashing
    │   │       └── 📄 FileUtilities.java        # Common file operations
    │   │
    │   └── 📁 resources/                        # Resource files (empty)
    │
    ├── 📁 reports/                              # Development documentation
    │   ├── 📄 00_README.md
    │   ├── 📄 01_Round_1_Optimization_Report.md
    │   ├── 📄 02_Round_2_Additional_Improvements.md
    │   ├── 📄 03_Round_3_Thread_Safety.md
    │   ├── 📄 04_Round_4_Dead_Code_Removal.md
    │   ├── 📄 05_Round_4_Code_Quality.md
    │   ├── 📄 06_Round_5_Final_Sweep.md
    │   ├── 📄 07_Ultimate_Final_Polish.md
    │   ├── 📄 08_GUI_Modernization.md
    │   ├── 📄 09_Code_Cleanup.md
    │   ├── 📄 10_Code_Refactoring.md
    │   └── 📄 99_Executive_Summary.md
    │
    └── 📁 test/                                 # Test files (empty)
        ├── 📁 java/
        └── 📁 resources/
```

### Layer Architecture Diagram
```
┌─────────────────────────────────────────────────────────┐
│                    GUI Layer (Swing)                    │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────┐  │
│  │ MainWindow  │  │FileListPanel│  │DuplicateViewer  │  │
│  └──────┬──────┘  └──────┬──────┘  └────────┬────────┘  │
└─────────┼────────────────┼──────────────────┼───────────┘
          │                │                  │
┌─────────▼────────────────▼──────────────────▼───────────┐
│                   Service Layer                          │
│  ┌────────────┐  ┌───────────────┐  ┌────────────────┐  │
│  │FileScanner │  │ BackupService │  │HashStorageServ.│  │
│  └────────────┘  └───────────────┘  └────────────────┘  │
│  ┌────────────────────┐  ┌─────────────────────────┐    │
│  │DuplicateDetection  │  │ConfigPersistenceService │    │
│  └────────────────────┘  └─────────────────────────┘    │
└─────────────────────────────┬───────────────────────────┘
                              │
┌─────────────────────────────▼───────────────────────────┐
│                    Utility Layer                         │
│  ┌──────────────────────────┐  ┌─────────────────────┐  │
│  │MultiThreadedHashCalculator│  │   FileUtilities    │  │
│  └──────────────────────────┘  └─────────────────────┘  │
└─────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────▼───────────────────────────┐
│                     Model Layer                          │
│      ┌───────────────────┐  ┌─────────────────┐         │
│      │BackupConfiguration│  │   BackupFile    │         │
│      └───────────────────┘  └─────────────────┘         │
└─────────────────────────────────────────────────────────┘
```

---

## 4. File-by-File Documentation

### 📄 Main.java
**Location:** `src/main/java/org/example/Main.java`  
**Lines of Code:** ~55  
**Purpose:** Application entry point and theme initialization

#### Overview
The Main class serves as the bootstrap for the entire application. It initializes the modern FlatLaf dark theme, prints startup diagnostics, and launches the GUI on the Event Dispatch Thread (EDT).

#### Key Constants
```java
private static final String APP_VERSION = "1.0-SNAPSHOT";
private static final String APP_NAME = "Multimedia File Backup Manager";
```

#### What It Does
1. **Prints Startup Banner** - Shows app name, version, Java version, and available CPU cores
2. **Initializes FlatLaf Theme** - Sets up the modern dark UI with custom properties:
   - Rounded buttons (arc: 8px)
   - Rounded progress bars
   - Modern scrollbars without buttons
   - Tab separators enabled
3. **Launches MainWindow** - On the EDT using `SwingUtilities.invokeLater()`
4. **Error Handling** - Shows user-friendly dialog if startup fails

#### UI Customizations Applied
| Property | Value | Effect |
|----------|-------|--------|
| `Button.arc` | 8 | Rounded button corners |
| `Component.arc` | 8 | Rounded component corners |
| `ProgressBar.arc` | 8 | Rounded progress bars |
| `ScrollBar.showButtons` | false | Hide scroll arrows |
| `ScrollBar.thumbArc` | 8 | Rounded scroll thumb |

---

### 📄 BackupConfiguration.java
**Location:** `src/main/java/org/example/model/BackupConfiguration.java`  
**Lines of Code:** ~95  
**Purpose:** Central configuration holder for all backup settings

#### Overview
This model class holds all user-configurable settings. It provides a clean API for managing source directories, sync locations, and operational flags.

#### Properties
| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `masterBackupLocation` | `File` | null | Where backed-up files are stored |
| `sourceDirectories` | `List<File>` | empty | Folders to scan for files |
| `syncLocations` | `List<File>` | empty | Additional backup destinations |
| `includeSubdirectories` | `boolean` | true | Scan nested folders |
| `createDateFolders` | `boolean` | false | Organize by YYYY/MM |
| `hashingThreadCount` | `int` | CPU cores | Parallel thread count |

#### Key Features
- **Immutable List Access** - Returns `Collections.unmodifiableList()` for safety
- **Validation on Add** - Checks if directory exists before adding
- **Thread Count Limits** - Caps at 2x available processors

#### Usage Example
```java
BackupConfiguration config = new BackupConfiguration();
config.setMasterBackupLocation(new File("E:/Backups"));
config.addSourceDirectory(new File("C:/Photos"));
config.setHashingThreadCount(16);
```

---

### 📄 BackupFile.java
**Location:** `src/main/java/org/example/model/BackupFile.java`  
**Lines of Code:** ~120  
**Purpose:** Represents a multimedia file with all its metadata

#### Overview
This immutable-style model wraps a file with its calculated hash, size, modification date, and processing status. It uses cached patterns for performance.

#### Cached Static Fields (Performance Optimization)
```java
private static final Pattern VIDEO_PATTERN = Pattern.compile("mp4|avi|mkv|...");
private static final Pattern IMAGE_PATTERN = Pattern.compile("jpg|jpeg|png|...");
private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
```

#### Properties
| Property | Type | Description |
|----------|------|-------------|
| `sourceFile` | `File` (final) | Original file reference |
| `hash` | `String` (final) | xxHash3 hash value |
| `size` | `long` (final) | File size in bytes |
| `lastModified` | `LocalDateTime` (final) | Last modification date |
| `selected` | `boolean` | Include in backup? |
| `status` | `BackupStatus` | Processing state |
| `existsInMaster` | `boolean` | Already backed up? |

#### BackupStatus Enum
```java
public enum BackupStatus {
    PENDING,      // Not yet processed
    IN_PROGRESS,  // Currently being copied
    COMPLETED,    // Successfully backed up
    ERROR,        // Failed to backup
    DUPLICATE     // Already exists in master
}
```

#### Key Methods
| Method | Returns | Description |
|--------|---------|-------------|
| `getFileName()` | String | File name only |
| `getPath()` | String | Full absolute path |
| `getFormattedSize()` | String | Human-readable (e.g., "1.5 MB") |
| `getFormattedDate()` | String | "yyyy-MM-dd HH:mm:ss" format |
| `isVideo()` | boolean | Matches video extension pattern |
| `isImage()` | boolean | Matches image extension pattern |

---

### 📄 FileScanner.java
**Location:** `src/main/java/org/example/service/FileScanner.java`  
**Lines of Code:** ~140  
**Purpose:** Discovers and catalogs multimedia files from source directories

#### Overview
Extends `SwingWorker<List<BackupFile>, String>` to perform background scanning without freezing the UI. Uses multi-threaded hashing for speed.

#### Callback Interface
```java
public interface ScanProgressCallback {
    void updateProgress(int current, int total, String currentFile);
    void scanCompleted(List<BackupFile> files);
    void scanFailed(String error);
}
```

#### Workflow
1. **Collect Files** - Uses `FileUtilities.collectFilesFromDirectory()` recursively
2. **Calculate Hashes** - Uses `MultiThreadedHashCalculator` for parallel hashing
3. **Detect In-Source Duplicates** - Tracks seen hashes to find duplicates within sources
4. **Create BackupFile Objects** - Wraps each file with metadata
5. **Report Results** - Calls completion callback with results

#### Key Methods
| Method | Description |
|--------|-------------|
| `doInBackground()` | Main scanning logic (runs in background thread) |
| `process()` | Updates progress (runs on EDT) |
| `done()` | Handles completion/failure (runs on EDT) |
| `isCancelled()` | Checks for user cancellation |

#### Performance Metrics Reported
- Total time elapsed
- Throughput in MB/s
- Files processed count
- Errors count

---

### 📄 BackupService.java
**Location:** `src/main/java/org/example/service/BackupService.java`  
**Lines of Code:** ~220  
**Purpose:** Performs actual file copying to master folder

#### Overview
Extends `SwingWorker<Boolean, BackupProgress>` to copy files in the background. Handles disk space validation, file conflicts, and date-based folder creation.

#### Callback Interface
```java
public interface BackupProgressCallback {
    void updateProgress(int current, int total, String currentFile, 
                       long bytesProcessed, long totalBytes);
    void backupCompleted(int successCount, int errorCount);
    void backupFailed(String error, int filesBackedUpBeforeCancellation);
    void fileCompleted(BackupFile file, boolean success, String error);
}
```

#### Key Features
1. **Disk Space Validation** - Checks available space before each copy
2. **Date-Based Folders** - Creates YYYY/MM structure if enabled
3. **File Conflict Resolution** - Adds `_1`, `_2`, etc. suffix if file exists
4. **Attribute Preservation** - Copies file modification times
5. **Cancellation Support** - Can be cancelled mid-operation

#### File Conflict Resolution Logic
```
photo.jpg exists → photo_1.jpg
photo_1.jpg exists → photo_2.jpg
photo_2.jpg exists → photo_3.jpg
... continues until unique name found
```

#### Destination Path Calculation
```java
if (configuration.isCreateDateFolders()) {
    // Creates: master/2024/01/photo.jpg
    String yearFolder = String.valueOf(fileDate.getYear());
    String monthFolder = String.format("%02d", fileDate.getMonthValue());
    File dateFolder = new File(masterLocation, yearFolder + "/" + monthFolder);
}
```

---

### 📄 BackupProgress.java
**Location:** `src/main/java/org/example/service/BackupProgress.java`  
**Lines of Code:** ~30  
**Purpose:** Data transfer object for backup progress information

#### Overview
Simple immutable DTO used by `SwingWorker.publish()` to communicate progress from background thread to EDT.

#### Properties
| Property | Type | Description |
|----------|------|-------------|
| `currentFile` | int | Current file number |
| `totalFiles` | int | Total files to process |
| `fileName` | String | Name of current file |
| `bytesProcessed` | long | Bytes copied so far |
| `totalBytes` | long | Total bytes to copy |

---

### 📄 HashStorageService.java
**Location:** `src/main/java/org/example/service/HashStorageService.java`  
**Lines of Code:** ~320  
**Purpose:** Manages the persistent hash database for master folder

#### Overview
This is a critical service that maintains a JSON database of file hashes for the master backup folder. It enables O(1) duplicate detection by keeping a reverse index.

#### Storage File
- **Name:** `.mfbcm_hashes.json`
- **Location:** Master backup folder root
- **Format:** JSON map of relative path → FileHashInfo

#### Key Data Structures
```java
// Primary storage: relativePath → FileHashInfo
Map<String, FileHashInfo> storedHashes = new ConcurrentHashMap<>();

// Reverse index for O(1) lookups: hash → FileHashInfo
Map<String, FileHashInfo> hashToInfoCache = new ConcurrentHashMap<>();
```

#### FileHashInfo Nested Class
```java
public static class FileHashInfo {
    private String relativePath;
    private String hash;
    private long lastModified;
    private long fileSize;
    // Getters, setters, default constructor for Jackson
}
```

#### ValidationResult Nested Class
Tracks changes during master folder validation:
- `newFiles` - Newly added files
- `modifiedFiles` - Files with changed content
- `deletedFiles` - Files no longer present
- `processingTimeMs` - Time taken
- `throughputMbPerSec` - Hashing speed

#### Key Methods
| Method | Description |
|--------|-------------|
| `validateAndUpdateHashesMultiThreaded()` | Syncs database with actual files |
| `forceRehashMultiThreaded()` | Clears and rebuilds entire database |
| `getHashToInfoMap()` | Returns reverse index for O(1) lookups |
| `loadStoredHashes()` | Loads from JSON on startup |
| `saveStoredHashes()` | Persists to JSON file |

#### Validation Process
1. Scan master folder for current files
2. Compare against stored hashes
3. Identify new files (need hashing)
4. Identify modified files (size/date changed)
5. Hash new/modified files in parallel
6. Remove entries for deleted files
7. Update reverse index
8. Save updated database

---

### 📄 DuplicateDetectionService.java
**Location:** `src/main/java/org/example/service/DuplicateDetectionService.java`  
**Lines of Code:** ~230  
**Purpose:** Finds duplicates between source directories and master folder

#### Overview
Extends `SwingWorker<DuplicateAnalysisResult, String>` to perform comprehensive duplicate analysis in the background.

#### Duplicate Categories
1. **Master Duplicates** - Source files that already exist in master (same hash)
2. **Source Duplicates** - Duplicate files within the source directories themselves

#### Callback Interface
```java
public interface DuplicateDetectionCallback {
    void updateProgress(int current, int total, String currentFile);
    void detectionCompleted(DuplicateAnalysisResult result);
    void detectionFailed(String error);
}
```

#### Workflow
1. Load master folder hashes from `HashStorageService`
2. Collect all multimedia files from source directories
3. Hash source files using multi-threaded calculator
4. Compare each source hash against master hashes
5. Track duplicates within sources using a `Set<String>`
6. Build and return `DuplicateAnalysisResult`

#### Key Methods
| Method | Description |
|--------|-------------|
| `scanSourceDirectories()` | Collects and hashes source files |
| `analyzeDuplicates()` | Categorizes duplicates |
| `doInBackground()` | Main detection logic |

---

### 📄 DuplicateAnalysisResult.java
**Location:** `src/main/java/org/example/service/DuplicateAnalysisResult.java`  
**Lines of Code:** ~85  
**Purpose:** Holds complete results of duplicate analysis

#### Properties
| Property | Type | Description |
|----------|------|-------------|
| `masterFileCount` | int | Files in master folder |
| `sourceFiles` | `List<BackupFile>` | All scanned source files |
| `duplicatesInMaster` | `List<BackupFile>` | Files existing in master |
| `duplicatesInSource` | `List<BackupFile>` | Duplicates within sources |
| `newFiles` | `List<BackupFile>` | Unique new files |
| `duplicatePairs` | `List<DuplicatePair>` | Source-to-master pairs |
| `hashToFilesMap` | `Map<String, List<File>>` | All locations by hash |
| `sourceDuplicateGroups` | `Map<String, List<BackupFile>>` | Grouped source duplicates |
| `processingTimeMs` | long | Analysis duration |
| `throughputMbPerSec` | double | Hashing speed |

#### Computed Properties
```java
getTotalSourceFiles()     // sourceFiles.size()
getDuplicateInMasterCount() // duplicatesInMaster.size()
getDuplicateInSourceCount() // duplicatesInSource.size()
getTotalDuplicateCount()    // master + source duplicates
getNewFileCount()           // newFiles.size()
getFormattedDuration()      // Human-readable time
```

---

### 📄 DuplicatePair.java
**Location:** `src/main/java/org/example/service/DuplicatePair.java`  
**Lines of Code:** ~60  
**Purpose:** Represents a pair of duplicate files (source and master)

#### Properties
| Property | Type | Description |
|----------|------|-------------|
| `sourceFile` | `BackupFile` | The source file |
| `masterFile` | `File` | Matching file in master |
| `masterInfo` | `FileHashInfo` | Master file metadata |
| `allDuplicateLocations` | `List<File>` | All locations of this hash |

#### Key Methods
```java
getSourcePath()               // Full path of source file
getMasterPath()               // Full path of master file
getHash()                     // Common hash value
getDuplicateCount()           // Number of additional locations
getFormattedDuplicateLocations() // Semicolon-separated paths
```

---

### 📄 SyncService.java
**Location:** `src/main/java/org/example/service/SyncService.java`  
**Lines of Code:** ~250  
**Purpose:** Synchronizes master folder to additional backup locations

#### Overview
Extends `SwingWorker<SyncResult, SyncProgress>` to copy master folder contents to one or more sync locations, maintaining identical structure.

#### Callback Interface
```java
public interface SyncProgressCallback {
    void updateProgress(int current, int total, String currentFile, 
                       long bytesProcessed, long totalBytes);
    void syncCompleted(SyncResult result);
    void syncFailed(String error);
}
```

#### Key Features
1. **Multi-Location Sync** - Syncs to multiple destinations
2. **Differential Sync** - Only copies changed files (checks size/date)
3. **Delete Orphans** - Removes files in target that don't exist in source
4. **Skip Hash Files** - Ignores `.mfbcm_hashes.json`
5. **Skip Temp Dirs** - Ignores `.mfbcm_temp` directories

#### Sync Logic
```java
// For each file in target:
if (!Files.exists(sourceFile)) {
    Files.delete(targetFile);  // Remove orphan
}

// For each file in source:
if (!Files.exists(targetFile) || isDifferent(source, target)) {
    Files.copy(source, target, COPY_ATTRIBUTES);
}
```

---

### 📄 SyncProgress.java
**Location:** `src/main/java/org/example/service/SyncProgress.java`  
**Lines of Code:** ~40  
**Purpose:** Progress DTO for sync operations

#### Properties
Same structure as `BackupProgress`:
- `current`, `total` - File counts
- `currentFile` - Current file name
- `bytesProcessed`, `totalBytes` - Byte counts

---

### 📄 SyncResult.java
**Location:** `src/main/java/org/example/service/SyncResult.java`  
**Lines of Code:** ~55  
**Purpose:** Result of a sync operation

#### Properties
| Property | Type | Description |
|----------|------|-------------|
| `successfulLocations` | `List<File>` | Successfully synced locations |
| `failedLocations` | `Map<File, String>` | Failed locations with errors |

#### Methods
```java
getSuccessCount()      // Number of successful syncs
getFailureCount()      // Number of failed syncs
hasFailures()          // Any failures?
isFullSuccess()        // All succeeded?
```

---

### 📄 ConfigurationPersistenceService.java
**Location:** `src/main/java/org/example/service/ConfigurationPersistenceService.java`  
**Lines of Code:** ~150  
**Purpose:** Saves and loads application configuration

#### Storage Location
- **File:** `config.properties`
- **Location:** Application working directory

#### Persisted Settings
| Key | Type | Description |
|-----|------|-------------|
| `masterBackupLocation` | String | Absolute path |
| `sourceDirectories` | String | Pipe-separated paths |
| `includeSubdirectories` | boolean | Flag |
| `createDateFolders` | boolean | Flag |
| `hashingThreadCount` | int | Thread count |
| `version` | String | "1.0" |
| `lastSaved` | long | Timestamp |

#### Config File Format
```properties
#Multimedia File Backup Manager Configuration
#Fri Dec 05 14:37:32 CET 2025
createDateFolders=false
hashingThreadCount=32
includeSubdirectories=true
lastSaved=1764941852684
masterBackupLocation=E\:\\java\\Master
version=1.0
```

#### Key Methods
| Method | Description |
|--------|-------------|
| `saveConfiguration()` | Writes to properties file |
| `loadConfiguration()` | Reads from properties file |
| `hasConfiguration()` | Checks if config file exists |
| `getConfigurationPath()` | Returns config file path |

---

### 📄 MultiThreadedHashCalculator.java
**Location:** `src/main/java/org/example/util/MultiThreadedHashCalculator.java`  
**Lines of Code:** ~290  
**Purpose:** High-performance parallel file hashing using xxHash3

#### Overview
This is the performance-critical utility that calculates file hashes using all available CPU cores. Uses work-stealing algorithm for optimal distribution.

#### Constants
```java
private static final int SMALL_FILE_THRESHOLD_MB = 100;   // Full-file read threshold
private static final int SLOW_FILE_THRESHOLD_MS = 5000;   // Warning threshold
private static final long LARGE_FILE_THRESHOLD_MB = 500;  // Logging threshold
```

#### Thread Pool Strategy
```java
if (threadCount == Runtime.getRuntime().availableProcessors()) {
    // Use shared common pool (more efficient)
    executor = ForkJoinPool.commonPool();
    ownsExecutor = false;  // Don't shut down
} else {
    // Create custom pool with named threads
    executor = Executors.newFixedThreadPool(threadCount, threadFactory);
    ownsExecutor = true;   // Must shut down
}
```

#### Hashing Strategy by File Size
| File Size | Strategy | Reason |
|-----------|----------|--------|
| < 100 MB | Read entire file into memory | Fast, simple |
| ≥ 100 MB | Read 10 × 10MB chunks | Faster, still unique |

#### Large File Chunk Strategy
```java
// For files >= 100MB:
// Read 10 equally-spaced 10MB chunks
final int NUM_CHUNKS = 10;
final int CHUNK_SIZE = 10 * 1024 * 1024;  // 10MB
long spacing = fileSize / NUM_CHUNKS;

for (int i = 0; i < NUM_CHUNKS; i++) {
    long position = i * spacing;
    // Read chunk at position
    // Combine hashes with rotation
    hash = Long.rotateLeft(hash, 1) ^ chunkHash;
}
```

#### Progress Callback Interface
```java
public interface ProgressCallback {
    void onProgress(int current, int total, String currentFile, int errors);
}
```

#### Key Methods
| Method | Description |
|--------|-------------|
| `calculateHashesWithCancellation()` | Main parallel hashing method |
| `calculateFileHash()` | Single file hash calculation |
| `calculateThroughput()` | Performance measurement |
| `shutdown()` | Clean up thread pool |

---

### 📄 FileUtilities.java
**Location:** `src/main/java/org/example/util/FileUtilities.java`  
**Lines of Code:** ~130  
**Purpose:** Common file operations and constants

#### Supported File Extensions
```java
public static final Set<String> MULTIMEDIA_EXTENSIONS = Set.of(
    // Images (18 formats)
    "jpg", "jpeg", "png", "gif", "bmp", "tiff", "tif", "webp", "svg",
    "raw", "cr2", "nef", "dng", "arw", "orf", "rw2", "pef", "srw",
    
    // Videos (18 formats)
    "mp4", "avi", "mkv", "mov", "wmv", "flv", "webm", "m4v", "3gp",
    "mpg", "mpeg", "m2v", "mts", "ts", "vob", "asf", "rm", "rmvb"
);
```

#### Key Methods
| Method | Description |
|--------|-------------|
| `isMultimediaFile(File)` | Checks if file has supported extension |
| `collectFilesFromDirectory()` | Recursive file collection with cancel support |
| `formatDuration(long)` | "45.2s", "1m 30s", "234ms" |
| `formatFileSize(long)` | "1.5 MB", "234 KB", "2.3 GB" |

#### CancelCheck Interface
```java
@FunctionalInterface
public interface CancelCheck {
    boolean isCancelled();
}
```

#### Duration Formatting Logic
```java
if (milliseconds < 1000)      → "234ms"
else if (milliseconds < 60000) → "45.2s"
else                           → "1m 30s"
```

#### Size Formatting Logic
```java
if (bytes < 1024)             → "234 B"
else if (bytes < 1MB)          → "234.5 KB"
else if (bytes < 1GB)          → "234.5 MB"
else                           → "2.34 GB"
```

---

### 📄 MainWindow.java
**Location:** `src/main/java/org/example/gui/MainWindow.java`  
**Lines of Code:** ~980  
**Purpose:** Main application window and controller

#### Overview
This is the central UI component that ties everything together. It extends `JFrame` and implements all callback interfaces to receive updates from background services.

#### Implemented Interfaces
```java
public class MainWindow extends JFrame implements 
    FileScanner.ScanProgressCallback,
    BackupService.BackupProgressCallback,
    DuplicateDetectionService.DuplicateDetectionCallback,
    SyncService.SyncProgressCallback
```

#### UI Components
| Component | Type | Purpose |
|-----------|------|---------|
| `masterLocationLabel` | JLabel | Shows selected master folder |
| `sourceDirectoriesList` | JList<File> | List of source folders |
| `syncLocationsList` | JList<File> | List of sync destinations |
| `scanButton` | JButton | Start/cancel scan |
| `backupButton` | JButton | Start/cancel backup |
| `syncButton` | JButton | Start/cancel sync |
| `viewDuplicatesButton` | JButton | Open duplicate viewer |
| `rescanMasterButton` | JButton | Rebuild hash database |
| `scanProgressBar` | JProgressBar | Scan progress |
| `backupProgressBar` | JProgressBar | Backup progress |
| `syncProgressBar` | JProgressBar | Sync progress |
| `fileListPanel` | FileListPanel | File table |
| `statusLabel` | JLabel | Status messages |

#### Layout Structure
```
┌────────────────────────────────────────────────────┐
│ Configuration Panel                                 │
│ ┌─────────────────────────────────────────────────┐│
│ │ Master Location: [...] [Browse]                 ││
│ │ Source Directories: [list] [Add] [Remove]       ││
│ │ Sync Locations: [list] [Add] [Remove]           ││
│ │ ☑ Subdirs  ☑ Date folders  ☑ Detect duplicates ││
│ │ [Scan] [Backup] [View Duplicates] [Rescan] [Sync]│
│ └─────────────────────────────────────────────────┘│
├────────────────────────────────────────────────────┤
│ FileListPanel (table of files)                      │
├────────────────────────────────────────────────────┤
│ Progress Panel                                      │
│ ┌─────────────────────────────────────────────────┐│
│ │ Scan: [=========>                  ] 45%        ││
│ │ Backup: [                          ] 0%         ││
│ │ Sync: [                            ] 0%         ││
│ └─────────────────────────────────────────────────┘│
├────────────────────────────────────────────────────┤
│ Status: Ready | Files: 0 | Selected: 0             │
└────────────────────────────────────────────────────┘
```

---

### 📄 FileListPanel.java
**Location:** `src/main/java/org/example/gui/FileListPanel.java`  
**Lines of Code:** ~350  
**Purpose:** File list table component with filtering and selection

#### Table Columns
| # | Column | Width | Description |
|---|--------|-------|-------------|
| 0 | ☐ | 40px | Checkbox for selection |
| 1 | Name | 250px | File name |
| 2 | Path | 350px | Full file path |
| 3 | Size | 90px | Formatted size |
| 4 | Date Modified | 140px | Formatted date |
| 5 | Type | 80px | "Image" or "Video" |
| 6 | Status | 100px | Colored status |

#### Filter Options
- All, Images, Videos, Selected, Duplicates

---

### 📄 DuplicateViewerWindow.java
**Location:** `src/main/java/org/example/gui/DuplicateViewerWindow.java`  
**Lines of Code:** ~450  
**Purpose:** Dialog for viewing and managing duplicate files

#### Tabs
1. **Master Duplicates** - Files already in master folder
2. **Source Duplicates** - Duplicates within source directories
3. **Summary** - Statistics and space calculations

---

## 5. Architecture & Design Patterns

### Design Patterns Used

#### 1. Model-View-Controller (MVC)
- **View:** MainWindow, FileListPanel, DuplicateViewerWindow
- **Controller:** Service classes (FileScanner, BackupService, etc.)
- **Model:** BackupConfiguration, BackupFile

#### 2. Observer Pattern (Callbacks)
Each service defines callback interfaces that MainWindow implements.

#### 3. SwingWorker Pattern
All long-running operations extend SwingWorker for background processing.

#### 4. Strategy Pattern
Different hashing strategies based on file size (< 100MB vs >= 100MB).

---

## 6. Data Flow Diagrams

### Scanning Flow
```
User clicks "Scan" → FileScanner.execute()
    → collectFilesFromDirectory() 
    → MultiThreadedHashCalculator.calculateHashes()
    → Create BackupFile objects
    → FileListPanel.setFiles()
```

### Backup Flow
```
User clicks "Backup" → BackupService.execute()
    → For each selected file:
        → calculateDestinationPath()
        → Check disk space
        → handleFileNameConflict()
        → Files.copy()
    → HashStorageService.validateAndUpdate()
```

---

## 7. Configuration System

### Files Overview
| File | Purpose |
|------|---------|
| `build.gradle` | Build configuration, dependencies |
| `config.properties` | Runtime user settings |
| `.mfbcm_hashes.json` | Hash database (in master folder) |

---

## 8. GUI Components

### Color Scheme
| Element | RGB |
|---------|-----|
| Main Background | (45, 49, 57) |
| Panel Background | (40, 44, 52) |
| Input Background | (50, 54, 62) |
| Primary Text | (200, 200, 200) |

### Accent Colors
| Purpose | RGB |
|---------|-----|
| Primary (Blue) | (52, 152, 219) |
| Success (Green) | (46, 204, 113) |
| Warning (Orange) | (230, 126, 34) |
| Error (Red) | (231, 76, 60) |

---

## 9. Key Algorithms

### xxHash3 Hashing
- Speed: 22+ GB/s on modern CPUs
- Files < 100MB: Read entire file
- Files >= 100MB: Read 10 × 10MB chunks

### O(1) Duplicate Detection
Uses reverse hash index (`hashToInfoCache`) for instant lookups.

---

## 10. Performance Optimizations

| Area | Improvement |
|------|-------------|
| Hash Lookups | O(n) → O(1) (1000x faster) |
| File Type Checks | Cached regex patterns |
| Thread Pool | ForkJoinPool work-stealing |
| Large Files | Chunked reading strategy |

---

## 11. Development Guide

### Building
```cmd
.\gradlew clean build    # Full build
.\gradlew run            # Run app
.\gradlew jar            # Create JAR
.\gradlew test           # Run tests
```

### Adding Features
1. **New File Type:** Edit `FileUtilities.MULTIMEDIA_EXTENSIONS`
2. **New Service:** Extend `SwingWorker`, create callback interface
3. **New Config Option:** Add to `BackupConfiguration`, update UI

---

## 12. Troubleshooting

### Common Issues

| Issue | Solution |
|-------|----------|
| Native access warnings | Use `--enable-native-access=ALL-UNNAMED` |
| Hash loading fails | Delete `.mfbcm_hashes.json`, rescan |
| Slow performance | Increase thread count, use SSD |
| Memory issues | Add `-Xmx4g` JVM argument |

---

## 13. Future Enhancements

- [ ] Custom theme colors
- [ ] File preview thumbnails
- [ ] Scheduled automatic backups
- [ ] Cloud storage integration
- [ ] Comprehensive unit tests

---

## Quick Reference

### Keyboard Shortcuts
| Shortcut | Action |
|----------|--------|
| Double-click | Toggle file selection |

### File Status Colors
| Color | Status |
|-------|--------|
| 🟢 Green | Completed |
| 🔵 Blue | In Progress |
| 🟠 Orange | Duplicate |
| 🔴 Red | Error |
| ⚪ Gray | Pending |

---

*Document Version: 2.0 | Generated: December 2025*
