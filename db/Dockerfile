FROM postgres:alpine
ADD scripts/2021-07-07_init.sql /docker-entrypoint-initdb.d 
RUN chmod a+r /docker-entrypoint-initdb.d/*.sql
EXPOSE 6666