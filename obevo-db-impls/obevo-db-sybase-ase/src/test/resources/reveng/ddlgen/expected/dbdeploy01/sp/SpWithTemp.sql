create proc SpWithTemp (@MaxCount  int) as
--$Header: /home/cvs/gdtech/costar/src/db/procs/DelExcessSelectTrades.eqd,v 1.2 2010/08/13 18:15:51 jayava Exp $
begin
    /* Adaptive Server has expanded all '*' elements in the following statement */ select #MyTemp.FieldA, #MyTemp.FieldB from #MyTemp
end
GO
sp_procxmode 'SpWithTemp', unchained
GO