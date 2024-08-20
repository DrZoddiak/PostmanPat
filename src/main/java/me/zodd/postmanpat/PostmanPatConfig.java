package me.zodd.postmanpat;

import redempt.redlib.config.annotations.*;

@ConfigMappable
public class PostmanPatConfig {

    PostmanPatConfig() {
    }

    @Comment("The discord Channel ID to ping users if DMs are disabled")
    public static Long NotifyChannel = 0L;

    @Comment("root command for mail")
    public static String rootCommand = "mail";

    @Comment("Subcommand for reading mail")
    public static String readSubCommand = "read";

    @Comment("Subcommand for sending mail to another user")
    public static String sendSubCommand = "send";

    @Comment("Subcommand for marking all unread mail as read")
    public static String markReadSubCommand = "mark-read";

    @Comment("Argument for including mail that is already read")
    public static String includeReadArg = "include-read";

    @Comments({
            @Comment("Maximum characters a message can be. Discord sets a limit at 2000."),
            @Comment("Default value is set to 1900 to allow for some additional meta-data"),
            @Comment("It is suggested you do not raise this value above 1900, lest ye risk errors")
    })
    public static int maxMessageSize = 1900;
}