// Namwoo and Max project
// only for pairwise comparison

p = 19; //number of variables
priceLevel = 5; // level of prices
//basePrice = 23000; // minimum price
//priceRange = 12000; // range of price, maximumPrice = basePrice + priceRange
priceSet = ['23', '25', '26', '29', '31'];
MPGLevel = 5; // level of prices
//baseMPG = 20; // minimum price
//MPGRange = 15; // range of price, maximumPrice = basePrice + priceRange
MPGSet = ['23/27', '23/29', '24/30', '25/31', '26/32'];
attp = priceLevel + MPGLevel-1; //number of purchase attributes

ntest = 10; // number of iterations
nvalidate = 5; // number of validation tests

crowdinfo = 0; // 0: active learning on individual only, 1: use crowd info // changed to 0 from 1 by MAX on 08162016 to speed up the demonstration for the paper
indifferentChoice = 0; // switch on/off indifferent choice option for users
purchaseChoice = 0; // 0: pairwise choice only, 1: rating

question1 = "<a style='color: #BA9488;'> Styling question:</a> Which of the following styles do you prefer?";
question2 = "<a style='color: #88AEBA;'> Purchase question:</a> Which car will you be more likely to buy?";

counterbalance = false; // start with normal query, then switch to counterbalance every other iteration

// test object
function test(){
	this.ntest = ntest;
	this.nvalidate = nvalidate;
	this.X = []; // accumulated design style parameters
	this.A = []; // accumulated design attributes
	this.Y = []; // 2:left much better than right, 1:left better than right, 0:neutral, 
	//	-1:right better than left, -2:right much better than left
	this.purchaseY = []; // choice combines style and other attributes
	this.n = 2; // total number of designs shown in one iteration
	this.needn = 2; // number of designs needed in this iteration
	this.currentX = []; // current designs
	this.currentA = []; // current attributes
	this.currentY = []; // current labels
	this.totaln = 2; // total number of designs shown
	this.p = p; // number of design variables
	this.attp = attp; // number of attributes, including styling
	this.iter = 0; // iteration count
	this.survey = {"age":"-1", "gender":"-1", "marital":"-1", "spouse":"-1", "family":"-1", "children":"-1", "education":"-1", "income":"-1", "earners":"-1", "classify":"-1", "occupation":"-1", "location":"-1"}; // survey string if needed, add all labels, set to -1
	this.styleChosen = true; // switch between first and second action
	this.priceLevel = priceLevel;
//	this.basePrice = basePrice;
//	this.priceRange = priceRange;
	this.priceSet = priceSet;
	this.MPGLevel = MPGLevel;
//	this.baseMPG = baseMPG;
//	this.MPGRange = MPGRange;
	this.MPGSet = MPGSet;
	this.time = new Date(); // time when the test is started
}

test.prototype.initiate = function(){
	$("div#status").append("<p>There are "+((this.ntest+this.nvalidate)*2)+" comparisons remaining.</p>");
	var randomInt;
	for(var i = 0; i<this.n; i++){
    	this.currentX[i] = [];
    	this.currentA[i] = [];
    	this.currentY.push(99);

    	// Namwoo: initial design not too close
    	this.currentX[0] = [0.8, 0.4, 0.3, 0.3, 0.5, 0.9, 0.8, 1.0, 0.6, 0.2, 0.8, 0.0, 0.2, 0.6, 0.6, 0.5, 0.0, 0.5, 0.1];
    	this.currentX[1] = [1.0, 0.6, 0.9, 0.0, 1.0, 0.5, 0.0, 1.0, 0.5, 0.9, 0.1, 0.7, 0.5, 0.0, 0.5, 1.0, 0.6, 1.0, 0.7];
    	
        this.currentA[i].push(0);
    }
    this.X = this.currentX.slice(0);
    this.A = this.currentA.slice(0);
    this.show();
    wspinner.hide();
}

test.prototype.show = function(){
	var x1 = this.currentX[0];
	var x2 = this.currentX[1];
	if(this.iter==0){
		testScene1 = new webgl_scene('displayframe1',$('#displayframe1').width(),$('#displayframe1').height(),0,0,x1);
		testScene2 = new webgl_scene('displayframe2',$('#displayframe2').width(),$('#displayframe2').height(),0,0,x2);
	}
	else{
		testScene1.update(x1);
		testScene1.renderer.clear();
		testScene1.renderer.render(testScene1.scene,testScene1.camera);
		testScene2.update(x2);
		testScene2.renderer.clear();
		testScene2.renderer.render(testScene2.scene,testScene2.camera);
	}
	$("div#blanket").fadeOut(200);
	wspinner.hide();
};

