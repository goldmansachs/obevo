//// CHANGE name=change0
CREATE TABLE table_a (
    a_id integer NOT NULL,
    a2_id integer
);



GO

//// CHANGE name=change1
ALTER TABLE ONLY table_a
    ADD CONSTRAINT table_a_pkey PRIMARY KEY (a_id);



GO
