FROM public.ecr.aws/docker/library/maven:3.9.8-amazoncorretto-21 AS builder

WORKDIR /tmp

COPY ./pom.xml ./pom.xml

RUN mvn dependency:go-offline

COPY ./src ./src

RUN mvn package


FROM public.ecr.aws/docker/library/amazoncorretto:21.0.4-alpine

ENV TZ=Asia/Hong_Kong

#RUN groupadd --system spring && useradd --system --gid spring spring
#USER spring:spring

WORKDIR /tmp

COPY extra-directories ./extra-directories

ARG JAR_FILE=/tmp/target/*.jar
COPY --from=builder ${JAR_FILE} hktv-ars.jar

ENTRYPOINT exec java $JAVA_OPTS -jar ./hktv-ars.jar