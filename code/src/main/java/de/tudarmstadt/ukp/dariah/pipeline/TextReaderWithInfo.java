package de.tudarmstadt.ukp.dariah.pipeline;

import de.tudarmstadt.ukp.dkpro.core.io.text.TextReader;

public class TextReaderWithInfo extends TextReader {

	@Override
	protected Resource nextFile() {
		Resource next = super.nextFile();
		System.out.println("Process file: "+next.toString());
		return next;
	}

}
