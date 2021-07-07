FROM adoptopenjdk/openjdk11:alpine

RUN mkdir app/ app/resources
WORKDIR app
COPY target/uberjar/*-standalone.jar .
COPY resources/ resources/

CMD java -jar "jira-mendix-integrator-${VERSION}-standalone.jar"
EXPOSE 3000