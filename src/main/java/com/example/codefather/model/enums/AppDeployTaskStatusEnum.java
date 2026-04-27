package com.example.codefather.model.enums;

import lombok.Getter;

/**
 * 应用部署任务状态枚举。
 */
@Getter
public enum AppDeployTaskStatusEnum {

    PENDING("PENDING"),
    DEPLOYING("DEPLOYING"),
    SCREENSHOTTING("SCREENSHOTTING"),
    SUCCESS("SUCCESS"),
    FAILED("FAILED");

    private final String value;

    AppDeployTaskStatusEnum(String value) {
        this.value = value;
    }

    public static AppDeployTaskStatusEnum getEnumByValue(String value) {
        for (AppDeployTaskStatusEnum statusEnum : values()) {
            if (statusEnum.value.equals(value)) {
                return statusEnum;
            }
        }
        return null;
    }
}
