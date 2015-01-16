# gateway.security

[![Build Status][build-status-image]][build-status]

[build-status-image]: https://travis-ci.org/kaazing/gateway.security.svg?branch=develop
[build-status]: https://travis-ci.org/kaazing/gateway.security

# About this Project

The gateway.security hosts common security abstractions of gateway. Gateway integrates with Java Authentication and Authorization Service (JAAS) and this project hosts the pluggable login modules to support authentication and authorization.

# Building this Project

## Minimum requirements for building the project
* Java SE Development Kit (JDK) 7 or higher

## Steps for building this project
0. Clone the repo
0. mvn clean install

# Running this Project

0. Integrate this component in gateway.distribution by updating the version in gateway.distribution's pom
0. Build the corresponding gateway.distribution and use it for application development
