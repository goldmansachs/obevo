/****** Object:  StoredProcedure [dbo].[SpWithTemp]    Script Date: 1/1/2017 12:00:00 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
create proc SpWithTemp (@MaxCount  int) as
--comment
begin
    select * from #MyTemp
end
GO
