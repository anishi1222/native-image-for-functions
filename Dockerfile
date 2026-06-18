FROM container-registry.oracle.com/graalvm/native-image:25-muslib-ol9 AS build
WORKDIR /
ARG MAVEN_VERSION=3.9.16
ARG MAVEN_SHA512=831a8591fe20c8243b1dbe7d71e3244f31d1665b0804b2e825e38cbbe5ce0cafb8338851f90780735568773e0a6cd07bbec107cda0b896b008b861075358b6f6
# install maven
RUN microdnf install -y ca-certificates gzip && microdnf clean all

ADD https://dlcdn.apache.org/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz apache-maven.tar.gz
RUN echo "${MAVEN_SHA512}  apache-maven.tar.gz" | sha512sum -c - \
	&& tar -xzf apache-maven.tar.gz -C / \
	&& rm -f apache-maven.tar.gz

WORKDIR /app
COPY pom.xml .
COPY src src

# compile the native image
RUN --mount=type=cache,target=/root/.m2 /apache-maven-${MAVEN_VERSION}/bin/mvn clean package -Pnative

FROM alpine
EXPOSE 8080
COPY --from=build /app/target/demofunc .

CMD ["./demofunc"]
