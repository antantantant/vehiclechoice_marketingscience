package namwoo2013;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import org.json.*;
import org.apache.commons.math3.genetics.*;
import au.com.bytecode.opencsv.CSVReader;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.Query;

public class Algorithm {
	private double[][] X;
	private int p;
	private int n;
	private double lambda;
	private double[] w;
	private double[] Y;
	private int[][] INDa;
	private int[][] INDb;
	private int[][] INDc;
	private double obj;
	private int nsample;
	private double[] mean;
	private double[] std;
	private boolean counterbalance;
	private boolean crowdinfo;
	private double[] style;
	
	private List<double[][]> s_allX; //iter * feature * user
	private double[][] allX;
	private List<int[][]> s_allINDa;
	private List<int[][]> s_allINDb;
	private List<int[][]> s_allINDc;
	private List<double[]> s_allw;
	private static double[] s_alllambda;
	private List<double[]> s_allmean;
	private List<double[]> s_allstd;
	private int totalUserNum = 0;
	private double[][] currentAllX;
	
    // training
	public static String doTrain(String quest, String cb, String ci) throws IOException, JSONException{
		Algorithm t = new Algorithm();
		return t.SVMtrain(quest, cb, ci);
	}
    
    // sampling
	public static String doCal(String quest, String cb, String ci) throws IOException, JSONException{
		Algorithm t = new Algorithm();
		return t.parse(quest, cb, ci);
	}
	
    
    /////////////////////////////// this is the main training code ////////////////////////////////////////////
	public String SVMtrain(String data, String cb, String ci) throws JSONException {
          // check if the current query is counterbalanced (purchase question goes first)
          // but this is not used in the current code...so just neglect this variable
		  counterbalance = cb.equals("true");
		  
          // check if crowdinfo is needed
          crowdinfo = ci.equals("1");
		  if (crowdinfo){
              // if crowdinfo, get all previous users
			  getAllData();
		  }
		  
		  JSONObject js = new JSONObject(data);
		  p = js.getInt("dim");
		  n = js.getInt("num");
		  
		  JSONArray X_temp = js.getJSONArray("dat"); // data X
		  JSONArray Y_temp = js.getJSONArray("lab"); // data Y
		  X = JSONM2J(X_temp); // converting JSON type to native double[][]
		  double[] Yt = JSON2J(Y_temp);
		  Y = new double[Yt.length]; 
		  for (int i = 0; i<Yt.length; i++){
			  Y[i] = Math.abs(Yt[i]); // if the car on the right is chosen, Y is negative (-1,-2)
              // Here in the training, Y needs to be positive, i.e., the X is always (better - worse)
		  }
		  
		  System.out.print("start learning with ranking SVM... \n");
		  
		  train(); // all training details are here
		  
          // output the trained model to the front end
		  JSONObject sj = new JSONObject();
		  sj.put("INDa", INDa);
		  sj.put("INDb", INDb);
		  sj.put("INDc", INDc);
		  sj.put("lambda", lambda);
		  sj.put("w", w);
		  sj.put("X", X);
		  sj.put("Y", Yt);
		  sj.put("nsample", 0);
		  sj.put("mean",mean);
		  sj.put("std",std);
		  sj.put("S", calculateScore()); // update styling scores according to the current model
		  String s = sj.toString();
		  return(s);
	}
	
    // function to calculate/update the styling scores of all existing samples
	private double[] calculateScore() {
		double[] S = new double[X.length];
		for (int i=0;i<X.length;i++){
			S[i] = decisionf(X[i], X, w, mean, std, INDa, INDb, INDc);
            // if crowdinfo, do a weighted sum
			if (crowdinfo&&totalUserNum>0){
				double fcrowd = allDecisionf(X[i]);
                // the following weighting scheme comes from Namwoo
				double rho = 0.75/200.0*totalUserNum; 
				S[i] = (1-rho)*S[i] + rho*fcrowd;
			}
		}
		return S;
	}
	
