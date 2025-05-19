# System Dashboard Implementation Tutorial

## Project Overview
A real-time system monitoring dashboard built with Java and C, providing detailed system metrics through a clean and modern interface.

## Implementation Details

### 1. Java Frontend (SystemMonitor.java)
The frontend is implemented using Java AWT/Swing and provides:
- Real-time system metrics display
- Dark/Light theme support
- Configurable refresh rates
- Multiple metric panels

#### Key Components:
```java
public class SystemMonitor {
    // GUI Components
    private JFrame mainFrame;
    private JPanel systemPanel;    // System info
    private JPanel cpuPanel;       // CPU metrics
    private JPanel memoryPanel;    // Memory stats
    private JPanel diskPanel;      // Storage info
    private JPanel networkPanel;   // Network stats
    private JPanel processPanel;   // Process info
    private JPanel batteryPanel;   // Battery status
    private JPanel controlPanel;   // Controls
}
```

### 2. C Backend (systeminfo.c)
The backend uses native C code to gather system metrics:

#### System Metrics Collection:
```c
// CPU Usage
static void read_cpu_stats(long long *idle_time, long long *total_time) {
    // Reads /proc/stat for CPU statistics
}

// Memory Information
JNIEXPORT jlong JNICALL Java_systemdashboard_SystemMonitor_getTotalMemory
  (JNIEnv *env, jobject obj) {
    // Uses sysinfo to get memory statistics
}
```

## Implementation Steps

1. **Project Setup**:
   ```bash
   mkdir SystemDashboard
   cd SystemDashboard
   mkdir -p src/systemdashboard
   ```

2. **Java Implementation**:
   - Create SystemMonitor.java
   - Implement GUI components
   - Add native method declarations
   - Implement metric update logic

3. **C Implementation**:
   - Create systeminfo.c
   - Implement JNI methods
   - Add system metric collection
   - Handle error cases

4. **Building and Running**:
   ```bash
   # Compile Java code
   javac systemdashboard/SystemMonitor.java

   # Build native library
   cd systemdashboard
   make clean
   make

   # Run application
   cd ..
   java -Djava.library.path=systemdashboard systemdashboard.SystemMonitor
   ```

## Features

1. **System Information**:
   - OS details (name, version, architecture)
   - Hostname
   - System uptime

2. **CPU Statistics**:
   - Total CPU usage
   - Per-core usage
   - Core count
   - Process count

3. **Memory Statistics**:
   - Physical memory usage
   - Swap usage
   - Usage percentages

4. **Storage Information**:
   - Disk space usage
   - Free space
   - Usage percentage

5. **Network Statistics**:
   - IP address
   - MAC address
   - Network traffic

6. **Process Information**:
   - Running process count
   - Top CPU-consuming processes

7. **Battery Status**:
   - Battery level
   - Charging status

## Code Structure

### Java Code Organization:
1. GUI Components
2. Native Method Declarations
3. Metric Update Logic
4. Theme Management
5. Event Handling

### C Code Organization:
1. Helper Functions
2. System Metric Collection
3. JNI Implementation
4. Error Handling

## Technical Details

### JNI Integration:
```java
// Java side
private native double getCpuUsage();
private native long getTotalMemory();
```

```c
// C side
JNIEXPORT jdouble JNICALL Java_systemdashboard_SystemMonitor_getCpuUsage
  (JNIEnv *env, jobject obj) {
    // Implementation
}
```

### System Monitoring:
1. CPU Usage:
   - Reads /proc/stat
   - Calculates usage percentage
   - Updates in real-time

2. Memory Usage:
   - Uses sysinfo struct
   - Monitors RAM and swap
   - Calculates percentages

3. Disk Space:
   - Uses statvfs
   - Monitors root partition
   - Calculates usage

4. Network:
   - Reads /proc/net/dev
   - Monitors traffic
   - Excludes loopback

## Troubleshooting

Common issues and solutions:

1. **Library Loading**:
   ```bash
   # Ensure correct library path
   java -Djava.library.path=systemdashboard
   ```

2. **Compilation**:
   ```bash
   # Clean build
   make clean
   make
   ```

3. **Permission Issues**:
   - Ensure read access to /proc
   - Check file permissions

## Future Enhancements

1. **Additional Metrics**:
   - GPU monitoring
   - Network quality
   - Temperature sensors

2. **UI Improvements**:
   - Graphs and charts
   - Custom themes
   - Layout customization

3. **Performance**:
   - Optimized metric collection
   - Reduced system impact
   - Better memory management

## Contributing

1. Fork the repository
2. Create feature branch
3. Commit changes
4. Push to branch
5. Create Pull Request

## License

[Your License Here]

## Contact

[Your Contact Information] 