import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.neo4j.graphalgo.CostEvaluator;
import org.neo4j.graphalgo.GraphAlgoFactory;
import org.neo4j.graphalgo.PathFinder;
import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphalgo.impl.centrality.EigenvectorCentralityArnoldi;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PathExpanders;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;


/**
 * Compute eigenvector centrality for a set of nodes.
 * 
 * Usage: java EigenvectorCentralitry -d <database dir> -f <input file> [-p] [-t]
 * 
 * @author dl
 *
 */
public class EigenvectorCentrality {

  private static final String SPLITTER = "\\s+";
  private static final String ID = "id";
  private static final Label NODE =  DynamicLabel.label("Node");
  private static final RelationshipType FRIEND = DynamicRelationshipType.withName("FRIEND");
  private static final String WEIGHT = "weight";

  public static void main(String args[]) throws FileNotFoundException {
    Options options = new Options();
    Option opt = new Option("f", true, "input node set file");
    opt.setRequired(true);
    options.addOption(opt);
    opt = new Option("d", true, "graph db directory");
    opt.setRequired(true);
    options.addOption(opt);
    opt = new Option("p", false, "print weights");
    opt.setRequired(false);
    options.addOption(opt);
    opt = new Option("t", false, "print total time");
    opt.setRequired(false);
    options.addOption(opt);   

    CommandLineParser parser = new BasicParser();
    CommandLine cmd = null;
    try {
      cmd = parser.parse(options, args);
    } catch (ParseException e) {
      System.out.println("Problem parsing command");
      System.out.println(e);
      System.exit(-1);
    }
    
    String graphFile = cmd.getOptionValue("f");
    String dbDirectory = cmd.getOptionValue("d");
    boolean print = cmd.hasOption('p');
    boolean printTime = cmd.hasOption('t');
    
    GraphDatabaseService graphDb = 
        new GraphDatabaseFactory().newEmbeddedDatabase(dbDirectory);
    
    Transaction tx = graphDb.beginTx();
    
    CostEvaluator evaluator = new CostEvaluator() {
      @Override
      public Object getCost(Relationship relationship, Direction direction) {
        return relationship.getProperty(WEIGHT);
      }
    };

    Set<Node> nodeSet = new HashSet<Node>();

    Scanner fileScanner = new Scanner(new File(graphFile));
    while (fileScanner.hasNextLine()) {
      String line = fileScanner.nextLine();
      String[] tokens = line.split(SPLITTER);
  
      ResourceIterator<Node> iterator = null;

      long id = Long.parseLong(tokens[0]);
      iterator = graphDb.findNodesByLabelAndProperty(NODE, ID, id).iterator();
      Node node = iterator.next();
      if (iterator.hasNext()) {
        throw new RuntimeException("Multiple nodes with ID:"+id);
      }
      nodeSet.add(node);
    }

    EigenvectorCentralityArnoldi centrality = 
        new EigenvectorCentralityArnoldi(Direction.BOTH, evaluator, 
//            nodeSet, relationshipSet, 0.0001);
            nodeSet, null, 0.0001);

    long startTime = System.currentTimeMillis();
  
    if (printTime) {
      System.out.println("Time="+(System.currentTimeMillis()-startTime));
    }

    fileScanner.close();
    tx.success();
    tx.close();
    graphDb.shutdown();
  }
}