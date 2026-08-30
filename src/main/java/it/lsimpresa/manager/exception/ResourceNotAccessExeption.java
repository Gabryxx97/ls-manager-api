package it.lsimpresa.manager.exception;

import java.io.Serial;

public class ResourceNotAccessExeption extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public ResourceNotAccessExeption(String msg) {
        super(msg);
    }
}
