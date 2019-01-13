create table HibClassA (
        id int not null,
        name varchar(255) null,
        hibClassB_id int null,
        primary key (id)
    ) lock datarows
GO
create table HibClassA_AUD (
        id int not null,
        REV int not null,
        REVTYPE tinyint null,
        name varchar(255) null,
        hibClassB_id int null,
        primary key (id, REV)
    ) lock datarows
GO
alter table HibClassA 
        add constraint FK731C08A9795BA5
        foreign key (hibClassB_id) 
        references HibClassB lock datarows
GO
alter table HibClassA_AUD 
        add constraint FKA016995BDF74E053 
        foreign key (REV) 
        references REVINFO lock datarows
GO