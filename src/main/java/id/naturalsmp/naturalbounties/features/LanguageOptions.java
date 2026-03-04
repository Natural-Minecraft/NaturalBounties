package me.naturalsmp.NaturalBounties.features;

import me.naturalsmp.NaturalBounties.data.Bounty;
import me.naturalsmp.NaturalBounties.NaturalBounties;
import me.naturalsmp.NaturalBounties.data.player_data.PlayerData;
import me.naturalsmp.NaturalBounties.features.settings.display.BountyHunt;
import me.naturalsmp.NaturalBounties.features.settings.display.BountyTracker;
import me.naturalsmp.NaturalBounties.features.settings.display.map.BountyMap;
import me.naturalsmp.NaturalBounties.features.settings.immunity.ImmunityManager;
import me.naturalsmp.NaturalBounties.features.settings.money.NumberFormatting;
import me.naturalsmp.NaturalBounties.ui.SkinManager;
import me.naturalsmp.NaturalBounties.ui.gui.GUI;
import me.naturalsmp.NaturalBounties.ui.gui.PlayerGUInfo;
import me.naturalsmp.NaturalBounties.ui.gui.display_items.PlayerItem;
import me.naturalsmp.NaturalBounties.utils.BountyManager;
import me.naturalsmp.NaturalBounties.utils.DataManager;
import me.naturalsmp.NaturalBounties.utils.LoggedPlayers;
import me.naturalsmp.NaturalBounties.utils.Tutorial;
import me.naturalsmp.NaturalBounties.data.Whitelist;
import me.naturalsmp.NaturalBounties.features.challenges.ChallengeManager;
import me.naturalsmp.NaturalBounties.features.settings.integrations.external_api.LocalTime;
import me.naturalsmp.NaturalBounties.features.settings.integrations.external_api.PlaceholderAPIClass;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.iridium.iridiumcolorapi.IridiumColorAPI;


import static me.naturalsmp.NaturalBounties.features.settings.integrations.external_api.LocalTime.formatTime;
import static net.md_5.bungee.api.ChatColor.COLOR_CHAR;

public class LanguageOptions {

    private static String prefix;


    private static final Map<String, String> messages = new HashMap<>();
    private static final Map<String, List<String>> listMessages = new HashMap<>();

    public static File getLanguageFile() {
        return new File(NaturalBounties.getInstance().getDataFolder() + File.separator + "language.yml");
    }

