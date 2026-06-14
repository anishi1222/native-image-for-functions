FROM container-registry.oracle.com/graalvm/native-image:25-muslib-ol9 AS build
WORKDIR /
# install maven
RUN microdnf install gzip
RUN curl https://dlcdn.apache.org/maven/maven-3/3.9.16/binaries/apache-maven-3.9.16-bin.tar.gz | tar zx
WORKDIR /app

COPY pom.xml .
COPY src src

# compile the native image
RUN --mount=type=cache,target=/root/.m2 /apache-maven-3.9.16/bin/mvn clean package -Pnative

FROM alpine
EXPOSE 8080
COPY --from=build /app/target/demofunc .

CMD ["./demofunc"]