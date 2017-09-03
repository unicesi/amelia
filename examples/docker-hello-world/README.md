# Docker's hello-world

This example requires specifying the following parameters:

- __host__: the host name to install docker and run the container
- __privileged-user__: a privileged user to install Docker
- __unprivileged-user__: an unprivileged user to run the container

To run the code execute the generated Java application using the following command. Notice you have to replace the parameters and update the jar file name.

```
java -Dhost=192.168.99.100 \
	-Dprivileged-user=root \
	-Dunprivileged-user=miguel \
	-jar target/org.amelia.dsl.examples.docker-0.0.1-SNAPSHOT-jar-with-dependencies.jar
```

If everything worked correctly, you should see the following message:

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
