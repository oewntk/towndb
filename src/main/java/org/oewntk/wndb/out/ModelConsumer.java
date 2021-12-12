/*
 * Copyright (c) 2021-2021. Bernard Bou.
 */

package org.oewntk.wndb.out;

import org.oewntk.model.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
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

	private final PrintStream ps;

	/**
	 * Constructor
	 *
	 * @param outDir output directory
	 * @param flags  flags
	 * @param ps     log print stream
	 */
	public ModelConsumer(final File outDir, final int flags, final PrintStream ps)
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
			e.printStackTrace(System.err);
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
		// Model
		ps.printf("[Model] %s%n", Arrays.toString(model.getSources()));

		// Output
		if (!outDir.exists())
		{
			//noinspection ResultOfMethodCallIgnored
			outDir.mkdirs();
		}

		// Compute synset offsets
		Map<String, Long> offsets = new OffsetFactory(model.lexesByLemma, model.synsetsById, model.sensesById, flags).compute();

		// Process
		data(outDir, model.lexesByLemma, model.synsetsById, model.sensesById, offsets);
		indexWords(outDir, model.lexesByLemma, model.synsetsById, offsets);
		indexSenses(outDir, model.sensesById, offsets);
		morphs(outDir, model.lexesByLemma);
		indexTemplates(outDir, model.sensesById);
		templates(outDir, model.verbTemplatesById);
		tagcounts(outDir, model.sensesById);
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
		DataGrinder grinder = new DataGrinder(lexesByLemma, synsetsById, sensesById, offsets, flags);
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
	 * @param dir          output directory
	 * @param lexesByLemma lexes mapped by lemma
	 * @param synsetsById  synsets mapped by synset id
	 * @param offsets      offsets mapped by synset id
	 * @throws IOException io
	 */
	public void indexWords(File dir, //
			Map<String, List<Lex>> lexesByLemma, //
			Map<String, Synset> synsetsById, //
			Map<String, Long> offsets) throws IOException
	{
		// Index
		WordIndexer indexer = new WordIndexer(offsets, flags);
		try (PrintStream ps = new PrintStream(new FileOutputStream(new File(dir, "index.noun")), true, StandardCharsets.UTF_8))
		{
			indexer.makeIndex(ps, lexesByLemma, synsetsById, Data.NOUN_POS_FILTER);
		}
		try (PrintStream ps = new PrintStream(new FileOutputStream(new File(dir, "index.verb")), true, StandardCharsets.UTF_8))
		{
			indexer.makeIndex(ps, lexesByLemma, synsetsById, Data.VERB_POS_FILTER);
		}
		try (PrintStream ps = new PrintStream(new FileOutputStream(new File(dir, "index.adj")), true, StandardCharsets.UTF_8))
		{
			indexer.makeIndex(ps, lexesByLemma, synsetsById, Data.ADJ_POS_FILTER);
		}
		try (PrintStream ps = new PrintStream(new FileOutputStream(new File(dir, "index.adv")), true, StandardCharsets.UTF_8))
		{
			indexer.makeIndex(ps, lexesByLemma, synsetsById, Data.ADV_POS_FILTER);
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
	public static void indexSenses(File dir, Map<String, Sense> sensesById, Map<String, Long> offsets) throws IOException
	{
		try (PrintStream ps = new PrintStream(new FileOutputStream(new File(dir, "index.sense")), true, StandardCharsets.UTF_8))
		{
			new SenseIndexer(offsets).makeIndexLowerMultiValue(ps, sensesById);
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
		MorphGrinder grinder = new MorphGrinder();
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
		TemplateGrinder grinder = new TemplateGrinder();
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
		TagCountGrinder grinder = new TagCountGrinder();
		try (PrintStream ps = new PrintStream(new FileOutputStream(new File(dir, "cntlist.rev")), true, StandardCharsets.UTF_8))
		{
			grinder.makeTagCountRev(ps, sensesById);
		}
		try (PrintStream ps = new PrintStream(new FileOutputStream(new File(dir, "cntlist")), true, StandardCharsets.UTF_8))
		{
			grinder.makeTagCount(ps, sensesById);
		}
	}
}
