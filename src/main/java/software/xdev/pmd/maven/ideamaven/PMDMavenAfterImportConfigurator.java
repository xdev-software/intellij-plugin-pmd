package software.xdev.pmd.maven.ideamaven;

import java.nio.file.Paths;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.jdom.Element;
import org.jetbrains.idea.maven.importing.MavenAfterImportConfigurator;
import org.jetbrains.idea.maven.importing.MavenWorkspaceConfigurator;
import org.jetbrains.idea.maven.model.MavenId;
import org.jetbrains.idea.maven.model.MavenPlugin;
import org.jetbrains.idea.maven.model.MavenProfile;
import org.jetbrains.idea.maven.project.MavenProject;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;

import software.xdev.pmd.config.PluginConfiguration;
import software.xdev.pmd.config.PluginConfigurationBuilder;
import software.xdev.pmd.config.PluginConfigurationManager;
import software.xdev.pmd.model.config.rulesetlocation.ConfigurationLocation;
import software.xdev.pmd.model.config.rulesetlocation.bundled.BundledConfigurationLocation;
import software.xdev.pmd.model.config.rulesetlocation.file.FileConfigurationLocation;
import software.xdev.pmd.model.config.rulesetlocation.file.RelativeFileConfigurationLocation;
import software.xdev.pmd.model.config.thirdpartycplocation.ThirdPartyCPLocation;
import software.xdev.pmd.model.config.thirdpartycplocation.maven.MavenThirdPartyCPLocationFactory;
import software.xdev.pmd.model.scope.ScanScope;


@SuppressWarnings("UnstableApiUsage")
public class PMDMavenAfterImportConfigurator implements MavenAfterImportConfigurator
{
	private static final Logger LOG = Logger.getInstance(PMDMavenAfterImportConfigurator.class);
	
	private static final List<MavenId> PMD_PLUGIN_MAVEN_IDS = List.of(
		new MavenId("org.apache.maven.plugins", "maven-pmd-plugin", null));
	public static final String PRIMARY_MAVEN_PROFILE_NAME = "pmd";
	
	@Override
	public void afterImport(final MavenAfterImportConfigurator.Context context)
	{
		final Project project = context.getProject();
		final PluginConfigurationManager configManager = project.getService(PluginConfigurationManager.class);
		final PluginConfiguration currentConfig = configManager.getCurrent();
		
		if(!currentConfig.importSettingsFromMaven())
		{
			LOG.debug("Abort - Maven import disabled");
			return;
		}
		
		final Optional<MavenProjectAndPlugin> optMavenProjectAndPlugin = findMavenProjectAndPlugin(context);
		if(optMavenProjectAndPlugin.isEmpty())
		{
			LOG.debug("Abort - Unable to find Maven Project with PMD plugin");
			return;
		}
		
		final MavenPlugin mavenPlugin = optMavenProjectAndPlugin.orElseThrow().plugin();
		
		final Element configElement = mavenPlugin.getConfigurationElement();
		if(configElement == null)
		{
			LOG.debug("Abort - Maven Configuration Element is missing");
			return;
		}
		
		final PluginConfigurationBuilder builder = new PluginConfigurationBuilder(currentConfig);
		
		this.configureFromMaven(project, mavenPlugin, configElement, currentConfig, builder);
		
		final PluginConfiguration newConfig = builder.build();
		if(currentConfig.isIdentical(newConfig))
		{
			LOG.debug("Abort - Nothing has changed");
			return;
		}
		
		LOG.debug("Applying new configuration");
		configManager.setCurrent(newConfig);
	}
	
	private void configureFromMaven(
		final Project project,
		final MavenPlugin mavenPlugin,
		final Element configElement,
		final PluginConfiguration currentConfig,
		final PluginConfigurationBuilder builder)
	{
		this.configureScanScope(configElement, builder);
		
		this.configureLocations(project, configElement, currentConfig, builder);
		
		this.configureExclusions(configElement, builder);
		
		this.configureThirdPartyCPLocations(project, mavenPlugin, builder);
	}
	
