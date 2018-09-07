# EQUELLA Blackboard Integration

Information about the Equella open source community and supporting documentation can be found at https://equella.github.io/

Versions are repo-wide (ie all building blocks will have the same version for a given build).  Versioning will be loosely tied to SemVer.

Version 1.X.Y will support Blackboard v3200 - v3300
Version 2.W.V will support Blackboard v3400+

## Building the primary building block
```
~$ ./gradlew :oeqPrimary:cleanAndRebuild
```
The war is placed in (cloned repo)/oeqPrimary/build/libs/

## Building the audit building block
For now, this is just a skeleton building block for testing.

Eventually it may be a helper building block to audit the Blackboard / openEQUELLA integration.

```
~$ ./gradlew :oeqAudit:cleanAndRebuild
```
The war is placed in (cloned repo)/oeqAudit/build/libs/

## Building the linkFixer building block
TODO

## Building the gbFixer building block
TODO
