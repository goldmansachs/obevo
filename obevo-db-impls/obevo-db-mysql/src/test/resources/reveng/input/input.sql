
USE dbdeploy01;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE TABLE_A (
  A_ID int(11) NOT NULL,
  B_ID int(11) NOT NULL,
  STRING_FIELD varchar(30) DEFAULT NULL,
  TIMESTAMP_FIELD timestamp(6) NULL DEFAULT NULL,
  C_ID int(11) DEFAULT NULL,
  EXTRA1 int(11) DEFAULT NULL,
  EXTRA2 int(11) DEFAULT NULL,
  EXTRA3 int(11) DEFAULT NULL,
  EXTRA4 int(11) DEFAULT NULL,
  PRIMARY KEY (A_ID),
  KEY B_ID (B_ID),
  CONSTRAINT TABLE_A_ibfk_1 FOREIGN KEY (B_ID) REFERENCES TABLE_B (B_ID)
);
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE TABLE_B (
  B_ID int(11) NOT NULL,
  PRIMARY KEY (B_ID)
);
/*!40101 SET character_set_client = @saved_cs_client */;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW "VIEW1" AS SELECT 
 1 AS A_ID,
 1 AS B_ID,
 1 AS STRING_FIELD,
 1 AS TIMESTAMP_FIELD,
 1 AS C_ID,
 1 AS EXTRA1,
 1 AS EXTRA2,
 1 AS EXTRA3,
 1 AS EXTRA4*/;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW "VIEW2" AS SELECT 
 1 AS A_ID,
 1 AS B_ID,
 1 AS STRING_FIELD,
 1 AS TIMESTAMP_FIELD,
 1 AS C_ID,
 1 AS EXTRA1,
 1 AS EXTRA2,
 1 AS EXTRA3,
 1 AS EXTRA4*/;
SET character_set_client = @saved_cs_client;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE artifactdeployment (
  ARTFTYPE varchar(31) NOT NULL,
  artifactpath varchar(255) NOT NULL,
  OBJECTNAME varchar(255) NOT NULL,
  "ACTIVE" int(11) DEFAULT NULL,
  changetype varchar(255) DEFAULT NULL,
  CONTENTHASH varchar(255) DEFAULT NULL,
  DBSCHEMA varchar(255) DEFAULT NULL,
  deploy_user_id varchar(32) DEFAULT NULL,
  time_inserted datetime DEFAULT NULL,
  time_updated datetime DEFAULT NULL,
  rollbackcontent varchar(2048) DEFAULT NULL,
  insertdeployid bigint(20) DEFAULT NULL,
  updatedeployid bigint(20) DEFAULT NULL,
  PRIMARY KEY (artifactpath,OBJECTNAME)
);
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE artifactexecution (
  id bigint(20) NOT NULL,
  "status" char(1) NOT NULL,
  deploytime datetime NOT NULL,
  executorid varchar(128) NOT NULL,
  toolversion varchar(32) NOT NULL,
  init_command int(11) NOT NULL,
  rollback_command int(11) NOT NULL,
  requesterid varchar(128) DEFAULT NULL,
  reason varchar(128) DEFAULT NULL,
  productversion varchar(255) DEFAULT NULL,
  dbschema varchar(255) DEFAULT NULL,
  PRIMARY KEY (id)
);
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE artifactexecutionattr (
  deployexecutionid bigint(20) NOT NULL,
  attrname varchar(128) NOT NULL,
  attrvalue varchar(128) NOT NULL
);
/*!40101 SET character_set_client = @saved_cs_client */;

USE dbdeploy01;
/*!50001 DROP VIEW IF EXISTS VIEW1*/;
/*!50001 CREATE VIEW VIEW1 AS select VIEW2.A_ID AS A_ID,VIEW2.B_ID AS B_ID,VIEW2.STRING_FIELD AS STRING_FIELD,VIEW2.TIMESTAMP_FIELD AS TIMESTAMP_FIELD,VIEW2.C_ID AS C_ID,VIEW2.EXTRA1 AS EXTRA1,VIEW2.EXTRA2 AS EXTRA2,VIEW2.EXTRA3 AS EXTRA3,VIEW2.EXTRA4 AS EXTRA4 from VIEW2 */;
/*!50001 DROP VIEW IF EXISTS VIEW2*/;
/*!50001 CREATE VIEW VIEW2 AS select TABLE_A.A_ID AS A_ID,TABLE_A.B_ID AS B_ID,TABLE_A.STRING_FIELD AS STRING_FIELD,TABLE_A.TIMESTAMP_FIELD AS TIMESTAMP_FIELD,TABLE_A.C_ID AS C_ID,TABLE_A.EXTRA1 AS EXTRA1,TABLE_A.EXTRA2 AS EXTRA2,TABLE_A.EXTRA3 AS EXTRA3,TABLE_A.EXTRA4 AS EXTRA4 from TABLE_A where (TABLE_A.A_ID = 4) */;
