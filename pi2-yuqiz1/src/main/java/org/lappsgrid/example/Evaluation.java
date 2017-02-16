package org.lappsgrid.example;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.lappsgrid.api.ProcessingService;
import org.lappsgrid.discriminator.Discriminators.Uri;
import org.lappsgrid.metadata.IOSpecification;
import org.lappsgrid.metadata.ServiceMetadata;
import org.lappsgrid.serialization.Data;
import org.lappsgrid.serialization.DataContainer;
import org.lappsgrid.serialization.Serializer;
import org.lappsgrid.serialization.lif.Annotation;
import org.lappsgrid.serialization.lif.Container;
import org.lappsgrid.serialization.lif.View;
import org.lappsgrid.vocabulary.Features;
/**
 * Evaluation: evaluate the system by checking the precision at N
 * @author yuqizhang
 *
 */
public class Evaluation implements ProcessingService{
  private String metadata;


  public Evaluation() {
      metadata = generateMetadata();
  }

  /**
   * Generate Metadata
   * Input format: TEXT/LAPPS
   * Output format: LAPPS
   * Annotation format: Uri.TOKEN
   * @return
   */
  private String generateMetadata() {
      // Create and populate the metadata object
      ServiceMetadata metadata = new ServiceMetadata();

      // Populate metadata using setX() methods
      metadata.setName(this.getClass().getName());
      metadata.setDescription("Evaluation");
      metadata.setVersion("1.0.0-SNAPSHOT");
      metadata.setVendor("http://www.lappsgrid.org");
      metadata.setLicense(Uri.APACHE2);

      // JSON for input information
      IOSpecification requires = new IOSpecification();
      requires.addFormat(Uri.TEXT);           // Plain text (form)
      requires.addFormat(Uri.LAPPS);            // LIF (form)
      requires.addLanguage("en");             // Source language
      requires.setEncoding("UTF-8");

      // JSON for output information
      IOSpecification produces = new IOSpecification();
      produces.addFormat(Uri.LAPPS);          // LIF (form) synonymous to LIF
      produces.addAnnotation(Uri.TOKEN);      // Tokens (contents)
      requires.addLanguage("en");             // Target language
      produces.setEncoding("UTF-8");

      // Embed I/O metadata JSON objects
      metadata.setRequires(requires);
      metadata.setProduces(produces);

      // Serialize the metadata to a string and return
      Data<ServiceMetadata> data = new Data<ServiceMetadata>(Uri.META, metadata);
      return data.asPrettyJson();
  }

  @Override
  /**
   * getMetadata simply returns metadata populated in the constructor
   */
  public String getMetadata() {
      return metadata;
  }

  @Override
  /**
   * Evaluate the system by comparing the output of answer scoring and the face and get precision
   */
  public String execute(String input) {
      // Step #1: Parse the input.
      Data data = Serializer.parse(input, Data.class);

      // Step #2: Check the discriminator
      final String discriminator = data.getDiscriminator();
      if (discriminator.equals(Uri.ERROR)) {
          // Return the input unchanged.
          return input;
      }

      // Step #3: Extract the text.
      Container container = null;
      if (discriminator.equals(Uri.TEXT)) {
          container = new Container();
          container.setText(data.getPayload().toString());
      }
      else if (discriminator.equals(Uri.LAPPS)) {
          container = new Container((Map) data.getPayload());
      }
      else {
          // This is a format we don't accept.
          String message = String.format("Unsupported discriminator type: %s", discriminator);
          return new Data<String>(Uri.ERROR, message).asJson();
      }

      // Step #4: Create a new View
      List<View> views = container.getViews();
      int lastview = views.size()-1;
      View oldview = container.getView(lastview);
      List<Annotation> annotations = oldview.getAnnotations();
      final HashMap<String, Double> scoremap = new HashMap<>();
      
      View view = container.newView();
      Annotation a = view.newAnnotation("Final Result", Uri.TOKEN, 0,0);
      
      
      for(int i = 0; i<annotations.size();i++){
        Annotation ans = annotations.get(i);
        
        double scorei = Double.parseDouble(ans.getFeature("Score"));
        String answer = ans.getFeature("Group");
        
        scoremap.put(answer, scorei);
      }
      
      
      List<String> anslist = new ArrayList<>(scoremap.keySet());
      Collections.sort(anslist, new Comparator<String>(){

        @Override
        public int compare(String o1, String o2) {
          return scoremap.get(o2).compareTo(scoremap.get(o1)) ;
        }
        
      });
      DecimalFormat df = new DecimalFormat("#.####");
      String result = "";
      for(String ans: anslist){
        result += df.format(scoremap.get(ans))+" "+ans+" ";
      }
      result += "\n";
      a.addFeature("Scorelist", result);
      View firstview = container.getView(0);
      List<Annotation> annotations1 = firstview.getAnnotations();
      Set<String> correctans = new HashSet<>();
      for(int i = 0; i<annotations1.size();i++){
        Annotation b = annotations1.get(i);
        if(b.getFeature("Type").equals("Answer")){
          if(b.getFeature("Score").equals("1")){
            correctans.add(b.getId());
          }
        }
      }
      double precision = 0;
      for(int i = 0; i<correctans.size();i++){
        if(correctans.contains(anslist.get(i))){
          precision++;
        }
      }
      precision = precision/correctans.size();
      a.addFeature("Precision", Double.toString(precision));
            
     
      

      // Step #6: Update the view's metadata. Each view contains metadata about the
      // annotations it contains, in particular the name of the tool that produced the
      // annotations.
      view.addContains(Uri.TOKEN, this.getClass().getName(), "evluation");

      // Step #7: Create a DataContainer with the result.
      data = new DataContainer(container);

      // Step #8: Serialize the data object and return the JSON.
      return data.asPrettyJson();
  }
}
