FROM mysql:8.0.25 as builder

# That file does the DB initialization but also runs mysql daemon, by removing the last line it will only init
RUN ["sed", "-i", "s/exec \"$@\"/echo \"not running $@\"/", "/usr/local/bin/docker-entrypoint.sh"]

# needed for intialization
ENV MYSQL_ROOT_PASSWORD=1234

COPY /manifests/deploy/local/setdb/my.cnf /etc/mysql/conf.d/

COPY /src/db/initialize.sql /docker-entrypoint-initdb.d/
COPY /src/db/initialize_qrtz.sql /docker-entrypoint-initdb.d/
COPY /src/db/initialize_data.sql /docker-entrypoint-initdb.d/
# COPY /src/db/local/test_data.sql /docker-entrypoint-initdb.d/

# Need to change the datadir to something else that /var/lib/mysql because the parent docker file defines it as a volume.
# https://docs.docker.com/engine/reference/builder/#volume :
#       Changing the volume from within the Dockerfile: If any build steps change the data within the volume after
#       it has been declared, those changes will be discarded.
RUN ["/usr/local/bin/docker-entrypoint.sh", "mysqld", "--datadir", "/initialized-db"]

FROM mysql:8.0.25
COPY /manifests/deploy/local/setdb/my.cnf /etc/mysql/conf.d/
COPY --from=builder /initialized-db /var/lib/mysql