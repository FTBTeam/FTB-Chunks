package com.feed_the_beast.mods.ftbchunks.client.map;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

/**
 * @author LatvianModder
 */
public class MapIOUtils
{
	public interface IOCallback<T>
	{
		void callback(T stream) throws IOException;
	}

	public static void write(OutputStream stream, IOCallback<DataOutputStream> callback)
	{
		try (DataOutputStream s = new DataOutputStream(new BufferedOutputStream(new DeflaterOutputStream(stream))))
		{
			callback.callback(s);
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}

	public static void write(Path path, IOCallback<DataOutputStream> callback)
	{
		try (OutputStream fos = Files.newOutputStream(path))
		{
			write(fos, callback);
		}
		catch (Exception ex)
		{
		}
	}

	public static boolean read(Path path, IOCallback<DataInputStream> callback)
	{
		if (Files.notExists(path) || !Files.isReadable(path))
		{
			return false;
		}

		try (DataInputStream in = new DataInputStream(new BufferedInputStream(new InflaterInputStream(Files.newInputStream(path)))))
		{
			callback.callback(in);
			return true;
		}
		catch (Exception ex)
		{
		}

		try (DataInputStream in = new DataInputStream(new BufferedInputStream(new GZIPInputStream(Files.newInputStream(path)))))
		{
			callback.callback(in);
			return false;
		}
		catch (Exception ex)
		{
		}

		return false;
	}
}