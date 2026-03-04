package me.naturalsmp.NaturalBounties.features.settings.integrations.external_api;

import com.github.rumsfield.konquest.api.KonquestAPI;
import com.github.rumsfield.konquest.api.manager.KonquestPlayerManager;
import com.github.rumsfield.konquest.api.model.KonquestDiplomacyType;
import com.github.rumsfield.konquest.api.model.KonquestPlayer;
import me.naturalsmp.NaturalBounties.NaturalBounties;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

public class KonquestClass {
    private KonquestClass() {}
    private static KonquestAPI api = null;
    private static boolean getAPI() {
        if (api != null) return true;
        Plugin konquest = Bukkit.getPluginManager().getPlugin("Konquest");
        if (konquest != null && konquest.isEnabled()) {
            RegisteredServiceProvider<KonquestAPI> provider = Bukkit.getServicesManager().getRegistration(KonquestAPI.class);
            if (provider != null) {
                api = provider.getProvider();
                NaturalBounties.debugMessage("Successfully enabled Konquest API", false);
                return true;
            } else {
                NaturalBounties.debugMessage("Failed to enable Konquest API, invalid provider", false);
            }
        } else {
            NaturalBounties.debugMessage("Failed to enable Konquest API, plugin not found or disabled", false);
        }
        return false;
    }

    public static boolean isInSameKingdom(Player player, Player killer) {
        if (getAPI()) {
            KonquestPlayerManager manager = api.getPlayerManager();
            KonquestPlayer player1 = manager.getPlayer(player);
            KonquestPlayer player2 = manager.getPlayer(killer);
            if (player1 == null || player2 == null) return false;
            return player1.getKingdom().equals(player2.getKingdom());
        }
        return false;
    }

    public static boolean isAllied(Player player, Player killer) {
        if (getAPI()) {
            KonquestPlayerManager manager = api.getPlayerManager();
            KonquestPlayer player1 = manager.getPlayer(player);
            KonquestPlayer player2 = manager.getPlayer(killer);
            if (player1 == null || player2 == null) return false;

            return player1.getKingdom().getActiveRelation(player2.getKingdom()) == KonquestDiplomacyType.ALLIANCE;
        }
        return false;
    }
}