    // main function for training
	private void train(){
		//rankSVM
		System.out.print("start rankSVM... \n");
		int i; int j;
        
        // convert 19 variables to 276 features, each representing a distance between a pair of control points
		double[][] F = calFeature(X);
        // to use the 19 variables directly, please use the following line:
//		double[][] F = X.clone();
        
		int nfeature = F[0].length;
		mean = new double[nfeature];
		std = new double[nfeature];
		lambda = 1.0/nfeature; // standard Gaussian parameter setting
		double[][] FBar = featureNormalize(F); // normalize features (zero mean, one std)
		System.out.print("features done... \n");
		
		int counta = 0;
		int countb = 0;
		int countc = 0;
		for (i = 0; i<Y.length; i++){
			if (Y[i]==0){countc++;// two designs are indifferent
			}
			else if (Y[i]==1){countb++;// design i is slightly better than design j
			}
			else if (Y[i]==2){counta++;// design i is much better than design j
			}
		}
		System.out.print("\n");
		
        
        // the following section creates the SVM dual problem 
		INDa = new int[counta][2];
		INDb = new int[countb][2];
		INDc = new int[countc][2];
		System.out.print("counting pairwise comparisons done... \n");
		
		int count = counta+countb+2*countc;
		double[][] H = new double[count][count];
		double[] f = new double[count];
		double[][] X1a = new double[counta][nfeature];
		double[][] X2a = new double[counta][nfeature];
		double[][] X1b = new double[countb][nfeature];
		double[][] X2b = new double[countb][nfeature];
		double[][] X1c = new double[countc][nfeature];
		double[][] X2c = new double[countc][nfeature];
		
		int cca=0,ccb=0,ccc=0;
		for (i = 0; i<Y.length; i++){
			if (Y[i]==0){
				INDc[ccc][0] = 2*i; INDc[ccc][1] = 2*i+1;
				X1c[ccc] = FBar[2*i];
				X2c[ccc] = FBar[2*i+1];
				ccc++;
			}
			else if (Y[i]==1){
				INDb[ccb][0] = 2*i; INDb[ccb][1] = 2*i+1;
				X1b[ccb] = FBar[2*i];
				X2b[ccb] = FBar[2*i+1];				
				ccb++;
			}
			else if (Y[i]==2){
				INDa[cca][0] = 2*i; INDa[cca][1] = 2*i+1;
				X1a[cca] = FBar[2*i];
				X2a[cca] = FBar[2*i+1];
				cca++;
			}
		}
		double a = 3, b = 2, c = 1;
		for (i = 0; i<counta; i++){
			for (j=0; j<counta; j++){
				H[i][j] = kernel(X1a[i],X1a[j]) - kernel(X1a[i],X2a[j]) - kernel(X2a[i],X1a[j]) + kernel(X2a[i],X2a[j]);
			}
			for (j=0; j<countb; j++){
				H[i][counta+j] = kernel(X1a[i],X1b[j]) - kernel(X1a[i],X2b[j]) - kernel(X2a[i],X1b[j]) + kernel(X2a[i],X2b[j]);
				H[counta+j][i] = H[i][counta+j];
			}
			f[i] = -a;
		}
		for (i = 0; i<countb; i++){
			for (j=0; j<countb; j++){
				H[counta+i][counta+j] = kernel(X1b[i],X1b[j]) - kernel(X1b[i],X2b[j]) - kernel(X2b[i],X1b[j]) + kernel(X2b[i],X2b[j]);
			}
			f[counta+i] = -b;
		}
		for (i = 0; i<counta; i++){
			for (j=0; j<countc; j++){
				H[i][counta+countb+j] = -kernel(X1a[i],X1c[j]) + kernel(X1a[i],X2c[j]) + kernel(X2a[i],X1c[j]) - kernel(X2a[i],X2c[j]);
				H[i][counta+countb+countc+j] = -H[i][counta+countb+j];
				H[counta+countb+j][i] = H[i][counta+countb+j];
				H[counta+countb+countc+j][i] = H[i][counta+countb+countc+j];
			}
		}
		for (i = 0; i<countb; i++){
			for (j=0; j<countc; j++){
				H[counta+i][counta+countb+j] = -kernel(X1b[i],X1c[j]) + kernel(X1b[i],X2c[j]) + kernel(X2b[i],X1c[j]) - kernel(X2b[i],X2c[j]);;
				H[counta+i][counta+countb+countc+j] = -H[counta+i][counta+countb+j];
				H[counta+countb+j][counta+i] = H[counta+i][counta+countb+j];
				H[counta+countb+countc+j][counta+i] = H[counta+i][counta+countb+countc+j];
			}
		}
		for (i = 0; i<countc; i++){
			for (j=0; j<countc; j++){
				H[counta+countb+i][counta+countb+j] = kernel(X1c[i],X1c[j]) - kernel(X1c[i],X2c[j]) - kernel(X2c[i],X1c[j]) + kernel(X2c[i],X2c[j]);
				H[counta+countb+j][counta+countb+i] = H[counta+countb+i][counta+countb+j];
				H[counta+countb+i][counta+countb+countc+j] = -H[counta+countb+i][counta+countb+j];
				H[counta+countb+countc+j][counta+countb+i] = -H[counta+countb+i][counta+countb+j];
				H[counta+countb+countc+i][counta+countb+j] = -H[counta+countb+i][counta+countb+j];
				H[counta+countb+j][counta+countb+countc+i] = -H[counta+countb+i][counta+countb+j];
				H[counta+countb+countc+i][counta+countb+countc+j] = H[counta+countb+i][counta+countb+j];
				H[counta+countb+countc+j][counta+countb+countc+i] = -H[counta+countb+i][counta+countb+j];
			}
			f[counta+countb+i] = c;
			f[counta+countb+countc+i] = c;
		}
		// the above section creates the SVM dual problem
		
        System.out.print("hessian done... \n");
		
		Solver s = new Solver(); 
		s.Solve(count, H, f, 1E-3); // solve the dual problem
		w = s.alpha; // output the lagrangian multipliers
	}
	
