package me.zodd.postmanpat;

import com.earth2me.essentials.User;
import github.scarsz.discordsrv.dependencies.jda.api.events.interaction.SlashCommandEvent;
import github.scarsz.discordsrv.objects.managers.AccountLinkManager;

import java.util.UUID;

public abstract class EssxData {
    PostmanPat plugin;

    public EssxData(PostmanPat plugin) {
        this.plugin = plugin;
    }

    /**
     * @param id A discord user ID
     * @return An Essentials User
     */
    protected User getEssxUser(String id) {
        return plugin.getEss().getUser(mgr().getUuid(id));
    }

    /**
     * @param uuid A players user ID
     * @return An Essentials User
     */
    protected User getEssxUser(UUID uuid) {
        return plugin.getEss().getUser(uuid);
    }

    /**
     * @param event SlashCommandEvent to get user from
     * @return An Essentials User
     */
    protected User getEssxUser(SlashCommandEvent event) {
        return getEssxUser(event.getUser().getId());
    }

    protected AccountLinkManager mgr() {
        return plugin.getSrv().getAccountLinkManager();
    }
}
