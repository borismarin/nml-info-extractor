package org.neuroml2.export.info;

import java.io.File;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.neuroml2.model.NeuroML2ModelReader;
import org.neuroml2.model.Neuroml2;

public class InfoExtractorTest {

	private Neuroml2 hh;

	@Rule public ExpectedException thrown = ExpectedException.none();

	@Before
	public void setUp() throws Throwable {
		hh = NeuroML2ModelReader.read(getLocalFile("/NML2_SingleCompHHCell.nml"));
	}

	@Test
	public void testGeneration() {
		System.out.println(hh.getBaseCells());

	}

	protected File getLocalFile(String fname) {
		return new File(getClass().getResource(fname).getFile());
	}

}
