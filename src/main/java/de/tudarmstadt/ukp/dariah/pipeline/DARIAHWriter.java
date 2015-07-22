/*******************************************************************************
 * Copyright 2014
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
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

package de.tudarmstadt.ukp.dariah.pipeline;

import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.apache.uima.fit.util.JCasUtil.select;
import static org.apache.uima.fit.util.JCasUtil.selectCovered;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.TypeCapability;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.dkpro.core.api.io.JCasFileWriter_ImplBase;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.morph.Morpheme;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.parameter.ComponentParameters;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Paragraph;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.constituent.ROOT;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;
import de.tudarmstadt.ukp.dkpro.core.io.penntree.PennTreeNode;
import de.tudarmstadt.ukp.dkpro.core.io.penntree.PennTreeUtils;
import de.tudarmstadt.ukp.dariah.type.DirectSpeech;

/**
 * @author Nils Reimers
 */
@TypeCapability(inputs = { "de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData",
		"de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence",
		"de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token",
		"de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.morph.Morpheme",
		"de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS",
		"de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma",
"de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency" })
public class DARIAHWriter
extends JCasFileWriter_ImplBase
{
	private static final String UNUSED = "_";
	private static final int UNUSED_INT = -2;

	/**
	 * Name of configuration parameter that contains the character encoding used by the input files.
	 */
	public static final String PARAM_ENCODING = ComponentParameters.PARAM_SOURCE_ENCODING;
	@ConfigurationParameter(name = PARAM_ENCODING, mandatory = true, defaultValue = "UTF-8")
	private String encoding;

	public static final String PARAM_FILENAME_SUFFIX = "filenameSuffix";
	@ConfigurationParameter(name = PARAM_FILENAME_SUFFIX, mandatory = true, defaultValue = ".csv")
	private String filenameSuffix;

	public static final String PARAM_WRITE_POS = ComponentParameters.PARAM_WRITE_POS;
	@ConfigurationParameter(name = PARAM_WRITE_POS, mandatory = true, defaultValue = "true")
	private boolean writePos;

	public static final String PARAM_WRITE_MORPH = "writeMorph";
	@ConfigurationParameter(name = PARAM_WRITE_MORPH, mandatory = true, defaultValue = "true")
	private boolean writeMorph;

	public static final String PARAM_WRITE_LEMMA = ComponentParameters.PARAM_WRITE_LEMMA;
	@ConfigurationParameter(name = PARAM_WRITE_LEMMA, mandatory = true, defaultValue = "true")
	private boolean writeLemma;

	public static final String PARAM_WRITE_DEPENDENCY = ComponentParameters.PARAM_WRITE_DEPENDENCY;
	@ConfigurationParameter(name = PARAM_WRITE_DEPENDENCY, mandatory = true, defaultValue = "true")
	private boolean writeDependency;

	@Override
	public void process(JCas aJCas)	throws AnalysisEngineProcessException{
		
		System.out.println("Write jCAS to output file");
		
		PrintWriter out = null;
		try {
			out = new PrintWriter(new OutputStreamWriter(getOutputStream(aJCas, filenameSuffix), encoding));
			convert(aJCas, out);
		}
		catch (Exception e) {
			throw new AnalysisEngineProcessException(e);
		}
		finally {
			closeQuietly(out);
		}
		
		System.out.println("File written");
	}

	private void convert(JCas aJCas, PrintWriter aOut)
	{
		int paragraphId = 0, sentenceId = 0, tokenId = 0;

		aOut.printf("%s\n",StringUtils.join(getHeader(), "\t"));

		Map<Token, Collection<NamedEntity>> neCoveringMap = JCasUtil.indexCovering(aJCas, Token.class, NamedEntity.class);
		Map<Token, Collection<DirectSpeech>> directSpeechCoveringMap = JCasUtil.indexCovering(aJCas, Token.class, DirectSpeech.class);

		for(Paragraph para : select(aJCas, Paragraph.class)) {
			for (Sentence sentence : selectCovered(Sentence.class, para)) {
				HashMap<Token, Row> ctokens = new LinkedHashMap<Token, Row>();

				// Tokens
				List<Token> tokens = selectCovered(Token.class, sentence);

				// Check if we should try to include the FEATS in output
				List<Morpheme> morphology = selectCovered(Morpheme.class, sentence);
				boolean useFeats = tokens.size() == morphology.size();
				
				//Parsing information
				String[] parseFragments = null;
	            List<ROOT> root = selectCovered(ROOT.class, sentence);
	            if (root.size() == 1) {
	                PennTreeNode rootNode = PennTreeUtils.convertPennTree(root.get(0));
	                if ("ROOT".equals(rootNode.getLabel())) {
	                    rootNode.setLabel("TOP");
	                }
	                parseFragments = toPrettyPennTree(rootNode);
	            }
	            boolean useParseFragements = (parseFragments != null && parseFragments.length == tokens.size());
	            

				for (int i = 0; i < tokens.size(); i++) {
					Row row = new Row();
					row.paragraphId = paragraphId;
					row.sentenceId = sentenceId;
					row.tokenId = tokenId;
					row.token = tokens.get(i);
					
					if(useParseFragements) {
						row.parseFragment = parseFragments[i];
					}
					
					if (useFeats) {
						row.morphology = morphology.get(i);
					}

					// Named entities
					Collection<NamedEntity> ne = neCoveringMap.get(row.token);
					if(ne.size() > 0)
						row.ne = ne.toArray(new NamedEntity[0])[0];
					
					Collection<DirectSpeech> ds = directSpeechCoveringMap.get(row.token);
					if(ds.size() > 0)
						row.directSpeech = ds.toArray(new DirectSpeech[0])[0];

					ctokens.put(row.token, row);

					tokenId++;
				}

				// Dependencies
				for (Dependency rel : selectCovered(Dependency.class, sentence)) {
					ctokens.get(rel.getDependent()).deprel = rel;
				}

				
	            

				// Write sentence 
				for (Row row : ctokens.values()) {
					String[] output = getData(ctokens, row);					
					aOut.printf("%s\n",StringUtils.join(output, "\t"));
				}           
	            
				sentenceId++;
			}
			paragraphId++;
		}
	}

	private String[] getData(HashMap<Token, Row> ctokens, Row row) {
		String lemma = UNUSED;
		if (writeLemma && (row.token.getLemma() != null)) {
			lemma = row.token.getLemma().getValue();
		}

		String pos = UNUSED;
		String cpos = UNUSED;
		if (writePos && (row.token.getPos() != null)) {
			POS posAnno = row.token.getPos();
			pos = posAnno.getPosValue();
			if (!posAnno.getClass().equals(POS.class)) {
				cpos = posAnno.getClass().getSimpleName();
			}
			else {
				cpos = pos;
			}
		}

		int headId = UNUSED_INT;
		String deprel = UNUSED;
		if (writeDependency && (row.deprel != null)) {
			deprel = row.deprel.getDependencyType();
			headId = ctokens.get(row.deprel.getGovernor()).tokenId;
			if (headId == row.tokenId) {
				// ROOT dependencies may be modeled as a loop, ignore these.
				headId = -1;
			}
		}

		String head = UNUSED;
		if (headId != UNUSED_INT) {
			head = Integer.toString(headId);
		}

		String morphology = UNUSED;
		if (writeMorph && (row.morphology != null)) {
			morphology = row.morphology.getMorphTag();
		}


		String ne = UNUSED;

		if(row.ne != null) {
			ne = row.ne.getValue().substring(2); //Remove IOB tagging from Stanford Tagger
			//BIO-Tagging, B for beginning tag, I for all intermediate tags
			if(row.ne.getBegin() == row.token.getBegin()) {
				ne = "B-"+ne;
			} else {
				ne = "I-"+ne;
			}
		}

		String quoteMarker = "0";
		if(row.directSpeech != null){
			quoteMarker = "1";
		}

		
		String parseFragment = UNUSED;
		if(row.parseFragment != null)
			parseFragment = row.parseFragment;


		String[] output = new String[] {
				row.sectionId,
				Integer.toString(row.paragraphId),
				Integer.toString(row.sentenceId),
				Integer.toString(row.tokenId),
				Integer.toString(row.token.getBegin()),
				Integer.toString(row.token.getEnd()),
				row.token.getCoveredText(), 
				lemma, 
				cpos, 
				pos, 
				morphology, 
				head, 
				deprel,
				ne,
				quoteMarker,
				parseFragment
				
		};
		return output;
	}

	private String[] getHeader() {
		String[] header = new String[] {
				"SectionId",
				"ParagraphId",
				"SentenceId",
				"TokenId",
				"Begin",
				"End",
				"Token",
				"Lemma",
				"CPOS",
				"POS",
				"Morphology",
				"DependencyHead",
				"DependencyRelation",
				"NamedEntity",
				"QuoteMarker",
				"SyntaxTree"
		};

		return header;
	}
	
	public static String[] toPrettyPennTree(PennTreeNode aNode)
    {
        StringBuilder sb = new StringBuilder();
        toPennTree(sb, aNode);
        return sb.toString().trim().split("\n+");
    }

    private static void toPennTree(StringBuilder aSb, PennTreeNode aNode)
    {
        // This is a "(Label Token)"
        if (aNode.isPreTerminal()) {
            aSb.append("*");
        }
        else {
            aSb.append('(');
            aSb.append(aNode.getLabel());
            
            Iterator<PennTreeNode> i = aNode.getChildren().iterator();
            while (i.hasNext()) {
                PennTreeNode child = i.next();
                toPennTree(aSb, child);
                if (i.hasNext()) {
                    aSb.append("\n");
                }
            }
            
            aSb.append(')');
        }
    }



	private static final class Row {
		String sectionId = "_";
		int paragraphId;
		int sentenceId;
		int tokenId;
		Token token;
		Morpheme morphology;
		Dependency deprel;
		NamedEntity ne;
		DirectSpeech directSpeech;
		String parseFragment;
	}
}
