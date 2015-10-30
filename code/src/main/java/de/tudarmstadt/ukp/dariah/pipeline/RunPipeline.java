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
import org.apache.tools.ant.DirectoryScanner;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_component.AnalysisComponent;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.component.CasDumpWriter;
import org.apache.uima.fit.component.NoOpAnnotator;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.resource.ResourceInitializationException;

import de.tudarmstadt.ukp.dariah.IO.AnnotationWriter;
import de.tudarmstadt.ukp.dariah.IO.DARIAHWriter;
import de.tudarmstadt.ukp.dariah.IO.GlobalFileStorage;
import de.tudarmstadt.ukp.dariah.IO.TextReaderWithInfo;
import de.tudarmstadt.ukp.dariah.IO.XmlReader;
import de.tudarmstadt.ukp.dariah.annotator.DirectSpeechAnnotator;
import de.tudarmstadt.ukp.dkpro.core.api.parameter.ComponentParameters;
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
import de.tudarmstadt.ukp.dkpro.core.treetagger.TreeTaggerPosTagger;

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
import java.util.Objects;
import java.util.Properties;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;


public class RunPipeline {
	
	private enum ReaderType {
		Text, XML
	}

	private static String optLanguage = "en";
	private static String optInput;
	private static String optOutput;
	private static String optStartQuote;
	private static ReaderType optReader = ReaderType.Text;
	
	private static boolean optParagraphSingleLineBreak = false;

	private static boolean optSegmenter = true;
	private static Class<? extends AnalysisComponent> optSegmenterCls;
	private static Object[] optSegmenterArguments;

	private static boolean optPOSTagger = true;
	private static Class<? extends AnalysisComponent> optPOSTaggerCls;
	private static Object[] optPOSTaggerArguments;

	private static boolean optLemmatizer = true;
	private static Class<? extends AnalysisComponent> optLemmatizerCls;
	private static Object[] optLemmatizerArguments;

	private static boolean optChunker = true;
	private static Class<? extends AnalysisComponent> optChunkerCls;
	private static Object[] optChunkerArguments;

	private static boolean optMorphTagger = true;
	private static Class<? extends AnalysisComponent> optMorphTaggerCls;
	private static Object[] optMorphTaggerArguments;

	private static boolean optDependencyParser = true;
	private static Class<? extends AnalysisComponent> optDependencyParserCls;
	private static Object[] optDependencyParserArguments;

	private static boolean optConstituencyParser = true;
	private static Class<? extends AnalysisComponent> optConstituencyParserCls;
	private static Object[] optConstituencyParserArguments;

	private static boolean optNER = true;
	private static Class<? extends AnalysisComponent> optNERCls;
	private static Object[] optNERArguments;

	private static boolean optSRL = true;
	private static Class<? extends AnalysisComponent> optSRLCls;
	private static Object[] optSRLArguments;


