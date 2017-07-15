# Getting Started with the Amelia DSL
## Installation
The Amelia DSL is an Eclipse plugin, that means you need Eclipse in order to generate executable deployment artifacts. You may also use Amelia as a Maven plugin, that way it is configured as a stand-alone compiler (cf. [Maven configuration](MavenConfiguration.md)).

**Note**: We recommend using Eclipse Mars. It's the one we use 😉

In Eclipse, add the Amelia Update Site URL: http://unicesi.github.io/amelia/releases and follow the steps in the installation wizard. If you've never done that before, the [Eclipse help site](http://help.eclipse.org/mars/index.jsp?topic=/org.eclipse.platform.doc.user/tasks/tasks-124.htm) can give you a hand.

## The helloworld-rmi Example
The `helloworld-rmi` example is a small project that comes with the FraSCAti distribution. During this tutorial we will use this example to explain the basics of specifying deployment artifacts with the Amelia DSL.

This example comprises two SCA components: a `Server`, exposing a print service through RMI, and a `Client`, consuming that service to print a message in the standard output. These components are configured by default to run in the same machine, but with litle effort we can change that, running each component in a separate node.

Generally, to run this example you only need to do two main tasks: _compile_ the source code, and then _run_ the components having into account the dependencies among them. The latter is generally time consuming and error prone in complex projects, because you have to carefully execute each component into its corresponding node respecting the dependencies order. Moreover, when you add several machines to the scenario, you need to perform another task: _transport_ either the source code or the compiled artifacts to the corresponding computing nodes. This may also requires relocating local resources, so they are available for the components using them.

## Specifying Deployment Artifacts with Amelia
Amelia provides the syntax and semantics to abstract those repetitive deployment tasks into composable elements that will allow you to perform systematic executions of SCA systems.

In Amelia there are two compilation units (think of Java interfaces and classes): _subsystems_ and _deployments_. The first ones contain the definition of hosts (computing nodes) and execution rules; the second ones allow to configure deployment strategies from subsystem definitions.

### Subsystems
First, let's specify the host in which the `Server` and `Client` components will run, _i.e._, _localhost_.

