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
package de.tudarmstadt.ukp.dariah.xml;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.stream.XMLStreamException;

import org.apache.uima.UimaContext;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.fit.component.CasCollectionReader_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.TypeCapability;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;

import de.tudarmstadt.ukp.dariah.type.Section;
import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import de.tudarmstadt.ukp.dkpro.core.api.parameter.ComponentParameters;



import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;


import javax.xml.parsers.*;

import org.xml.sax.*;
import org.xml.sax.helpers.*;

import java.util.*;
import java.io.*;


@TypeCapability(
        outputs={
                "de.tudarmstadt.ukp.dkpro.core.api.structure.type.Field",
                "de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData"})

public class XmlReader extends CasCollectionReader_ImplBase {

	/**
	 * Location from which the input is read.
	 */
	public static final String PARAM_SOURCE_LOCATION = ComponentParameters.PARAM_SOURCE_LOCATION;
	@ConfigurationParameter(name=PARAM_SOURCE_LOCATION, mandatory=true)
	private String inputPath;

	/**
	 * Set this as the language of the produced documents.
	 */
	public static final String PARAM_LANGUAGE = ComponentParameters.PARAM_LANGUAGE;
	@ConfigurationParameter(name=PARAM_LANGUAGE, mandatory=false)
	private String language;

	/**
	 * optional, tags those should be worked on (if empty, then all tags
	 * except those ExcludeTags will be worked on)
	 * 
	 * @NOTE: Currently not implemented
	 */
//	public static final String PARAM_INCLUDE_TAG = "IncludeTag";
//	@ConfigurationParameter(name=PARAM_INCLUDE_TAG, mandatory=true, defaultValue={})
//	private Set<String> includeTags;

	/**
	 * optional, tags those should not be worked on. Out them should no
	 * text be extracted and also no Annotations be produced.
	 * 
	 *  @NOTE: Currently not implemented
	 */
//	public static final String PARAM_EXCLUDE_TAG = "ExcludeTag";
//	@ConfigurationParameter(name=PARAM_EXCLUDE_TAG, mandatory=true, defaultValue={})
//	private Set<String> excludeTags;

	/**
	 * tag which contains the docId
	 * 
	 *  @NOTE: Currently not implemented
	 */
//	public static final String PARAM_DOC_ID_TAG = "DocIdTag";
//	@ConfigurationParameter(name=PARAM_DOC_ID_TAG, mandatory=false)
//	private String docIdTag;

	/**
	 * The collection ID to set in the {@link DocumentMetaData}.
	 * 
	 *  @NOTE: Currently not implemented
	 */
//	public static final String PARAM_COLLECTION_ID = "collectionId";
//	@ConfigurationParameter(name=PARAM_COLLECTION_ID, mandatory=false)
//	private String collectionId;



	// mandatory, list of xml files to be readed in
	private final ArrayList<File> xmlFiles = new ArrayList<File>();

	// current be parsed file index
	private int currentParsedFile;



	@Override
	public void initialize(UimaContext aContext)
		throws ResourceInitializationException
	{
		super.initialize(aContext);

		// mandatory, directory where that those be parsed XML files are
		File inPath = new File(inputPath);
		// get all xml files from the input directory (ignore the
		// subdirectories)
		if (inPath.isDirectory()) {
			File[] files = inPath.listFiles();
			for (File file : files) {
				if (file.isFile() && (file.toString().endsWith(".xml") || file.toString().endsWith(".sgml"))) {
					xmlFiles.add(file);
				}
			}
			Collections.sort(xmlFiles);
		} else if(inPath.isFile() && inPath.exists()) {
			xmlFiles.add(inPath);
		}
		else {
			throw new ResourceInitializationException("Invalid path", new Object[] {inputPath});
		}

		
		currentParsedFile = 0;

//		if (docIdTag != null && docIdTag.contains("/@")) {
//			int split = docIdTag.indexOf("/@");
//			docIdElementLocalName = docIdTag.substring(0, split);
//			docIdAttributeName = docIdTag.substring(split+2);
//		}
//		else {
//			docIdElementLocalName = docIdTag;
//		}
	}

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
			File xmlFile = xmlFiles.get(currentParsedFile);
			
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
			
			currentParsedFile++;	
		} catch (Exception e) {
			e.printStackTrace();
			throw new CollectionException(e);
		}

	}

	@Override
	public Progress[] getProgress()
	{
		return new Progress[] { new ProgressImpl(currentParsedFile, xmlFiles
				.size(), Progress.ENTITIES) };
	}

	@Override
	public boolean hasNext()
		throws IOException, CollectionException
	{

		if (currentParsedFile >= 0 && currentParsedFile < xmlFiles.size()) {
			// There are additional files to parse
			return true;
		}
		
		return false;		
	}

	@Override
	public void close()
		throws IOException
	{
		// Nothing to do
	}

	
}