	private static void printConfiguration(String[] configFileNames) {
		System.out.println("Input: "+optInput);
		System.out.println("Output: "+optOutput);
		System.out.println("Config: "+StringUtils.join(configFileNames, ", "));

		System.out.println("Language: "+optLanguage);
		System.out.println("Reader: "+optReader);
		System.out.println("Start Quote: "+optStartQuote);
		System.out.println("Paragraph Single Line Break: "+optParagraphSingleLineBreak);

		System.out.println("Segmenter: "+optSegmenter);
		System.out.println("Segmenter: "+optSegmenterCls);
		printIfNotEmpty("Segmenter: ", optSegmenterArguments);

		System.out.println("POS-Tagger: "+optPOSTagger);
		System.out.println("POS-Tagger: "+optPOSTaggerCls);
		printIfNotEmpty("POS-Tagger: ", optPOSTaggerArguments);

		System.out.println("Lemmatizer: "+optLemmatizer);
		System.out.println("Lemmatizer: "+optLemmatizerCls);
		printIfNotEmpty("Lemmatizer: ", optLemmatizerArguments);

		System.out.println("Chunker: "+optChunker);
		System.out.println("Chunker: "+optChunkerCls);
		printIfNotEmpty("Chunker: ", optChunkerArguments);

		System.out.println("Morphology Tagging: "+optMorphTagger);
		System.out.println("Morphology Tagging: "+optMorphTaggerCls);
		printIfNotEmpty("Morphology Tagging: ", optMorphTaggerArguments);
		
		System.out.println("Named Entity Recognition: "+optNER);		
		System.out.println("Named Entity Recognition: "+optNERCls);
		printIfNotEmpty("Named Entity Recognition: ", optNERArguments);

		System.out.println("Dependency Parsing: "+optDependencyParser);
		System.out.println("Dependency Parsing: "+optDependencyParserCls);
		printIfNotEmpty("Dependency Parsing: ", optDependencyParserArguments);

		System.out.println("Constituency Parsing: "+optConstituencyParser);
		System.out.println("Constituency Parsing: "+optConstituencyParserCls);
		printIfNotEmpty("Constituency Parsing: ", optConstituencyParserArguments);

		System.out.println("Semantic Role Labeling: "+optSRL);		
		System.out.println("Semantic Role Labeling: "+optSRLCls);
		printIfNotEmpty("Semantic Role Labelingr: ", optSRLArguments);

	}

	private static void printIfNotEmpty(String text,
			Object[] arguments) {
		if(arguments != null && arguments.length > 0)
			System.out.println(text+StringUtils.join(arguments, ", "));
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
			optSegmenterArguments = parseParameters(config, "segmenterArguments");

		if(config.containsKey("usePosTagger"))
			optPOSTagger = config.getBoolean("usePosTagger", true);		
		if(config.containsKey("posTagger"))
			optPOSTaggerCls = getClassFromConfig(config, "posTagger");		
		if(config.containsKey("posTaggerArguments"))
			optPOSTaggerArguments = parseParameters(config, "posTaggerArguments");

		if(config.containsKey("useLemmatizer"))
			optLemmatizer = config.getBoolean("useLemmatizer", true);
		if(config.containsKey("lemmatizer"))
			optLemmatizerCls = getClassFromConfig(config, "lemmatizer");
		if(config.containsKey("lemmatizerArguments"))
			optLemmatizerArguments = parseParameters(config, "lemmatizerArguments");

		if(config.containsKey("useChunker"))
			optChunker = config.getBoolean("useChunker", true);
		if(config.containsKey("chunker"))
			optChunkerCls = getClassFromConfig(config, "chunker");
		if(config.containsKey("chunkerArguments"))
			optChunkerArguments = parseParameters(config, "chunkerArguments");

		if(config.containsKey("useMorphTagger"))
			optMorphTagger = config.getBoolean("useMorphTagger", true);
		if(config.containsKey("morphTagger"))
			optMorphTaggerCls = getClassFromConfig(config, "morphTagger");
		if(config.containsKey("morphTaggerArguments"))
			optMorphTaggerArguments = parseParameters(config, "morphTaggerArguments");

		if(config.containsKey("useDependencyParser"))
			optDependencyParser = config.getBoolean("useDependencyParser", true);
		if(config.containsKey("dependencyParser"))
			optDependencyParserCls = getClassFromConfig(config, "dependencyParser");
		if(config.containsKey("dependencyParserArguments"))
			optDependencyParserArguments = parseParameters(config, "dependencyParserArguments");

		if(config.containsKey("useConstituencyParser"))
			optConstituencyParser = config.getBoolean("useConstituencyParser", true);
		if(config.containsKey("constituencyParser"))
			optConstituencyParserCls = getClassFromConfig(config, "constituencyParser");
		if(config.containsKey("constituencyParserArguments"))
			optConstituencyParserArguments = parseParameters(config, "constituencyParserArguments");

		if(config.containsKey("useNER"))
			optNER = config.getBoolean("useNER", true);
		if(config.containsKey("ner"))
			optNERCls = getClassFromConfig(config, "ner");
		if(config.containsKey("nerArguments"))
			optNERArguments = parseParameters(config, "nerArguments");

		if(config.containsKey("useSRL"))
			optSRL = config.getBoolean("useSRL", true);
		if(config.containsKey("srl"))
			optSRLCls = getClassFromConfig(config, "srl");
		if(config.containsKey("srlArguments"))
			optSRLArguments = parseParameters(config, "srlArguments");

		if(config.containsKey("splitParagraphOnSingleLineBreak"))
			optParagraphSingleLineBreak = config.getBoolean("splitParagraphOnSingleLineBreak", false);
		if(config.containsKey("startingQuotes"))
			optStartQuote = config.getString("startingQuotes", "»\"„");

		if(config.containsKey("language"))
			optLanguage = config.getString("language");
	}

