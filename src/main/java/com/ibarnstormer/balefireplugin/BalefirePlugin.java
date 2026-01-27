package com.ibarnstormer.balefireplugin;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.ibarnstormer.balefireplugin.interactions.BalefireBlastInteraction;

import javax.annotation.Nonnull;

public class BalefirePlugin extends JavaPlugin {

    private static BalefirePlugin instance;
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public BalefirePlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    public static BalefirePlugin getInstance() {
        return instance;
    }

    @Override
    protected void setup() {
        instance = this;

        LOGGER.atInfo().log("Setting up plugin: " + this.getName());
        Interaction.CODEC.register("BalefireBlast", BalefireBlastInteraction.class, BalefireBlastInteraction.CODEC);
    }

}