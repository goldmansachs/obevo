/**
 * Copyright 2017 Goldman Sachs.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.gs.obevo.amazon;

import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.AmazonRDSClientBuilder;
import com.amazonaws.services.rds.model.CreateDBInstanceRequest;
import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.DeleteDBInstanceRequest;
import com.amazonaws.services.rds.model.DescribeDBInstancesRequest;
import com.amazonaws.services.rds.model.DescribeDBInstancesResult;

public class CreateDbInstance {
    private static final AmazonRDS client = AmazonRDSClientBuilder.defaultClient();
    public static final String dbInstanceIdentifier = "dbdeploy-oracle-12-1";

    public static void main(String[] args) throws Exception {
/*
        create();
        describe();
*/

        delete();
    }

    public static void describe() throws Exception {
        while (true) {
            DescribeDBInstancesRequest request = new DescribeDBInstancesRequest()
                    .withDBInstanceIdentifier(dbInstanceIdentifier);

            DescribeDBInstancesResult response = client.describeDBInstances(request);
            DBInstance dbInstance = response.getDBInstances().get(0);
            if (!dbInstance.getDBInstanceStatus().equalsIgnoreCase("creating")) {
                System.out.println("Done! " + response);
                System.out.println(dbInstance.getEndpoint().getAddress());
                System.out.println(dbInstance.getEndpoint().getPort());
                break;
            }

            System.out.println("Not done - will wait 10s: " + response);
            Thread.sleep(10000L);
        }
    }

    public static void create() throws Exception {
        // http://docs.aws.amazon.com/AmazonRDS/latest/APIReference/API_CreateDBInstance.html
        CreateDBInstanceRequest request = new CreateDBInstanceRequest()
                .withEngine("oracle-se2")
                //.withEngineVersion("12.1.0.2.v8")
                .withLicenseModel("license-included")
                .withAllocatedStorage(10)
                .withStorageType("gp2")  // SSD
                .withBackupRetentionPeriod(0)
                .withDBInstanceClass("db.t2.micro")
                .withDBInstanceIdentifier(dbInstanceIdentifier)
                .withDBName("DBDEPLOY")
                .withMasterUsername("deploybuilddbo")
                .withMasterUserPassword("deploybuilddb0")
                ;
        DBInstance response = client.createDBInstance(request);
        System.out.println(response);

        describe();
    }

    public static void delete() {
        DBInstance dbInstance = client.deleteDBInstance(
                new DeleteDBInstanceRequest(dbInstanceIdentifier).withSkipFinalSnapshot(true)
        );
        System.out.println(dbInstance);
    }
}
