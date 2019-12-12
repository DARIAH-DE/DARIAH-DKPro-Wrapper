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
package de.tudarmstadt.ukp.dariah.pipeline;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReaderDescription;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.text.MessageFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import de.tudarmstadt.ukp.dkpro.core.io.xmi.XmiWriter;
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
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.io.IoBuilder;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_component.AnalysisComponent;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.component.NoOpAnnotator;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.resource.ResourceInitializationException;

import com.google.common.base.Joiner;

import de.tudarmstadt.ukp.dariah.IO.AnnotationWriter;
import de.tudarmstadt.ukp.dariah.IO.DARIAHWriter;
import de.tudarmstadt.ukp.dariah.IO.GlobalFileStorage;
import de.tudarmstadt.ukp.dariah.IO.TextReaderWithInfo;
import de.tudarmstadt.ukp.dariah.IO.XmlReader;
import de.tudarmstadt.ukp.dariah.annotator.DirectSpeechAnnotator;
import de.tudarmstadt.ukp.dariah.annotator.ParagraphSentenceCorrector;
import de.tudarmstadt.ukp.dkpro.core.tokit.ParagraphSplitter;
import de.tudarmstadt.ukp.dkpro.core.tokit.PatternBasedTokenSegmenter;


public class RunPipeline {

    private static boolean optWriteXmi = false;

	static {
		System.setProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager");
	}
	
	private static Logger logger = LogManager.getLogger(RunPipeline.class);
	private static PrintStream stdout = System.out;	// we'll redirect the originals later to the logging syste
	private static PrintStream stderr = System.err;
	
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
	
	private static boolean optHyphenation = true;
	private static Class<? extends AnalysisComponent> optHyphenationCls;
	private static Object[] optHyphenationArguments;
	
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
	
	private static boolean optCoref = true;
	private static Class<? extends AnalysisComponent> optCorefCls;
	private static Object[] optCorefArguments;
	
	

	
	private static boolean optResume = false;
	private static boolean optWriteAnn = false;

	private static void printConfiguration(String[] configFileNames) {
		logger.info("Input: "+optInput);
		logger.info("Output: "+optOutput);
		logger.info("Config: "+StringUtils.join(configFileNames, ", "));

		logger.info("Language: "+optLanguage);
		logger.info("Reader: "+optReader);
		logger.debug("Start Quote: "+optStartQuote);
		logger.debug("Paragraph Single Line Break: "+optParagraphSingleLineBreak);

		logger.debug("Segmenter: "+optSegmenter);
		logger.debug("Segmenter: "+optSegmenterCls);
		debugIfNotEmpty("Segmenter: ", optSegmenterArguments);

		logger.debug("POS-Tagger: "+optPOSTagger);
		logger.debug("POS-Tagger: "+optPOSTaggerCls);
		debugIfNotEmpty("POS-Tagger: ", optPOSTaggerArguments);

		logger.debug("Lemmatizer: "+optLemmatizer);
		logger.debug("Lemmatizer: "+optLemmatizerCls);
		debugIfNotEmpty("Lemmatizer: ", optLemmatizerArguments);

		logger.debug("Chunker: "+optChunker);
		logger.debug("Chunker: "+optChunkerCls);
		debugIfNotEmpty("Chunker: ", optChunkerArguments);

		logger.debug("Morphology Tagging: "+optMorphTagger);
		logger.debug("Morphology Tagging: "+optMorphTaggerCls);
		debugIfNotEmpty("Morphology Tagging: ", optMorphTaggerArguments);
		
		logger.debug("Hyphenation Algorithm: "+optHyphenation);
		logger.debug("Hyphenation Algorithm: "+optHyphenationCls);
		debugIfNotEmpty("Morphology Tagging: ", optHyphenationArguments);
		
		logger.debug("Named Entity Recognition: "+optNER);		
		logger.debug("Named Entity Recognition: "+optNERCls);
		debugIfNotEmpty("Hyphenation Algorithm: ", optNERArguments);

		logger.debug("Dependency Parsing: "+optDependencyParser);
		logger.debug("Dependency Parsing: "+optDependencyParserCls);
		debugIfNotEmpty("Dependency Parsing: ", optDependencyParserArguments);

		logger.debug("Constituency Parsing: "+optConstituencyParser);
		logger.debug("Constituency Parsing: "+optConstituencyParserCls);
		debugIfNotEmpty("Constituency Parsing: ", optConstituencyParserArguments);

		logger.debug("Semantic Role Labeling: "+optSRL);		
		logger.debug("Semantic Role Labeling: "+optSRLCls);
		debugIfNotEmpty("Semantic Role Labeling: ", optSRLArguments);
		
		logger.debug("Coreference Resolver: "+optCoref);		
		logger.debug("Coreference Resolver: "+optCorefCls);
		debugIfNotEmpty("Coreference Resolver: ", optCorefArguments);

	}

