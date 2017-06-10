//// CHANGE name=change0
CREATE TABLE table_b_with_multicol_fk (
    b_id integer NOT NULL,
    other_a1_id integer,
    other_a2_id integer
);



GO

//// CHANGE name=change1
ALTER TABLE ONLY table_b_with_multicol_fk
    ADD CONSTRAINT table_b_with_multicol_fk_pkey PRIMARY KEY (b_id);



GO

//// CHANGE name=change2
ALTER TABLE ONLY table_b_with_multicol_fk
    ADD CONSTRAINT fk_a_multicol FOREIGN KEY (other_a1_id, other_a2_id) REFERENCES table_a_multicol_pk(a1_id, a2_id);


--
-- PostgreSQL database dump complete
--

GO
