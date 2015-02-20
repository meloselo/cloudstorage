/**
 * Copyright(c) 2014 MeloSelo, Inc. All rights reserved
 */
package com.meloselo.storage;

import java.io.InputStream;
import java.util.Map;

/**
 * @author roshan
 *
 */
public class CloudFile {
    
    public static final String USERID_KEY = "userid";

    private InputStream inputStream;
    private long contentLength;
    private String contentType;
    private Map<String, String> metaData;
    
    public CloudFile() {
        //do nothing
    }

    /**
     * @return the inputStream
     */
    public InputStream getInputStream() {
        return inputStream;
    }

    /**
     * @param inputStream the inputStream to set
     * @return this object
     */
    public CloudFile setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
        return this;
    }

    /**
     * @return the contentLength
     */
    public long getContentLength() {
        return contentLength;
    }

    /**
     * @param contentLength the contentLength to set
     * @return this object
     */
    public CloudFile setContentLength(long contentLength) {
        this.contentLength = contentLength;
        return this;
    }

    /**
     * @return the contentType
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * @param contentType the contentType to set
     * @return this object
     */
    public CloudFile setContentType(String contentType) {
        this.contentType = contentType;
        return this;
    }

    /**
     * @return the metaData
     */
    public Map<String, String> getMetaData() {
        return metaData;
    }

    /**
     * @param metaData the metaData to set
     * @return this object
     */
    public CloudFile setMetaData(Map<String, String> metaData) {
        this.metaData = metaData;
        return this;
    }
}
