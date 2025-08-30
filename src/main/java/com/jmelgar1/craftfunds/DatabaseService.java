package com.jmelgar1.craftfunds;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;
import net.minecraft.text.Text;
import net.minecraft.text.MutableText;

public class DatabaseService {
    private final ConfigManager config;
    private static boolean driverLoaded = false;
    
    public DatabaseService() {
        this.config = ConfigManager.getInstance();
        loadDriver();
    }
    
    private static synchronized void loadDriver() {
        if (!driverLoaded) {
            CraftFunds.LOGGER.info("Attempting to load MySQL JDBC driver...");
            
            // List all available drivers for debugging
            java.util.Enumeration<java.sql.Driver> drivers = java.sql.DriverManager.getDrivers();
            CraftFunds.LOGGER.info("Available JDBC drivers:");
            while (drivers.hasMoreElements()) {
                java.sql.Driver driver = drivers.nextElement();
                CraftFunds.LOGGER.info("  - {}", driver.getClass().getName());
            }
            
            try {
                // Try the newer MySQL connector first
                Class.forName("com.mysql.cj.jdbc.Driver");
                driverLoaded = true;
                CraftFunds.LOGGER.info("MySQL JDBC driver loaded successfully");
            } catch (ClassNotFoundException e) {
                CraftFunds.LOGGER.warn("Could not load com.mysql.cj.jdbc.Driver: {}", e.getMessage());
                try {
                    // Fallback to older driver name if available
                    Class.forName("com.mysql.jdbc.Driver");
                    driverLoaded = true;
                    CraftFunds.LOGGER.info("MySQL JDBC driver (legacy) loaded successfully");
                } catch (ClassNotFoundException e2) {
                    CraftFunds.LOGGER.error("Failed to load any MySQL JDBC driver");
                    CraftFunds.LOGGER.error("  com.mysql.cj.jdbc.Driver: {}", e.getMessage());
                    CraftFunds.LOGGER.error("  com.mysql.jdbc.Driver: {}", e2.getMessage());
                }
            }
        }
    }
    
    public static class FundingReport {
        public final String summary;
        public final Text donationDetails;
        public final double totalDonations;
        public final double totalSpending;
        public final double netAmount;
        
        public FundingReport(String summary, Text donationDetails, double totalDonations, double totalSpending) {
            this.summary = summary;
            this.donationDetails = donationDetails;
            this.totalDonations = totalDonations;
            this.totalSpending = totalSpending;
            this.netAmount = totalDonations - totalSpending;
        }
    }
    
