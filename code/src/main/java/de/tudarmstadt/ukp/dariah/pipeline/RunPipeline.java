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
import org.apache.commons.configuration.ConfigurationUtils;
import org.apache.commons.configuration.FileSystem;
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
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;


public class RunPipeline {
	 
	private static String optLanguage = "en";
	private static String optInput;
	private static String optOutput;
	private static String optStartQuote;
	private static boolean optParagraphSingleLineBreak = false;
	
	private static boolean optSegmenter = true;
	private static Class<? extends AnalysisComponent> optSegmenterCls;
	private static String[] optSegmenterArguments;
	
	private static boolean optPOSTagger = true;
	private static Class<? extends AnalysisComponent> optPOSTaggerCls;
	private static String[] optPOSTaggerArguments;
	
	private static boolean optLemmatizer = true;
	private static Class<? extends AnalysisComponent> optLemmatizerCls;
	private static String[] optLemmatizerArguments;
	
	private static boolean optChunker = true;
	private static Class<? extends AnalysisComponent> optChunkerCls;
	private static String[] optChunkerArguments;
	
	private static boolean optMorphTagger = true;
	private static Class<? extends AnalysisComponent> optMorphTaggerCls;
	private static String[] optMorphTaggerArguments;
	
	private static boolean optDependencyParsing = true;
	private static Class<? extends AnalysisComponent> optDependencyParserCls;
	private static String[] optDependencyParserArguments;
	
	private static boolean optConstituencyParsing = true;
	private static Class<? extends AnalysisComponent> optConstituencyParserCls;
	private static String[] optConstituencyParserArguments;
	
	private static boolean optNER = true;
	private static Class<? extends AnalysisComponent> optNERCls;
	private static String[] optNERArguments;
	
	private static boolean optSRL = true;
	private static Class<? extends AnalysisComponent> optSRLCls;
	private static String[] optSRLArguments;
	
	
	private static void printConfiguration(String configFileName) {
		System.out.println("Input: "+optInput);
		System.out.println("Output: "+optOutput);
		System.out.println("Config: "+configFileName);
		
		System.out.println("Language: "+optLanguage);
		System.out.println("Start Quote: "+optStartQuote);
		System.out.println("Paragraph Single Line Break: "+optParagraphSingleLineBreak);
		
		System.out.println("Segmenter: "+optSegmenter);
		System.out.println("Segmenter: "+optSegmenterCls);
		
		System.out.println("POS-Tagger: "+optPOSTagger);
		System.out.println("POS-Tagger: "+optPOSTaggerCls);
		
		System.out.println("Lemmatizer: "+optLemmatizer);
		System.out.println("Lemmatizer: "+optPOSTaggerCls);
		
		System.out.println("Chunker: "+optChunker);
		System.out.println("Chunker: "+optChunkerCls);
		
		System.out.println("Morphology Tagging: "+optMorphTagger);
		System.out.println("Morphology Tagging: "+optMorphTaggerCls);
		
		System.out.println("Dependency Parsing: "+optDependencyParsing);
		System.out.println("Dependency Parsing: "+optDependencyParserCls);
		
		System.out.println("Constituency Parsing: "+optConstituencyParsing);
		System.out.println("Constituency Parsing: "+optConstituencyParserCls);
		
		System.out.println("Named Entity Recognition: "+optNER);		
		System.out.println("Named Entity Recognition: "+optNERCls);
		
		System.out.println("Semantic Role Labeling: "+optSRL);		
		System.out.println("Semantic Role Labeling: "+optSRLCls);
		
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
		optSegmenterArguments = config.getList("segmenterArguments", new LinkedList<String>()).toArray(new String[0]);
		
		optPOSTagger = config.getBoolean("usePosTagger", true);
		optPOSTaggerCls = getClassFromConfig(config, "posTagger");
		optPOSTaggerArguments = config.getList("posArguments", new LinkedList<String>()).toArray(new String[0]);
		
		optLemmatizer = config.getBoolean("useLemmatizer", true);
		optLemmatizerCls = getClassFromConfig(config, "lemmatizer");
		optLemmatizerArguments = config.getList("lemmatizerArguments", new LinkedList<String>()).toArray(new String[0]);
		
		optChunker = config.getBoolean("useChunker", true);
		optChunkerCls = getClassFromConfig(config, "chunker");
		optChunkerArguments = config.getList("chunkerArguments", new LinkedList<String>()).toArray(new String[0]);
		
		optMorphTagger = config.getBoolean("useMorphTagger", true);
		optMorphTaggerCls = getClassFromConfig(config, "morphTagger");
		optMorphTaggerArguments = config.getList("morphArguments", new LinkedList<String>()).toArray(new String[0]);
		
		optDependencyParsing = config.getBoolean("useDependencyParser", true);
		optDependencyParserCls = getClassFromConfig(config, "dependencyParser");
		optDependencyParserArguments = config.getList("dependencyParserArguments", new LinkedList<String>()).toArray(new String[0]);
		
		optConstituencyParsing = config.getBoolean("useConstituencyParser", true);
		optConstituencyParserCls = getClassFromConfig(config, "constituencyParser");
		optConstituencyParserArguments = config.getList("constituencyParserArguments", new LinkedList<String>()).toArray(new String[0]);
		
		optNER = config.getBoolean("useNER", true);
		optNERCls = getClassFromConfig(config, "ner");
		optNERArguments = config.getList("nerArguments", new LinkedList<String>()).toArray(new String[0]);
		
		optSRL = config.getBoolean("useSRL", true);
		optSRLCls = getClassFromConfig(config, "srl");
		optSRLArguments = config.getList("srlArguments", new LinkedList<String>()).toArray(new String[0]);
		
		
		optParagraphSingleLineBreak = config.getBoolean("splitParagraphOnSingleLineBreak", false);
		optStartQuote = config.getString("startingQuotes", "»\"„");
		
		if(config.containsKey("language"))
			optLanguage = config.getString("language");
	}

