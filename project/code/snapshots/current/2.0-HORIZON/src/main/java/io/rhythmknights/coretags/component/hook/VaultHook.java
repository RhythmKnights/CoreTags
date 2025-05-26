package io.rhythmknights.coretags.component.hook;

import io.rhythmknights.coretags.CoreTags;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.OfflinePlayer;

public final class VaultHook {
   private final CoreTags plugin;
   private final Economy economy;
   private final boolean costSystem;

   public VaultHook(CoreTags plugin, Economy economyHook) {
      this.plugin = plugin;
      this.economy = economyHook;
      this.costSystem = plugin.configs().costSystemEnabled();
      if (this.costSystem && this.economy == null) {
         plugin.getLogger().warning("Vault not detected – tag costs will be ignored.");
      }

   }

   public boolean active() {
      return this.costSystem && this.economy != null;
   }

   public boolean canAfford(OfflinePlayer player, double amount) {
      return !this.active() || this.economy.has(player, amount);
   }

   public boolean withdraw(OfflinePlayer player, double amount) {
      return !this.active() || this.economy.withdrawPlayer(player, amount).transactionSuccess();
   }
}
