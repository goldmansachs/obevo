# Building Obevo

Requirements:

### Java 7 or higher
* [Oracle JDK Download](http://www.oracle.com/technetwork/java/javase/downloads/index.html)
* [OpenJDK Download](http://openjdk.java.net/install/)

Note that it is possible to develop with JDK8, but
* the code must conform to Java 7 language syntax
* libraries must be Java 7 compatible.


### Maven 3.2.5 or higher

[Download](https://maven.apache.org/download.cgi)


### Amazon RDS (for integration testing)

###### Background:

For integration testing against live databases, we rely on Amazon RDS to host our database instances.

Where possible (i.e. if it is free), we have an instance always running and available for test (e.g. Postgres, SQL Server).
These servers are currently available for any developer to use, but we will soon dedicate these servers for the continuous
build and make it easier for developers to setup their own environments.

If it is not free (e.g. Oracle), we can use the Amazon RDS API to create an instance for testing and to deactivate it when not in use.

The module obevo-internal-test-util has code for you to use to run this.


###### Setup instructions:

Setup the account in AWS:
* Go to the AWS Page: https://aws.amazon.com/
* My Account -> Security Credentials
* Go to Users, then Add User
* Create a user with "Programmatic Access" (not AWS Management Console access)
* Create user with RDS Full Permissions
* Ensure that the user has the "AmazonRDSFullAccess" policy
* Note the access key and secret access key that you

Then setup your credentials and config files using the access keys above - see the links below:
* [Credential Setup](https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/setup-credentials.html)


### IDE

The main developers use IntelliJ. Hence, the IDE files for IntelliJ (*.iml, .idea) have been checked in - this particularly
helps with enforcing the code formatting standards.

If you use another IDE and would like to commit the IDE files, please raise an issue and discuss with the Obevo team.
