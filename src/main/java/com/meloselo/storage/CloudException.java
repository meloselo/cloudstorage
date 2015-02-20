/**
 * Copyright(c) 2014 MeloSelo, Inc. All rights reserved
 */
package com.meloselo.storage;

/**
 * @author roshan
 *
 */
public class CloudException extends Exception {

    private static final long serialVersionUID = 1L;

    public CloudException() {
        //do nothing
    }
    
    public CloudException(String message) {
        super(message);
    }
    
    public CloudException(String message, Throwable t) {
        super(message, t);
    }
    
    public CloudException(Throwable t) {
        super(t);
    }
}
