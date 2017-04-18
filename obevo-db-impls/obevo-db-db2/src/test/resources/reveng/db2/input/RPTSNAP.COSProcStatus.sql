CREATE TABLE COSProcStatus ( 
	ProcName      	char(8) NOT NULL,
	RunDate       	datetime NOT NULL,
	RunLocation   	char(2) NOT NULL,
	StartTime     	datetime NULL,
	EndTime       	datetime NULL,
	Status        	char(1) NOT NULL,
	UpdateUserId  	varchar(10) NOT NULL,
	TimeUpdated   	datetime NOT NULL,
	UpdateLocation	char(2) NOT NULL 
	)
LOCK DATAROWS
GO

CREATE UNIQUE CLUSTERED INDEX COSProcSta_890483221
	ON COSProcStatus(ProcName, RunDate, RunLocation)
GO