    // define the kernel to be used in SVM
	private double kernel(double[] v1, double[] v2) {
		int l = v1.length;
		double k = 0;
		for (int i = 0;i<l;i++){
			k+= (v1[i]-v2[i])*(v1[i]-v2[i]);
		}
		k*=lambda;
		k = Math.exp(-k);
		
        // use the following for linear kernel
//		int l = v1.length;
//		double k = 0;
//		for (int i = 0;i<l;i++){
//			k+= v1[i]*v2[i];
//		}
		return k;
	}
    
    // normalize the input matrix by column
	private double[][] featureNormalize(double[][] phi) {
		int i = phi.length;
		int j = phi[0].length;
		double mean1;
		double std1;
		double[][] normalizedPhi = new double[i][j];
		for (int k = 0;k<j;k++){
			mean1 = 0; std1 = 0;
			for (int l = 0;l<i;l++){
				mean1+= phi[l][k];
			}
			mean1 = mean1/i;
			for (int l = 0;l<i;l++){
				std1+=(phi[l][k]-mean1)*(phi[l][k]-mean1);
			}
			std1 = Math.sqrt(std1/i);
			if (Math.abs(std1)<1e-6){
				std1=1.0;
			}
			for (int l = 0;l<i;l++){
				normalizedPhi[l][k]=(phi[l][k]-mean1)/std1;
			}
			mean[k] = mean1;
			std[k] = std1;
		}
		return normalizedPhi;
	}
	
    // convert variables to distance features (for a set X) 
	private double[][] calFeature(double[][] X){
		// this is hard coded feature calculation
		int ncp = 24;
		int pfeature = ncp*(ncp-1)/2;
		double[][] features = new double[n][pfeature];
		for (int i = 0; i<n; i++){
			ControlPoints cp = new ControlPoints();
			double[][] controlPoints = cp.getControlPoints(X[i]);
			features[i] = getFeatures(controlPoints, pfeature);
		}
		return features;
	}
    
