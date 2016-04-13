# Amelia
A Domain Specific Language for deploying FraSCAti applications

## Clone & Compile the source code

Install [Pascani](https://github.com/unicesi/pascani) and then:

```bash
export MAVEN_OPTS="-Xmx512M -XX:MaxPermSize=512M"
git clone https://github.com/unicesi/amelia
cd amelia
mvn install -file maven/org.amelia.tycho.parent/pom.xml
```
