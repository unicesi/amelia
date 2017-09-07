# Amelia Example Projects

These examples are intended to demonstrate features of the Amelia DSL. Keep in mind that most of them assume an Ubuntu target environment (e.g., apt-get as package manager). However, these assumptions are made only to keep them as simple as possible; updating the examples to support other platforms should not be complicated.

To run the examples, you must first compile them using maven:

```
mvn package
```

Then, perform the necessary actions to run specific projects. You may find instructions inside each project's directory:

- [Base library](base) defines both Java files and Amelia subsystems that are reused within each example project
- [Eclipse plugin project](eclipse-plugin-project) shows the basic configuration to use Amelia within an Eclipse plugin project
- [Docker's helloworld](docker-hello-world) installs Docker Community Edition and runs the hello-world container
- [FraSCAti helloworld-rmi](frascati-helloworld-rmi) runs FraSCAti 1.4 on docker and compiles and executes the helloworld-rmi project from the FraSCAti distribution
