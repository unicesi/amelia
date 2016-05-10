# Amelia
Amelia is a Domain Specific Language for deploying SCA components, with custom commands to compile and execute FraSCAti applications.

### Eclipse Update Site

http://unicesi.github.io/amelia/releases

Amelia requires Pascani, so please make sure you add the Pascani update site (http://unicesi.github.io/pascani/releases) before you install Amelia features.

### Clone

Before cloning this repository, please notice two things: first, this repository does not contain generated sources, and second, the Eclipse update site is hosted in the `gh-pages` branch. That being said, my advice is to clone each branch separately; this avoids compiling the sources everytime you checkout the `gh-pages` branch. Additionally, this makes cloning the `master` branch lighter.

To clone the `master` branch:
```bash
git clone -b "master" --single-branch https://github.com/unicesi/amelia
```
To clone the `gh-pages` branch:
```bash
git clone -b "gh-pages" --single-branch https://github.com/unicesi/amelia p2-repository
```

### Compiling From Sources

If you want to build the Amelia sources locally, you need Maven.

First of all, make sure to increase memory

```bash
export MAVEN_OPTS="-Xmx512m -XX:MaxPermSize=256m"
```

Then install [Pascani](https://github.com/unicesi/pascani). This extra step is temporarily necessary because Pascani has not been uploaded to Maven central yet. This wont be necessary in the future.

And then run

```bash
mvn install
```

### Questions?

If you have any questions about Amelia, or something doesn't work as expected, please [submit an issue here](https://github.com/unicesi/amelia/issues/new).
