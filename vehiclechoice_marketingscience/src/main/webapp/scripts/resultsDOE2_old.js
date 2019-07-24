function result() {
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
	this.get();
}

totaluser = 100;
id = 0;
n = 1;
result.prototype.get = function(){
	data = "action=readDOE2&id="+id+"&n="+n;
	var request = getRequestObject();
	var r = this;
	
	request.onreadystatechange = function(){
		if ((request.readyState == 4) && (request.status == 200)) {
			var d = request.responseText;
			if (d==""||d=="{}"){
//				r.get();
				id = id + 1;
				if (id < totaluser){r.get();}
				else{r.parse();}				
			}
			else{
				r.rawdata.push(eval("(" + d + ")"));
				id = id + 1;
				if (id < totaluser){r.get();}
				else{r.parse();}				
			}
		}
	};
	request.open("POST", "main", true);
	request.setRequestHeader("Content-Type","application/x-www-form-urlencoded");
	request.send(data);
};

result.prototype.parse = function(){
	var X = []; // styling training
	var A = []; // attribute training
	var row, i, j, x1, x2, order;
	for (i = 0; i<this.rawdata.length; i++){
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
			row = row.concat(r.u_allX[0][2*j][0]);
			row = row.concat(reverseA(r.u_allX[0][2*j].slice(1,3)));
			row = row.concat(r.u_allX[0][2*j+1][0]);
			row = row.concat(reverseA(r.u_allX[0][2*j+1].slice(1,3)));
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
	DownloadJSON2CSV(X,'train_styling.csv');
	DownloadJSON2CSV(A,'train_attribute.csv');
	
	var X = []; // styling testing
	var A = []; // attribute testing
	for (i = 0; i<this.rawdata.length; i++){
		r = this.rawdata[i];
		for (j = 0; j<r.allValidateX[0].length/2;j++){
			row = [];
			row.push(i);
			row = row.concat(r.allValidateFeature[0][2*j]);
			row = row.concat(r.allValidateFeature[0][2*j+1]);
//			row = row.concat(r.allValidateX[0][2*j]);
//			row = row.concat(r.allValidateX[0][2*j+1]);
			row = row.concat(r.alluserY[0][2*j]);
			X.push(row);
			
			row = [];
			row.push(i);
			row = row.concat(r.allValidateFeature[0][2*j]);
//			row = row.concat(r.allValidateX[0][2*j]);
			row = row.concat(r.allValidateA[0][2*j]);
			row = row.concat(r.allValidateFeature[0][2*j+1]);
//			row = row.concat(r.allValidateX[0][2*j+1]);
			row = row.concat(r.allValidateA[0][2*j+1]);
			
			row = row.concat(r.alluserY[0][2*j+1]);
			A.push(row);
		}
	}
	DownloadJSON2CSV(X,'test_styling.csv');
	DownloadJSON2CSV(A,'test_attribute.csv');
	
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
}

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

function reverseA(b)
{
	o = [0,0,0,0,0,0,0,0];
	if(b[0]>0){
		o[4-b[0]*4] = 1;
	}
	if(b[1]>0){
		o[8-b[1]*4] = 1;
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
