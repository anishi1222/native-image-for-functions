# native-image-for-functions

A sample project that compiles a [Micronaut](https://micronaut.io/) HTTP application into a
[GraalVM native image](https://www.graalvm.org/latest/reference-manual/native-image/) and runs it on
**Azure Functions** using a [custom handler](https://learn.microsoft.com/azure/azure-functions/functions-custom-handlers).

Because the application is shipped as a self-contained native executable, it starts in milliseconds and
keeps a small memory footprint — a good fit for the **Flex Consumption** plan where cold starts matter.

---

## Table of contents

- [How it works](#how-it-works)
- [Tech stack](#tech-stack)
- [Project layout](#project-layout)
- [The HTTP endpoint](#the-http-endpoint)
- [Prerequisites](#prerequisites)
- [Build](#build)
- [Run locally](#run-locally)
- [Package for Azure Functions](#package-for-azure-functions)
- [Deploy to Azure](#deploy-to-azure)
- [Configuration reference](#configuration-reference)
- [Testing](#testing)
- [Security](#security)
- [References](#references)

---

## How it works

Azure Functions custom handlers let you run any executable that exposes an HTTP server. The Functions host
forwards each incoming trigger to that executable. In this project the executable is a Micronaut + Netty server
built as a native image.

```mermaid
flowchart LR
    client([HTTP client]) -->|GET /api/greeting| host[Azure Functions host]
    host -->|forwards request| handler[demofunc native executable<br/>Micronaut + Netty]
    handler -->|JSON response| host
    host --> client
```

- The Functions host is configured with a `customHandler` whose `defaultExecutablePath` is `demofunc`.
- `enableForwardingHttpRequest` is `true`, so the original HTTP request is forwarded as-is to the Micronaut server.
- The Micronaut server listens on the port provided by `FUNCTIONS_CUSTOMHANDLER_PORT` and serves requests under the `/api` context path.

---

## Tech stack

| Area | Choice |
| --- | --- |
| Language | Java 25 |
| Framework | Micronaut Platform 5.0.x |
| HTTP runtime | Netty |
| Serialization | Micronaut Serialization (Jackson) |
| Native build | GraalVM `native-image` via `native-maven-plugin` |
| Build-time optimization | Micronaut AOT |
| Build tool | Maven |
| Hosting | Azure Functions (custom handler, Linux, Flex Consumption) |
| Tests | JUnit 5 + Micronaut Test |

Key versions are centralized as properties in [pom.xml](pom.xml) (for example `jdk.version`, `micronaut.version`,
`azure.functions.maven.plugin.version`).

---

## Project layout

```text
.
├── Dockerfile                       # Multi-stage native build + minimal runtime image
├── assembly.xml                     # maven-assembly descriptor (zips target/app)
├── compress.sh                      # Re-zips the package with no compression for deployment
├── aot-native-image.properties      # Micronaut AOT settings for native-image packaging
├── aot-jar.properties               # Micronaut AOT settings for jar packaging
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/dev/logicojp/micronaut/
│   │   │   ├── Application.java      # Micronaut entry point
│   │   │   ├── GreetingService.java  # @Controller exposing GET /greeting
│   │   │   └── Message.java          # @Serdeable response record
│   │   ├── function/                 # Azure Functions metadata
│   │   │   ├── host.json             # customHandler + extension bundle config
│   │   │   ├── local.settings.json   # FUNCTIONS_WORKER_RUNTIME=custom
│   │   │   └── greeting/function.json# httpTrigger (anonymous GET) binding
│   │   └── resources/
│   │       ├── application.properties
│   │       └── logback.xml
│   └── test/java/dev/logicojp/micronaut/
│       ├── GreetingServiceTest.java
│       └── MessageTest.java
└── .github/workflows/
    └── deploy-native-image.yml       # Build native image and deploy to Azure Functions
```

---

## The HTTP endpoint

A single endpoint is exposed:

```
GET /api/greeting?name={name}
```

- `name` is optional and defaults to `world`.
- The response is JSON produced from the `Message` record.

Example:

```bash
curl "http://localhost:8080/api/greeting?name=Taro"
# {"message":"Hi, Taro, what's up?"}

curl "http://localhost:8080/api/greeting"
# {"message":"Hi, world, what's up?"}
```

The `/api` prefix comes from `micronaut.server.context-path` and lines up with the Azure Functions default route prefix.

---

## Prerequisites

| Tool | Notes |
| --- | --- |
| JDK 25 (GraalVM) | Required for the `native-image` build. Oracle GraalVM / GraalVM CE for JDK 25. |
| Maven 3.9+ | Or use the GraalVM image / CI. |
| Docker | Optional — for the containerized native build. |
| Azure Functions Core Tools v4 | Optional — to run the function locally with `func`. |
| Azure CLI + subscription | For deploying to Azure. |

---

## Build

The default Maven packaging is `native-image`, so a standard install produces the native executable and the
deployable package in one step:

```bash
# Build the GraalVM native image and assemble the Azure Functions package
mvn -B install

# Produce the deployment artifact target/app.zip (stored, uncompressed)
./compress.sh
```

After `mvn install`:

- `target/demofunc` is the native executable.
- `target/app/` contains the executable together with the Functions metadata (`host.json`, `greeting/function.json`, ...).
- `target/demofunc.zip` is the assembled package; `compress.sh` repackages it into `target/app.zip`.

> The native image is built with `--no-fallback -Ob --gc=G1` (see the `native` profile in [pom.xml](pom.xml)).

### Build the JAR variant instead

```bash
mvn -Dpackaging=jar install
```

### Build with Docker

The [Dockerfile](Dockerfile) performs the native build inside the official GraalVM image and ships the
executable on a minimal base image:

```bash
docker build -t demofunc .
docker run --rm -p 8080:8080 demofunc
```

---

## Run locally

### Option A — run the native executable directly

```bash
./target/demofunc
# then, in another shell:
curl "http://localhost:8080/api/greeting?name=Taro"
```

### Option B — run through Azure Functions Core Tools

Run `func` from the assembled folder, which contains both the executable and `host.json`:

```bash
cd target/app
func start
```

---

## Package for Azure Functions

Packaging is handled by Maven plus a small shell step:

1. `maven-resources-plugin` copies `src/main/function/**` and the native executable into `target/app`.
2. `maven-assembly-plugin` (using [assembly.xml](assembly.xml)) zips `target/app` into `target/demofunc.zip`.
3. [compress.sh](compress.sh) extracts that archive and re-zips it **without compression** (`zip -0`) into
   `target/app.zip`. Storing the native binary uncompressed keeps it directly executable after extraction on the host.

The resulting `target/app.zip` is the artifact deployed to Azure Functions.

---

## Deploy to Azure

### Option A — GitHub Actions (recommended)

[.github/workflows/deploy-native-image.yml](.github/workflows/deploy-native-image.yml) builds the native image and
deploys it on every push to `main` (and via manual dispatch). It authenticates to Azure with OIDC federated
credentials, so no long-lived secrets are stored.

Configure these repository secrets:

| Secret | Purpose |
| --- | --- |
| `AZURE_CLIENT_ID` | App registration (federated credential) client ID |
| `AZURE_TENANT_ID` | Microsoft Entra tenant ID |
| `AZURE_SUBSCRIPTION_ID` | Target subscription |
| `AZURE_FUNCTIONS_APP_NAME` | Target Function App name |

The workflow uploads `target/app.zip` as a build artifact and deploys it with `azure/functions-action`.

### Option B — azure-functions-maven-plugin

The `azure-functions-maven-plugin` is preconfigured in [pom.xml](pom.xml) (Linux, custom runtime, Flex Consumption).
Adjust the `functionAppName`, `resourceGroup`, and `region` properties, then:

```bash
az login
mvn -B install
mvn azure-functions:deploy
```

### Option C — Azure Functions Core Tools

```bash
mvn -B install && ./compress.sh
az functionapp deployment source config-zip \
  -g <resource-group> \
  -n <function-app-name> \
  --src target/app.zip
```

Once deployed, the endpoint is available at:

```
https://<function-app-name>.azurewebsites.net/api/greeting?name=Taro
```

---

## Configuration reference

### `src/main/resources/application.properties`

| Property | Value | Notes |
| --- | --- | --- |
| `micronaut.application.name` | `demofunc` | Application name. |
| `micronaut.server.port` | `${FUNCTIONS_CUSTOMHANDLER_PORT:8080}` | Uses the port supplied by the Functions host; falls back to `8080` locally. |
| `micronaut.server.context-path` | `/api` | Matches the Azure Functions route prefix. |

### `src/main/function/host.json`

- `customHandler.description.defaultExecutablePath`: `demofunc`
- `customHandler.enableForwardingHttpRequest`: `true` (forwards the raw HTTP request to the handler)
- `extensionBundle`: `[4.*, 5.0.0)`
- `functionTimeout`: `00:02:00`
- Dynamic concurrency is enabled.

### `src/main/function/greeting/function.json`

- `httpTrigger`, `authLevel: anonymous`, method `GET`
- HTTP output binding

### Micronaut AOT

[aot-native-image.properties](aot-native-image.properties) and [aot-jar.properties](aot-jar.properties) drive
build-time optimizations (precomputed property sources, build-time service loading, GraalVM config generation, etc.).

---

## Testing

Tests use JUnit 5 and Micronaut Test (`GreetingServiceTest` exercises the endpoint via an injected HTTP client):

```bash
mvn -Dpackaging=jar test
```

> Use `-Dpackaging=jar` when running Maven goals that should not trigger the native-image packaging.

---

## Security

Dependency updates are managed by Dependabot ([.github/dependabot.yml](.github/dependabot.yml)) for Maven,
GitHub Actions, and Docker. The build also pins the Maven download in the [Dockerfile](Dockerfile) with a verified
SHA-512 checksum, and GitHub Actions are pinned to commit SHAs.

For how Dependabot alerts originating from build-time plugin transitive dependencies are triaged, see
[docs/security/dependabot-dismissal-policy.md](docs/security/dependabot-dismissal-policy.md).

---

## References

- [Micronaut User Guide](https://docs.micronaut.io/latest/guide/index.html)
- [Micronaut AOT documentation](https://micronaut-projects.github.io/micronaut-aot/latest/guide/)
- [Micronaut Serialization documentation](https://micronaut-projects.github.io/micronaut-serialization/latest/guide/)
- [Micronaut Maven Plugin documentation](https://micronaut-projects.github.io/micronaut-maven-plugin/latest/)
- [GraalVM Native Image](https://www.graalvm.org/latest/reference-manual/native-image/)
- [Azure Functions custom handlers](https://learn.microsoft.com/azure/azure-functions/functions-custom-handlers)
- [Azure Functions Flex Consumption plan](https://learn.microsoft.com/azure/azure-functions/flex-consumption-plan)
- [maven-enforcer-plugin](https://maven.apache.org/enforcer/maven-enforcer-plugin/)


