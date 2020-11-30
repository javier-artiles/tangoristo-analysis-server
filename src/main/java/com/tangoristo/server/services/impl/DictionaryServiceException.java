package com.tangoristo.server.services.impl;

public class DictionaryServiceException extends Exception {

    DictionaryServiceException(String message) {
        super(message);
    }

    DictionaryServiceException(String message, Throwable cause) {
        super(message, cause);
    }

}