    private double getTotalSpending(Connection connection) throws SQLException {
        String query = "SELECT total_spent FROM total_spending LIMIT 1";
        try (PreparedStatement stmt = connection.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getDouble("total_spent");
            }
            return 0.0;
        }
    }
    
    public CompletableFuture<FundingReport> getMonthlyFundingTotal() {
        return CompletableFuture.supplyAsync(() -> {
            if (!config.hasValidDatabaseCredentials()) {
                return new FundingReport("Database credentials not configured. Please check your craftfunds.conf file.", Text.literal(""), 0.0, 0.0);
            }
            
            try (Connection connection = createConnection()) {
                LocalDate now = LocalDate.now();
                LocalDate startDate = LocalDate.of(now.getYear(), now.getMonth(), 2);
                LocalDate endDate = now.withDayOfMonth(now.lengthOfMonth());
                
                // Get total donations for USD (primary currency) - all time
                String totalQuery = "SELECT SUM(amount) as total, COUNT(*) as donation_count " +
                                   "FROM donations " +
                                   "WHERE currency = 'USD'";
                
                double totalUSD = 0;
                int totalCount = 0;
                
                try (PreparedStatement stmt = connection.prepareStatement(totalQuery)) {
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            totalUSD = rs.getDouble("total");
                            totalCount = rs.getInt("donation_count");
                        }
                    }
                }
                
                // Get total spending
                double totalSpending = getTotalSpending(connection);
                double netAmount = totalUSD - totalSpending;
                
                // Get detailed donations for hover text (only show excess donations after spending)
                // Query all donations, not limited by date range
                String detailQuery = "SELECT name, amount, date " +
                                    "FROM donations " +
                                    "WHERE currency = 'USD' " +
                                    "ORDER BY date ASC"; // Order by oldest first to subtract spending
                
                MutableText detailsText = Text.literal("Donations:\n");
                int excessDonationCount = 0;
                
                try (PreparedStatement stmt = connection.prepareStatement(detailQuery)) {
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        double remainingSpending = totalSpending;
                        int count = 1;
                        
                        while (rs.next()) {
                            String donorName = rs.getString("name");
                            double amount = rs.getDouble("amount");
                            LocalDate donationDate = rs.getDate("date").toLocalDate();
                            
                            if (remainingSpending > 0) {
                                if (amount <= remainingSpending) {
                                    // This donation is fully consumed by spending
                                    remainingSpending -= amount;
                                    continue;
                                } else {
                                    // This donation is partially consumed
                                    double excessAmount = amount - remainingSpending;
                                    remainingSpending = 0;
                                    
                                    // Show the excess portion with alternating colors
                                    boolean isEven = (count % 2 == 0);
                                    int nameColor = isEven ? 0x565e58 : 0x667369;
                                    int amountColor = isEven ? 0x3a944f : 0x40b85c;
                                    int dateColor = isEven ? 0x9c934b : 0xbfb354;
                                    
                                    detailsText.append(Text.literal(count + ". "))
                                              .append(Text.literal(donorName).styled(style -> style.withColor(nameColor)))
                                              .append(Text.literal(": "))
                                              .append(Text.literal("$" + String.format("%.2f", excessAmount)).styled(style -> style.withColor(amountColor)))
                                              .append(Text.literal(" ("))
                                              .append(Text.literal(donationDate.format(DateTimeFormatter.ofPattern("MM-dd"))).styled(style -> style.withColor(dateColor)))
                                              .append(Text.literal(")\n"));
                                    count++;
                                    excessDonationCount++;
                                }
                            } else {
                                // All spending is covered, show full donation with alternating colors
                                boolean isEven = (count % 2 == 0);
                                int nameColor = isEven ? 0x565e58 : 0x667369;
                                int amountColor = isEven ? 0x3a944f : 0x40b85c;
                                int dateColor = isEven ? 0x9c934b : 0xbfb354;
                                
                                detailsText.append(Text.literal(count + ". "))
                                          .append(Text.literal(donorName).styled(style -> style.withColor(nameColor)))
                                          .append(Text.literal(": "))
                                          .append(Text.literal("$" + String.format("%.2f", amount)).styled(style -> style.withColor(amountColor)))
                                          .append(Text.literal(" ("))
                                          .append(Text.literal(donationDate.format(DateTimeFormatter.ofPattern("MM-dd"))).styled(style -> style.withColor(dateColor)))
                                          .append(Text.literal(")\n"));
                                count++;
                                excessDonationCount++;
                            }
                            
                            // Limit to 10 excess donations for display in hover, but continue counting
                            if (count > 10) {
                                // Continue counting remaining excess donations without adding to display
                                while (rs.next()) {
                                    double remainingAmount = rs.getDouble("amount");
                                    // Only count if spending is already covered (remainingSpending should be 0 at this point)
                                    if (remainingSpending <= 0) {
                                        excessDonationCount++;
                                    }
                                }
                                break;
                            }
                        }
                        
                        // If no excess donations, update the message
                        if (count == 1) {
                            detailsText = Text.literal("All donations have been consumed by spending.\n");
                        }
                    }
                }
                
                // Create summary message
                String summary;
                if (totalCount == 0) {
                    summary = "§c No donations found.";
                } else {
                    String donationText = excessDonationCount == 1 ? "donation" : "donations";
                    summary = String.format("§6$%.2f / $15 §7(%d %s)", netAmount, excessDonationCount, donationText);
                }
                
                return new FundingReport(summary, detailsText, totalUSD, totalSpending);
                
            } catch (SQLException e) {
                CraftFunds.LOGGER.error("Database error while retrieving funding total", e);
                return new FundingReport("§cDatabase error: " + e.getMessage(), Text.literal(""), 0.0, 0.0);
            }
        });
    }
    
    private Connection createConnection() throws SQLException {
        String url = config.getDatabaseUrl();
        String username = config.getDatabaseUsername();
        String password = config.getDatabasePassword();
        int timeoutSeconds = config.getDatabaseTimeoutSeconds();
        
        // Add connection properties for better MySQL compatibility
        if (!url.contains("?")) {
            url += "?";
        } else if (!url.endsWith("&") && !url.endsWith("?")) {
            url += "&";
        }
        
        url += "useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&connectTimeout=" + (timeoutSeconds * 1000);
        
        DriverManager.setLoginTimeout(timeoutSeconds);
        
        CraftFunds.LOGGER.debug("Attempting to connect to database: {}", url.replaceAll("password=[^&]*", "password=***"));
        
        Connection connection = DriverManager.getConnection(url, username, password);
        
        // Don't set network timeout as it requires an executor in newer MySQL connector versions
        // The connectTimeout in the URL should handle connection timeouts
        
        return connection;
    }
}