import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.lappsgrid.api.WebService;
import org.lappsgrid.discriminator.Discriminators.Uri;
import org.lappsgrid.example.*;
import org.lappsgrid.serialization.Data;
import org.lappsgrid.serialization.Serializer;
import org.lappsgrid.serialization.lif.Annotation;
import org.lappsgrid.serialization.lif.Container;
import org.lappsgrid.serialization.lif.View;
/**
 * QAPipeline: main function of the QA system
 * @author yuqizhang
 *
 */
public class QAPipeline extends Pipeline{
	
	/*
	 * Treat this as the entry point to your Web Service pipeline.
	 * The abstract class Pipeline is a collection of web services with getter and setter methods.
	 * Read more about abstract classes here - https://www.tutorialspoint.com/java/java_abstraction.htm
	 * 
	 * What other design patterns would fit better? (Think around the lines of Interface
	 * and abstract class implementing the interface)  
	 */
	
	@Override
	/**
	 * read input file as an array of string
	 */
	String readInput(String filePath) {
		/*
		 * Read in the file into a String
		 */
	  String result ="";
	  try {
      BufferedReader br = new BufferedReader(new FileReader(filePath));
      
      String qline = null;
      while((qline = br.readLine()) != null){
        result += (qline+'\n');
      }
      br.close();
	  } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
	  
	  
		return result;
	}

	@Override
	/**
	 * Write output to file. 
	 */
	void writeOutput(String filePath, String outputJson) {

	  Data data = Serializer.parse(outputJson, Data.class);

    // Step #2: Check the discriminator
    final String discriminator = data.getDiscriminator();

    // Step #3: Extract the text.
    Container container = null;

    container = new Container((Map) data.getPayload());

    // Step #4: Create a new View
    List<View> views = container.getViews();
    int lastview = views.size()-1;
    View oldview = container.getView(lastview);
    List<Annotation> annotations = oldview.getAnnotations();
    Annotation an = annotations.get(0);
    String result = an.getFeature("Scorelist");
    String p = an.getFeature("Precision")+"\n";
	  try {
      BufferedWriter bw = new BufferedWriter(new FileWriter(filePath));
      bw.write(p);
      bw.write(result);
      bw.close();

      
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
	  
	}	
	
	@Override
	void runPipeline() {
		
		 // One possible implementation could be this
		 String stageInput = getPipelineInput();
		 String intermediateOutput = "";
		 for(WebService service: getPipelineStages())
		   {
		 		intermediateOutput = service.execute(stageInput);
		 		stageInput = intermediateOutput;
		 	}
		 	setOutput(intermediateOutput);
		 
	}
	
	public static void main(String[] args) {
		File folder = new File(args[1]);
		String[] files = folder.list();
    

		for(String f: files){
		  String inputPath = args[1]+"/"+f;
	    Pipeline pl = new QAPipeline();
	    String input = pl.readInput(inputPath);
	    String af = f.replace('q', 'a');
	    String outputPath = args[2]+"/"+af;
	    pl.setPipelineInput(input);
	    pl.Stages = new ArrayList<WebService>();
	    pl.addService(new ElementAnnotation());
	    pl.addService(new TokenAnnotation());
	    pl.addService(new NgramAnnotation());
	    pl.addService(new AnswerScoring(Integer.parseInt(args[0])));
	    pl.addService(new Evaluation());
	    pl.runPipeline();
	    pl.writeOutput(outputPath, pl.getOutput());
		}
		
		/*
		 * A guideline for implementing this method
		 * 1) Implement the readInput() method
		 * 2) Create a Pipeline object and setup the Pipeline by initializing 
		 *    input to the pipeline, the web service sequence and run the pipeline
		 *    You can do this by:
		 *    - set the input to the pipeline using setPipelineInput()
		 *    - add the services into your pipeline using addService()
		 *      Eg: pipelineObject.addService(new TestElementAnnotation());
		 *    - call the runPipeline() method
		 * 3) Implement the writeOutput() method 
		 */

		 
	}

}
