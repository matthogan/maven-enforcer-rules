# Maven major version rule enforcer

Short circuit the maven build when direct dependencies are found with invalid major versions. 

## Projects

* [maven-enforcer-rules](maven-enforcer-rules) - Rules for the enforcer plugin.
* [maven-enforcer-sample](maven-enforcer-sample) - Usage sample that fails when it discovers a java 1.7 maven dependency.

## Sample output

```
[WARNING] Not enforcing jar D:\.m2\repository\javax\servlet\javax.servlet-api\3.0.1\javax.servlet-api-3.0.1.jar at provided scope
[WARNING] Enforcing jar D:\.m2\repository\org\jgroups\jgroups\3.6.13.Final\jgroups-3.6.13.Final.jar
[WARNING] Rule 0: com.codejago.maven.enforcer.MajorVersionRule failed with message:
Invalid major version in [D:\.m2\repository\org\jgroups\jgroups\3.6.13.Final\jgroups-3.6.13.Final.jar:org/jgroups/Address.class], found major version 51 but expected <= 50
[INFO] ------------------------------------------------------------------------
[INFO] BUILD FAILURE
[INFO] ------------------------------------------------------------------------
```

## Sample plugin configuration

```
<build>
	<plugins>
		<plugin>
			<groupId>org.apache.maven.plugins</groupId>
			<artifactId>maven-enforcer-plugin</artifactId>
			<version>3.0.0-M1</version>
			<dependencies>
				<dependency>
					<groupId>com.codejago</groupId>
					<artifactId>maven-enforcer-rules</artifactId>
					<version>0.0.1-SNAPSHOT</version>
				</dependency>
			</dependencies>
			<executions>
				<execution>
					<id>enforce</id>
					<configuration>
						<rules>
							<majorVersionRule
								implementation="com.codejago.maven.enforcer.MajorVersionRule">
								<!-- Maximum version allowed for any direct dependency -->
								<version>1.6</version>
								<!-- fail out or keep going, i.e. just log them -->
								<failOnInvalidVersion>true</failOnInvalidVersion>
								<!-- log all checked jars to the console -->
								<logAllJars>true</logAllJars>
								<!-- Only check these scopes, missing is none -->
								<scopes>compile</scopes>
							</majorVersionRule>
						</rules>
					</configuration>
					<goals>
						<goal>enforce</goal>
					</goals>
				</execution>
			</executions>
		</plugin>
	</plugins>
</build>
```

 
