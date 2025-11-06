package software.xdev.pmd.analysis;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.PsiFile;

import net.sourceforge.pmd.lang.LanguageVersion;
import net.sourceforge.pmd.lang.document.FileId;
import net.sourceforge.pmd.lang.document.TextFile;
import net.sourceforge.pmd.lang.document.TextFileContent;


class IDETextFile implements TextFile
{
	private static final Logger LOG = Logger.getInstance(IDETextFile.class);
	private final LanguageVersion languageVersion;
	private final PsiFile psiFile;
	
	private FileId fileId;
	
	public IDETextFile(final LanguageVersion languageVersion, final PsiFile psiFile)
	{
		this.languageVersion = languageVersion;
		this.psiFile = psiFile;
	}
	
	@Override
	public LanguageVersion getLanguageVersion()
	{
		return this.languageVersion;
	}
	
	@Override
	public FileId getFileId()
	{
		this.initFileIdIfRequired();
		return this.fileId;
	}
	
	public FileId getFileIdIfPresent()
	{
		return this.fileId;
	}
	
	public boolean hasFileId()
	{
		return this.getFileIdIfPresent() != null;
	}
	
	private void initFileIdIfRequired()
	{
		if(this.fileId == null)
		{
			this.initFileId();
		}
	}
	
	private synchronized void initFileId()
	{
		if(this.fileId == null)
		{
			this.fileId = this.calculateFileId();
		}
	}
	
	@SuppressWarnings("PMD.PreserveStackTrace")
	private FileId calculateFileId()
	{
		try
		{
			return FileId.fromPath(this.psiFile.getVirtualFile().toNioPath());
		}
		catch(final Exception ex)
		{
			// Sometimes files are not physically present on the disk and are just available in memory
			LOG.debug("Failed to get NioPath for file " + this.psiFile + ". Falling back to URI", ex);
			try
			{
				return FileId.fromURI(this.psiFile.getVirtualFile().getUrl());
			}
			catch(final Exception ex2)
			{
				LOG.info("Failed to get URI for file " + this.psiFile + ". Falling back to temp file", ex2);
				// FiledId.INVALID is not working as it results in crashes when trying to parse the file-path
				// -> Create a temporary file and use that instead
				try
				{
					return FileId.fromPath(Files.createTempFile("intellij-pmd", "tmp"));
				}
				catch(final IOException e)
				{
					throw new UncheckedIOException(e);
				}
			}
		}
	}
	
	@Override
	public TextFileContent readContents()
	{
		final Application application = ApplicationManager.getApplication();
		final Computable<TextFileContent> action = () -> TextFileContent.fromCharSeq(this.psiFile.getText());
		if(application.isReadAccessAllowed())
		{
			return action.compute();
		}
		return application.runReadAction(action);
	}
	
	@Override
	public void close()
	{
		// Nothing
	}
	
	public PsiFile getPsiFile()
	{
		return this.psiFile;
	}
}