test.prototype.styleChosenAction = function(cb){
	if (this.currentY<0){
		this.X[this.totaln-2] = this.currentX[1];
		this.X[this.totaln-1] = this.currentX[0];
	}
    this.Y = this.Y.concat(this.currentY);
    this.currentX = [];
    var t = this;
    
	if (!cb){ // normal order: first style then attributes
	    $("div#status p").html("<p>There are "+((t.ntest+t.nvalidate-t.iter)*2-1)+" comparisons remaining.</p>");

		// (1) Train style function and update all styling scores for iteration 1 to k-1
		// (2) Train utility function: U = w*[S(X),A] using data from iteration 1 to k-1
		// (3) Generate the pair of attribute: A1, A2 
			
	    // (1) train style function

	    var traindata = {"dim":this.p, "num":this.totaln, "dat":this.X, "lab":this.Y};
	    var traindataString = JSON.stringify(traindata);
	    data = "data="+traindataString+"&action=train&counterbalance="+cb+"&crowdinfo="+crowdinfo;
	    var request = getRequestObject();
	    request.onreadystatechange = 
	      function() {
		    if ((request.readyState == 4) && (request.status == 200)) {
//		    	$("#status a").text("Searching for new designs... "+S_.needn+" remains.");
			    styleModel = request.responseText;
			    stylingTraindata = {"dim":t.p, "num":t.totaln, "svmPar":styleModel, 
				  	  "numIter":t.iter, "dimone":t.n, "obj":0};// set the reference preference value to 0
			    
			    // (1) update styling scores
				var data = eval("(" + styleModel + ")").S;
				for (var i=0;i<t.totaln/2-1;i++){
					if (t.purchaseY[i]*t.Y[i]>0){
						t.A[2*i][0] = data[2*i];
						t.A[2*i+1][0] = data[2*i+1];
					}
					else{
						t.A[2*i][0] = data[2*i+1];
						t.A[2*i+1][0] = data[2*i];
					}
				}
				if (t.Y[i]>0){
					t.A[t.totaln-2][0] = data[t.totaln-2];
					t.A[t.totaln-1][0] = data[t.totaln-1];
				}
				else{
					t.A[t.totaln-2][0] = data[t.totaln-1];
					t.A[t.totaln-1][0] = data[t.totaln-2];
				}
				
				
				if (t.iter>1){
					// (2) train utility function using data from previous iterations
				    var traindata = {"dim":t.attp, "num":t.totaln-t.n, "dat":t.A.slice(0,t.totaln-t.n), "lab":t.purchaseY};
				    var traindataString = JSON.stringify(traindata);
				    data = "data="+traindataString+"&action=trainAtt";
				    var request2 = getRequestObject();
				    request2.onreadystatechange = 
				    	function() {
						    if ((request2.readyState == 4) && (request2.status == 200)) {
	//						    	$("#status a").text("Searching for attributes... "+S_.needn+" remains.");
						    	t.currentY = [];
							    utilityModel = request2.responseText;
							    attributeTraindata = {"dim":t.attp, "num":t.totaln, "svmPar":utilityModel, 
								  	  "numIter":t.iter, "dimone":t.n, "obj":0, "level1":t.priceLevel, "level2":t.MPGLevel};
							    		// set the reference preference value to 0
							    attributeTraindata.A = t.A;
							    attributeTraindataString = "data="+JSON.stringify(attributeTraindata)+"&action=getAtt&counterbalance="+cb+"&crowdinfo="+crowdinfo;
							    
							    // (3) Generate the pair of attribute: A1, A2 
							    createAttribute("main", attributeTraindataString, t.needn, t, cb);
						    }
						    else if ((request2.readyState == 4) && (request2.status == 500)){ // if fail to read, resend data
						    	request.open("POST", "main", true);
						        request.setRequestHeader("Content-Type", 
						                               "application/x-www-form-urlencoded");
						        request.send(data);
						    }
				      	};
				    request2.open("POST", "main", true);
				    request2.setRequestHeader("Content-Type", 
				                           "application/x-www-form-urlencoded");
				    request2.send(data);
				}
				else{
					// if first round, show randomly generated attributes
					if (t.currentY>0){// if first style better
						t.A[0].push(0,0,0,1);
						t.A[0].push(0,0,0,0);
						t.A[1].push(0,0,0,0);
						t.A[1].push(0,0,0,1);
					}
					else{
						t.A[1].push(0,0,0,1);
						t.A[1].push(0,0,0,0);
						t.A[0].push(0,0,0,0);
						t.A[0].push(0,0,0,1);						
					}
					$('#question').html(question2);
					$("div#attribute2frame1").html("<a>"+calAttribute(t.A[0],t.MPGSet,t.MPGLevel,'MPG')+"</a><a style='font-size:100%'>MPG<br>(City/Hwy)");
					$("div#attribute1frame1").html("<a>"+calAttribute(t.A[0],t.priceSet,t.priceLevel,'price')+"</a><a style='font-size:100%'>,000 USD (MSRP)");
					$("div#attribute2frame2").html("<a>"+calAttribute(t.A[1],t.MPGSet,t.MPGLevel,'MPG')+"</a><a style='font-size:100%'>MPG<br>(City/Hwy)");
					$("div#attribute1frame2").html("<a>"+calAttribute(t.A[1],t.priceSet,t.priceLevel,'price')+"</a><a style='font-size:100%'>,000 USD (MSRP)");
					$("div#blanket").fadeOut(200);
					wspinner.hide();
					$('#leftmost').addClass("active");
					$('#rightmost').addClass("active");
					$('#left').addClass("active");
					$('#right').addClass("active");
				}
		    }
	    };
	}
	else{ // counterbalance step: first attributes and style, then style only
		// when style is chosen, show next round of style
	    
	    // (1) train style function
	    var traindata = {"dim":this.p, "num":this.totaln, "dat":this.X, "lab":this.Y};
	    var traindataString = JSON.stringify(traindata);
	    data = "data="+traindataString+"&action=train&counterbalance="+cb+"&crowdinfo="+crowdinfo;
	    var request = getRequestObject();
	    request.onreadystatechange = 
	    	function() {
			    if ((request.readyState == 4) && (request.status == 200)) {
	//		    	$("#status a").text("Searching for new designs... "+S_.needn+" remains.");
				    styleModel = request.responseText;
				    stylingTraindata = {"dim":t.p, "num":t.totaln, "svmPar":styleModel, 
					  	  "numIter":t.iter, "dimone":t.n, "obj":0};// set the reference preference value to 0
				    
				    if (t.iter<t.ntest-1){
				    	// (1) Generate the next pair of styles
					    stylingTraindataString = "data="+JSON.stringify(stylingTraindata)+"&action=getTest&counterbalance="+cb+"&crowdinfo="+crowdinfo;
					    createStyle("main", stylingTraindataString, t.needn, t, cb);
				    }
				    else{
				    	// (3) if max iteration, retrain the utility model
						var data = eval("(" + styleModel + ")").S;
						for (var i=0;i<t.totaln/2;i++){
							if (t.purchaseY[i]*t.Y[i]>0){
								t.A[2*i][0] = data[2*i];
								t.A[2*i+1][0] = data[2*i+1];
							}
							else{
								t.A[2*i][0] = data[2*i+1];
								t.A[2*i+1][0] = data[2*i];
							}
						}
					    var traindata = {"dim":t.attp, "num":t.totaln, "dat":t.A, "lab":t.purchaseY};
					    var traindataString = JSON.stringify(traindata);
					    data = "data="+traindataString+"&action=trainAtt";
					    var request2 = getRequestObject();
					    request2.onreadystatechange = 
					    	function() {
							    if ((request2.readyState == 4) && (request2.status == 200)) {
		//						    	$("#status a").text("Searching for attributes... "+S_.needn+" remains.");
								    utilityModel = request2.responseText;
								    t.getValidate();
							    }
							    else if ((request2.readyState == 4) && (request2.status == 500)){ // if fail to read, resend data
							    	request.open("POST", "main", true);
							        request.setRequestHeader("Content-Type", 
							                               "application/x-www-form-urlencoded");
							        request.send(data);
							    }
					      	};
					    request2.open("POST", "main", true);
					    request2.setRequestHeader("Content-Type", 
					                           "application/x-www-form-urlencoded");
					    request2.send(data);
				    }
			    }
	    	};
	}
    request.open("POST", "main", true);
    request.setRequestHeader("Content-Type", 
                           "application/x-www-form-urlencoded");
    request.send(data);
}

