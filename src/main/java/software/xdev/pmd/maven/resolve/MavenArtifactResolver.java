package software.xdev.pmd.maven.resolve;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import com.google.common.base.Suppliers;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;

import software.xdev.pmd.config.state.application.ApplicationConfigurationState;
import software.xdev.pmd.maven.MavenId;
import software.xdev.pmd.maven.resolve.mirror.MavenMirrorUrlResolver;
import software.xdev.pmd.maven.resolve.mirror.MavenMirrorUrlResolverService;


public class MavenArtifactResolver implements Disposable
{
	private static final Logger LOG = Logger.getInstance(MavenArtifactResolver.class);
	
	private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(30);
	private static final Duration DOWNLOAD_TIMEOUT = Duration.ofMinutes(3);
	
	private static final int DOWNLOAD_TRIES = 2;
	
	private final HttpClient httpClient;
	private final List<Supplier<Optional<String>>> mavenBaseUrlSupplier;
	private final Supplier<Path> m2RootSupplier;
	
	public MavenArtifactResolver(final Project project)
	{
		this.httpClient = HttpClient.newBuilder()
			.followRedirects(HttpClient.Redirect.NORMAL)
			.connectTimeout(CONNECT_TIMEOUT)
			.build();
		
		this.mavenBaseUrlSupplier = List.of(
			Suppliers.memoize(() -> project.getService(MavenMirrorUrlResolverService.class).resolve(project)),
			() -> Optional.of(MavenMirrorUrlResolver.DEFAULT_CENTRAL_REPOSITORY_URL)
		);
		
		this.m2RootSupplier = Suppliers.memoize(MavenArtifactResolver::determineM2Root);
	}
	
	public Path ensureResolved(final MavenId mavenId)
	{
		final String groupIdWithSlash = mavenId.groupId().replace('.', '/');
		final String jarFileName = mavenId.artifactId() + "-" + mavenId.version() + ".jar";
		final Path resolvedPath = this.m2RootSupplier.get()
			.resolve(groupIdWithSlash)
			.resolve(mavenId.artifactId())
			.resolve(mavenId.version())
			.resolve(jarFileName);
		
		if(!Files.exists(resolvedPath))
		{
			this.download(mavenId, groupIdWithSlash, jarFileName, resolvedPath);
		}
		
		return resolvedPath;
	}
	
	private void download(
		final MavenId mavenId,
		final String groupIdWithSlash,
		final String jarFileName,
		final Path resolvedPath)
	{
		final List<Exception> downloadExceptions = new ArrayList<>();
		if(this.baseUrlsForDownload().stream()
			.map(Supplier::get)
			.filter(Optional::isPresent)
			.map(Optional::orElseThrow)
			.noneMatch(baseUrl -> {
				final String downloadUrl = baseUrl
					+ (baseUrl.endsWith("/") ? "" : "/")
					+ groupIdWithSlash
					+ "/" + mavenId.artifactId()
					+ "/" + mavenId.version()
					+ "/" + jarFileName;
				for(int i = 1; i <= DOWNLOAD_TRIES; i++)
				{
					try
					{
						Files.createDirectories(resolvedPath.getParent());
						this.downloadTo(downloadUrl, resolvedPath);
						return true;
					}
					catch(final Exception e)
					{
						LOG.debug(
							"Download attempt #" + i + " " + mavenId + " from " + downloadUrl + " failed",
							e);
						downloadExceptions.add(e);
					}
					
					try
					{
						Thread.sleep(1000);
					}
					catch(final InterruptedException e)
					{
						Thread.currentThread().interrupt();
					}
				}
				return false;
			}))
		{
			final IllegalStateException ex = new IllegalStateException("Failed to download " + mavenId);
			downloadExceptions.forEach(ex::addSuppressed);
			throw ex;
		}
	}
	
	private List<Supplier<Optional<String>>> baseUrlsForDownload()
	{
		return Optional.ofNullable(ApplicationManager.getApplication())
			.map(application -> application.getService(ApplicationConfigurationState.class))
			.map(ApplicationConfigurationState::getArtifactRepositoryBaseUrlOverride)
			.filter(url -> !url.isBlank())
			.filter(url -> {
				try
				{
					final String scheme = new URI(url).getScheme();
					return "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
				}
				catch(final URISyntaxException ignored)
				{
					return false;
				}
			})
			.map(url -> List.<Supplier<Optional<String>>>of(() -> Optional.of(url)))
			.orElse(this.mavenBaseUrlSupplier);
	}
	
	private void downloadTo(final String url, final Path target) throws IOException
	{
		LOG.debug("Downloading " + url + " to " + target);
		
		final HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create(url))
			.timeout(DOWNLOAD_TIMEOUT)
			.GET()
			.build();
		final Path tmp = Files.createTempFile(target.getParent(), ".download-", ".part");
		try
		{
			final HttpResponse<Path> response = this.httpClient.send(
				request,
				HttpResponse.BodyHandlers.ofFile(
					tmp));
			if(response.statusCode() != 200)
			{
				throw new IOException("Encountered HTTP " + response.statusCode() + " while downloading " + url);
			}
			Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
		}
		catch(final InterruptedException e)
		{
			Thread.currentThread().interrupt();
			throw new IOException("Download interrupted: " + url, e);
		}
		finally
		{
			Files.deleteIfExists(tmp);
		}
	}
	
	private static Path determineM2Root()
	{
		final String envMavenUserHome = System.getenv("MAVEN_USER_HOME");
		if(envMavenUserHome != null)
		{
			return Path.of(envMavenUserHome).resolve("repository");
		}
		
		return Path.of(System.getProperty("user.home"), ".m2", "repository");
	}
	
	@Override
	public void dispose()
	{
		this.httpClient.close();
	}
}
