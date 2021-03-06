package com.codeglif.main;

import java.util.ArrayList;
import java.util.List;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XmlFormReportAnalyser {

	// http://www.drdobbs.com/jvm/easy-dom-parsing-in-java/231002580
	//http://www.javaspecialists.eu/archive/Issue163.html

    private Document root;
    private String file1;
    private String file2;
    private String formName;
    private ArrayList<ChangeFacts> extensionList;
    

	private Node mainFormNode;
    private ChangeFacts currentFacts;
    
    private Utilities util;

    public XmlFormReportAnalyser(Document doc){
    	this.util = new Utilities();
    	this.root = doc;
    	extensionList = new ArrayList<>();

    }
    /*
     * TODO
     * Priority is to abstract the way
     * how navigation is done on the nodes
     * 
     * Too much 'for' cycles
     * There is many code duplications
     */
    
    public void mainReportProcessor(){
    	
    	NodeList differenceMainNode = (NodeList) util.getNode("Differences", root.getChildNodes());
    	
    	for (int i = 0; i<differenceMainNode.getLength(); i++){
    		mainFormNode = differenceMainNode.item(i);
    		if (mainFormNode.getNodeName() == "Difference" && mainFormNode.getNodeType()==Node.ELEMENT_NODE)
    		{
    			formListProcessor(mainFormNode);
    		}
    	}
    }
    
    public void formListProcessor(Node differenceNode){
    	
    	//TODO
    	//change variable names for something more intuitive
    	//for the navigation between nodes like "onDifferenceNode"
    	formName = util.getName(util.getNodeAttr("target", differenceNode));
   
    	ChangeFacts changes = new ChangeFacts();
    	currentFacts = changes;
    	extensionList.add(currentFacts);
    	
    	currentFacts.setFormName(formName);
    	
    	Node newTagNode = util.getNode("New", differenceNode.getChildNodes());
    	if (newTagNode != null){
    		currentFacts.setTotalNewOp(getTotalNewOp(newTagNode));
    	}

    	Node diffTagNode = util.getNode("Diff", differenceNode.getChildNodes());
    	if (diffTagNode != null){
    		currentFacts.setTotalOperationalDiff(getOpperationalDiff(diffTagNode));
//	    	formExtList.get(formName).setTotalOperationalDiff(getOpperationalDiff(diffTagNode));
	    	getStructuralDiff(diffTagNode);
    	}

    	currentFacts.printValues();
    }
    
   
    
    /*
     * Node - <NewOperation>
     * get totals of new operations with necessary exceptions
     */
    
    protected Integer getNewOpNodeSize(String tagName, NodeList nodes){
    	int count = 0;
    	
    	/*
         * TODO
         * this class is obsolete. It is possible
         * to reduce the code lines for the simple
         * task
         */
        
        
    	for ( int x = 0; x < nodes.getLength(); x++ ) {
	        Node node = nodes.item(x);
	        if (node.getNodeName().equalsIgnoreCase(tagName) ){
	        	if (util.getNewOpValid(node.getAttributes().getNamedItem("name").getNodeValue())){
	        		count +=1;
	        	}
	        }
    	}
    	return count;
	}
    
    protected Integer getTotalNewOp(Node newTagNode){
    	return getNewOpNodeSize("NewOperation", newTagNode.getChildNodes());	
    }
    
    /*
     * NODE - <Structural>
     * Deals with Structural differences
     */
    
    protected void getStructuralDiff(Node newTagNode){
    	getStructuralDiffSize("StructuralDiff", newTagNode.getChildNodes());	

    }
        
    protected void getStructuralDiffSize(String tagName, NodeList nodes){
    	/*
    	 * TODO
    	 * add rules for each node name
    	 * like Lov, Block Item
    	 * 
    	 */
    	
    	for ( int x = 0; x < nodes.getLength(); x++ ) {
    		 Node currentNode = nodes.item(x);
    		 if (currentNode.getNodeName() == tagName && currentNode.getNodeType() == Node.ELEMENT_NODE){
    			 NodeList diffsNode = (NodeList)currentNode.getChildNodes();
    			 String nodeName = currentNode.getAttributes().getNamedItem("name").getNodeValue();
    			 
    			 if (nodeName.equals("LOV")){
    				 int totalNewLov = isLovCountable(diffsNode);
    				 currentFacts.setTotalNewLov(totalNewLov);
    			 }
    			 else if ( nodeName.equals("Canvas")){
    				 int totalNewCanvas = isCanvasCountable(diffsNode);
    				 currentFacts.setTotalNewCanvas(totalNewCanvas);
    			 	}
    			 else if (nodeName.equals("Block_Item") ){
    				 isItemPropertyCountable(currentNode);
    				 isBlockCountable(currentNode);
    			 }
			 }
		 }
    	}
    
      private void isBlockCountable(Node currentNode) {
    	String blockName;
    	Integer count=0;
    	
		if (util.getNodeSize("Element",util.getNode("File2",(NodeList)currentNode).getChildNodes()) > 0)
			for (int x = 0; x < util.getNode("File2",(NodeList)currentNode).getChildNodes().getLength() ; x++){
				if (util.getNode("File2",(NodeList)currentNode).getChildNodes().item(x).getNodeName() == "Element" &&
					  util.getNode("File2",(NodeList)currentNode).getChildNodes().item(x).getAttributes().getNamedItem("diffType").getNodeValue().equals("Missing") &&
					  util.getNode("File2",(NodeList)currentNode).getChildNodes().item(x).getAttributes().getNamedItem("node").getNodeValue().equals("Block")){
						currentFacts.setTotalNewBlock(count+=1);
						blockName = util.getNode("File2",(NodeList)currentNode).getChildNodes().item(x).getAttributes().getNamedItem("value").getNodeValue();
						currentFacts.setTotalNewItems(newBlockItems(currentNode, blockName));
					}
			}
	}

	private Integer newBlockItems(Node currentNode, String blockName) {
		
		List<String> newItems = new ArrayList<>();
		int count = 0;
		
		for (int x = 0; x < util.getNodeSize("NewOperation",(NodeList)util.getNode("New",mainFormNode.getChildNodes()).getChildNodes()) ; x++){
			if (util.getNode("New",mainFormNode.getChildNodes()).getChildNodes().item(x).getNodeName() == "NewOperation"){
				String newOpName = util.getNode("New",mainFormNode.getChildNodes()).getChildNodes().item(x).getAttributes().getNamedItem("name").getNodeValue();
				if (util.getNewOpValid(newOpName) && util.parsingNewOperationValue(newOpName).length >= 4 ){
					String blockNameParsed = util.getNewBlockName(newOpName);
					String itemNameParsed = util.getNewItemName(newOpName);
					if (blockName.equals(blockNameParsed) && !newItems.contains(itemNameParsed)){
						newItems.add(itemNameParsed);
						count += 1;
					}
				}
			}
		}
		return count;
	}

	private Integer isLovCountable(NodeList nodeItem){
    	  
    	  int count = 0;
    	  /*
    	   * Condition eliminated elements
    	   */
    	  if (util.getNodeSize("Element",util.getNode("File1",(NodeList)nodeItem).getChildNodes()) > 0  ){
    		  for (int x = 0; x < util.getNode("File1",(NodeList)nodeItem).getChildNodes().getLength() ; x++){
				  if (util.getNode("File1",(NodeList)nodeItem).getChildNodes().item(x).getNodeName() == "Element" &&
						  util.getNode("File2",(NodeList)nodeItem).getChildNodes().item(x).getAttributes().getNamedItem("diffType").getNodeValue().equals("Missing")){
					  count += 1;
				  }
    		  }
    	  }
    	  /*
    	   * Condition for new elements added or value change
    	   */
    	  if (util.getNodeSize("Element",util.getNode("File2",(NodeList)nodeItem).getChildNodes()) > 0  ){
			  for (int x = 0; x < util.getNode("File2",(NodeList)nodeItem).getChildNodes().getLength() ; x++){
				  if (util.getNode("File2",(NodeList)nodeItem).getChildNodes().item(x).getNodeName() == "Element"){
					  count += 1;
				  }
			  }
		  
    	  }
    	  return count;
    }
    private void isItemPropertyCountable(Node nodeItem){
    	
    	Integer count;
    	count = runItemPropertyFile("File1", nodeItem);
    	count += runItemPropertyFile("File2", nodeItem);
		this.currentFacts.setTotalPropDiff(count);
    }
    
    private int runItemPropertyFile(String nodeId, Node nodeItem){
    	
    	int count = 0;
    	if (util.getNode("Element",util.getNode(nodeId,(NodeList)nodeItem).getChildNodes()) != null ){
			  for (int x = 0; x < util.getNode(nodeId,(NodeList)nodeItem).getChildNodes().getLength() ; x++){
				  
				  if (util.getNode(nodeId,(NodeList)nodeItem).getChildNodes().item(x).getNodeName() == "Element" &&
						  util.getNode(nodeId,(NodeList)nodeItem).getChildNodes().item(x).getAttributes().getNamedItem("diffType").getNodeValue().equals("Missing")){
					  count += 1;
				  }
				  /*
				   * this else/if is poor evaluated
				   * the goal is to count each individual DiffValue exist
				   * in File1 and File2
				   * 
				   * The best way is to verify if the item is declared in
				   * File1 and File2.
				   */
				  else if (nodeId.equals("File1") && 
						  util.getNode(nodeId,(NodeList)nodeItem).getChildNodes().item(x).getNodeName() == "Element" &&
						  util.getNode(nodeId,(NodeList)nodeItem).getChildNodes().item(x).getAttributes().getNamedItem("diffType").getNodeValue().equals("DiffValue")){
					  count += 1;
				  }
    
			}
    	}
    	return count;
    }
  
    private int isCanvasCountable(NodeList diffsNode){
	
	    int count = 0;
		if (util.getNodeSize("Element",util.getNode("File2",(NodeList)diffsNode).getChildNodes()) > 0  ){
		  
		  for (int x = 0; x < util.getNode("File2",(NodeList)diffsNode).getChildNodes().getLength() ; x++){
		  
			  if (util.getNode("File2",(NodeList)diffsNode).getChildNodes().item(x).getNodeName() == "Element" &&
					  util.getNode("File2",(NodeList)diffsNode).getChildNodes().item(x).getAttributes().getNamedItem("diffType").getNodeValue().equals("Missing") &&
					  util.getNode("File2",(NodeList)diffsNode).getChildNodes().item(x).getAttributes().getNamedItem("node").getNodeValue().equals("Canvas")){
					  count += 1;
				  }
		  	}
			  
	  	  }
	  	  return count;
	    }
    
    

    /*
     * NODE - <Operational>
     * Deals with Operational differences
     */
    
    protected Integer getOpperationalDiff(Node newTagNode){
    	return getOpperationalDiffSize("OperationDiff", newTagNode.getChildNodes());
    }
   
    protected Integer getOpperationalDiffSize(String tagName, NodeList nodes){
    	
    	int count = 0;
    	
    	/*
    	 * TODO
    	 * It is possible to abstract way more
    	 * the node navigation.
    	 * Encapsulate much more code.
    	 * 
    	 * TODO
    	 * How to deal with node <RemovedOperation>
    	 * for now will be ignored
    	 * in the evaluation will discard everyone 
    	 * except <OperationDiff>
    	 */
    	
    	for ( int x = 0; x < nodes.getLength(); x++ ) {
    		 Node node = nodes.item(x);
    		 if (node.getNodeType()==Node.ELEMENT_NODE && node.getNodeName() == tagName){
    			 NodeList diffsNode = (NodeList)node.getChildNodes();
				 
    			 if (util.getNodeSize("Statement",(NodeList)util.getNode("File1", diffsNode)) == util.getNodeSize("Statement",(NodeList)util.getNode("File2", diffsNode))){
    				 for (int w = 0; w < util.getNode("File1", diffsNode).getChildNodes().getLength(); w++){
    					 this.file1 = "";
    					 this.file2 = "";
    					 if (util.getNode("File1", diffsNode).getChildNodes().item(w).getNodeType() == Node.ELEMENT_NODE){
    						 this.file1 = util.getNode("File1", diffsNode).getChildNodes().item(w).getAttributes().getNamedItem("stmt").toString().replace("\n", "").replace(" ", "");
    						 this.file2 = util.getNode("File2", diffsNode).getChildNodes().item(w).getAttributes().getNamedItem("stmt").toString().replace("\n", "").replace(" ", "");
    					 }
    					 if(!file1.isEmpty() && !file2.isEmpty()){
    						 if(util.statementsCompare(file1, file2)){
    							 count +=1;
    						 }
    					 } 
    				 }
	    		 }else{
	    			 
	    			 /*
	    			  * TODO
	    			  * This condition needs to be worked.
	    			  * Encapsulate all the new operation types and
	    			  * further analysis is needed.
	    			  * There are three situations possible, they are noted.
	    			  */
	    			 count +=1;
	    		 }
    		 }
    	}
    	return count;
    }
    
    public ArrayList<ChangeFacts> getExtensionList() {
		return extensionList;
	}

	public void setExtensionList(ArrayList<ChangeFacts> extensionList) {
		this.extensionList = extensionList;
	}
}
