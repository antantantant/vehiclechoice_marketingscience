package namwoo2013;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.Query;

public class postAnalysisDOE {
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
	static List<double[][]> allValidateX;
	static List<double[][]> allValidateA;
	static List<double[]> alluserY;
	static List<String> allSurvey;
	static int totalUserNum;
	static private double[][] X;
	static private int p;
	static private int n;
	static private double lambda;
	static private double[] w;
	static private double[] Y;
	static private int[][] INDa;
	static private int[][] INDb;
	static private int[][] INDc;
	static private double[] mean;
	static private double[] std;
	
	public static String parseDOE(String data) throws JSONException{
		JSONObject js = new JSONObject(data);
		  
//		JSONArray X_temp = js.getJSONArray("X");
//		JSONArray Y_temp = js.getJSONArray("Y");
		
//		X = JSONM2J(X_temp);
//		p = X[0].length;
//		n = X.length;
//		
//		double[] Yt = JSON2J(Y_temp);
//		double[] tempX = new double[p];
//		Y = new double[Yt.length]; 
//		for (int i = 0; i<Yt.length; i++){
//			if (Yt[i]<0){
//				tempX = X[i*2].clone();
//				X[i*2]=X[i*2+1].clone();
//				X[i*2+1] = tempX.clone();
//				Y[i] = -Yt[i];
//			}
//			else{
//				Y[i] = Yt[i];
//			}
//			
//		}
//		  
//		System.out.print("start learning with ranking SVM... \n");
//		  
//		train();
		
//		JSONObject sm = new JSONObject();
//		sm.put("INDa", INDa);
//		sm.put("INDb", INDb);
//		sm.put("INDc", INDc);
//		sm.put("lambda", lambda);
//		sm.put("w", w);
//		sm.put("X", X);
//		sm.put("Y", Yt);
//		sm.put("mean",mean);
//		sm.put("std",std);
		JSONObject sj = new JSONObject();
//		sj.put("styleModel", sm.toString());
		sj.put("X", js.getJSONArray("X"));
		sj.put("A", js.getJSONArray("A"));
		sj.put("purchaseY", js.getJSONArray("purchaseY"));
		sj.put("validateX", js.getJSONArray("validateX"));
		sj.put("validateA", js.getJSONArray("validateA"));
		sj.put("validateY", js.getJSONArray("validateY"));
		sj.put("survey", js.getString("survey"));
		return sj.toString();
	}
	
