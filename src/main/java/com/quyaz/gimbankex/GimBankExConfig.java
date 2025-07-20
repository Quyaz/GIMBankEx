package com.quyaz.gimbankex;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("GimBankEx")
public interface GimBankExConfig extends Config {
    @ConfigItem(
            keyName = "api_base",
            name = "API URL",
            description = "The server url",
            position = 1
    )
    default String api() {
        return "https://localhost";
    }

    @ConfigItem(
            keyName = "shared",
            name = "Shared endpoint",
            description = "Endpoint for shared get/post",
            position = 2
    )
    default String shared() {
        return "bank";
    }

    @ConfigItem(
            keyName = "messages",
            name = "Messages endpoint",
            description = "Endpoint for messages get/post",
            position = 3
    )
    default String messages() {
        return "messages";
    }

    @ConfigItem(
            keyName = "groupName",
            name = "Group name",
            description = "Group name",
            position = 4
    )
    default String groupName() {
        return "";
    }

    @ConfigItem(
            keyName = "token",
            name = "Token",
            description = "Token",
            position = 5
    )
    default String token() {
        return "";
    }

    @ConfigItem(
            keyName = "auto_open",
            name = "Open when opening bank",
            description = "Automatically open panel when opening shared",
            position = 6
    )
    default AutoOpen autoOpen() {
        return AutoOpen.NONE;
    }

    @ConfigItem(
            keyName = "datetime_format",
            name = "Datetime format",
            description = "Date time format",
            position = 7
    )
    default DateTimeFormat dateTimeFormat() {
        return DateTimeFormat.TIME_DAY_MONTH;
    }
    // "HH:mm d/M/y")

    @Getter
    @RequiredArgsConstructor
    enum AutoOpen {
        NONE("None"),
        BANK("Bank"),
        MESSAGES("Messages"),
        ;

        @Getter
        private final String group;

        @Override
        public String toString() {
            return group;
        }
    }

    @Getter
    @RequiredArgsConstructor
    enum DateTimeFormat {
        TIME_DAY_MONTH_YEAR("HH:mm dd/MM/y"),
        TIME_MONTH_DAY_YEAR("HH:mm MM/dd/y"),
        TIME_DAY_MONTH("HH:mm dd/MM"),
        TIME_MONTH_DAY("HH:mm MM/dd"),
        ;

        @Getter
        private final String group;

        @Override
        public String toString() {
            return group;
        }
    }
}
