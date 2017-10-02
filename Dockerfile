FROM maven:onbuild-alpine
EXPOSE 8080

CMD ["java","-jar","target/airlock-gate-1.0-SNAPSHOT.jar"]
