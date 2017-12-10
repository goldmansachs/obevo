//// CHANGE name=change0
create table TABLE1
(
    ID bigint not null,
    APPLICATION_ID bigint not null,
    ROLE_ID bigint not null,
    USER_ID bigint not null,
    CREATED_BY varchar(64),
    CREATED_DATE_UTC timestamp,
    LAST_UPDATED_BY varchar(64),
    IN_UTC timestamp not null,
    OUT_UTC timestamp not null
)
GO

//// CHANGE name=APPLCTN_SR_ROLE_PK
alter table TABLE1 add constraint APPLCTN_SR_ROLE_PK primary key (ID, OUT_UTC)
GO

//// CHANGE INDEX name=APPLCTN_SR_RL_IDX0
create index APPLCTN_SR_RL_IDX0 on TABLE1(APPLICATION_ID, OUT_UTC)
GO

//// CHANGE INDEX name=APPLCTN_SR_RL_IDX1
create index APPLCTN_SR_RL_IDX1 on TABLE1(USER_ID, OUT_UTC)
GO

//// CHANGE INDEX name=APPLCTN_SR_RL_IDX2
create index APPLCTN_SR_RL_IDX2 on TABLE1(ROLE_ID, OUT_UTC)
GO
