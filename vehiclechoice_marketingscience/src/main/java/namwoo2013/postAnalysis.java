package namwoo2013;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.genetics.Chromosome;
import namwoo2013.CrossoverPolicy;
import org.apache.commons.math3.genetics.ElitisticListPopulation;
import org.apache.commons.math3.genetics.FixedGenerationCount;
import namwoo2013.GeneticAlgorithm;
import namwoo2013.MutationPolicy;
import namwoo2013.OnePointCrossover;
import org.apache.commons.math3.genetics.Population;
import namwoo2013.RealChromosome;
import namwoo2013.RealMutation;
import org.apache.commons.math3.genetics.SelectionPolicy;
import org.apache.commons.math3.genetics.StoppingCondition;
import org.apache.commons.math3.genetics.TournamentSelection;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.Query;

public class postAnalysis {
	static List<double[][]> s_allX; //iter * feature * user
	static List<double[]> s_allY;
	static List<double[][]> s_allINDa;
	static List<double[][]> s_allINDb;
	static List<double[][]> s_allINDc;
	static List<double[]> s_allw;
	private static double[] s_alllambda;
	static List<double[]> s_allmean;
	static List<double[]> s_allstd;
	static List<double[][]> u_allX;
	static List<double[]> u_allY;
	static List<double[][]> u_allINDa;
	static List<double[][]> u_allINDb;
	static List<double[][]> u_allINDc;
	static List<double[]> u_allw;
	static List<double[]> u_allmean;
	static List<double[]> u_allstd;
	static List<double[][]> allValidateX;
	static List<double[][]> allValidateA;
	static List<double[]> alluserY;
	static List<String> allSurvey;
	static int totalUserNum;
	static List<double[][]> allStylingScore;
	
	// the following will be used in case individual styling model needs retrain
	static double[][] X;
	static double[] Y;
	static double[] mean;
	static double[] std;
	static double lambda;
	static int[][] INDa;
	static int[][] INDb;
	static int[][] INDc;
	static double[] w;
	static int p;

    // read in EVERYTHING
	public static String read(int id, int n) throws IOException, JSONException{
		JSONObject sj = new JSONObject();
		if (getAllData(id, n)){
//			List<double[]> allbeta = calAllPartworth(n);
			List<double[][]> allTrainingFeature = allFeatures(s_allX);
			List<double[][]> allValidateFeature = allFeatures(allValidateX);
			
			sj.put("s_allX", s_allX); // a set of arrays of all styling pairs 
			sj.put("s_allY", s_allY); // a set of arrays of all labels on styling pairs
			sj.put("s_allINDa", s_allINDa); // this is needed for training, indices of pairs with label 2 (much better)
			sj.put("s_allINDb", s_allINDb); // this is needed for training, indices of pairs with label 1 (slightly better)
			sj.put("s_allINDc", s_allINDc); // this is needed for training, indices of pairs with label 0 (no differece)
			sj.put("s_allw", s_allw); // a set of arrays of all individual lagrangian multipliers
			sj.put("s_allfeature", allTrainingFeature); // converted from s_allX
			sj.put("s_allmean", s_allmean); // mean values for features
			sj.put("s_allstd", s_allstd); // std for features
			sj.put("u_allX", u_allX); // a set of arrays of all attribute levels
			sj.put("u_allY", u_allY); // a set of arrays of all labels on purchase pairs
			sj.put("u_allINDa", u_allINDa); 
			sj.put("u_allINDb", u_allINDb);
			sj.put("u_allINDc", u_allINDc);
			sj.put("u_allw", u_allw);
			sj.put("u_allmean", u_allmean);
			sj.put("u_allstd", u_allstd);
//			sj.put("u_allbeta", allbeta);
			sj.put("allValidateX", allValidateX); // a set of arrays of validation pairs
			sj.put("allValidateA", allValidateA); // a set of arrays of validation attribute levels
			sj.put("allValidateFeature", allValidateFeature); // converted from allValidateX
			sj.put("alluserY", alluserY); // user responses on validation
			sj.put("allSurvey", allSurvey); // other survey data
			
			// add importance of all geometry features
			sj.put("allStylingScore", allStylingScore);
			
		}
		return(sj.toString());
	}
	
