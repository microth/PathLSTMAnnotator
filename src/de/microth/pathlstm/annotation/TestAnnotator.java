package de.microth.pathlstm.annotation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import edu.illinois.cs.cogcomp.annotation.Annotator;
import edu.illinois.cs.cogcomp.annotation.AnnotatorException;
import edu.illinois.cs.cogcomp.annotation.BasicAnnotatorService;
import edu.illinois.cs.cogcomp.annotation.BasicTextAnnotationBuilder;
import edu.illinois.cs.cogcomp.core.datastructures.ViewNames;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.core.utilities.configuration.Configurator;
import edu.illinois.cs.cogcomp.core.utilities.configuration.ResourceManager;
import edu.illinois.cs.cogcomp.nlp.tokenizer.StatefulTokenizer;
import edu.illinois.cs.cogcomp.nlp.utility.TokenizerTextAnnotationBuilder;
import edu.illinois.cs.cogcomp.pipeline.common.PipelineConfigurator;
import edu.illinois.cs.cogcomp.pipeline.main.PipelineFactory;

public class TestAnnotator {
	public static void main(String[] args) {

		/* Example sentence to parse */
		List<String[]> sentences = new ArrayList<String[]>();
		sentences.add("I have another meeting at 5pm".split(" "));
		
		/* Set up Illinois pipeline */
		Properties nonDefaultProps = new Properties();
		nonDefaultProps.put(PipelineConfigurator.USE_POS.key, Configurator.TRUE);
		nonDefaultProps.put(PipelineConfigurator.USE_LEMMA.key, Configurator.TRUE);
		nonDefaultProps.put(PipelineConfigurator.USE_SHALLOW_PARSE.key, Configurator.FALSE);
		nonDefaultProps.put(PipelineConfigurator.USE_NER_CONLL.key, Configurator.FALSE);
		nonDefaultProps.put(PipelineConfigurator.USE_NER_ONTONOTES.key, Configurator.FALSE);
		nonDefaultProps.put(PipelineConfigurator.USE_STANFORD_DEP.key, Configurator.FALSE);
		nonDefaultProps.put(PipelineConfigurator.USE_STANFORD_PARSE.key, Configurator.FALSE);
		nonDefaultProps.put(PipelineConfigurator.USE_SRL_VERB.key, Configurator.FALSE);
		nonDefaultProps.put(PipelineConfigurator.USE_SRL_NOM.key, Configurator.FALSE);
   		ResourceManager rm = Configurator.mergeProperties(new PipelineConfigurator().getDefaultConfig(),
				new ResourceManager(nonDefaultProps));
   		
   		/* Illinois preprocessing */
   		BasicAnnotatorService as = null;   		
   		Map<String, Annotator> annotators;
		try {
			annotators = PipelineFactory.buildAnnotators(rm);
	   		//annotators.put(ViewNames.SRL_VERB, a);
	   		as = new BasicAnnotatorService(new TokenizerTextAnnotationBuilder(new StatefulTokenizer()), annotators, rm);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		TextAnnotation annotation = BasicTextAnnotationBuilder.createTextAnnotationFromTokens("", "", sentences);
		
		/* SRL annotator */
		Annotator srl = new PathLSTMAnnotator(ViewNames.SRL_VERB, new String[]{});
		try {
			srl.getView(annotation);
			System.out.println(annotation.getAvailableViews());
			System.err.println(annotation.getView(ViewNames.SRL_VERB));
		} catch (AnnotatorException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}
