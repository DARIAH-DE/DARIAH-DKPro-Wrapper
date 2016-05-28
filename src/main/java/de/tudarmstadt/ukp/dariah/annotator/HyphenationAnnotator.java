/*******************************************************************************
 * Copyright 2016
 * Ubiquitous Knowledge Processing (UKP) Lab 
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package de.tudarmstadt.ukp.dariah.annotator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

import net.davidashen.text.Hyphenator;
import net.davidashen.util.ErrorHandler;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import de.tudarmstadt.ukp.dariah.type.Hyphenation;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

/**
 * Adds hyphenations to each token in a document
 * @author Nils Reimers
 */
public class HyphenationAnnotator extends JCasAnnotator_ImplBase{
	

	public static final String PARAM_HYPHEN_TABLE = "hyphenTable";
	@ConfigurationParameter(name = PARAM_HYPHEN_TABLE, mandatory = false)
	protected String hyphenTable;
	
	
	public static final String PARAM_CODE_TABLE = "codeTable";
	@ConfigurationParameter(name = PARAM_HYPHEN_TABLE, mandatory = false)
	protected String codeTable;
	
	protected Hyphenator hyphenator;
	
	private boolean errorInitMessageDisplayed = false;
	 
	
	@Override
	public void initialize(UimaContext context)
			throws ResourceInitializationException {
		// TODO Auto-generated method stub
		super.initialize(context);		
	}
	
	protected void initHyphenator(String language) {
		this.hyphenator=new Hyphenator();
		
		hyphenator.setErrorHandler(new ErrorHandler() {
			public void debug(String guard,String s) {}
			public void info(String s) {System.err.println(s);}
			public void warning(String s) {System.err.println("WARNING: "+s);}
			public void error(String s) {System.err.println("ERROR: "+s);}
			public void exception(String s,Exception e) {System.err.println("ERROR: "+s); e.printStackTrace(); }
			public boolean isDebugged(String guard) {return false;}
		});
		
		if(hyphenTable == null) {
			hyphenTable = "configs/hyphenation/hyph-"+language+".tex";
			
			File f = new File(hyphenTable);
			if(!f.exists()) {
				hyphenator = null;
				if(!errorInitMessageDisplayed)
					System.err.println("Hyphenation table "+hyphenTable+" does not exist");
				errorInitMessageDisplayed = true;
				return;
			}
		}
		
		BufferedReader table=null;
		try {
			table = new BufferedReader(
					   new InputStreamReader(
			                      new FileInputStream(hyphenTable), "UTF8"));
//			table=new java.io.BufferedInputStream(new FileInputStream(hyphenTable));
		} catch(java.io.IOException e) {
			System.err.println("cannot open hyphenation table "+hyphenTable+": "+e.toString());
			System.exit(1);
		}
		
		int[] codelist=new int[256];
		for(int i=0;i!=256;++i) codelist[i]=i;
		if(codeTable != null) {
			java.io.BufferedReader codes=null;
			try {
				codes=new java.io.BufferedReader(new java.io.FileReader(codeTable));
			} catch(java.io.IOException e) {
				System.err.println("cannot open code list"+codeTable+": "+e.toString());
				System.exit(1);
			}
			try {
				String line;
				while((line=codes.readLine())!=null) {
					java.util.StringTokenizer tokenizer=new java.util.StringTokenizer(line);
					String token;
					if(tokenizer.hasMoreTokens()) { // skip empty lines
						token=tokenizer.nextToken();
						if(!token.startsWith("%")) { // lines starting with % are comments
							int key=Integer.decode(token).intValue(), value=key;
							if(tokenizer.hasMoreTokens()) {
								token=tokenizer.nextToken();
								value=Integer.decode(token).intValue();
							}
							codelist[key]=value;
						}
					}
				}
				codes.close();
			} catch(java.io.IOException e) {
				System.err.println("error reading code list: "+e.toString());
				System.exit(1);
			}
		}
		
		try {
			hyphenator.loadTable(table,codelist);
			table.close();
		} catch(java.io.IOException e) {
			System.err.println("error loading hyphenation table: "+e.toString());
			System.exit(1);
		}
	}
	
	@Override
	public void process(JCas jCas) throws AnalysisEngineProcessException {			
		
		if(hyphenator == null) {
			initHyphenator(jCas.getDocumentLanguage());
		}
		
		if(hyphenator != null) {		
			for(Token t : JCasUtil.select(jCas, Token.class)) {
				String softHyphen = hyphenator.hyphenate(t.getCoveredText().trim());
				
				Hyphenation hyphens = new Hyphenation(jCas);
				hyphens.setValue(softHyphen);
				hyphens.setBegin(t.getBegin());
				hyphens.setEnd(t.getEnd());
				hyphens.addToIndexes();
			}
		}
		
	}
	
	
}