**Note**: Amelia makes use of SSH to communicate with remote machines (including _localhost_), and FTP to transport resources. In this tutorial we use the default SSH & FTP ports 21 and 22.
```java
var Host localhost = new Host("localhost", 21, 22, "user", "pass", "Ubuntu-16.04")
```
Notice that you can specify hosts using either the constructors in [Host](https://github.com/unicesi/amelia/blob/master/maven/org.amelia.dsl.lib/src/main/java/org/amelia/dsl/lib/descriptors/Host.java) or the helper methods in [Hosts](https://github.com/unicesi/amelia/blob/master/maven/org.amelia.dsl.lib/src/main/java/org/amelia/dsl/lib/util/Hosts.java).

Now, to specify deployment actions you only need to group command declarations as rules. An execution rule has three parts: target, dependencies (other targets), and commands. It's syntax looks like this:
```make
target2: target0, target1, ...;
    command1
    command2
    ...
```
If a rule has no dependencies, don't use the trailing semi-colon.
Let's see how to specify the compilation and execution of the `helloworld-rmi` example:
```java
compilation:
    cd "$FRASCATI_HOME/examples/helloworld-rmi"
    compile "server/src" "server"
    compile "client/src" "client"

execution: compilation;
    cd "$FRASCATI_HOME/examples/helloworld-rmi"
    run "helloworld-rmi-server" -libpath "server.jar"
    run "helloworld-rmi-client" -libpath "client.jar" -s "r" -m "run"
```
Notice that $FRASCATI_HOME is not related to Amelia in any way, it's just an environment variable that gets resolved during the SSH session.

Of course, different set of rules can express the same, that's how programming works! let's see another way of specifying the deployment actions above:
```java
server:
    cd "$FRASCATI_HOME/examples/helloworld-rmi"
    compile "server/src" "server"
    run "helloworld-rmi-server" -libpath "server.jar"

client: server;
    cd "$FRASCATI_HOME/examples/helloworld-rmi"
    compile "client/src" "client"
    run "helloworld-rmi-client" -libpath "client.jar" -s "r" -m "run"
```
In the first case the overall execution would be:
1. Change the current directory to _$FRASCATI_HOME/examples/helloworld-rmi_
2. Compile source code of the `Server` component
3. Compile source code of the `Client` component
4. Change the current directory to _$FRASCATI_HOME/examples/helloworld-rmi_
5. Run the `Server` component
6. Run the `Client` component

As these commands are executed in the same SSH session, you can ommit step 4. Now it's your turn, what would be the overall execution for the second set of rules?

#### Putting it all together
Now that we know how to specify hosts and execution rules, let's create a subsystem `HelloworldRMI`:
```java
package subsystems

import org.amelia.dsl.lib.descriptors.Host

subsystem HelloworldRMI {
    
    var Host localhost = new Host("localhost", 21, 22, "user", "pass", "Ubuntu-16.04")
    
    on localhost {
        server:
            cd "$FRASCATI_HOME/examples/helloworld-rmi"
            compile "server/src" "server"
            run "helloworld-rmi-server" -libpath "server.jar"

        client: server;
            cd "$FRASCATI_HOME/examples/helloworld-rmi"
            compile "client/src" "client"
            run "helloworld-rmi-client" -libpath "client.jar" -s "r" -m "run"
    }
}
```
All rules without dependencies are executed concurrently*, while commands within the same rule are executed sequentially.

*: In fact, if they are executed using the same SSH session they will be executed sequentially. If concurrent execution is desired, you must create several instances of the same node, for example:
```java
package ^package

import org.amelia.dsl.lib.descriptors.Host

subsystem Subsystem {
    
    var Host localhost1 = new Host("localhost", 21, 22, "user", "pass", "local1")
    var Host localhost2 = new Host("localhost", 21, 22, "user", "pass", "local2")
    
    on localhost1 {
        target1: ...
            cmd "echo concurrent"
    }
    
    on localhost2 {
        target2:
            cmd "echo concurrent"
        target3: target1;
            cmd "echo after target1"
    }
}
```
You can find a complete list of the supported commands [here](Commands.md).

Subsystems are parameterizable. Parameters are useful for instantiating subsystems with different values. For example, the host may be a parameter, that way the components may be executed in several hosts abstracting the execution rules:
```java
package subsystems

import org.amelia.dsl.lib.descriptors.Host

subsystem HelloworldRMI {
    
    param Host host
    
    on host {
        server:
            cd "$FRASCATI_HOME/examples/helloworld-rmi"
            compile "server/src" "server"
            run "helloworld-rmi-server" -libpath "server.jar"

        client: server;
            cd "$FRASCATI_HOME/examples/helloworld-rmi"
            compile "client/src" "client"
            run "helloworld-rmi-client" -libpath "client.jar" -s "r" -m "run"
    }
}
```
In the next section, we will see how to pass a parameter value to the subsystem.

### Deployment

When executing the subsystem `HelloworldRMI` you're getting the default deployment. That is, a single execution with no automatic shutdown. However, if a subsystem has dependencies or parameters, it is necessary to create a deployment strategy. Deployment strategies are also useful for automatically repeating the same deployment, or retrying on failure; these are custom behaviors regarding how the deployment is executed. For example:
```java
package deployments

import org.amelia.dsl.lib.util.RetryOnFailure

include subsystems.HelloworldRMI

deployment RetryOnFailure {
    add(new HelloworldRMI) // deploy one instance of the subsystem
	var helper = new RetryableDeployment()
	helper.deploy([
		start(true) // Deploy and stop executed components when finish
	], 3) // In case of failure, retry 2 more times
}
```

```java
package deployments

import org.amelia.dsl.lib.util.RetryOnFailure

include subsystems.HelloworldRMI

deployment SequentialDeployments {
    add(new HelloworldRMI)
	for (i : 1..5) {
		start(true) // Deploy and stop executed components when finish
	}
}
```

If the subsystem expects a parameter, this is the way to go:

```java
package deployments

import org.amelia.dsl.lib.util.RetryOnFailure

include subsystems.HelloworldRMI

deployment SequentialDeployments {
    var Host host = new Host("localhost", 21, 22, "user", "pass", "Ubuntu-16.04")
    add(new HelloworldRMI(host))
    start(true) // Deploy and stop executed components when finish
}
```

In case a subsystem expects several parameters, they must be passed in the same order they were defined.
