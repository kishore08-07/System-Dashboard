#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

//System & Filesystem Info
#include <sys/sysinfo.h>
#include <sys/statvfs.h>
#include <sys/utsname.h>
#include <unistd.h>
#include <dirent.h>

//Network Information
#include <ifaddrs.h>
#include <netdb.h>
#include <net/if.h>
#include <sys/ioctl.h> //e.g., IP, MAC.
#include <linux/wireless.h>
#include <arpa/inet.h> //Functions for converting IP addresses between binary and text

//User Info
#include <sys/types.h>
#include <pwd.h>
#include "systemdashboard_SystemMonitor.h"

#define MAX_CPU_CORES 32
#define MAX_PROCESSES 3  // top 3 memory- or CPU-consuming processes
#define PROC_PATH "/proc"  /// stat (CPU usage) [pid]/status (per-process info) net/dev (network stats)
#define BATTERY_PATH "/sys/class/power_supply/BAT0"
#define MIN(a,b) ((a) < (b) ? (a) : (b))

// Helper function to read CPU stats from /proc/stat
static void read_cpu_stats(long long *idle_time, long long *total_time) {
    FILE *fp = fopen("/proc/stat", "r");
    if (fp == NULL) return;

    char line[256];
    if (fgets(line, sizeof(line), fp) != NULL) {
        long long user, nice, system, idle, iowait, irq, softirq, steal;
        sscanf(line, "cpu %lld %lld %lld %lld %lld %lld %lld %lld",
               &user, &nice, &system, &idle, &iowait, &irq, &softirq, &steal);
        
        *idle_time = idle + iowait;
        *total_time = user + nice + system + idle + iowait + irq + softirq + steal;
    }
    fclose(fp);
}

// Helper function to read individual CPU core stats
static void read_cpu_core_stats(int core, long long *idle_time, long long *total_time) {
    FILE *fp = fopen("/proc/stat", "r");
    if (fp == NULL) return;

    char line[256];
    char cpu_label[10];
    snprintf(cpu_label, sizeof(cpu_label), "cpu%d", core);

    while (fgets(line, sizeof(line), fp) != NULL) {
        if (strncmp(line, cpu_label, strlen(cpu_label)) == 0) {
            long long user, nice, system, idle, iowait, irq, softirq, steal;
            sscanf(line, "%*s %lld %lld %lld %lld %lld %lld %lld %lld",
                   &user, &nice, &system, &idle, &iowait, &irq, &softirq, &steal);
            
            *idle_time = idle + iowait;
            *total_time = user + nice + system + idle + iowait + irq + softirq + steal;
            break;
        }
    }
    fclose(fp);
}

// Helper function to create Java string
jstring create_jstring(JNIEnv *env, const char *str) {
    return (*env)->NewStringUTF(env, str ? str : "N/A");
}

// Helper function to read a file content
char* read_file_content(const char *path) {
    FILE *fp = fopen(path, "r");
    if (!fp) return NULL;
    
    char *buffer = malloc(256);
    size_t bytes_read = fread(buffer, 1, 255, fp);
    fclose(fp);
    
    if (bytes_read > 0) {
        buffer[bytes_read] = '\0';
        return buffer;
    }
    
    free(buffer);
    return NULL;
}

JNIEXPORT jint JNICALL Java_systemdashboard_SystemMonitor_getCpuCores
  (JNIEnv *env, jobject obj) {
    return sysconf(_SC_NPROCESSORS_ONLN);
}

JNIEXPORT jdoubleArray JNICALL Java_systemdashboard_SystemMonitor_getPerCpuUsage
  (JNIEnv *env, jobject obj) {
    static long long prev_idle_time[MAX_CPU_CORES] = {0};
    static long long prev_total_time[MAX_CPU_CORES] = {0};
    int num_cores = sysconf(_SC_NPROCESSORS_ONLN);
    
    jdoubleArray result = (*env)->NewDoubleArray(env, num_cores);
    jdouble cpu_usage[MAX_CPU_CORES];

    for (int i = 0; i < num_cores; i++) {
        long long idle_time, total_time;
        read_cpu_core_stats(i, &idle_time, &total_time);

        if (prev_total_time[i] == 0) {
            cpu_usage[i] = 0.0;
        } else {
            long long idle_diff = idle_time - prev_idle_time[i];
            long long total_diff = total_time - prev_total_time[i];
            cpu_usage[i] = 100.0 * (1.0 - ((double)idle_diff / total_diff));
        }

        prev_idle_time[i] = idle_time;
        prev_total_time[i] = total_time;
    }

    (*env)->SetDoubleArrayRegion(env, result, 0, num_cores, cpu_usage);
    return result;
}
//total amount of swap memory from sysinfo()
JNIEXPORT jlong JNICALL Java_systemdashboard_SystemMonitor_getSwapTotal
  (JNIEnv *env, jobject obj) {
    struct sysinfo si;
    if (sysinfo(&si) == 0) {
        return (jlong)si.totalswap;
    }
    return 0;
}
//currently free swap memory available from sysinfo()
JNIEXPORT jlong JNICALL Java_systemdashboard_SystemMonitor_getSwapFree
  (JNIEnv *env, jobject obj) {
    struct sysinfo si;
    if (sysinfo(&si) == 0) {
        return (jlong)si.freeswap;
    }
    return 0;
}

