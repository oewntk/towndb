/*
 * Copyright (c) 2021. Bernard Bou.
 */

package org.oewntk.wndb.out;

import org.junit.Test;

import java.util.*;

public class TestCoder
{
	@Test
	public void testCoderCompat()
	{
		testCoder(true);
	}

	@Test
	public void testCoderNoCompat()
	{
		testCoder(true);
	}

	public void testCoder(boolean pointerCompat)
	{
		final Map<Character, Set<String>> allRelations = new HashMap<>();
		final Set<String> nSet = allRelations.computeIfAbsent('n', (k) -> new HashSet<>());
		nSet.addAll(Arrays.asList(Coder.ANTONYM, Coder.HYPERNYM, Coder.INSTANCE_HYPERNYM, Coder.HYPONYM, Coder.INSTANCE_HYPONYM, Coder.HOLO_MEMBER, Coder.HOLO_SUBSTANCE, Coder.HOLO_PART, Coder.MERO_MEMBER, Coder.MERO_SUBSTANCE, Coder.MERO_PART, Coder.ATTRIBUTE, Coder.PERTAINYM, Coder.DERIVATION, Coder.DOMAIN_TOPIC, Coder.HAS_DOMAIN_TOPIC, Coder.DOMAIN_REGION, Coder.HAS_DOMAIN_REGION, Coder.DOMAIN_USAGE, Coder.HAS_DOMAIN_USAGE));
		final Set<String> vSet = allRelations.computeIfAbsent('v', (k) -> new HashSet<>());
		vSet.addAll(Arrays.asList(Coder.ANTONYM, Coder.HYPERNYM, Coder.HYPONYM, Coder.ENTAILS, Coder.IS_ENTAILED, Coder.CAUSES, Coder.IS_CAUSED, Coder.ALSO, Coder.VERB_GROUP, Coder.DERIVATION, Coder.DOMAIN_TOPIC, Coder.DOMAIN_REGION, Coder.DOMAIN_USAGE));
		final Set<String> aSet = allRelations.computeIfAbsent('a', (k) -> new HashSet<>());
		aSet.addAll(Arrays.asList(Coder.ANTONYM, Coder.SIMILAR, Coder.PARTICIPLE, Coder.PERTAINYM, Coder.ATTRIBUTE, Coder.ALSO, Coder.DERIVATION, Coder.DOMAIN_TOPIC, Coder.DOMAIN_REGION, Coder.DOMAIN_USAGE, Coder.HAS_DOMAIN_TOPIC, Coder.HAS_DOMAIN_REGION, Coder.HAS_DOMAIN_USAGE));
		final Set<String> rSet = allRelations.computeIfAbsent('r', (k) -> new HashSet<>());
		rSet.addAll(Arrays.asList(Coder.ANTONYM, Coder.PERTAINYM, Coder.DERIVATION, Coder.DOMAIN_TOPIC, Coder.DOMAIN_REGION, Coder.DOMAIN_USAGE, Coder.HAS_DOMAIN_TOPIC, Coder.HAS_DOMAIN_REGION, Coder.HAS_DOMAIN_USAGE));

		final Set<String> allPointers = new TreeSet<>();
		final Map<Character, Map<String, String>> toRelations = new HashMap<>();
		for (Character pos : Arrays.asList('n', 'v', 'a', 'r'))
		{
			for (String relation : allRelations.get(pos))
			{
				String pointer;
				try
				{
					pointer = Coder.codeRelation(relation, pos, pointerCompat);
				}
				catch (CompatException e)
				{
					Tracing.psErr.println(e.getCause().getMessage());
					continue;
				}
				catch (IllegalArgumentException e)
				{
					Tracing.psErr.println(relation + " for " + pos + " " + e.getCause().getMessage());
					continue;
				}
				allPointers.add(pointer);
				Map<String, String> pointerToRelation = toRelations.computeIfAbsent(pos, (p) -> new HashMap<>());
				pointerToRelation.put(pointer, relation);
			}
		}
		for (String pointer : allPointers)
		{
			Tracing.psInfo.printf("%-2s\t", pointer);
			for (Character pos : Arrays.asList('n', 'v', 'a', 'r'))
			{
				String relation = toRelations.get(pos).get(pointer);
				if (relation != null)
				{
					Tracing.psInfo.printf("%s:%s  ", pos, relation);
				}
			}
			Tracing.psInfo.println();
		}
	}
}
