package de.tudarmstadt.ukp.dariah.IO;

import static org.apache.commons.io.IOUtils.closeQuietly;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.fit.component.CasCollectionReader_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.util.Progress;

import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import de.tudarmstadt.ukp.dkpro.core.api.parameter.ComponentParameters;

/**
 * Outputs which file is currently read
 * @author reimers
 *
 */
public class TextReaderWithInfo extends CasCollectionReader_ImplBase {

	/**
	 * Set this as the language of the produced documents.
	 */
	public static final String PARAM_LANGUAGE = ComponentParameters.PARAM_LANGUAGE;
	@ConfigurationParameter(name=PARAM_LANGUAGE, mandatory=false)
	private String language;
	
	/**
	 * Name of configuration parameter that contains the character encoding used by the input files.
	 */
	public static final String PARAM_ENCODING = ComponentParameters.PARAM_SOURCE_ENCODING;
	@ConfigurationParameter(name = PARAM_ENCODING, mandatory = true, defaultValue = "UTF-8")
	private String encoding;
	
	private static final Logger logger = LogManager.getLogger(TextReaderWithInfo.class);
	
	@Override
	public void getNext(CAS aCAS) throws IOException, CollectionException {
		JCas jcas;
		try {
			jcas = aCAS.getJCas();
		}
		catch (CASException e) {
			throw new CollectionException(e);
		}

		try {		
			File file = GlobalFileStorage.getInstance().poll();
			
			logger.info("Process file: "+file.getName());
			
			InputStream is = null;
			try {
				is = new BufferedInputStream(new FileInputStream(file));				
				aCAS.setDocumentText(IOUtils.toString(is, encoding));				
			}
			finally {
				closeQuietly(is);
			}

	        jcas.setDocumentLanguage(language);
	        
	        DocumentMetaData docMetaData = DocumentMetaData.create(aCAS);
            docMetaData.setDocumentTitle(file.getName());
            docMetaData.setDocumentId(file.getAbsolutePath());            
            docMetaData.setDocumentBaseUri("file:"+file.getAbsoluteFile().getParentFile().getAbsolutePath());            
            docMetaData.setDocumentUri("file:"+file.getAbsolutePath());
		} catch(Exception e) {
			throw new CollectionException(e);
		}
		
	}

	@Override
	public Progress[] getProgress() {
		return null;
	}

	@Override
	public boolean hasNext()
		throws IOException, CollectionException {
		
		return !GlobalFileStorage.getInstance().isEmpty();		
	}

	

}
