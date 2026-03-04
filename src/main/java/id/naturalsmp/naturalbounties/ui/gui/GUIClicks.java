package me.naturalsmp.NaturalBounties.ui.gui;

import me.naturalsmp.NaturalBounties.data.Bounty;
import me.naturalsmp.NaturalBounties.NaturalBounties;
import me.naturalsmp.NaturalBounties.features.settings.display.BountyTracker;
import me.naturalsmp.NaturalBounties.features.settings.display.map.BountyMap;
import me.naturalsmp.NaturalBounties.utils.LoggedPlayers;
import me.naturalsmp.NaturalBounties.features.ConfigOptions;
import me.naturalsmp.NaturalBounties.features.LanguageOptions;
import me.naturalsmp.NaturalBounties.features.settings.money.NumberFormatting;
import net.md_5.bungee.api.chat.*;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;


import static me.naturalsmp.NaturalBounties.features.LanguageOptions.*;

public class GUIClicks {
    enum ClickAction {
        POSTER, TRACKER, VIEW, REMOVE, EDIT, NONE
    }
    private static ClickAction playerRight;
    private static ClickAction playerLeft;
    private static ClickAction playerMiddle;
    private static ClickAction playerBedrock;
    private static ClickAction playerDrop;
    private static ClickAction adminRight;
    private static ClickAction adminLeft;
    private static ClickAction adminMiddle;
    private static ClickAction adminBedrock;
    private static ClickAction adminDrop;
    public static void loadConfiguration(ConfigurationSection configuration) {
        playerRight = getClickActionOrNone(configuration.getString("right"));
        playerLeft = getClickActionOrNone(configuration.getString("left"));
        playerMiddle = getClickActionOrNone(configuration.getString("middle"));
        playerBedrock = getClickActionOrNone(configuration.getString("bedrock"));
        playerDrop = getClickActionOrNone(configuration.getString("drop"));
        adminRight = getClickActionOrNone(configuration.getString("admin.right"));
        adminLeft = getClickActionOrNone(configuration.getString("admin.left"));
        adminMiddle = getClickActionOrNone(configuration.getString("admin.middle"));
        adminBedrock = getClickActionOrNone(configuration.getString("admin.bedrock"));
        adminDrop = getClickActionOrNone(configuration.getString("admin.drop"));
    }