    public static void reloadOptions() throws IOException {
        NaturalBounties bounties = NaturalBounties.getInstance();
        File language = getLanguageFile();

        // create language file if it doesn't exist
        if (!language.exists()) {
            bounties.saveResource("language.yml", false);
        }

        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(language);
        boolean saveChanges = true;
        if (configuration.getKeys(true).size() <= 2) {
            saveChanges = false;
            Bukkit.getLogger().severe("[NaturalBounties] Loaded an empty configuration for the language.yml file. Fix the YAML formatting errors, or the messages may not work!\nFor more information on YAML formatting, see here: https://spacelift.io/blog/yaml");
        }
        // remove stats from help.view
        if (!configuration.isSet("help.stats") && configuration.isSet("help.view")) {
            List<String> statsList = Arrays.asList(
                    "&9/bounty top (all/kills/claimed/deaths/set/immunity) <list> &8- &dLists the top 10 players with the respective stats.",
                    "&9/bounty stat (all/kills/claimed/deaths/set/immunity) &8- &dView your bounty stats.",
                    "&9/bounty stat (all/kills/claimed/deaths/set/immunity) (player) &8- &dView another player's stats.");
            List<String> viewList = new ArrayList<>(configuration.getStringList("help.view"));
            viewList.removeIf(statsList::contains);
            configuration.set("help.view", viewList);
        }

        // fill in any default options that aren't present
        if (NaturalBounties.getInstance().getResource("language.yml") != null) {
            configuration.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(Objects.requireNonNull(NaturalBounties.getInstance().getResource("language.yml")))));
            for (String key : Objects.requireNonNull(configuration.getDefaults()).getKeys(true)) {
                if (!configuration.isSet(key))
                    configuration.set(key, configuration.getDefaults().get(key));
            }
            if (saveChanges)
                configuration.save(language);
        }

        for (String key : configuration.getKeys(true)) {
            if (configuration.isList(key)) {
                listMessages.put(key, configuration.getStringList(key));
            } else {
                messages.put(key, configuration.getString(key));
            }
        }


        prefix = configuration.getString("prefix");

        Tutorial.loadConfiguration(Objects.requireNonNull(configuration.getConfigurationSection("tutorial")));

    }

    public static String getMessage(String key) {
        if (messages.containsKey(key))
            return messages.get(key);
        if (listMessages.containsKey(key))
            return String.join("\n", listMessages.get(key));
        return "&cInvalid Message! There may be YAML errors in the language.yml file, or this is a bug!";
    }

    public static boolean isMessage(String key) {
        return messages.containsKey(key) || listMessages.containsKey(key);
    }

    public static List<String> getListMessage(String key) {
        if (listMessages.containsKey(key))
            return listMessages.get(key);
        if (messages.containsKey(key))
            return new ArrayList<>(Arrays.stream(messages.get(key).split("\n")).toList());
        return new ArrayList<>(List.of("&cInvalid Message! There may be YAML errors in the language.yml file, or this is a bug!"));
    }

    private static int getAdjustedPage(CommandSender sender, int page) {
        if (page < 1)
            page = 1;
        if (page == 2 && !sender.hasPermission("NaturalBounties.view"))
            page++;
        if (page == 3 && !sender.hasPermission("NaturalBounties.stats"))
            page++;
        if (page == 4 && !sender.hasPermission("NaturalBounties.set"))
            page++;
        if (page == 5 && !(sender.hasPermission("NaturalBounties.whitelist") && Whitelist.isEnabled()))
            page++;
        if (page == 6 && !(sender.hasPermission("NaturalBounties.buyown") && ConfigOptions.getMoney().isBuyOwn()) && !(sender.hasPermission("NaturalBounties.buyimmunity") && ImmunityManager.getImmunityType() != ImmunityManager.ImmunityType.DISABLE))
            page++;
        if (page == 7 && !(sender.hasPermission("NaturalBounties.removeimmunity") && ImmunityManager.getImmunityType() != ImmunityManager.ImmunityType.DISABLE) && !(sender.hasPermission("NaturalBounties.removeset") && !sender.hasPermission(NaturalBounties.getAdminPermission())))
            page++;
        if (page == 8 && !(sender.hasPermission(NaturalBounties.getAdminPermission()) || sender.hasPermission("NaturalBounties.spawntracker") || (BountyTracker.isGiveOwnTracker() && sender.hasPermission("NaturalBounties.tracker"))) && !(sender.hasPermission(NaturalBounties.getAdminPermission()) || BountyMap.isGiveOwn() || sender.hasPermission("NaturalBounties.spawnposter")))
            page++;
        if (page == 9 && (!sender.hasPermission("NaturalBounties.challenges") || !ChallengeManager.isEnabled()))
            page++;
        if (page == 10 && (!BountyHunt.isEnabled() || (!sender.hasPermission("NaturalBounties.hunt.start") && !sender.hasPermission("NaturalBounties.hunt.participate"))))
            page++;
        if (page == 11 && !sender.hasPermission(NaturalBounties.getAdminPermission()))
            page++;
        if (page >= 12)
            page = 1;
        return page;
    }

    public static void sendHelpMessage(CommandSender sender) {
        Player parser = sender instanceof Player player ? player : null;
        sender.sendMessage(parse(getPrefix() + getMessage("help.title"), parser));
        sendHelpMessage(sender, getListMessage("help.basic"));
        if (sender.hasPermission("NaturalBounties.view")) {
            sendHelpMessage(sender, getListMessage("help.view"));
        }
        if (sender.hasPermission("NaturalBounties.stats")) {
            sendHelpMessage(sender, getListMessage("help.stats"));
        }
        if (sender.hasPermission("NaturalBounties.set")) {
            sendHelpMessage(sender, getListMessage("help.set"));
        }
        if (sender.hasPermission("NaturalBounties.whitelist") && Whitelist.isEnabled()) {
            sendHelpMessage(sender, getListMessage("help.whitelist"));
            if (Whitelist.isAllowTogglingWhitelist()) {
                sendHelpMessage(sender, getListMessage("help.blacklist"));
            }
        }
        if (sender.hasPermission("NaturalBounties.buyown") && ConfigOptions.getMoney().isBuyOwn()) {
            sendHelpMessage(sender, getListMessage("help.buy-own"));
        }
        if (sender.hasPermission("NaturalBounties.buyimmunity") && ImmunityManager.getImmunityType() != ImmunityManager.ImmunityType.DISABLE) {
            switch (ImmunityManager.getImmunityType()) {
                case PERMANENT:
                    sendHelpMessage(sender, getListMessage("help.buy-immunity.permanent"));
                    break;
                case SCALING:
                    sendHelpMessage(sender, getListMessage("help.buy-immunity.scaling"));
                    break;
                case TIME:
                    sendHelpMessage(sender, getListMessage("help.buy-immunity.time"));
                    break;
            }
        }
        if (sender.hasPermission("NaturalBounties.removeimmunity")) {
            sendHelpMessage(sender, getListMessage("help.remove-immunity"));
        }
        if (sender.hasPermission("NaturalBounties.removeset") && !sender.hasPermission(NaturalBounties.getAdminPermission())) {
            sendHelpMessage(sender, getListMessage("help.remove-set"));
        }
        if (sender.hasPermission(NaturalBounties.getAdminPermission()) || BountyMap.isGiveOwn() || sender.hasPermission("NaturalBounties.spawnposter")) {
            sendHelpMessage(sender, getListMessage("help.poster-own"));
            if (sender.hasPermission(NaturalBounties.getAdminPermission()))
                sendHelpMessage(sender, getListMessage("help.poster-other"));
        }
        if (BountyTracker.isEnabled())
            if (sender.hasPermission(NaturalBounties.getAdminPermission()) || sender.hasPermission("NaturalBounties.spawntracker") || (BountyTracker.isGiveOwnTracker() && sender.hasPermission("NaturalBounties.tracker"))) {
                sendHelpMessage(sender, getListMessage("help.tracker-own"));
                if (sender.hasPermission(NaturalBounties.getAdminPermission()))
                    sendHelpMessage(sender, getListMessage("help.tracker-other"));
            }
        if (sender.hasPermission("NaturalBounties.challenges") && ChallengeManager.isEnabled()) {
            sendHelpMessage(sender, getListMessage("help.challenges"));
        }
        if (BountyHunt.isEnabled()) {
            if (sender.hasPermission(NaturalBounties.getAdminPermission()) || sender.hasPermission("NaturalBounties.hunt.start")) {
                sendHelpMessage(sender, getListMessage("help.hunt-start"));
            }
            if (sender.hasPermission(NaturalBounties.getAdminPermission()) || sender.hasPermission("NaturalBounties.hunt.participate")) {
                sendHelpMessage(sender, getListMessage("help.hunt-participate"));
            }
        }
        if (sender.hasPermission(NaturalBounties.getAdminPermission())) {
            sendHelpMessage(sender, getListMessage("help.admin"));
        }

        if (ImmunityManager.isPermissionImmunity() && sender.hasPermission("NaturalBounties.immune")) {
            sendHelpMessage(sender, getListMessage("help.immune"));
        }
        sender.sendMessage(ChatColor.GRAY + "" + ChatColor.STRIKETHROUGH + "                                                 ");
    }

    public static void sendHelpMessage(CommandSender sender, int page) {
        if (!(sender instanceof Player parser)) {
            sendHelpMessage(sender);
            return;
        }
        sender.sendMessage(parse(getMessage("help.title"), parser));
        page = getAdjustedPage(sender, page);


        switch (page) {
            case 1:
                // basic
                sendHelpMessage(sender, getListMessage("help.basic"));
                if (ImmunityManager.isPermissionImmunity() && sender.hasPermission("NaturalBounties.immune")) {
                    sendHelpMessage(sender, getListMessage("help.immune"));
                }
                break;
            case 2:
                // view
                sendHelpMessage(sender, getListMessage("help.view"));
                break;
            case 3:
                // stats
                sendHelpMessage(sender, getListMessage("help.stats"));
                break;
            case 4:
                // set
                sendHelpMessage(sender, getListMessage("help.set"));
                break;
            case 5:
                // whitelist
                sendHelpMessage(sender, getListMessage("help.whitelist"));
                if (Whitelist.isAllowTogglingWhitelist())
                    sendHelpMessage(sender, getListMessage("help.blacklist"));
                break;
            case 6:
                // buy
                if (sender.hasPermission("NaturalBounties.buyown") && ConfigOptions.getMoney().isBuyOwn()) {
                    sendHelpMessage(sender, getListMessage("help.buy-own"));
                }
                if (sender.hasPermission("NaturalBounties.buyimmunity") && ImmunityManager.getImmunityType() != ImmunityManager.ImmunityType.DISABLE) {
                    switch (ImmunityManager.getImmunityType()) {
                        case PERMANENT:
                            sendHelpMessage(sender, getListMessage("help.buy-immunity.permanent"));
                            break;
                        case SCALING:
                            sendHelpMessage(sender, getListMessage("help.buy-immunity.scaling"));
                            break;
                        case TIME:
                            sendHelpMessage(sender, getListMessage("help.buy-immunity.time"));
                            break;
                    }
                }
                break;
            case 7:
                // remove
                if (sender.hasPermission("NaturalBounties.removeimmunity") && ImmunityManager.getImmunityType() != ImmunityManager.ImmunityType.DISABLE)
                    sendHelpMessage(sender, getListMessage("help.remove-immunity"));
                if (sender.hasPermission("NaturalBounties.removeset") && !sender.hasPermission(NaturalBounties.getAdminPermission()))
                    sendHelpMessage(sender, getListMessage("help.remove-set"));
                break;
            case 8:
                // item
                if (sender.hasPermission(NaturalBounties.getAdminPermission()) || BountyMap.isGiveOwn()|| sender.hasPermission("NaturalBounties.spawnposter")) {
                    sendHelpMessage(sender, getListMessage("help.poster-own"));
                    if (sender.hasPermission(NaturalBounties.getAdminPermission()))
                        sendHelpMessage(sender, getListMessage("help.poster-other"));
                }
                if (BountyTracker.isEnabled())
                    if (sender.hasPermission(NaturalBounties.getAdminPermission()) || sender.hasPermission("NaturalBounties.spawntracker") || (BountyTracker.isGiveOwnTracker() && sender.hasPermission("NaturalBounties.tracker"))) {
                        sendHelpMessage(sender, getListMessage("help.tracker-own"));
                        if (sender.hasPermission(NaturalBounties.getAdminPermission()))
                            sendHelpMessage(sender, getListMessage("help.tracker-other"));
                    }
                break;
            case 9:
                // challenges
                sendHelpMessage(sender, getListMessage("help.challenges"));
                break;
            case 10:
                // bounty hunt
                if (sender.hasPermission(NaturalBounties.getAdminPermission()) || sender.hasPermission("NaturalBounties.hunt.start")) {
                    sendHelpMessage(sender, getListMessage("help.hunt-start"));
                }
                if (sender.hasPermission(NaturalBounties.getAdminPermission()) || sender.hasPermission("NaturalBounties.hunt.participate")) {
                    sendHelpMessage(sender, getListMessage("help.hunt-participate"));
                }
                break;
            case 11:
                // admin
                sendHelpMessage(sender, getListMessage("help.admin"));
                break;
            default:
                sender.sendMessage("You're not supposed to be here...");
                sender.sendMessage("Join the discord! https://discord.gg/zEsUzwYEx7");
                break;
        }

        sendPageLine(sender, page);
    }

    public static void sendPageLine(CommandSender sender, int currentPage) {
        Player parser = sender instanceof Player player ? player : null;
        int previousPage = currentPage-1;
        int calculatedPrevPage = getAdjustedPage(sender, previousPage);
        while (previousPage > 0 && calculatedPrevPage >= currentPage) {
            previousPage--;
            calculatedPrevPage = getAdjustedPage(sender, previousPage);
        }
        int nextPage = getAdjustedPage(sender, currentPage + 1);


        Tutorial.sendUnifiedPageLine(sender, currentPage, parser, previousPage, nextPage, "help", 12);
    }

    public static void sendHelpMessage(CommandSender sender, List<String> message) {
        Player parser = sender instanceof Player player ? player : null;
        for (String str : message) {
            str = str.replace("{whitelist}", (NumberFormatting.getCurrencyPrefix() + NumberFormatting.formatNumber(Whitelist.getCost()) + NumberFormatting.getCurrencySuffix()));
            sender.sendMessage(parse(str, parser));
        }
    }

    /**
     * Will not add the player prefix or player suffix
     * Mainly for unknown player
     */
    public static String parse(String str, String player, OfflinePlayer receiver) {
        str = str.replace("{receiver}", (player));
        str = str.replace("{player}", (player));
        return parse(str, receiver);
    }

    public static String parse(String str, OfflinePlayer receiver) {
        if (receiver != null) {
            str = str.replace("{sort_type}", GUI.getActiveSortType(receiver.getUniqueId()));
        }
        if (str.contains("{time}")) {
            String timeString = formatTime(System.currentTimeMillis(), LocalTime.TimeFormat.PLAYER, receiver.getPlayer());
            str = str.replace("{time}", (timeString));
        }
        str = str.replace("{next_challenges}", formatTime(ChallengeManager.getNextChallengeChange() - System.currentTimeMillis(), LocalTime.TimeFormat.RELATIVE))
                .replace("{min_bounty}", (NumberFormatting.getValue(ConfigOptions.getMoney().getMinBounty())))
                .replace("{c_prefix}", (NumberFormatting.getCurrencyPrefix()))
                .replace("{c_suffix}", (NumberFormatting.getCurrencySuffix()))
                .replace("{whitelist_cost}", NumberFormatting.getCurrencyPrefix() + NumberFormatting.formatNumber(Whitelist.getCost()) + NumberFormatting.getCurrencySuffix())
                .replace("{tax}", (NumberFormatting.formatNumber(ConfigOptions.getMoney().getBountyTax() * 100)))
                .replace("{buy_back_interest}", (NumberFormatting.formatNumber(ConfigOptions.getMoney().getBuyOwnCostMultiply() * 100)))
                .replace("{permanent_cost}", (NumberFormatting.getCurrencyPrefix() + NumberFormatting.formatNumber(ImmunityManager.getPermanentCost()) + NumberFormatting.getCurrencySuffix()))
                .replace("{scaling_ratio}", (NumberFormatting.formatNumber(ImmunityManager.getScalingRatio())))
                .replace("{time_immunity}", (formatTime((long) (ImmunityManager.getTime() * 1000L), LocalTime.TimeFormat.RELATIVE)));


        if (receiver != null) {
            Bounty bounty = BountyManager.getBounty(receiver.getUniqueId());
            if (bounty != null) {
                str = str.replace("{min_expire}", (formatTime(BountyExpire.getLowestExpireTime(bounty), LocalTime.TimeFormat.RELATIVE)))
                        .replace("{max_expire}", (formatTime(BountyExpire.getHighestExpireTime(bounty), LocalTime.TimeFormat.RELATIVE)))
                        .replace("{bounty}", NumberFormatting.getCurrencyPrefix() + NumberFormatting.formatNumber(bounty.getTotalDisplayBounty()) + NumberFormatting.getCurrencySuffix())
                        .replace("{bounty_value}", NumberFormatting.getValue(bounty.getTotalDisplayBounty()) );
            } else {
                str = str.replace("{min_expire}", "");
                str = str.replace("{max_expire}", "");
            }
            if (receiver.getName() != null) {
                str = str.replace("{player}", getMessage("player-prefix") + receiver.getName() + getMessage("player-suffix"))
                        .replace("{receiver}", getMessage("player-prefix") + receiver.getName() + getMessage("player-suffix"))
                        .replace("{viewer}", getMessage("player-prefix") + receiver.getName() + getMessage("player-suffix"));
            } else {
                str = str.replace("{player}", getMessage("player-prefix") + LoggedPlayers.getPlayerName(receiver.getUniqueId()) + getMessage("player-prefix"))
                        .replace("{receiver}", getMessage("player-prefix") + LoggedPlayers.getPlayerName(receiver.getUniqueId()) + getMessage("player-suffix"))
                        .replace("{viewer}", getMessage("player-prefix") + LoggedPlayers.getPlayerName(receiver.getUniqueId()) + getMessage("player-suffix"));
            }
            if (str.contains("{balance}"))
                str = str.replace("{balance}", (NumberFormatting.getCurrencyPrefix() + NumberFormatting.formatNumber(NumberFormatting.getBalance(receiver)) + NumberFormatting.getCurrencySuffix()));
            PlayerData playerData = DataManager.getPlayerData(receiver.getUniqueId());
            Whitelist whitelist = playerData.getWhitelist();
            str = str.replace("{whitelist}", (whitelist.toString()));
            String mode = whitelist.isBlacklist() ? "Blacklist" : "Whitelist";
            str = str.replace("{mode}", mode);
            mode = whitelist.isBlacklist() ? "false" : "true";
            str = str.replace("{mode_raw}", mode);
            String notification = playerData.getBroadcastSettings().toString();
            str = str.replace("{notification}", notification)
                    .replace("{immunity}", NumberFormatting.formatNumber(ImmunityManager.getImmunity(receiver.getUniqueId())));

            // {sort_type_(gui)} turns into the name of the sort type in the GUI
            while (str.contains("{sort_type_") && str.substring(str.indexOf("{sort_type_")).contains("}")) {
                String stringValue = str.substring(str.indexOf("{sort_type_") + 11, str.indexOf("{sort_type_") + str.substring(str.indexOf("{sort_type_")).indexOf("}"));
                str = str.replace("{sort_type_" + stringValue + "}", GUI.parseSortType(stringValue, DataManager.getPlayerData(receiver.getUniqueId()).getGUISortType(stringValue)));
            }

            // {whitelist2} turns into the name of the second player in the receiver's whitelist
            while (str.contains("{whitelist") && str.substring(str.indexOf("{whitelist")).contains("}")) {
                int num;
                String stringValue = str.substring(str.indexOf("{whitelist") + 10, str.indexOf("{whitelist") + str.substring(str.indexOf("{whitelist")).indexOf("}"));
                try {
                    num = Integer.parseInt(stringValue);
                } catch (NumberFormatException e) {
                    str = str.replace("{whitelist" + stringValue + "}", "<Error>");
                    continue;
                }
                if (num < 1)
                    num = 1;
                if (whitelist.getList().size() > num)
                    str = str.replace("{whitelist" + stringValue + "}", "");
                else
                    str = str.replace("{whitelist" + stringValue + "}", LoggedPlayers.getPlayerName(whitelist.getList().last()));
            }
            // parsing for GUI
            if (receiver.isOnline() && GUI.playerInfo.containsKey(receiver.getUniqueId())) {
                PlayerGUInfo info = GUI.playerInfo.get(receiver.getUniqueId());
                str = str.replace("{page}", info.page() + "")
                        .replace("{page_max}", info.maxPage() + "");

                // check for {player<x>}
                while (str.contains("{player") && str.substring(str.indexOf("{player")).contains("}")) {
                    String replacement = "";
                    String slotString = str.substring(str.indexOf("{player") + 7, str.substring(str.indexOf("{player")).indexOf("}") + str.substring(0, str.indexOf("{player")).length());
                    try {
                        int slot = Integer.parseInt(slotString);
                        if (info.displayItems().size() > slot-1 && info.displayItems().get(slot-1) instanceof PlayerItem playerItem) {
                            replacement = LoggedPlayers.getPlayerName(playerItem.getUuid());
                        }
                    } catch (NumberFormatException e) {
                        Bukkit.getLogger().warning("Error getting player in command: \n" + str);
                    }
                    str = str.replace(("{player" + slotString + "}"), (replacement));
                }
            }
            // papi parse
            if (ConfigOptions.getIntegrations().isPapiEnabled()) {
                str = new PlaceholderAPIClass().parse(receiver, str);
            }
        }

        return color(str);
    }

    public static String parse(String str, long time, LocalTime.TimeFormat format, OfflinePlayer receiver) {
        if (str.contains("{time}")) {
            String timeString = formatTime(time, format, receiver.getPlayer());
            str = str.replace("{time}", (timeString));
        }
        str = str.replace("{amount}", (time + ""));
        return parse(str, receiver);
    }

    public static String parse(String str, double amount, OfflinePlayer receiver) {
        str = str.replace("{amount}", (NumberFormatting.getCurrencyPrefix() + NumberFormatting.formatNumber(amount) + NumberFormatting.getCurrencySuffix()));
        str = str.replace("{amount_plain}", NumberFormatting.formatNumber(amount));
        return parse(str, receiver);
    }

    public static String parse(String str, String player, double amount, OfflinePlayer receiver) {
        str = str.replace("{player}", (player));
        return parse(str,amount,receiver);
    }

    public static String parse(String str, double amount, long time, LocalTime.TimeFormat format, OfflinePlayer receiver) {
        if (str.contains("{time}")) {
            String timeString = formatTime(time, format, receiver.getPlayer());
            str = str.replace("{time}", (timeString));
        }
        return parse(str, amount, receiver);
    }

    /**
     * This does not add the player prefix or suffix
     * Used for console name
     */
    public static String parse(String str, String player, double amount, double bounty, OfflinePlayer receiver) {
        str = str.replace("{bounty}", (NumberFormatting.getCurrencyPrefix() + NumberFormatting.formatNumber(bounty) + NumberFormatting.getCurrencySuffix()));
        str = str.replace("{bounty_plain}", NumberFormatting.formatNumber(bounty));
        return parse(str, player, amount, receiver);
    }

    public static String parse(String str, double amount, double bounty, OfflinePlayer receiver) {
        str = str.replace("{bounty}", (NumberFormatting.getCurrencyPrefix() + NumberFormatting.formatNumber(bounty) + NumberFormatting.getCurrencySuffix()));
        str = str.replace("{bounty_plain}", NumberFormatting.formatNumber(bounty));
        return parse(str, amount, receiver);
    }

    public static String parse(String str, double amount, double bounty, long time, LocalTime.TimeFormat format, OfflinePlayer receiver) {
        if (str.contains("{time}")) {
            String timeString = formatTime(time, format, receiver.getPlayer());
            str = str.replace("{time}", (timeString));
        }
        return parse(str, amount, bounty, receiver);
    }

    public static String parse(String str, OfflinePlayer player, double amount, OfflinePlayer receiver) {
        PlayerData playerData = DataManager.getPlayerData(player.getUniqueId());
        Whitelist whitelist = playerData.getWhitelist();
        str = str.replace("{whitelist}", (whitelist.toString()));
        str = parsePlayerName(str, player);
        return parse(str, amount, receiver);
    }

    private static String parsePlayerName(String str, OfflinePlayer player) {
        if (player != null) {
            String replacement;
            if (player.getName() != null) {
                replacement = player.getName();
            } else {
                replacement = LoggedPlayers.getPlayerName(player.getUniqueId());
            }
            replacement = getMessage("player-prefix") + replacement + getMessage("player-suffix");
            if (ConfigOptions.getIntegrations().isPapiEnabled())
                replacement = new PlaceholderAPIClass().parse(player, replacement);
            str = str.replace("{player}", replacement);
        }
        return str;
    }

    public static String parse(String str, OfflinePlayer player, OfflinePlayer receiver) {
        str = parsePlayerName(str, player);
        return parse(str, receiver);
    }

    public static String parse(String str, OfflinePlayer player, double amount, double totalBounty, OfflinePlayer receiver) {
        str = str.replace("{bounty}", (NumberFormatting.getCurrencyPrefix() + NumberFormatting.formatNumber(totalBounty) + NumberFormatting.getCurrencySuffix()));
        str = str.replace("{bounty_plain}", NumberFormatting.formatNumber(totalBounty));
        return parse(str, player, amount, receiver);
    }

    public static String color(String str){
        if (str == null) return null;
        return IridiumColorAPI.process(str);
    }


    public static String getPrefix() {
        return prefix;
    }

    public static TextComponent getTextComponent(String message) {
        TextComponent textComponent;
        try {
            textComponent = (TextComponent) TextComponent.fromLegacy(message);
        } catch (Exception | NoSuchMethodError e) {
            // not using a version that supports fromLegacy
            textComponent = new TextComponent(message);
        }
        return textComponent;
    }

    public static String parseImageURL(String url, UUID uuid, boolean skinLoaded) {
        if (url.contains("{any}")) {
            String identifier;
            if (skinLoaded) {
                identifier = SkinManager.getSkin(uuid).id();
            } else if (uuid.version() == 4) {
                identifier = uuid.toString();
            } else {
                identifier = LoggedPlayers.getPlayerName(uuid);
            }
            url = url.replace("{any}", identifier);
        }
        if (url.contains("{name}")) {
            url = url.replace("{name}", LoggedPlayers.getPlayerName(uuid));
        }
        return url.replace("{uuid}", uuid.toString());
    }
}
