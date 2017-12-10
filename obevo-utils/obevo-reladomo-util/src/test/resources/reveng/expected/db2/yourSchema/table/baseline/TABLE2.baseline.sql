create table TABLE2
(
    ID bigint not null,
    HOST_NAME varchar(255) not null,
    DEPLOYMENT_ID integer not null,
    ASSOCIATED_VIA varchar(100) not null,
    OWNERSHIP varchar(100) not null
)
GO
alter table TABLE2 add constraint ANPMSRRBC40A8AA_PK primary key (ID)
GO
alter table TABLE2 add constraint AFYSRBC40A8AA_fk_0 foreign key (
    HOST_NAME
)
references GI_VIRTUAL_CONTAINER(
    HYPERVISOR
)
GO
alter table TABLE2 add constraint AFYSRBC40A8AA_fk_1 foreign key (
    HOST_NAME
)
references GI_VIRTUAL_CONTAINER(
    VM
)
GO
alter table TABLE2 add constraint AFYSRBC40A8AA_fk_2 foreign key (
    HOST_NAME
)
references GI_HOST(
    HOST_NAME
)
GO
create index AFYSRBC40A8AA_IDX0 on TABLE2(HOST_NAME)
GO