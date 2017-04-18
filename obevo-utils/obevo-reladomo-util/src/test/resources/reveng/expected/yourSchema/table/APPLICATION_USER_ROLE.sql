//// CHANGE name="init-table"
create table APPLICATION_USER_ROLE
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

//// CHANGE INDEX name="index0"
alter table APPLICATION_USER_ROLE add constraint APPLCTN_SR_ROLE_PK primary key (ID, OUT_UTC)

//// CHANGE INDEX name="index1"
create index APPLCTN_SR_RL_IDX0 on APPLICATION_USER_ROLE(APPLICATION_ID, OUT_UTC)

//// CHANGE INDEX name="index2"
create index APPLCTN_SR_RL_IDX1 on APPLICATION_USER_ROLE(USER_ID, OUT_UTC)

//// CHANGE INDEX name="index3"
create index APPLCTN_SR_RL_IDX2 on APPLICATION_USER_ROLE(ROLE_ID, OUT_UTC)
