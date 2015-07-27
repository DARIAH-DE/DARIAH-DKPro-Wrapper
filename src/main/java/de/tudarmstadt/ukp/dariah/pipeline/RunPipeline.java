package de.tudarmstadt.ukp.dariah.pipeline;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.*;
import static org.apache.uima.fit.factory.CollectionReaderFactory.*;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.uima.analysis_component.AnalysisComponent;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.component.CasDumpWriter;
import org.apache.uima.fit.component.NoOpAnnotator;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.resource.ResourceInitializationException;

import de.tudarmstadt.ukp.dkpro.core.io.conll.Conll2006Writer;
import de.tudarmstadt.ukp.dkpro.core.io.conll.Conll2009Writer;
import de.tudarmstadt.ukp.dkpro.core.io.conll.Conll2012Writer;
import de.tudarmstadt.ukp.dkpro.core.io.text.TextReader;
import de.tudarmstadt.ukp.dkpro.core.matetools.MateLemmatizer;
import de.tudarmstadt.ukp.dkpro.core.matetools.MateMorphTagger;
import de.tudarmstadt.ukp.dkpro.core.matetools.MateParser;
import de.tudarmstadt.ukp.dkpro.core.matetools.MatePosTagger;
import de.tudarmstadt.ukp.dkpro.core.matetools.MateSemanticRoleLabeler;
import de.tudarmstadt.ukp.dkpro.core.opennlp.OpenNlpPosTagger;
import de.tudarmstadt.ukp.dkpro.core.opennlp.OpenNlpSegmenter;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordNamedEntityRecognizer;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordParser;
import de.tudarmstadt.ukp.dkpro.core.tokit.ParagraphSplitter;
import de.tudarmstadt.ukp.dkpro.core.tokit.PatternBasedTokenSegmenter;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;


public class RunPipeline {
	
	private static String optLanguage = "de";
	private static String optInput;
	private static String optOutput;
	private static String optStartQuote = "»\"„";
	private static boolean optParagraphSingleLineBreak = false;
	
	private static boolean optSegmenter = true;
	private static Class<? extends AnalysisComponent> optSegmenterCls;
	
	private static boolean optPOSTagger = true;
	private static Class<? extends AnalysisComponent> optPOSTaggerCls;
	
	private static boolean optLemmatizer = true;
	private static Class<? extends AnalysisComponent> optLemmatizerCls;
	
	private static boolean optMorphTagger = true;
	private static Class<? extends AnalysisComponent> optMorphTaggerCls;
	
	private static boolean optDependencyParsing = true;
	private static Class<? extends AnalysisComponent> optDependencyParserCls;
	
	private static boolean optConstituencyParsing = true;
	private static Class<? extends AnalysisComponent> optConstituencyParserCls;
	
	private static boolean optNER = true;
	private static Class<? extends AnalysisComponent> optNerCls;
	
	private static boolean optSRL = true;
	private static Class<? extends AnalysisComponent> optSrlCls;
	
	
	
	private static void printConfiguration() {
		System.out.println("Input: "+optInput);
		System.out.println("Output: "+optOutput);
		System.out.println("Language: "+optLanguage);
		System.out.println("Start Quote: "+optStartQuote);
		System.out.println("Paragraph Single Line Break: "+optParagraphSingleLineBreak);
		
		System.out.println("POS-Tagger: "+optPOSTagger);
		System.out.println("Lemmatizer: "+optLemmatizer);
		System.out.println("Morphology Tagging: "+optMorphTagger);
		System.out.println("Dependency Parsing: "+optDependencyParsing);
		System.out.println("Constituency Parsing: "+optConstituencyParsing);
		System.out.println("Named Entity Recognition: "+optNER);		
	}
	
	public static Class<? extends AnalysisComponent> getClassFromConfig(Configuration config, String key) throws ClassNotFoundException {
		
		String entry = config.getString(key, "null");
		
		if(entry.toLowerCase().equals("null")) {
			return NoOpAnnotator.class;
		}
		
		return (Class<? extends AnalysisComponent>) Class.forName(entry);
		
	}
	
