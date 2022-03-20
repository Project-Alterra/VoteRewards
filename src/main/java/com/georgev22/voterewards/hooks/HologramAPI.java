package com.georgev22.voterewards.hooks;

import com.georgev22.api.maps.HashObjectMap;
import com.georgev22.api.maps.ObjectMap;
import com.georgev22.api.minecraft.MinecraftUtils;
import com.georgev22.api.minecraft.configmanager.CFG;
import com.georgev22.api.utilities.Utils;
import com.georgev22.voterewards.VoteRewardPlugin;
import com.georgev22.voterewards.utilities.MessagesUtil;
import com.georgev22.voterewards.utilities.OptionsUtil;
import com.georgev22.voterewards.utilities.configmanager.FileManager;
import com.georgev22.voterewards.utilities.player.VotePartyUtils;
import com.georgev22.voterewards.utilities.player.VoteUtils;
import com.github.unldenis.hologram.Hologram;
import com.github.unldenis.hologram.HologramPool;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;

/**
 * @author GeorgeV22
 */
public class HologramAPI {

    private final static FileManager fileManager = FileManager.getInstance();
    private final static CFG dataCFG = fileManager.getData();
    private final static FileConfiguration data = dataCFG.getFileConfiguration();
    private final static VoteRewardPlugin m = VoteRewardPlugin.getInstance();
    private static final ObjectMap<String, Hologram> hologramMap = new HashObjectMap<>();
    private static final HologramPool hologramPool = new HologramPool(m, 70);

    /**
     * Create a hologram
     *
     * @param name     Hologram name.
     * @param location Hologram location.
     * @param type     Hologram type.
     * @param save     Save the hologram in the file.
     * @return {@link Hologram} instance.
     */
    @NotNull
    public static Hologram create(String name, Location location, String type, boolean save) {
        Hologram hologram = getHologramMap().get(name) != null ? getHologramMap().get(name) : null;
        if (hologram == null) {
            Hologram.Builder builder = Hologram.builder().location(location);

            for (String line : fileManager.getConfig().getFileConfiguration().getStringList("Holograms." + type)) {
                builder.addLine(MinecraftUtils.colorize(line));
            }
            hologram = builder.build(hologramPool);
        }

        if (save) {
            data.set("Holograms." + name + ".location", location);
            data.set("Holograms." + name + ".type", type);
            dataCFG.saveFile();
        }
        getHologramMap().append(name, hologram);
        return hologram;
    }

    /**
     * Remove a hologram.
     *
     * @param name Hologram name.
     * @param save Save the changes in file.
     */
    public static void remove(String name, boolean save) {
        Hologram hologram = getHologramMap().remove(name);

        hologramPool.remove(hologram);

        if (save) {
            data.set("Holograms." + name, null);
            dataCFG.saveFile();
        }
    }

    /**
     * Show a hologram to a specific player.
     *
     * @param name   Hologram name.
     * @param player Player to show the hologram.
     */
    public static void show(String name, Player player) {
        Hologram hologram = getHologramMap().get(name);

        if (hologram == null) {
            MinecraftUtils.msg(player, "Hologram " + name + " doesn't exist");
            return;
        }
        hologram.show(player);
    }

    /**
     * Hide a hologram from a specific player.
     *
     * @param name   Hologram name.
     * @param player Player to hide the hologram.
     */
    public static void hide(String name, Player player) {
        Hologram hologram = getHologramMap().get(name);

        if (hologram == null) {
            MinecraftUtils.msg(player, "Hologram " + name + " doesn't exist");
            return;
        }

        hologram.hide(player);
    }

    /**
     * Show a hologram from a specific player.
     *
     * @param hologram Hologram instance.
     * @param player   Player to hide the hologram.
     */
    public static void show(@NotNull Hologram hologram, Player player) {
        hologram.show(player);
    }

    /**
     * Hide a hologram from a specific player.
     *
     * @param hologram Hologram instance.
     * @param player   Player to hide the hologram.
     */
    public static void hide(@NotNull Hologram hologram, Player player) {
        hologram.hide(player);
    }

    /**
     * Return all holograms in a collection.
     *
     * @return all holograms in a collection.
     */
    @NotNull
    public static Collection<Hologram> getHolograms() {
        return getHologramMap().values();
    }

    /**
     * Return a {@link Hologram} from hologram name.
     *
     * @param name Hologram name
     * @return a {@link Hologram} from hologram name.
     */
    public static Hologram getHologram(String name) {
        return getHologramMap().get(name);
    }

    /**
     * Check if a hologram exists
     *
     * @param name Hologram name.
     * @return if the hologram exists
     */
    public static boolean hologramExists(String name) {
        return getHologramMap().get(name) != null;
    }

