package io.rhythmknights.coretags.component.hook;

import io.rhythmknights.coretags.CoreTags;
import net.luckperms.api.LuckPerms;

public final class LuckPermsHook {
   private final CoreTags plugin;
   private final LuckPerms api;

   public LuckPermsHook(CoreTags plugin, LuckPerms api) {
      this.plugin = plugin;
      this.api = api;
      if (api == null) {
         plugin.getLogger().warning("LuckPerms not found – permission sync limited to YAML only.");
      }

   }

   public LuckPerms api() {
      return this.api;
   }

   public boolean isPresent() {
      return this.api != null;
   }
}
