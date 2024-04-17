/*
 * Copyright (c) 2021-2021. Bernard Bou.
 */

package org.oewntk.wndb.out;

import org.oewntk.model.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap.SimpleEntry;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;

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
		indexWords(outDir, model.senses, model.getSynsetsById(), offsets);
		indexSenses(outDir, model.senses, offsets);
		morphs(outDir, model.getLexesByLemma());
		templates(outDir, model.getVerbTemplatesById());
		indexTemplates(outDir, model.getSensesById());
		verbFrames(outDir, model.verbFrames);
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
			Map<String, Collection<Lex>> lexesByLemma, //
			Map<String, Synset> synsetsById, //
			Map<String, Sense> sensesById, //
			Map<String, Long> offsets) throws IOException
	{
		long nCount, vCount, aCount, rCount;
		GrindSynsets grinder = new GrindSynsets(lexesByLemma, synsetsById, sensesById, offsets, flags);
		try (PrintStream ps = new PrintStream(new FileOutputStream(new File(dir, "data.noun")), true, StandardCharsets.UTF_8))
		{
			nCount = grinder.makeData(ps, synsetsById, Data.NOUN_POS_FILTER);
			grinder.report();
		}
		try (PrintStream ps = new PrintStream(new FileOutputStream(new File(dir, "data.verb")), true, StandardCharsets.UTF_8))
		{
			vCount = grinder.makeData(ps, synsetsById, Data.VERB_POS_FILTER);
			grinder.report();
		}
		try (PrintStream ps = new PrintStream(new FileOutputStream(new File(dir, "data.adj")), true, StandardCharsets.UTF_8))
		{
			aCount = grinder.makeData(ps, synsetsById, Data.ADJ_POS_FILTER);
			grinder.report();
		}
		try (PrintStream ps = new PrintStream(new FileOutputStream(new File(dir, "data.adv")), true, StandardCharsets.UTF_8))
		{
			rCount = grinder.makeData(ps, synsetsById, Data.ADV_POS_FILTER);
			grinder.report();
		}
		long sum = nCount + vCount + aCount + rCount;
		Tracing.psInfo.printf("Synsets: %d [n:%d v:%d a:%d r:%d]%n", sum, nCount, vCount, aCount, rCount);
	}

	/**
	 * Make word index
	 *
	 * @param dir         output directory
	 * @param senses      senses
	 * @param synsetsById synsets mapped by synset id
	 * @param offsets     offsets mapped by synset id
	 * @throws IOException io
	 */
	public void indexWords(File dir, //
			Collection<Sense> senses, //
			Map<String, Synset> synsetsById, //
			Map<String, Long> offsets) throws IOException
	{
		long nCount, vCount, aCount, rCount;
		WordIndexer indexer = new WordIndexer(offsets, flags);
		try (PrintStream ps = new PrintStream(new FileOutputStream(new File(dir, "index.noun")), true, StandardCharsets.UTF_8))
		{
			nCount = indexer.make(ps, senses, synsetsById, Data.NOUN_POS_FILTER);
		}
		try (PrintStream ps = new PrintStream(new FileOutputStream(new File(dir, "index.verb")), true, StandardCharsets.UTF_8))
		{
			vCount = indexer.make(ps, senses, synsetsById, Data.VERB_POS_FILTER);
		}
		try (PrintStream ps = new PrintStream(new FileOutputStream(new File(dir, "index.adj")), true, StandardCharsets.UTF_8))
		{
			aCount = indexer.make(ps, senses, synsetsById, Data.ADJ_POS_FILTER);
		}
		try (PrintStream ps = new PrintStream(new FileOutputStream(new File(dir, "index.adv")), true, StandardCharsets.UTF_8))
		{
			rCount = indexer.make(ps, senses, synsetsById, Data.ADV_POS_FILTER);
		}
		long sum = nCount + vCount + aCount + rCount;
		Tracing.psInfo.printf("Indexes: %d [n:%d v:%d a:%d r:%d]%n", sum, nCount, vCount, aCount, rCount);
	}

	/**
	 * Grind index.sense
	 *
	 * @param dir     output directory
	 * @param senses  senses
	 * @param offsets offsets mapped by synsetId
	 * @throws IOException io
	 */
	public void indexSenses(File dir, Collection<Sense> senses, Map<String, Long> offsets) throws IOException
	{
		try (PrintStream ps = new PrintStream(new FileOutputStream(new File(dir, "index.sense")), true, StandardCharsets.UTF_8))
		{
			new SenseIndexer(flags, offsets).make(ps, senses);
		}
	}

	/**
	 * Grind {noun|verb|adj|adv}.exc
	 *
	 * @param lexesByLemma lexes mapped by lemma
	 * @param dir          output directory
	 * @throws IOException io
	 */
	public static void morphs(File dir, Map<String, Collection<Lex>> lexesByLemma) throws IOException
	{
		long nCount, vCount, aCount, rCount;
		GrindMorphs grinder = new GrindMorphs();
		try (PrintStream ps = new PrintStream(new FileOutputStream(new File(dir, "noun.exc")), true, StandardCharsets.UTF_8))
		{
			nCount = grinder.makeMorph(ps, lexesByLemma, Data.NOUN_POS_FILTER);
		}
		try (PrintStream ps = new PrintStream(new FileOutputStream(new File(dir, "verb.exc")), true, StandardCharsets.UTF_8))
		{
			vCount = grinder.makeMorph(ps, lexesByLemma, Data.VERB_POS_FILTER);
		}
		try (PrintStream ps = new PrintStream(new FileOutputStream(new File(dir, "adj.exc")), true, StandardCharsets.UTF_8))
		{
			aCount = grinder.makeMorph(ps, lexesByLemma, Data.ADJ_POS_FILTER);
		}
		try (PrintStream ps = new PrintStream(new FileOutputStream(new File(dir, "adv.exc")), true, StandardCharsets.UTF_8))
		{
			rCount = grinder.makeMorph(ps, lexesByLemma, Data.ADV_POS_FILTER);
		}
		long sum = nCount + vCount + aCount + rCount;
		Tracing.psInfo.printf("Morphs: %d [n:%d v:%d a:%d r:%d]%n", sum, nCount, vCount, aCount, rCount);
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
		try (PrintStream ps = new PrintStream(new FileOutputStream(new File(dir, "cntlist")), true, StandardCharsets.UTF_8))
		{
			grinder.makeTagCount(ps, sensesById);
		}
		try (PrintStream ps = new PrintStream(new FileOutputStream(new File(dir, "cntlist.rev")), true, StandardCharsets.UTF_8))
		{
			grinder.makeTagCountRev(ps, sensesById);
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
		Tracing.psInfo.printf("Lexfiles: %d%n", Coder.LEXFILE_TO_NUM.size());
	}

	/**
	 * Pos name to int
	 *
	 * @param posName pos name
	 * @return integer code
	 */
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

	/**
	 * Grind verb frames
	 *
	 * @param dir        output directory
	 * @param verbFrames verb frames
	 */
	private void verbFrames(final File dir, final Collection<VerbFrame> verbFrames) throws FileNotFoundException
	{
		try (PrintStream ps = new PrintStream(new FileOutputStream(new File(dir, "verb.Framestext")), true, StandardCharsets.UTF_8))
		{
			int[] i = {0};
			verbFrames.stream() //
					.peek(vf -> ++i[0]) //
					.map(vf -> new SimpleEntry<>(getVerbFrameNID(vf, i[0]), vf)) //
					.sorted(Comparator.comparingInt(Map.Entry::getKey)) //
					.forEach(e -> ps.printf("%d %s%n", e.getKey(), e.getValue().frame));
		}
		Tracing.psInfo.printf("Verb frames: %d%n", verbFrames.size());
	}

	// name, frameid
	public static final Object[][] VERBFRAME_VALUES = new Object[][]{ //
			{"vii", 1}, //
			{"via", 2}, //
			{"nonreferential", 3}, //
			{"vii-pp", 4}, //
			{"vtii-adj", 5}, //
			{"vii-adj", 6}, //
			{"via-adj", 7}, //
			{"vtai", 8}, //
			{"vtaa", 9}, //
			{"vtia", 10}, //
			{"vtii", 11}, //
			{"vii-to", 12}, //
			{"via-on-inanim", 13}, //
			{"ditransitive", 14}, //
			{"vtai-to", 15}, //
			{"vtai-from", 16}, //
			{"vtaa-with", 17}, //
			{"vtaa-of", 18}, //
			{"vtai-on", 19}, //
			{"vtaa-pp", 20}, //
			{"vtai-pp", 21}, //
			{"via-pp", 22}, //
			{"vibody", 23}, //
			{"vtaa-to-inf", 24}, //
			{"vtaa-inf", 25}, //
			{"via-that", 26}, //
			{"via-to", 27}, //
			{"via-to-inf", 28}, //
			{"via-whether-inf", 29}, //
			{"vtaa-into-ger", 30}, //
			{"vtai-with", 31}, //
			{"via-inf", 32}, //
			{"via-ger", 33}, //
			{"nonreferential-sent", 34}, //
			{"vii-inf", 35}, //
			{"via-at", 36}, //
			{"via-for", 37}, //
			{"via-on-anim", 38}, //
			{"via-out-of", 39}, //
	};

	/**
	 * Map frame id (via, ...) to numeric id
	 */
	public static final Map<String, Integer> VERB_FRAME_ID_TO_NIDS = Stream.of(VERBFRAME_VALUES).collect(toMap(data -> (String) data[0], data -> (Integer) data[1]));

	/**
	 * Get verb frame nid
	 * @param vf verb frame
	 * @param index index if map lookup fails
	 * @return map lookup, 100 + index if it fails
	 */
	private static int getVerbFrameNID(VerbFrame vf, int index)
	{
		String id = vf.id;
		Integer nid = VERB_FRAME_ID_TO_NIDS.get(id);
		if (nid != null)
		{
			return nid;
		}
		return 100 + index;
	}
}
