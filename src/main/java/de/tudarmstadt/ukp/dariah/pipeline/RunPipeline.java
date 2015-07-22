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
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.component.CasDumpWriter;
import org.apache.uima.fit.component.NoOpAnnotator;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.resource.ResourceInitializationException;

import de.tudarmstadt.ukp.dkpro.core.io.conll.Conll2006Writer;
import de.tudarmstadt.ukp.dkpro.core.io.conll.Conll2012Writer;
import de.tudarmstadt.ukp.dkpro.core.io.text.TextReader;
import de.tudarmstadt.ukp.dkpro.core.matetools.MateLemmatizer;
import de.tudarmstadt.ukp.dkpro.core.matetools.MateMorphTagger;
import de.tudarmstadt.ukp.dkpro.core.matetools.MateParser;
import de.tudarmstadt.ukp.dkpro.core.matetools.MatePosTagger;
import de.tudarmstadt.ukp.dkpro.core.opennlp.OpenNlpPosTagger;
import de.tudarmstadt.ukp.dkpro.core.opennlp.OpenNlpSegmenter;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordNamedEntityRecognizer;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordParser;
import de.tudarmstadt.ukp.dkpro.core.tokit.ParagraphSplitter;
import de.tudarmstadt.ukp.dkpro.core.tokit.PatternBasedTokenSegmenter;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;


public class RunPipeline {
	
	private static String optLanguage = "de";
	private static String optInput;
	private static String optOutput;
	private static String optStartQuote = "»\"„";
	private static boolean optParagraphSingleLineBreak = false;
	
	private static boolean optPOSTagger = true;
	private static boolean optLemmatizer = true;
	private static boolean optMorphTagger = true;
	private static boolean optDependencyParsing = true;
	private static boolean optConstituencyParsing = true;
	private static boolean optNER = true;
	
	private static String[] parseConfigFile(String configFile, String[] cmdArgs) throws IOException {
		HashMap<String, String> properties = new HashMap<>();
		
		//Read the config file
		Properties javaProperties = new Properties();
		BufferedInputStream stream = new BufferedInputStream(new FileInputStream(configFile));
		javaProperties.load(stream);
		stream.close();
		
		for (final Entry<Object, Object> entry : javaProperties.entrySet()) {
	        properties.put((String) entry.getKey(), (String) entry.getValue());
	    }
		
		//Cmd Args overwrites config file
		for(int i=0;i<cmdArgs.length-1; i++) {
			if(cmdArgs[i].startsWith("-")) { 
				properties.put(cmdArgs[i], cmdArgs[i+1]);
				i++;
			}
		}
		
		//Map Hashmap for args array
		String[] newArgs = new String[2*properties.size()];
		int i=0;
		for (Map.Entry<String, String> entry : properties.entrySet()) {
			newArgs[i] = "-"+entry.getKey();
			newArgs[i+1] = entry.getValue();
			i += 2;
		}
		
		return newArgs;
	}


