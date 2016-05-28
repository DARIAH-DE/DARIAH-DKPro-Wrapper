/*******************************************************************************
 * Copyright 2015
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

import java.util.ArrayList;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;

import de.tudarmstadt.ukp.dariah.type.DirectSpeech;

import org.apache.uima.fit.descriptor.ConfigurationParameter;

/**
 * Annotates text inside quotes.
 * @author Nils Reimers
 */
public class DirectSpeechAnnotator extends JCasAnnotator_ImplBase{
	
	private final String QuotationMarkList="\"\"''„“„“„”„”‚‘‚‘‘’“”«»»«‹››‹";
	public static final String PARAM_START_QUOTE = "startingQuote";
	@ConfigurationParameter(name = PARAM_START_QUOTE, mandatory = true)
	protected String startingQuote;
	

	
	@Override
	public void process(JCas jCas) throws AnalysisEngineProcessException {			
		DirectSpeech speech;
		String DocText=jCas.getDocumentText();		
		Set<Character> startQuoteSet=new HashSet<Character>(getCharacterList(startingQuote));

		Map<Character,Character> QuotePairMap=getQuotePairMap(QuotationMarkList);
		boolean flag=false;
		int start,end;
		char startquote='\0',endquote='\0';
		start=end=0;
		for(int i=0;i<DocText.length();i++){
			if(startQuoteSet.contains(DocText.charAt(i)) && !flag){				
				start=i+1;
				startquote=DocText.charAt(i);
				endquote=QuotePairMap.get(startquote);
				flag=true;
			}
			else if(flag && DocText.charAt(i)==endquote){				
				end=i;
				flag=false;
				speech=new DirectSpeech(jCas,start,end);
				speech.addToIndexes();
				startquote=endquote='\0';
			}
		}
		
	}
	
	private Map<Character, Character> getQuotePairMap(String QuoteList) {
		Map<Character,Character> map=new HashMap<Character,Character>();
		char key,value;
		for(int i=0;i<QuoteList.length();i+=2){
			key=QuoteList.charAt(i);
			value=QuoteList.charAt(i+1);
			if(map.containsKey(key)){
				if(map.get(key)!=value){
					map.put(key, value);
				}
			}
			else{
				map.put(key, value);
			}
		}
		return map;
	}

	private List<Character> getCharacterList(String quotelist) {					
		List<Character> list=new ArrayList<Character>();
		for(char c:quotelist.toCharArray()){
			list.add(c);
		}
		return list;
	}
}
