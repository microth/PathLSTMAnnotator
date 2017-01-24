package de.microth.pathlstm.annotation;

import edu.illinois.cs.cogcomp.annotation.Annotator;
import edu.illinois.cs.cogcomp.annotation.AnnotatorException;
import edu.illinois.cs.cogcomp.annotation.BasicTextAnnotationBuilder;
import edu.illinois.cs.cogcomp.core.datastructures.IntPair;
import edu.illinois.cs.cogcomp.core.datastructures.ViewNames;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.Constituent;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.PredicateArgumentView;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.View;
import edu.illinois.cs.cogcomp.core.utilities.configuration.ResourceManager;
import edu.illinois.cs.cogcomp.srl.SRLProperties;
import edu.illinois.cs.cogcomp.srl.SemanticRoleLabeler;
import edu.illinois.cs.cogcomp.srl.config.SrlConfigurator;
import edu.illinois.cs.cogcomp.srl.core.SRLManager;
import edu.illinois.cs.cogcomp.srl.core.SRLType;
import edu.illinois.cs.cogcomp.srl.experiment.TextPreProcessor;
import edu.illinois.cs.cogcomp.srl.learn.SRLMulticlassInstance;
import edu.illinois.cs.cogcomp.srl.learn.SRLPredicateInstance;
import edu.illinois.cs.cogcomp.srl.learn.SRLPredicateStructure;
import java_cup.parse_action;
import se.lth.cs.srl.CompletePipeline;
import se.lth.cs.srl.corpus.Predicate;
import se.lth.cs.srl.corpus.Sentence;
import se.lth.cs.srl.corpus.Word;
import se.lth.cs.srl.corpus.Yield;
import se.lth.cs.srl.options.CompletePipelineCMDLineOptions;
import se.lth.cs.srl.options.FullPipelineOptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

public class PathLSTMAnnotator extends Annotator {
	CompletePipeline SRLpipeline;
		
	public PathLSTMAnnotator(String viewName, String[] requiredViews) {
        super(ViewNames.SRL_VERB, TextPreProcessor.requiredViews);
        
        // SRL pipeline options (currently hard-coded)
        String[] args = new String[]{
        		"eng", 
        		"-lemma", "models/CoNLL2009-ST-English-ALL.anna-3.3.lemmatizer.model",
        		"-parser", "models/CoNLL2009-ST-English-ALL.anna-3.3.parser.model",
        		"-tagger", "models/CoNLL2009-ST-English-ALL.anna-3.3.postagger.model",
        		"-srl", "models/srl-ACL2016-eng.model",
        		"-reranker", "-externalNNs",
        };
        CompletePipelineCMDLineOptions options = new CompletePipelineCMDLineOptions();
        options.parseCmdLineArgs(args);
        try {
			SRLpipeline = CompletePipeline.getCompletePipeline(options);
		} catch (ClassNotFoundException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private final static Logger log = LoggerFactory.getLogger(PathLSTMAnnotator.class);

	public static void main(String[] arguments) {
		/*ResourceManager rm = null;
        try {
            rm = new SrlConfigurator().getConfig( new PipelineConfigurator().getDefaultConfig() );
        } catch (Exception e) {
            e.printStackTrace();
            System.exit( -1 );
        }*/
        //String srlType;
		// If no second argument is provided it means we need all the SRL types
		//srlType = arguments.length == 1 ? null : arguments[1];
		
		String input;
		List<PathLSTMAnnotator> srlLabelers = new ArrayList<>();

        Properties props = new Properties();
        props.setProperty( SrlConfigurator.SRL_TYPE.key, SRLType.Verb.name() );
        props.setProperty( SrlConfigurator.INSTANTIATE_PREPROCESSOR.key, SrlConfigurator.TRUE );
        props.setProperty("PipelineConfig", "config/pipeline.properties");
        SRLProperties.initialize(new ResourceManager(props));
        //SrlConfigurator.mergeProperties( rm, new ResourceManager(props));

        srlLabelers.add(new PathLSTMAnnotator(ViewNames.SRL_VERB, new String[]{}));

		do {
			System.out.print("Enter text (underscore to quit): ");
			if(System.console()==null)
				input = "Let us try this .";
			else
				input = System.console().readLine().trim();
			if (input.equals("_"))
				return;

			if (!input.isEmpty()) {
				// XXX Assuming that all SRL types require the same views
				TextAnnotation ta;
				try {
			   		List<String[]> tokens = new LinkedList<>();
			   		tokens.add(input.split(" "));
					ta = BasicTextAnnotationBuilder.createTextAnnotationFromTokens("", "", tokens);
					
					//ta = TextPreProcessor.getInstance().preProcessText(input);
				} catch (Exception e) {
					log.error("Unable to pre-process the text:");
					e.printStackTrace();
					continue;
				}

				for (PathLSTMAnnotator srl : srlLabelers) {
					if (srlLabelers.size() > 1)
						System.out.println(srl.getViewName());

					PredicateArgumentView p;
					try {
						p = srl.getSRL(ta);
					} catch (Exception e) {
						log.error("Unable to produce SRL annotation:");
						e.printStackTrace();
						System.exit(1);
						continue;
					}

					System.out.println(p);
					System.out.println();
				}
			}
		} while (!input.equals("_"));
	}

	@Override
	public void initialize( ResourceManager rm )
	{
		/** TODO **/
    }

	public PredicateArgumentView getSRL(TextAnnotation ta) throws Exception {
		log.debug("Input: {}", ta.getText());

		String viewName = ViewNames.SRL_VERB;
		PredicateArgumentView pav = new PredicateArgumentView(viewName, 
				"PathLSTMGenerator", ta, 1.0);
		
		List<String> words = new LinkedList<String>();
		words.add("<ROOT>"); // dummy ROOT token
		words.addAll(Arrays.asList(ta.getTokens())); // pre-tokenized text
		
		// run SRL
		Sentence parsed = SRLpipeline.parse(words);
		
		for (Predicate p : parsed.getPredicates()) {
			IntPair predicateSpan = new IntPair(p.getIdx()-1, p.getIdx());
			String predicateLemma = p.getLemma();

			Constituent predicate = new Constituent("Predicate", viewName, ta,
					predicateSpan.getFirst(), predicateSpan.getSecond());
			predicate.addAttribute(PredicateArgumentView.LemmaIdentifier, predicateLemma);

			String sense = p.getSense();
			predicate.addAttribute(PredicateArgumentView.SenseIdentifer, sense);

			List<Constituent> args = new ArrayList<>();
			List<String> relations = new ArrayList<>();

			for (Word a : p.getArgMap().keySet()) {

				Set<Word> singleton = new TreeSet<Word>();
				String label = p.getArgumentTag(a);
				Yield y = a.getYield(p, label, singleton);
				IntPair span = new IntPair(y.first().getIdx()-1, y.last().getIdx());

				assert span.getFirst() <= span.getSecond() : ta;
				args.add(new Constituent(label, viewName, ta, span.getFirst(), span.getSecond()));
				relations.add(label);
			}

			pav.addPredicateArguments(predicate, args,
					relations.toArray(new String[relations.size()]),
					new double[relations.size()]);

		}

		return pav;
	}

	@Override
	public void addView(TextAnnotation ta) throws AnnotatorException {
		// Check if all required views are present
		try {
            View srlView = getSRL(ta);
            ta.addView( getViewName(), srlView);
		} catch (Exception e) {
			e.printStackTrace();
			throw new AnnotatorException(e.getMessage());
		}
	}

	@Override
	public String getViewName() {
		return ViewNames.SRL_VERB;

	}

}