    // convert variables to distance features (for a single x) 
	private double[] calOneFeature(double[] x){
		int ncp = 24;
		int pfeature = ncp*(ncp-1)/2;
		ControlPoints cp = new ControlPoints();
		double[][] controlPoints = cp.getControlPoints(x);
		double[] feature = getFeatures(controlPoints, pfeature);
		return feature;
	}
	
    // compute the distances for calFeature() and calOneFeature()
	private double[] getFeatures(double[][] controlPoints, int pfeature) {
		int n = controlPoints.length;
		int p = controlPoints[0].length;
		int id;
		double[] features = new double[pfeature];
		for (int i = 0; i<n-1; i++){
			for (int j = i+1; j<n; j++){
				id = (n-1+n-1-(i-1))*i/2 + (j-i) - 1; 
				features[id] = 0;
				for (int k = 0; k<p; k++){
					features[id]+=(controlPoints[i][k]-controlPoints[j][k])*(controlPoints[i][k]-controlPoints[j][k]);
				}
				features[id] = Math.sqrt(features[id]);
			}
		}
		return features;
	}
    /////////////////////////////// above is the main training code ////////////////////////////////////////////

    
    
    /////////////////////////////// below is the main sampling code ////////////////////////////////////////////
	private String parse(String array, String cb, String ci) throws JSONException {
		  counterbalance = cb.equals("true");
		  crowdinfo = ci.equals("1");
        
          // if crowdinfo, get all user data and put all X into one big allX
		  if (crowdinfo){
			  if (getAllData()){
				  int ll = s_allX.get(0).length;
				  int p = s_allX.get(0)[0].length;
				  allX = new double[s_allX.size()*ll][p];
				  for (int i=0;i<s_allX.size();i++){
					  for (int j=0;j<ll;j++){
						  for (int k=0;k<p;k++){
							  allX[i*ll+j][k] = s_allX.get(i)[j][k];
						  }
					  }
				  }				  
			  }
		  }
		  
          // get the trained SVM model
		  JSONObject js = new JSONObject(array);
		  p = js.getInt("dim");
		  n = js.getInt("num");
		  obj = js.getDouble("obj");
		  
		  JSONObject svmPar = new JSONObject(js.getString("svmPar"));

		  lambda = svmPar.getDouble("lambda");
		  w = JSON2J(svmPar.getJSONArray("w"));
		  X = JSONM2J(svmPar.getJSONArray("X"));
		  JSONArray INDaArray = svmPar.getJSONArray("INDa");
		  if(INDaArray.length() >0){INDa = JSONMI2J(INDaArray);}
		  else{INDa = new int[0][0];}
		  JSONArray INDbArray = svmPar.getJSONArray("INDb");
		  if(INDbArray.length() >0){INDb = JSONMI2J(INDbArray);}
		  else{INDb = new int[0][0];}
		  JSONArray INDcArray = svmPar.getJSONArray("INDc");
		  if(INDcArray.length() >0){INDb = JSONMI2J(INDcArray);}
		  else{INDc = new int[0][0];}
		  nsample = svmPar.getInt("nsample"); 
		  mean = JSON2J(svmPar.getJSONArray("mean"));
		  std = JSON2J(svmPar.getJSONArray("std"));
		  style = new double[nsample+1];
		  
	  	  System.out.print("\nstart searching with objective: "+obj+"... \n");
		  String s = null;
		  
          // if there is no model, just randomly sample some, which is NEVER the case in this study
          // so neglect this part
          if (w.length==0){
			  Scatter_null();
			  svmPar.put("X", new JSONArray(X));
			  nsample++;
			  svmPar.put("nsample", nsample);
			  js.put("obj",obj);
			  js.put("svmPar",svmPar);
			  s = js.toString();
			  s = "data="+s;
		  }
          // otherwise do adaptive sampling
		  else {
			  Scatter(); // this is the function that does adaptive sampling
              
              // send back the data
			  svmPar.put("X", new JSONArray(X));
			  nsample++;
			  svmPar.put("nsample", nsample);
			  js.put("obj",obj);
			  js.put("svmPar",svmPar);
			  js.put("style", style);
			  s = js.toString();
			  s = "data="+s;
		  }
		  return(s);	  
	  }
	
    
	private void Scatter() throws JSONException{
		// Get new samples
		double[] t = sampleGA();
        
        // once done, add the new pair of designs to the end of current user's X
		X = arrayadd(t,X);
		if (nsample==1){
            // calculate styling scores of the new pair
			style[0] = decisionf(X[X.length-2], X, w, mean, std, INDa, INDb, INDc);
			style[1] = decisionf(X[X.length-1], X, w, mean, std, INDa, INDb, INDc);
			
            // if crowdinfo, adjust the scores based on all previous users
            if (crowdinfo&&totalUserNum>0){
				double fcrowd = allDecisionf(X[X.length-2]);
				double rho = 0.75/200.0*totalUserNum;
				style[0] = (1-rho)*style[0] + rho*fcrowd;
				fcrowd = allDecisionf(X[X.length-1]);
				style[1] = (1-rho)*style[1] + rho*fcrowd;				
			}
		}
	}
    
