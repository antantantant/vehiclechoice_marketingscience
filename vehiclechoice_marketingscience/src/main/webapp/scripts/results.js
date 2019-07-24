function result(arg) {
	this.s_allX = [];
	this.s_allINDa = [];
	this.s_allINDb = [];
	this.s_allINDc = [];
	this.s_allw = [];
	this.s_allfeature = [];
	this.s_allmean = [];
	this.s_allstd = [];
	this.u_allX = [];
	this.u_allINDa = [];
	this.u_allINDb = [];
	this.u_allINDc = [];
	this.u_allw = [];
	this.u_allmean = [];
	this.u_allstd = [];
	this.u_allbeta = [];
	this.allValidateX = [];
	this.allValidateA = [];
	this.alluserY = [];
	this.rawdata = [];
	this.arg = arg;
	this.get();
}

totaluser = 100;
id = 0;
n = 1;
result.prototype.get = function(){
	if(this.arg=="-optimal"){
		data = "action=readOptimal&id="+id;
	}
	else{
		data = "action=read&id="+id+"&n="+n;
	}
	
	var request = getRequestObject();
	var r = this;
	
	request.onreadystatechange = function(){
		if ((request.readyState == 4) && (request.status == 200)) {
			var d = request.responseText;
			if (d==""||d=="{}"){
				id = id + 1;
				if (id < totaluser){r.get();}
				else{
					r.parse();
				}				
			}
			else{
				r.rawdata.push(eval("(" + d + ")"));
				id = id + 1;
				if (id < totaluser){r.get();}
				else{
					r.parse();
				}				
			}
		}
	};
	request.open("POST", "main", true);
	request.setRequestHeader("Content-Type","application/x-www-form-urlencoded");
	request.send(data);
};

