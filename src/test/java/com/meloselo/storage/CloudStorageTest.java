/**
 * Copyright(c) 2014 MeloSelo, Inc. All rights reserved
 */
package com.meloselo.storage;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.UUID;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.DefaultConfigurationBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.BeforeClass;
import org.junit.Test;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;

/**
 * @author roshan
 *
 */
public class CloudStorageTest {
    
    private static Log log = LogFactory.getLog(CloudStorageTest.class);
    
    private static CloudStorage storage;
    private static String bucketName;
    
    @BeforeClass
    public static void setup() {
        Configuration config = null;
        try {
            DefaultConfigurationBuilder configBuilder = new DefaultConfigurationBuilder("config.xml");
            config = configBuilder.getConfiguration();
        } catch (ConfigurationException e) {
            log.error("Error reading config file", e);
            fail("error reading config file");
        }
        storage = new S3CloudStorage(
                new BasicAWSCredentials(
                        config.getString("s3.accesskey"),
                        config.getString("s3.secretkey")),
                new BasicAWSCredentials(
                        config.getString("s3.urlgen.accesskey"),
                        config.getString("s3.urlgen.secretkey")),
                Regions.fromName(config.getString("s3.bucket.region")));
        bucketName = config.getString("s3.bucket");
        assertNotNull("bucketName is null", bucketName);
    }

    @Test
    public void testCRD() {
        String testStr = "hello world";
        String contentType = "text/plain";
        String filename = "qa." + UUID.randomUUID().toString();
        
        InputStream is = new ByteArrayInputStream(testStr.getBytes());
        
        String fileUri = null;
        try {
            fileUri = storage.storeFile(bucketName, is, filename, contentType, testStr.length(), null);
        } catch (CloudException e) {
            log.debug("error on storing temp file", e);
            fail("error on storing temp file");
        }
        
        assertNotNull("fileUri is null after storage", fileUri);
        
        try {
            CloudFile file = storage.getFile(fileUri);
            assertNotNull("file fetched is null", file);
            assertEquals("file contentType not equal", contentType, file.getContentType());
            assertEquals("file contentLength not equal", testStr.length(), file.getContentLength());

            char[] buffer = new char[1024];
            StringBuffer strBuffer = new StringBuffer();
            InputStreamReader reader = new InputStreamReader(file.getInputStream());
            try {
                while(true) {
                    int read = reader.read(buffer, 0, 1024);
                    if (read < 0) {
                        break;
                    }
                    strBuffer.append(buffer, 0, read);
                }
                assertEquals("file content not equal", testStr, strBuffer.toString());
            } catch (IOException ex) {
                log.debug("error on reading file", ex);
                fail("error on reading file");
            } finally {
                try {
                    reader.close();
                } catch (IOException ex) {
                    try {
                        file.getInputStream().close();
                    } catch (IOException ex1) {
                        log.debug("error on closing file input stream", ex1);
                        fail("error on closing file input stream");
                    }
                }
            }
        } catch (CloudException e) {
            log.debug("error on fetching temp file", e);
            fail("error on fetching temp file");
        }
        
        try {
            URL url = storage.getExpiringUrl(fileUri, 600l);
            assertNotNull("expiring url is null", url);
        } catch (CloudException e) {
            log.debug("error on generating expiring url", e);
            fail("error on generating expiring url");
        }
        
        try {
            storage.deleteFile(fileUri);
        } catch (CloudException e) {
            log.debug("error on deleting temp file", e);
            fail("error on deleting temp file");
        }  
    }
}
