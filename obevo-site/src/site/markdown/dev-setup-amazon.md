<!--

    Copyright 2017 Goldman Sachs.
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.

-->

(WORK IN PROGRESS)

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
