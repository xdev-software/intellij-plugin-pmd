/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.cache.internal;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.Checksum;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.xdev.pmd.external.org.apache.shiro.lang.util.SoftHashMap;


/**
 * Fork/Override of upstream to fix some performance problems. See IMPROVED comments for details
 * <p>
 * Based on PMD 7.17.0
 */
@SuppressWarnings("all")
public class ZipFileFingerprinter implements ClasspathEntryFingerprinter
{
	// IMPROVED
	private static final Map<String, UrlCachedPayload> FILE_CRC_CHECKSUMS_CACHE =
		Collections.synchronizedMap(new SoftHashMap<>());
	
	
	record UrlCachedPayload(
		long length,
		long lastModified,
		byte[] checksumUpdateData,
		int checksumUpdateEntries)
	{
		boolean isValid(final long length, final long lastModified)
		{
			return this.length() == length
				&& this.lastModified() == lastModified;
		}
		
		void updateCheckSum(final Checksum checksum)
		{
			if(this.checksumUpdateEntries > 0)
			{
				checksum.update(this.checksumUpdateData, 0, this.checksumUpdateEntries);
			}
		}
	}
	
	
	private static final Logger LOG = LoggerFactory.getLogger(ZipFileFingerprinter.class);
	
	private static final Set<String> SUPPORTED_EXTENSIONS = Set.of("jar", "zip");
	private static final Set<String> SUPPORTED_ENTRY_EXTENSIONS = Set.of("class");
	
	private static final Comparator<ZipEntry> FILE_NAME_COMPARATOR = Comparator.comparing(ZipEntry::getName);
	
	@Override
	public boolean appliesTo(final String fileExtension)
	{
		return SUPPORTED_EXTENSIONS.contains(fileExtension);
	}
	
	@Override
	public void fingerprint(final URL entry, final Checksum checksum) throws IOException
	{
		try
		{
			// IMPROVEMENT: USE CACHE IF POSSIBLE
			final String cacheKey = entry.toString();
			UrlCachedPayload cache = FILE_CRC_CHECKSUMS_CACHE.get(cacheKey);
			
			final File file = new File(entry.toURI());
			final long length = file.length();
			final long lastModified = file.lastModified();
			
			if(cache != null && cache.isValid(length, lastModified))
			{
				cache.updateCheckSum(checksum);
				return;
			}
			
			try(final ZipFile zip = new ZipFile(file))
			{
				final List<ZipEntry> meaningfulEntries = this.getMeaningfulEntries(zip);
				
				/*
				 *  Make sure the order of entries in the zip do not matter.
				 *  Duplicates are technically possible, but shouldn't exist in classpath entries
				 */
				meaningfulEntries.sort(FILE_NAME_COMPARATOR);
				
				// IMPROVEMENT: DO NOT UPDATE CHECKSUM FOR EACH SINGLE FILE
				// AGGREGATE FILES AND UPDATE IT ONE TIME ONLY
				final ByteBuffer buffer = ByteBuffer.allocate(4 * meaningfulEntries.size()); // Size of an int
				int counter = 0;
				for(final ZipEntry zipEntry : meaningfulEntries)
				{
					/*
					 * The CRC actually uses 4 bytes, but as it's unsigned Java uses a long…
					 * the cast changes the sign, but not the underlying byte values themselves
					 */
					buffer.putInt(4 * counter, (int)zipEntry.getCrc());
					counter++;
				}
				
				cache = new UrlCachedPayload(
					length,
					lastModified,
					buffer.array(),
					meaningfulEntries.size());
				FILE_CRC_CHECKSUMS_CACHE.put(cacheKey, cache);
				
				cache.updateCheckSum(checksum);
			}
		}
		catch(final FileNotFoundException | NoSuchFileException ignored)
		{
			LOG.warn("Classpath entry {} doesn't exist, ignoring it", entry);
		}
		catch(final URISyntaxException e)
		{
			// Should never happen?
			LOG.warn("Malformed classpath entry doesn't refer to zip in filesystem.", e);
		}
	}
	
	private List<ZipEntry> getMeaningfulEntries(final ZipFile zip)
	{
		final List<ZipEntry> meaningfulEntries = new ArrayList<>();
		final Enumeration<? extends ZipEntry> entries = zip.entries();
		
		while(entries.hasMoreElements())
		{
			final ZipEntry zipEntry = entries.nextElement();
			
			final String fileExtension = this.getFileExtension(zipEntry);
			if(fileExtension != null && SUPPORTED_ENTRY_EXTENSIONS.contains(fileExtension))
			{
				meaningfulEntries.add(zipEntry);
			}
		}
		
		return meaningfulEntries;
	}
	
	private String getFileExtension(final ZipEntry entry)
	{
		if(entry.isDirectory())
		{
			return null;
		}
		
		final String file = entry.getName();
		final int lastDot = file.lastIndexOf('.');
		
		if(lastDot == -1)
		{
			return "";
		}
		
		return file.substring(lastDot + 1);
	}
}
