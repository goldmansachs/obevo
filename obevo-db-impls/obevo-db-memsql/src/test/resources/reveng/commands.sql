
https://www.postgresql.org/docs/9.6/static/app-pgdump.html

https://www.postgresql.org/download/


pg_dump -O --disable-dollar-quoting -h dbdeploybuild.c87tzbeo5ssa.us-west-2.rds.amazonaws.com -p 5432 --username=deploybuilddbo -s  -d dbdeploy -n dbdeploymeta -f postgre-deploymeta.sqlp
