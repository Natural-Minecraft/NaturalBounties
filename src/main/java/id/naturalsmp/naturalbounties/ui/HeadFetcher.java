package me.naturalsmp.NaturalBounties.ui;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import me.naturalsmp.NaturalBounties.NaturalBounties;
import me.naturalsmp.NaturalBounties.ui.gui.PlayerGUInfo;
import me.naturalsmp.NaturalBounties.utils.LoggedPlayers;
import me.naturalsmp.NaturalBounties.utils.tasks.HeadLoader;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class HeadFetcher {
    private static final Cache<UUID, ItemStack> savedHeads = CacheBuilder.newBuilder()
            .expireAfterWrite(15, TimeUnit.MINUTES)
            .build();

    public void loadHeads(Player player, PlayerGUInfo guInfo, List<QueuedHead> heads) {
        HeadLoader headLoader = new HeadLoader(player, guInfo, heads, savedHeads);
        headLoader.setTaskImplementation(NaturalBounties.getServerImplementation().async().runAtFixedRate(headLoader, 1, 4));
    }


    public static ItemStack getUnloadedHead(UUID uuid) {
        ItemStack head = savedHeads.getIfPresent(uuid);
        if (head != null)
            return head;
        head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        assert meta != null;
        try {
            meta.setOwningPlayer(Bukkit.getOfflinePlayer(uuid));
        } catch (NullPointerException e) {
            if (NaturalBounties.getServerVersion() >= 18) {
                try {
                    PlayerProfile profile = Bukkit.createPlayerProfile(uuid, LoggedPlayers.getPlayerName(uuid));
                    meta.setOwnerProfile(profile);
                } catch (IllegalArgumentException ignored) {
                    // The name of the profile is longer than 16 characters
                    NaturalBounties.debugMessage("Could not get an unloaded head for: " + LoggedPlayers.getPlayerName(uuid), true);
                }
            }
        }
        head.setItemMeta(meta);
        return head;
    }
}
