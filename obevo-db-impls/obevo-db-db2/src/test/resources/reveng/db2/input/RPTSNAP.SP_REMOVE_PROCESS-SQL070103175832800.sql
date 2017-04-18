CREATE PROCEDURE SP_REMOVE_PROCESS(
            IN in_process_id INTEGER,
            IN in_modified_by varchar(30),
		    OUT p_sqlstate_out char(5),
		    OUT p_sqlcode_out integer,
		    OUT p_error_message varchar(300))
    LANGUAGE SQL
BEGIN

    declare sqlcode integer default 0;
    declare sqlstate char(5) default '00000';
    declare v_level varchar(10);
    declare v_procedure varchar(140) default 'APPDIR.SP_REMOVE_PROCESS(?,?,?,?,?)';
    declare v_message varchar(2048);
    declare v_return_status integer default 0;
    declare v_sp_return_status integer default 0;
    declare v_sqlcode integer default 0;
    declare v_sqlstate char(5) default '00000';

	begin

        -- in case of an exception, return SQLCODE to client

        DECLARE EXIT HANDLER FOR SQLEXCEPTION
            begin
	            select SQLSTATE, SQLCODE INTO p_sqlstate_out, p_sqlcode_out FROM sysibm.Sysdummy1;
                GET DIAGNOSTICS EXCEPTION 1 p_error_message = MESSAGE_TEXT;
	            rollback work;

	            CASE p_sqlstate_out
	                WHEN '80100' THEN
	                    set v_message = 'Failed to provide all required parameters';
                    ELSE
                        set v_message = 'APPDIR.SP_REMOVE_PROCESS(?,?,?,?,?) / rollback -- sqlcode: ' ||
                        cast(p_sqlcode_out as char(10)) || ' sqlstate: ' || p_sqlstate_out;
                END CASE;

                set v_level = 'severe';
	            insert into t_message_log (level, message, date, procedure, user)
	                values (v_level, v_message, current timestamp, v_procedure, current user);
	            commit;
            end;

		begin
				values (SQLSTATE, SQLCODE) INTO p_sqlstate_out, p_sqlcode_out;
	            if (in_process_id <= 0) then
	                    SIGNAL SQLSTATE '80100'
	                    SET MESSAGE_TEXT='Failed to provide all required parameters: ';
	            else        
				delete from t_process_component_mapping where process_id = in_process_id;
				delete from t_process_dependency where process_id = in_process_id;
				delete from t_process_dependency where to_process_id = in_process_id;
						--updating the record to know who is deleting the process
						update T_PROCESS set modified_by=in_modified_by,modified_on=current timestamp where process_id=in_process_id;
						delete from T_PROCESS where PROCESS_ID=in_process_id;
                end if;
		end;
    end; 
END

GO

