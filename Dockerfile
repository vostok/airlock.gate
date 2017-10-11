FROM maven:onbuild-alpine

EXPOSE 8888

CMD ["java", "-Xms256m", "-Xmx256m", "-XX:+UseG1GC", "-XX:MaxGCPauseMillis=200", "-jar","target/vostok-airlock-gate-1.0-SNAPSHOT.jar"]
