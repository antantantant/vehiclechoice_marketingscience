// Namwoo and Max project
// only for pairwise comparison

p = 19; //number of variables
attp = 2; //number of purchase attributes
priceLevel = 11; // level of prices
basePrice = 20000; // minimum price
priceRange = 50000; // range of price, maximumPrice = basePrice + priceRange

// test object
function test(){
	this.ntest = 15; // number of iterations
	this.X = []; // accumulated design style parameters
	this.A = []; // accumulated design attributes
	this.Y = []; // 2:left much better than right, 1:left better than right, 0:neutral, 
	//	-1:right better than left, -2:right much better than left
	this.purchaseY = []; // choice combines style and other attributes
	this.P = []; // accumulated prices
	this.n = 2; // total number of designs shown in one iteration
	this.needn = 2; // number of designs needed in this iteration
	this.ind = []; // index of all designs
	this.currentX = []; // current designs
	this.currentA = []; // current attributes
	this.currentY = []; // current labels
	this.totaln = 0; // total number of designs shown
	this.p = p; // number of design variables
	this.iter = 1; // iteration count
	this.survey = []; // survey string if needed
	this.showdesign = true; // first show designs
	this.beta = 0; // parameter for the utility model
	this.time = new Date(); // time when the test is started
}

test.prototype.initiate = function(){
	$("div#question").append("<p>Drag mouse to rotate the designs.<br>" +
			"** Please choose HARD TO SAY when a choice is hard to make. This will be much better than a random choice. **<br><br>" +
			"There are "+this.ntest+" comparisons remaining.</p>");
	for(var i = 0; i<this.n; i++){
    	this.currentX[i] = [];
    	this.ind.push(i);
    	this.currentY.push(99);
        for(var j = 0; j<this.p; j++){
            this.currentX[i].push(Math.random());
        }
    }
    this.X = this.currentX;
    this.show();
    wspinner.hide();
}

test.prototype.show = function(){
	var x1 = this.currentX[0];
	var x2 = this.currentX[1];
	if(this.iter==1){
		testScene1 = new webgl_scene('displayframe1',$('#displayframe1').width(),$('#displayframe1').height(),0,0,x1);
		testScene1.renderer.render(testScene1.scene,testScene1.camera);
		testScene2 = new webgl_scene('displayframe2',$('#displayframe2').width(),$('#displayframe2').height(),0,0,x2);
		testScene2.renderer.render(testScene2.scene,testScene2.camera);
	}
	else{
		testScene1.update(x1);
		testScene1.renderer.clear();
		testScene1.renderer.render(testScene1.scene,testScene1.camera);
		testScene2.update(x2);
		testScene2.renderer.clear();
		testScene2.renderer.render(testScene2.scene,testScene2.camera);
	}
};


test.prototype.nextStyle = function(){
	this.totaln = this.X.length;
	if (this.currentY<0){
		this.X[this.totaln-2] = this.currentX[1];
		this.X[this.totaln-1] = this.currentX[0];
	}
    this.Y = this.Y.concat(Math.abs(this.currentY));
	this.price = [0,0];
	$("#status a").text("Learning feedback data...");
    this.currentY = [];
    this.currentX = [];
    // index the next round
    for (var i = 0; i<this.n; i++){
    	this.ind[i]=i+this.totaln;
    }
    // train and explore
    var S_ = this;
    var address = "main";
    var traindata = {"dim":this.p, "num":this.totaln, "dat":this.X, "lab":this.Y};
    var traindataString = JSON.stringify(traindata);
    data = "data="+traindataString+"&action=train";
    var request = getRequestObject();
    request.onreadystatechange = 
      function() {
	    if ((request.readyState == 4) && (request.status == 200)) {
	    	$("#status a").text("Searching for new designs... "+S_.needn+" remains.");
		    var svmPar = request.responseText;
		    traindata = {"dim":S_.p, "num":S_.totaln, "svmPar":svmPar, 
			  	  "numIter":S_.iter, "dimone":S_.n, "obj":0};// set the reference preference value to 0
		    traindataString = JSON.stringify(traindata);
		    data = "data="+traindataString+"&action=getTest";
		    address = "main";
		    searchMethod(address, data, S_.needn, S_);
	    }
      };
    request.open("POST", address, true);
    request.setRequestHeader("Content-Type", 
                           "application/x-www-form-urlencoded");
    request.send(data);
}