test.prototype.attributeChosenAction = function(cb){
	if (this.currentY<0){
		this.A[this.totaln-2] = this.currentA[1];
		this.A[this.totaln-1] = this.currentA[0];
	}
	this.purchaseY = this.purchaseY.concat(this.currentY);
	this.currentA = [];
	var t = this;
	
	if (!cb){ // normal order: first style then attributes
		var traindata = {"dim":this.attp, "num":this.totaln, "dat":this.A, "lab":this.purchaseY};
	    var traindataString = JSON.stringify(traindata);
	    data = "data="+traindataString+"&action=trainAtt";
	    var request = getRequestObject();
	    request.onreadystatechange = 
	    function() {
		    if ((request.readyState == 4) && (request.status == 200)) {
//			    	$("#status a").text("Searching for attributes... "+S_.needn+" remains.");
			    utilityModel = request.responseText;
			    if (t.iter<t.ntest-1){
			    	// (1) Generate the next pair of styles
				    stylingTraindataString = "data="+JSON.stringify(stylingTraindata)+"&action=getTest&counterbalance="+cb+"&crowdinfo="+crowdinfo;
				    createStyle("main", stylingTraindataString, t.needn, t, cb);
				}
				else { // if max iteration go to validation
					t.getValidate();
				}
		    }
	    }
	}
	else{ // counterbalance step: first attributes and style, then style only
	    $("div#status p").html("<p>There are "+((t.ntest+t.nvalidate-t.iter)*2-1)+" comparisons remaining.</p>");

		var traindata = {"dim":this.attp, "num":this.totaln, "dat":this.A, "lab":this.purchaseY};
	    var traindataString = JSON.stringify(traindata);
	    data = "data="+traindataString+"&action=trainAtt";
	    var request = getRequestObject();
	    request.onreadystatechange = 
	    function() {
		    if ((request.readyState == 4) && (request.status == 200)) {
//			    	$("#status a").text("Searching for attributes... "+S_.needn+" remains.");
			    utilityModel = request.responseText;
			    $('#question').html(question1);
				$("div#attribute1frame1").text("");
		        $("div#attribute2frame1").text("");
			    $("div#attribute1frame2").text("");
				$("div#attribute2frame2").text("");
				$("div#blanket").fadeOut(200);
				wspinner.hide();
				$('#leftmost').addClass("active");
				$('#rightmost').addClass("active");
				$('#left').addClass("active");
				$('#right').addClass("active");
		    }
	    }
	}
    request.open("POST", "main", true);
    request.setRequestHeader("Content-Type", 
                           "application/x-www-form-urlencoded");
    request.send(data);	
}