	/**
	 * Parses a parameter string that can be found in the .properties files.
	 * The parameterString is of the format parameterName1,parameterType1,parameterValue2,parameterName2,parameterType2,parameterValue2
	 * @param config 
	 * 
	 * @param parametersString
	 * @return Mapped paramaterString to an Object[] array that can be inputted to UIMA components
	 */
	private static Object[] parseParameters(Configuration config, String parameterName) throws ConfigurationException {
		
		LinkedList<Object> parameters = new LinkedList<>();
		List<Object> parameterList = config.getList(parameterName);
		
		
		
		if(parameterList.size()%3 != 0) {
			throw new ConfigurationException("Parameter String must be a multiple of 3 in the format: name, type, value. "+parameterName);
		}
		
		for(int i=0;i<parameterList.size(); i+=3) {
			String name = (String) parameterList.get(i+0);
			String type = ((String)parameterList.get(i+1)).toLowerCase();
			String value = (String)parameterList.get(i+2);
			
			Object obj;
			
			switch(type) {
				case "bool":
				case "boolean": obj = Boolean.valueOf(value); break;
				
				case "int":
				case "integer": obj = Integer.valueOf(value); break;
				
				default: obj = value;
			}
			
			parameters.add(name);
			parameters.add(obj);
		}
		
		return parameters.toArray(new Object[0]);
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
		
		Option reader = OptionBuilder.withArgName("reader")
				.hasArg()
				.withDescription("Either text (default) or xml")
				.create("reader");
		options.addOption(reader);



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
		
		if(cmd.hasOption(reader.getOpt())) {
			String readerParam = cmd.getOptionValue(reader.getOpt()).toLowerCase();
			
			if(readerParam.equals("text") || readerParam.equals("txt") || readerParam.equals("textreader") || readerParam.equals("txtreader") ) {
				optReader = ReaderType.Text;
			} else if(readerParam.equals("xml") || readerParam.equals("xmlreader")){
				optReader = ReaderType.XML;
			} else {
				System.out.println("The reader parameter is unknown: "+optReader);
				System.out.println("Valid argument values are: text, xml");
				return false;
			}
		}


		return true;
	}