    /**
     * Update the lines in a specific hologram
     *
     * @param hologram     {@link Hologram} instance to change the lines.
     * @param lines        The new lines.
     * @param placeholders The placeholders.
     * @return the updated {@link Hologram} instance.
     */
    @Contract("_, _, _ -> param1")
    public static @NotNull Hologram updateHologram(@NotNull Hologram hologram, @NotNull String @NotNull [] lines, ObjectMap<String, String> placeholders) {
        int i = 0;
        for (final String key : lines) {
            for (String placeholder : placeholders.keySet()) {
                if (key.contains(placeholder)) {
                    hologram.setLine(i, Utils.placeHolder(MinecraftUtils.colorize(key), placeholders, true));
                    break;
                }
            }
            ++i;
        }
        return hologram;
    }

    /**
     * Update all {@link Hologram} instances.
     */
    public static void updateAll() {
        if (data.get("Holograms") == null)
            return;
        for (String hologramName : data.getConfigurationSection("Holograms").getKeys(false)) {
            hologramPool.remove(getHologram(hologramName));
            Hologram.Builder builder = new Hologram.Builder().location(getHologram(hologramName).getLocation());
            int i = 0;
            for (String line : m.getConfig().getStringList("Holograms." + data.getString("Holograms." + hologramName + ".type"))) {
                builder.addLine(MinecraftUtils.colorize(Utils.placeHolder(line, getPlaceholderMap(), true)));
            }
            getHologramMap().append(hologramName, builder.build(hologramPool));
            getPlaceholderMap().clear();
        }
    }

    /**
     * @return A map with all the holograms.
     */
    public static ObjectMap<String, Hologram> getHologramMap() {
        return hologramMap;
    }

    /**
     * A map with all hologram placeholders
     *
     * @return a map with all hologram placeholders
     */
    public static ObjectMap<String, String> getPlaceholderMap() {
        final ObjectMap<String, String> map = new HashObjectMap<>();
        int top = 1;
        for (Map.Entry<String, Integer> b : VoteUtils.getTopPlayers(OptionsUtil.VOTETOP_VOTERS.getIntValue()).entrySet()) {
            String[] args = String.valueOf(b).split("=");
            map.append("%top-" + top + "%", args[0]).append("%vote-" + top + "%", args[1]);
            top++;
        }
        int allTimeTop = 1;
        for (Map.Entry<String, Integer> b : VoteUtils.getAllTimeTopPlayers(OptionsUtil.VOTETOP_ALL_TIME_VOTERS.getIntValue()).entrySet()) {
            String[] args = String.valueOf(b).split("=");
            map.append("%alltimetop-" + allTimeTop + "%", args[0]).append("%alltimevote-" + allTimeTop + "%", args[1]);
            allTimeTop++;
        }
        return map.append("%bar%", MinecraftUtils.getProgressBar(
                        data.getInt("VoteParty-Votes"),
                        OptionsUtil.VOTEPARTY_VOTES.getIntValue(),
                        OptionsUtil.VOTEPARTY_BARS.getIntValue(),
                        OptionsUtil.VOTEPARTY_BAR_SYMBOL.getStringValue(),
                        OptionsUtil.VOTEPARTY_COMPLETE_COLOR.getStringValue(),
                        OptionsUtil.VOTEPARTY_NOT_COMPLETE_COLOR.getStringValue()))
                .append("%voteparty_votes_until%", String.valueOf(OptionsUtil.VOTEPARTY_VOTES.getIntValue()
                        - fileManager.getData().getFileConfiguration().getInt("VoteParty-Votes", 0)))
                .append("%voteparty_votes_need%", OptionsUtil.VOTEPARTY_VOTES.getStringValue())
                .append("%voteparty_total_votes%", String.valueOf(fileManager.getData().getFileConfiguration().getInt("VoteParty-Votes")))
                .appendIfTrue("%voteparty_votes_full%",
                        MinecraftUtils.colorize(Utils.placeHolder(
                                MessagesUtil.VOTEPARTY_WAITING_FOR_MORE_PLAYERS_PLACEHOLDER.getMessages()[0],
                                new HashObjectMap<String, String>()
                                        .append("%online%", String.valueOf(Bukkit.getOnlinePlayers().size()))
                                        .append("%need%", String.valueOf(OptionsUtil.VOTEPARTY_PLAYERS_NEED.getIntValue())),
                                true)),
                        MinecraftUtils.colorize(Utils.placeHolder(
                                MessagesUtil.VOTEPARTY_PLAYERS_FULL_PLACEHOLDER.getMessages()[0],
                                new HashObjectMap<String, String>()
                                        .append("%until%", String.valueOf(OptionsUtil.VOTEPARTY_VOTES.getIntValue()
                                                - fileManager.getData().getFileConfiguration().getInt("VoteParty-Votes", 0)))
                                        .append("%total%", String.valueOf(fileManager.getData().getFileConfiguration().getInt("VoteParty-Votes")))
                                        .append("%need%", String.valueOf(OptionsUtil.VOTEPARTY_VOTES.getIntValue())),
                                true)),
                        OptionsUtil.VOTEPARTY_PLAYERS.getBooleanValue() & VotePartyUtils.isWaitingForPlayers());
    }

    //IGNORE
    private static boolean a = false;


    public static void setHook(boolean b) {
        a = b;
    }

    public static boolean isHooked() {
        return a;
    }
    //

}
