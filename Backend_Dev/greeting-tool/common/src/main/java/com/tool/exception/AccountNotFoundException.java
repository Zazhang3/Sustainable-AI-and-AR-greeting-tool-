package com.tool.exception;

/**
 * Account not found
 */
public class AccountNotFoundException extends BaseException {

    public AccountNotFoundException(String msg) {
        super(msg);
    }

}