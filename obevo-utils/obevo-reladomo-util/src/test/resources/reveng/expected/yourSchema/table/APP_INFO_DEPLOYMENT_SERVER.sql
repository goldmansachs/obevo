//// CHANGE name="init-table"
create table APP_INFO_DEPLOYMENT_SERVER
(
    ID bigint not null,
    HOST_NAME varchar(255) not null,
    DEPLOYMENT_ID integer not null,
    ASSOCIATED_VIA varchar(100) not null,
    OWNERSHIP varchar(100) not null
)

//// CHANGE FK name="fk0"
alter table APP_INFO_DEPLOYMENT_SERVER add constraint AFYSRBC40A8AA_fk_0 foreign key (
    HOST_NAME
)
references GI_VIRTUAL_CONTAINER(
    HYPERVISOR
)

//// CHANGE INDEX name="index0"
alter table APP_INFO_DEPLOYMENT_SERVER add constraint ANPMSRRBC40A8AA_PK primary key (ID)

//// CHANGE FK name="fk1"
alter table APP_INFO_DEPLOYMENT_SERVER add constraint AFYSRBC40A8AA_fk_1 foreign key (
    HOST_NAME
)
references GI_VIRTUAL_CONTAINER(
    VM
)

//// CHANGE INDEX name="index1"
create index AFYSRBC40A8AA_IDX0 on APP_INFO_DEPLOYMENT_SERVER(HOST_NAME)

//// CHANGE FK name="fk2"
alter table APP_INFO_DEPLOYMENT_SERVER add constraint AFYSRBC40A8AA_fk_2 foreign key (
    HOST_NAME
)
references GI_HOST(
    HOST_NAME
)