// total number of bytes received by all network interfaces from /proc/net/dev
JNIEXPORT jlong JNICALL Java_systemdashboard_SystemMonitor_getNetworkBytesReceived
  (JNIEnv *env, jobject obj) {
    FILE *fp = fopen("/proc/net/dev", "r");
    if (fp == NULL) return 0;

    char line[256];
    long long total_bytes = 0;
    
    // Skip header lines
    fgets(line, sizeof(line), fp);
    fgets(line, sizeof(line), fp);

    while (fgets(line, sizeof(line), fp)) {
        char iface[32];
        long long bytes;
        sscanf(line, "%s %lld", iface, &bytes);
        if (strcmp(iface, "lo:") != 0) { // Skip loopback
            total_bytes += bytes;
        }
    }
    fclose(fp);
    return (jlong)total_bytes;
}

// total number of bytes sent/transmitted by all network interfaces from /proc/net/dev
JNIEXPORT jlong JNICALL Java_systemdashboard_SystemMonitor_getNetworkBytesTransmitted
  (JNIEnv *env, jobject obj) {
    FILE *fp = fopen("/proc/net/dev", "r");
    if (fp == NULL) return 0;

    char line[256];
    long long total_bytes = 0;
    
    // Skip header lines
    fgets(line, sizeof(line), fp);
    fgets(line, sizeof(line), fp);

    while (fgets(line, sizeof(line), fp)) {
        char iface[32];
        long long trans;
        sscanf(line, "%s %*s %*s %*s %*s %*s %*s %*s %*s %lld", iface, &trans);
        if (strcmp(iface, "lo:") != 0) { // Skip loopback
            total_bytes += trans;
        }
    }
    fclose(fp);
    return (jlong)total_bytes;
}

//number of currently running processes from /proc
JNIEXPORT jint JNICALL Java_systemdashboard_SystemMonitor_getProcessCount
  (JNIEnv *env, jobject obj) {
    DIR *dir = opendir("/proc");
    if (dir == NULL) return 0;

    int count = 0;
    struct dirent *entry;
    while ((entry = readdir(dir)) != NULL) {
        // Check if the directory name is a number (PID)
        if (entry->d_type == DT_DIR) {
            char *endptr;
            strtol(entry->d_name, &endptr, 10);
            if (*endptr == '\0') {
                count++;
            }
        }
    }
    closedir(dir);
    return count;
}

//system uptime in seconds since the last boot from sysinfo() to access si.uptime.
JNIEXPORT jlong JNICALL Java_systemdashboard_SystemMonitor_getSystemUptime
  (JNIEnv *env, jobject obj) {
    struct sysinfo si;
    if (sysinfo(&si) == 0) {
        return (jlong)si.uptime;
    }
    return 0;
}

//total CPU usage percentage across all cores.
JNIEXPORT jdouble JNICALL Java_systemdashboard_SystemMonitor_getCpuUsage
  (JNIEnv *env, jobject obj) {
    static long long prev_idle_time = 0;
    static long long prev_total_time = 0;
    long long idle_time, total_time;
    
    read_cpu_stats(&idle_time, &total_time);
    
    if (prev_total_time == 0) {
        prev_idle_time = idle_time;
        prev_total_time = total_time;
        return 0.0;
    }
    
    long long idle_diff = idle_time - prev_idle_time;
    long long total_diff = total_time - prev_total_time;

    //Calculates the difference between current and previous idle/total times.
    double cpu_usage = 100.0 * (1.0 - ((double)idle_diff / total_diff));
    
    prev_idle_time = idle_time;
    prev_total_time = total_time;
    
    return cpu_usage;
}

//total physical RAM in bytes from sysinfo() struct’s totalram field.
JNIEXPORT jlong JNICALL Java_systemdashboard_SystemMonitor_getTotalMemory
  (JNIEnv *env, jobject obj) {
    struct sysinfo si;
    if (sysinfo(&si) == 0) {
        return (jlong)si.totalram;
    }
    return 0;
}

