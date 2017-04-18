


//// METADATA includeEnvs=q1 includePlatforms=DB2,SYBASE_ASE,HSQL
//// CHANGE name=chng1
CREATE TABLE;
//// CHANGE FK name=chng2
ADD FK1
  only pick up this part if at start of line // // CHANGE whatever (disabling this check)

//// CHANGE FK name=chng3


ADD FK2
//// CHANGE name=blank1NoLine
//// CHANGE name=blank2WithLine


//// CHANGE TRIGGER name=trigger1
CREATE TRIGGER ABC123
//// CHANGE name=chng4
  ALTER TABLE position ADD quantity DOUBLE
  

//// CHANGE name=chng5Rollback includeEnvs=abc* excludePlatforms=HSQL
mychange

// ROLLBACK-IF-ALREADY-DEPLOYED
myrollbackcommand

//// CHANGE name=chng5Rollback excludeEnvs=abc*
mychange

// ROLLBACK-IF-ALREADY-DEPLOYED
myrollbackcommand

//// CHANGE name=chng6Inactive INACTIVE
 myinactive change


//// CHANGE name=chng7InactiveWithRollback INACTIVE
inroll change

// ROLLBACK-IF-ALREADY-DEPLOYED
myotherrollbackcommand

