package com.shadebyte.auctionhouse.inventory.inventories;

import com.shadebyte.auctionhouse.Core;
import com.shadebyte.auctionhouse.api.AuctionAPI;
import com.shadebyte.auctionhouse.api.enums.Lang;
import com.shadebyte.auctionhouse.api.event.TransactionCompleteEvent;
import com.shadebyte.auctionhouse.auction.AuctionItem;
import com.shadebyte.auctionhouse.auction.Transaction;
import com.shadebyte.auctionhouse.inventory.AGUI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * The current file has been created by Kiran Hart
 * Date Created: 7/6/2018
 * Time Created: 11:56 AM
 * Usage of any code found within this class is prohibited unless given explicit permission otherwise.
 */
public class ConfirmationGUI implements AGUI {

    private AuctionItem auctionItem;

    public ConfirmationGUI(AuctionItem auctionItem) {
        this.auctionItem = auctionItem;
    }

    @Override
    public void click(InventoryClickEvent e, ItemStack clicked, int slot) {
        e.setCancelled(true);
        Player p = (Player) e.getWhoClicked();
        if (clicked.isSimilar(AuctionAPI.getInstance().fill("&a&lYes", 5))) {
            if (Core.getInstance().auctionItems.contains(auctionItem)) {
                if (Core.getEconomy().getBalance(p) < auctionItem.getBuyNowPrice()) {
                    p.sendMessage(Core.getInstance().getSettings().getPrefix() + Core.getInstance().getLocale().getMessage(Lang.NOT_ENOUGH_MONEY.getNode()));
                } else {
                    Core.getEconomy().withdrawPlayer(p, auctionItem.getBuyNowPrice());
                    Core.getEconomy().depositPlayer(Bukkit.getOfflinePlayer(UUID.fromString(auctionItem.getOwner())), auctionItem.getBuyNowPrice());

                    if (AuctionAPI.getInstance().availableSlots(p.getInventory()) == 0) {
                        Core.getInstance().getData().getConfig().set("expired." + p.getUniqueId().toString() + "." + auctionItem.getKey() + ".item", auctionItem.getItem());
                        Core.getInstance().getData().getConfig().set("expired." + p.getUniqueId().toString() + "." + auctionItem.getKey() + ".display", AuctionAPI.getInstance().expiredAuctionItem(auctionItem));
                    } else
                        p.getInventory().addItem(auctionItem.getItem());

                    p.sendMessage(Core.getInstance().getSettings().getPrefix() + Core.getInstance().getLocale().getMessage(Lang.AUCTION_BUY.getNode()).replace("{itemname}", auctionItem.getDisplayName()).replace("{price}", AuctionAPI.getInstance().friendlyNumber(auctionItem.getBuyNowPrice())));
                    Player owner = Bukkit.getPlayer(UUID.fromString(auctionItem.getOwner()));

                    if (owner != null) {
                        owner.sendMessage(Core.getInstance().getSettings().getPrefix() + Core.getInstance().getLocale().getMessage(Lang.AUCTION_SOLD.getNode()).replace("{player}", p.getName()).replace("{item}", auctionItem.getDisplayName()).replace("{price}", AuctionAPI.getInstance().friendlyNumber(auctionItem.getBuyNowPrice())));
                    }

                    long time = System.currentTimeMillis();
                    Transaction transaction = new Transaction(Transaction.TransactionType.BOUGHT, auctionItem, p.getUniqueId().toString(), time);
                    transaction.saveTransaction();
                    auctionItem.setTime(0);
                    Core.getInstance().auctionItems.remove(auctionItem);
                    p.closeInventory();
                    TransactionCompleteEvent completeEvent = new TransactionCompleteEvent(transaction);
                    Core.getInstance().getServer().getPluginManager().callEvent(completeEvent);
                    p.openInventory(new AuctionGUI(p).getInventory());
                }
            } else {
                p.closeInventory();
                p.openInventory(new AuctionGUI(p).getInventory());
            }
        } else if (clicked.isSimilar(AuctionAPI.getInstance().fill("&c&lNo", 14))) {
            p.closeInventory();
            p.openInventory(new AuctionGUI(p).getInventory());
        }
    }

    @Override
    public void close(InventoryCloseEvent e) {
        // Bukkit.getServer().getScheduler().runTaskLater(Core.getInstance(), () -> e.getPlayer().openInventory(e.getInventory()), 1);
    }

    @Override
    public Inventory getInventory() {
        Inventory inventory = Bukkit.createInventory(this, 9, ChatColor.translateAlternateColorCodes('&', Core.getInstance().getConfig().getString("gui.confirm.title")));
        for (int i = 0; i <= 3; i++) {
            inventory.setItem(i, AuctionAPI.getInstance().fill("&a&lYes", 5));
        }

        for (int i = 5; i <= 8; i++) {
            inventory.setItem(i, AuctionAPI.getInstance().fill("&c&lNo", 14));
        }

        inventory.setItem(4, auctionItem.getItem());
        return inventory;
    }
}
