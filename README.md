# openEQUELLA Blackboard Integration

Information about the openEQUELLA community and supporting documentation can be found at https://equella.github.io/

Versions are repo-wide (ie all building blocks / web services will have the same version for a given build).  Versioning will be loosely tied to SemVer.

Version 1.X.Y will support Blackboard v3200 - v3300
Version 2.W.V will support Blackboard v3400+

You can override the default Bb API version by adding `-PbbLearnVersion=XYZ` to the `./gradlew` invocation.

## Building the primary building block
```bash
~$ ./gradlew :oeqPrimaryB2:clean :oeqPrimaryB2:buildB2
```
The war is placed in (cloned repo)/oeqPrimaryB2/build/libs/

## Building the primary web service
```bash
~$ ./gradlew :oeqPrimaryWS:clean :oeqPrimaryWS:buildWS
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

## Logging
### Self / Managed Hosting
Logs to bb-services.txt.  The Building Block Settings page will list the log file as well.

### SaaS
Logs to Kibana.  Search for `oeqInteg`.  The default graph won't provide the rows, so hover over the bottom rectangle on the left-hand side of the Kibana Learn interface (should be green) to Add Panel.  Select Table and 'selected' queries.  
