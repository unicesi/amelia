# Amelia
Amelia is a Domain Specific Language for deploying SCA components, with custom commands to compile and execute FraSCAti applications.

## Eclipse Update Site

http://unicesi.github.io/amelia/releases

Amelia requires Pascani, so please make sure you add the Pascani update site (http://unicesi.github.io/amelia/pascani) before you install Amelia features.

## Compiling From Sources

If you want to build the Amelia sources locally, you need Maven.

First of all, make sure to increase memory

```
export MAVEN_OPTS="-Xmx512m -XX:MaxPermSize=256m"
```

Then install [Pascani](https://github.com/unicesi/pascani). This extra step is temporarily necessary because Pascani has not been uploaded to Maven central yet. This wont be necessary in the future.

And then run

```
mvn install -file maven/org.amelia.tycho.parent/pom.xml
```