	private static List<double[]> calAllPartworth(int n){
		List<double[]> beta = new ArrayList<double[]>();
		double[] b;
		double[] w;
		double[] mean;
		double[] std;
		double[][] X;
		
		for(int i=0;i<n;i++){
			w = u_allw.get(i);
			mean = u_allmean.get(i);
			std = u_allstd.get(i);
			X = u_allX.get(i);
			b = calPartworth(X, mean, std, w);
			beta.add(b);
		}
		return beta;
	}
	
	public static double[] calPartworth(double[][] X, double[] mean, double[] std, double[] w){
		int n = X.length/2;
		int p = mean.length;
		double[] b = new double[p];
		for(int j=0;j<n;j++){
			for(int k=0;k<p;k++){
				b[k] += w[j]*(X[2*j][k]-X[2*j+1][k])/std[k];
			}
		}
		return b;
	}
	
	private static List<double[][]> allFeatures(List<double[][]> LX){
		List<double[][]> phi = new ArrayList<double[][]>();
		double[][] F;
		double[][] X;
		int n;
		for(int i=0;i<LX.size();i++){
			X = LX.get(i);
			n = X.length;
			F = calFeature(X,n);
			phi.add(F);
		}
		return phi;
	}
	private static double[][] calFeature(double[][] X, int n){
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
	private static double[] getFeatures(double[][] controlPoints, int pfeature) {
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
	
    
    // the following hitrate part is imcomplete and never used
	public static String calHitRate() throws IOException, JSONException{
		getAllData(0,0);
		
		double hitRate = 0;
		for(int i=0;i<totalUserNum;i++){
			hitRate += calIndividualHitRate(i); 
		}
		hitRate /= totalUserNum;
		JSONObject sj = new JSONObject();
		sj.put("hitRate", hitRate);
		String s = sj.toString();
		return(s);
	}
	private static double calIndividualHitRate(int i){
		double h = 0;
		double s1,s2;
		double f1,f2;
		double s1bar, s2bar;
		double f1bar, f2bar;
		double[] x1,x2;
		double[] a1,a2;
		int validateNum = alluserY.get(i).length;
		double[] userY = alluserY.get(i);
	    double Y;
	    double w1 = 0.25;
	    double w2 = 0.75;
	    
		for(int j=0;j<validateNum;j++){
			x1 = allValidateX.get(i)[2*j];
			x2 = allValidateX.get(i)[2*j+1];
			a1 = allValidateA.get(i)[2*j];
			a2 = allValidateA.get(i)[2*j+1];			
			s1 = calStyle(x1);
			s2 = calStyle(x2);
			s1bar = calAverageStyle(x1);
			s2bar = calAverageStyle(x2);
			f1 = calUtility(s1,a1);
			f2 = calUtility(s2,a2);
			f1bar = calAverageUtility(s1,a1);
			f2bar = calAverageUtility(s2,a2);
			Y = userY[j];
			if ((w1*(f1-f2)+w2*(f1bar-f2bar))*Y > 0){
				h++;
			}
		}
		h /= validateNum;
		return h;
	}
	
    
    // main entrance for reading all data
	private static Boolean getAllData(int id, int n) throws JSONException{
		Objectify ofy = ObjectifyService.begin();
        
        // MTURKnonlinear is used for model 3, change this if you want to read from model 1 or 2
		Query<Test> q = ofy.query(Test.class).filter("model =","MTURKnonlinear");
		totalUserNum = q.count();
		
		if (id+n<=totalUserNum){
	
			s_allX = new ArrayList<double[][]>(); //iter * feature * user
			s_allY = new ArrayList<double[]>();
			s_allINDa = new ArrayList<double[][]>();
			s_allINDb = new ArrayList<double[][]>();
			s_allINDc = new ArrayList<double[][]>();
			s_allw = new ArrayList<double[]>();
			s_allmean = new ArrayList<double[]>();
			s_allstd = new ArrayList<double[]>();
			u_allX = new ArrayList<double[][]>();
			u_allY = new ArrayList<double[]>();
			u_allINDa = new ArrayList<double[][]>();
			u_allINDb = new ArrayList<double[][]>();
			u_allINDc = new ArrayList<double[][]>();
			u_allw = new ArrayList<double[]>();
			u_allmean = new ArrayList<double[]>();
			u_allstd = new ArrayList<double[]>();
			allValidateX = new ArrayList<double[][]>();
			allValidateA = new ArrayList<double[][]>();
			alluserY = new ArrayList<double[]>();
			allSurvey = new ArrayList<String>();
			allStylingScore = new ArrayList<double[][]>();
			
			double[][] s_X;
			double[] s_Y;
			double[][] s_INDa;
			double[][] s_INDb;
			double[][] s_INDc;
			double s_lambda;
			double[] s_w;
			double[] s_mean;
			double[] s_std;
			
			double[][] u_X;
			double[] u_Y;
			double[][] u_INDa;
			double[][] u_INDb;
			double[][] u_INDc;
			double[] u_w;
			double[] u_mean;
			double[] u_std;
			
			double[][] validateX;
			double[][] validateA;
			double[] userY;
			
			String survey;
			
			double[][] stylingScore;
					
			s_alllambda = new double[Math.min(totalUserNum-id, n)];
			int count = 0;
			for (Test t:q){
				if (count>=id&&count<Math.min(id+n,totalUserNum)){
					String data = t.data;
					JSONObject js = new JSONObject(data);
					JSONObject styleModel = new JSONObject(js.getString("styleModel"));
					JSONArray s_X_ = styleModel.getJSONArray("X");
					JSONArray s_Y_ = styleModel.getJSONArray("Y");
					JSONArray s_INDa_ = styleModel.getJSONArray("INDa");
					JSONArray s_INDb_ = styleModel.getJSONArray("INDb");
					JSONArray s_INDc_ = styleModel.getJSONArray("INDc");
					s_lambda = styleModel.getDouble("lambda");
					JSONArray s_w_ = styleModel.getJSONArray("w");
					JSONArray s_mean_ = styleModel.getJSONArray("mean");
					JSONArray s_std_ = styleModel.getJSONArray("std");
					s_X = JSONM2J(s_X_);
					s_Y = JSON2J(s_Y_);
					s_INDa = JSONM2J(s_INDa_);
					s_INDb = JSONM2J(s_INDb_);
					s_INDc = JSONM2J(s_INDc_);
					
					// in case want linear model from nonlinear experiment
//					X = s_X.clone();
//					Y = s_Y.clone();
//					train();
//					s_w = w;
//					s_mean = mean;
//					s_std = std;
					// in case want linear model from nonlinear experiment
					
					// otherwise...
					s_w = JSON2J(s_w_);
					s_mean = JSON2J(s_mean_);
					s_std = JSON2J(s_std_);
					// otherwise...
					
					JSONObject utilityModel = new JSONObject(js.getString("utilityModel"));
					JSONArray u_X_ = utilityModel.getJSONArray("X");
					JSONArray u_Y_ = utilityModel.getJSONArray("Y");
					JSONArray u_INDa_ = utilityModel.getJSONArray("INDa");
					JSONArray u_INDb_ = utilityModel.getJSONArray("INDb");
					JSONArray u_INDc_ = utilityModel.getJSONArray("INDc");
					JSONArray u_w_ = utilityModel.getJSONArray("w");
					JSONArray u_mean_ = utilityModel.getJSONArray("mean");
					JSONArray u_std_ = utilityModel.getJSONArray("std");
					u_X = JSONM2J(u_X_);
					u_Y = JSON2J(u_Y_);
					u_INDa = JSONM2J(u_INDa_);
					u_INDb = JSONM2J(u_INDb_);
					u_INDc = JSONM2J(u_INDc_);
					u_w = JSON2J(u_w_);
					u_mean = JSON2J(u_mean_);
					u_std = JSON2J(u_std_);	
					
					JSONObject validateSet = new JSONObject(js.getString("validateSet"));
					JSONArray validateX_ = validateSet.getJSONArray("X");
					JSONArray validateA_ = validateSet.getJSONArray("A");
					validateX = JSONM2J(validateX_);
					validateA = JSONM2J(validateA_);
					
					JSONArray userY_ = js.getJSONArray("validateY");
				    userY = JSON2J(userY_);
				    
				    survey = js.getString("survey");
				    
					s_allX.add(s_X);
					s_allY.add(s_Y);
					s_allINDa.add(s_INDa);
					s_allINDb.add(s_INDb);
					s_allINDc.add(s_INDc);
					s_allw.add(s_w);
					s_alllambda[0] = s_lambda;
					s_allmean.add(s_mean);
					s_allstd.add(s_std);
					u_allX.add(u_X);
					u_allY.add(u_Y);
					u_allINDa.add(u_INDa);
					u_allINDb.add(u_INDb);
					u_allINDc.add(u_INDc);
					u_allw.add(u_w);
					u_allmean.add(u_mean);
					u_allstd.add(u_std);
					allValidateX.add(validateX);
					allValidateA.add(validateA);
					alluserY.add(userY);
					allSurvey.add(survey);
					
					
					// calculate geometry importance for current user
					//stylingScore = scanStylingScore(s_X, s_INDa, s_INDb, s_INDc, s_w, s_lambda, s_mean, s_std);
					//allStylingScore.add(stylingScore);
				}
				else if (count>=Math.min(id+n,totalUserNum)){
					break;
				}
				count ++;
			}
		return true;
		}
		else{
			return false;
		}
	}
	
	private static double[][] scanStylingScore(double[][] X, double[][] INDa,
			double[][] INDb, double[][] INDc, double[] w,
			double lambda, double[] mean, double[] std) {
		double[][] S = new double[X[0].length][11];
		double[] x = new double[X[0].length];
		double[] x_org = {0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5};
		for(int i=0;i<X[0].length;i++){
			for(int j=0;j<11;j++){
				x = x_org.clone();
				x[i] = (double) j/10.0;
				S[i][j] = decisionf(x, X, d2i(INDa), d2i(INDb), d2i(INDc), w, lambda, mean, std);
			}
		}
		return S;
	}

	private static double calStyle(double[] x){
		double s=0;
		return s;
	}
	private static double calUtility(double s, double[] a){
		double u=0;
		return u;
	} 
	private static double calAverageStyle(double[] x){
		double s=0;
		return s;		
	}	
	private static double calAverageUtility(double s, double[] a){
		double u=0;
		return u;		
	}
	
	public static double s_allDecisionf(double[] x){
		double f = 0;
		try {
			getAllData(0,0);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		for (int i=0;i<totalUserNum;i++){
			f += s_decisionf(x,i);
		}
		return f;
	}
	
	private static double s_decisionf(double[] x, int id){
		double f = 0;
		double[] feature = calOneFeature(x);
		double nfeature = feature.length;
		double[] mean = s_allmean.get(id);
		double[] std = s_allstd.get(id);
		double[][] INDa = s_allINDa.get(id);
		double[][] INDb = s_allINDb.get(id);
		double[][] INDc = s_allINDc.get(id);
		double[][] X = s_allX.get(id);
		double[] w = s_allw.get(id);
		double lambda = s_alllambda[id];
		
		for (int i = 0; i<nfeature; i++){
			feature[i]= (feature[i]-mean[i])/std[i];
		}
		double[] goodfeature; double[] badfeature;
		for (int i = 0; i<INDa.length;i++){
			goodfeature = calOneFeature(X[(int) INDa[i][0]]);
			badfeature = calOneFeature(X[(int) INDa[i][1]]);
			for (int j = 0; j<nfeature; j++){
				goodfeature[j]= (goodfeature[j]-mean[j])/std[j];
				badfeature[j]= (badfeature[j]-mean[j])/std[j];
			}
			f+= w[i]*(kernel(goodfeature,feature, lambda)-kernel(badfeature,feature, lambda));
		}
		for (int i = 0; i<INDb.length;i++){
			goodfeature = calOneFeature(X[(int) INDb[i][0]]);
			badfeature = calOneFeature(X[(int) INDb[i][1]]);
			for (int j = 0; j<nfeature; j++){
				goodfeature[j]= (goodfeature[j]-mean[j])/std[j];
				badfeature[j]= (badfeature[j]-mean[j])/std[j];
			}
			f+= w[INDa.length+i]*(kernel(goodfeature,feature, lambda)-kernel(badfeature,feature, lambda));
		}
		for (int i = 0; i<INDc.length;i++){
			goodfeature = calOneFeature(X[(int) INDc[i][0]]);
			badfeature = calOneFeature(X[(int) INDc[i][1]]);
			for (int j = 0; j<nfeature; j++){
				goodfeature[j]= (goodfeature[j]-mean[j])/std[j];
				badfeature[j]= (badfeature[j]-mean[j])/std[j];
			}
			f+= w[INDa.length+INDb.length+i]*(kernel(goodfeature,feature, lambda)-kernel(badfeature,feature, lambda));
		}
		return f;
	}
	
	private static double decisionf(double[] x, double[][] X, int[][] INDa, int[][] INDb, int[][] INDc, 
			double[] w, double lambda, double[] mean, double[] std){
		double f = 0;
		double[] feature = calOneFeature(x);
		double nfeature = feature.length;
		
		for (int i = 0; i<nfeature; i++){
			feature[i]= (feature[i]-mean[i])/std[i];
		}
		double[] goodfeature; double[] badfeature;
		for (int i = 0; i<INDa.length;i++){
			goodfeature = calOneFeature(X[(int) INDa[i][0]]);
			badfeature = calOneFeature(X[(int) INDa[i][1]]);
			for (int j = 0; j<nfeature; j++){
				goodfeature[j]= (goodfeature[j]-mean[j])/std[j];
				badfeature[j]= (badfeature[j]-mean[j])/std[j];
			}
			f+= w[i]*(kernel(goodfeature,feature, lambda)-kernel(badfeature,feature, lambda));
		}
		for (int i = 0; i<INDb.length;i++){
			goodfeature = calOneFeature(X[(int) INDb[i][0]]);
			badfeature = calOneFeature(X[(int) INDb[i][1]]);
			for (int j = 0; j<nfeature; j++){
				goodfeature[j]= (goodfeature[j]-mean[j])/std[j];
				badfeature[j]= (badfeature[j]-mean[j])/std[j];
			}
			f+= w[INDa.length+i]*(kernel(goodfeature,feature, lambda)-kernel(badfeature,feature, lambda));
		}
		for (int i = 0; i<INDc.length;i++){
			goodfeature = calOneFeature(X[(int) INDc[i][0]]);
			badfeature = calOneFeature(X[(int) INDc[i][1]]);
			for (int j = 0; j<nfeature; j++){
				goodfeature[j]= (goodfeature[j]-mean[j])/std[j];
				badfeature[j]= (badfeature[j]-mean[j])/std[j];
			}
			f+= w[INDa.length+INDb.length+i]*(kernel(goodfeature,feature, lambda)-kernel(badfeature,feature, lambda));
		}
		return f;
	}
	
	private static double kernel(double[] v1, double[] v2, double lambda) {
		int l = v1.length;
		double k = 0;
		for (int i = 0;i<l;i++){
			k+= (v1[i]-v2[i])*(v1[i]-v2[i]);
		}
		k*=lambda;
		k = Math.exp(-k);
		return k;
	}
	
	private static double[][] calFeature(double[][] X){
		// this is hard coded feature calculation
		int n = X.length;
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
	private static double[] calOneFeature(double[] x){
		int ncp = 24;
		int pfeature = ncp*(ncp-1)/2;
		ControlPoints cp = new ControlPoints();
		double[][] controlPoints = cp.getControlPoints(x);
		double[] feature = getFeatures(controlPoints, pfeature);
		return feature;
	}
	
	private static double[] JSON2J(JSONArray a){
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
	private static double[][] JSONM2J(JSONArray a){
		int pp = 0;
		if(a.length()>0){
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
		else{
			return null;
		}
		
	}
	private int[][] JSONMI2J(JSONArray a){
		int pp = 0;
		if(a.length()>0){
			try {
				pp = a.getJSONArray(0).length();
			} catch (JSONException e1) {
				e1.printStackTrace();
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
		else{
			return null;
		}
	}
    
    // this is used to update the crowd-level information after a single user is done
	public static void updateCrowd(Objectify ofy, String data) throws JSONException {
		JSONObject js = new JSONObject(data);
		JSONObject utilityModel = new JSONObject(js.getString("utilityModel"));
		double[] w = JSON2J(utilityModel.getJSONArray("w"));
		double[][] X = JSONM2J(utilityModel.getJSONArray("X"));
		double[] mean = JSON2J(utilityModel.getJSONArray("mean"));
		double[] std = JSON2J(utilityModel.getJSONArray("std"));
		
        // update the crowd-level partworth by adding in the partworth of the current user
        // partworth are for attribute levels
        // and bias is for the additional standalone term induced by 
        // the mean
		double[] partworthraw = calPartworth(X, mean, std, w);
		double[] partworth = new double[partworthraw.length];
		double bias = 0;
		for(int i=0;i<partworth.length;i++){
			partworth[i] = partworthraw[i]/std[i];
			bias += -partworthraw[i]*mean[i]/std[i];
		}
		
        // MTURKnonlinearCrowd is data tag for the crowd model in model 3. 
        // The current study does not require crowd model for model 1 or 2.
		Query<Crowd> q = ofy.query(Crowd.class).filter("name =", "MTURKnonlinearCrowd");
		if (q.count()==0){//if no crowd, create one
			JSONObject js1 = new JSONObject();
			js1.put("crowdPartworth",partworth);
			js1.put("crowdBias",bias);
			js1.put("totalUserNum", 1);
			String s = js1.toString();
			Crowd c = new Crowd(s,"MTURKnonlinearCrowd");
			ofy.put(c);
			assert c.id != null;
		}
		else{// else update current crowd
			Crowd c = ofy.query(Crowd.class).filter("name","MTURKnonlinearCrowd").get();
			JSONObject js1 = new JSONObject(c.data);
			double[] crowdPartworth = JSON2J(js1.getJSONArray("crowdPartworth"));
			double crowdBias = js1.getDouble("crowdBias");
			int totalUserNum = js1.getInt("totalUserNum");
			for(int i=0;i<crowdPartworth.length;i++){
				crowdPartworth[i] = (crowdPartworth[i]*totalUserNum+partworth[i])/(totalUserNum+1);
			}
			crowdBias = (crowdBias*totalUserNum+bias)/(totalUserNum+1);
			totalUserNum += 1;
			js1.put("crowdPartworth",crowdPartworth);
			js1.put("crowdBias",crowdBias);
			js1.put("totalUserNum", totalUserNum);
			String s = js1.toString();
			c.data = s;
			ofy.put(c);
		}
	}

	public static double[] getCrowdPartworth() throws JSONException {
		Objectify ofy = ObjectifyService.begin();
		Crowd c = ofy.query(Crowd.class).filter("name","MTURKnonlinearCrowd").get();
		JSONObject js1 = new JSONObject(c.data);
		double[] crowdPartworth = JSON2J(js1.getJSONArray("crowdPartworth"));
		return crowdPartworth;
	}

	public static int getCrowdSize() throws JSONException {
		Objectify ofy = ObjectifyService.begin();
		Crowd c = ofy.query(Crowd.class).filter("name","MTURKnonlinearCrowd").get();
		JSONObject js1 = new JSONObject(c.data);
		int crowdSize = js1.getInt("totalUserNum");
		return crowdSize;
	}
	
	public static double getCrowdBias() throws JSONException {
		Objectify ofy = ObjectifyService.begin();
		Query<Crowd> q = ofy.query(Crowd.class).filter("name =","MTURKnonlinearCrowd");
		double crowdBias = 0.0;
		if (q.count()!=0){
			Crowd c = ofy.query(Crowd.class).filter("name","MTURKnonlinearCrowd").get();
			JSONObject js1 = new JSONObject(c.data);
			crowdBias = js1.getDouble("crowdBias");		
		}
		return crowdBias;
	}
	
	
	
	private static void train(){
		//rankSVM
		System.out.print("start rankSVM... \n");
		int i; int j;

//		double[][] F = calFeature(X);
		double[][] F = X.clone();
		int nfeature = F[0].length;
		mean = new double[nfeature];
		std = new double[nfeature];
		lambda = 1.0/nfeature;
		double[][] FBar = featureNormalize(F);
		System.out.print("features done... \n");
		
		int counta = 0;
		int countb = 0;
		int countc = 0;
		for (i = 0; i<Y.length; i++){
			if (Y[i]==0){countc++;// two designs are indifferent
			}
			else if (Math.abs(Y[i])==1){countb++;// design i is slightly better than design j
			}
			else if (Math.abs(Y[i])==2){counta++;// design i is much better than design j
			}
		}
		System.out.print("\n");
		
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
			else if (Math.abs(Y[i])==1){
				INDb[ccb][0] = 2*i; INDb[ccb][1] = 2*i+1;
				X1b[ccb] = FBar[2*i];
				X2b[ccb] = FBar[2*i+1];				
				ccb++;
			}
			else if (Math.abs(Y[i])==2){
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
		
		System.out.print("hessian done... \n");
		
		Solver s = new Solver();
		s.Solve(count, H, f, 1E-3);
		w = s.alpha;
	}
	
	private static double kernel(double[] v1, double[] v2) {
		int l = v1.length;
		double k = 0;
		for (int i = 0;i<l;i++){
			k+= (v1[i]-v2[i])*(v1[i]-v2[i]);
		}
		k*=lambda;
		k = Math.exp(-k);
		
//		// linear kernel for styling model
//		int l = v1.length;
//		double k = 0;
//		for (int i = 0;i<l;i++){
//			k+= v1[i]*v2[i];
//		}
		return k;
	}
	
	private static double[][] featureNormalize(double[][] phi) {
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
	
	
	// get optimal design
	public static String readOptimal(int id) throws IOException, JSONException{
		JSONObject sj = new JSONObject();
		if (getAllData(id, 1)){
			double[] optimald = optimalGA();
			
			// output optimal design for user id
			sj.put("optimal", optimald);
			
		}
		return(sj.toString());
	}
	
	private static double[] optimalGA(){
		int POP_LIMIT = 80;
		int NUM_GENERATIONS = 800;
		int TOURNAMENT_ARITY = POP_LIMIT;
		double CROSSOVER_RATE = 1;
		double ELITISM_RATE = 0.9;
		double MUTATION_RATE = 0.1;
		
		double obj = -1;//hard coded objective type
		X = s_allX.get(0);
		mean = s_allmean.get(0);
		p = X[0].length;
		std = s_allstd.get(0);
		w = s_allw.get(0);
		lambda = s_alllambda[0];
		INDa = d2i(s_allINDa.get(0));
		INDb = d2i(s_allINDb.get(0));
		INDc = d2i(s_allINDc.get(0));
		
		CrossoverPolicy crossoverPolicy = (CrossoverPolicy) new OnePointCrossover<Integer>();
		MutationPolicy mutationPolicy = (MutationPolicy) new RealMutation();
		SelectionPolicy selectionPolicy = new TournamentSelection(TOURNAMENT_ARITY);
		GeneticAlgorithm ga = new GeneticAlgorithm(crossoverPolicy, CROSSOVER_RATE, 
				mutationPolicy, MUTATION_RATE, selectionPolicy,
				X, X, w, lambda, mean, std, INDa, INDb, INDc, obj, X);
		
		Population initial = getInitialPopulation(POP_LIMIT, ELITISM_RATE, X);
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
		obj = decisionf(t, X, INDa, INDb, INDc,	w, lambda, mean, std);
		
		return t;
	}
	
	private static int[][] d2i(double[][] x){
		int[][] I;
		if(x != null){
			I = new int[x.length][x[0].length];
			for(int i=0;i<x.length;i++){
				for(int j=0;j<x[0].length;j++){
					I[i][j] = (int) x[i][j];
				}
			}	
		}
		else{
			I = new int[0][0];
		}
		return I;
	}
	
	private static Population getInitialPopulation(int POP_LIMIT, double ELITISM_RATE, double[][] SV) {
		List<Chromosome> chromosomes = initializeChromosomes(POP_LIMIT, SV);
		Population pop = new ElitisticListPopulation(chromosomes, POP_LIMIT, ELITISM_RATE);
		return pop;
	}

	private static List<Chromosome> initializeChromosomes(int POP_LIMIT, double[][] sv) {
		List<Chromosome> chromosomes = new ArrayList<Chromosome>(POP_LIMIT);
		for (int i = 0; i<POP_LIMIT; i++){
			List<Double> representation = RealChromosome.randomRealRepresentation(p);
			chromosomes.add(new RealChromosome(representation, 
					decisionf(list2prim(representation), X, INDa, INDb, INDc, w, lambda, mean, std)));
		}
		return chromosomes;
	}
	private static double[] list2prim(List<Double> l) {
		double[] x = new double[l.size()];
		for (int i = 0; i<x.length; i++) {
			x[i] = l.get(i);
		}
		return x;
	}
}