	@SuppressWarnings("static-access")
	private static boolean parseArgs(String[] args) throws ParseException {
		Options options = new Options();
		options.addOption("help", false, "print this message");
		
		Option paragraphSingleLineBreak = OptionBuilder.withArgName("True/False")
											.hasArg()
											.withDescription("Paragraphs are splitted along single line breaks (default: "+optParagraphSingleLineBreak+")")
											.create("paragraphSingleLineBreak");
		options.addOption(paragraphSingleLineBreak);
		
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
		
		Option startQuote = OptionBuilder.withArgName("quotes")
				.hasArg()
				.withDescription("Starting quoates (default: "+optStartQuote+")")							
				.create("quotestart");
		options.addOption(startQuote);
		
		Option configFile = OptionBuilder.withArgName("path")
				.hasArg()
				.withDescription("Config file")							
				.create("config");
		options.addOption(configFile);
		
		
		//For the components of the pipeline
		Option posTagger = OptionBuilder.withArgName("True/False").hasArg().withDescription("POS-tagging").create("pos_tagger");
		options.addOption(posTagger);
		
		Option lemmatizer = OptionBuilder.withArgName("True/False").hasArg().withDescription("Lemmatizer").create("lemmatizer");
		options.addOption(lemmatizer);
		
		Option morphTagger = OptionBuilder.withArgName("True/False").hasArg().withDescription("Morphology-tagging").create("morph_tagger");
		options.addOption(morphTagger);
		
		Option depParser = OptionBuilder.withArgName("True/False").hasArg().withDescription("Dependency Parsing").create("dep_parser");
		options.addOption(depParser);
		
		Option conParser = OptionBuilder.withArgName("True/False").hasArg().withDescription("Constituency Parsing").create("con_parser");
		options.addOption(conParser);
		
		Option ner = OptionBuilder.withArgName("True/False").hasArg().withDescription("Named Entity Recognition").create("ner");
		options.addOption(ner);
		

		
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
		
		if(cmd.hasOption(startQuote.getOpt())) {
			optStartQuote = cmd.getOptionValue(startQuote.getOpt());
		}		

		if(cmd.hasOption(paragraphSingleLineBreak.getOpt())) {			
			optParagraphSingleLineBreak = Boolean.parseBoolean(cmd.getOptionValue(paragraphSingleLineBreak.getOpt()));
		}
		
		//For the components of the pipeline
		if(cmd.hasOption(posTagger.getOpt())) {			
			optPOSTagger = Boolean.parseBoolean(cmd.getOptionValue(posTagger.getOpt()));
		}
		if(cmd.hasOption(lemmatizer.getOpt())) {			
			optLemmatizer = Boolean.parseBoolean(cmd.getOptionValue(lemmatizer.getOpt()));
		}
		if(cmd.hasOption(morphTagger.getOpt())) {			
			optMorphTagger = Boolean.parseBoolean(cmd.getOptionValue(morphTagger.getOpt()));
		}
		if(cmd.hasOption(depParser.getOpt())) {			
			optDependencyParsing = Boolean.parseBoolean(cmd.getOptionValue(depParser.getOpt()));
		}
		if(cmd.hasOption(conParser.getOpt())) {			
			optConstituencyParsing = Boolean.parseBoolean(cmd.getOptionValue(conParser.getOpt()));
		}
		if(cmd.hasOption(ner.getOpt())) {			
			optNER = Boolean.parseBoolean(cmd.getOptionValue(ner.getOpt()));
		}
		
		
		return true;
		
	}
	
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
	
	
	public static void main(String[] args) throws Exception {
		
		//Check if -config parameter is specified
		for(int i=0; i<args.length-1; i++) {
			if(args[i].equals("-config")) {
				String configFile = args[i+1];
				
				args = parseConfigFile(configFile, args);
			}
		}
		
		if(!parseArgs(args)) {
			System.out.println("Usage: java -jar pipeline.jar -input <Input File> -output <Output Folder>");
			System.out.println("Usage: java -jar pipeline.jar -help");
			System.out.println("Usage: java -jar pipeline.jar -config <Config File> -input <Input File> -output <Output Folder>");
			return;
		}
		
		printConfiguration();
		
			
		
		
		
		CollectionReaderDescription reader = createReaderDescription(
				TextReader.class,
				TextReader.PARAM_SOURCE_LOCATION, optInput,
				TextReader.PARAM_LANGUAGE, optLanguage);

		AnalysisEngineDescription paragraph = createEngineDescription(ParagraphSplitter.class,
				ParagraphSplitter.PARAM_SPLIT_PATTERN, (optParagraphSingleLineBreak) ? ParagraphSplitter.SINGLE_LINE_BREAKS_PATTERN : ParagraphSplitter.DOUBLE_LINE_BREAKS_PATTERN);	
		AnalysisEngineDescription seg = createEngineDescription(OpenNlpSegmenter.class);	
		AnalysisEngineDescription frenchQuotesSeg = createEngineDescription(PatternBasedTokenSegmenter.class,
			    PatternBasedTokenSegmenter.PARAM_PATTERNS, "+|[»«]");
		AnalysisEngineDescription quotesSeg = createEngineDescription(PatternBasedTokenSegmenter.class,
			    PatternBasedTokenSegmenter.PARAM_PATTERNS, "+|[\"\"]");
		AnalysisEngineDescription posTagger = createEngineDescription(MatePosTagger.class);	     
		AnalysisEngineDescription lemma = createEngineDescription(MateLemmatizer.class);	
		
		
		AnalysisEngineDescription morph = optLanguage.toLowerCase().equals("de") ? createEngineDescription(MateMorphTagger.class) : createEngineDescription(NoOpAnnotator.class);	 
		
		AnalysisEngineDescription depParser = createEngineDescription(MateParser.class); 		
		AnalysisEngineDescription constituencyParser = createEngineDescription(StanfordParser.class);
		
		AnalysisEngineDescription ner = createEngineDescription(StanfordNamedEntityRecognizer.class); 
		AnalysisEngineDescription directSpeech =createEngineDescription(
				DirectSpeechAnnotator.class,
				DirectSpeechAnnotator.PARAM_START_QUOTE, optStartQuote
		);

//		AnalysisEngineDescription srl = createEngineDescription(MateSemanticRoleLabeler.class); //Requires DKPro 1.8.0
		
		AnalysisEngineDescription writer = createEngineDescription(
				DARIAHWriter.class,
				DARIAHWriter.PARAM_TARGET_LOCATION, optOutput);
		
		AnalysisEngineDescription annWriter = createEngineDescription(
				AnnotationWriter.class
				);
		
		AnalysisEngineDescription noOp = createEngineDescription(NoOpAnnotator.class);
		
		
		
		SimplePipeline.runPipeline(reader, 
				paragraph,
				seg, 
				frenchQuotesSeg,
				quotesSeg,
				(optPOSTagger) ? posTagger : noOp, 
				(optLemmatizer) ? lemma : noOp,
				(optMorphTagger) ? morph : noOp,
				directSpeech,
				(optDependencyParsing) ? depParser : noOp,
				(optConstituencyParsing) ? constituencyParser : noOp,
				(optNER) ? ner : noOp,
//				srl, //Requires DKPro 1.8.0
				writer
//				annWriter
		);
		System.out.println("DONE");

	}

	

	

}
