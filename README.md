# EQUELLA Blackboard Integration

Information about the openEQUELLA community and supporting documentation can be found at https://equella.github.io/

Versions are repo-wide (ie all building blocks / web services will have the same version for a given build).  Versioning will be loosely tied to SemVer.

Version 1.X.Y will support Blackboard v3200 - v3300
Version 2.W.V will support Blackboard v3400+

## Building the primary building block
```
~$ ./gradlew :oeqPrimaryB2:clean
~$ ./gradlew :oeqPrimaryB2:buildB2
```
The war is placed in (cloned repo)/oeqPrimaryB2/build/libs/

## Building the primary web service
```
~$ ./gradlew :oeqPrimaryWS:clean
~$ ./gradlew :oeqPrimaryWS:buildWs
```
The jar is placed in (cloned repo)/oeqPrimaryWS/build/libs/

## Building the audit building block
For now, this is just a skeleton building block for testing.

Eventually it may be a helper building block to audit the Blackboard / openEQUELLA integration.

```
~$ ./gradlew :oeqAuditB2:clean
~$ ./gradlew :oeqAuditB2:buildB2
```
The war is placed in (cloned repo)/oeqAuditB2/build/libs/

## Building the linkFixer building block
TODO

## Building the gbFixer building block
TODO

## Check (and fix) dependency issues
```
~$ ./gradlew generateGradleLintReport
~$ ./gradlew fixGradleLint
```