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
import java.util.Collection;
import java.util.LinkedList;
import java.util.TreeSet;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Paragraph;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;

/**
 * Checks that sentences do not span over more than one paragraph. If this is the case, it breaks ups the sentence
 * and introduces new sentences.
 * @author Nils Reimers
 *
 */
public class ParagraphSentenceCorrector extends JCasAnnotator_ImplBase {

	@Override
	public void process(JCas jCas) throws AnalysisEngineProcessException {
		Collection<Paragraph> paragraphs = JCasUtil.select(jCas, Paragraph.class);
		
		ArrayList<int[]> paragraphBoundaries = new ArrayList<>();
		
		for(Paragraph para : paragraphs) {
			paragraphBoundaries.add(new int[] {para.getBegin(), para.getEnd()});
		}
		
		Collection<Sentence> sentences = JCasUtil.select(jCas, Sentence.class);
		
		LinkedList<Sentence> sentencesToDelete = new LinkedList<>();
		LinkedList<Sentence> sentencesToAdd = new LinkedList<>();
		
		for(Sentence s : sentences) {
			int beginParagraphId = getParagraphId(s.getBegin(), paragraphBoundaries);
			int endParagraphId = getParagraphId(s.getEnd(), paragraphBoundaries);
			
			if(beginParagraphId != endParagraphId) {
				//Delete the sentence and set new sentence boundaries
				sentencesToDelete.add(s);
				
				//Add sentences for the paragraphs that lie in between
				for(int paraId=beginParagraphId; paraId<endParagraphId; paraId++) {
					Sentence newSentence = new Sentence(jCas);					
					newSentence.setBegin( paragraphBoundaries.get(paraId)[0] );
					newSentence.setEnd( paragraphBoundaries.get(paraId)[1] );
					sentencesToAdd.add(newSentence);
				}
				Sentence newSentence = new Sentence(jCas);			
				newSentence.setBegin( paragraphBoundaries.get(endParagraphId)[0] );
				newSentence.setEnd( s.getEnd() );
				sentencesToAdd.add(newSentence);
			}
			
		}
		
		//Remove wrong sentences and add new sentences to the annotations
		for(Sentence s : sentencesToDelete) {
			s.removeFromIndexes();
		}
		
		for(Sentence s : sentencesToAdd) {
			s.addToIndexes();
		}
		
	}

	private int getParagraphId(int position, ArrayList<int[]> paragraphBoundaries) {
		int idx = 0;
		for(int[] boundary : paragraphBoundaries) {
			if(boundary[0] <= position && position <= boundary[1]) {
				return idx;
			}
			idx++;
		}
		return 0;
	}

}
