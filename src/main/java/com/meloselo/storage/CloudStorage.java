package com.meloselo.storage;

import java.io.InputStream;
import java.net.URL;
import java.util.Map;

public interface CloudStorage {

    /**
     * parse fileUri to region, bucket and filename
     * 
     * @param fileUri the uri of file
     * @return Map<String, String> with keys region, bucket and filename mapped to values in uri 
     */
    Map<String, String> parseFileUri(String fileUri);

    /**
     * store file in the cloud
     * 
     * @param bucket the cloud bucket/store name
     * @param is the file InputStream
     * @param filename the name to store the file as
     * @param contentType the contentType for file
     * @param contentLength the contentLength for file
     * @param metaData additional meta data to attach to file
     * @return file uri after storage e.g. for S3 s3://{region}/{bucket}/{filename}
     * @throws CloudException on Cloud Storage Service/Client errors
     */
    String storeFile(String bucket, InputStream is, String filename, String contentType,
            long contentLength, Map<String, String> metaData) throws CloudException;
    
    /**
     * fetch file using file uri. Care must be taken to consume and close the input stream from 
     * return object as soon as possible. e.g. the S3 client will keep http resources open
     * until the input stream is consumed. 
     * 
     * @param fileUri the uri of file
     * @return the CloudFile object with file input stream and file metadata 
     * @throws CloudException on Cloud Storage Service/Client errors
     */
    CloudFile getFile(String fileUri) throws CloudException;

    /**
     * delete file using file uri
     * 
     * @param fileUri the uri of the file to delete
     * @throws CloudException on Cloud Storage Service/Client errors
     */
    void deleteFile(String fileUri) throws CloudException;

    /**
     * create url for a file which will expire in specified seconds.
     * 
     * @param fileUri the uri of file
     * @param expirySeconds the number of seconds after which this url will expire
     * @return the url
     */
    URL getExpiringUrl(String fileUri, long expirySeconds)
            throws CloudException;
}