function createStyle(address, data, nsample, t, cb){
  var request = getRequestObject();
  var style = [];
  request.onreadystatechange = 
    function(){
	  if ((request.readyState == 4) && (request.status == 200)) {
		  if (nsample>1) {
			  nsample-=1;
//			  if (nsample>1){$("#status a").text("generating new designs... "+nsample+" remains");}
//			  else {$("#status a").text("some rendering going on...Please wait");}
			  data = request.responseText+"&action=getTest&counterbalance="+cb+"&crowdinfo="+crowdinfo;
			  createStyle(address, data, nsample, t, cb);
		  }
		  else {
			  t.iter = t.iter + 1;
			  $("div#status p").html("<p>There are "+((t.ntest+t.nvalidate-t.iter)*2)+" comparisons remaining.</p>");
			  var rawData = request.responseText;
			  rawData = rawData.slice(5); //cut off the "data=" part
			  style = eval("("+rawData+")").style;
			  var data = eval("(" + rawData + ")").svmPar;
			  t.X = data.X;
			  t.totaln = t.X.length;
			  t.currentX = [t.X[t.totaln-2],t.X[t.totaln-1]];
			  var temp;
			  for (var i=0;i<t.n;i++){
				  temp = [style[i]];
				  for (var j=1;j<t.attp;j++){
					  temp.push(0);
				  }
				  t.A.push(temp);
			  }
			  t.show();
			  $('#question').html(question1);
			  $("div#attribute1frame1").text("");
			  $("div#attribute2frame1").text("");
			  $("div#attribute1frame2").text("");
			  $("div#attribute2frame2").text("");
			  if (!cb){ // the next iteration will be counterbalance
				    // (2) Generate attributes
				  	t.currentY = [];
				    attributeTraindata = {"dim":t.attp, "num":t.totaln, "svmPar":utilityModel, 
						  	  "numIter":t.iter, "dimone":t.n, "obj":0, "level1":t.priceLevel, "level2":t.MPGLevel};
				    attributeTraindata.A = t.A;
				    attributeTraindataString = "data="+JSON.stringify(attributeTraindata)+"&action=getAtt&counterbalance="+cb+"&crowdinfo="+crowdinfo;
				    
				    // (3) Generate the pair of attribute: A1, A2 
				    createAttribute("main", attributeTraindataString, t.needn, t, cb);
			  }
			  else{
				  $("div#blanket").fadeOut(200);
				  wspinner.hide();
				  $('#leftmost').addClass("active");
				  $('#rightmost').addClass("active");
				  $('#left').addClass("active");
				  $('#right').addClass("active");
			  }
		  }
	  }
  	};
  request.open("POST", address, true);
  request.setRequestHeader("Content-Type", 
                           "application/x-www-form-urlencoded");
  request.send(data);
}

