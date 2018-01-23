/**
 * 
 */
package com.codejago.maven.enforcer;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.enforcer.rule.api.EnforcerRule;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;

/**
 * Kills the build when a direct dependency is found with an invalid major
 * version Doesn't work on the managed dependencies.
 */
public class MajorVersionRule implements EnforcerRule {

	/** 50 => 1.6, 51 => 1.7, 52 -> 1.8 */
	private String version = "1.6";
	/** Exception and fail out or log and continue */
	private boolean failOnInvalidVersion = true;
	/** Log each jar being processed */
	private boolean logAllJars = false;
	/** Scopes to check */
	private String scopes = null;
	/**
	 * This should enable using java.version type properties already in the pom
	 * without the need for maintenance when upgrading jdks.
	 */
	private Map<String, Integer> versions = new HashMap<String, Integer>() {
		private static final long serialVersionUID = 1L;
		{
			put("1.2", 46);
			put("1.3", 47);
			put("1.4", 48);
			put("1.5", 49);
			put("1.6", 50);
			put("1.7", 51);
			put("1.8", 52);
			put("1.9", 53);
		}
	};

	/**
	 * Entry point {@link EnforcerRule#execute(EnforcerRuleHelper)}
	 * 
	 * @param helper
	 *            - access the maven model
	 * @throws EnforcerRuleException
	 *             - short circuits the build
	 */
	public void execute(EnforcerRuleHelper helper) throws EnforcerRuleException {
		Log log = helper.getLog();
		long time = System.currentTimeMillis();
		Integer majorVersion = versions.get(version);
		if (majorVersion == null) {
			throw new EnforcerRuleException(String.format("Unknown java version %s", version));
		}
		log.warn(String.format("Enforcing dependencies major version %s/%d", version, majorVersion));
		try {
			// get the various expressions out of the helper.
			MavenProject project = (MavenProject) helper.evaluate("${project}");
			for (Object o : project.getArtifacts()) {
				Artifact af = (Artifact) o;
				if (af.getFile() == null) {
					continue;
				}
				if (scopes == null || scopes.trim().length() == 0 || scopes.contains(af.getScope())) {
					process(helper, af.getFile(), majorVersion);
				} else if (logAllJars) {
					log.warn(String.format("Not enforcing jar %s at %s scope", af.getFile().getAbsolutePath(),
							af.getScope()));
				}
			}
		} catch (ExpressionEvaluationException e) {
			throw new EnforcerRuleException("Unable to lookup an expression " + e.getLocalizedMessage(), e);
		}
		log.warn(String.format("Enforced dependencies major version %s/%d in %d millis", version, majorVersion,
				System.currentTimeMillis() - time));
	}

	private void process(EnforcerRuleHelper helper, File f, int majorVersion) throws EnforcerRuleException {
		Log log = helper.getLog();
		JarFile jar = null;
		try {
			jar = new JarFile(f);
			if (logAllJars) {
				log.warn(String.format("Enforcing jar %s", f.getAbsolutePath()));
			}
			for (Enumeration<JarEntry> jes = jar.entries(); jes.hasMoreElements();) {
				JarEntry je = jes.nextElement();
				if (je.getName().endsWith(".class")) {
					DataInputStream in = new DataInputStream(jar.getInputStream(je));
					if (in.available() == 0) {
						log.debug(String.format("%s:%s no data available to read", f.getAbsolutePath(), je.getName()));
						continue;
					}
					int magic = in.readInt();
					if (magic != 0xCAFEBABE) {
						log.debug(String.format("%s:%s is an invalid class", f.getAbsolutePath(), je.getName()));
						continue;
					}
					int minor = in.readUnsignedShort();
					log.debug(String.format("%s:%s minor version found %d", f.getAbsolutePath(), je.getName(), minor));
					int major = in.readUnsignedShort();
					log.debug(String.format("%s:%s major version found %d", f.getAbsolutePath(), je.getName(), major));
					if (major > majorVersion) {
						String error = String.format(
								"Invalid major version in [%s:%s], found major version %d but expected <= %d",
								f.getAbsolutePath(), je.getName(), major, majorVersion);
						if (failOnInvalidVersion) {
							throw new EnforcerRuleException(error);
						} else {
							log.error(error);
						}
					}
					close(helper, in);
					break;
				}
			}
			close(helper, jar);
			jar = null;
		} catch (IOException e) {
			log.error(String.format("Failed to load jar file %s %s", f.getAbsolutePath(), e.getMessage()), e);
		} finally {
			close(helper, jar);
		}
	}

	private void close(EnforcerRuleHelper helper, Closeable cl) {
		if (cl != null) {
			try {
				cl.close();
			} catch (IOException e) {
				Log log = helper.getLog();
				log.error(String.format("Failed to close a resource %s %s", cl.toString(), e.getMessage()), e);
			}
		}
	}

	/**
	 * If your rule is cacheable, you must return a unique id when parameters or
	 * conditions change that would cause the result to be different. Multiple
	 * cached results are stored based on their id.
	 * 
	 * The easiest way to do this is to return a hash computed from the values
	 * of your parameters.
	 * 
	 * If your rule is not cacheable, then the result here is not important, you
	 * may return anything.
	 */
	public String getCacheId() {
		// no hash on boolean...only parameter so no hash is needed.
		return Integer.toString(hashCode());
	}

	/**
	 * This tells the system if the results are cacheable at all. Keep in mind
	 * that during forked builds and other things, a given rule may be executed
	 * more than once for the same project. This means that even things that
	 * change from project to project may still be cacheable in certain
	 * instances.
	 */
	public boolean isCacheable() {
		return false;
	}

	/**
	 * If the rule is cacheable and the same id is found in the cache, the
	 * stored results are passed to this method to allow double checking of the
	 * results. Most of the time this can be done by generating unique ids, but
	 * sometimes the results of objects returned by the helper need to be
	 * queried. You may for example, store certain objects in your rule and then
	 * query them later.
	 */
	public boolean isResultValid(EnforcerRule arg0) {
		return false;
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (failOnInvalidVersion ? 1231 : 1237);
		result = prime * result + (logAllJars ? 1231 : 1237);
		result = prime * result + ((scopes == null) ? 0 : scopes.hashCode());
		result = prime * result + ((version == null) ? 0 : version.hashCode());
		result = prime * result + ((versions == null) ? 0 : versions.hashCode());
		return result;
	}

	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MajorVersionRule other = (MajorVersionRule) obj;
		if (failOnInvalidVersion != other.failOnInvalidVersion)
			return false;
		if (logAllJars != other.logAllJars)
			return false;
		if (scopes == null) {
			if (other.scopes != null)
				return false;
		} else if (!scopes.equals(other.scopes))
			return false;
		if (version == null) {
			if (other.version != null)
				return false;
		} else if (!version.equals(other.version))
			return false;
		if (versions == null) {
			if (other.versions != null)
				return false;
		} else if (!versions.equals(other.versions))
			return false;
		return true;
	}

}
