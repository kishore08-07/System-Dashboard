package systemdashboard;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Arrays;

public class SystemMonitor {
    private JFrame mainFrame;
    private JPanel systemPanel;    // New panel for system info
    private JPanel cpuPanel;
    private JPanel memoryPanel;
    private JPanel diskPanel;
    private JPanel networkPanel;
    private JPanel processPanel;   // New panel for process info
    private JPanel batteryPanel;   // New panel for battery info
    private JPanel controlPanel;
    private boolean isDarkTheme = false;
    private int refreshRate = 1000; // milliseconds

    // Colors for themes
    private Color lightBackground = new Color(240, 240, 240);
    private Color darkBackground = new Color(30, 30, 30);
    private Color lightText = new Color(33, 33, 33);
    private Color darkText = new Color(255, 255, 255);  // Brighter white for better contrast
    private Color lightPanelBackground = new Color(255, 255, 255);
    private Color darkPanelBackground = new Color(45, 45, 45);
    private Color lightBorder = new Color(180, 180, 180);
    private Color darkBorder = new Color(100, 100, 100);  // Lighter border for better visibility
    private Color darkButtonBackground = new Color(60, 60, 60);
    private Color darkButtonHover = new Color(80, 80, 80);
    private Color lightButtonBackground = new Color(230, 230, 230);
    private Color lightButtonHover = new Color(210, 210, 210);
    private Color darkSelection = new Color(100, 100, 100);
    private Color lightSelection = new Color(200, 200, 200);

    // Native method declarations
    private native double getCpuUsage();
    private native int getCpuCores();
    private native double[] getPerCpuUsage();
    private native long getTotalMemory();
    private native long getFreeMemory();
    private native long getSwapTotal();
    private native long getSwapFree();
    private native long getTotalDiskSpace();
    private native long getFreeDiskSpace();
    private native long getNetworkBytesReceived();
    private native long getNetworkBytesTransmitted();
    private native int getProcessCount();
    private native long getSystemUptime();
    
    // New native method declarations
    private native String getOsName();
    private native String getOsVersion();
    private native String getOsArch();
    private native String getHostname();
    private native String[] getNetworkInterfaces();
    private native String getIpAddress();
    private native String getMacAddress();
    private native boolean hasBattery();
    private native int getBatteryLevel();
    private native boolean isBatteryCharging();
    private native String[] getTopProcesses();

    static {
        System.loadLibrary("systeminfo");
    }

    public SystemMonitor() {
        prepareGUI();
    }

    private void prepareGUI() {
        mainFrame = new JFrame("System Dashboard");
        mainFrame.setSize(1200, 800);
        mainFrame.setLayout(new BorderLayout());
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Create main panels
        JPanel contentPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // System Info Panel
        systemPanel = createMetricPanel("System Information");
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 0.5;
        contentPanel.add(systemPanel, gbc);

        // CPU Panel
        cpuPanel = createMetricPanel("CPU Statistics");
        gbc.gridx = 1;
        contentPanel.add(cpuPanel, gbc);

        // Memory Panel
        memoryPanel = createMetricPanel("Memory Statistics");
        gbc.gridx = 0;
        gbc.gridy = 1;
        contentPanel.add(memoryPanel, gbc);

        // Process Panel
        processPanel = createMetricPanel("Process Information");
        gbc.gridx = 1;
        contentPanel.add(processPanel, gbc);

        // Disk Panel
        diskPanel = createMetricPanel("Storage Statistics");
        gbc.gridx = 0;
        gbc.gridy = 2;
        contentPanel.add(diskPanel, gbc);

        // Network Panel
        networkPanel = createMetricPanel("Network Statistics");
        gbc.gridx = 1;
        contentPanel.add(networkPanel, gbc);

        // Battery Panel (if available)
        if (hasBattery()) {
            batteryPanel = createMetricPanel("Battery Status");
            gbc.gridx = 0;
            gbc.gridy = 3;
            gbc.gridwidth = 2;
            contentPanel.add(batteryPanel, gbc);
        }

        // Control Panel
        controlPanel = new JPanel(new FlowLayout());
        JButton themeButton = new JButton("Toggle Theme");
        themeButton.addActionListener(e -> toggleTheme());
        styleButton(themeButton);
        
        JComboBox<String> refreshRateCombo = new JComboBox<>(new String[]{"1s", "2s", "5s"});
        refreshRateCombo.addActionListener(e -> {
            String selected = (String)refreshRateCombo.getSelectedItem();
            refreshRate = Integer.parseInt(selected.substring(0, 1)) * 1000;
        });
        styleComboBox(refreshRateCombo);

        JLabel refreshLabel = new JLabel("Refresh Rate: ");
        controlPanel.add(refreshLabel);
        controlPanel.add(refreshRateCombo);
        controlPanel.add(themeButton);

        mainFrame.add(contentPanel, BorderLayout.CENTER);
        mainFrame.add(controlPanel, BorderLayout.SOUTH);

        applyTheme();
        mainFrame.setVisible(true);

        // Start update thread
        startMonitoring();
    }