//free physical RAM available (not used) in bytes from sysinfo() struct’s freeram field.
JNIEXPORT jlong JNICALL Java_systemdashboard_SystemMonitor_getFreeMemory
  (JNIEnv *env, jobject obj) {
    struct sysinfo si;
    if (sysinfo(&si) == 0) {
        return (jlong)si.freeram;
    }
    return 0;
}

//total disk space in bytes on the root (/) filesystem from statvfs() on / to read file system stats.
JNIEXPORT jlong JNICALL Java_systemdashboard_SystemMonitor_getTotalDiskSpace
  (JNIEnv *env, jobject obj) {
    struct statvfs buf;
    if (statvfs("/", &buf) == 0) {
        return (jlong)buf.f_blocks * buf.f_frsize;
    }
    return 0;
}

// free disk space in bytes on the root (/) filesystem from statvfs() and calculates:
JNIEXPORT jlong JNICALL Java_systemdashboard_SystemMonitor_getFreeDiskSpace
  (JNIEnv *env, jobject obj) {
    struct statvfs buf;
    if (statvfs("/", &buf) == 0) {
        return (jlong)buf.f_bfree * buf.f_frsize;
    }
    return 0;
}

//Uses uname() system call to fill a utsname struct .sysname
JNIEXPORT jstring JNICALL Java_systemdashboard_SystemMonitor_getOsName
  (JNIEnv *env, jobject obj) {
    struct utsname system_info;
    if (uname(&system_info) == 0) {
        return create_jstring(env, system_info.sysname);
    }
    return create_jstring(env, "Unknown");
}

//Uses uname() again returns .release field
JNIEXPORT jstring JNICALL Java_systemdashboard_SystemMonitor_getOsVersion
  (JNIEnv *env, jobject obj) {
    struct utsname system_info;
    if (uname(&system_info) == 0) {
        return create_jstring(env, system_info.release);
    }
    return create_jstring(env, "Unknown");
}

//Uses uname() again returns .machine field
JNIEXPORT jstring JNICALL Java_systemdashboard_SystemMonitor_getOsArch
  (JNIEnv *env, jobject obj) {
    struct utsname system_info;
    if (uname(&system_info) == 0) {
        return create_jstring(env, system_info.machine);
    }
    return create_jstring(env, "Unknown");
}

JNIEXPORT jstring JNICALL Java_systemdashboard_SystemMonitor_getHostname
  (JNIEnv *env, jobject obj) {
    char hostname[256];
    if (gethostname(hostname, sizeof(hostname)) == 0) {
        return create_jstring(env, hostname);
    }
    return create_jstring(env, "Unknown");
}

//Uses getifaddrs() to get the linked list of network interfaces.
//Iterates through interfaces looking for:
//IPv4 addresses (AF_INET)
//Calls getnameinfo() with NI_NUMERICHOST to get the IP address string.
//Returns the first matching IP as a Java string.
JNIEXPORT jstring JNICALL Java_systemdashboard_SystemMonitor_getIpAddress
  (JNIEnv *env, jobject obj) {
    struct ifaddrs *ifaddr, *ifa;
    char host[NI_MAXHOST];
    
    if (getifaddrs(&ifaddr) == -1) {
        return create_jstring(env, "Unknown");
    }
    
    // Look for the first non-loopback IPv4 address
    for (ifa = ifaddr; ifa != NULL; ifa = ifa->ifa_next) {
        if (ifa->ifa_addr == NULL) continue;
        
        if (ifa->ifa_addr->sa_family == AF_INET && 
            !(ifa->ifa_flags & IFF_LOOPBACK)) {
            int s = getnameinfo(ifa->ifa_addr, sizeof(struct sockaddr_in),
                              host, NI_MAXHOST, NULL, 0, NI_NUMERICHOST);
            if (s == 0) {
                freeifaddrs(ifaddr);
                return create_jstring(env, host);
            }
        }
    }

    // free the interface list with freeifaddrs() to avoid memory leaks.
    freeifaddrs(ifaddr);
    return create_jstring(env, "Not available");
}