	public static String read(int id, int n) throws IOException, JSONException{
		JSONObject sj = new JSONObject();
		if (getAllData(id, n)){
	
			List<double[][]> allTrainingFeature = allFeatures(s_allX);
			List<double[][]> allValidateFeature = allFeatures(allValidateX);

			sj.put("s_allX", s_allX);
	//		sj.put("s_allY", s_allY);
	//		sj.put("s_allINDa", s_allINDa);
	//		sj.put("s_allINDb", s_allINDb);
	//		sj.put("s_allINDc", s_allINDc);
	//		sj.put("s_allw", s_allw);
			sj.put("s_allfeature", allTrainingFeature);
	//		sj.put("s_allmean", s_allmean);
	//		sj.put("s_allstd", s_allstd);
			sj.put("u_allX", u_allX);
			sj.put("u_allY", u_allY);
			sj.put("allValidateX", allValidateX);
			sj.put("allValidateA", allValidateA);
			sj.put("allValidateFeature", allValidateFeature);
			sj.put("alluserY", alluserY);
			sj.put("allSurvey", allSurvey);
		}
		return(sj.toString());
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
	
	private static Boolean getAllData(int id, int n) throws JSONException{
		Objectify ofy = ObjectifyService.begin();
		Query<TestDOE> q = ofy.query(TestDOE.class).filter("name", "MTurkDOE");
		
		totalUserNum = q.count();
		
		if (id+n<=totalUserNum){
	
			s_allX = new ArrayList<double[][]>(); //iter * feature * user
//			s_allY = new ArrayList<double[]>();
//			s_allINDa = new ArrayList<double[][]>();
//			s_allINDb = new ArrayList<double[][]>();
//			s_allINDc = new ArrayList<double[][]>();
//			s_allw = new ArrayList<double[]>();
//			s_allmean = new ArrayList<double[]>();
//			s_allstd = new ArrayList<double[]>();
			u_allX = new ArrayList<double[][]>();
			u_allY = new ArrayList<double[]>();
			allValidateX = new ArrayList<double[][]>();
			allValidateA = new ArrayList<double[][]>();
			alluserY = new ArrayList<double[]>();
			allSurvey = new ArrayList<String>();
			
			double[][] s_X;
//			double[] s_Y;
//			double[][] s_INDa;
//			double[][] s_INDb;
//			double[][] s_INDc;
//			double s_lambda;
//			double[] s_w;
//			double[] s_mean;
//			double[] s_std;
			
			double[][] u_X;
			double[] u_Y;
			
			double[][] validateX;
			double[][] validateA;
			double[] userY;
			
			String survey;
			
//			s_alllambda = new double[Math.min(totalUserNum-id, n)];
			int count = 0;
			for (TestDOE t:q){
				if (count>=id&&count<Math.min(id+n,totalUserNum)){
					String data = t.data;
					JSONObject js = new JSONObject(data);
//					JSONObject styleModel = new JSONObject(js.getString("styleModel"));
					JSONArray s_X_ = js.getJSONArray("X");
//					JSONArray s_Y_ = styleModel.getJSONArray("Y");
//					JSONArray s_INDa_ = styleModel.getJSONArray("INDa");
//					JSONArray s_INDb_ = styleModel.getJSONArray("INDb");
//					JSONArray s_INDc_ = styleModel.getJSONArray("INDc");
//					s_lambda = styleModel.getDouble("lambda");
//					JSONArray s_w_ = styleModel.getJSONArray("w");
//					JSONArray s_mean_ = styleModel.getJSONArray("mean");
//					JSONArray s_std_ = styleModel.getJSONArray("std");
					s_X = JSONM2J(s_X_);
//					s_Y = JSON2J(s_Y_);
//					s_INDa = JSONM2J(s_INDa_);
//					s_INDb = JSONM2J(s_INDb_);
//					s_INDc = JSONM2J(s_INDc_);
//					s_w = JSON2J(s_w_);
//					s_mean = JSON2J(s_mean_);
//					s_std = JSON2J(s_std_);
					
					JSONArray u_X_ = js.getJSONArray("A");
					JSONArray u_Y_ = js.getJSONArray("purchaseY");
					u_X = JSONM2J(u_X_);
					u_Y = JSON2J(u_Y_);
					
					JSONArray validateX_ = js.getJSONArray("validateX");
					JSONArray validateA_ = js.getJSONArray("validateA");
					validateX = JSONM2J(validateX_);
					validateA = JSONM2J(validateA_);
					
					JSONArray userY_ = js.getJSONArray("validateY");
				    userY = JSON2J(userY_);
				    
				    survey = js.getString("survey");
				    
					s_allX.add(s_X);
//					s_allY.add(s_Y);
//					s_allINDa.add(s_INDa);
//					s_allINDb.add(s_INDb);
//					s_allINDc.add(s_INDc);
//					s_allw.add(s_w);
//					s_alllambda[0] = s_lambda;
//					s_allmean.add(s_mean);
//					s_allstd.add(s_std);
					u_allX.add(u_X);
					u_allY.add(u_Y);
					allValidateX.add(validateX);
					allValidateA.add(validateA);
					alluserY.add(userY);
					allSurvey.add(survey);
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
	
	private static void train(){
		//rankSVM
		System.out.print("start rankSVM... \n");
		int i; int j;

		double[][] F = calFeature(X);
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
			else if (Y[i]==1){countb++;// design i is slightly better than design j
			}
			else if (Y[i]==2){counta++;// design i is much better than design j
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
		
		System.out.print("hessian done... \n");
		
		Solver s = new Solver();
		s.Solve(count, H, f, 1E-3);
		w = s.alpha;
	}
	
	static private double[][] featureNormalize(double[][] phi) {
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
	static private double kernel(double[] v1, double[] v2) {
		int l = v1.length;
		double k = 0;
		for (int i = 0;i<l;i++){
			k+= (v1[i]-v2[i])*(v1[i]-v2[i]);
		}
		k*=lambda;
		k = Math.exp(-k);
		return k;
	}
}

