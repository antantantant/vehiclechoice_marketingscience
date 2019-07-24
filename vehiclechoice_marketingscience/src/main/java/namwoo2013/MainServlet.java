// this is the main entrance to the server-side code
package namwoo2013;

// import packages needed for data storage on Google App Engine and other stuff
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.http.*;
import org.json.JSONException;
import org.json.JSONObject;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.Query;


@SuppressWarnings("serial")
public class MainServlet extends HttpServlet {
	// declare data classes
    static{
		ObjectifyService.register(Test.class);
		ObjectifyService.register(Crowd.class);
		ObjectifyService.register(TestDOE.class);
	}
	
    // store data for the ADAPTIVE experiment (model 3)
	private void store(HttpServletRequest request,HttpServletResponse response)throws ServletException,IOException {
		Objectify ofy = ObjectifyService.begin(); 
		response.setHeader("Cache-Control", "no-cache");
		response.setHeader("Pragma", "no-cache");
		String data = request.getParameter("data");
		try {
            // Test name is currently set to MTURKnonlinear
            // To run a new experiment, either delete existing data 
            // or replace MTURKnonlinear with something else in all codes
			Test t = new Test(data, "MTURKnonlinear");
			ofy.put(t);
			assert t.id != null;
			
			// update crowd info
			postAnalysis.updateCrowd(ofy, data);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
    // store data for the DOE experiment (model 1)
	private void storeDOE(HttpServletRequest request,HttpServletResponse response)throws ServletException,IOException, JSONException {
		Objectify ofy = ObjectifyService.begin(); 
		response.setHeader("Cache-Control", "no-cache");
		response.setHeader("Pragma", "no-cache");
		String data = request.getParameter("data");
		
		String s = postAnalysisDOE.parseDOE(data);
		TestDOE t = new TestDOE(s, "MTurkDOE");
		ofy.put(t);
		assert t.id != null;
	}
	
	// store data for the bi-level DOE (model 2)
	private void storeDOE2(HttpServletRequest request,HttpServletResponse response)throws ServletException,IOException, JSONException {
		Objectify ofy = ObjectifyService.begin(); 
		response.setHeader("Cache-Control", "no-cache");
		response.setHeader("Pragma", "no-cache");
		String data = request.getParameter("data");
		
		String s = postAnalysisDOE2.parseDOE(data);
		TestDOE t = new TestDOE(s, "MTurkDOE2");
		ofy.put(t);
		assert t.id != null;
	}
	
    // select action based on action type: store/storeDOE/storeDOE2/train/...
	public void doPost(HttpServletRequest request,
	               HttpServletResponse response)
	throws ServletException, IOException {
		String action = request.getParameter("action");
		if (action.equals("store")){
			store(request, response);
		}
		else if (action.equals("storeDOE")){
			try {
				storeDOE(request, response);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else if (action.equals("storeDOE2")){
			try {
				storeDOE2(request, response);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
        
        // train the SVM styling model (model 3)
		else if (action.equals("train")){
			try {
				String model = Algorithm.doTrain(request.getParameter("data"),
						request.getParameter("counterbalance"),request.getParameter("crowdinfo"));
				PrintWriter out = response.getWriter();
				out.print(model);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
        
        // get styling parameters for the next query (model 3)
		else if (action.equals("getTest")){
			try {
				String model = Algorithm.doCal(request.getParameter("data"),
						request.getParameter("counterbalance"),request.getParameter("crowdinfo"));
				PrintWriter out = response.getWriter();
				out.print(model);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		
        // train the linear purchase model (model 3)
        else if (action.equals("trainAtt")){
			try {
				String model = AlgorithmAtt.doTrain(request.getParameter("data"));
				PrintWriter out = response.getWriter();
				out.print(model);
			} catch (JSONException e) {
				e.printStackTrace();
			}			
		}
        
        // get purchase attribute levels for the next query (model 3)
		else if (action.equals("getAtt")){
			try {
				String model = AlgorithmAtt.doCal(request.getParameter("data"),
						request.getParameter("counterbalance"),request.getParameter("crowdinfo"));
				PrintWriter out = response.getWriter();
				out.print(model);
			} catch (JSONException e) {
				e.printStackTrace();
			}			
		}
        
        // get validate set (model 3)
		else if (action.equals("validate")){
			try {
				String set = Validate.getValidateSet(request.getParameter("n"));
				PrintWriter out = response.getWriter();
				out.print(set);
			} catch (JSONException e) {
				e.printStackTrace();
			}			
		}
        
        // get validate set for DOE (model 1,2)
		else if (action.equals("validateDOE")){
			try {
				String set = ValidateDOE.getValidateSet(request.getParameter("n"));
				PrintWriter out = response.getWriter();
				out.print(set);
			} catch (JSONException e) {
				e.printStackTrace();
			}			
		}
        
        // read data for post-analysis (model 3)
		else if (action.equals("read")){
			try {
				int id = Integer.parseInt(request.getParameter("id"));
				int n = Integer.parseInt(request.getParameter("n"));
				String set = postAnalysis.read(id, n);
				PrintWriter out = response.getWriter();
				out.print(set);
			} catch (JSONException e) {
				e.printStackTrace();
			}			
		}
        
        // find optimal design for post-analysis
		else if (action.equals("readOptimal")){
			try {
				int id = Integer.parseInt(request.getParameter("id"));
				String set = postAnalysis.readOptimal(id);
				PrintWriter out = response.getWriter();
				out.print(set);
			} catch (JSONException e) {
				e.printStackTrace();
			}			
		}
        
        // read data for post-analysis (model 1)
		else if (action.equals("readDOE")){
			try {
				int id = Integer.parseInt(request.getParameter("id"));
				int n = Integer.parseInt(request.getParameter("n"));
				String set = postAnalysisDOE.read(id, n);
				PrintWriter out = response.getWriter();
				out.print(set);
			} catch (JSONException e) {
				e.printStackTrace();
			}			
		}
        
        // read data for post-analysis (model 2)
		else if (action.equals("readDOE2")){
			try {
				int id = Integer.parseInt(request.getParameter("id"));
				int n = Integer.parseInt(request.getParameter("n"));
				String set = postAnalysisDOE2.read(id, n);
				PrintWriter out = response.getWriter();
				out.print(set);
			} catch (JSONException e) {
				e.printStackTrace();
			}			
		}
        
        // get DOE data for the next query (model 1,2)
		else if (action.equals("getDOE")){
			try {
				String data = Algorithm.getDOE(request.getParameter("n"));
				PrintWriter out = response.getWriter();
				out.print(data);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		
        
        // get hitRate, post-analysis, not implemented
		else if (action.equals("hitRate")){
			//	String hitRate = postAnalysis.calHitRate(request.getParameter("data"));
			String hitRate = "";
			PrintWriter out = response.getWriter();
			out.print(hitRate);	
		}
        
        // get code for mturk people
		else if (action.equals("mturkGetCode")){
			mturkGetCode(request, response);
		}
	}

	private void mturkGetCode(HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		response.setHeader("Cache-Control", "no-cache");
		response.setHeader("Pragma", "no-cache");
		PrintWriter out = response.getWriter();
        // manually set mturk code here
//		out.print("1414");
		out.print("7311");
//		out.print("5831");
	}
}