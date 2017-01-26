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
			// skip nominal predicates
			if(p.getPOS().startsWith("N")) continue;
			
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
