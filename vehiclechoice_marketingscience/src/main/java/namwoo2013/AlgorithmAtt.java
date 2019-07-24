package namwoo2013;

import java.io.*;
import java.util.List;

import org.json.*;

public class AlgorithmAtt {
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
	private double level1;
	private double level2;
	private boolean counterbalance;
	private boolean crowdinfo;
	private double[] crowdPartworth;
	private double crowdBias;
	private int crowdSize;
	
	public static String doTrain(String quest) throws IOException, JSONException{
		AlgorithmAtt t = new AlgorithmAtt();
		return t.SVMtrain(quest);
	}
	public static String doCal(String quest, String cb, String ci) throws IOException, JSONException{
		AlgorithmAtt t = new AlgorithmAtt();
		return t.parse(quest, cb, ci);
	}
	
	public String SVMtrain(String data) throws JSONException {
		  
		  JSONObject js = new JSONObject(data);
		  p = js.getInt("dim");
		  n = js.getInt("num");
		  
		  JSONArray X_temp = js.getJSONArray("dat");
		  JSONArray Y_temp = js.getJSONArray("lab");
		  X = JSONM2J(X_temp);
		  double[] Yt = JSON2J(Y_temp);
		  Y = new double[Yt.length]; 
		  for (int i = 0; i<Yt.length; i++){
			  Y[i] = Math.abs(Yt[i]);
		  }
		  
		  System.out.print("start learning with ranking SVM... \n");
		  
		  train();
		  
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
		  String s = sj.toString();
		  return(s);
	}
	
	private void train(){
		//rankSVM
		System.out.print("start rankSVM... \n");
		int i; int j;

		double[][] F = X;
		int nfeature = F[0].length;
		mean = new double[nfeature];
		std = new double[nfeature];
		lambda = -1; // in realchromosome.java, we use lambda to switch between gaussian and linear kernels.
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
		double[] wraw = s.alpha;
		
		// normalized w
		w = new double[wraw.length];
		double sum = 0;
		for (i=0;i<wraw.length;i++){
			sum += Math.abs(wraw[i]);
		}
		for (i=0;i<wraw.length;i++){
			w[i] = wraw[i]/sum;
			if (Double.isNaN(w[i])){
				w[i]=1.0/wraw.length;
			}
		}
	}
	
	private double kernel(double[] v1, double[] v2) {
		// linear kernel for utility model
		int l = v1.length;
		double k = 0;
		for (int i = 0;i<l;i++){
			k+= v1[i]*v2[i];
		}
		return k;
	}
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
	
	private String parse(String array, String cb, String ci) throws JSONException {
		  counterbalance = cb.equals("true");
		  crowdinfo = ci.equals("1");
		  if (crowdinfo){
			  crowdBias = postAnalysis.getCrowdBias();
			  if (crowdBias !=0){
				  crowdPartworth = postAnalysis.getCrowdPartworth();
				  crowdSize = postAnalysis.getCrowdSize();
			  }
		  }
		  
		  try {
			  JSONObject js = new JSONObject(array);
			  p = js.getInt("dim");
			  n = js.getInt("num");
			  obj = js.getDouble("obj");
			  level1 = js.getInt("level1");
			  level2 = js.getInt("level2");
			  X = JSONM2J(js.getJSONArray("A"));
			  
			  JSONObject svmPar = new JSONObject(js.getString("svmPar"));

			  lambda = svmPar.getDouble("lambda");
			  w = JSON2J(svmPar.getJSONArray("w"));
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
			  
		  	  System.out.print("\nstart searching with objective: "+obj+"... \n");
			  String s = null;
			  if (w.length==0){
				  Scatter_null();
				  js.put("A", new JSONArray(X));
				  nsample++;
				  svmPar.put("nsample", nsample);
				  js.put("obj",obj);
				  js.put("svmPar",svmPar);
				  s = js.toString();
				  s = "data="+s;
			  }
			  else {
				  Scatter();
				  js.put("A", new JSONArray(X));
				  nsample++;
				  svmPar.put("nsample", nsample);
				  js.put("obj",obj);
				  js.put("svmPar",svmPar);
				  js.put("partworth",partworth());
				  s = js.toString();
				  s = "data="+s;
			  }
			  return(s);
		  }
		  catch (JSONException e) {
			  JSONObject js = new JSONObject(array);
			  String s = js.toString();
			  s = "data="+s;
		  }
		  return("");
	  }
	
