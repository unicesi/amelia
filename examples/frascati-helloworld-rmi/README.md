# FraSCAti helloworld-rmi

This example runs FraSCAti 1.4 on docker and compiles and executes the helloworld-rmi project from the FraSCAti distribution. It requires specifying the following parameters:

- __host__: the host name to install docker and run the container
- __privileged-user__: a privileged user to install Docker
- __unprivileged-user__: an unprivileged user to run the container

By default, the project pom defines these parameters as follow:

- host = localhost
- privileged-user = root
- unprivileged-user = ${user.name} # the user running maven

1. To compile the example, execute:

```bash
mvn clean compile
```

2. To run the example, execute:

```bash
mvn exec:java -Dhost=... # specify here the rest of the parameters
```

If everything ran correctly, you should see the following message:

```
Compiling ...
client/src
client

Library client.jar created!
Running OW2 FraSCAti ...

OW2 FraSCAti Standalone Runtime
CLIENT created
CLIENT initialized
Call the service...
Call done!
Exiting OW2 FraSCAti ...
```