	private void configureScanScope(final Element configElement, final PluginConfigurationBuilder builder)
	{
		// https://maven.apache.org/plugins/maven-pmd-plugin/pmd-mojo.html#includeTests
		builder.withScanScope(
			Optional.ofNullable(configElement.getChild("includeTests"))
				.map(Element::getText)
				.map(Boolean::parseBoolean)
				.orElse(false)
				? ScanScope.SUPPORTED_ONLY_WITH_TESTS
				: ScanScope.SUPPORTED_ONLY);
	}
	
	private void configureLocations(
		final Project project,
		final Element configElement,
		final PluginConfiguration currentConfig,
		final PluginConfigurationBuilder builder)
	{
		final List<BundledConfigurationLocation> allCurrentlyBundledLocations =
			currentConfig.locations()
				.stream()
				.filter(BundledConfigurationLocation.class::isInstance)
				.map(BundledConfigurationLocation.class::cast)
				.toList();
		
		// https://maven.apache.org/plugins/maven-pmd-plugin/pmd-mojo.html#rulesets
		final List<ConfigurationLocation> mavenLocations = getChildrenTexts(configElement, "rulesets")
			.map(s -> {
				// Example in maven plugin:
				// <ruleset>/category/java/bestpractices.xml</ruleset>
				// Our ids are formatted like this:
				// id="java-category/java/bestpractices.xml"
				if(s.startsWith("/category/"))
				{
					// Example: category/java/bestpractices.xml
					final String relativeCategory = s.substring(1);
					
					return allCurrentlyBundledLocations
						.stream()
						.filter(l -> l.getId().endsWith(relativeCategory))
						.findFirst()
						.orElse(null);
				}
				return this.createFileBasedConfigurationLocation(project, s);
			})
			.filter(Objects::nonNull)
			.toList();
		
		final TreeSet<ConfigurationLocation> newLocations = new TreeSet<>(allCurrentlyBundledLocations);
		newLocations.addAll(mavenLocations);
		builder
			.withLocations(newLocations)
			.withActiveLocationIds(new TreeSet<>(mavenLocations.stream().map(ConfigurationLocation::getId).toList()));
	}
	
	private ConfigurationLocation createFileBasedConfigurationLocation(final Project project, final String s)
	{
		final boolean absolutePath;
		try
		{
			absolutePath = Paths.get(s).isAbsolute();
		}
		catch(final Exception _)
		{
			// Ignore invalid paths
			return null;
		}
		
		final String id = String.valueOf(s.hashCode());
		final ConfigurationLocation configurationLocation = absolutePath
			? new FileConfigurationLocation(project, id)
			: new RelativeFileConfigurationLocation(project, id);
		configurationLocation.setLocation(s);
		configurationLocation.setDescription(s);
		
		return configurationLocation;
	}
	
	private void configureExclusions(final Element configElement, final PluginConfigurationBuilder builder)
	{
		// https://maven.apache.org/plugins/maven-pmd-plugin/pmd-mojo.html#excludes
		builder.withProjectRelativeFileExclusionsRaw(getChildrenTexts(configElement, "excludes")
			// The paths in the plugin are module based
			.map(s -> {
				if(!s.startsWith("**/"))
				{
					return "**/" + s;
				}
				return s;
			})
			.map(GlobToRegex::toRegex)
			.toList());
	}
	
	private void configureThirdPartyCPLocations(
		final Project project,
		final MavenPlugin mavenPlugin,
		final PluginConfigurationBuilder builder)
	{
		final List<MavenId> relevantDeps = mavenPlugin.getDependencies()
			.stream()
			// These modules are already bundled
			.filter(dep -> !"net.sourceforge.pmd".equals(dep.getGroupId()))
			.toList();
		if(relevantDeps.isEmpty())
		{
			builder.withThirdPartyCPLocations(List.of());
			return;
		}
		
		final MavenThirdPartyCPLocationFactory factory = project.getService(MavenThirdPartyCPLocationFactory.class);
		builder.withThirdPartyCPLocations(
			relevantDeps.stream()
				.map(id -> new software.xdev.pmd.maven.MavenId(
					id.getGroupId(),
					id.getArtifactId(),
					id.getVersion()))
				.map(id -> {
					try
					{
						return factory.fromUI(id);
					}
					catch(final Exception ex)
					{
						LOG.debug("Maven 3rd party CP import of " + id + " failed", ex);
						return null;
					}
				})
				.filter(Objects::nonNull)
				.map(ThirdPartyCPLocation.class::cast)
				.toList());
	}
	