    private void styleButton(JButton button) {
        button.setFocusPainted(false);
        button.setBorderPainted(true);
        button.setOpaque(true);
        button.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
        button.setBackground(isDarkTheme ? darkButtonBackground : lightButtonBackground);
        button.setForeground(isDarkTheme ? darkText : lightText);
        
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(isDarkTheme ? darkButtonHover : lightButtonHover);
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(isDarkTheme ? darkButtonBackground : lightButtonBackground);
            }
        });
    }

    private void styleComboBox(JComboBox<?> combo) {
        combo.setBackground(isDarkTheme ? darkPanelBackground : lightPanelBackground);
        combo.setForeground(isDarkTheme ? darkText : lightText);
        combo.setBorder(BorderFactory.createLineBorder(isDarkTheme ? darkBorder : lightBorder));
        
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (isSelected) {
                    c.setBackground(isDarkTheme ? darkSelection : lightSelection);
                    c.setForeground(isDarkTheme ? darkText : lightText);
                } else {
                    c.setBackground(isDarkTheme ? darkPanelBackground : lightPanelBackground);
                    c.setForeground(isDarkTheme ? darkText : lightText);
                }
                return c;
            }
        });
    }

    private JPanel createMetricPanel(String title) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        TitledBorder titledBorder = BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(isDarkTheme ? darkBorder : lightBorder, 1),
            title,
            TitledBorder.LEFT,
            TitledBorder.TOP
        );
        titledBorder.setTitleColor(isDarkTheme ? darkText : lightText);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(5, 5, 5, 5),
            titledBorder
        ));
        panel.setOpaque(true);
        panel.setBackground(isDarkTheme ? darkPanelBackground : lightPanelBackground);
        return panel;
    }

    private void toggleTheme() {
        isDarkTheme = !isDarkTheme;
        applyTheme();
        SwingUtilities.updateComponentTreeUI(mainFrame);
    }

    private void applyTheme() {
        Color bg = isDarkTheme ? darkBackground : lightBackground;
        Color fg = isDarkTheme ? darkText : lightText;
        Color panelBg = isDarkTheme ? darkPanelBackground : lightPanelBackground;
        Color borderColor = isDarkTheme ? darkBorder : lightBorder;

        // Update main frame and content
        mainFrame.getContentPane().setBackground(bg);
        for (Component c : mainFrame.getContentPane().getComponents()) {
            if (c instanceof JPanel) {
                c.setBackground(bg);
                // Ensure all labels in the panel get updated
                for (Component child : ((JPanel)c).getComponents()) {
                    if (child instanceof JLabel) {
                        child.setForeground(fg);
                    }
                }
            }
        }
        
        // Update all panels
        JPanel[] panels = {
            systemPanel, cpuPanel, memoryPanel, diskPanel,
            networkPanel, processPanel, controlPanel
        };
        
        if (batteryPanel != null) {
            panels = Arrays.copyOf(panels, panels.length + 1);
            panels[panels.length - 1] = batteryPanel;
        }

        for (JPanel panel : panels) {
            if (panel != null) {
                panel.setBackground(panelBg);
                panel.setForeground(fg);
                
                // Update border
                TitledBorder titledBorder = BorderFactory.createTitledBorder(
                    BorderFactory.createLineBorder(borderColor, 1),
                    panel.getBorder() instanceof TitledBorder ? 
                        ((TitledBorder) panel.getBorder()).getTitle() : "",
                    TitledBorder.LEFT,
                    TitledBorder.TOP
                );
                titledBorder.setTitleColor(fg);
                panel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createEmptyBorder(5, 5, 5, 5),
                    titledBorder
                ));
                
                // Ensure all labels get updated with correct colors
                for (Component c : panel.getComponents()) {
                    if (c instanceof JLabel) {
                        c.setForeground(fg);
                    } else if (c instanceof JButton) {
                        styleButton((JButton) c);
                    } else if (c instanceof JComboBox) {
                        styleComboBox((JComboBox<?>) c);
                    } else {
                        updateComponentColors(c, fg, panelBg);
                    }
                }
            }
        }

        // Update control panel components
        for (Component c : controlPanel.getComponents()) {
            if (c instanceof JButton) {
                styleButton((JButton) c);
            } else if (c instanceof JComboBox) {
                styleComboBox((JComboBox<?>) c);
            } else if (c instanceof JLabel) {
                c.setForeground(fg);
                c.setBackground(panelBg);
            }
        }

        // Force an update of all metric panels
        updateSystemPanel();
        updateProcessPanel();
        updateMetrics();

        mainFrame.repaint();
        mainFrame.revalidate();
    }

    private void updateComponentColors(Component c, Color fg, Color bg) {
        if (c instanceof JComponent) {
            JComponent jc = (JComponent) c;
            jc.setForeground(fg);
            jc.setBackground(bg);
            jc.setOpaque(c instanceof JPanel);
            
            if (c instanceof Container) {
                for (Component child : ((Container) c).getComponents()) {
                    updateComponentColors(child, fg, bg);
                }
            }
        } else {
            c.setForeground(fg);
            c.setBackground(bg);
        }
    }

    private void updateMetrics() {
        // System Info
        updateSystemPanel();

        // CPU Metrics
        double cpuUsage = getCpuUsage();
        double[] perCpuUsage = getPerCpuUsage();
        updateCpuPanel(cpuUsage, perCpuUsage);

        // Memory Metrics
        long totalMem = getTotalMemory();
        long freeMem = getFreeMemory();
        long swapTotal = getSwapTotal();
        long swapFree = getSwapFree();
        updateMemoryPanel(totalMem, freeMem, swapTotal, swapFree);

        // Process Information
        updateProcessPanel();

        // Disk Metrics
        long totalDisk = getTotalDiskSpace();
        long freeDisk = getFreeDiskSpace();
        updateDiskPanel(totalDisk, freeDisk);

        // Network Metrics
        updateNetworkPanel();

        // Battery Status (if available)
        if (hasBattery()) {
            updateBatteryPanel();
        }

        mainFrame.repaint();
    }

    private void updateSystemPanel() {
        systemPanel.removeAll();
        
        addMetricLabel(systemPanel, "Operating System:");
        addMetricLabel(systemPanel, String.format("  Name: %s", getOsName()));
        addMetricLabel(systemPanel, String.format("  Version: %s", getOsVersion()));
        addMetricLabel(systemPanel, String.format("  Architecture: %s", getOsArch()));
        addMetricLabel(systemPanel, String.format("  Hostname: %s", getHostname()));
        
        // System uptime
        long uptime = getSystemUptime();
        int days = (int) (uptime / 86400);
        int hours = (int) ((uptime % 86400) / 3600);
        int minutes = (int) ((uptime % 3600) / 60);
        addMetricLabel(systemPanel, String.format("System Uptime: %d days, %d hours, %d minutes", 
            days, hours, minutes));
        
        systemPanel.revalidate();
    }

    private void updateProcessPanel() {
        processPanel.removeAll();
        
        addMetricLabel(processPanel, String.format("Total Processes: %d", getProcessCount()));
        addMetricLabel(processPanel, "\nTop Processes by CPU Usage:");
        
        String[] topProcesses = getTopProcesses();
        for (String process : topProcesses) {
            addMetricLabel(processPanel, "  " + process);
        }
        
        processPanel.revalidate();
    }

    private void updateNetworkPanel() {
        networkPanel.removeAll();
        
        addMetricLabel(networkPanel, "Network Interfaces:");
        addMetricLabel(networkPanel, String.format("  IP Address: %s", getIpAddress()));
        addMetricLabel(networkPanel, String.format("  MAC Address: %s", getMacAddress()));
        
        double receivedGB = getNetworkBytesReceived() / (1024.0 * 1024 * 1024);
        double transmittedGB = getNetworkBytesTransmitted() / (1024.0 * 1024 * 1024);
        
        addMetricLabel(networkPanel, "\nNetwork Traffic:");
        addMetricLabel(networkPanel, String.format("  Total Received: %.2f GB", receivedGB));
        addMetricLabel(networkPanel, String.format("  Total Transmitted: %.2f GB", transmittedGB));
        addMetricLabel(networkPanel, String.format("  Total Traffic: %.2f GB", receivedGB + transmittedGB));
        
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        addMetricLabel(networkPanel, "\nLast Updated: " + sdf.format(new Date()));
        
        networkPanel.revalidate();
    }

    private void updateBatteryPanel() {
        if (batteryPanel != null) {
            batteryPanel.removeAll();
            
            int batteryLevel = getBatteryLevel();
            boolean isCharging = isBatteryCharging();
            
            addMetricLabel(batteryPanel, String.format("Battery Level: %d%%", batteryLevel));
            addMetricLabel(batteryPanel, String.format("Status: %s", 
                isCharging ? "Charging" : "Discharging"));
            
            batteryPanel.revalidate();
        }
    }

    private void updateCpuPanel(double cpuUsage, double[] perCpuUsage) {
        cpuPanel.removeAll();
        
        // System uptime
        long uptime = getSystemUptime();
        int days = (int) (uptime / 86400);
        int hours = (int) ((uptime % 86400) / 3600);
        int minutes = (int) ((uptime % 3600) / 60);
        
        // Add metrics with proper spacing
        addMetricLabel(cpuPanel, String.format("System Uptime: %d days, %d hours, %d minutes", days, hours, minutes));
        addMetricLabel(cpuPanel, String.format("Total CPU Usage: %.1f%%", cpuUsage));
        addMetricLabel(cpuPanel, String.format("Number of CPU Cores: %d", perCpuUsage.length));
        addMetricLabel(cpuPanel, "Per Core Usage:");
        
        for (int i = 0; i < perCpuUsage.length; i++) {
            addMetricLabel(cpuPanel, String.format("  Core %d: %.1f%%", i + 1, perCpuUsage[i]));
        }
        
        addMetricLabel(cpuPanel, String.format("Active Processes: %d", getProcessCount()));
        
        cpuPanel.revalidate();
    }

    private void updateMemoryPanel(long total, long free, long swapTotal, long swapFree) {
        memoryPanel.removeAll();
        
        long used = total - free;
        long swapUsed = swapTotal - swapFree;
        
        addMetricLabel(memoryPanel, "Physical Memory:");
        addMetricLabel(memoryPanel, String.format("  Total: %.2f GB", total / (1024.0 * 1024 * 1024)));
        addMetricLabel(memoryPanel, String.format("  Used: %.2f GB (%.1f%%)", 
            used / (1024.0 * 1024 * 1024), 
            (double)used / total * 100));
        addMetricLabel(memoryPanel, String.format("  Free: %.2f GB", 
            free / (1024.0 * 1024 * 1024)));
        
        addMetricLabel(memoryPanel, "\nSwap Memory:");
        addMetricLabel(memoryPanel, String.format("  Total: %.2f GB", 
            swapTotal / (1024.0 * 1024 * 1024)));
        addMetricLabel(memoryPanel, String.format("  Used: %.2f GB (%.1f%%)", 
            swapUsed / (1024.0 * 1024 * 1024),
            swapTotal > 0 ? (double)swapUsed / swapTotal * 100 : 0));
        
        memoryPanel.revalidate();
    }

    private void updateDiskPanel(long total, long free) {
        diskPanel.removeAll();
        
        long used = total - free;
        double totalGB = total / (1024.0 * 1024 * 1024);
        double usedGB = used / (1024.0 * 1024 * 1024);
        double freeGB = free / (1024.0 * 1024 * 1024);
        
        addMetricLabel(diskPanel, "Root Partition (/):");
        addMetricLabel(diskPanel, String.format("  Total Space: %.2f GB", totalGB));
        addMetricLabel(diskPanel, String.format("  Used Space: %.2f GB (%.1f%%)", 
            usedGB, (double)used / total * 100));
        addMetricLabel(diskPanel, String.format("  Free Space: %.2f GB", freeGB));
        
        diskPanel.revalidate();
    }

    private void addMetricLabel(JPanel panel, String text) {
        JLabel label = new JLabel(text);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setBorder(BorderFactory.createEmptyBorder(2, 10, 2, 10));
        if (isDarkTheme) {
            label.setForeground(darkText);
        }
        panel.add(label);
    }

    private void startMonitoring() {
        Thread updateThread = new Thread(() -> {
            while (true) {
                SwingUtilities.invokeLater(this::updateMetrics);
                try {
                    Thread.sleep(refreshRate);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        updateThread.setDaemon(true);
        updateThread.start();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SystemMonitor());
    }
}