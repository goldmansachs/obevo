//// CHANGE name=change0
CREATE TABLE table_a_multicol_pk (
    a1_id integer NOT NULL,
    a2_id integer NOT NULL,
    val3 integer
);



GO

//// CHANGE name=change1
ALTER TABLE ONLY table_a_multicol_pk
    ADD CONSTRAINT table_a_multicol_pk_pkey PRIMARY KEY (a1_id, a2_id);



GO