function createAttribute(address, data, nsample, t, cb){
	var keep_data = data;
    var request = getRequestObject();
    request.onreadystatechange = 
    function(){
	  if ((request.readyState == 4) && (request.status == 200)) {
		  if (nsample>1) {
			  nsample-=1;
//				  if (nsample>1){$("#status a").text("generating new attributes... "+nsample+" remains");}
//				  else {$("#status a").text("some rendering going on...Please wait");}
			  d = request.responseText+"&action=getAtt&counterbalance="+cb+"&crowdinfo="+crowdinfo;
			  createAttribute(address, d, nsample, t, cb);
		  }
		  else {
			  var rawData = request.responseText;
			  rawData = rawData.slice(5); //cut off the "data=" part
			  var data = eval("(" + rawData + ")");
			  t.A = data.A;
			  partworth = data.partworth;
			  t.currentA = [data.A[t.totaln-2],data.A[t.totaln-1]];
			  $('#question').html(question2);
			  if (t.currentY.length == 0 || t.currentY>0){
				  $("div#attribute2frame1").html("<a>"+calAttribute(t.currentA[0],t.MPGSet,t.MPGLevel,"MPG")+"</a><a style='font-size:100%'>MPG<br>(City/Hwy)");
				  $("div#attribute1frame1").html("<a>"+calAttribute(t.currentA[0],t.priceSet,t.priceLevel,"price")+"</a><a style='font-size:100%'>,000 USD (MSRP)");
				  $("div#attribute2frame2").html("<a>"+calAttribute(t.currentA[1],t.MPGSet,t.MPGLevel,"MPG")+"</a><a style='font-size:100%'>MPG<br>(City/Hwy)");
				  $("div#attribute1frame2").html("<a>"+calAttribute(t.currentA[1],t.priceSet,t.priceLevel,"price")+"</a><a style='font-size:100%'>,000 USD (MSRP)");				  
			  }
			  else{
				  $("div#attribute2frame2").html("<a>"+calAttribute(t.currentA[0],t.MPGSet,t.MPGLevel,"MPG")+"</a><a style='font-size:100%'>MPG<br>(City/Hwy)");
				  $("div#attribute1frame2").html("<a>"+calAttribute(t.currentA[0],t.priceSet,t.priceLevel,"price")+"</a><a style='font-size:100%'>,000 USD (MSRP)");
				  $("div#attribute2frame1").html("<a>"+calAttribute(t.currentA[1],t.MPGSet,t.MPGLevel,"MPG")+"</a><a style='font-size:100%'>MPG<br>(City/Hwy)");
				  $("div#attribute1frame1").html("<a>"+calAttribute(t.currentA[1],t.priceSet,t.priceLevel,"price")+"</a><a style='font-size:100%'>,000 USD (MSRP)");
			  }
			  $("div#blanket").fadeOut(200);
			  wspinner.hide();
			  $('#leftmost').addClass("active");
			  $('#rightmost').addClass("active");
			  $('#left').addClass("active");
			  $('#right').addClass("active");
		  }
	  }
	  else if ((request.readyState == 4) && (request.status == 500)) {
		  createAttribute(address, keep_data, 2, t, cb);	
	  }
    };
    request.open("POST", address, true);
    request.setRequestHeader("Content-Type", 
                           "application/x-www-form-urlencoded");
    request.send(data);		
}

test.prototype.getValidate = function(){
//	var data = "data="+JSON.stringify({"nvalidate":this.nvalidate,"p":this.p, "attp":this.attp, "level1":this.priceLevel, "level2":this.MPGLevel})+"&action=validate";
	var data = "n="+(nvalidate*2)+"&action=validate";
	var request = getRequestObject();
	var t = this;
	request.onreadystatechange = function(){
		if ((request.readyState == 4) && (request.status == 200)) {
			t.iter += 1;
			validateSet = request.responseText;
			var data = eval("(" + validateSet + ")");
			t.validateX = data.X;
			t.validateA = data.A;
			t.validateY = [];
			t.styleChosen=true;
			t.showValidate();
		}
	};
	request.open("POST", "main", true);
	request.setRequestHeader("Content-Type","application/x-www-form-urlencoded");
	request.send(data);
};

