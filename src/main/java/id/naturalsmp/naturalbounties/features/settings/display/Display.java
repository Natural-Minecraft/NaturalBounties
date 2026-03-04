package me.naturalsmp.NaturalBounties.features.settings.display;

import me.naturalsmp.NaturalBounties.features.settings.ResourceConfiguration;
import me.naturalsmp.NaturalBounties.features.settings.display.map.BountyBoard;
import me.naturalsmp.NaturalBounties.features.settings.display.map.BountyMap;
import me.naturalsmp.NaturalBounties.ui.SkinManager;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.Objects;

public class Display extends ResourceConfiguration {
    @Override
    protected void loadConfiguration(YamlConfiguration config) {
        WantedTags.loadConfiguration(Objects.requireNonNull(config.getConfigurationSection("wanted-tag")), plugin);
        BountyMap.loadConfiguration(Objects.requireNonNull(config.getConfigurationSection("bounty-posters")));
        BountyBoard.loadConfiguration(Objects.requireNonNull(config.getConfigurationSection("bounty-board")));
        BountyTracker.loadConfiguration(Objects.requireNonNull(config.getConfigurationSection("bounty-tracker")));
        BountyHunt.loadConfiguration(Objects.requireNonNull(config.getConfigurationSection("bounty-hunt")));
        SkinManager.loadConfiguration(Objects.requireNonNull(config.getConfigurationSection("skins")));
    }

    @Override
    protected String[] getModifiableSections() {
        return new String[]{"wanted-tag.level"};
    }

    @Override
    protected String getPath() {
        return "settings/display.yml";
    }
}
