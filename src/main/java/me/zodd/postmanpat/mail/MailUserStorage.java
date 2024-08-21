package me.zodd.postmanpat.mail;

import redempt.redlib.config.annotations.Comment;
import redempt.redlib.config.annotations.ConfigMappable;

import java.util.*;

@ConfigMappable
public class MailUserStorage {

    MailUserStorage() {
    }

    @Comment("A map of players ignore-lists, stored by UUID")
    public static Map<UUID, List<UUID>> mailIgnoreList = new HashMap<>(HashMap.newHashMap(3));
}
