//// CHANGE name=change0
create table HibClassSchemaD (
       id integer not null,
        name varchar(255),
        hibClassC_id integer,
        primary key (id)
    ) lock datarows
GO

//// CHANGE FK name=FKf7hhmrxhgcpqdm6nhgdru1mso
alter table HibClassSchemaD 
       add constraint FKf7hhmrxhgcpqdm6nhgdru1mso 
       foreign key (hibClassC_id) 
       references HibClassSchemaC
GO