	private static void debugIfNotEmpty(String text,
			Object[] arguments) {
		if(arguments != null && arguments.length > 0)
			logger.debug(text+StringUtils.join(arguments, ", "));
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
		
		if(config.containsKey("useHyphenation"))
			optHyphenation = config.getBoolean("useHyphenation", true);
		if(config.containsKey("hyphenationAlgorithm"))
			optHyphenationCls = getClassFromConfig(config, "hyphenationAlgorithm");
		if(config.containsKey("hyphenationArguments"))
			optHyphenationArguments = parseParameters(config, "hyphenationArguments");

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
		
		if(config.containsKey("useCoref"))
			optCoref = config.getBoolean("useCoref", true);
		if(config.containsKey("coref"))
			optCorefCls = getClassFromConfig(config, "coref");
		if(config.containsKey("corefArguments"))
			optCorefArguments = parseParameters(config, "corefArguments");

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
	 * @param parameterName
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
		
		Option resume = OptionBuilder.withDescription("Already processed files will be skipped").create("resume");
		options.addOption(resume);
		
		Option writeAnnotations = OptionBuilder
				.withDescription("Debug option: Dump annotations to the log")
				.create("wann");
		options.addOption(writeAnnotations);

		Option writeXmi = OptionBuilder.withDescription("Also write XMI files").create("xmi");
		options.addOption(writeXmi);


		CommandLineParser argParser = new BasicParser();
		CommandLine cmd = argParser.parse(options, args);
		if(cmd.hasOption("help")) {
			// automatically generate the help statement
			HelpFormatter formatter = new HelpFormatter();
			PrintStream loggedOut = System.out;
			System.setOut(stdout);
			try {
				formatter.printHelp("ddw.jar",  options);
			} finally {
				System.setOut(loggedOut);
			}
			
			return false;
		}
		if(cmd.hasOption(input.getOpt())) {
			optInput = cmd.getOptionValue(input.getOpt());
		} else {
			logger.error("Input option required");
			return false;
		}
		if(cmd.hasOption(output.getOpt())) {
			optOutput = cmd.getOptionValue(output.getOpt());
		} else {
			logger.error("Output option required");
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
				logger.error("The reader parameter is unknown: "+optReader);
				logger.error("Valid argument values are: text, xml");
				return false;
			}
		}
		
		if(cmd.hasOption(resume.getOpt())) {
			optResume = true;
		}
		if (cmd.hasOption(writeAnnotations.getOpt())) {
			optWriteAnn = true;
		}
		if (cmd.hasOption(writeXmi.getOpt())) {
			optWriteXmi = true;
		}


