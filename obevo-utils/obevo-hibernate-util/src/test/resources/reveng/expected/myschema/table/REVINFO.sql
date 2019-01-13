//// CHANGE name=init
create table REVINFO (
        REV int identity not null,
        REVTSTMP bigint null,
        primary key (REV)
    ) lock datarows
GO
