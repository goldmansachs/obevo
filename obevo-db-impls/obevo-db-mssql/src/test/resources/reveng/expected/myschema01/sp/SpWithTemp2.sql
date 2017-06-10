create proc SpWithTemp2 (@MaxCount  int) as
--comment
begin
    select * from #MyTemp
end
GO