test.prototype.showValidate = function(){
	if (this.styleChosen){
		$("div#status p").html("<p>There are "+((this.ntest+this.nvalidate-this.iter)*2)+" comparisons remaining.</p>");
		$('#question').html(question1);
		this.currentX[0] = this.validateX[2*(this.iter-this.ntest)];
		this.currentX[1] = this.validateX[2*(this.iter-this.ntest)+1];
		this.currentA[0] = this.validateA[2*(this.iter-this.ntest)];
		this.currentA[1] = this.validateA[2*(this.iter-this.ntest)+1];
		$("div#attribute1frame1").text("");
		$("div#attribute2frame1").text("");
		$("div#attribute1frame2").text("");
		$("div#attribute2frame2").text("");
		this.show();
		this.styleChosen = false;
		$('#leftmost').show();
		$('#rightmost').show();	
		$('#left').html('Better');
		$('#right').html('Better');
	}
	else{
		$('#question').html(question2);
		$("div#status p").html("<p>There are "+((this.ntest+this.nvalidate-this.iter)*2-1)+" comparisons remaining.</p>");
		$("div#attribute2frame1").html("<a>"+this.MPGSet[Math.round(this.currentA[0][1]*(this.MPGLevel-1))]+"</a><a style='font-size:100%'>MPG<br>(City/Hwy)");
		$("div#attribute1frame1").html("<a>"+this.priceSet[Math.round(this.currentA[0][0]*(this.priceLevel-1))]+"</a><a style='font-size:100%'>,000 USD (MSRP)");
		$("div#attribute2frame2").html("<a>"+this.MPGSet[Math.round(this.currentA[1][1]*(this.MPGLevel-1))]+"</a><a style='font-size:100%'>MPG<br>(City/Hwy)");
		$("div#attribute1frame2").html("<a>"+this.priceSet[Math.round(this.currentA[1][0]*(this.priceLevel-1))]+"</a><a style='font-size:100%'>,000 USD (MSRP)");
		this.styleChosen = true;
		this.iter = this.iter + 1;
		$('#leftmost').hide();
		$('#rightmost').hide();
		$('#left').html('Buy this');
		$('#right').html('Buy this');
	}
	$("div#blanket").fadeOut(200);
	wspinner.hide();
	$('#leftmost').addClass("active");
	$('#rightmost').addClass("active");
	$('#left').addClass("active");
	$('#right').addClass("active");
};

test.prototype.store = function(){
	var data = {"styleModel":styleModel, "utilityModel":utilityModel, "validateSet":validateSet, "validateY":this.validateY, "survey":this.survey};
	data = JSON.stringify(data);
	data = "data="+data+"&action=store";
	var request = getRequestObject();
	request.onreadystatechange = function(){
		getMTurkCodePost();
	};
	request.open("POST", "main", true);
	request.setRequestHeader("Content-Type","application/x-www-form-urlencoded");
	request.send(data);
};


function getRequestObject() {
	  if (window.XMLHttpRequest) {
	    return(new XMLHttpRequest());
	  } else if (window.ActiveXObject) { 
	    return(new ActiveXObject("Microsoft.XMLHTTP"));
	  } else {
	    return(null); 
	  }
}

function getMTurkCodePost(){
	  var address = "main";
	  var data = "&action=mturkGetCode";
	  var request = getRequestObject();
	  request.onreadystatechange = function(){getMTurkCode(request)};
	  request.open("POST", address, true);
	  request.setRequestHeader("Content-Type","application/x-www-form-urlencoded");
	  request.send(data);
}

function getMTurkCode(request){
	if ((request.readyState == 4) &&
		    (request.status == 200)) {
			var rawData = request.responseText;
//			alert("The MTurk code is: "+rawData);
			$("#mturk").text("Your responses are now submitted. Your code is: "+rawData); 
			$("#mturk").removeClass("active");
	}
}

function calAttribute(x,set,level,attribute){
	if (attribute=="MPG"){
		id = priceLevel;
	}
	else if(attribute=="price"){
		id = 1;
	}
	s = set[0];
	for (var i=0;i<level-1;i++){
		if (x[id+i]==1){
			s = set[i+1];
			break;
		}
	}
	return s;
}