create proc SpWithTemp2 (@MaxCount  int) as
begin
    /* Adaptive Server has expanded all '*' elements in the following statement */ select #MyTemp.FieldA, #MyTemp.FieldB from #MyTemp
end
GO
sp_procxmode 'SpWithTemp2', unchained
GO