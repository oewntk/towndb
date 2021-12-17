/*
 * Copyright (c) 2021-2021. Bernard Bou.
 */

package org.oewntk.wndb.out;

import org.oewntk.model.CoreModel;
import org.oewntk.model.Model;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;

/**
 * Main class that generates the serialized synsetId to offset map
 *
 * @author Bernard Bou
 * @see "https://wordnet.princeton.edu/documentation/wndb5wn"
 */
public class OffsetMapper implements Consumer<Model>
{
	static final String FILE_OFFSET_MAP = "offsets.map";

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
	public OffsetMapper(final File outDir, final int flags, final PrintStream ps)
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
		writeOffsets(offsets, new File(outDir, FILE_OFFSET_MAP));
	}

	public static void writeOffsets(final Map<String, Long> offsets, final File file) throws IOException
	{
		try (PrintStream ps = new PrintStream(new FileOutputStream(file), true, StandardCharsets.UTF_8))
		{
			offsets.keySet().stream() //
					.sorted() //
					.forEach(k -> ps.printf("%s %d%n", k, offsets.get(k)));
		}
	}

	public static Map<String, Long> readOffsets(File file) throws IOException, ClassNotFoundException
	{
		try (Stream<String> stream = Files.lines(file.toPath()))
		{
			return stream //
					.map(str -> str.split("\\s")) //
					.collect(toMap(f -> f[0], f -> Long.parseLong(f[1])));
		}
	}
}
