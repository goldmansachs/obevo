//// CHANGE name=change0
create table HibClassA (
        id integer not null,
        name varchar(255),
        hibClassB_id integer,
        primary key (id)
    ) lock datarows
GO

//// CHANGE name=change1
create table HibClassA_AUD (
        id integer not null,
        REV integer not null,
        REVTYPE smallint,
        name varchar(255),
        hibClassB_id integer,
        primary key (id, REV)
    ) lock datarows
GO

//// CHANGE FK name=FK731C08AEA1D86A6
alter table HibClassA 
        add constraint FK731C08AEA1D86A6 
        foreign key (hibClassB_id) 
        references HibClassB
GO

//// CHANGE FK name=FKA016995BDF74E053
alter table HibClassA_AUD 
        add constraint FKA016995BDF74E053 
        foreign key (REV) 
        references REVINFO
GO
