# Maven major version rule enforcer

Short circuit the build when direct dependencies are found with invalid major versions. 

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

 