result.prototype.parse = function(){
	
	
	if(this.arg=="-optimal"){
		var optX = [];
		for (i = 0; i<this.rawdata.length; i++){
			r = this.rawdata[i];
			optX.push(r.optimal);
		}
		DownloadJSON2CSV(optX,'optimal_design.csv');
	}
	else{
		var X = []; // styling training
		var A = []; // attribute training
		var row, i, j, x1, x2, order;
//		for (i = 0; i<this.rawdata.length; i++){
		for (i = 0; i<this.rawdata.length/4; i++){
			r = this.rawdata[i];
			order = getOrder(r.s_allINDa[0], r.s_allINDb[0], r.s_allINDc[0]);
			for (j = 0; j<r.s_allX[0].length/2;j++){
				row = [];
				row.push(i);
				row = row.concat(r.s_allw[0][order[j]]);
				row = row.concat(r.s_allfeature[0][2*j]);
				row = row.concat(r.s_allfeature[0][2*j+1]);
//				row = row.concat(r.s_allX[0][2*j]);
//				row = row.concat(r.s_allX[0][2*j+1]);
				X.push(row);
				
				row = [];
				row.push(i);
				row = row.concat(r.u_allX[0][2*j]);
				row = row.concat(r.u_allX[0][2*j+1]);
				row = row.concat(r.u_allY[0][j]*r.s_allY[0][j]);
				A.push(row);
			}
			row = [];
			row.push(i);
			row.push(0);
			row = row.concat(r.s_allmean[0]);
			row = row.concat(r.s_allstd[0]);
			X.push(row);
		}
		DownloadJSON2CSV(X,'train_styling1.csv');
		DownloadJSON2CSV(A,'train_attribute1.csv');
		
		var X = []; // styling training
		var A = []; // attribute training
		for (i = this.rawdata.length/4; i<this.rawdata.length/4*2; i++){
			r = this.rawdata[i];
			order = getOrder(r.s_allINDa[0], r.s_allINDb[0], r.s_allINDc[0]);
			for (j = 0; j<r.s_allX[0].length/2;j++){
				row = [];
				row.push(i);
				row = row.concat(r.s_allw[0][order[j]]);
				row = row.concat(r.s_allfeature[0][2*j]);
				row = row.concat(r.s_allfeature[0][2*j+1]);
//				row = row.concat(r.s_allX[0][2*j]);
//				row = row.concat(r.s_allX[0][2*j+1]);
				X.push(row);
				
				row = [];
				row.push(i);
				row = row.concat(r.u_allX[0][2*j]);
				row = row.concat(r.u_allX[0][2*j+1]);
				row = row.concat(r.u_allY[0][j]*r.s_allY[0][j]);
				A.push(row);
			}
			row = [];
			row.push(i);
			row.push(0);
			row = row.concat(r.s_allmean[0]);
			row = row.concat(r.s_allstd[0]);
			X.push(row);
		}
		DownloadJSON2CSV(X,'train_styling2.csv');
		DownloadJSON2CSV(A,'train_attribute2.csv');
		var X = []; // styling training
		var A = []; // attribute training
		for (i = this.rawdata.length/4*2; i<this.rawdata.length/4*3; i++){
			r = this.rawdata[i];
			order = getOrder(r.s_allINDa[0], r.s_allINDb[0], r.s_allINDc[0]);
			for (j = 0; j<r.s_allX[0].length/2;j++){
				row = [];
				row.push(i);
				row = row.concat(r.s_allw[0][order[j]]);
				row = row.concat(r.s_allfeature[0][2*j]);
				row = row.concat(r.s_allfeature[0][2*j+1]);
//				row = row.concat(r.s_allX[0][2*j]);
//				row = row.concat(r.s_allX[0][2*j+1]);
				X.push(row);
				
				row = [];
				row.push(i);
				row = row.concat(r.u_allX[0][2*j]);
				row = row.concat(r.u_allX[0][2*j+1]);
				row = row.concat(r.u_allY[0][j]*r.s_allY[0][j]);
				A.push(row);
			}
			row = [];
			row.push(i);
			row.push(0);
			row = row.concat(r.s_allmean[0]);
			row = row.concat(r.s_allstd[0]);
			X.push(row);
		}
		DownloadJSON2CSV(X,'train_styling3.csv');
		DownloadJSON2CSV(A,'train_attribute3.csv');
		var X = []; // styling training
		var A = []; // attribute training
		for (i = this.rawdata.length/4*3; i<this.rawdata.length; i++){
			r = this.rawdata[i];
			order = getOrder(r.s_allINDa[0], r.s_allINDb[0], r.s_allINDc[0]);
			for (j = 0; j<r.s_allX[0].length/2;j++){
				row = [];
				row.push(i);
				row = row.concat(r.s_allw[0][order[j]]);
				row = row.concat(r.s_allfeature[0][2*j]);
				row = row.concat(r.s_allfeature[0][2*j+1]);
//				row = row.concat(r.s_allX[0][2*j]);
//				row = row.concat(r.s_allX[0][2*j+1]);
				X.push(row);
				
				row = [];
				row.push(i);
				row = row.concat(r.u_allX[0][2*j]);
				row = row.concat(r.u_allX[0][2*j+1]);
				row = row.concat(r.u_allY[0][j]*r.s_allY[0][j]);
				A.push(row);
			}
			row = [];
			row.push(i);
			row.push(0);
			row = row.concat(r.s_allmean[0]);
			row = row.concat(r.s_allstd[0]);
			X.push(row);
		}
		DownloadJSON2CSV(X,'train_styling4.csv');
		DownloadJSON2CSV(A,'train_attribute4.csv');
		
		var X = []; // styling testing
		var A = []; // attribute testing
		for (i = 0; i<this.rawdata.length/4; i++){
			r = this.rawdata[i];
			for (j = 0; j<r.allValidateX[0].length/2;j++){
				row = [];
				row.push(i);
				row = row.concat(r.allValidateFeature[0][2*j]);
				row = row.concat(r.allValidateFeature[0][2*j+1]);
//				row = row.concat(r.allValidateX[0][2*j]);
//				row = row.concat(r.allValidateX[0][2*j+1]);
				
				row = row.concat(r.alluserY[0][2*j]);
				X.push(row);
				
				row = [];
				row.push(i);
				row = row.concat(r.allValidateFeature[0][2*j]);
//				row = row.concat(r.allValidateX[0][2*j]);
				row = row.concat(r.allValidateA[0][2*j]);
				row = row.concat(r.allValidateFeature[0][2*j+1]);
//				row = row.concat(r.allValidateX[0][2*j+1]);
				row = row.concat(r.allValidateA[0][2*j+1]);
				
				row = row.concat(r.alluserY[0][2*j+1]);
				A.push(row);
			}
		}
		DownloadJSON2CSV(X,'test_styling1.csv');
		DownloadJSON2CSV(A,'test_attribute1.csv');
		
		var X = []; // styling testing
		var A = []; // attribute testing
		for (i = this.rawdata.length/4; i<this.rawdata.length/4*2; i++){
			r = this.rawdata[i];
			for (j = 0; j<r.allValidateX[0].length/2;j++){
				row = [];
				row.push(i);
				row = row.concat(r.allValidateFeature[0][2*j]);
				row = row.concat(r.allValidateFeature[0][2*j+1]);
//				row = row.concat(r.allValidateX[0][2*j]);
//				row = row.concat(r.allValidateX[0][2*j+1]);
				
				row = row.concat(r.alluserY[0][2*j]);
				X.push(row);
				
				row = [];
				row.push(i);
				row = row.concat(r.allValidateFeature[0][2*j]);
//				row = row.concat(r.allValidateX[0][2*j]);
				row = row.concat(r.allValidateA[0][2*j]);
				row = row.concat(r.allValidateFeature[0][2*j+1]);
//				row = row.concat(r.allValidateX[0][2*j+1]);
				row = row.concat(r.allValidateA[0][2*j+1]);
				
				row = row.concat(r.alluserY[0][2*j+1]);
				A.push(row);
			}
		}
		DownloadJSON2CSV(X,'test_styling2.csv');
		DownloadJSON2CSV(A,'test_attribute2.csv');
		var X = []; // styling testing
		var A = []; // attribute testing
		for (i = this.rawdata.length/4*2; i<this.rawdata.length/4*3; i++){
			r = this.rawdata[i];
			for (j = 0; j<r.allValidateX[0].length/2;j++){
				row = [];
				row.push(i);
				row = row.concat(r.allValidateFeature[0][2*j]);
				row = row.concat(r.allValidateFeature[0][2*j+1]);
//				row = row.concat(r.allValidateX[0][2*j]);
//				row = row.concat(r.allValidateX[0][2*j+1]);
				
				row = row.concat(r.alluserY[0][2*j]);
				X.push(row);
				
				row = [];
				row.push(i);
				row = row.concat(r.allValidateFeature[0][2*j]);
//				row = row.concat(r.allValidateX[0][2*j]);
				row = row.concat(r.allValidateA[0][2*j]);
				row = row.concat(r.allValidateFeature[0][2*j+1]);
//				row = row.concat(r.allValidateX[0][2*j+1]);
				row = row.concat(r.allValidateA[0][2*j+1]);
				
				row = row.concat(r.alluserY[0][2*j+1]);
				A.push(row);
			}
		}
		DownloadJSON2CSV(X,'test_styling3.csv');
		DownloadJSON2CSV(A,'test_attribute3.csv');
		var X = []; // styling testing
		var A = []; // attribute testing
		for (i = this.rawdata.length/4*3; i<this.rawdata.length; i++){
			r = this.rawdata[i];
			for (j = 0; j<r.allValidateX[0].length/2;j++){
				row = [];
				row.push(i);
				row = row.concat(r.allValidateFeature[0][2*j]);
				row = row.concat(r.allValidateFeature[0][2*j+1]);
//				row = row.concat(r.allValidateX[0][2*j]);
//				row = row.concat(r.allValidateX[0][2*j+1]);
				
				row = row.concat(r.alluserY[0][2*j]);
				X.push(row);
				
				row = [];
				row.push(i);
				row = row.concat(r.allValidateFeature[0][2*j]);
//				row = row.concat(r.allValidateX[0][2*j]);
				row = row.concat(r.allValidateA[0][2*j]);
				row = row.concat(r.allValidateFeature[0][2*j+1]);
//				row = row.concat(r.allValidateX[0][2*j+1]);
				row = row.concat(r.allValidateA[0][2*j+1]);
				
				row = row.concat(r.alluserY[0][2*j+1]);
				A.push(row);
			}
		}
		DownloadJSON2CSV(X,'test_styling4.csv');
		DownloadJSON2CSV(A,'test_attribute4.csv');
		var survey = [];
		for (i = 0; i<this.rawdata.length; i++){
			row = [];
			row.push(i);
			var o = eval("("+this.rawdata[i].allSurvey[0]+")");
			$.each(o,function(key,value){
				row.push(parseInt(value));
			});
			survey.push(row);
		}
		DownloadJSON2CSV(survey,'demographicinfo.csv');	
		this.postanalysis();
	}
}

