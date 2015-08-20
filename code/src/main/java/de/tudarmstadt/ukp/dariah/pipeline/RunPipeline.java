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
import org.apache.commons.lang.StringUtils;
import org.apache.uima.UIMAException;
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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;


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
	
	
	private static void printConfiguration(String[] configFileNames) {
		System.out.println("Input: "+optInput);
		System.out.println("Output: "+optOutput);
		System.out.println("Config: "+StringUtils.join(configFileNames, ", "));
		
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
		
		if(config.containsKey("useSegmenter"))
			optSegmenter = config.getBoolean("useSegmenter", true);		
		if(config.containsKey("segmenter"))
			optSegmenterCls = getClassFromConfig(config, "segmenter");		
		if(config.containsKey("segmenterArguments"))
			optSegmenterArguments = config.getList("segmenterArguments", new LinkedList<String>()).toArray(new String[0]);
		
		if(config.containsKey("usePosTagger"))
			optPOSTagger = config.getBoolean("usePosTagger", true);		
		if(config.containsKey("posTagger"))
			optPOSTaggerCls = getClassFromConfig(config, "posTagger");		
		if(config.containsKey("posArguments"))
			optPOSTaggerArguments = config.getList("posArguments", new LinkedList<String>()).toArray(new String[0]);
		
		if(config.containsKey("useLemmatizer"))
			optLemmatizer = config.getBoolean("useLemmatizer", true);
		if(config.containsKey("lemmatizer"))
			optLemmatizerCls = getClassFromConfig(config, "lemmatizer");
		if(config.containsKey("lemmatizerArguments"))
			optLemmatizerArguments = config.getList("lemmatizerArguments", new LinkedList<String>()).toArray(new String[0]);
		
		if(config.containsKey("useChunker"))
			optChunker = config.getBoolean("useChunker", true);
		if(config.containsKey("chunker"))
			optChunkerCls = getClassFromConfig(config, "chunker");
		if(config.containsKey("chunkerArguments"))
			optChunkerArguments = config.getList("chunkerArguments", new LinkedList<String>()).toArray(new String[0]);
		
		if(config.containsKey("useMorphTagger"))
			optMorphTagger = config.getBoolean("useMorphTagger", true);
		if(config.containsKey("morphTagger"))
			optMorphTaggerCls = getClassFromConfig(config, "morphTagger");
		if(config.containsKey("morphArguments"))
			optMorphTaggerArguments = config.getList("morphArguments", new LinkedList<String>()).toArray(new String[0]);
		
		if(config.containsKey("useDependencyParser"))
			optDependencyParsing &= config.getBoolean("useDependencyParser", true);
		if(config.containsKey("dependencyParser"))
			optDependencyParserCls = getClassFromConfig(config, "dependencyParser");
		if(config.containsKey("dependencyParserArguments"))
			optDependencyParserArguments = config.getList("dependencyParserArguments", new LinkedList<String>()).toArray(new String[0]);
		
		if(config.containsKey("useConstituencyParser"))
			optConstituencyParsing &= config.getBoolean("useConstituencyParser", true);
		if(config.containsKey("constituencyParser"))
			optConstituencyParserCls = getClassFromConfig(config, "constituencyParser");
		if(config.containsKey("constituencyParserArguments"))
			optConstituencyParserArguments = config.getList("constituencyParserArguments", new LinkedList<String>()).toArray(new String[0]);
		
		if(config.containsKey("useNER"))
			optNER = config.getBoolean("useNER", true);
		if(config.containsKey("ner"))
			optNERCls = getClassFromConfig(config, "ner");
		if(config.containsKey("nerArguments"))
			optNERArguments = config.getList("nerArguments", new LinkedList<String>()).toArray(new String[0]);
		
		if(config.containsKey("useSRL"))
			optSRL = config.getBoolean("useSRL", true);
		if(config.containsKey("srl"))
			optSRLCls = getClassFromConfig(config, "srl");
		if(config.containsKey("srlArguments"))
			optSRLArguments = config.getList("srlArguments", new LinkedList<String>()).toArray(new String[0]);
		
		if(config.containsKey("splitParagraphOnSingleLineBreak"))
			optParagraphSingleLineBreak = config.getBoolean("splitParagraphOnSingleLineBreak", false);
		if(config.containsKey("startingQuotes"))
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
	
	public static void main(String[] args)  {
		
		Date startDate = new Date();
		
		PrintStream ps;
		try {
			ps = new PrintStream("error.log");
			System.setErr(ps);
		} catch (FileNotFoundException e) {
			System.out.println("Errors cannot be redirected");
		}
		
		
		
		
		try {
			if(!parseArgs(args)) {
				System.out.println("Usage: java -jar pipeline.jar -help");
				System.out.println("Usage: java -jar pipeline.jar -input <Input File> -output <Output Folder>");
				System.out.println("Usage: java -jar pipeline.jar -config <Config File> -input <Input File> -output <Output Folder>");
				return;
			}
		} catch (ParseException e) {			
			e.printStackTrace();
			System.out.println("Error when parsing command line arguments. Use\njava -jar pipeline.jar -help\n to get further information");
			System.out.println("See error.log for further details");
			return;
		}
		
		LinkedList<String> configFiles = new LinkedList<>();
		
		String configFolder = "configs/";
		String path = configFolder+"default_"+optLanguage+".properties";			
		URL url = ConfigurationUtils.locate(FileSystem.getDefaultFileSystem(), null, path);
		
		File f = new File(url.getPath());
		if(f.exists()) {
			configFiles.add(path);		
		} else {
			configFiles.add(configFolder+"default.properties");
		}
	
		
		String[] configFileArg = new String[0];		
		for(int i=0; i<args.length-1; i++) {
			if(args[i].equals("-config")) {
				configFileArg = args[i+1].split("[,;]");	
				break;
			} 
		}
		
		for(String configFile : configFileArg) {			
			
			f = new File(configFile);
			if(f.exists()) {
				configFiles.add(configFile);	
			} else {
				//Check in configs folder
				path = configFolder+configFile;
				url = ConfigurationUtils.locate(FileSystem.getDefaultFileSystem(), null, path);
				f = new File(url.getPath());
				if(f.exists()) {
					configFiles.add(path);		
				} else {
					System.err.println("Config file: "+configFile+" not found");
					return;
				}
			}			
		}
		
		
		for(String configFile : configFiles) {
			try {
				parseConfig(configFile);
			} catch (Exception e) {				
				e.printStackTrace();
				System.out.println("Exception when parsing config file: "+configFile);
				System.out.println("See error.log for further details");
			} 
		}
		
		printConfiguration(configFiles.toArray(new String[0])); 
		
		try {
			CollectionReaderDescription reader = createReaderDescription(
					TextReaderWithInfo.class,
					TextReaderWithInfo.PARAM_SOURCE_LOCATION, optInput,
					TextReaderWithInfo.PARAM_LANGUAGE, optLanguage);
	
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
			
			
			System.out.println("\nStart running the pipeline (this may take a while)...");
			
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
	//				,annWriter
			);
			
			Date enddate = new Date();
			double duration = (enddate.getTime() - startDate.getTime()) / (1000*60.0);
			
			System.out.println("---- DONE -----");
			System.out.printf("All files processed in %.2f minutes", duration);
		} catch(ResourceInitializationException e) {
			System.out.println("Error when initializing the pipeline.");	
			if(e.getCause() instanceof FileNotFoundException) {
				System.out.println("File not found. Maybe the input / output path is incorrect?");
				System.out.println(e.getCause().getMessage());
			}
			
			e.printStackTrace();
			System.out.println("See error.log for further details");
		} catch (UIMAException e) {			
			e.printStackTrace();
			System.out.println("Error in the pipeline.");			
			System.out.println("See error.log for further details");
		} catch (IOException e) {			
			e.printStackTrace();
			System.out.println("Error while reading or writing to the file system. Maybe some paths are incorrect?");	
			System.out.println("See error.log for further details");
		}
		
		
		
	}

	
	

	

}
