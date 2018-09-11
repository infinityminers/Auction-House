package com.shadebyte.auctionhouse.cmds.subcmds;

import com.shadebyte.auctionhouse.Core;
import com.shadebyte.auctionhouse.api.AuctionAPI;
import com.shadebyte.auctionhouse.api.discordwebhook.DiscordEmbed;
import com.shadebyte.auctionhouse.api.discordwebhook.DiscordHook;
import com.shadebyte.auctionhouse.api.discordwebhook.DiscordMessage;
import com.shadebyte.auctionhouse.api.discordwebhook.embed.FieldEmbed;
import com.shadebyte.auctionhouse.api.enums.Lang;
import com.shadebyte.auctionhouse.api.enums.Permissions;
import com.shadebyte.auctionhouse.api.event.AuctionStartEvent;
import com.shadebyte.auctionhouse.auction.AuctionItem;
import com.shadebyte.auctionhouse.auction.AuctionPlayer;
import com.shadebyte.auctionhouse.cmds.SubCommand;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;

/**
 * The current file has been created by Kiran Hart
 * Date Created: 7/6/2018
 * Time Created: 11:50 AM
 * Usage of any code found within this class is prohibited unless given explicit permission otherwise.
 */
public class SellCommand extends SubCommand {

    @Override
    public void onCommand(CommandSender sender, String[] args) {

        if (!sender.hasPermission(Permissions.SELL_CMD.getNode())) {
            sender.sendMessage(Core.getInstance().getSettings().getPrefix() + Core.getInstance().getLocale().getMessage(Lang.NO_PERMISSION.getNode()));
            return;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(Core.getInstance().getSettings().getPrefix() + Core.getInstance().getLocale().getMessage(Lang.PLAYERS_ONLY.getNode()));
            return;
        }

        Player p = (Player) sender;

        if (args.length == 1) {
            p.sendMessage(Core.getInstance().getSettings().getPrefix() + Core.getInstance().getLocale().getMessage(Lang.CMD_SELL.getNode()));
            return;
        }

        if (args.length == 2) {
            if (AuctionAPI.getInstance().isNumeric(args[1])) {
                if (Core.getInstance().getConfig().getBoolean("settings.use-bid-system")) {
                    p.sendMessage(Core.getInstance().getSettings().getPrefix() + Core.getInstance().getLocale().getMessage(Lang.CMD_SELL.getNode()));
                } else {

                    int buyNow = Integer.parseInt(args[1]);

                    //Max Prices
                    if (buyNow > Core.getInstance().getConfig().getInt("settings.max-auction-price")) {
                        p.sendMessage(Core.getInstance().getSettings().getPrefix() + Core.getInstance().getLocale().getMessage(Lang.MAX_AUCTION_PRICE.getNode()));
                        return;
                    }

                    if (buyNow < Core.getInstance().getConfig().getInt("settings.min-auction-price")) {
                        p.sendMessage(Core.getInstance().getSettings().getPrefix() + Core.getInstance().getLocale().getMessage(Lang.MIN_AUCTION_PRICE.getNode()));
                        return;
                    }

                    AuctionItem auctionItem = new AuctionItem(p.getUniqueId().toString(), AuctionAPI.getItemInHand(p), Core.getInstance().getConfig().getInt("settings.default-auction-time"), buyNow, 0, buyNow);
                    AuctionStartEvent auctionStartEvent = new AuctionStartEvent(auctionItem);
                    Core.getInstance().getServer().getPluginManager().callEvent(auctionStartEvent);

                    if (!auctionStartEvent.isCancelled()) {
                        Core.getInstance().auctionItems.add(0, auctionItem);
                        p.sendMessage(Core.getInstance().getSettings().getPrefix() + Core.getInstance().getLocale().getMessage(Lang.AUCTION_LISTED.getNode()).replace("{itemname}", auctionItem.getDisplayName()).replace("{price}", AuctionAPI.getInstance().friendlyNumber(buyNow)));

                        //Discord Hook
                        if (Core.getInstance().getConfig().getBoolean("discord.enabled")) {
                            DiscordHook discordHook = new DiscordHook(Core.getInstance().getConfig().getString("discord.webhook"));
                            DiscordEmbed de = DiscordEmbed.builder()
                                    .title(Core.getInstance().getConfig().getString("discord.title"))
                                    .description(Core.getInstance().getConfig().getString("discord.description"))
                                    .color(1)
                                    .fields(Arrays.asList(
                                            FieldEmbed.builder().name("Seller").value(p.getName()).inline(true).build(),
                                            FieldEmbed.builder().name("Buy Now").value(AuctionAPI.getInstance().friendlyNumber(buyNow)).build(),
                                            FieldEmbed.builder().name("Item").value(auctionItem.getItem().getType().name() + ":" + auctionItem.getItem().getDurability()).build()
                                    ))
                                    .build();

                            DiscordMessage dm = DiscordMessage.builder().username(Core.getInstance().getConfig().getString("discord.username")).content("").avatarUrl(Core.getInstance().getConfig().getString("discord.profilepicture")).embeds(Arrays.asList(de)).build();
                            discordHook.send(dm);
                        }

                        AuctionAPI.setItemInHand(p, null);
                    }
                }
            } else {
                p.sendMessage(Core.getInstance().getSettings().getPrefix() + Core.getInstance().getLocale().getMessage(Lang.NOT_A_NUMBER.getNode()));
            }
        }

        if (args.length == 3) {
            if (Core.getInstance().getConfig().getBoolean("settings.use-bid-system")) {
                if (AuctionAPI.getInstance().isNumeric(args[1]) && AuctionAPI.getInstance().isNumeric(args[2])) {
                    p.sendMessage(Core.getInstance().getSettings().getPrefix() + Core.getInstance().getLocale().getMessage(Lang.CMD_SELL.getNode()));
                } else {
                    p.sendMessage(Core.getInstance().getSettings().getPrefix() + Core.getInstance().getLocale().getMessage(Lang.NOT_A_NUMBER.getNode()));
                }
            }
        }

        if (args.length == 4) {
            if (Core.getInstance().getConfig().getBoolean("settings.use-bid-system")) {
                if (AuctionAPI.getInstance().isNumeric(args[1]) && AuctionAPI.getInstance().isNumeric(args[2]) && AuctionAPI.getInstance().isNumeric(args[3])) {
                    if (new AuctionPlayer(p).getLimit() - 1 < new AuctionPlayer(p).getTotalActiveAuctions()) {
                        p.sendMessage(Core.getInstance().getSettings().getPrefix() + Core.getInstance().getLocale().getMessage(Lang.AUCTION_MAX.getNode()));
                        return;
                    }

                    int buyNow = Integer.parseInt(args[1]);
                    int startPrice = Integer.parseInt(args[2]);
                    int increment = Integer.parseInt(args[3]);

                    if (AuctionAPI.getItemInHand(p) == null || AuctionAPI.getItemInHand(p).getType() == Material.AIR) {
                        p.sendMessage(Core.getInstance().getSettings().getPrefix() + Core.getInstance().getLocale().getMessage(Lang.AIR.getNode()));
                        return;
                    }

                    //Max Prices
                    if (buyNow > Core.getInstance().getConfig().getInt("settings.max-auction-price")) {
                        p.sendMessage(Core.getInstance().getSettings().getPrefix() + Core.getInstance().getLocale().getMessage(Lang.MAX_AUCTION_PRICE.getNode()));
                        return;
                    }

                    if (startPrice > Core.getInstance().getConfig().getInt("settings.max-auction-start")) {
                        p.sendMessage(Core.getInstance().getSettings().getPrefix() + Core.getInstance().getLocale().getMessage(Lang.MAX_START_PRICE.getNode()));
                        return;
                    }

                    if (increment > Core.getInstance().getConfig().getInt("settings.max-auction-increment")) {
                        p.sendMessage(Core.getInstance().getSettings().getPrefix() + Core.getInstance().getLocale().getMessage(Lang.MAX_INCREMENT_PRICE.getNode()));
                        return;
                    }

                    //Min Prices
                    if (buyNow < Core.getInstance().getConfig().getInt("settings.min-auction-price")) {
                        p.sendMessage(Core.getInstance().getSettings().getPrefix() + Core.getInstance().getLocale().getMessage(Lang.MIN_AUCTION_PRICE.getNode()));
                        return;
                    }

                    if (startPrice < Core.getInstance().getConfig().getInt("settings.min-auction-start")) {
                        p.sendMessage(Core.getInstance().getSettings().getPrefix() + Core.getInstance().getLocale().getMessage(Lang.MIN_START_PRICE.getNode()));
                        return;
                    }

                    if (increment < Core.getInstance().getConfig().getInt("settings.min-auction-increment")) {
                        p.sendMessage(Core.getInstance().getSettings().getPrefix() + Core.getInstance().getLocale().getMessage(Lang.MIN_INCREMENT_PRICE.getNode()));
                        return;
                    }

                    AuctionItem auctionItem = new AuctionItem(p.getUniqueId().toString(), AuctionAPI.getItemInHand(p), Core.getInstance().getConfig().getInt("settings.default-auction-time"), startPrice, increment, buyNow);
                    AuctionStartEvent auctionStartEvent = new AuctionStartEvent(auctionItem);
                    Core.getInstance().getServer().getPluginManager().callEvent(auctionStartEvent);

                    if (!auctionStartEvent.isCancelled()) {
                        Core.getInstance().auctionItems.add(0, auctionItem);
                        p.sendMessage(Core.getInstance().getSettings().getPrefix() + Core.getInstance().getLocale().getMessage(Lang.AUCTION_LISTED.getNode()).replace("{itemname}", auctionItem.getDisplayName()).replace("{price}", AuctionAPI.getInstance().friendlyNumber(startPrice)));

                        //Discord Hook
                        if (Core.getInstance().getConfig().getBoolean("discord.enabled")) {
                            DiscordHook discordHook = new DiscordHook(Core.getInstance().getConfig().getString("discord.webhook"));
                            DiscordEmbed de = DiscordEmbed.builder()
                                    .title(Core.getInstance().getConfig().getString("discord.title"))
                                    .description(Core.getInstance().getConfig().getString("discord.description"))
                                    .color(1)
                                    .fields(Arrays.asList(
                                            FieldEmbed.builder().name("Seller").value(p.getName()).inline(true).build(),
                                            FieldEmbed.builder().name("Start Price").value(AuctionAPI.getInstance().friendlyNumber(startPrice)).build(),
                                            FieldEmbed.builder().name("Increment").value(AuctionAPI.getInstance().friendlyNumber(increment)).build(),
                                            FieldEmbed.builder().name("Buy Now").value(AuctionAPI.getInstance().friendlyNumber(buyNow)).build(),
                                            FieldEmbed.builder().name("Item").value(auctionItem.getItem().getType().name() + ":" + auctionItem.getItem().getDurability()).build()
                                    ))
                                    .build();

                            DiscordMessage dm = DiscordMessage.builder().username(Core.getInstance().getConfig().getString("discord.username")).content("").avatarUrl(Core.getInstance().getConfig().getString("discord.profilepicture")).embeds(Arrays.asList(de)).build();
                            discordHook.send(dm);
                        }

                        AuctionAPI.setItemInHand(p, null);
                    }
                } else {
                    p.sendMessage(Core.getInstance().getSettings().getPrefix() + Core.getInstance().getLocale().getMessage(Lang.NOT_A_NUMBER.getNode()));
                }
            }
        }
    }

    @Override
    public String name() {
        return Core.getInstance().getCommandManager().sell;
    }

    @Override
    public String info() {
        return null;
    }

    @Override
    public String[] aliases() {
        return new String[0];
    }
}
