/*
 * Copyright (c) 2021-2021. Bernard Bou.
 */

package org.oewntk.wndb.out;

import org.oewntk.model.CoreModel;
import org.oewntk.model.Model;

import java.io.*;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Main class that generates the serialized synsetId to offset map
 *
 * @author Bernard Bou
 * @see "https://wordnet.princeton.edu/documentation/wndb5wn"
 */
public class OffsetSerializer implements Consumer<Model>
{
	static final String FILE_OFFSET_MAP = "offsets.ser";

	private final File outDir;

	private final int flags;

	private final PrintStream ps;

	/**
	 * Constructor
	 *
	 * @param outDir output directory
	 * @param flags  flags
	 * @param ps     log print stream
	 */
	public OffsetSerializer(final File outDir, final int flags, final PrintStream ps)
	{
		this.outDir = outDir;
		this.flags = flags;
		this.ps = ps;
	}

	@Override
	public void accept(final Model model)
	{
		try
		{
			grind(model);
		}
		catch (IOException e)
		{
			e.printStackTrace(Tracing.psErr);
		}
	}

	/**
	 * Grind
	 *
	 * @param model model
	 * @throws IOException io exception
	 */
	public void grind(final CoreModel model) throws IOException
	{
		// Model
		ps.printf("[Model] %s%n", model.getSource());

		// Output
		if (!outDir.exists())
		{
			//noinspection ResultOfMethodCallIgnored
			outDir.mkdirs();
		}

		// Compute synset offsets
		Map<String, Long> offsets = new GrindOffsets(model.getLexesByLemma(), model.getSynsetsById(), model.getSensesById(), flags).compute();

		// Serialize offsets
		serializeOffsets(offsets, new File(outDir, FILE_OFFSET_MAP));
	}

	public static void serializeOffsets(final Map<String, Long> offsets, final File file) throws IOException
	{
		try (OutputStream os = new FileOutputStream(file))
		{
			serialize(os, offsets);
		}
	}

	private static void serialize(final OutputStream os, final Object object) throws IOException
	{
		try (ObjectOutputStream oos = new ObjectOutputStream(os))
		{
			oos.writeObject(object);
		}
	}

	public static Map<String, Long> deSerializedOffsets(File file) throws IOException, ClassNotFoundException
	{
		try (InputStream is = new FileInputStream(file))
		{
			return (Map<String, Long>) deSerialize(is);
		}
	}

	private static Object deSerialize(final InputStream is) throws IOException, ClassNotFoundException
	{
		try (ObjectInputStream ois = new ObjectInputStream(is))
		{
			return ois.readObject();
		}
	}
}
