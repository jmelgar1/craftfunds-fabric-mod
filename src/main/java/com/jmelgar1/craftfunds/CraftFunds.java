package com.jmelgar1.craftfunds;

import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.text.Text;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.CompletableFuture;

/**
 * Main mod class for CraftFunds - A minimal Fabric server-side mod template.
 * 
 * This class implements DedicatedServerModInitializer to ensure it only loads
 * on the server side and not on clients. This follows Fabric's best practices
 * for server-only mods.
 */
public class CraftFunds implements DedicatedServerModInitializer {
    
    /**
     * Mod ID - should match the id field in fabric.mod.json
     */
    public static final String MOD_ID = "craftfunds";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    
    @Override
    public void onInitializeServer() {
        LOGGER.info("CraftFunds mod initialized on server side!");
        
        // Register commands
        CommandRegistrationCallback.EVENT.register(this::registerCommands);
        
        // Register player join event
        ServerPlayConnectionEvents.JOIN.register(this::onPlayerJoin);
        
        LOGGER.info("CraftFunds commands and events registered successfully!");
    }
    
    /**
     * Registers all CraftFunds commands
     * 
     * @param dispatcher The command dispatcher
     * @param registryAccess Registry access for command registration
     * @param environment The registration environment (integrated/dedicated server)
     */
    private void registerCommands(com.mojang.brigadier.CommandDispatcher<net.minecraft.server.command.ServerCommandSource> dispatcher, 
                                 net.minecraft.command.CommandRegistryAccess registryAccess, 
                                 net.minecraft.server.command.CommandManager.RegistrationEnvironment environment) {
        
        LOGGER.info("Registering CraftFunds commands");
        
        FundCommand.register(dispatcher);
        DonateCommand.register(dispatcher);
        
        LOGGER.info("Successfully registered /fund and /donate commands");
    }
    
    /**
     * Handles player join events by showing the funding information
     * 
     * @param handler The network handler
     * @param sender The packet sender
     * @param server The server instance
     */
    private void onPlayerJoin(ServerPlayNetworkHandler handler, PacketSender sender, net.minecraft.server.MinecraftServer server) {
        ServerPlayerEntity player = handler.getPlayer();
        LOGGER.info("Player {} joined, showing funding information", player.getName().getString());
        
        // Create database service and query for funding totals
        DatabaseService databaseService = new DatabaseService();
        
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
                player.sendMessage(Text.literal("§cServer is below the funding goal, use /donate"), false);
            } else {
                int monthsCovered = (int) Math.floor(report.netAmount / 15.0);
                if (monthsCovered > 0) {
                    String monthText = monthsCovered == 1 ? "month" : "months";
                    player.sendMessage(Text.literal("§7Covers " + monthsCovered + " " + monthText + " of server costs"), false);
                    player.sendMessage(Text.literal("§7Use '/donate' to fund the server"), false);
                }
            }
            
            LOGGER.info("Funding information displayed to player {} on join", player.getName().getString());
        }).exceptionally(throwable -> {
            // Handle any errors that occurred during database query
            LOGGER.error("Error retrieving funding data for player {} on join", 
                player.getName().getString(), throwable);
            player.sendMessage(Text.literal("§cFailed to retrieve funding information."), false);
            return null;
        });
    }
}