result.prototype.postanalysis = function(){
	
	// ************* STYLING SCORE ************* 
	// the following outputs the styling scores from all users of a predefined sweep of designs
	// individual scores are combined with crowd ones
	var i,j,k,s,c;
	var ind_weight = 0.11; // weight on the individual, weight on crowd will be 1-ind_weight
	this.individual_geometry_score = [];
	// this should be #individuals * #features * #feature levels
	for (i = 0; i<this.rawdata.length; i++){
		this.individual_geometry_score.push(this.rawdata[i].allStylingScore[0]);
	}
	
	this.crowd_geometry_score = [];
	// this should be #features * #feature levels

	this.combined_geometry_score = [];
	// this should be (#features * #feature levels) * #individuals
	// [f1, l1, i1], [f1, l1, i2], ..., [f1, l1, i_end]
	// [f1, l2, i1], [f1, l2, i2], ..., [f1, l2, i_end]
	// ...
	// [f1, l_end, i1], [f1, l_end, i2], ..., [f1, l_end, i_end]
	// [f2, l1, i1], [f2, l1, i2], ..., [f2, l1, i_end]
	// ...
	// [f_end, l_end, i1], [f_end, l_end, i2], ..., [f_end, l_end, i_end]
	
	for (i = 0; i<this.individual_geometry_score[0].length; i++){
		s = [];
		for (j = 0; j<this.individual_geometry_score[0][0].length; j++){
			c = [];
			s.push(0.0);
			for (k = 0; k<this.rawdata.length; k++){
				s[j]+=this.individual_geometry_score[k][i][j];
			}
			s[j] = s[j]/this.rawdata.length;
			
			for (k = 0; k<this.rawdata.length; k++){
				c.push(ind_weight * this.individual_geometry_score[k][i][j]
				+ (1-ind_weight) * s[j]); 
			}
			this.combined_geometry_score.push(c);
		}
		this.crowd_geometry_score.push(s);
	}
	DownloadJSON2CSV(this.combined_geometry_score,'combined_geometry_score.csv');
	// ************* STYLING SCORE ************* 
};

