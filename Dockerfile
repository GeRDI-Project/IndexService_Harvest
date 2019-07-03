FROM openjdk:8
COPY ./build/libs/index-service-*.jar /usr/src/persistence-api/app.jar
WORKDIR /usr/src/persistence-api
ENTRYPOINT ["java", "-jar" , "app.jar"]