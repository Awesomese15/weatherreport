# Project README

## Overview

This project uses Apache Maven as its build automation tool and is configured with a parent POM. It leverages Spring Boot and its Maven plugin for building and packaging the application, including support for creating OCI images.

## Getting Started

### Reference Documentation

For more detailed information, please refer to the following resources:

- [Official Apache Maven documentation](https://maven.apache.org/guides/index.html)
- [Spring Boot Maven Plugin Reference Guide](https://docs.spring.io/spring-boot/3.4.5/maven-plugin)
- [Create an OCI image](https://docs.spring.io/spring-boot/3.4.5/maven-plugin/build-image.html)

### Maven Parent Overrides

Maven projects inherit configuration elements from their parent POMs. While this inheritance is generally beneficial, some elements such as `<license>` and `<developers>` may be inherited unintentionally.

To avoid unwanted inheritance of these elements, this project’s POM contains empty overrides for them. If you switch to a different parent POM and want to inherit these elements, you will need to remove these overrides manually.

## Building the Project

To build the project, run:

```bash
mvn clean install
```

## Packaging and Running

To package the application as a runnable JAR:

```bash
mvn package
```

To run the application:

```bash
java -jar target/weatherreport-0.0.1-SNAPSHOT.jar
```

## Creating an OCI Image

This project supports building OCI images using the Spring Boot Maven plugin. For more information, see the [Create an OCI image](https://docs.spring.io/spring-boot/3.4.5/maven-plugin/build-image.html) guide.



---

For any questions or contributions, please open an issue or submit a pull request.