	@SuppressWarnings("static-access")
	private static boolean parseArgs(String[] args) throws ParseException {
		Options options = new Options();
		options.addOption("help", false, "print this message");
		
		Option lang = OptionBuilder.withArgName("lang")
				.hasArg()
				.withDescription("Language code for input file (default: "+optLanguage+")")
				.create("language");
		options.addOption(lang);
		
		Option input = OptionBuilder.withArgName("path")
				.hasArg()
				.withDescription("Input path")
				.create("input");
		options.addOption(input);
		
		Option output = OptionBuilder.withArgName("path")
				.hasArg()
				.withDescription("Output path")
				.create("output");
		options.addOption(output);
		
		Option configFile = OptionBuilder.withArgName("path")
				.hasArg()
				.withDescription("Config file")
				.create("config");
		options.addOption(configFile);
		
		
	
		CommandLineParser argParser = new BasicParser();
		CommandLine cmd = argParser.parse(options, args);
		if(cmd.hasOption("help")) {
			// automatically generate the help statement
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp( "pipeline.jar", options );
			return false;
		}
		if(cmd.hasOption(input.getOpt())) {
			optInput = cmd.getOptionValue(input.getOpt());
		} else {
			System.out.println("Input option required");
			return false;
		}
		if(cmd.hasOption(output.getOpt())) {
			optOutput = cmd.getOptionValue(output.getOpt());
		} else {
			System.out.println("Output option required");
			return false;
		}
		if(cmd.hasOption(lang.getOpt())) {
			optLanguage = cmd.getOptionValue(lang.getOpt());
		}
		
		
		return true;
	}
	
	public static void main(String[] args) throws Exception {
		
		if(!parseArgs(args)) {
			System.out.println("Usage: java -jar pipeline.jar -help");
			System.out.println("Usage: java -jar pipeline.jar -input <Input File> -output <Output Folder>");
			System.out.println("Usage: java -jar pipeline.jar -config <Config File> -input <Input File> -output <Output Folder>");
			return;
		}
		
		String defaultConfigFile = "configs/default";		
		String configFile = null;
		
		for(int i=0; i<args.length-1; i++) {
			if(args[i].equals("-config")) {
				configFile = args[i+1];	
				break;
			} 
		}

		
		if(configFile == null) { //No config file set
			String path = defaultConfigFile+"_"+optLanguage+".properties";			
			URL url = ConfigurationUtils.locate(FileSystem.getDefaultFileSystem(), null, path);
			
			File f = new File(url.getPath());
			if(f.exists()) {
				configFile = path;		
			} else {
				configFile = defaultConfigFile+".properties";
			}
		}
		
		
		
		parseConfig(configFile);	
		
		printConfiguration(configFile); 
		
		CollectionReaderDescription reader = createReaderDescription(
				TextReader.class,
				TextReader.PARAM_SOURCE_LOCATION, optInput,
				TextReader.PARAM_LANGUAGE, optLanguage);

		AnalysisEngineDescription paragraph = createEngineDescription(ParagraphSplitter.class,
				ParagraphSplitter.PARAM_SPLIT_PATTERN, (optParagraphSingleLineBreak) ? ParagraphSplitter.SINGLE_LINE_BREAKS_PATTERN : ParagraphSplitter.DOUBLE_LINE_BREAKS_PATTERN);	
		
		AnalysisEngineDescription seg = createEngineDescription(optSegmenterCls,
				(Object[])optSegmenterArguments);	
		
		AnalysisEngineDescription frenchQuotesSeg = createEngineDescription(PatternBasedTokenSegmenter.class,
			    PatternBasedTokenSegmenter.PARAM_PATTERNS, "+|[»«]");
		
		AnalysisEngineDescription quotesSeg = createEngineDescription(PatternBasedTokenSegmenter.class,
			    PatternBasedTokenSegmenter.PARAM_PATTERNS, "+|[\"\"]");
		
		AnalysisEngineDescription posTagger = createEngineDescription(optPOSTaggerCls,
				(Object[])optPOSTaggerArguments);	     
		
		AnalysisEngineDescription lemma = createEngineDescription(optLemmatizerCls,
				(Object[])optLemmatizerArguments);	
		
		AnalysisEngineDescription chunker = createEngineDescription(optChunkerCls,
				(Object[])optChunkerArguments);	
				
		AnalysisEngineDescription morph = createEngineDescription(optMorphTaggerCls,
				(Object[])optMorphTaggerArguments);	 
		
		AnalysisEngineDescription depParser = createEngineDescription(optDependencyParserCls,
				(Object[])optDependencyParserArguments); 	
		
		AnalysisEngineDescription constituencyParser = createEngineDescription(optConstituencyParserCls,
				(Object[])optConstituencyParserArguments);
				
		
		AnalysisEngineDescription ner = createEngineDescription(optNERCls,
				(Object[])optNERArguments); 
		
		AnalysisEngineDescription directSpeech =createEngineDescription(
				DirectSpeechAnnotator.class,
				DirectSpeechAnnotator.PARAM_START_QUOTE, optStartQuote
		);

		AnalysisEngineDescription srl = createEngineDescription(optSRLCls,
				(Object[])optSRLArguments); //Requires DKPro 1.8.0
		
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
				(optChunker) ? chunker : noOp,
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