    // Obsolete in the current study
	private void Scatter_null() throws JSONException{
		// Get new points
		double[] t = new double[p];
		for (int j = 0; j<p; j++){
			t[j] = Math.random();
		}
		X = arrayadd(t,X);
	}
	
    // The main GA code for adaptive sampling
	private double[] sampleGA(){
		int POP_LIMIT = 50;
		int NUM_GENERATIONS = 500;
        
        // for the first point, distance search only, 
        // since we don't want to always search for corners
        // use a smaller population and generation size
		if(obj==0){
			POP_LIMIT = 20;
			NUM_GENERATIONS = 100;
		}
		int TOURNAMENT_ARITY = POP_LIMIT;
		double CROSSOVER_RATE = 1;
		double ELITISM_RATE = 0.9;
		double MUTATION_RATE = 0.1;
		double[][] SV = new double[X.length-nsample][]; 
		for (int i=0;i<SV.length;i++) {
			SV[i] = X[i].clone();
		}
		currentAllX = concat(X,allX);
		
		CrossoverPolicy crossoverPolicy = new OnePointCrossover<Integer>();
		MutationPolicy mutationPolicy = new RealMutation();
		SelectionPolicy selectionPolicy = new TournamentSelection(TOURNAMENT_ARITY);
		GeneticAlgorithm ga = new GeneticAlgorithm(crossoverPolicy, CROSSOVER_RATE, 
				mutationPolicy, MUTATION_RATE, selectionPolicy,
				X, SV, w, lambda, mean, std, INDa, INDb, INDc, obj, currentAllX);
		
		Population initial = getInitialPopulation(POP_LIMIT, ELITISM_RATE, SV);
		StoppingCondition stopCond = new FixedGenerationCount(NUM_GENERATIONS);
		System.out.print("GA started running...\n");
		Population finalPopulation = ga.evolve(initial, stopCond);
		RealChromosome bestFinal = (RealChromosome) finalPopulation.getFittestChromosome();
		
		List<Double> solution = bestFinal.getRepresentation();
		System.out.print("GA finished...\n");
		
		double[] t = new double[p];
		for (int j = 0; j<solution.size(); j++){
			 t[j]= solution.get(j);
		}
        
        // store the objective value of the first sample, and use this value for the second
		obj = decisionf(t, SV, w, mean, std, INDa, INDb, INDc);
		
		return t;
	}
	
	private Population getInitialPopulation(int POP_LIMIT, double ELITISM_RATE, double[][] SV) {
		List<Chromosome> chromosomes = initializeChromosomes(POP_LIMIT, SV);
		Population pop = new ElitisticListPopulation(chromosomes, POP_LIMIT, ELITISM_RATE);
		return pop;
	}

	private List<Chromosome> initializeChromosomes(int POP_LIMIT, double[][] sv) {
		List<Chromosome> chromosomes = new ArrayList<Chromosome>(POP_LIMIT);
		for (int i = 0; i<POP_LIMIT; i++){
			List<Double> representation = RealChromosome.randomRealRepresentation(p);
			chromosomes.add(new RealChromosome(representation, calculateFitness(list2prim(representation),sv)));
		}
		return chromosomes;
	}

