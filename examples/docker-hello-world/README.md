# Docker hello-world

This example installs Docker Community Edition and runs the hello-world container. It requires specifying the following parameters:

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
Hello from Docker!
This message shows that your installation appears to be working correctly.

To generate this message, Docker took the following steps:
 1. The Docker client contacted the Docker daemon.
 2. The Docker daemon pulled the "hello-world" image from the Docker Hub.
 3. The Docker daemon created a new container from that image which runs the
    executable that produces the output you are currently reading.
 4. The Docker daemon streamed that output to the Docker client, which sent it
    to your terminal.

To try something more ambitious, you can run an Ubuntu container with:
 $ docker run -it ubuntu bash

Share images, automate workflows, and more with a free Docker ID:
 https://cloud.docker.com/

For more examples and ideas, visit:
 https://docs.docker.com/engine/userguide/
```
