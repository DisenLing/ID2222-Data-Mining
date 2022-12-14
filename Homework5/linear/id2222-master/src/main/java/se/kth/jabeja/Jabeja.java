package se.kth.jabeja;

import org.apache.log4j.Logger;
import se.kth.jabeja.config.Config;
import se.kth.jabeja.config.NodeSelectionPolicy;
import se.kth.jabeja.io.FileIO;
import se.kth.jabeja.rand.RandNoGenerator;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class Jabeja {
  final static Logger logger = Logger.getLogger(Jabeja.class);
  private final Config config;
  private final HashMap<Integer/*id*/, Node/*neighbors*/> entireGraph;
  private final List<Integer> nodeIds;
  private int numberOfSwaps;
  private int round;
  private double T;
  private double Min_T = Math.pow(10,-5); // the Min temperature
  private double Max_T; // the Max temperature
  private final String Flag = "Cus"; // choose the annealing type and acceptance function
  private int reset_rounds = 0;
  private boolean resultFileCreated = false;

  //-------------------------------------------------------------------
  public Jabeja(HashMap<Integer, Node> graph, Config config) {
    this.entireGraph = graph;
    this.nodeIds = new ArrayList(entireGraph.keySet());
    this.round = 0;
    this.numberOfSwaps = 0;
    this.config = config;
    if (Flag.equals("Lin")){
      this.T = config.getTemperature();
    }else{
      this.T = 1;
      this.Max_T = 1;
      config.setDelta((float) 0.9);
    }

  }


  //-------------------------------------------------------------------
  public void startJabeja() throws IOException {
    for (round = 0; round < config.getRounds(); round++) {
      for (int id : entireGraph.keySet()) {
        sampleAndSwap(id);
      }
//      if (round % 500 == 0){
//        this.T = config.getTemperature();
//      }
      //one cycle for all nodes have completed.
      //reduce the temperature
      saCoolDown();
      report();
    }
  }

  private double compute_ap(double New_E, double Pre_E){
    if (Flag.equals("Exp")){
      return Math.exp((New_E-Pre_E) / T); // Exponential acceptance function
    } else if (Flag.equals("Cus")) {
      return 1 + (Math.exp((New_E-Pre_E) / T) - Math.exp((Pre_E-New_E) / T))/(Math.exp((New_E-Pre_E) / T) + Math.exp((Pre_E-New_E) / T));
      // come from the Tanh function
    }else {
      return 0; // Actually, there just return nothing
    }
  }

  /**
   * Simulated analealing cooling function
   */
  private void saCoolDown(){
    // TODO for second task
    if (Flag.equals("Lin")){
      if (T > 1)
        T -= config.getDelta();
      if (T < 1)
        T = 1;
    } else if (Flag.equals("Exp")) {
      T *= config.getDelta();
      if (T < Min_T)
        T = Min_T;
//      if (T == Min_T){
//          reset_rounds++;
//          if (reset_rounds == 100){
//            T = 1;
//            reset_rounds = 0;
//          }
//      }
    }
//    if (T > 1)
//      T -= config.getDelta();
//    if (T < 1)
//      T = 1;
  }

  /**
   * Sample and swap algorith at node p
   * @param nodeId
   */
  private void sampleAndSwap(int nodeId) {
    Node partner = null;
    Node nodep = entireGraph.get(nodeId);

    if (config.getNodeSelectionPolicy() == NodeSelectionPolicy.HYBRID
            || config.getNodeSelectionPolicy() == NodeSelectionPolicy.LOCAL) {
      // swap with random neighbors
      // TODO
      partner = findPartner(nodeId, getNeighbors(nodep));
    }

    if (config.getNodeSelectionPolicy() == NodeSelectionPolicy.HYBRID
            || config.getNodeSelectionPolicy() == NodeSelectionPolicy.RANDOM) {
      // if local policy fails then randomly sample the entire graph
      // TODO
      if (partner == null){
        partner = findPartner(nodeId, getSample(nodeId));
      }
    }

    // swap the colors
    // TODO
    if (partner != null){
      int color = partner.getColor();
      partner.setColor(nodep.getColor());
      nodep.setColor(color);
      numberOfSwaps ++;
    }
  }

  public Node findPartner(int nodeId, Integer[] nodes){

    Node nodep = entireGraph.get(nodeId);

    Node bestPartner = null;
    double highestBenefit = 0;

    // TODO
    for (Integer node : nodes) {
      Node target_node = entireGraph.get(node);
      //setting threshold
      int dqq = this.getDegree(target_node, target_node.getColor()); //get the color degree of target node
      int dpp = this.getDegree(nodep, nodep.getColor()); // get the color degree of current node

      double pre_energy = Math.pow(dqq, config.getAlpha()) + Math.pow(dpp, config.getAlpha()); // the previous energy

      int dqp = this.getDegree(target_node, nodep.getColor()); // After swapping, the color degree of target node
      int dpq = this.getDegree(nodep, target_node.getColor()); // After swapping, the color degree of current node

      double new_energy = Math.pow(dqp, config.getAlpha()) + Math.pow(dpq, config.getAlpha()); // the new energy

      if (Flag.equals("Lin")){
        if( new_energy * T > pre_energy && new_energy > highestBenefit) {
          highestBenefit = new_energy;
          bestPartner = target_node;
        }
      }else {
        Random random = new Random();
        double prob = random.nextDouble();
        double ap = compute_ap(new_energy,pre_energy); // compute the acceptance
        if (new_energy != pre_energy && ap > prob && ap > highestBenefit){ // if the requirement is satisfied
          highestBenefit = new_energy;
          bestPartner = target_node;
        }
      }
    }

    return bestPartner;
  }

  /**
   * The the degreee on the node based on color
   * @param node
   * @param colorId
   * @return how many neighbors of the node have color == colorId
   */
  private int getDegree(Node node, int colorId){
    int degree = 0;
    for(int neighborId : node.getNeighbours()){
      Node neighbor = entireGraph.get(neighborId);
      if(neighbor.getColor() == colorId){
        degree++;
      }
    }
    return degree;
  }

  /**
   * Returns a uniformly random sample of the graph
   * @param currentNodeId
   * @return Returns a uniformly random sample of the graph
   */
  private Integer[] getSample(int currentNodeId) {
    int count = config.getUniformRandomSampleSize();
    int rndId;
    int size = entireGraph.size();
    ArrayList<Integer> rndIds = new ArrayList<Integer>();

    while (true) {
      rndId = nodeIds.get(RandNoGenerator.nextInt(size));
      if (rndId != currentNodeId && !rndIds.contains(rndId)) {
        rndIds.add(rndId);
        count--;
      }

      if (count == 0)
        break;
    }

    Integer[] ids = new Integer[rndIds.size()];
    return rndIds.toArray(ids);
  }

  /**
   * Get random neighbors. The number of random neighbors is controlled using
   * -closeByNeighbors command line argument which can be obtained from the config
   * using {@link Config#getRandomNeighborSampleSize()}
   * @param node
   * @return
   */
  private Integer[] getNeighbors(Node node) {
    ArrayList<Integer> list = node.getNeighbours();
    int count = config.getRandomNeighborSampleSize();
    int rndId;
    int index;
    int size = list.size();
    ArrayList<Integer> rndIds = new ArrayList<Integer>();

    if (size <= count)
      rndIds.addAll(list);
    else {
      while (true) {
        index = RandNoGenerator.nextInt(size);
        rndId = list.get(index);
        if (!rndIds.contains(rndId)) {
          rndIds.add(rndId);
          count--;
        }

        if (count == 0)
          break;
      }
    }

    Integer[] arr = new Integer[rndIds.size()];
    return rndIds.toArray(arr);
  }


  /**
   * Generate a report which is stored in a file in the output dir.
   *
   * @throws IOException
   */
  private void report() throws IOException {
    int grayLinks = 0;
    int migrations = 0; // number of nodes that have changed the initial color
    int size = entireGraph.size();

    for (int i : entireGraph.keySet()) {
      Node node = entireGraph.get(i);
      int nodeColor = node.getColor();
      ArrayList<Integer> nodeNeighbours = node.getNeighbours();

      if (nodeColor != node.getInitColor()) {
        migrations++;
      }

      if (nodeNeighbours != null) {
        for (int n : nodeNeighbours) {
          Node p = entireGraph.get(n);
          int pColor = p.getColor();

          if (nodeColor != pColor)
            grayLinks++;
        }
      }
    }

    int edgeCut = grayLinks / 2;

    logger.info("round: " + round +
            ", edge cut:" + edgeCut +
            ", swaps: " + numberOfSwaps +
            ", migrations: " + migrations);

    saveToFile(edgeCut, migrations);
  }

  private void saveToFile(int edgeCuts, int migrations) throws IOException {
    String delimiter = "\t\t";
    String outputFilePath;

    //output file name
    File inputFile = new File(config.getGraphFilePath());
    outputFilePath = config.getOutputDir() +
            File.separator +
            inputFile.getName() + "_" +
            "NS" + "_" + config.getNodeSelectionPolicy() + "_" +
            "GICP" + "_" + config.getGraphInitialColorPolicy() + "_" +
            "T" + "_" + config.getTemperature() + "_" +
            "D" + "_" + config.getDelta() + "_" +
            "RNSS" + "_" + config.getRandomNeighborSampleSize() + "_" +
            "URSS" + "_" + config.getUniformRandomSampleSize() + "_" +
            "A" + "_" + config.getAlpha() + "_" +
            "R" + "_" + config.getRounds() + ".txt";

    if (!resultFileCreated) {
      File outputDir = new File(config.getOutputDir());
      if (!outputDir.exists()) {
        if (!outputDir.mkdir()) {
          throw new IOException("Unable to create the output directory");
        }
      }
      // create folder and result file with header
      String header = "# Migration is number of nodes that have changed color.";
      header += "\n\nRound" + delimiter + "Edge-Cut" + delimiter + "Swaps" + delimiter + "Migrations" + delimiter + "Skipped" + "\n";
      FileIO.write(header, outputFilePath);
      resultFileCreated = true;
    }

    FileIO.append(round + delimiter + (edgeCuts) + delimiter + numberOfSwaps + delimiter + migrations + "\n", outputFilePath);
  }
}
