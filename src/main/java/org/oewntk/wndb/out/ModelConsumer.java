/*
 * Copyright (c) 2021-2021. Bernard Bou.
 */

package org.oewntk.wndb.out;

import org.oewntk.model.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Main class that generates the WN database in the WNDB format as per wndb(5WN)
 *
 * @author Bernard Bou
 * @see "https://wordnet.princeton.edu/documentation/wndb5wn"
 */
public class ModelConsumer implements Consumer<Model>
{
	private final File outDir;

	private final int flags;

	/**
	 * Constructor
	 *
	 * @param outDir output directory
	 * @param flags  flags
	 */
	public ModelConsumer(final File outDir, final int flags)
	{
		this.outDir = outDir;
		this.flags = flags;
	}

	@Override
	public void accept(final Model model)
	{
		Tracing.psInfo.printf("[Model] %s%n", Arrays.toString(model.getSources()));

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
	public void grind(final Model model) throws IOException
	{
		// Output
		if (!outDir.exists())
		{
			//noinspection ResultOfMethodCallIgnored
			outDir.mkdirs();
		}

		// Compute synset offsets
		Map<String, Long> offsets = new GrindOffsets(model.getLexesByLemma(), model.getSynsetsById(), model.getSensesById(), flags).compute();

		// Process
		data(outDir, model.getLexesByLemma(), model.getSynsetsById(), model.getSensesById(), offsets);
		indexWords(outDir, model.getSensesById(), model.getSynsetsById(), offsets);
		indexSenses(outDir, model.getSensesById(), offsets);
		morphs(outDir, model.getLexesByLemma());
		indexTemplates(outDir, model.getSensesById());
		templates(outDir, model.getVerbTemplatesById());
		tagcounts(outDir, model.getSensesById());
		domains(outDir);
	}

	/**
	 * Grind data.{noun|verb|adj|adv}
	 *
	 * @param dir          output directory
	 * @param lexesByLemma lexes mapped by lemma
	 * @param synsetsById  synsets mapped by synset id
	 * @param sensesById   senses mapped by sense id
	 * @param offsets      offsets mapped by synset id
	 * @throws IOException io
	 */
	public void data(File dir, //
			Map<String, List<Lex>> lexesByLemma, //
			Map<String, Synset> synsetsById, //
			Map<String, Sense> sensesById, //
			Map<String, Long> offsets) throws IOException
	{
		// Data
		GrindSynsets grinder = new GrindSynsets(lexesByLemma, synsetsById, sensesById, offsets, flags);
		try (PrintStream ps = new PrintStream(new FileOutputStream(new File(dir, "data.noun")), true, StandardCharsets.UTF_8))
		{
			grinder.makeData(ps, synsetsById, Data.NOUN_POS_FILTER);
			grinder.report();
		}
		try (PrintStream ps = new PrintStream(new FileOutputStream(new File(dir, "data.verb")), true, StandardCharsets.UTF_8))
		{
			grinder.makeData(ps, synsetsById, Data.VERB_POS_FILTER);
			grinder.report();
		}
		try (PrintStream ps = new PrintStream(new FileOutputStream(new File(dir, "data.adj")), true, StandardCharsets.UTF_8))
		{
			grinder.makeData(ps, synsetsById, Data.ADJ_POS_FILTER);
			grinder.report();
		}
		try (PrintStream ps = new PrintStream(new FileOutputStream(new File(dir, "data.adv")), true, StandardCharsets.UTF_8))
		{
			grinder.makeData(ps, synsetsById, Data.ADV_POS_FILTER);
			grinder.report();
		}
	}

	/**
	 * Make word index
	 *
	 * @param dir         output directory
	 * @param sensesById  senses mapped by sense id (sensekey)
	 * @param synsetsById synsets mapped by synset id
	 * @param offsets     offsets mapped by synset id
	 * @throws IOException io
	 */
	public void indexWords(File dir, //
			Map<String, Sense> sensesById, //
			Map<String, Synset> synsetsById, //
			Map<String, Long> offsets) throws IOException
	{
		// Index
		WordIndexer indexer = new WordIndexer(offsets, flags);
		try (PrintStream ps = new PrintStream(new FileOutputStream(new File(dir, "index.noun")), true, StandardCharsets.UTF_8))
		{
			indexer.make(ps, sensesById, synsetsById, Data.NOUN_POS_FILTER);
		}
		try (PrintStream ps = new PrintStream(new FileOutputStream(new File(dir, "index.verb")), true, StandardCharsets.UTF_8))
		{
			indexer.make(ps, sensesById, synsetsById, Data.VERB_POS_FILTER);
		}
		try (PrintStream ps = new PrintStream(new FileOutputStream(new File(dir, "index.adj")), true, StandardCharsets.UTF_8))
		{
			indexer.make(ps, sensesById, synsetsById, Data.ADJ_POS_FILTER);
		}
		try (PrintStream ps = new PrintStream(new FileOutputStream(new File(dir, "index.adv")), true, StandardCharsets.UTF_8))
		{
			indexer.make(ps, sensesById, synsetsById, Data.ADV_POS_FILTER);
		}
	}

	/**
	 * Grind index.sense
	 *
	 * @param dir        output directory
	 * @param sensesById senses mapped by id
	 * @param offsets    offsets mapped by synsetId
	 * @throws IOException io
	 */
	public void indexSenses(File dir, Map<String, Sense> sensesById, Map<String, Long> offsets) throws IOException
	{
		try (PrintStream ps = new PrintStream(new FileOutputStream(new File(dir, "index.sense")), true, StandardCharsets.UTF_8))
		{
			new SenseIndexer(flags, offsets).make(ps, sensesById);
		}
	}

	/**
	 * Grind {noun|verb|adj|adv}.exc
	 *
	 * @param lexesByLemma lexes mapped by lemma
	 * @param dir          output directory
	 * @throws IOException io
	 */
	public static void morphs(File dir, Map<String, List<Lex>> lexesByLemma) throws IOException
	{
		GrindMorphs grinder = new GrindMorphs();
		try (PrintStream ps = new PrintStream(new FileOutputStream(new File(dir, "noun.exc")), true, StandardCharsets.UTF_8))
		{
			grinder.makeMorph(ps, lexesByLemma, Data.NOUN_POS_FILTER);
		}
		try (PrintStream ps = new PrintStream(new FileOutputStream(new File(dir, "verb.exc")), true, StandardCharsets.UTF_8))
		{
			grinder.makeMorph(ps, lexesByLemma, Data.VERB_POS_FILTER);
		}
		try (PrintStream ps = new PrintStream(new FileOutputStream(new File(dir, "adj.exc")), true, StandardCharsets.UTF_8))
		{
			grinder.makeMorph(ps, lexesByLemma, Data.ADJ_POS_FILTER);
		}
		try (PrintStream ps = new PrintStream(new FileOutputStream(new File(dir, "adv.exc")), true, StandardCharsets.UTF_8))
		{
			grinder.makeMorph(ps, lexesByLemma, Data.ADV_POS_FILTER);
		}
	}

	/**
	 * Grind sentidx.vrb
	 *
	 * @param dir        output directory
	 * @param sensesById senses mapped by id
	 * @throws IOException io
	 */
	public static void indexTemplates(File dir, Map<String, Sense> sensesById) throws IOException
	{
		TemplateIndexer indexer = new TemplateIndexer();
		try (PrintStream ps = new PrintStream(new FileOutputStream(new File(dir, "sentidx.vrb")), true, StandardCharsets.UTF_8))
		{
			indexer.makeIndex(ps, sensesById);
		}
	}

	/**
	 * Grind sent.vrb
	 *
	 * @param dir               output directory
	 * @param verbTemplatesById verb templates mapped by id
	 * @throws IOException io
	 */
	public static void templates(File dir, Map<Integer, VerbTemplate> verbTemplatesById) throws IOException
	{
		GrindVerbTemplates grinder = new GrindVerbTemplates();
		try (PrintStream ps = new PrintStream(new FileOutputStream(new File(dir, "sents.vrb")), true, StandardCharsets.UTF_8))
		{
			grinder.makeTemplates(ps, verbTemplatesById);
		}
	}

	/**
	 * Grind cntlist cntlist.rev
	 *
	 * @param dir        output directory
	 * @param sensesById senses mapped by id
	 * @throws IOException io
	 */
	public static void tagcounts(File dir, Map<String, Sense> sensesById) throws IOException
	{
		GrindTagCounts grinder = new GrindTagCounts();
		try (PrintStream ps = new PrintStream(new FileOutputStream(new File(dir, "cntlist.rev")), true, StandardCharsets.UTF_8))
		{
			grinder.makeTagCountRev(ps, sensesById);
		}
		try (PrintStream ps = new PrintStream(new FileOutputStream(new File(dir, "cntlist")), true, StandardCharsets.UTF_8))
		{
			grinder.makeTagCount(ps, sensesById);
		}
	}


	/**
	 * Grind lexdomains
	 *
	 * @param dir output directory
	 */
	private void domains(final File dir) throws FileNotFoundException
	{
		try (PrintStream ps = new PrintStream(new FileOutputStream(new File(dir, "lexnames")), true, StandardCharsets.UTF_8))
		{
			Coder.LEXFILE_TO_NUM.entrySet().stream() //
					.sorted(Comparator.comparingInt(Map.Entry::getValue)) //
					.forEach(e -> ps.printf("%02d\t%s\t%d%n", e.getValue(), e.getKey(), posNameToInt(e.getKey().split("\\.")[0])));
		}
		Tracing.psInfo.printf("Lex names%n");
	}

	private static int posNameToInt(final String posName)
	{
		switch (posName)
		{
			case "noun":
				return 1;
			case "verb":
				return 2;
			case "adj":
				return 3;
			case "adv":
				return 4;
		}
		return 0;
	}
}
