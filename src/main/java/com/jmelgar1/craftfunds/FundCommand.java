package com.jmelgar1.craftfunds;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.util.Formatting;
import java.util.concurrent.CompletableFuture;

public class FundCommand {

    /**
     * Registers the /fund command with the command dispatcher.
     * 
     * The command is configured to:
     * - Require the sender to be a player (not console/command block)
     * - Execute the fund command logic when invoked
     * - Provide appropriate error messages for invalid usage
     * 
     * @param dispatcher The command dispatcher to register with
     */
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
            CommandManager.literal("fund")
                .requires(source -> source.isExecutedByPlayer()) // Only players can use this command
                .executes(FundCommand::execute)
        );
    }

    /**
     * Executes the /fund command logic.
     * 
     * This method:
     * 1. Validates that the command source is a player
     * 2. Queries the database for monthly funding totals
     * 3. Sends the response back to the player
     * 4. Logs the command execution for server monitoring
     * 5. Returns success status to the command system
     * 
     * @param context The command execution context
     * @return Command execution result (1 for success, 0 for failure)
     */
    private static int execute(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        try {
            // Get the player who executed the command
            ServerPlayerEntity player = source.getPlayerOrThrow();
            
            // Log the command execution
            CraftFunds.LOGGER.info("Player {} executed /fund command, querying database for funding totals", 
                player.getName().getString());
            
            // Create database service and query for funding totals
            DatabaseService databaseService = new DatabaseService();
            
            // Show loading message to player
            player.sendMessage(Text.literal("§7Retrieving funding information..."), false);
            
            // Query database asynchronously
            CompletableFuture<DatabaseService.FundingReport> futureResult = databaseService.getMonthlyFundingTotal();
            
            futureResult.thenAccept(report -> {
                // Send header message
                player.sendMessage(Text.literal("=== Server Fund ===").formatted(Formatting.DARK_GREEN), false);
                
                // Create hover text component with conditional coloring
                if (report.summary.contains("donation)") || report.summary.contains("donations)")) {
                    // Use the net amount for conditional coloring
                    double netAmount = report.netAmount;
                    
                    // Determine color based on net amount (< 15 = red, >= 15 = gold)
                    Formatting amountColor = netAmount < 15 ? Formatting.RED : Formatting.GOLD;
                    
                    // Find the part with donation count and make it hoverable
                    String[] parts = report.summary.split("\\(");
                    if (parts.length > 1) {
                        // Remove existing color codes and apply new color
                        String beforeHover = parts[0].replaceAll("§.", "");
                        String hoverPart = "(" + parts[1];
                        
                        MutableText finalMessage = Text.literal(beforeHover)
                            .formatted(amountColor)
                            .append(Text.literal(hoverPart)
                                .styled(style -> style
                                .withColor(Formatting.GRAY)
                                .withHoverEvent(new HoverEvent.ShowText(report.donationDetails))
                                ));
                        
                        player.sendMessage(finalMessage, false);
                    } else {
                        // Fallback if parsing fails
                        MutableText message = Text.literal(report.summary.replaceAll("§.", ""))
                            .formatted(amountColor);
                        player.sendMessage(message, false);
                    }
                } else {
                    // No donations case
                    player.sendMessage(Text.literal(report.summary), false);
                }
                
                // Calculate and display months covered or funding goal message
                if (report.netAmount < 15) {
                    player.sendMessage(Text.literal("§cServer is below the funding goal"), false);
                } else {
                    int monthsCovered = (int) Math.floor(report.netAmount / 15.0);
                    if (monthsCovered > 0) {
                        String monthText = monthsCovered == 1 ? "month" : "months";
                        player.sendMessage(Text.literal("§7Covers " + monthsCovered + " " + monthText + " of server costs"), false);
                    }
                }
                
                CraftFunds.LOGGER.info("Fund command completed successfully for player {}", 
                    player.getName().getString());
            }).exceptionally(throwable -> {
                // Handle any errors that occurred during database query
                CraftFunds.LOGGER.error("Error retrieving funding data for player {}", 
                    player.getName().getString(), throwable);
                player.sendMessage(Text.literal("§cFailed to retrieve funding information. Please try again later."), false);
                return null;
            });
            
            // Return 1 to indicate successful command execution (async operation will continue)
            return 1;
            
        } catch (Exception e) {
            // Handle any other unexpected errors gracefully
            CraftFunds.LOGGER.error("Error executing /fund command", e);
            
            // Try to send error message to source if possible
            try {
                source.sendMessage(Text.literal("§cAn error occurred while executing the command."));
            } catch (Exception sendError) {
                CraftFunds.LOGGER.error("Could not send error message to command source", sendError);
            }
            
            // Return 0 to indicate command failure
            return 0;
        }
    }
}