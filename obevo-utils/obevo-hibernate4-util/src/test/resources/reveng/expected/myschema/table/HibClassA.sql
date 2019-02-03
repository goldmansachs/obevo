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

//// CHANGE FK name=FK_beta180euspqa24wi4npniw0g
alter table HibClassA 
        add constraint FK_beta180euspqa24wi4npniw0g 
        foreign key (hibClassB_id) 
        references HibClassB
GO

//// CHANGE FK name=FK_7q1lnr4mrmnll582slvxxe5wi
alter table HibClassA_AUD 
        add constraint FK_7q1lnr4mrmnll582slvxxe5wi 
        foreign key (REV) 
        references REVINFO
GO
