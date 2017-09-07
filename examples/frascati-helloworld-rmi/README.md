# FraSCAti helloworld-rmi

This example requires specifying the following parameters:

- __host__: the host name to install docker and run the container
- __privileged-user__: a privileged user to install Docker
- __unprivileged-user__: an unprivileged user to run the container

To run the code execute the generated Java application using the following command. Notice you have to replace the parameters and update the jar file name.

```
java -Dhost=192.168.99.100 \
	-Dprivileged-user=root \
	-Dunprivileged-user=miguel \
	-jar target/frascati-helloworld-rmi-0.11.11-SNAPSHOT-jar-with-dependencies.jar
```