	private void Scatter() throws JSONException{
		// Get new points
		double[] t = new double[p];
		double[] ta = new double[p];
		double[] tb = new double[p];
		int i,j;
		
		if (obj==0){ // find the attribute set with max min distance
			double[][] T = new double[n-2][p];
			for (i=0;i<n-2;i++){
				T[i] = X[i];
			}
			double[] f_set = new double[(int) (level1*level2)];
			int count = 0;
			
			double f = 0;
			for (i=0;i<level1;i++){
				ta = new double[p];
				ta[0] = X[X.length-2][0]; // styling score for the first design of the pair
				if (i>0){ta[i] = 1;}
				for (j=0;j<level2;j++){
					tb = ta.clone();
					if (j>0){tb[(int) (level1+j-1)] = 1;}
					if ((X[X.length-2][0]<X[X.length-1][0]&&
							(i!=(int)(level1-1)||j!=0))||
							(X[X.length-2][0]>X[X.length-1][0]&&
							(i!=0||j!=(int)(level2-1)))){
						f_set[(int) (i*level2+j)] = distance(tb,T);
						if (f_set[(int) (i*level2+j)]>f){
							f = f_set[(int) (i*level2+j)];
							count = 1;
						}
						else if (f_set[(int) (i*level2+j)]==f){
							count ++;
						}
					}
				}
			}
			
			int a = (int) Math.floor(Math.random()*count);
			count = 0;
			for (i=0;i<level1;i++){
				for (j=0;j<level2;j++){
					if (f_set[(int) (i*level2+j)]==f){
						if (count == a){
							if(i>0){X[X.length-2][i] = 1;}
							if(j>0){X[X.length-2][(int)(level1+j-1)] = 1;}
							obj = decisionf(X[X.length-2]);
							return;
						}
						else{
							count++;
						}
					}
				}
			}
		}
		else{
			double[][] T = new double[n-1][p];
			for (i=0;i<n-1;i++){
				T[i] = X[i];
			}
			
			double f = 0.0;
			double fnew = 0.0;
			int level1Comparison = 0;
			int level2Comparison = 0;
			for (i=0;i<level1-1;i++){
				level1Comparison+=X[X.length-2][1+i]*(1+i);
			}
			for (i=0;i<level2-1;i++){
				level2Comparison+=X[X.length-2][(int)(level1+i)]*(1+i);
			}
			for (i=0;i<level1;i++){
				ta = new double[p];
				ta[0] = X[X.length-1][0]; // styling score for the second design of the pair
				if (i>0){ta[i] = 1;}
				for (j=0;j<level2;j++){
					tb = ta.clone();
					if (j>0){tb[(int) (level1+j-1)] = 1;}
					if (X[X.length-2][0]>X[X.length-1][0]){
						if (i<level1Comparison||j>level2Comparison){
							fnew = calculateFitness(tb,T);
							if (fnew>f){
								t = tb.clone();
								f = fnew;
							}
						}				
					}
					else {
						if (i>level1Comparison||j<level2Comparison){
							fnew = calculateFitness(tb,T);
							if (fnew>f){
								t = tb.clone();
								f = fnew;
							}
						}						
					}
				}
			}
			X[X.length-1] = t.clone();
		}
	}

	private void Scatter_null() throws JSONException{
		// Get new points
		double[] t = new double[p];
		for (int j = 0; j<p; j++){
			t[j] = Math.random();
		}
		X = arrayadd(t,X);
	}
	
	private double calculateFitness(double[] x, double[][] X) {
		double f = Math.exp(-Math.abs(decisionf(x)-obj));
		double d = distance(x, X);
		double fitness = merit(0.9,0.1,f,d);
		return fitness;
	}
	
	private double merit(double w1, double w2, double f, double s) {
		double fitness = w1*f+w2*s;
		return fitness;
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
		   int aLen = A.length;
		   int bLen = B.length;
		   double[][] C= new double[aLen+bLen][A[0].length];
		   System.arraycopy(A, 0, C, 0, aLen);
		   System.arraycopy(B, 0, C, aLen, bLen);
		   return C;
	}
	
	private double distance(double[] x, double[][] X) {
		double f = 1e12;
		double g;
		
		// find the largest and smallest in styling score
		double maxs = 0;
		double mins = 1e12;
		for (int i=0; i<X.length; i++){
			if (X[i][0]>maxs){maxs=X[i][0];}
			if (X[i][0]<mins){mins=X[i][0];}
		}
		
		for(int i=0;i<X.length;i++){
			g = 0.0;
			//styling difference
			g += Math.abs(x[0]-X[i][0])/(maxs-mins); 
			//price difference
			for (int j=0;j<level1-1;j++){
				if(x[1+j]-X[i][1+j]!=0){
					g+=1;break;
				}
			}
			for (int j=0;j<level2-1;j++){
				if(x[(int)(level1+j)]-X[i][(int)(level1+j)]!=0){
					g+=1;break;
				}
			}
			f = Math.min(f,g);
		}
		return f;
	}
	
