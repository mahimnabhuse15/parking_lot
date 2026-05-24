package com.parking;

import org.springframework.stereotype.Component;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

@Component
public class DatabaseManager {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Default Connection Parameters (Dynamically configurable)
    private static String dbHost = "localhost";
    private static String dbPort = "3306";
    private static String dbName = "autopark";
    private static String dbUser = "root";
    private static String dbPass = "";
    private static boolean isConnected = false;

    static {
        try {
            // Force load modern MySQL JDBC Driver
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("Failed to load MySQL Connector/J driver: " + e.getMessage());
        }
        // Load stored credentials from properties on startup
        loadConfig();
    }

    public static boolean isDbConnected() {
        return isConnected;
    }

    public static String getDbHost() { return dbHost; }
    public static String getDbPort() { return dbPort; }
    public static String getDbName() { return dbName; }
    public static String getDbUser() { return dbUser; }
    public static String getDbPass() { return dbPass; }

    private static Connection getConnection() throws SQLException {
        String dbUrl = "jdbc:mysql://" + dbHost + ":" + dbPort + "/" + dbName + "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
        return DriverManager.getConnection(dbUrl, dbUser, dbPass);
    }

    // Config Manager: load credentials from environment variables or properties file
    public static void loadConfig() {
        // Read environment variables first (Railway production deployment)
        String envHost = System.getenv("DB_HOST");
        String envPort = System.getenv("DB_PORT");
        String envName = System.getenv("DB_NAME");
        String envUser = System.getenv("DB_USER");
        String envPass = System.getenv("DB_PASS");

        if (envHost != null && !envHost.trim().isEmpty()) {
            dbHost = envHost;
            dbPort = envPort != null ? envPort : "3306";
            dbName = envName != null ? envName : "autopark";
            dbUser = envUser != null ? envUser : "root";
            dbPass = envPass != null ? envPass : "";
            System.out.println("Loaded DB configuration from environment variables (Railway).");
            return;
        }

        Properties props = new Properties();
        File configFile = new File("config.properties");
        if (configFile.exists()) {
            try (FileReader reader = new FileReader(configFile)) {
                props.load(reader);
                dbHost = props.getProperty("db.host", "localhost");
                dbPort = props.getProperty("db.port", "3306");
                dbName = props.getProperty("db.name", "autopark");
                dbUser = props.getProperty("db.user", "root");
                dbPass = props.getProperty("db.pass", "");
            } catch (Exception e) {
                System.err.println("Failed to load config properties: " + e.getMessage());
            }
        }
    }

    // Config Manager: save dynamic credentials
    public static void saveConfig(String host, String port, String name, String user, String pass) {
        dbHost = host;
        dbPort = port;
        dbName = name;
        dbUser = user;
        dbPass = pass;

        Properties props = new Properties();
        props.setProperty("db.host", host);
        props.setProperty("db.port", port);
        props.setProperty("db.name", name);
        props.setProperty("db.user", user);
        props.setProperty("db.pass", pass);

        try (FileWriter writer = new FileWriter("config.properties")) {
            props.store(writer, "AutoPark MySQL Configurations");
        } catch (Exception e) {
            System.err.println("Failed to save config properties: " + e.getMessage());
        }
    }

