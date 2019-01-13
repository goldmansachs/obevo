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

//// CHANGE FK name=FKkr5k1hvmc0duc9mxfjd0jnv31
alter table HibClassA 
       add constraint FKkr5k1hvmc0duc9mxfjd0jnv31 
       foreign key (hibClassB_id) 
       references HibClassB
GO

//// CHANGE FK name=FK1vf52j8qgmxg5sfnbaup4f10v
alter table HibClassA_AUD 
       add constraint FK1vf52j8qgmxg5sfnbaup4f10v 
       foreign key (REV) 
       references REVINFO
GO
