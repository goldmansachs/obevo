//// CHANGE name=change0
create table TABLE2
(
    ID bigint not null,
    HOST_NAME varchar(255) not null,
    DEPLOYMENT_ID integer not null,
    ASSOCIATED_VIA varchar(100) not null,
    OWNERSHIP varchar(100) not null
)
GO

//// CHANGE name=ANPMSRRBC40A8AA_PK
alter table TABLE2 add constraint ANPMSRRBC40A8AA_PK primary key (ID)
GO

//// CHANGE FK name=AFYSRBC40A8AA_fk_0
alter table TABLE2 add constraint AFYSRBC40A8AA_fk_0 foreign key (
    HOST_NAME
)
references GI_VIRTUAL_CONTAINER(
    HYPERVISOR
)
GO

//// CHANGE FK name=AFYSRBC40A8AA_fk_1
alter table TABLE2 add constraint AFYSRBC40A8AA_fk_1 foreign key (
    HOST_NAME
)
references GI_VIRTUAL_CONTAINER(
    VM
)
GO

//// CHANGE FK name=AFYSRBC40A8AA_fk_2
alter table TABLE2 add constraint AFYSRBC40A8AA_fk_2 foreign key (
    HOST_NAME
)
references GI_HOST(
    HOST_NAME
)
GO

//// CHANGE INDEX name=AFYSRBC40A8AA_IDX0
create index AFYSRBC40A8AA_IDX0 on TABLE2(HOST_NAME)
GO