//uses getifaddrs() to list network interfaces.
//  Skips loopback interfaces.
//  For each interface, opens a socket and uses ioctl() with SIOCGIFHWADDR to get the MAC address.
//  Formats the MAC address as a hex string ("xx:xx:xx:xx:xx:xx").
//  Returns the first valid MAC address found as a Java string.
JNIEXPORT jstring JNICALL Java_systemdashboard_SystemMonitor_getMacAddress
  (JNIEnv *env, jobject obj) {
    struct ifaddrs *ifaddr, *ifa;
    char mac_addr[18] = {0};
    
    if (getifaddrs(&ifaddr) == -1) {
        return create_jstring(env, "Unknown");
    }
    
    for (ifa = ifaddr; ifa != NULL; ifa = ifa->ifa_next) {
        if (ifa->ifa_addr == NULL) continue;
        
        // Skip loopback
        if (!(ifa->ifa_flags & IFF_LOOPBACK)) {
            int fd = socket(AF_INET, SOCK_DGRAM, 0);
            if (fd != -1) {
                struct ifreq ifr;
                strcpy(ifr.ifr_name, ifa->ifa_name);
                if (ioctl(fd, SIOCGIFHWADDR, &ifr) == 0) {
                    unsigned char *mac = (unsigned char *)ifr.ifr_hwaddr.sa_data;
                    snprintf(mac_addr, sizeof(mac_addr), "%02x:%02x:%02x:%02x:%02x:%02x",
                            mac[0], mac[1], mac[2], mac[3], mac[4], mac[5]);
                    close(fd);
                    freeifaddrs(ifaddr);
                    return create_jstring(env, mac_addr);
                }
                close(fd);
            }
        }
    }

// free the interface list with freeifaddrs() to avoid memory leaks.
    freeifaddrs(ifaddr);
    return create_jstring(env, "Not available");
}

JNIEXPORT jboolean JNICALL Java_systemdashboard_SystemMonitor_hasBattery
  (JNIEnv *env, jobject obj) {
    DIR *dir = opendir(BATTERY_PATH);
    if (dir) {
        closedir(dir);
        return JNI_TRUE;
    }
    return JNI_FALSE;
}

JNIEXPORT jint JNICALL Java_systemdashboard_SystemMonitor_getBatteryLevel
  (JNIEnv *env, jobject obj) {
    char path[256];
    snprintf(path, sizeof(path), "%s/capacity", BATTERY_PATH);
    
    char *content = read_file_content(path);
    if (content) {
        int level = atoi(content);
        free(content);
        return level;
    }
    return 0;
}

JNIEXPORT jboolean JNICALL Java_systemdashboard_SystemMonitor_isBatteryCharging
  (JNIEnv *env, jobject obj) {
    char path[256];
    snprintf(path, sizeof(path), "%s/status", BATTERY_PATH);
    
    char *content = read_file_content(path);
    if (content) {
        jboolean is_charging = (strstr(content, "Charging") != NULL);
        free(content);
        return is_charging;
    }
    return JNI_FALSE;
}

typedef struct {
    char name[256];
    float cpu_usage;
} ProcessInfo;

static int compare_processes(const void *a, const void *b) {
    return ((ProcessInfo*)b)->cpu_usage - ((ProcessInfo*)a)->cpu_usage;
}

JNIEXPORT jobjectArray JNICALL Java_systemdashboard_SystemMonitor_getTopProcesses
  (JNIEnv *env, jobject obj) {
    DIR *dir;
    struct dirent *entry;
    ProcessInfo processes[128];
    int process_count = 0;

    dir = opendir("/proc");
    if (!dir) {
        return NULL;
    }
    
    while ((entry = readdir(dir)) != NULL && process_count < 128) {
        if (entry->d_type == DT_DIR) {
            char *endptr;
            long pid = strtol(entry->d_name, &endptr, 10);
            if (*endptr == '\0') {
                char stat_path[256];
                snprintf(stat_path, sizeof(stat_path), "/proc/%ld/stat", pid);
                
                FILE *fp = fopen(stat_path, "r");
                if (fp) {
                    char comm[256], state;
                    unsigned long utime, stime;
                    if (fscanf(fp, "%*d (%[^)]) %c %*d %*d %*d %*d %*d %*u %*u %*u %*u %lu %lu",
                             comm, &state, &utime, &stime) == 4) {
                        processes[process_count].cpu_usage = utime + stime;
                        strncpy(processes[process_count].name, comm, sizeof(processes[process_count].name)-1);
                        process_count++;
                    }
                    fclose(fp);
                }
            }
        }
    }
    closedir(dir);
    
    // Sort processes by CPU usage
    qsort(processes, process_count, sizeof(ProcessInfo), compare_processes);
    
    // Create Java string array for top processes
    jobjectArray result = (*env)->NewObjectArray(env, 
        MIN(MAX_PROCESSES, process_count),
        (*env)->FindClass(env, "java/lang/String"),
        NULL);
    
    for (int i = 0; i < MIN(MAX_PROCESSES, process_count); i++) {
        char process_info[512];
        snprintf(process_info, sizeof(process_info), "%s (CPU: %.1f%%)",
                processes[i].name, processes[i].cpu_usage * 100.0 / sysconf(_SC_CLK_TCK));
        
        (*env)->SetObjectArrayElement(env, result, i,
            create_jstring(env, process_info));
    }
    
    return result;
} 