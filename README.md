# Multimedia File Backup Manager (MFBCM)

A modern, high-performance Java application for managing backups of multimedia files (photos and videos) with advanced duplicate detection.

## Features

- **Modern Dark Theme GUI** - Professional FlatLaf dark theme interface
- **Smart Duplicate Detection** - Ultra-fast xxHash3 algorithm for detecting duplicates
- **Multi-threaded Hashing** - Leverages all CPU cores for maximum performance
- **Master Folder Management** - Maintains a hash database of your master backup
- **Flexible Source Selection** - Add multiple source directories to scan
- **Visual Feedback** - Progress bars, status indicators, and detailed statistics
- **Duplicate Viewer** - Dedicated window to review and manage duplicates
- **Automatic Refresh** - Master folder updates automatically after backups

## System Requirements

- **Java**: Version 11 or higher (tested with Java 24.0.1)
- **OS**: Windows, macOS, or Linux
- **RAM**: 2GB minimum, 4GB+ recommended for large libraries
- **Disk**: Sufficient space for backups

## Quick Start

### 1. Build the Application

```cmd
.\gradlew clean build
```

### 2. Run the Application

**Option A: Using the batch file (Windows)**
```cmd
run.bat
```

**Option B: Using Gradle**
```cmd
.\gradlew run
```

**Option C: Direct JAR execution**
```cmd
java --enable-native-access=ALL-UNNAMED -jar build\libs\MFBCM-1.0-SNAPSHOT.jar
```

## Usage Guide

### Setting Up

1. **Select Master Backup Location**
   - Click "Browse..." next to Master Backup Location
   - Choose the folder where backed-up files will be stored
   - The application creates a hash database in this folder

2. **Add Source Directories**
   - Click "Add..." under Source Directories
   - Select folders containing photos/videos to backup
   - Add as many source folders as needed
   - Remove sources with "Remove" button

### Scanning for Files

1. **Configure Options**
   - ☑ Include subdirectories - Scans nested folders
   - ☑ Create date-based folders - Organizes by date
   - ☑ Detect duplicates - Checks against master folder
   - Hash threads: Set CPU threads to use (default: all cores)

2. **Start Scan**
   - Click "Scan for Files"
   - Progress bar shows current file being processed
   - Found files appear in the table below
   - Duplicates are automatically unchecked

### Managing Files

- **Select/Deselect Files**: Use checkbox in first column
- **Select All**: Use "Select All" checkbox above table
- **Filter View**: Use dropdown to filter by type or status
- **Double-click**: Toggle selection on any file

### Backing Up

1. Select files to backup (checked items)
2. Click "Start Backup"
3. Confirm the operation
4. Monitor progress in backup progress bar
5. Master folder refreshes automatically after completion

### Viewing Duplicates

1. After scanning with duplicate detection enabled
2. Click "View Duplicates" button
3. Three tabs show different duplicate categories:
   - **Master Duplicates**: Files already in master folder
   - **Source Duplicates**: Duplicate files within sources
   - **Summary**: Statistics and space savings

### Rescanning Master Folder

- Click "Rescan Master" to rebuild the hash database
- Useful after manual changes to master folder
- Updates automatically after each backup

## Technical Details

### Hash Algorithm
- **xxHash3**: Ultra-fast non-cryptographic hash
- **Performance**: 22+ GB/s throughput on modern hardware
- **Collision Resistance**: Suitable for file deduplication

### File Support
- **Images**: jpg, jpeg, png, gif, bmp, tiff, webp, heic
- **Videos**: mp4, avi, mov, mkv, wmv, flv, m4v, mpg, mpeg

### Multi-threading
- Parallel hash calculation across multiple CPU cores
- Configurable thread count (1 to 2x CPU cores)
- Optimized work distribution

### Storage
- Hash database stored as JSON (.mfbcm_hashes.json)
- Configuration persisted in user app data
- Relative paths for portability

## Architecture

