# Amelia Style Guide

The following are style considerations when developing Amelia specifications that will facilitate not only development but maintenance tasks:

- Modules, as objects in OOP, should be small. Each module encapsulates the deployment life cycle of one subsystem; the smaller this subsystem is, the more reusability it promotes. A module containing 3000 lines of code is not only very difficult to understand and modify, but also cannot be reused at all by other subsystems.
- Strings use variable interpolation rather than concatenation. This makes reading code much easier, especially when the string is part of a command declaration.
- Naming subsystems and execution rules is relevant. Names should be short yet descriptive. Mixing letters, numbers, and symbols says nothing about the subsystem or the meaning of the commands encapsulated by the rule. In the case of subsystems, use packages as an extension of the name, that way the name is as specific as it needs to be. In the case of rules, name them after a phase in the subsystem's deployment life cycle.
- Subsystems do not contain magic numbers or strings, they reference Java enumerations instead.
- Subsystems and variables are documented using Java-like comments. People and tools are already familiarised with them.
- Subsystems contain as few variables as possible.
- Subsystems should not contain long lines. However, readability has priority over length.
- Subsystems use Java methods and extensions to improve code readability and, at the same time, promote reusability.
- Subsystems use parameters and inclusions to avoid duplicating code.
- Subsystems use AtomicBoolean instances to control the execution flow and avoid executing unnecessary commands.
- Subsystems do not contain hard-coded system paths. This makes necessary to update the specifications if the source code is moved to another location.
- Subsystems do not declare an excesive amount of parameters. This may mean that the level of abstraction is not appropriate. Instead, they rely on included parameters.
