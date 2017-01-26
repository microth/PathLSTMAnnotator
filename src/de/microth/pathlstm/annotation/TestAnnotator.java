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

