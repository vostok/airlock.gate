FROM maven:onbuild-alpine
EXPOSE 8888

CMD ["java","-jar","target/airlock-gate-1.0-SNAPSHOT.jar"]