    // Initialize Database: connects to server, creates DB if absent, then creates tables
    public static boolean initializeDatabase(String host, String port, String name, String user, String pass, int defaultCapacity) {
        dbHost = host;
        dbPort = port;
        dbName = name;
        dbUser = user;
        dbPass = pass;

        String serverUrl = "jdbc:mysql://" + host + ":" + port + "/?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";

        try {
            // Register MySQL Driver explicitly
            Class.forName("com.mysql.cj.jdbc.Driver");

            // Step 1: Connect to server and create database if it does not exist
            try (Connection conn = DriverManager.getConnection(serverUrl, user, pass);
                 Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS " + name);
            }

            // Step 2: Connect to the database and build tables
            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement()) {

                String createSlotsTable = "CREATE TABLE IF NOT EXISTS slots (" +
                        "slot_number INT PRIMARY KEY, " +
                        "vehicle_number VARCHAR(50), " +
                        "vehicle_type VARCHAR(20), " +
                        "entry_time VARCHAR(30)" +
                        ")";

                String createTransactionsTable = "CREATE TABLE IF NOT EXISTS transactions (" +
                        "id INT PRIMARY KEY AUTO_INCREMENT, " +
                        "vehicle_number VARCHAR(50), " +
                        "vehicle_type VARCHAR(20), " +
                        "slot_number INT, " +
                        "entry_time VARCHAR(30), " +
                        "exit_time VARCHAR(30), " +
                        "fee INT" +
                        ")";

                stmt.executeUpdate(createSlotsTable);
                stmt.executeUpdate(createTransactionsTable);

                // Populate slots table with vacant slots if empty
                String countQuery = "SELECT COUNT(*) FROM slots";
                try (ResultSet rs = stmt.executeQuery(countQuery)) {
                    if (rs.next() && rs.getInt(1) == 0) {
                        String insertSlot = "INSERT INTO slots (slot_number, vehicle_number, vehicle_type, entry_time) VALUES (?, NULL, NULL, NULL)";
                        try (PreparedStatement pstmt = conn.prepareStatement(insertSlot)) {
                            for (int i = 1; i <= defaultCapacity; i++) {
                                pstmt.setInt(1, i);
                                pstmt.addBatch();
                            }
                            pstmt.executeBatch();
                        }
                    }
                }
            }

            // Persist the verified credentials
            saveConfig(host, port, name, user, pass);
            isConnected = true;
            System.out.println("MySQL database initialised successfully.");
            return true;

        } catch (Exception e) {
            System.err.println("MySQL Connection/Initialization failed: " + e.getMessage());
            isConnected = false;
            return false;
        }
    }

    // Load active slots from the database
    public static List<SlotData> loadSlots() {
        List<SlotData> list = new ArrayList<>();
        if (!isConnected) return list;

        String query = "SELECT slot_number, vehicle_number, vehicle_type, entry_time FROM slots ORDER BY slot_number ASC";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                int slotNum = rs.getInt("slot_number");
                String plate = rs.getString("vehicle_number");
                String type = rs.getString("vehicle_type");
                String entryTime = rs.getString("entry_time");

                list.add(new SlotData(slotNum, plate, type, entryTime));
            }
        } catch (SQLException e) {
            System.err.println("Failed to load slots from database: " + e.getMessage());
        }
        return list;
    }

    // Save parked vehicle state
    public static void saveParking(int slotNumber, String plate, String type, LocalDateTime entryTime) {
        if (!isConnected) return;

        String updateQuery = "UPDATE slots SET vehicle_number = ?, vehicle_type = ?, entry_time = ? WHERE slot_number = ?";
        String entryTimeStr = entryTime.format(FORMATTER);

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(updateQuery)) {

            pstmt.setString(1, plate);
            pstmt.setString(2, type);
            pstmt.setString(3, entryTimeStr);
            pstmt.setInt(4, slotNumber);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Failed to save parking state to MySQL: " + e.getMessage());
        }
    }

    // Clear slot on departure
    public static void releaseSlot(int slotNumber) {
        if (!isConnected) return;

        String updateQuery = "UPDATE slots SET vehicle_number = NULL, vehicle_type = NULL, entry_time = NULL WHERE slot_number = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(updateQuery)) {

            pstmt.setInt(1, slotNumber);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Failed to release slot in MySQL: " + e.getMessage());
        }
    }

    // Log transaction into history
    public static void recordTransaction(String plate, String type, int slotNumber, String entryTime, LocalDateTime exitTime, int fee) {
        if (!isConnected) return;

        String insertQuery = "INSERT INTO transactions (vehicle_number, vehicle_type, slot_number, entry_time, exit_time, fee) VALUES (?, ?, ?, ?, ?, ?)";
        String exitTimeStr = exitTime.format(FORMATTER);

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertQuery)) {

            pstmt.setString(1, plate);
            pstmt.setString(2, type);
            pstmt.setInt(3, slotNumber);
            pstmt.setString(4, entryTime);
            pstmt.setString(5, exitTimeStr);
            pstmt.setInt(6, fee);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Failed to record MySQL transaction: " + e.getMessage());
        }
    }

    // Reinitialize slots table with a new capacity
    public static void reinitializeSlots(int newCapacity) {
        if (!isConnected) return;

        String dropQuery = "DROP TABLE IF EXISTS slots";
        String createSlotsTable = "CREATE TABLE slots (" +
                "slot_number INT PRIMARY KEY, " +
                "vehicle_number VARCHAR(50), " +
                "vehicle_type VARCHAR(20), " +
                "entry_time VARCHAR(30)" +
                ")";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute(dropQuery);
            stmt.execute(createSlotsTable);

            // Populate slots table with vacant slots
            String insertSlot = "INSERT INTO slots (slot_number, vehicle_number, vehicle_type, entry_time) VALUES (?, NULL, NULL, NULL)";
            try (PreparedStatement pstmt = conn.prepareStatement(insertSlot)) {
                for (int i = 1; i <= newCapacity; i++) {
                    pstmt.setInt(1, i);
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
            }
        } catch (SQLException e) {
            System.err.println("Failed to reinitialize slots table: " + e.getMessage());
        }
    }

    // Get lifetime aggregate revenue
    public static int getTotalRevenue() {
        if (!isConnected) return 0;

        String query = "SELECT SUM(fee) FROM transactions";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Failed to query total MySQL revenue: " + e.getMessage());
        }
        return 0;
    }

    // Get list of past transactions for logs
    public static List<TransactionData> getHistory(int limit) {
        List<TransactionData> history = new ArrayList<>();
        if (!isConnected) return history;

        String query = "SELECT vehicle_number, vehicle_type, slot_number, entry_time, exit_time, fee FROM transactions ORDER BY id DESC LIMIT ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, limit);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String plate = rs.getString("vehicle_number");
                    String type = rs.getString("vehicle_type");
                    int slotNum = rs.getInt("slot_number");
                    String entry = rs.getString("entry_time");
                    String exit = rs.getString("exit_time");
                    int fee = rs.getInt("fee");

                    history.add(new TransactionData(plate, type, slotNum, entry, exit, fee));
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to pull MySQL transaction history: " + e.getMessage());
        }
        return history;
    }

    // Data Transfer Objects
    public static class SlotData {
        public final int slotNumber;
        public final String vehicleNumber;
        public final String vehicleType;
        public final String entryTime;

        public SlotData(int slotNumber, String vehicleNumber, String vehicleType, String entryTime) {
            this.slotNumber = slotNumber;
            this.vehicleNumber = vehicleNumber;
            this.vehicleType = vehicleType;
            this.entryTime = entryTime;
        }

        public boolean isEmpty() {
            return vehicleNumber == null;
        }
    }

    public static class TransactionData {
        public final String vehicleNumber;
        public final String vehicleType;
        public final int slotNumber;
        public final String entryTime;
        public final String exitTime;
        public final int fee;

        public TransactionData(String vehicleNumber, String vehicleType, int slotNumber, String entryTime, String exitTime, int fee) {
            this.vehicleNumber = vehicleNumber;
            this.vehicleType = vehicleType;
            this.slotNumber = slotNumber;
            this.entryTime = entryTime;
            this.exitTime = exitTime;
            this.fee = fee;
        }
    }
}
