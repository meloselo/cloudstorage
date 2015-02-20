Cloud Storage Client
====================

Contains abstraction CloudStorage to create/fetch/delete files in cloud storage services like AWS S3. AWS S3 implementation S3CloudStorage is also provided, along with abstraction CloudFile representing a file in cloud storage.

Compile
-------

 * `mvn compile`

Test
----

 * `cp src/test/config/cloudstorage.properties src/test/resources/cloudstorage.properties` 
 * Replace ENTER_AWS_ACCESSKEY, ENTER_AWS_SECRETKEY, ENTER_BUCKETNAME with appropriate values
 * `mvn test`
 
Package (Create JAR)
--------------------

 * Configure for Test as indicated above
 * `mvn package`
