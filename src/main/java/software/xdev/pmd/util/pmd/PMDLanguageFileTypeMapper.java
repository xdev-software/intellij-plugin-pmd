package software.xdev.pmd.util.pmd;

import java.util.Collections;
import java.util.Map;

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeRegistry;
import com.intellij.openapi.fileTypes.UnknownFileType;

import net.sourceforge.pmd.lang.Language;
import software.xdev.pmd.external.org.apache.shiro.lang.util.SoftHashMap;


public class PMDLanguageFileTypeMapper
{
	private final Map<Language, FileType> cache = Collections.synchronizedMap(new SoftHashMap<>());
	
	public FileType get(final Language language)
	{
		final FileTypeRegistry fileTypeRegistry = FileTypeRegistry.getInstance();
		return this.cache.computeIfAbsent(
			language, lang -> lang.getExtensions()
				.stream()
				.map(fileTypeRegistry::getFileTypeByExtension)
				.filter(ft -> ft != UnknownFileType.INSTANCE)
				.findFirst()
				.orElse(UnknownFileType.INSTANCE));
	}
}
