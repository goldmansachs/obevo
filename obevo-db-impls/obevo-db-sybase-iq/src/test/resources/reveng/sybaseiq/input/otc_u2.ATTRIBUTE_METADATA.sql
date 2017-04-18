CREATE TABLE ATTRIBUTE_METADATA  ( 
	ATTRIBUTE_ID      	bigint NOT NULL,
	ATTRIBUTE_GROUP   	varchar(25) NULL,
	ATTRIBUTE_CATEGORY	varchar(25) NULL,
	ATTRIBUTE_NAME    	varchar(25) NULL,
	ATTRIBUTE_TYPE    	varchar(25) NULL,
	IN_TMSTMP         	timestamp NULL,
	OUT_TMSTMP        	timestamp NULL,
	EFF_TMSTMP        	timestamp NULL,
	EXP_TMSTMP        	timestamp NULL,
	PRIMARY KEY(ATTRIBUTE_ID)
)
GO

