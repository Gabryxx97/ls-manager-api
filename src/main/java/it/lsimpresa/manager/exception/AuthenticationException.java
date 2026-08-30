package it.lsimpresa.manager.exception;

import java.io.Serial;


public class AuthenticationException  extends RuntimeException{

    @Serial
    private static final long serialVersionUID = 3856732903236606804L;

    public AuthenticationException(String msg) {
        super(msg);
    }
}
