package com.jmelgar1.craftfunds;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.*;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.util.Formatting;

import java.net.URI;

public class DonateCommand {

    /**
     * Registers the /donate command with the command dispatcher.
     * 
     * The command is configured to:
     * - Require the sender to be a player (not console/command block)
     * - Execute the donate command logic when invoked
     * - Provide appropriate error messages for invalid usage
     * 
     * @param dispatcher The command dispatcher to register with
     */
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
            CommandManager.literal("donate")
                .requires(source -> source.isExecutedByPlayer()) // Only players can use this command
                .executes(DonateCommand::execute)
        );
    }

    /**
     * Executes the /donate command logic.
     * 
     * This method:
     * 1. Validates that the command source is a player
     * 2. Sends a "Donation" message to the player
     * 3. Logs the command execution for server monitoring
     * 4. Returns success status to the command system
     * 
     * @param context The command execution context
     * @return Command execution result (1 for success, 0 for failure)
     */
    private static int execute(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        try {
            // Get the player who executed the command
            ServerPlayerEntity player = source.getPlayerOrThrow();
            
            // Create clickable PayPal donation link as URI from config
            String donationUrl = ConfigManager.getInstance().getPayPalDonationUrl();
            URI donationUri = URI.create(donationUrl);
            final Text clickableLink = Text.literal("Click here to donate!").styled(s -> 
                s.withClickEvent(new ClickEvent.OpenUrl(donationUri))
                    .withHoverEvent(new HoverEvent.ShowText(Text.literal("Click to Open Link!")))
                    .withColor(Formatting.BLUE).withUnderline(true));
            
            // Send the clickable link to the player
            player.sendMessage(clickableLink, false);
            
            // Log the command execution (useful for server monitoring)
            CraftFunds.LOGGER.info("Player {} executed /donate command", player.getName().getString());
            
            // Return 1 to indicate successful command execution
            return 1;
            
        } catch (Exception e) {
            // Handle any unexpected errors gracefully
            CraftFunds.LOGGER.error("Error executing /donate command", e);
            
            // Try to send error message to source if possible
            try {
                source.sendMessage(Text.literal("An error occurred while executing the command."));
            } catch (Exception sendError) {
                CraftFunds.LOGGER.error("Could not send error message to command source", sendError);
            }
            
            // Return 0 to indicate command failure
            return 0;
        }
    }
}