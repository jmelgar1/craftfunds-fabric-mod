package com.example.craftfunds;

import net.fabricmc.api.DedicatedServerModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    
    /**
     * Logger instance for this mod using SLF4J
     */
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    
    /**
     * Server-side mod initialization method.
     * This method is called when the mod is initialized on the server.
     * 
     * Use this method to:
     * - Register server-side event listeners
     * - Initialize server-side configuration
     * - Register custom commands
     * - Set up data persistence systems
     * - Initialize server-side networking handlers
     */
    @Override
    public void onInitializeServer() {
        LOGGER.info("CraftFunds mod initialized on server side!");
        
        // Add your server-side initialization code here
        // Examples:
        // - ServerLifecycleEvents.SERVER_STARTED.register(this::onServerStarted);
        // - CommandRegistrationCallback.EVENT.register(this::registerCommands);
        // - ServerPlayerEvents.AFTER_RESPAWN.register(this::onPlayerRespawn);
    }
    
    /**
     * Example method for server started event
     * Uncomment and implement when needed
     */
    /*
    private void onServerStarted(MinecraftServer server) {
        LOGGER.info("Server has started, CraftFunds is ready!");
    }
    */
    
    /**
     * Example method for command registration
     * Uncomment and implement when needed
     */
    /*
    private void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        // Register your custom commands here
        LOGGER.info("Registering CraftFunds commands");
    }
    */
}