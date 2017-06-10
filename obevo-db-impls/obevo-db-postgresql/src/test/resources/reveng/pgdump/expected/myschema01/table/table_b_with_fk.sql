//// CHANGE name=change0
CREATE TABLE table_b_with_fk (
    b_id integer NOT NULL,
    other_a_id integer
);



GO

//// CHANGE name=change1
ALTER TABLE ONLY table_b_with_fk
    ADD CONSTRAINT table_b_with_fk_pkey PRIMARY KEY (b_id);



GO

//// CHANGE name=change2
ALTER TABLE ONLY table_b_with_fk
    ADD CONSTRAINT fk_a FOREIGN KEY (other_a_id) REFERENCES table_a(a_id);



GO