test.prototype.nextAttribute = function(){
	this.show(); // show next round of designs
	$("div#attframe1").text("");
	$("div#attframe2").text("");
	this.purchaseY.push(this.currentY);
	this.currentY = [];
//	//newton-raphson to find beta
//	var beta_ = 0.0;
//	var count = 0;
//	while(count<1e3){
//		fg = utility_train_gh(this,beta_);
//		if(Math.abs(fg[0])<1e-3){
//			this.beta = beta_; 
//			break;
//		}
//		beta_ = -fg[0]/fg[1]+beta_;
//		count++;
//		if (count>900){
//			fuck = 1;
//		}
//	}
    // train and explore
    var S_ = this;
    var address = "main";
    var traindata = {"dim":this.attp, "num":this.totaln, "attribute":this.A, "lab":this.purchaseY};
    var traindataString = JSON.stringify(traindata);
    data = "data="+traindataString+"&action=trainAtt";
    var request = getRequestObject();
    request.onreadystatechange = 
      function() {
	    if ((request.readyState == 4) && (request.status == 200)) {
	    	$("#status a").text("Searching for attributes... "+S_.needn+" remains.");
		    var svmPar = request.responseText;
		    traindata = {"dim":S_.p, "num":S_.totaln, "svmPar":svmPar, 
			  	  "numIter":S_.iter, "dimone":S_.n, "obj":0};// set the reference preference value to 0
		    traindataString = JSON.stringify(traindata);
		    data = "data="+traindataString+"&action=getAtt";
		    address = "main";
		    searchMethod(address, data, S_.needn, S_);
	    }
      };
    request.open("POST", address, true);
    request.setRequestHeader("Content-Type", 
                           "application/x-www-form-urlencoded");
    request.send(data);	
}

//test.prototype.nextprice = function(){
//	if (this.P.length==0){
//		this.P.push(Math.round(Math.random()*10)/10);
//		this.P.push(Math.round(Math.random()*10)/10);
//	}
//	else{
//		// enumerate price space
//		// utility model: u = s + beta * p
//		var i,j,k,p1,p2,f1,f2,besti,bestj;
//		var s1 = this.S[this.totaln-2];
//		var s2 = this.S[this.totaln-1];
//		var count = 0;
//		var gap = 1.0/(priceLevel-1);
//		var fit = null;
//		var bestfit = -1;
//		for (i=0;i<priceLevel;i++){
//			for (j=0;j<priceLevel;j++){
//				p1 = i*gap; // relative price (0,1)
//				p2 = j*gap;
//				f1 = Math.exp(-Math.abs((s1-s2)/this.beta+p1-p2));
//				f2 = 0;
//				for (k=0;k<this.totaln-2;k++){
//					f2+= Math.exp(-((this.S[k]-s1)*(this.S[k]-s1)+(this.P[k]-p1)*(this.P[k]-p1)));
//					f2+= Math.exp(-((this.S[k]-s2)*(this.S[k]-s2)+(this.P[k]-p2)*(this.P[k]-p2)));
//				}
//				f2 = f2/2/(this.totaln-2);
//				fit = f1 + f2;
//				if (fit>bestfit){
//					bestfit = fit;
//					besti = p1;
//					bestj = p2;
//				}
//				count++;
//			}
//		}
//		this.P.push(besti);
//		this.P.push(bestj);
//	}
//	$("div#priceframe1").text("price: "+(this.P[this.totaln-2]*priceRange+basePrice));
//	$("div#priceframe2").text("price: "+(this.P[this.totaln-1]*priceRange+basePrice));
//}

