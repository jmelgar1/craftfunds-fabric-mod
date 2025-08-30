package com.jmelgar1.craftfunds;

import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
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
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    
    @Override
    public void onInitializeServer() {
        LOGGER.info("CraftFunds mod initialized on server side!");
        
        // Register commands
        CommandRegistrationCallback.EVENT.register(this::registerCommands);
        
        LOGGER.info("CraftFunds commands registered successfully!");
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
}