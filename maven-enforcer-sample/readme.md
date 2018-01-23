# Maven major version rule enforcer - Sample

This demo fails to build as it has a dependency build with a 1.7 jdk when 1.6 is being enforced.

See [sample pom](pom.xml) for a sample configuration. 

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
					<groupId>com.allfinanz</groupId>
					<artifactId>maven-enforcer</artifactId>
					<version>0.0.1-SNAPSHOT</version>
				</dependency>
			</dependencies>
			<executions>
				<execution>
					<id>enforce</id>
					<configuration>
						<rules>
							<majorVersionRule
								implementation="com.allfinanz.maven.enforcer.MajorVersionRule">
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

## Sample

This sample demonstrates a fail.

```
Invalid major version in [D:\.m2\repository\org\jgroups\jgroups\3.6.13.Final\jgroups-3.6.13.Final.jar:org/jgroups/Address.class], found major version 51 but expected <= 50
[INFO] ------------------------------------------------------------------------
[INFO] BUILD FAILURE
[INFO] ------------------------------------------------------------------------
```
 