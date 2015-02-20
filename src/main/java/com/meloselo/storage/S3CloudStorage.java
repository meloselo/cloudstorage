/**
 * Copyright(c) 2014 MeloSelo, Inc. All rights reserved
 */
package com.meloselo.storage;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.InputStream;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.joda.time.Duration;
import org.joda.time.Instant;

import com.amazonaws.AmazonClientException;
import com.amazonaws.HttpMethod;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.google.common.base.Strings;

/**
 * @author roshan
 *
 */
public class S3CloudStorage implements CloudStorage {

    private AmazonS3 s3;
    private AmazonS3 urlGenS3;
    private Regions region;
    private Pattern fileUriPattern;
    
    /**
     * class to store files on AWS S3
     *  
     * @param credentials the AWSCredentials object for create, fetch, delete object, required
     * @param urlGenCredentials the AWSCredentials object to generate urls for object, required
     * @param region the AWS region to use, required
     */
    public S3CloudStorage(AWSCredentials credentials, AWSCredentials urlGenCredentials, 
            Regions region) {
        checkArgument(credentials != null, "aws credentials is null");
        checkArgument(region != null, "aws region is null");
        
        s3 = new AmazonS3Client(credentials);
        s3.setRegion(Region.getRegion(region));
        
        if (urlGenCredentials == null) {
            urlGenCredentials = credentials;
        }
        
        urlGenS3 = new AmazonS3Client(credentials);
        urlGenS3.setRegion(Region.getRegion(region));
        
        this.region = region;
        this.fileUriPattern = Pattern.compile("s3://([^/]+)/([^/]+)/([^/]+)");
    }

    /**
     * parse fileUri to region, bucket and filename
     * 
     * @param fileUri the uri of file in s3://{region}/{bucket}/{filename} format
     * @return Map<String, String> with keys region, bucket and filename mapped to values in uri 
     */
    @Override
    public Map<String, String> parseFileUri(String fileUri) {
        checkArgument(! Strings.isNullOrEmpty(fileUri), "file uri is empty");
        
        Matcher matcher = fileUriPattern.matcher(fileUri);
        checkArgument(matcher.matches(), "fileUri %s not in s3://{region}/{bucket}/{filename} format", fileUri);
        
        Map<String, String> fileInfo = new HashMap<String, String>();
        fileInfo.put("region", matcher.group(1));
        fileInfo.put("bucket", matcher.group(2));
        fileInfo.put("filename", matcher.group(3));
        
        return fileInfo;
    }
    
    /**
     * store file in AWS S3 bucket
     * 
     * @param bucket the S3 bucket name
     * @param is the file InputStream
     * @param filename the name to store the file as
     * @param contentType the contentType for file
     * @param contentLength the contentLength for file
     * @param metaData additional meta data to attach to file
     * @return file uri after storage in format s3://{region}/{bucket}/{filename}
     * @throws CloudException on AWS Service/Client errors
     */
    public String storeFile(String bucket, InputStream is, String filename, String contentType,
            long contentLength, Map<String, String> metaData)
            throws CloudException {
        try {
            checkArgument(! Strings.isNullOrEmpty(bucket), "bucket is null");
            checkArgument(is != null, "is, inputstream to store is null");
            checkArgument(! Strings.isNullOrEmpty(filename), "filename is null");
            
            ObjectMetadata objMeta = new ObjectMetadata();
            if (contentType != null) {
                objMeta.setContentType(contentType);
            }
            if (contentLength > 0) {
                objMeta.setContentLength(contentLength);
            }
            if (metaData != null) {
                objMeta.setUserMetadata(metaData);
            }
            
            s3.putObject(bucket, filename, is, objMeta);
            
            return String.format("s3://%s/%s/%s", region.name(), bucket, filename);
        } catch (AmazonClientException ex) {
            throw new CloudException(String.format("Error storing file %s type %s in bucket %s region %s",
                    filename, contentType, bucket, region), ex);
        }
    }
    
    /**
     * fetch file using file uri. Care must be taken to consume and close the input stream from 
     * return object as soon as possible. the S3 client will keep http resources open
     * until the input stream is consumed. 
     * 
     * @param fileUri the uri of file in s3://{region}/{bucket}/{filename} format
     * @return the CloudFile object with file input stream and file metadata 
     * @throws CloudException on AWS Service/Client errors
     */
    @Override
    public CloudFile getFile(String fileUri) throws CloudException {
        Map<String, String> fileInfo = parseFileUri(fileUri);
        
        String fileregion = fileInfo.get("region");
        String filebucket = fileInfo.get("bucket");
        String filename = fileInfo.get("filename");
        
        checkArgument(region.name().equalsIgnoreCase(fileregion), "fileUri %s not same as this storage region %s",
                fileUri, region);
        
        return getFile(filebucket, filename);
    }
    
