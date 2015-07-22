package de.tudarmstadt.ukp.dariah.pipeline;


import java.util.Collection;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.AnnotationBase;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.util.Level;
import org.apache.uima.fit.component.JCasConsumer_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;

import de.tudarmstadt.ukp.dariah.type.DirectSpeech;
import de.tudarmstadt.ukp.dkpro.core.api.coref.type.CoreferenceChain;
import de.tudarmstadt.ukp.dkpro.core.api.coref.type.CoreferenceLink;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Stem;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.chunk.Chunk;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.constituent.Constituent;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;




 
/**
 * Prints all annotation of a JCas.
 * @author Nils Reimers
 *
 */
public class AnnotationWriter extends JCasConsumer_ImplBase {
	public static final String PARAM_VIEW_USED = "ViewUsed";
	@ConfigurationParameter(name = PARAM_VIEW_USED, 
			defaultValue = "")
	private String viewName;


	public static final String LF = System.getProperty("line.separator");

	public void process(JCas j) throws AnalysisEngineProcessException {
		JCas jcas;
		if(viewName.isEmpty())
			jcas = j;
		else {
			try {
				jcas = j.getView(viewName);
			} catch (CASException e) {
				throw new AnalysisEngineProcessException();
			}
		}
		
		
		
		
		StringBuilder sb = new StringBuilder();
		sb.append("=== CAS ===");
		sb.append(LF);
		sb.append("-- Document Text --");
		sb.append(LF);
		sb.append(jcas.getDocumentText());
		sb.append(LF);
		sb.append("-- Annotations --");
		sb.append(LF);
		
		for (CoreferenceChain a : JCasUtil.select(jcas, CoreferenceChain.class)) {
			CoreferenceLink link = a.getFirst();
			
			sb.append(LF+LF+"CoreferenceChain:"+LF);
			while(link != null) {
				sb.append(link.getCoveredText()+LF);
				
				
				link = link.getNext();
			}
			sb.append(LF);
		}
		
	
		
		for (Annotation a : JCasUtil.select(jcas, Annotation.class)) {
			String value = "";
			
			
		
			if(!(a instanceof DirectSpeech))
				continue;
			

			// If the annotation is a lemma, find out what the lemmatized form is
			if (a instanceof Token)  {
				Token t = (Token) a;
				if(t.getPos() != null)
					value = ((Token) a).getPos().getPosValue();
				
				
			}
			
			if(a instanceof Stem)
				continue;
			
			if(a instanceof Chunk) {
				value = ((Chunk) a).getChunkValue();
				continue;
			}
			
	
			
			if(a instanceof NamedEntity) {
				value = ((NamedEntity) a).getValue();
				continue;
			}
			
			if(a instanceof Lemma) {
				value = ((Lemma) a).getValue();
				continue;
			}

			if(a instanceof Dependency) {
				value = ((Dependency) a).getDependencyType() + " - " + ((Dependency) a).getGovernor().getCoveredText();
				
			}
			
			if(a instanceof Constituent) {
				continue;
			}

			
			if(a instanceof CoreferenceLink) {
				
				CoreferenceLink link = ((CoreferenceLink)a);
				CoreferenceLink next = link.getNext();
				String nextText = (next != null) ? next.getCoveredText() + "(" + next.getBegin()+","+next.getEnd()+")" : "";
				value = ((CoreferenceLink)a).getReferenceRelation() + " " + ((CoreferenceLink)a).getReferenceType() + " - " + nextText;
			}
		
	
			
			// Print everything nicely
			sb.append(String.format("[%1$-5s] {%2$-6s} (%3$4d, %4$4d) %5$s", a
					.getType().getName(), value, a.getBegin(), a.getEnd(), a
					.getCoveredText()));
			sb.append(LF);

		}

		sb.append(LF);

		getContext().getLogger().log(Level.INFO, sb.toString());
	}

}