	private double calculateFitness(double[] x, double[][] SV) {
		// for the first sample, maximize distance only
        if (obj==0){
			return distance(x, currentAllX);
		}
        // for the second sample, focus mainly on utility balance
		else{
			return merit(0.99,0.01,
					Math.exp(-Math.abs(obj-decisionf(x, SV, w, mean, std, INDa, INDb, INDc))),distance(x,currentAllX));
		}
	}
	
	private double merit(double w1, double w2, double f, double s) {
		double fitness = w1*f+w2*s;
		return fitness;
	}
	
	private double[] list2prim(List<Double> l) {
		double[] x = new double[l.size()];
		for (int i = 0; i<x.length; i++) {
			x[i] = l.get(i);
		}
		return x;
	}

	private double[][] arrayadd(double[] src, double[][] dst) {
		if (dst!=null){
			double[][] out = new double[1+dst.length][dst[0].length];
			for (int i = 0; i<dst.length; i++) {
				out[i] = dst[i];
			}
			out[dst.length] = src;
			return out;
		}
		else {
			double[][] out = new double[1][src.length];
			out[0] = src;
			return out;
		}
	}
	double[][] concat(double[][] A, double[][] B) {
		if (A==null){
			return B;
		}
		if (B==null){
			return A;
		}
		int aLen = A.length;
		int bLen = B.length;
		double[][] C= new double[aLen+bLen][A[0].length];
		System.arraycopy(A, 0, C, 0, aLen);
		System.arraycopy(B, 0, C, aLen, bLen);
		return C;
	}
	
	private double decisionf(double[] x, double[][] X, double[] w, double[] mean, double[] std, int[][] INDa, int[][] INDb, int[][] INDc){
		double f = 0;
		double[] feature = calOneFeature(x);
//		double[] feature = x.clone();
		double nfeature = feature.length;
		for (int i = 0; i<nfeature; i++){
			feature[i]= (feature[i]-mean[i])/(std[i]);
		}
		double[] goodfeature; double[] badfeature;
		for (int i = 0; i<INDa.length;i++){
			goodfeature = calOneFeature(X[INDa[i][0]]);
//			goodfeature = X[INDa[i][0]].clone();
			badfeature = calOneFeature(X[INDa[i][1]]);
//			badfeature = X[INDa[i][1]].clone();
			for (int j = 0; j<nfeature; j++){
				goodfeature[j]= (goodfeature[j]-mean[j])/(std[j]);
				badfeature[j]= (badfeature[j]-mean[j])/(std[j]);
			}
			f+= w[i]*(kernel(goodfeature,feature)-kernel(badfeature,feature));
		}
		for (int i = 0; i<INDb.length;i++){
			goodfeature = calOneFeature(X[INDb[i][0]]);
//			goodfeature = X[INDb[i][0]].clone();
			badfeature = calOneFeature(X[INDb[i][1]]);
//			badfeature = X[INDb[i][1]].clone();
			for (int j = 0; j<nfeature; j++){
				goodfeature[j]= (goodfeature[j]-mean[j])/std[j];
				badfeature[j]= (badfeature[j]-mean[j])/std[j];
			}
			f+= w[INDa.length+i]*(kernel(goodfeature,feature)-kernel(badfeature,feature));
		}
		for (int i = 0; i<INDc.length;i++){
			goodfeature = calOneFeature(X[INDc[i][0]]);
//			goodfeature = X[INDc[i][0]].clone();
			badfeature = calOneFeature(X[INDc[i][1]]);
//			badfeature = X[INDc[i][1]].clone();
			for (int j = 0; j<nfeature; j++){
				goodfeature[j]= (goodfeature[j]-mean[j])/std[j];
				badfeature[j]= (badfeature[j]-mean[j])/std[j];
			}
			f+= w[INDa.length+INDb.length+i]*(kernel(goodfeature,feature)-kernel(badfeature,feature));
		}
		return f;
	}
	
