# System Dashboard

A real-time system monitoring dashboard built with Java and C, providing detailed system metrics through a clean and modern interface.

## Features

- **Real-time System Information**
  - Operating System details (Name, Version, Architecture)
  - Hostname
  - System uptime

- **CPU Statistics**
  - Total CPU usage
  - Per-core CPU usage
  - Number of CPU cores
  - Active process count

- **Memory Statistics**
  - Physical memory usage (Total, Used, Free)
  - Memory usage percentage
  - Swap memory usage and percentage

- **Process Information**
  - Total number of running processes
  - Top 3 processes by CPU usage

- **Storage Statistics**
  - Root partition details
  - Total, used, and free space
  - Usage percentage

- **Network Statistics**
  - IP address
  - MAC address
  - Network traffic (Received/Transmitted)
  - Real-time traffic monitoring

- **Battery Information** (if available)
  - Battery level percentage
  - Charging status

- **User Interface Features**
  - Dark/Light theme toggle
  - Configurable refresh rate (1s, 2s, 5s)
  - Clean and organized layout
  - Real-time updates

## Requirements

- Linux operating system
- JDK 21 or higher
- GCC compiler
- Make utility

## Building and Running

1. Clone the repository:
   ```bash
   git clone <repository-url>
   cd SystemDashboard
   ```

2. Build the native library:
   ```bash
   cd src/systemdashboard
   make clean
   make
   cd ..
   ```

3. Compile Java code:
   ```bash
   javac systemdashboard/SystemMonitor.java
   ```

4. Run the application:
   ```bash
   java -Djava.library.path=systemdashboard systemdashboard.SystemMonitor
   ```

## Project Structure

```
SystemDashboard/
├── src/
│   └── systemdashboard/
│       ├── SystemMonitor.java    # Main Java application
│       ├── systeminfo.c         # Native C implementation
│       ├── systeminfo.h         # JNI header file
│       └── Makefile            # Build configuration
└── README.md
```

## Implementation Details

- **Frontend**: Java AWT/Swing for the graphical interface
- **Backend**: Native C code via JNI for system metrics
- **Metrics Collection**: Direct system calls and proc filesystem access
- **Update Mechanism**: Background thread with configurable refresh rate

## Features in Detail

### System Metrics
- CPU usage calculated from /proc/stat
- Memory information from sysinfo
- Disk statistics via statvfs
- Network traffic from /proc/net/dev
- Process information from /proc filesystem
- Battery status from /sys/class/power_supply

### User Interface
- Organized panel layout
- Responsive design
- Theme support with proper contrast
- Interactive controls
- Real-time updates

## Notes

- The application requires root partition access for disk statistics
- Network statistics exclude loopback interface
- Battery information is only available on systems with battery
- Process CPU usage is calculated based on process time

## License

[Your License Here]

## Contributing

Contributions are welcome! Please feel free to submit pull requests. 