function getOrder(A,B,C){
	var i,la,lb,lc,o;
	var count = 0;
	if (A==null){la = 0;}
	else{la = A.length;}
	if (B==null){lb = 0;}
	else{lb = B.length;}
	if (C==null){lc = 0;}
	else{lc = C.length;}

	o = Array(la+lb+lc); //index: original X index, element: reordered w index

	for (i=0;i<la;i++){
		o[A[i][0]/2] = count;
		count += 1;
	}
	for (i=0;i<lb;i++){
		o[B[i][0]/2] = count;
		count += 1;
	}
	for (i=0;i<lc;i++){
		o[C[i][0]/2] = count;
		count += 1;
	}
	return o;
}

function DownloadJSON2CSV(objArray,fileName)
{
    var array = typeof objArray != 'object' ? JSON.parse(objArray) : objArray;

    var str = '';

    for (var row = 0; row < array.length; row++) {
    	var line = '';
    	for (var col in array[row]) {
    		line += Math.round(array[row][col]*100)/100 + ',';
        }
    	line.slice(0,line.Length-1); 
    	str += line + '\r\n';
    }
    a=document.createElement('a');
    a.textContent='download';
    a.download=fileName;
    a.href='data:text/csv;charset=utf-8,'+escape(str);
    document.body.appendChild(a);
//    window.open( "data:text/csv;charset=utf-8," + escape(str))
}
