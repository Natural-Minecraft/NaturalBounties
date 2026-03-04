package me.naturalsmp.NaturalBounties.features.settings.databases;

import me.naturalsmp.NaturalBounties.utils.DataManager;
import me.naturalsmp.NaturalBounties.features.settings.ResourceConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.Objects;

public class Databases extends ResourceConfiguration {
    @Override
    protected void loadConfiguration(YamlConfiguration config) {
        DataManager.loadDatabaseConfig(Objects.requireNonNull(config), plugin);
    }

    @Override
    protected String[] getModifiableSections() {
        return new String[]{""};
    } // means all sections

    @Override
    protected String getPath() {
        return "settings/databases.yml";
    }
}