	private double[] partworth(){
		int nfeature = X[0].length;
		double[] partworth = new double[nfeature];
		for (int i = 0; i<nfeature; i++){
			partworth[i] = 0.0;
		}
		double[] goodfeature; double[] badfeature;
		for (int i = 0; i<INDa.length;i++){
			goodfeature = X[INDa[i][0]].clone();
			badfeature = X[INDa[i][1]].clone();
			for (int j = 0; j<nfeature; j++){
				goodfeature[j]= (goodfeature[j]-mean[j])/std[j];
				badfeature[j]= (badfeature[j]-mean[j])/std[j];
				partworth[j] += w[i]*(goodfeature[j]-badfeature[j]);
			}
		}
		for (int i = 0; i<INDb.length;i++){
			goodfeature = X[INDb[i][0]].clone();
			badfeature = X[INDb[i][1]].clone();
			for (int j = 0; j<nfeature; j++){
				goodfeature[j]= (goodfeature[j]-mean[j])/std[j];
				badfeature[j]= (badfeature[j]-mean[j])/std[j];
				partworth[j] += w[i]*(goodfeature[j]-badfeature[j]);
			}
		}
		for (int i = 0; i<INDc.length;i++){
			goodfeature = X[INDc[i][0]].clone();
			badfeature = X[INDc[i][1]].clone();
			for (int j = 0; j<nfeature; j++){
				goodfeature[j]= (goodfeature[j]-mean[j])/std[j];
				badfeature[j]= (badfeature[j]-mean[j])/std[j];
				partworth[j] += w[i]*(goodfeature[j]-badfeature[j]);
			}
		}
		for (int i = 0; i<level1-1; i++){
			if (partworth[1+i]>0){partworth[1+i]=-1e-6;} // lower price is always preferred
		}
		for (int i = 0; i<level2-1; i++){
			if (partworth[(int)(level1+i)]<0){partworth[(int)(level1+i)]=-1e-6;} // higher MPG is always preferred
		}
		return partworth;
	}
	
	private double decisionf(double[] x){
		double f = 0;
		double[] feature = x.clone();
		int nfeature = feature.length;
		for (int i = 0; i<nfeature; i++){
			feature[i]= (feature[i]-mean[i])/std[i];
		}
		double[] partworth = new double[nfeature];
		for (int i = 0; i<nfeature; i++){
			partworth[i] = 0.0;
		}
		double[] goodfeature; double[] badfeature;
		for (int i = 0; i<INDa.length;i++){
			goodfeature = X[INDa[i][0]].clone();
			badfeature = X[INDa[i][1]].clone();
			for (int j = 0; j<nfeature; j++){
				goodfeature[j]= (goodfeature[j]-mean[j])/std[j];
				badfeature[j]= (badfeature[j]-mean[j])/std[j];
				partworth[j] += w[i]*(goodfeature[j]-badfeature[j]);
			}
		}
		for (int i = 0; i<INDb.length;i++){
			goodfeature = X[INDb[i][0]].clone();
			badfeature = X[INDb[i][1]].clone();
			for (int j = 0; j<nfeature; j++){
				goodfeature[j]= (goodfeature[j]-mean[j])/std[j];
				badfeature[j]= (badfeature[j]-mean[j])/std[j];
				partworth[j] += w[i]*(goodfeature[j]-badfeature[j]);
			}
		}
		for (int i = 0; i<INDc.length;i++){
			goodfeature = X[INDc[i][0]].clone();
			badfeature = X[INDc[i][1]].clone();
			for (int j = 0; j<nfeature; j++){
				goodfeature[j]= (goodfeature[j]-mean[j])/std[j];
				badfeature[j]= (badfeature[j]-mean[j])/std[j];
				partworth[j] += w[i]*(goodfeature[j]-badfeature[j]);
			}
		}
		for (int i = 0; i<level1-1; i++){
			if (partworth[1+i]>0){partworth[1+i]=-1e-6;} // lower price is always preferred
		}
		for (int i = 0; i<level2-1; i++){
			if (partworth[(int)(level1+i)]<0){partworth[(int)(level1+i)]=-1e-6;} // higher MPG is always preferred
		}
		
		for (int i=0; i<nfeature; i++){
			f += partworth[i]*x[i];
		}
		
		if (crowdinfo && crowdBias!=0.0){
			double fcrowd = 0.0;
			for (int i=0; i<crowdPartworth.length; i++){
				fcrowd += crowdPartworth[i]*x[i];
			}
			fcrowd += crowdBias;
			double rho = 0.75/200.0*crowdSize;
			f = (1-rho)*f + rho*fcrowd;
		}
		return f+1e-128;
	}
	
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
	private int[][] JSONMI2J(JSONArray a){
		int pp = 0;
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
	
	private AlgorithmAtt() {} // Uninstantiatable class
}
