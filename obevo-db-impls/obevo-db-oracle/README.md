# Oracle Setup steps

## Part 1 - JDBC Driver Setup
1) Download the JDBC driver from Oracle

[Download Link](http://www.oracle.com/technetwork/database/features/jdbc/index-091264.html)

Use the "JDBC Thin" driver (i.e. ojdbc7.jar)


2) Install it into your local Maven repository.

(Substitute the version and file parameters as needed)

```
mvn install:install-file -DgroupId=com.oracle -DartifactId=ojdbc7 -Dversion=12.1.0.2 -Dfile=C:\Downloads\ojdbc7.jar -Dpackaging=jar -DgeneratePom=true

```


3) In obevo-db-oracle/pom.xml, update the Maven coordinates of the ojdbc7 driver if needed.


4) When developing, use the Maven profile "-P amazon-personal-build"


## Part 2 - Oracle DB Instance Setup
1) Use the com.gs.obevo.amazon.CreateDbInstance Java class inside the obevo-internal-test-util module to create an Oracle
instance for you in Amazon RDS and wait for the connection URL to appear.

Warning - unless you already have an Oracle license, it is not free to use Oracle on Amazon RDS; there is an hourly rate.

The default configuration of CreateDbInstance will create the smallest possible Oracle instance so that the cost is minimized.
When you are not actively using the DB, you can destroy the instance using the same CreateDbInstance utility.


2) Using the connection URL from the previous step, update the amazon-personal-oracle-creds.properties file as needed.
