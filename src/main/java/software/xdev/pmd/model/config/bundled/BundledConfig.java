package software.xdev.pmd.model.config.bundled;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.intellij.openapi.application.ApplicationManager;

import software.xdev.pmd.langversion.LanguageVersionResolverService;
import software.xdev.pmd.model.config.ConfigurationLocation;
import software.xdev.pmd.model.config.ConfigurationType;


public final class BundledConfig
{
	private static final String BUNDLED_LOCATION = "(bundled)";
	
	private final int sortOrder;
	private final String id;
	
	private final String location;
	
	private final String description;
	
	private BundledConfig(
		final int sortOrder,
		@NotNull final String id,
		@NotNull final String description)
	{
		this.sortOrder = sortOrder;
		this.id = id;
		this.location = BUNDLED_LOCATION;
		this.description = description;
	}
	
	public int getSortOrder()
	{
		return this.sortOrder;
	}
	
	public String getId()
	{
		return this.id;
	}
	
	@NotNull
	public String getLocation()
	{
		return this.location;
	}
	
	@NotNull
	public String getDescription()
	{
		return this.description;
	}
	
	public boolean matches(@NotNull final ConfigurationLocation configurationLocation)
	{
		return configurationLocation.getType() == ConfigurationType.BUNDLED
			&& Objects.equals(configurationLocation.getLocation(), this.location)
			&& Objects.equals(configurationLocation.getDescription(), this.description);
	}
	
	private static final AtomicInteger UNKNOWN_COUNTER = new AtomicInteger(1000);
	
	@NotNull
	public static BundledConfig createUnknownDummy(@NotNull final String id)
	{
		return new BundledConfig(UNKNOWN_COUNTER.getAndIncrement(), id, "UNKNOWN: " + id);
	}
	
	@Nullable
	public static BundledConfig fromId(@NotNull final String id)
	{
		initAllIfRequired();
		return all.get(id);
	}
	
	public static Collection<BundledConfig> getAllBundledConfigs()
	{
		initAllIfRequired();
		return all.values();
	}
	
	@SuppressWarnings("checkstyle:IllegalIdentifierName")
	private static void initAllIfRequired()
	{
		if(all != null)
		{
			return;
		}
		
		final AtomicInteger counter = new AtomicInteger(0);
		all = ApplicationManager.getApplication()
			.getService(LanguageVersionResolverService.class)
			.supportedLanguageIds()
			.stream()
			.flatMap(langId -> PMDBuiltInRulesFinder.find(langId)
				.stream()
				.map(ruleSet -> Map.entry(langId, ruleSet)))
			.map(e -> new BundledConfig(
				counter.getAndIncrement(),
				e.getKey() + "-" + e.getValue(),
				e.getKey() + "-" + Arrays.stream(e.getValue().split("/"))
					.reduce((l, r) -> r)
					.flatMap(fileName -> Arrays.stream(fileName.split("\\.")).findFirst())
					.orElse(e.getValue())
			))
			.collect(Collectors.toMap(
				BundledConfig::getId,
				Function.identity(),
				(l, r) -> r,
				LinkedHashMap::new));
	}
	
	private static LinkedHashMap<String, BundledConfig> all;
}
