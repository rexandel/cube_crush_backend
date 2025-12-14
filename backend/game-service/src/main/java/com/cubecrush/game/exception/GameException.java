package com.cubecrush.game.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class GameException extends RuntimeException {
    private final String localizationKey;
    private final HttpStatus status;

    public GameException(String localizationKey, HttpStatus status) {
        super(localizationKey);
        this.localizationKey = localizationKey;
        this.status = status;
    }

    public GameException(String localizationKey, HttpStatus status, String message) {
        super(message);
        this.localizationKey = localizationKey;
        this.status = status;
    }
}