    private static ClickAction getClickActionOrNone(@Nullable String action) {
        if (action == null)
            return ClickAction.NONE;
        try {
            return ClickAction.valueOf(action.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ClickAction.NONE;
        }
    }

    public static List<String> getClickLore(Player player, boolean buyBack, double buyBackPrice) {
        List<String> lore = new ArrayList<>();
        boolean admin = player.hasPermission(NaturalBounties.getAdminPermission());
        if (admin && usingActions(ClickAction.REMOVE, false, true))
            getListMessage("remove-bounty-lore").stream().map(str -> parse(str, player)).forEach(lore::add);
        if (admin && usingActions(ClickAction.EDIT, false, true))
            getListMessage("edit-bounty-lore").stream().map(str -> parse(str, player)).forEach(lore::add);
        if (buyBack && (!admin || !usingActions(ClickAction.REMOVE, false, true))) // only add buy back if there isn't an admin remove action
            getListMessage("buy-back-lore").stream().map(bbLore -> parse(bbLore, buyBackPrice, player)).forEach(lore::add);
        if ((BountyMap.isGiveOwn() || player.hasPermission("NaturalBounties.spawnposter")) && usingActions(ClickAction.POSTER, buyBack, admin))
            getListMessage("give-poster-lore").stream().map(str -> parse(str, player)).forEach(lore::add);
        if ((BountyTracker.isEnabled() && (BountyTracker.isGiveOwnTracker() || BountyTracker.isWriteEmptyTrackers() || admin || player.hasPermission("NaturalBounties.spawntracker") || player.hasPermission("NaturalBounties.writeemptytracker")) && player.hasPermission("NaturalBounties.tracker")) && usingActions(ClickAction.TRACKER, buyBack, admin))
            getListMessage("give-tracker-lore").stream().map(str -> parse(str, player)).forEach(lore::add);
        if (usingActions(ClickAction.VIEW, buyBack, admin))
            getListMessage("view-bounty-lore").stream().map(str -> parse(str, player)).forEach(lore::add);
        return lore;

    }

    private static boolean usingActions(ClickAction clickAction, boolean buyBack, boolean admin) {
        if (admin)
            return adminRight == clickAction || adminLeft == clickAction || adminMiddle == clickAction || adminDrop == clickAction;
        return playerRight == clickAction || (playerLeft == clickAction && !buyBack) || playerMiddle == clickAction || playerDrop == clickAction;
    }

    /**
     * Run click actions for a player clicking on a bounty in the bounty-gui.
     * ClickType.UNKNOWN will run a bedrock click
     * @param player Player to execute the click actions
     * @param bounty Bounty that the player clicked,
     * @param clickType Type of click
     */
    public static void runClickActions(Player player, Bounty bounty, ClickType clickType) {
        NaturalBounties.debugMessage("Running " + clickType.name() + " click action for " + player.getName(), false);
        switch (clickType) {
            case RIGHT -> {
                if (player.hasPermission(NaturalBounties.getAdminPermission())) {
                    runAction(player, bounty, adminRight);
                } else {
                    runAction(player, bounty, playerRight);
                }
            }
            case LEFT -> {
                if (player.hasPermission(NaturalBounties.getAdminPermission())) {
                    runAction(player, bounty, adminLeft);
                } else {
                    if (player.getUniqueId().equals(bounty.getUUID()) && ConfigOptions.getMoney().isBuyOwn() && player.hasPermission("NaturalBounties.buyown")) {
                        runAction(player, bounty, ClickAction.REMOVE); // buy back
                    } else {
                        runAction(player, bounty, playerLeft);
                    }
                }
            }
            case MIDDLE -> {
                if (player.hasPermission(NaturalBounties.getAdminPermission())) {
                    runAction(player, bounty, adminMiddle);
                } else {
                    runAction(player, bounty, playerMiddle);
                }
            }
            case UNKNOWN -> {
                // bedrock
                if (player.hasPermission(NaturalBounties.getAdminPermission())) {
                    runAction(player, bounty, adminBedrock);
                } else {
                    runAction(player, bounty, playerBedrock);
                }
            }
            case DROP -> {
                if (player.hasPermission(NaturalBounties.getAdminPermission())) {
                    runAction(player, bounty, adminDrop);
                } else {
                    runAction(player, bounty, playerDrop);
                }
            }
            default -> {
                // click type not registered - ignore
            }
        }
    }

    private static void runAction(Player player, Bounty bounty, ClickAction clickAction) {
        switch (clickAction) {
            case EDIT -> {
                if (player.hasPermission(NaturalBounties.getAdminPermission())) {
                    player.closeInventory();
                    String messageText = LanguageOptions.parse(LanguageOptions.getMessage("edit-bounty-clickable").replace("{receiver}", bounty.getName()), player);
                    TextComponent message =  LanguageOptions.getTextComponent(messageText);

                    BaseComponent prefix = LanguageOptions.getTextComponent(LanguageOptions.parse(LanguageOptions.getPrefix(), player));
                    message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(messageText)));
                    message.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/" + ConfigOptions.getPluginBountyCommands().get(0) + " edit " + bounty.getName() + " "));
                    prefix.addExtra(message);
                    player.spigot().sendMessage(prefix);
                }
            }
            case REMOVE -> {
                if (player.hasPermission(NaturalBounties.getAdminPermission())) {
                    GUI.openGUI(player, "confirm", 1, bounty.getUUID(), 0);
                } else if (player.getUniqueId().equals(bounty.getUUID()) && ConfigOptions.getMoney().isBuyOwn() && player.hasPermission("NaturalBounties.buyown")) {
                    // buy back
                    double balance = NumberFormatting.getBalance(player);
                    if (balance >= (int) (bounty.getTotalDisplayBounty() * ConfigOptions.getMoney().getBuyOwnCostMultiply())) {
                        GUI.openGUI(player, "confirm", 1, bounty.getUUID(), (ConfigOptions.getMoney().getBuyOwnCostMultiply() * bounty.getTotalDisplayBounty()));
                    } else {
                        player.sendMessage(parse(getPrefix() + getMessage("broke"), (bounty.getTotalDisplayBounty() * ConfigOptions.getMoney().getBuyOwnCostMultiply()), player));
                    }
                }
            }
            case POSTER -> {
                if (BountyMap.isGiveOwn() || player.hasPermission("NaturalBounties.spawnposter")) {
                    player.closeInventory();
                    Bukkit.getServer().dispatchCommand(player, ConfigOptions.getPluginBountyCommands().get(0) + " poster " + LoggedPlayers.getPlayerName(bounty.getUUID()));
                }
            }
            case TRACKER -> {
                if (BountyTracker.isEnabled() && (BountyTracker.isGiveOwnTracker() || BountyTracker.isWriteEmptyTrackers() || player.hasPermission(NaturalBounties.getAdminPermission()) || player.hasPermission("NaturalBounties.spawntracker") || player.hasPermission("NaturalBounties.writeemptytracker")) && player.hasPermission("NaturalBounties.tracker")) {
                    player.closeInventory();
                    Bukkit.getServer().dispatchCommand(player, ConfigOptions.getPluginBountyCommands().get(0) + " tracker " + LoggedPlayers.getPlayerName(bounty.getUUID()));
                }
            }
            case VIEW -> GUI.openGUI(player, "view-bounty", 1, bounty.getUUID());
            case NONE -> {
                // no action
            }
        }
    }
}
