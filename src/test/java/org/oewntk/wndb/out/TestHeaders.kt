/*
 * Copyright (c) 2021. Bernard Bou.
 */

package org.oewntk.wndb.out;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TestHeaders
{
	@Test
	public void testHeaders()
	{
		int pwnHeader = Formatter.PRINCETON_HEADER.length();
		int oewnHeader = Formatter.OEWN_HEADER.length();
		System.out.println("PWN " + pwnHeader);
		System.out.println("OEWN " + oewnHeader);
		assertEquals(pwnHeader, oewnHeader);
	}
}
