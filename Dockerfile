FROM openjdk:8-jre

COPY target/gatling-report-1.0-SNAPSHOT-capsule-fat.jar reporter.jar

CMD java -jar reporter.jar $LOGS -o $SIMULATION-report -f
