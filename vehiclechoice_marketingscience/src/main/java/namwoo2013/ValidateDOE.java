package namwoo2013;

import java.io.*;

import org.json.*;

import au.com.bytecode.opencsv.CSVReader;

import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.Query;

public class ValidateDOE {
	public static String getValidateSet(String ns) throws IOException, JSONException{
//		JSONObject js = new JSONObject(data);
//		int p = js.getInt("p");
//		int attp = js.getInt("attp");
//		int nvalidate = js.getInt("nvalidate");
//		int level1 = js.getInt("level1");
//		int level2 = js.getInt("level2");
//		
//		double[][] X = new double[nvalidate*2][p];
//		double[][] A = new double[nvalidate*2][attp];
//		for (int i=0;i<nvalidate*2;i++){
//			for(int j=0;j<p;j++){
//				X[i][j] = Math.random();
//			}
//			A[i][0] = 0.0;
//			A[i][1] = Math.floor(Math.random() * level1)/(level1-1);
//			A[i][2] = Math.floor(Math.random() * level2)/(level2-1);
//		}
		
		// read from existing validation file
		int n = Integer.parseInt(ns);
		String strFile = "resource/validation.csv";
		CSVReader reader = new CSVReader(new FileReader(strFile));
		JSONObject js = new JSONObject();
		String [] nextLine;
		double[][] X = new double[n][19];
		double[][] A = new double[n][2];

		Objectify ofy = ObjectifyService.begin();
		Query<TestDOE> q = ofy.query(TestDOE.class).filter("name", "MTurkDOE2");
		int totalDOE = q.count();

		int lineNumber = 0;
		while (lineNumber < totalDOE*n/2) {
			nextLine = reader.readNext();
			lineNumber++;
		}
		while (lineNumber < (totalDOE+1)*n/2 ){
			nextLine = reader.readNext();
			for (int i=1;i<20;i++){
				X[lineNumber*2-totalDOE*n][i-1] = Double.parseDouble(nextLine[i]);
			}
			for (int i=20;i<22;i++){
				A[lineNumber*2-totalDOE*n][i-20] = Double.parseDouble(nextLine[i]);
			}
			for (int i=22;i<41;i++){
				X[lineNumber*2+1-totalDOE*n][i-22] = Double.parseDouble(nextLine[i]);
			}
			for (int i=41;i<43;i++){
				A[lineNumber*2+1-totalDOE*n][i-41] = Double.parseDouble(nextLine[i]);
			}	
			lineNumber++;
		}
		js.put("X", X);
		js.put("A", A);
		js.put("user_id", totalDOE);
		reader.close();

		JSONObject sj = new JSONObject();
		sj.put("X", X);
		sj.put("A", A);
		String s = sj.toString();
		return s;
	}

}