	private double distance(double[] x, double[][] X) {
		double f = 1e12;
		double g;
		for(int i=0;i<X.length;i++){
			g = 0.0;
			for(int j=0;j<x.length;j++){
				g += (x[j]-X[i][j])*(x[j]-X[i][j]);
			}
			f = Math.min(f,g);
		}
		return f;
	}
	
	private double allDecisionf(double[] x) {
		double f = 0.0;
		double[] w;
		double[] mean;
		double[] std;
		double[][] X;
		int[][] INDa;
		int[][] INDb;
		int[][] INDc;
		
		for (int i=0;i<totalUserNum;i++){
			w = s_allw.get(i);
			mean = s_allmean.get(i);
			std = s_allstd.get(i);
			X = s_allX.get(i);
			INDa = s_allINDa.get(i);
			INDb = s_allINDb.get(i);
			INDc = s_allINDc.get(i);
			f += decisionf(x, X, w, mean, std, INDa, INDb, INDc);
		}
		return f/totalUserNum;
	}
	/////////////////////////////// above is the main sampling code ////////////////////////////////////////////
    
    /////////////////////////////// below are some functions called by the main code ///////////////////////////
	private double[] JSON2J(JSONArray a){
		double[] b = new double[a.length()];
		for (int i=0;i<a.length();i++){
			try {
				b[i] = a.getDouble(i);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		return b;
	}
	private double[][] JSONM2J(JSONArray a){
		int pp = 0;
		try {
			pp = a.getJSONArray(0).length();
		} catch (JSONException e1) {
			e1.printStackTrace();
		}
		double[][] b = new double[a.length()][pp];

		for (int i=0;i<a.length();i++){
			for (int j=0;j<pp;j++){
				try {
					b[i][j] = a.getJSONArray(i).getDouble(j);
				} catch (JSONException e) {
					e.printStackTrace();
				}				
			}
		}
		return b;
	}
	
	private int[][] JSONMI2J(JSONArray a) throws JSONException{
		int pp = 0;
		if(a.length()>0) {
			pp = a.getJSONArray(0).length();
		}
		int[][] b = new int[a.length()][pp];

		for (int i=0;i<a.length();i++){
			for (int j=0;j<pp;j++){
				try {
					b[i][j] = a.getJSONArray(i).getInt(j);
				} catch (JSONException e) {
					e.printStackTrace();
				}				
			}
		}
		return b;
	}
	public static void quicksort(double[] main, int[] index) {
	    quicksort(main, index, 0, index.length - 1);
	}
	public static void quicksort(double[] a, int[] index, int left, int right) {
	    if (right <= left) return;
	    int i = partition(a, index, left, right);
	    quicksort(a, index, left, i-1);
	    quicksort(a, index, i+1, right);
	}
	// partition a[left] to a[right], assumes left < right
	private static int partition(double[] a, int[] index, 
	int left, int right) {
	    int i = left - 1;
	    int j = right;
	    while (true) {
	        while (less(a[++i], a[right]))      // find item on left to swap
	            ;                               // a[right] acts as sentinel
	        while (less(a[right], a[--j]))      // find item on right to swap
	            if (j == left) break;           // don't go out-of-bounds
	        if (i >= j) break;                  // check if pointers cross
	        exch(a, index, i, j);               // swap two elements into place
	    }
	    exch(a, index, i, right);               // swap with partition element
	    return i;
	}
	// is x < y ?
	private static boolean less(double x, double y) {
	    return (x < y);
	}
	// exchange a[i] and a[j]
	private static void exch(double[] a, int[] index, int i, int j) {
	    double swap = a[i];
	    a[i] = a[j];
	    a[j] = swap;
	    int b = index[i];
	    index[i] = index[j];
	    index[j] = b;
	}
	
	
	private Boolean getAllData() throws JSONException{
		s_allX = new ArrayList<double[][]>(); //iter * feature * user
		s_allINDa = new ArrayList<int[][]>();
		s_allINDb = new ArrayList<int[][]>();
		s_allINDc = new ArrayList<int[][]>();
		s_allw = new ArrayList<double[]>();
		s_allmean = new ArrayList<double[]>();
		s_allstd = new ArrayList<double[]>();
		
		double[][] s_X;
		int[][] s_INDa;
		int[][] s_INDb;
		int[][] s_INDc;
		double s_lambda;
		double[] s_w;
		double[] s_mean;
		double[] s_std;
		
		Objectify ofy = ObjectifyService.begin();
		Query<Test> q = ofy.query(Test.class).filter("model =","MTURKnonlinear");
		totalUserNum = q.count();
		if (totalUserNum>0){
			s_alllambda = new double[totalUserNum];
			int count = 0;
			for (Test t:q){
				String data = t.data;
				JSONObject js = new JSONObject(data);
				JSONObject styleModel = new JSONObject(js.getString("styleModel"));
				JSONArray s_X_ = styleModel.getJSONArray("X");
				JSONArray s_INDa_ = styleModel.getJSONArray("INDa");
				JSONArray s_INDb_ = styleModel.getJSONArray("INDb");
				JSONArray s_INDc_ = styleModel.getJSONArray("INDc");
				s_lambda = styleModel.getDouble("lambda");
				JSONArray s_w_ = styleModel.getJSONArray("w");
				JSONArray s_mean_ = styleModel.getJSONArray("mean");
				JSONArray s_std_ = styleModel.getJSONArray("std");
				s_X = JSONM2J(s_X_);
				s_INDa = JSONMI2J(s_INDa_);
				s_INDb = JSONMI2J(s_INDb_);
				s_INDc = JSONMI2J(s_INDc_);
				s_w = JSON2J(s_w_);
				s_mean = JSON2J(s_mean_);
				s_std = JSON2J(s_std_);
				
				s_allX.add(s_X);
				s_allINDa.add(s_INDa);
				s_allINDb.add(s_INDb);
				s_allINDc.add(s_INDc);
				s_allw.add(s_w);
				s_alllambda[count] = s_lambda;
				s_allmean.add(s_mean);
				s_allstd.add(s_std);
			}			
			return true;
		}
		else{
			return false;
		}
	}
    /////////////////////////////// above are some functions called by the main code ///////////////////////////

	
	/////////////////////////////// below is the DOE method (model 1,2) ////////////////////////////////////////
	public static String getDOE(String ns) throws IOException, JSONException {
		  int n = Integer.parseInt(ns);
        
          // this file is used for model 2, change the file name to DOE.csv if model 1
	      String strFile = "resource/DOE2.csv";
	      CSVReader reader = new CSVReader(new FileReader(strFile));
	      JSONObject js = new JSONObject();
	      String [] nextLine;
        
          // the number of attribute levels are hard coded
          // change the following numbers if the number of levels change
	      double[][] X = new double[n][19]; 
	      double[][] A = new double[n][8];
	      
          // MTurkDOE2 is the database tag for model 2, change it to MTurkDOE for model 1
          // for new experiments, either delete the existing database or change the tags to something new
	      Objectify ofy = ObjectifyService.begin();
		  Query<TestDOE> q = ofy.query(TestDOE.class).filter("name", "MTurkDOE2");
		  int totalDOE = q.count();
		  
        
          // the following numbers are based on the number of attribute levels
          // change these if the number of levels change
	      int lineNumber = 0;
	      while (lineNumber < totalDOE*n) {
	    	  nextLine = reader.readNext();
	    	  lineNumber++;
	      }
	      while (lineNumber < (totalDOE+1)*n ){
	    	  nextLine = reader.readNext();
		      for (int i=0;i<19;i++){
		    	  X[lineNumber-totalDOE*n][i] = Double.parseDouble(nextLine[i]);
		      }
		      for (int i=19;i<27;i++){
		    	  A[lineNumber-totalDOE*n][i-19] = Double.parseDouble(nextLine[i]);
		      }	        
		      lineNumber++;
	      }
	      js.put("X", X);
	      js.put("A", A);
	      js.put("user_id", totalDOE);
	      reader.close();
		return js.toString();
	}
	
	private Algorithm() {} // Uninstantiatable class
}