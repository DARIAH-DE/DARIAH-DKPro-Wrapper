package de.tudarmstadt.ukp.dariah.pipeline;

import de.tudarmstadt.ukp.dkpro.core.io.text.TextReader;

/**
 * Outputs which file is currently read
 * @author reimers
 *
 */
public class TextReaderWithInfo extends TextReader {

	@Override
	protected Resource nextFile() {
		Resource next = super.nextFile();
		System.out.println("Process file: "+next.toString());
		return next;
	}

}
