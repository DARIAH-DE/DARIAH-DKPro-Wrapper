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
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.resource.ResourceInitializationException;

import de.tudarmstadt.ukp.dkpro.core.io.conll.Conll2006Writer;
import de.tudarmstadt.ukp.dkpro.core.io.conll.Conll2012Writer;
import de.tudarmstadt.ukp.dkpro.core.io.tei.TeiReader;
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

import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.util.Properties;

public class RunTEIPipeline {
	
	private static String optLanguage = "de";
	private static String optInput;
	private static String optOutput;
	private static String optStartQuote = "»\"„";
	private static boolean optParagraphSingleLineBreak = false;

	@SuppressWarnings("static-access")
	private static boolean parseArgs(String[] args) throws ParseException {
		Options options = new Options();
		options.addOption("help", false, "print this message");
		
		Option paragraphSingleLineBreak = OptionBuilder.withDescription("Paragraphs are splitted along single line breaks")
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
		

		
		optParagraphSingleLineBreak = cmd.hasOption(paragraphSingleLineBreak.getOpt());
		
		return true;
		
	}
	
	private static void printConfiguration() {
		System.out.println("Input: "+optInput);
		System.out.println("Output: "+optOutput);
		System.out.println("Language: "+optLanguage);
		System.out.println("Start Quote: "+optStartQuote);
		System.out.println("Paragraph Single Line Break: "+optParagraphSingleLineBreak);
		
	}
	
	
	public static void main(String[] args) throws Exception {
		
		if(!parseArgs(args)) {
			System.out.println("Usage: java -jar pipeline.jar -input <Input File> -output <Output Folder>");
			System.out.println("Usage: java -jar pipeline.jar -help");
			return;
		}
		
		printConfiguration();
		
			
		
		
		
		CollectionReaderDescription reader = createReaderDescription(
				TeiReader.class,
				TeiReader.PARAM_SOURCE_LOCATION, optInput,
				TeiReader.PARAM_LANGUAGE, "de");

		AnalysisEngineDescription paragraph = createEngineDescription(ParagraphSplitter.class,
				ParagraphSplitter.PARAM_SPLIT_PATTERN, (optParagraphSingleLineBreak) ? ParagraphSplitter.SINGLE_LINE_BREAKS_PATTERN : ParagraphSplitter.DOUBLE_LINE_BREAKS_PATTERN);	
		AnalysisEngineDescription seg = createEngineDescription(OpenNlpSegmenter.class);	
		AnalysisEngineDescription frenchQuotesSeg = createEngineDescription(PatternBasedTokenSegmenter.class,
			    PatternBasedTokenSegmenter.PARAM_PATTERNS, "+|[»«]");
		AnalysisEngineDescription quotesSeg = createEngineDescription(PatternBasedTokenSegmenter.class,
			    PatternBasedTokenSegmenter.PARAM_PATTERNS, "+|[\"\"]");
		AnalysisEngineDescription tagger = createEngineDescription(MatePosTagger.class);	     
		AnalysisEngineDescription lemma = createEngineDescription(MateLemmatizer.class);	     
		AnalysisEngineDescription morph = createEngineDescription(MateMorphTagger.class);	     
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
		
		
		
		SimplePipeline.runPipeline(reader, 
				paragraph,
				seg, 
				frenchQuotesSeg,
				quotesSeg,
				tagger, 
				lemma,
				morph,
				directSpeech,
				depParser,
				constituencyParser,
				ner,
//				srl, //Requires DKPro 1.8.0
//				writer
				annWriter
		);
		System.out.println("DONE");

	}


	

}
