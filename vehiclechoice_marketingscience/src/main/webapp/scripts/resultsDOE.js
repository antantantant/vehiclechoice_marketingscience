function result() {
	this.s_allX = [];
//	this.s_allINDa = [];
//	this.s_allINDb = [];
//	this.s_allINDc = [];
//	this.s_allw = [];
	this.s_allfeature = [];
//	this.s_allmean = [];
//	this.s_allstd = [];
	this.u_allX = [];
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
	data = "action=readDOE&id="+id+"&n="+n;
	var request = getRequestObject();
	var r = this;
	
	request.onreadystatechange = function(){
		if ((request.readyState == 4) && (request.status == 200)) {
			var d = request.responseText;
			if (d==""||d=="{}"){
//				r.get();
				id = id + n;
				if (id < totaluser){r.get();}
				else{r.parse();}				
			}
			else{
				r.rawdata.push(eval("(" + d + ")"));
				id = id + n;
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
	var A = []; // styling and attribute training
	var row, i, j, x1, x2, order;
	for (i = 0; i<this.rawdata.length; i++){
		r = this.rawdata[i];
		for (j = 0; j<r.s_allX[0].length/2;j++){
			row = [];
			row.push(i);
			
			if (r.u_allY[0][j]>0){ // better first
				row = row.concat(r.s_allX[0][2*j]);
				row = row.concat(r.u_allX[0][2*j].slice(1,3));
				row = row.concat(r.s_allX[0][2*j+1]);
				row = row.concat(r.u_allX[0][2*j+1].slice(1,3));
			}
			else {
				row = row.concat(r.s_allX[0][2*j+1]);
				row = row.concat(r.u_allX[0][2*j+1].slice(1,3));
				row = row.concat(r.s_allX[0][2*j]);
				row = row.concat(r.u_allX[0][2*j].slice(1,3));
			}
			A.push(row);
		}
	}
	DownloadJSON2CSV(A,'train_styling_attribute.csv');
	
	var X = []; // styling testing
	var A = []; // attribute testing
	for (i = 0; i<this.rawdata.length; i++){
		r = this.rawdata[i];
		for (j = 0; j<r.allValidateFeature[0].length/2;j++){
			row = [];
			row.push(i);
			row = row.concat(r.allValidateX[0][2*j]);
			row = row.concat(r.allValidateX[0][2*j+1]);
			row = row.concat(r.alluserY[0][2*j]);
			X.push(row);
			
			row = [];
			row.push(i);
			row = row.concat(r.allValidateX[0][2*j]);
			row = row.concat(r.allValidateA[0][2*j]);
			row = row.concat(r.allValidateX[0][2*j+1]);
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

function DownloadJSON2CSV(objArray,fileName)
{
    var array = typeof objArray != 'object' ? JSON.parse(objArray) : objArray;

    var str = '';

    for (var row = 0; row < array.length; row++) {
    	var line = '';
    	for (var col in array[row]) {
    		line += array[row][col] + ',';
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
