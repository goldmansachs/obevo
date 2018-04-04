/**
 * Copyright 2017 Goldman Sachs.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.gs.obevo.amazon;

import com.amazonaws.services.rds.model.DescribeDBInstancesRequest;
import com.amazonaws.services.redshift.AmazonRedshift;
import com.amazonaws.services.redshift.AmazonRedshiftClientBuilder;
import com.amazonaws.services.redshift.model.Cluster;
import com.amazonaws.services.redshift.model.CreateClusterRequest;
import com.amazonaws.services.redshift.model.DeleteClusterRequest;
import com.amazonaws.services.redshift.model.DescribeClustersRequest;
import com.amazonaws.services.redshift.model.DescribeClustersResult;
import com.amazonaws.services.redshift.model.RestoreFromClusterSnapshotRequest;

public class RedshiftInstanceUtil {
    private static final AmazonRedshift client = AmazonRedshiftClientBuilder.defaultClient();
    private static final String INSTANCE_ID = "dbdeploy-redshift";
    private static final String SNAPSHOT_IDENTIFIER = "dbdeploy-redshift-snapshot";

    public static void main(String[] args) throws Exception {
        RedshiftInstanceUtil instance = new RedshiftInstanceUtil();
//        instance.create(INSTANCE_ID);
//        instance.describe(INSTANCE_ID);
        instance.delete(INSTANCE_ID);
//        instance.restore(INSTANCE_ID);
    }

    private void create(String dbInstanceIdentifier) throws Exception {
        // http://docs.aws.amazon.com/AmazonRDS/latest/APIReference/API_CreateDBInstance.html
        CreateClusterRequest request = new CreateClusterRequest()
                .withClusterIdentifier(INSTANCE_ID)
                .withNodeType("dc2.large")
                .withDBName("dbdeploy")
                .withPubliclyAccessible(true)
                .withAvailabilityZone("us-east-1")
                .withClusterType("single-node")
                .withMasterUsername("deploybuilddbo")
                .withMasterUserPassword("Deploybuilddb0");
        Cluster response = client.createCluster(request);
        System.out.println(response);

        describe(dbInstanceIdentifier);
    }

    private void restore(String dbInstanceIdentifier) throws Exception {
        Cluster response = client.restoreFromClusterSnapshot(new RestoreFromClusterSnapshotRequest()
                .withSnapshotIdentifier(SNAPSHOT_IDENTIFIER)
                .withClusterIdentifier(INSTANCE_ID)
        );
        System.out.println(response);

        describe(dbInstanceIdentifier);
    }

    private void describe(String dbInstanceIdentifier) throws Exception {
        while (true) {
            DescribeDBInstancesRequest request = new DescribeDBInstancesRequest()
                    .withDBInstanceIdentifier(dbInstanceIdentifier);

            DescribeClustersResult response = client.describeClusters(new DescribeClustersRequest()
                    .withClusterIdentifier(INSTANCE_ID)
            );
            Cluster dbInstance = response.getClusters().get(0);
            if (!dbInstance.getClusterStatus().equalsIgnoreCase("creating")) {
                System.out.println("Done! " + response);
                System.out.println(dbInstance.getEndpoint().getAddress());
                System.out.println(dbInstance.getEndpoint().getPort());
                break;
            }

            System.out.println("Not done - will wait 10s: " + response);
            Thread.sleep(10000L);
        }
    }

    private void delete(String dbInstanceIdentifier) {
        Cluster dbInstance = client.deleteCluster(new DeleteClusterRequest()
                .withClusterIdentifier(INSTANCE_ID)
//                .withFinalClusterSnapshotIdentifier(SNAPSHOT_IDENTIFIER)
                .withSkipFinalClusterSnapshot(true)
        );
        System.out.println(dbInstance);
    }
}
