# syntax=docker/dockerfile:1.7
# Shared build stage. It produces the linux/amd64 native executable, the
# Functions zip package, and the unpacked app layout reused by the targets below.
FROM container-registry.oracle.com/graalvm/native-image:25-muslib-ol9 AS build
WORKDIR /
ARG MAVEN_VERSION=3.9.16
ARG MAVEN_SHA512=831a8591fe20c8243b1dbe7d71e3244f31d1665b0804b2e825e38cbbe5ce0cafb8338851f90780735568773e0a6cd07bbec107cda0b896b008b861075358b6f6
# install maven
ADD https://dlcdn.apache.org/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz /tmp/apache-maven.tar.gz
RUN microdnf install -y ca-certificates gzip unzip zip && microdnf clean all
RUN echo "${MAVEN_SHA512}  /tmp/apache-maven.tar.gz" | sha512sum -c - && tar -xzf /tmp/apache-maven.tar.gz -C / && rm -f /tmp/apache-maven.tar.gz

WORKDIR /app

COPY pom.xml assembly.xml compress.sh aot-jar.properties aot-native-image.properties ./
COPY src src

# Build the linux/amd64 native image and the Azure Functions zip package.
RUN --mount=type=cache,target=/root/.m2 chmod +x ./compress.sh \
    && /apache-maven-${MAVEN_VERSION}/bin/mvn -B clean verify -Pnative -DskipTests \
    && ./compress.sh \
    && mkdir -p /functions-app \
    && unzip -q /app/target/app.zip -d /functions-app \
    && rm -f /functions-app/local.settings.json \
    && chmod 0755 /functions-app/demofunc

# Export target/app.zip for Azure Functions Flex Consumption zip deployment.
# Build with:
#   docker build --platform linux/amd64 --target artifact --output type=local,dest=target .
FROM scratch AS artifact
COPY --from=build /app/target/app.zip /app.zip

# Local-only smoke test image. This bypasses the Azure Functions host and runs
# the Micronaut native executable directly on port 8080.
# Build with:
#   docker build --platform linux/amd64 --target local-runtime -t demofunc-local .
FROM alpine AS local-runtime
EXPOSE 8080
COPY --from=build /app/target/demofunc /usr/local/bin/demofunc
CMD ["demofunc"]

# Azure Functions on Azure Container Apps image. Do not add CMD or ENTRYPOINT:
# the Azure Functions base image starts the Functions host, which then launches
# demofunc via host.json customHandler.
# Build with:
#   docker build --platform linux/amd64 --target functions-runtime -t demofunc-functions .
FROM mcr.microsoft.com/azure-functions/base:4 AS functions-runtime
ENV AzureWebJobsScriptRoot=/home/site/wwwroot \
    AzureFunctionsJobHost__Logging__Console__IsEnabled=true \
    FUNCTIONS_WORKER_RUNTIME=Custom
COPY --from=build /functions-app/ /home/site/wwwroot/
