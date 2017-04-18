create table APP_INFO_DEPLOYMENT_SERVER
(
    ID bigint not null,
    HOST_NAME varchar(255) not null,
    DEPLOYMENT_ID integer not null,
    ASSOCIATED_VIA varchar(100) not null,
    OWNERSHIP varchar(100) not null
);