    /**
     * fetch file using bucket and filename. Care must be taken to consume and close the input stream from 
     * return object as soon as possible. the S3 client will keep http resources open
     * until the input stream is consumed.
     * 
     * @param bucket the bucket name
     * @param filename the file name
     * @return the CloudFile object with file input stream and file metadata 
     * @throws CloudException on AWS Service/Client errors
     */
    public CloudFile getFile(String bucket, String filename) throws CloudException {
        checkArgument(! Strings.isNullOrEmpty(bucket), "bucket is null or empty");
        checkArgument(! Strings.isNullOrEmpty(filename), "filename is null or empty");
        
        try {
            S3Object object = s3.getObject(bucket, filename);
            if (object == null) {
                throw new CloudException(String.format("null object found for bucket %s filename %s in region %s",
                        bucket, filename, region));
            }
            
            return new CloudFile()
                .setInputStream(object.getObjectContent())
                .setContentType(object.getObjectMetadata().getContentType())
                .setContentLength(object.getObjectMetadata().getContentLength())
                .setMetaData(object.getObjectMetadata().getUserMetadata());
        } catch (AmazonClientException ex) {
            throw new CloudException(String.format("Error getting file for bucket %s filename %s in region %s",
                    bucket, filename, region), ex);
        }
    }
    
    /**
     * delete file using file uri in s3://{region}/{bucket}/{filename} format
     * 
     * @param fileUri the uri of the file to delete
     * @throws CloudException on AWS Service/Client errors
     */
    @Override
    public void deleteFile(String fileUri) throws CloudException {
        Map<String, String> fileInfo = parseFileUri(fileUri);
        
        String fileregion = fileInfo.get("region");
        String filebucket = fileInfo.get("bucket");
        String filename = fileInfo.get("filename");
        
        checkArgument(region.name().equalsIgnoreCase(fileregion), "fileUri %s not same as this storage region %s",
                fileUri, region);
        
        deleteFile(filebucket, filename);
    }
    
    /**
     * delete file using bucket and filename 
     * 
     * @param bucket the bucket name
     * @param filename the file name to delete
     * @throws CloudException on AWS Service/Client errors
     */
    public void deleteFile(String bucket, String filename) throws CloudException {
        checkArgument(! Strings.isNullOrEmpty(bucket), "bucket is null or empty");
        checkArgument(! Strings.isNullOrEmpty(filename), "filename is null or empty");
        
        try {
            s3.deleteObject(bucket, filename);
        } catch (AmazonClientException ex) {
            throw new CloudException(String.format("Error getting file for bucket %s filename %s in region %s",
                    bucket, filename, region), ex);
        }
    }
    
    /**
     * create url for a file which will expire in specified seconds.
     * 
     * @param fileUri the uri of file in s3://{region}/{bucket}/{filename} format
     * @param expirySeconds the number of seconds after which this url will expire
     * @return the url
     * @throws CloudException on AWS Service/Client errors
     */
    @Override
    public URL getExpiringUrl(String fileUri, long expirySeconds) throws CloudException {
        Map<String, String> fileInfo = parseFileUri(fileUri);
        
        String fileregion = fileInfo.get("region");
        String filebucket = fileInfo.get("bucket");
        String filename = fileInfo.get("filename");
        
        checkArgument(region.name().equalsIgnoreCase(fileregion), "fileUri %s not same as this storage region %s",
                fileUri, region);
        
        return getExpiringUrl(filebucket, filename, expirySeconds);
    }
    
    /**
     * create url for a file which will expire in specified seconds.
     * 
     * @param bucket the bucket name
     * @param filename the filename
     * @param expirySeconds the number of seconds after which this url will expire
     * @return the url
     * @throws CloudException on AWS Service/Client errors
     */
    public URL getExpiringUrl(String bucket, String filename, long expirySeconds) throws CloudException {
        checkArgument(! Strings.isNullOrEmpty(bucket), "bucket is null or empty");
        checkArgument(! Strings.isNullOrEmpty(filename), "filename is null or empty");
        checkArgument(expirySeconds > 0, "expirySeconds %d is not postive for bucket %s filename %s", 
                expirySeconds, bucket, filename);
        
        Date expiry = Instant.now().plus(Duration.standardSeconds(expirySeconds)).toDate();
        
        try {
            return urlGenS3.generatePresignedUrl(bucket, filename, expiry, HttpMethod.GET);
        } catch (AmazonClientException ex) {
            throw new CloudException(
                    String.format("Error fetching expiring url for bucket %s filename %s in region %s",
                            bucket, filename, region), ex);
        }
    }
    
}
