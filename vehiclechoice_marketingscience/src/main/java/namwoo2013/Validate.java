package namwoo2013;

import java.io.*;

import org.json.*;

import au.com.bytecode.opencsv.CSVReader;

import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.Query;

public class Validate {
	public static String getValidateSet(String ns) throws IOException, JSONException{
		// read from existing validation file
		int n = Integer.parseInt(ns);
		String strFile = "resource/validation.csv";
		CSVReader reader = new CSVReader(new FileReader(strFile));
		JSONObject js = new JSONObject();
		String [] nextLine;
		double[][] X = new double[n][19];
		double[][] A = new double[n][2];
        
        // MTURKnonlinear is used for model 3, for model 1 and 2, please use their own data tag
        // each model/experiment should use its own data tag to fetch its validation set
		Objectify ofy = ObjectifyService.begin();
		Query<Test> q = ofy.query(Test.class).filter("model =","MTURKnonlinear");
		int total = q.count();

		int lineNumber = 0;
		while (lineNumber < total*n/2) {
			nextLine = reader.readNext();
			lineNumber++;
		}
		while (lineNumber < (total+1)*n/2 ){
			nextLine = reader.readNext();
			for (int i=1;i<20;i++){
				X[lineNumber*2-total*n][i-1] = Double.parseDouble(nextLine[i]);
			}
			for (int i=20;i<22;i++){
				A[lineNumber*2-total*n][i-20] = Double.parseDouble(nextLine[i]);
			}
			for (int i=22;i<41;i++){
				X[lineNumber*2+1-total*n][i-22] = Double.parseDouble(nextLine[i]);
			}
			for (int i=41;i<43;i++){
				A[lineNumber*2+1-total*n][i-41] = Double.parseDouble(nextLine[i]);
			}	
			lineNumber++;
		}
		js.put("X", X);
		js.put("A", A);
		js.put("user_id", total);
		reader.close();

		JSONObject sj = new JSONObject();
		sj.put("X", X);
		sj.put("A", A);
		String s = sj.toString();
		return s;
	}

}