```
MFBCM
├── GUI Layer (Swing + FlatLaf)
│   ├── MainWindow - Main application interface
│   ├── FileListPanel - File list with controls
│   └── DuplicateViewerWindow - Duplicate management
│
├── Service Layer
│   ├── FileScanner - Discovers multimedia files
│   ├── BackupService - Copies files to master
│   ├── HashStorageService - Manages hash database
│   ├── DuplicateDetectionService - Finds duplicates
│   └── ConfigurationPersistenceService - Saves settings
│
├── Utility Layer
│   ├── MultiThreadedHashCalculator - Parallel hashing
│   └── FileUtilities - File operations
│
└── Model Layer
    ├── BackupConfiguration - App settings
    ├── BackupFile - File metadata
    └── DuplicatePair - Duplicate file info
```

## Color Scheme

The modern dark theme uses carefully selected colors:

- **Backgrounds**: Dark grays (40-60 RGB range)
- **Text**: Light grays (180-220 RGB range)
- **Accents**:
  - Blue (52, 152, 219) - Primary actions
  - Green (46, 204, 113) - Success/New files
  - Orange (230, 126, 34) - Warnings/Duplicates
  - Red (231, 76, 60) - Errors/Delete
  - Purple (155, 89, 182) - View actions

## Performance Tips

1. **Thread Count**: Use all available cores for best performance
2. **Large Libraries**: Expect initial scan to take time; subsequent scans are much faster
3. **SSD Storage**: Store master folder on SSD for faster hashing
4. **Memory**: Increase JVM heap for very large libraries (10,000+ files)

## Troubleshooting

### Application Won't Start
- Ensure Java 11+ is installed
- Run with native access flag: `--enable-native-access=ALL-UNNAMED`
- Check console for error messages

### Hash Loading Fails
- Delete `.mfbcm_hashes.json` in master folder
- Click "Rescan Master" to rebuild database

### Slow Performance
- Increase thread count to match CPU cores
- Check disk I/O (especially on HDDs)
- Close other resource-intensive applications

### Duplicates Not Detected
- Enable "Detect duplicates" checkbox before scanning
- Ensure master folder has been scanned at least once
- Check that files actually match (same content)

## Development

### Building from Source

```cmd
# Clean and build
.\gradlew clean build

# Run tests
.\gradlew test

# Create JAR only
.\gradlew jar

# Run without building JAR
.\gradlew run
```

### Dependencies

- **FlatLaf 3.5.2** - Modern Look and Feel
- **Jackson 2.18.2** - JSON serialization
- **Commons IO 2.18.0** - File utilities
- **Commons Codec 1.17.1** - Encoding utilities
- **Zero Allocation Hashing 0.27ea1** - xxHash3 implementation

### Project Structure

```
src/
├── main/
│   ├── java/org/example/
│   │   ├── Main.java
│   │   ├── gui/ - UI components
│   │   ├── service/ - Business logic
│   │   ├── model/ - Data models
│   │   └── util/ - Utilities
│   └── resources/
└── reports/ - Development documentation
```

## Known Issues

- Java 24+ shows native access warnings (suppressed with flag)
- Very large video files (>10GB) may take time to hash
- UI scaling on high-DPI displays may need adjustment

## Future Enhancements

- [ ] Custom theme colors
- [ ] File preview thumbnails
- [ ] Advanced filtering options
- [ ] Export/import configuration
- [ ] Scheduled automatic backups
- [ ] Cloud storage integration
- [ ] File integrity verification

## License

This is a personal project. Feel free to use and modify as needed.

## Credits

Developed as a modern solution for managing multimedia file backups with emphasis on performance, duplicate detection, and user experience.

## Version History

### v1.0-SNAPSHOT (Current)
- Modern dark theme GUI (FlatLaf)
- Multi-threaded xxHash3 hashing
- Advanced duplicate detection
- Responsive, scalable interface
- Configuration persistence
- Comprehensive error handling

---

**Note**: For detailed technical documentation, see the `src/reports/` directory.

