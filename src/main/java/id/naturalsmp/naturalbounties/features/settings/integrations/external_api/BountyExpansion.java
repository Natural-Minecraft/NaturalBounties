package me.naturalsmp.NaturalBounties.features.settings.integrations.external_api;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.naturalsmp.NaturalBounties.data.Bounty;
import me.naturalsmp.NaturalBounties.Leaderboard;
import me.naturalsmp.NaturalBounties.NaturalBounties;
import me.naturalsmp.NaturalBounties.data.Setter;
import me.naturalsmp.NaturalBounties.ui.gui.GUI;
import me.naturalsmp.NaturalBounties.utils.DataManager;
import me.naturalsmp.NaturalBounties.utils.LoggedPlayers;
import me.naturalsmp.NaturalBounties.features.challenges.ChallengeManager;
import me.naturalsmp.NaturalBounties.features.settings.display.WantedTags;
import me.naturalsmp.NaturalBounties.features.settings.auto_bounties.TimedBounties;
import me.naturalsmp.NaturalBounties.utils.BountyManager;
import me.naturalsmp.NaturalBounties.features.LanguageOptions;
import me.naturalsmp.NaturalBounties.features.settings.money.NumberFormatting;
import me.naturalsmp.NaturalBounties.data.Whitelist;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class BountyExpansion extends PlaceholderExpansion {

    @Override
    public @NotNull String getAuthor() {
        return "NaturalDev";
    }

    @Override
    public @NotNull String getIdentifier() {
        return "NaturalBounties";
    }

    @Override
    public @NotNull String getVersion() {
        return NaturalBounties.getInstance().getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true; // This is required or else PlaceholderAPI will unregister the Expansion on reload
    }

    /**
     * Add "_formatted" to the end to add the currency prefix and suffix
     * Add "_full" to the end of leaderboard to add what the stat is about
     * Add "_value" to the end of leaderboard to get the raw value
     * Add "_name" to the end of top placeholder to get the name of the player in that position
     * <p>%NaturalBounties_bounty%</p>
     * <p>%NaturalBounties_total%</p>
     * <p>%NaturalBounties_(all/kills/claimed/deaths/set/immunity/current)%</p>
     * <p>%NaturalBounties_top_[x]_(all/kills/claimed/deaths/set/immunity/current)%</p>
     * <p>%NaturalBounties_wanted%</p> Wanted tag
     * <p>%NaturalBounties_notification%</p> Bounty broadcast -> EXTENDED, SHORT, or DISABLED
     * <p>%NaturalBounties_mode%</p> Whitelist/Blacklist
     * <p>%NaturalBounties_timed_bounty%</p>
     * <p>%NaturalBounties_challenge_[x/time]%</p>
     * <p>%NaturalBounties_current_page%</p>
     * <p>%NaturalBounties_total_pages%</p>
     * <p>%NaturalBounties_sort(_gui)%</p>
     * @Depricated <p>%NaturalBounties_bounties_claimed%</p>
     * <p>%NaturalBounties_bounties_set%</p>
     * <p>%NaturalBounties_bounties_received%</p>
     * <p>%NaturalBounties_immunity_spent%</p>
     * <p>%NaturalBounties_all_time_bounty%</p>
     * <p>%NaturalBounties_currency_gained%</p>
     */

    @Override
    public String onRequest(OfflinePlayer player, String params){
        UUID uuid = player.getUniqueId();
        if (params.equalsIgnoreCase("current_page")) {
            if (GUI.playerInfo.containsKey(player.getUniqueId())) {
                return GUI.playerInfo.get(player.getUniqueId()).page() + "";
            }
            return "";
        }
        if (params.equalsIgnoreCase("total_pages")) {
            if (GUI.playerInfo.containsKey(player.getUniqueId())) {
                return GUI.playerInfo.get(player.getUniqueId()).maxPage() + "";
            }
            return "";
        }
        if (params.equalsIgnoreCase("timed_bounty")) {
            if (BountyManager.hasBounty(uuid) && TimedBounties.isMaxed(Objects.requireNonNull(BountyManager.getBounty(uuid)).getTotalDisplayBounty()))
                // maxed out, cant get any higher
                return "";
            long next = TimedBounties.getUntilNextBounty(player.getUniqueId());
            if (next == -1)
                return "";
            return LocalTime.formatTime(next, LocalTime.TimeFormat.RELATIVE);
        }
        if (params.equalsIgnoreCase("wanted")) {
            Bounty bounty = BountyManager.getBounty(uuid);
            if (bounty == null)
                return "";
            return WantedTags.getWantedDisplayText(player);
        }
        if (params.startsWith("bounty")){
            Bounty bounty = BountyManager.getBounty(uuid);
            if (bounty != null){
                if (params.endsWith("_rank")) {
                    return DataManager.getLocalData().getBountyRank(bounty.getTotalDisplayBounty()) + "";
                }
                if (params.endsWith("_formatted"))
                    return LanguageOptions.color(NumberFormatting.getCurrencyPrefix() + NumberFormatting.formatNumber(bounty.getTotalDisplayBounty()) + NumberFormatting.getCurrencySuffix());
                return NumberFormatting.getValue(bounty.getTotalDisplayBounty());
            }
            return "0";
        }
        if (params.startsWith("sort")){
            if (params.equalsIgnoreCase("sort")) {
                return LanguageOptions.parse(GUI.getActiveSortType(player.getUniqueId()), player);
            }
            String guiName = params.substring(5);
            return LanguageOptions.parse(GUI.parseSortType(guiName, DataManager.getPlayerData(player.getUniqueId()).getGUISortType(guiName)), player);
        }
        if (params.startsWith("challenge")) {
            if (params.length() < 11)
                return ChallengeManager.getTimeLeft();
            params = params.substring(10);
            if (params.equalsIgnoreCase("time"))
                return ChallengeManager.getTimeLeft();
            try {
                int index = (int) NumberFormatting.tryParse(params);
                return ChallengeManager.getChallengeTitle(player, index);
            } catch (NumberFormatException e) {
                return "Placeholder Error";
            }
        }
        if (params.startsWith("total")) {
             if (params.startsWith("total_unique")) {
                 // NaturalBounties_total_unique
                List<Bounty> bounties = BountyManager.getAllBounties(-1);
                Set<UUID> counted = new HashSet<>();
                for (Bounty bounty : bounties) {
                    for (Setter setter : bounty.getSetters()) {
                        counted.add(setter.getUuid());
                    }
                }
                return NumberFormatting.formatNumber(counted.size());
            } else {
                 // NaturalBounties_total
                int bounties = BountyManager.getAllBounties(-1).size();
                return NumberFormatting.formatNumber(bounties);
            }
        }

        if (params.equalsIgnoreCase("bounties_claimed")){
            return String.valueOf(Leaderboard.KILLS.getStat(uuid));
        }

        if (params.equalsIgnoreCase("bounties_set")){
            return String.valueOf(Leaderboard.SET.getStat(uuid));
        }

        if (params.equalsIgnoreCase("bounties_received")){
            return String.valueOf(Leaderboard.DEATHS.getStat(uuid));
        }

        if (params.equalsIgnoreCase("immunity_spent")){
            return String.valueOf(Leaderboard.IMMUNITY.getStat(uuid));
        }

        if (params.equalsIgnoreCase("all_time_bounty")){
            return String.valueOf(Leaderboard.ALL.getStat(uuid));
        }

        if (params.equalsIgnoreCase("currency_gained")){
            return String.valueOf(Leaderboard.CLAIMED.getStat(uuid));
        }

        if (params.equalsIgnoreCase("notification")) {
            return DataManager.getPlayerData(player.getUniqueId()).getBroadcastSettings() + "";
        }

        if (params.equalsIgnoreCase("mode")) {
            Whitelist whitelist = DataManager.getPlayerData(player.getUniqueId()).getWhitelist();
            return whitelist.isBlacklist() ? "Blacklist" : "Whitelist";
        }

        int ending = 0;
        if (params.endsWith("_full")) {
            ending = 1;
            params = params.substring(0,params.lastIndexOf("_"));
        }
        if (params.endsWith("_formatted")) {
            ending = 2;
            params = params.substring(0,params.lastIndexOf("_"));
        }
        if (params.endsWith("_value")) {
            ending = 3;
            params = params.substring(0,params.lastIndexOf("_"));
        }
        if (params.endsWith("_name")) {
            ending = 4;
            params = params.substring(0,params.lastIndexOf("_"));
        }
        if (params.endsWith("_rank")) {
            ending = 5;
            params = params.substring(0,params.lastIndexOf("_"));
        }
        if (params.startsWith("top_")) {
            params = params.substring(4);
            int rank;
            try {
                if (params.contains("_"))
                    rank = Integer.parseInt(params.substring(0,params.indexOf("_")));
                else
                    rank = Integer.parseInt(params);
            } catch (NumberFormatException ignored) {
                rank = 0;
            }
            if (rank < 1)
                rank = 1;
            Leaderboard leaderboard;
            if (!params.contains("_")) {
                leaderboard = Leaderboard.CURRENT;
            } else {
                params = params.substring(params.indexOf("_") + 1);
                try {
                    leaderboard = Leaderboard.valueOf(params.toUpperCase());
                } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
                    return null;
                }
            }
            Map<UUID, Double> stat = leaderboard.getTop(rank - 1, 1);
            if (stat.isEmpty())
                return "...";
            boolean useCurrency = leaderboard == Leaderboard.IMMUNITY || leaderboard == Leaderboard.CLAIMED || leaderboard == Leaderboard.ALL || leaderboard == Leaderboard.CURRENT;
            Map.Entry<UUID, Double> entry = stat.entrySet().iterator().next();
            double amount = entry.getValue();
            UUID uuid1 = entry.getKey();
            String name = LoggedPlayers.getPlayerName(uuid1);
            OfflinePlayer p = Bukkit.getOfflinePlayer(uuid1);
            if (ending == 1)
                return LanguageOptions.parse(leaderboard.getStatMsg(true).replace("{amount}", (leaderboard.getFormattedStat(uuid1))), p);
            if (ending == 2)
                return LanguageOptions.parse(leaderboard.getFormattedStat(uuid1),p);
            if (ending == 3)
                return NumberFormatting.getValue(leaderboard.getStat(uuid1));
            if (ending == 4) {
                return name;
            }
            return Leaderboard.parseBountyTopString(rank, name, amount, useCurrency, p);
        }

        String value = params.contains("_") ? params.substring(0, params.indexOf("_")) : params;

        try {
            Leaderboard leaderboard = Leaderboard.valueOf(value.toUpperCase());
            if (ending == 1)
                return LanguageOptions.parse(leaderboard.getStatMsg(true).replace("{amount}", (leaderboard.getFormattedStat(player.getUniqueId()))), player);
            if (ending == 2)
                return LanguageOptions.parse(leaderboard.getFormattedStat(player.getUniqueId()),player);
            if (ending == 3)
                return NumberFormatting.getValue(leaderboard.getStat(player.getUniqueId()));
            if (ending == 5) {
                return NumberFormatting.formatNumber(leaderboard.getRank(player.getUniqueId()));
            }
            return NumberFormatting.formatNumber(leaderboard.getStat(player.getUniqueId()));
        } catch (IllegalArgumentException ignored){
            // not a valid leaderboard
        }

        return null;
    }
}
