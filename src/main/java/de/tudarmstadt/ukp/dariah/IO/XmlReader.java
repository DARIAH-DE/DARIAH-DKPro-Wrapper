/*******************************************************************************
 * Copyright 2015
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package de.tudarmstadt.ukp.dariah.IO;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.LinkedList;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.fit.component.CasCollectionReader_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.TypeCapability;
import org.apache.uima.jcas.JCas;
import org.apache.uima.util.Progress;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import de.tudarmstadt.ukp.dariah.type.Section;
import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import de.tudarmstadt.ukp.dkpro.core.api.parameter.ComponentParameters;








/**
 * Reads in xml files. The xpath to each element is stored in a special annotation.
 * @author Nils Reimers
 *
 */
@TypeCapability(
        outputs={
                "de.tudarmstadt.ukp.dkpro.core.api.structure.type.Field",
                "de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData"})
public class XmlReader extends CasCollectionReader_ImplBase {

	
	/**
	 * Set this as the language of the produced documents.
	 */
	public static final String PARAM_LANGUAGE = ComponentParameters.PARAM_LANGUAGE;
	@ConfigurationParameter(name=PARAM_LANGUAGE, mandatory=false)
	private String language;


	private static Logger logger = LogManager.getLogger(XmlReader.class);

	@Override
	public void getNext(CAS aCAS)
		throws IOException, CollectionException
	{
		JCas jcas;
		try {
			jcas = aCAS.getJCas();
		}
		catch (CASException e) {
			throw new CollectionException(e);
		}

		try {
			// parse the xml file
			File xmlFile = GlobalFileStorage.getInstance().poll();
			
			logger.info("Process file: "+xmlFile.getName());
			
			SAXParserFactory spf = SAXParserFactory.newInstance();
	        SAXParser sp = spf.newSAXParser();
	        XMLReader xr = sp.getXMLReader();
	        
	        LinkedList<String[]> textElements = new LinkedList<>();
	        FragmentContentHandler fch = new FragmentContentHandler(xr, textElements);	        
	        xr.setContentHandler(fch);
	        xr.parse(new InputSource(new FileInputStream(xmlFile)));
	      
	        StringBuilder docText = new StringBuilder();
	        
	        for(String[] element : textElements) {
	        	
	        	int start = docText.length();
	        	int end = start + element[1].length();
	        	
	        	docText.append(element[1]+"\n\n");
	        	
	        	Section section = new Section(jcas, start, end);
	        	section.setValue(element[0]);
	        	section.addToIndexes();
	        	
	        }
	        
	        jcas.setDocumentText(docText.toString().trim());
	        jcas.setDocumentLanguage(language);
	        
	        DocumentMetaData docMetaData = DocumentMetaData.create(aCAS);
            docMetaData.setDocumentTitle(xmlFile.getName());
            docMetaData.setDocumentId(xmlFile.getAbsolutePath());
            docMetaData.setDocumentBaseUri("file:"+xmlFile.getParentFile().getAbsolutePath());
            docMetaData.setDocumentUri("file:"+xmlFile.getAbsolutePath());

		} catch (Exception e) {
			//e.printStackTrace();
			throw new CollectionException(e);
		}

	}

	@Override
	public Progress[] getProgress()
	{
		return null;
	}

	@Override
	public boolean hasNext()
		throws IOException, CollectionException {
		
		return !GlobalFileStorage.getInstance().isEmpty();		
	}

	
}