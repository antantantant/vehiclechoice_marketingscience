// Namwoo and Max project
// only for pairwise comparison
// use DOE

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

crowdinfo = 1; // 0: active learning on individual only, 1: use crowd info
indifferentChoice = 0; // switch on/off indifferent choice option for users
purchaseChoice = 0; // 0: pairwise choice only, 1: rating

question1 = "<a style='color: #BA9488;'> Styling question:</a> Which of the following styles do you prefer more?";
question2 = "<a style='color: #88AEBA;'> Purchase question:</a> Which car will you be more likely to buy?";

counterbalance = false; // start with normal query, then switch to counterbalance every other iteration

// test object
function test(){
	this.ntest = ntest;
	this.nvalidate = nvalidate;
	this.X = []; // accumulated design style parameters, [x1, x2]: first design, second design (NOT better, worse)
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
	var data = "n="+(ntest*2)+"&action=getDOE";
	var request = getRequestObject();
	var t = this;
	request.onreadystatechange = function(){
		if ((request.readyState == 4) && (request.status == 200)) {
			var data = eval("(" + request.responseText + ")");
			t.X = data.X;
			t.A = convertA(data.A);
			t.user_id = data.user_id;
			t.currentX[0]=t.X[0].slice(0);
			t.currentX[1]=t.X[1].slice(0);
			t.currentA[0]=t.A[0].slice(0);
			t.currentA[1]=t.A[1].slice(0);	
		    t.show();
//			$('#question').html(question2);
//			$("div#attribute2frame1").html("<a>"+(t.A[t.iter*2][2]*t.MPGRange+t.baseMPG)+"</a><a style='font-size:100%'>MPG (City)");
//			$("div#attribute1frame1").html("<a>"+(t.A[t.iter*2][1]*t.priceRange+t.basePrice)+"</a><a style='font-size:100%'>USD (MSRP)");
//			$("div#attribute2frame2").html("<a>"+(t.A[t.iter*2+1][2]*t.MPGRange+t.baseMPG)+"</a><a style='font-size:100%'>MPG (City)");
//			$("div#attribute1frame2").html("<a>"+(t.A[t.iter*2+1][1]*t.priceRange+t.basePrice)+"</a><a style='font-size:100%'>USD (MSRP)");
//			$('#leftmost').hide();
//			$('#rightmost').hide();	
		}
	};
	request.open("POST", "main", true);
	request.setRequestHeader("Content-Type","application/x-www-form-urlencoded");
	request.send(data);
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
		testScene1.carModel.rotation.z = 0;
		testScene1.renderer.render(testScene1.scene,testScene1.camera);
		testScene2.update(x2);
		testScene2.renderer.clear();
		testScene2.carModel.rotation.z = 0;
		testScene2.renderer.render(testScene2.scene,testScene2.camera);
	}
	$("div#blanket").fadeOut(500);
	wspinner.hide();
};

test.prototype.styleChosenAction = function(cb){
	wspinner.show();
    this.Y = this.Y.concat(this.currentY);
    var t = this;
    
	if (!cb){ // normal order: first style then attributes
	    $("div#status p").html("<p>There are "+((t.ntest+t.nvalidate-t.iter)*2-1)+" comparisons remaining.</p>");
		$('#question').html(question2);
		$("div#attribute2frame1").html("<a>"+this.MPGSet[Math.round(this.A[t.iter*2][2]*(this.MPGLevel-1))]+"</a><a style='font-size:100%'>MPG<br>(City/Hwy)");
		$("div#attribute1frame1").html("<a>"+this.priceSet[Math.round(this.A[t.iter*2][1]*(this.priceLevel-1))]+"</a><a style='font-size:100%'>,000 USD (MSRP)");
		$("div#attribute2frame2").html("<a>"+this.MPGSet[Math.round(this.A[t.iter*2+1][2]*(this.MPGLevel-1))]+"</a><a style='font-size:100%'>MPG<br>(City/Hwy)");
		$("div#attribute1frame2").html("<a>"+this.priceSet[Math.round(this.A[t.iter*2+1][1]*(this.priceLevel-1))]+"</a><a style='font-size:100%'>,000 USD (MSRP)");
		$("div#blanket").fadeOut(200);
		wspinner.hide();
		$('#leftmost').addClass("active");
		$('#rightmost').addClass("active");
		$('#left').addClass("active");
		$('#right').addClass("active");
	}
	else{ // counterbalance step: first attributes and style, then style only
		// when style is chosen, show next round of style
		if (t.iter<t.ntest-1){
			t.iter = t.iter + 1;
			t.currentX[0] = t.X[2*t.iter].slice(0);
			t.currentX[1] = t.X[2*t.iter+1].slice(0);
			$("div#status p").html("<p>There are "+((t.ntest+t.nvalidate-t.iter)*2)+" comparisons remaining.</p>");
			$("div#attribute2frame1").html("");
			$("div#attribute1frame1").html("");
			$("div#attribute2frame2").html("");
			$("div#attribute1frame2").html("");
		    this.show();
			$('#leftmost').addClass("active");
			$('#rightmost').addClass("active");
			$('#left').addClass("active");
			$('#right').addClass("active");
		}
		else{ // if max iteration go to validation
			t.getValidate();
		}
	}
}