	public static void main(String[] args)  {


		Date startDate = new Date();

		PrintStream ps;
		try {
			ps = new PrintStream("error.log");
//			System.setErr(ps);
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
		configFiles.add(configFolder+"default.properties");
		
		
		//Language dependent properties file
		String path = configFolder+"default_"+optLanguage+".properties";	
		File f = new File(path);
		if(f.exists()) {
			configFiles.add(path);		
		} else {
			System.out.println("Language config file: "+path+" not found");
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
				
				f = new File(path);
				if(f.exists()) {
					configFiles.add(path);		
				} else {
					System.out.println("Config file: "+configFile+" not found");
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
			
			// Read in the input files
			String defaultFileExtension = (optReader == ReaderType.XML) ? ".xml" : ".txt";
			
			GlobalFileStorage.getInstance().readFilePaths(optInput, defaultFileExtension);	
			
			System.out.println("Process "+GlobalFileStorage.getInstance().size()+" files");
			
			CollectionReaderDescription reader;
			
			if(optReader == ReaderType.XML) {
				reader = createReaderDescription(
						XmlReader.class,
						XmlReader.PARAM_LANGUAGE, optLanguage);
			} else {
				reader = createReaderDescription(
						TextReaderWithInfo.class,						
						TextReaderWithInfo.PARAM_LANGUAGE, optLanguage);
			}
			
		


			AnalysisEngineDescription paragraph = createEngineDescription(ParagraphSplitter.class,
					ParagraphSplitter.PARAM_SPLIT_PATTERN, (optParagraphSingleLineBreak) ? ParagraphSplitter.SINGLE_LINE_BREAKS_PATTERN : ParagraphSplitter.DOUBLE_LINE_BREAKS_PATTERN);	

			AnalysisEngineDescription seg = createEngineDescription(optSegmenterCls,
					optSegmenterArguments);	

			AnalysisEngineDescription frenchQuotesSeg = createEngineDescription(PatternBasedTokenSegmenter.class,
					PatternBasedTokenSegmenter.PARAM_PATTERNS, "+|[»«]");

			AnalysisEngineDescription quotesSeg = createEngineDescription(PatternBasedTokenSegmenter.class,
					PatternBasedTokenSegmenter.PARAM_PATTERNS, "+|[\"\"]");

			AnalysisEngineDescription posTagger = createEngineDescription(optPOSTaggerCls,
					optPOSTaggerArguments);	     

			AnalysisEngineDescription lemma = createEngineDescription(optLemmatizerCls,
					optLemmatizerArguments);	

			AnalysisEngineDescription chunker = createEngineDescription(optChunkerCls,
					optChunkerArguments);	

			AnalysisEngineDescription morph = createEngineDescription(optMorphTaggerCls,
					optMorphTaggerArguments);	 

			AnalysisEngineDescription depParser = createEngineDescription(optDependencyParserCls,					
					optDependencyParserArguments); 	

			AnalysisEngineDescription constituencyParser = createEngineDescription(optConstituencyParserCls,					
					optConstituencyParserArguments);
			
			AnalysisEngineDescription ner = createEngineDescription(optNERCls,
					optNERArguments); 

			AnalysisEngineDescription directSpeech =createEngineDescription(
					DirectSpeechAnnotator.class,
					DirectSpeechAnnotator.PARAM_START_QUOTE, optStartQuote
					);

			AnalysisEngineDescription srl = createEngineDescription(optSRLCls,
					optSRLArguments); //Requires DKPro 1.8.0

			AnalysisEngineDescription writer = createEngineDescription(
					DARIAHWriter.class,
					DARIAHWriter.PARAM_TARGET_LOCATION, optOutput,
					DARIAHWriter.PARAM_OVERWRITE, true);

			AnalysisEngineDescription annWriter = createEngineDescription(
					AnnotationWriter.class
					);


			

			AnalysisEngineDescription noOp = createEngineDescription(NoOpAnnotator.class);


			System.out.println("\nStart running the pipeline (this may take a while)...");

			while(!GlobalFileStorage.getInstance().isEmpty()) {
				try {
				SimplePipeline.runPipeline(
						reader,					
						paragraph,
						(optSegmenter) ? seg : noOp, 
						frenchQuotesSeg,
						quotesSeg,
						(optPOSTagger) ? posTagger : noOp, 
						(optLemmatizer) ? lemma : noOp,
						(optChunker) ? chunker : noOp,
						(optMorphTagger) ? morph : noOp,
						directSpeech,
						(optDependencyParser) ? depParser : noOp,
						(optConstituencyParser) ? constituencyParser : noOp,
						(optNER) ? ner : noOp,
						(optSRL) ? srl : noOp, //Requires DKPro 1.8.0
						writer
	//					,annWriter
						);
				} catch (OutOfMemoryError e) {
					System.out.println("Out of Memory at file: "+GlobalFileStorage.getInstance().getLastPolledFile().getAbsolutePath());
				}
			}

			
			
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