	record MavenProjectAndPlugin(
		MavenProject project,
		MavenPlugin plugin
	)
	{
	}
	
	private static Optional<MavenProjectAndPlugin> findMavenProjectAndPlugin(
		final MavenAfterImportConfigurator.Context context)
	{
		return StreamSupport.stream(
				Spliterators.spliteratorUnknownSize(
					context.getMavenProjectsWithModules().iterator(),
					Spliterator.ORDERED),
				false)
			.map(MavenWorkspaceConfigurator.MavenProjectWithModules::getMavenProject)
			.sorted(Comparator.comparing(project -> project.getMavenId().getKey()))
			.map(project -> new MavenProjectAndPlugin(
				project,
				findPMDPlugin(project)
			))
			.filter(mpp -> mpp.plugin() != null)
			.findFirst();
	}
	
	private static MavenPlugin findPMDPlugin(final MavenProject project)
	{
		final List<MavenProfile> profiles = project.getProfiles();
		
		final Map<String, MavenProfile> profilesContainingPmd = profiles.stream()
			.filter(profile -> {
				final String id = profile.getId();
				return id.contains("pmd")
					&& !id.startsWith("no")
					&& !id.startsWith("disable");
			})
			.collect(Collectors.toMap(
				MavenProfile::getId,
				Function.identity(),
				(l, r) -> l,
				LinkedHashMap::new));
		
		// 1. Try to lookup config from profile named "pmd"
		return Optional.ofNullable(profilesContainingPmd.get(PRIMARY_MAVEN_PROFILE_NAME))
			.flatMap(PMDMavenAfterImportConfigurator::findPMDPluginFromProfile)
			// 2. Try any profile containing "pmd"
			.or(() -> profilesContainingPmd.entrySet().stream()
				.filter(e -> !PRIMARY_MAVEN_PROFILE_NAME.equals(e.getKey()))
				.map(Map.Entry::getValue)
				.map(PMDMavenAfterImportConfigurator::findPMDPluginFromProfile)
				.filter(Optional::isPresent)
				.map(Optional::orElseThrow)
				.findFirst()
			)
			// 3. Try direct plugins
			.or(() -> findPMDPluginDirectly(project))
			// 4. Try all other profiles
			.or(() -> profiles.stream()
				.filter(profile -> !profilesContainingPmd.containsKey(profile.getId()))
				.map(PMDMavenAfterImportConfigurator::findPMDPluginFromProfile)
				.filter(Optional::isPresent)
				.map(Optional::orElseThrow)
				.findFirst()
			)
			.orElse(null);
	}
	
	private static Optional<MavenPlugin> findPMDPluginFromProfile(final MavenProfile profile)
	{
		return profile.getPlugins()
			.stream()
			.filter(plugin -> PMD_PLUGIN_MAVEN_IDS.stream().anyMatch(id ->
				Objects.equals(id.getGroupId(), plugin.getGroupId())
					&& Objects.equals(id.getArtifactId(), plugin.getArtifactId())
			))
			.findFirst();
	}
	
	private static Optional<MavenPlugin> findPMDPluginDirectly(final MavenProject project)
	{
		return PMD_PLUGIN_MAVEN_IDS.stream()
			.map(id -> project.findPlugin(id.getGroupId(), id.getArtifactId()))
			.filter(Objects::nonNull)
			.findFirst();
	}
	
	private static Stream<String> getChildrenTexts(final Element configElement, final String target)
	{
		return Optional.ofNullable(configElement.getChild(target))
			.map(Element::getChildren)
			.stream()
			.flatMap(List::stream)
			.map(Element::getText)
			.filter(Objects::nonNull)
			.filter(s -> !s.isBlank());
	}
}