test.prototype.attributeChosenAction = function(cb){
	wspinner.show();
	this.purchaseY = this.purchaseY.concat(this.currentY);
	var t = this;
	
	if (!cb){ // normal order: first style then attributes
	    if (t.iter<t.ntest-1){
			t.iter = t.iter + 1;
			t.currentX[0] = t.X[2*t.iter].slice(0);
			t.currentX[1] = t.X[2*t.iter+1].slice(0);
		    this.show();
			$("div#status p").html("<p>There are "+((t.ntest+t.nvalidate-t.iter)*2)+" comparisons remaining.</p>");
			$('#question').html(question2);
			$("div#attribute2frame1").html("<a>"+this.MPGSet[Math.round(this.A[t.iter*2][2]*(this.MPGLevel-1))]+"</a><a style='font-size:100%'>MPG<br>(City/Hwy)");
			$("div#attribute1frame1").html("<a>"+this.priceSet[Math.round(this.A[t.iter*2][1]*(this.priceLevel-1))]+"</a><a style='font-size:100%'>,000 USD (MSRP)");
			$("div#attribute2frame2").html("<a>"+this.MPGSet[Math.round(this.A[t.iter*2+1][2]*(this.MPGLevel-1))]+"</a><a style='font-size:100%'>MPG<br>(City/Hwy)");
			$("div#attribute1frame2").html("<a>"+this.priceSet[Math.round(this.A[t.iter*2+1][1]*(this.priceLevel-1))]+"</a><a style='font-size:100%'>,000 USD (MSRP)");
			$('#leftmost').addClass("active");
			$('#rightmost').addClass("active");
			$('#left').addClass("active");
			$('#right').addClass("active");
	    }
		else { // if max iteration go to validation
			t.getValidate();
		}
	}
	else{ // counterbalance step: first attributes and style, then style only
	    $("div#status p").html("<p>There are "+((t.ntest+t.nvalidate-t.iter)*2-1)+" comparisons remaining.</p>");
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
		$('#left').html('Better');
		$('#right').html('Better');
	}
}

test.prototype.getValidate = function(){
	var data = "n="+(nvalidate*2)+"&action=validateDOE";
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
		$('#leftmost').show();
		$('#rightmost').show();	
		$('#left').html('Better');
		$('#right').html('Better');
		this.show();
		this.styleChosen = false;
	}
	else{
		$('#question').html(question2);
		$("div#status p").html("<p>There are "+((this.ntest+this.nvalidate-this.iter)*2-1)+" comparisons remaining.</p>");
		$("div#attribute2frame1").html("<a>"+this.MPGSet[Math.round(this.currentA[0][1]*(this.MPGLevel-1))]+"</a><a style='font-size:100%'>MPG<br>(City/Hwy)");
		$("div#attribute1frame1").html("<a>"+this.priceSet[Math.round(this.currentA[0][0]*(this.priceLevel-1))]+"</a><a style='font-size:100%'>,000 USD (MSRP)");
		$("div#attribute2frame2").html("<a>"+this.MPGSet[Math.round(this.currentA[1][1]*(this.MPGLevel-1))]+"</a><a style='font-size:100%'>MPG<br>(City/Hwy)");
		$("div#attribute1frame2").html("<a>"+this.priceSet[Math.round(this.currentA[1][0]*(this.priceLevel-1))]+"</a><a style='font-size:100%'>,000 USD (MSRP)");
		$('#leftmost').hide();
		$('#rightmost').hide();
		$('#left').html('Buy this');
		$('#right').html('Buy this');
		this.styleChosen = true;
		this.iter = this.iter + 1;		
	}
	$("div#blanket").fadeOut(200);
	wspinner.hide();
	$('#leftmost').addClass("active");
	$('#rightmost').addClass("active");
	$('#left').addClass("active");
	$('#right').addClass("active");
};

test.prototype.store = function(){
	var data = {"user_id":this.user_id, "X":this.X, "A":this.A, "Y":this.Y, "purchaseY":this.purchaseY, "validateX":this.validateX, "validateA":this.validateA, "validateY":this.validateY, "survey":this.survey};
	data = JSON.stringify(data);
	data = "data="+data+"&action=storeDOE2";
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

function convertA(A){
	var B = [];
	var i,j,b,bb;
	for (i=0;i<A.length;i++){
		b = [0]; //default style is set to 0
		bb = A[i][0]*1 + A[i][1]/4*3 + A[i][2]/4*2 + A[i][3]/4*1;
		b.push(bb);
		bb = A[i][4]*1 + A[i][5]/4*3 + A[i][6]/4*2 + A[i][7]/4*1;
		b.push(bb);
		B.push(b);
	}
	return B;
}