	private static void parseConfig(String configFile) throws ConfigurationException,
	ClassNotFoundException {
		Configuration config = new PropertiesConfiguration(configFile);
		
		
		
		optSegmenter = config.getBoolean("useSegmenter", true);
		optSegmenterCls = getClassFromConfig(config, "segmenter");
		
		optPOSTagger = config.getBoolean("usePosTagger", true);
		optPOSTaggerCls = getClassFromConfig(config, "posTagger");
		
		optLemmatizer = config.getBoolean("useLemmatizer", true);
		optLemmatizerCls = getClassFromConfig(config, "lemmatizer");
		
		optMorphTagger = config.getBoolean("useMorphTagger", true);
		optMorphTaggerCls = getClassFromConfig(config, "morphTagger");
		
		optDependencyParsing = config.getBoolean("useDependencyParser", true);
		optDependencyParserCls = getClassFromConfig(config, "dependencyParser");
		
		optConstituencyParsing = config.getBoolean("useConstituencyParser", true);
		optConstituencyParserCls = getClassFromConfig(config, "constituencyParser");
		
		optNER = config.getBoolean("useNER", true);
		optNerCls = getClassFromConfig(config, "ner");
		
		optSRL = config.getBoolean("useSRL", true);
		optSrlCls = getClassFromConfig(config, "srl");
		
		if(config.containsKey("language"))
			optLanguage = config.getString("language");
	}

	
	public static void main(String[] args) throws Exception {
		
		String defaultConfigFile = "/home/likewise-open/UKP/reimers/Dropbox/Doktor/DARIAH/java/Pipeline/src/main/resources/configs/default_de.properties";		
		String configFile = null;
		String optLanguage = null;
		
		for(int i=0; i<args.length-1; i++) {
			if(args[i].equals("-config")) {
				configFile = args[i+1];				
			} else if(args[i].equals("-language")) {
				optLanguage = args[i+1];
			}
		}

		
		if(configFile == null) { //No config file set
			
			switch(optLanguage) {
				default: configFile = defaultConfigFile; break;
			}
		}
		
		
		
		parseConfig(configFile);
		
		
		printConfiguration();
		
		CollectionReaderDescription reader = createReaderDescription(
				TextReader.class,
				TextReader.PARAM_SOURCE_LOCATION, optInput,
				TextReader.PARAM_LANGUAGE, optLanguage);

		AnalysisEngineDescription paragraph = createEngineDescription(ParagraphSplitter.class,
				ParagraphSplitter.PARAM_SPLIT_PATTERN, (optParagraphSingleLineBreak) ? ParagraphSplitter.SINGLE_LINE_BREAKS_PATTERN : ParagraphSplitter.DOUBLE_LINE_BREAKS_PATTERN);	
		AnalysisEngineDescription seg = createEngineDescription(optSegmenterCls);	
		AnalysisEngineDescription frenchQuotesSeg = createEngineDescription(PatternBasedTokenSegmenter.class,
			    PatternBasedTokenSegmenter.PARAM_PATTERNS, "+|[»«]");
		AnalysisEngineDescription quotesSeg = createEngineDescription(PatternBasedTokenSegmenter.class,
			    PatternBasedTokenSegmenter.PARAM_PATTERNS, "+|[\"\"]");
		AnalysisEngineDescription posTagger = createEngineDescription(optPOSTaggerCls);	     
		AnalysisEngineDescription lemma = createEngineDescription(optLemmatizerCls);	
		
		
		AnalysisEngineDescription morph = createEngineDescription(optMorphTaggerCls);	 
		
		AnalysisEngineDescription depParser = createEngineDescription(optDependencyParserCls); 		
		AnalysisEngineDescription constituencyParser = createEngineDescription(optConstituencyParserCls);
		
		AnalysisEngineDescription ner = createEngineDescription(optNerCls); 
		AnalysisEngineDescription directSpeech =createEngineDescription(
				DirectSpeechAnnotator.class,
				DirectSpeechAnnotator.PARAM_START_QUOTE, optStartQuote
		);

		AnalysisEngineDescription srl = createEngineDescription(optSrlCls); //Requires DKPro 1.8.0
		
		AnalysisEngineDescription writer = createEngineDescription(
				DARIAHWriter.class,
				DARIAHWriter.PARAM_TARGET_LOCATION, optOutput);
		
		AnalysisEngineDescription annWriter = createEngineDescription(
				AnnotationWriter.class
				);
		
		
		
		AnalysisEngineDescription noOp = createEngineDescription(NoOpAnnotator.class);
		
		
		
		SimplePipeline.runPipeline(reader, 
				paragraph,
				(optSegmenter) ? seg : noOp, 
				frenchQuotesSeg,
				quotesSeg,
				(optPOSTagger) ? posTagger : noOp, 
				(optLemmatizer) ? lemma : noOp,
				(optMorphTagger) ? morph : noOp,
				directSpeech,
				(optDependencyParsing) ? depParser : noOp,
				(optConstituencyParsing) ? constituencyParser : noOp,
				(optNER) ? ner : noOp,
				(optSRL) ? srl : noOp, //Requires DKPro 1.8.0
				writer
//				annWriter
		);
		System.out.println("DONE");

	}

	
	

	

}
