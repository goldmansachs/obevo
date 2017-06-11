create proc SpWithTemp (@MaxCount  int) as
--comment
begin
    select * from #MyTemp
end
GO