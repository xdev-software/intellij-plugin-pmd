package software.xdev.pmd.util;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.jetbrains.annotations.NotNull;


public final class Streams
{
	private static final int BUFFER_SIZE = 4096;
	
	private Streams()
	{
	}
	
	public static InputStream inMemoryCopyOf(@NotNull final InputStream sourceStream) throws IOException
	{
		try(final BufferedInputStream bis = new BufferedInputStream(sourceStream))
		{
			return new ByteArrayInputStream(readContentOf(bis));
		}
	}
	
	public static byte[] readContentOf(final InputStream source) throws IOException
	{
		final ByteArrayOutputStream destination = new ByteArrayOutputStream(BUFFER_SIZE);
		final byte[] readBuffer = new byte[BUFFER_SIZE];
		int count;
		while((count = source.read(readBuffer, 0, BUFFER_SIZE)) != -1)
		{
			destination.write(readBuffer, 0, count);
		}
		return destination.toByteArray();
	}
}