		return true;
	}

	public static void main(String[] args)  {

		Date startDate = new Date();
		
		
		logger.debug("==== Starting new session ====");
		logger.debug("Arguments: " + Joiner.on(' ').join(args));
		
		logger.info(MessageFormat.format("Running on up to {0} cores, max heap is about {1} GB",
				Runtime.getRuntime().availableProcessors(),
				ManagementFactory.getMemoryPoolMXBeans().stream()
					.filter(pool -> pool.getType().equals(MemoryType.HEAP))
					.mapToLong(pool -> pool.getUsage().getMax())
					.sum() / (1024.0 * 1024 * 1024)));

		if (System.getProperty("sun.arch.data.model", "").equals("32"))
			logger.warn(MessageFormat.format("You are running a 32-bit java ({0}), try a 64-bit version for more memory", System.getProperty("java.vm.name")));

		System.setErr(IoBuilder.forLogger(logger.getName() + ".stderr").setLevel(Level.WARN) .setMarker(MarkerManager.getMarker("STDERR")).buildPrintStream());
		System.setOut(IoBuilder.forLogger(logger.getName() + ".stdout").setLevel(Level.DEBUG).setMarker(MarkerManager.getMarker("STDOUT")).buildPrintStream());
		
		try {
			if(!parseArgs(args)) {
				stdout.println("Usage: java -jar pipeline.jar -help");
				stdout.println("Usage: java -jar pipeline.jar -input <Input File or Folder> -output <Output Folder>");
				stdout.println("Usage: java -jar pipeline.jar -config <Config File> -input <Input File or Folder> -output <Output Folder>");
				return;
			}
		} catch (ParseException e) {			
			logger.error("Error when parsing command line arguments. Use\njava -jar pipeline.jar -help\n to get further information", e);
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
			logger.warn("Language config file: "+path+" not found.");
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
					logger.warn("Config file: "+configFile+" not found");
					return;
				}
			}			
		}


		boolean configFileParsed = false;
		for(String configFile : configFiles) {
			try {
				parseConfig(configFile);
				configFileParsed = true;
			} catch (Exception e) {				
				logger.error("Exception when parsing config file: "+configFile, e);
			} 
		}
		
		if (!configFileParsed) {
			File configFolderFile = new File(configFolder);
			logger.fatal(MessageFormat.format("None of the configuration files ({0}) could be parsed. Please make sure the folder with the config files exists as {1}.", Joiner.on(", ").join(configFiles), configFolderFile.getAbsolutePath()));
			System.exit(1);
		}

		printConfiguration(configFiles.toArray(new String[0]));
	
		

		try {
			
			// Read in the input files
			String defaultFileExtension = (optReader == ReaderType.XML) ? ".xml" : ".txt";
			
			GlobalFileStorage.getInstance().readFilePaths(optInput, defaultFileExtension, optOutput, optResume);	
			
			logger.info("Process "+GlobalFileStorage.getInstance().size()+" files");
			
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
			
			AnalysisEngineDescription paragraphSentenceCorrector = createEngineDescription(ParagraphSentenceCorrector.class);

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
			
			AnalysisEngineDescription hyphenation = createEngineDescription(optHyphenationCls,
					optHyphenationArguments);

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
			
			AnalysisEngineDescription coref = createEngineDescription(optCorefCls,
					optCorefArguments); //StanfordCoreferenceResolver.PARAM_POSTPROCESSING, true
			
			
			AnalysisEngineDescription writer = createEngineDescription(
					DARIAHWriter.class,
					DARIAHWriter.PARAM_TARGET_LOCATION, optOutput,
					DARIAHWriter.PARAM_OVERWRITE, true);

			AnalysisEngineDescription annWriter = createEngineDescription(
					AnnotationWriter.class
					);


			AnalysisEngineDescription xmiWriter = createEngineDescription(XmiWriter.class,
					XmiWriter.PARAM_TARGET_LOCATION, optOutput,
					XmiWriter.PARAM_OVERWRITE, true,
					XmiWriter.PARAM_TYPE_SYSTEM_FILE, new File(optOutput, "typesystem.xml"));

			AnalysisEngineDescription noOp = createEngineDescription(NoOpAnnotator.class);

			logger.info("Start running the pipeline (this may take a while)...");

			while(!GlobalFileStorage.getInstance().isEmpty()) {
				try {
				SimplePipeline.runPipeline(
						reader,					
						paragraph,
						(optSegmenter) ? seg : noOp, 
						paragraphSentenceCorrector,
						frenchQuotesSeg,
						quotesSeg,
						(optPOSTagger) ? posTagger : noOp, 
						(optLemmatizer) ? lemma : noOp,
						(optChunker) ? chunker : noOp,
						(optMorphTagger) ? morph : noOp,
						(optHyphenation) ? hyphenation : noOp,
						directSpeech,
						(optDependencyParser) ? depParser : noOp,
						(optConstituencyParser) ? constituencyParser : noOp,
						(optNER) ? ner : noOp,
						(optSRL) ? srl : noOp, //Requires DKPro 1.8.0
						(optCoref) ? coref : noOp,
						writer,
						optWriteXmi? xmiWriter : noOp,
						optWriteAnn? annWriter : noOp
						);
				} catch (OutOfMemoryError e) {
					logger.error("Out of Memory at file: "+GlobalFileStorage.getInstance().getLastPolledFile().getAbsolutePath(), e);
				} catch (AnalysisEngineProcessException e) {
					logger.error(MessageFormat.format("Processing failed for file: {0}, Message: {1}, Cause: {2}",
							GlobalFileStorage.getInstance().getLastPolledFile().getPath(),
							e.getMessage(),
							e.getCause() == null? "none" : e.getCause().getMessage()), e);
				}
			}

			
			
			Date enddate = new Date();
			double duration = (enddate.getTime() - startDate.getTime()) / (1000*60.0);

			logger.info("---- DONE -----");
			logger.info(MessageFormat.format("All files processed in {0,number,#.##} minutes", duration));
		} catch(ResourceInitializationException e) {
			logger.fatal("Error when initializing the pipeline." + 
				(e.getCause() instanceof FileNotFoundException? 
				   "\nFile not found. Maybe the input / output path is incorrect?\n" + e.getCause().getMessage() :"")
				, e);
		} catch (UIMAException e) {			
			logger.fatal("Error in the pipeline.", e);			
		} catch (IOException e) {			
			logger.fatal("Error while reading or writing to the file system. Maybe some paths are incorrect?", e);	
		}
	}
}