function searchMethod(address, data, nsample, s){
  var request = getRequestObject();
  request.onreadystatechange = 
    function(){
	  if ((request.readyState == 4) && (request.status == 200)) {
		  if (nsample>1) {
			  nsample-=1;
			  if (nsample>1){$("#status a").text("generating new designs... "+nsample+" remains");}
			  else {$("#status a").text("some rendering going on...Please wait");}
			  data = request.responseText+"&action=getTest";
			  searchMethod(address, data, nsample, s);
		  }
		  else {
			  s.iter = s.iter + 1;
			  showResponse(request, s);
		  }
	  }
  	};
  request.open("POST", address, true);
  request.setRequestHeader("Content-Type", 
                           "application/x-www-form-urlencoded");
  request.send(data);
}

function showResponse(request, test) {
	  if ((request.readyState == 4) &&
	      (request.status == 200)) {
		  var rawData = request.responseText;
		  rawData = rawData.slice(5); //cut off the "data=" part
		  var data = eval("(" + rawData + ")").svmPar;
		  test.X = data.X;
		  test.currentX = [data.X[test.totaln],data.X[test.totaln+1]];
		  test.S = data.S;
		  test.nextprice();
	  }
}

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
	  var address = "store";
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
			alert("The MTurk code is: "+rawData);
	}
}

test.prototype.store = function(){
    this.totaln = this.X.length;
	var address = "main";
	if(!this.modelName){this.modelName = 'model_'+Math.round(Math.random()*1e6);}
	var data = {"G":this.G, "allX":this.allX, "allY":this.allY, "allInd":this.allInd,
			"modelName":this.modelName, "survey":this.survey};
	data = JSON.stringify(data);
	data = "data="+data+"&action=store";
	storePost(address,data,this);
};

function storePost(address, data, search) {
	  var request = getRequestObject();
	  request.onreadystatechange = function(){};
	  request.open("POST", address, true);
	  request.setRequestHeader("Content-Type","application/x-www-form-urlencoded");
	  request.send(data);
}

function utility_train_gh(test,beta){
	var g = 0, h = 0;
	var i,s1,s2,p1,p2;
	for (i=0;i<test.priceY.length;i++){
		s1 = test.S[2*i];
		s2 = test.S[2*i+1];
		p1 = test.P[2*i];
		p2 = test.P[2*i+1];
		if(test.priceY[i]==2){
			h+= Math.exp(-s1+s2-(p1-p2)*beta+3)*(-p1+p2)*(-p1+p2);
			g+= Math.exp(-s1+s2-(p1-p2)*beta+3)*(-p1+p2);
		}
		else if(test.priceY[i]==1){
			h+= Math.exp(-s1+s2-(p1-p2)*beta+2)*(-p1+p2)*(-p1+p2);
			g+= Math.exp(-s1+s2-(p1-p2)*beta+2)*(-p1+p2);
		}
		else if(test.priceY[i]==-2){
			h+= Math.exp(s1-s2+(p1-p2)*beta+3)*(p1-p2)*(p1-p2);
			g+= Math.exp(s1-s2+(p1-p2)*beta+3)*(p1-p2);
		}
		else if(test.priceY[i]==-1){
			h+= Math.exp(s1-s2+(p1-p2)*beta+2)*(p1-p2)*(p1-p2);
			g+= Math.exp(s1-s2+(p1-p2)*beta+2)*(p1-p2);
		}
		else if(test.priceY[i]==0){
			h+= Math.exp(s1-s2+(p1-p2)*beta-1)*(p1-p2)*(p1-p2)+Math.exp(-s1+s2-(p1-p2)*beta-1)*(-p1+p2)*(-p1+p2);
			g+= Math.exp(s1-s2+(p1-p2)*beta-1)*(p1-p2)+Math.exp(-s1+s2-(p1-p2)*beta-1)*(-p1+p2);
		}
	}
	return [g,h];
}