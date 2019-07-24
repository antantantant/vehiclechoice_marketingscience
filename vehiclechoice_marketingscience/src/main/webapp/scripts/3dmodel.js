var canvas_dragging=null,canvas_over=null,alt=false,shift=false;
var r = "textures/";
var urls = [ r + "px.png", r + "nx.png",
			 r + "pz.png", r + "nz.png",
			 r + "py.png", r + "ny.png" ];
var textureCube = THREE.ImageUtils.loadTextureCube( urls, new THREE.CubeRefractionMapping() );

var mlib = {
		"Orange metal": new THREE.MeshLambertMaterial( { color: 0xff6600, envMap: textureCube, side: THREE.DoubleSide } ),
		"yes": new THREE.MeshLambertMaterial( { color: 0xffee00, envMap: textureCube, refractionRatio: 0.05, side: THREE.DoubleSide } ),
		"Darkgray shiny":	new THREE.MeshPhongMaterial( { color: 0x000000, specular: 0x050505, side: THREE.DoubleSide } ),
		"Gray shiny":		new THREE.MeshPhongMaterial( { color: 0xdddddd, specular: 0xeeeedd, shininess: 30, side: THREE.DoubleSide } ),
		}	
var carMaterial = mlib["Gray shiny"];
var wheelColor = 0xAAAAAA;
var carSelectedColor = 0xffffff;
var carUnselectedColor = 0xeeeeee;
var carSpecColor = 0xeeeeee;
var tireSelectedColor = 0x555555;
var tireUnselectedColor = 0x111111;


function model(parameter){
    var order = 2;
    var step = 16;
    var body_n = 32;
    var wheel_n = 5;
    var wheel_step = 38;
    var tire_step = 55;
    this.car = {VertexPositionData:[],IndexData:[],NormalData:[]};
    
    var m = car_model(parameter, order, step, wheel_n, wheel_step, tire_step);
    this.car.VertexPositionData = m[0];
    this.wheel_center = m[1];
    this.wheelbase = m[2];
    
    // car body
    this.car.IndexData = carIndex(step, body_n);
    this.car.NormalData = carNormal(this.car.VertexPositionData, this.car.IndexData, step, body_n);
    this.car.IndexData = mirrorindex(this.car.IndexData, this.car.VertexPositionData.length/3);
    this.car.VertexPositionData = mirrorvertex(this.car.VertexPositionData);
    this.car.NormalData = mirrornormal(this.car.NormalData);

    // make black boxes
    var px = (this.car.VertexPositionData[4*(step+1)*(step+1)*3]+this.car.VertexPositionData[(4*(step+1)*(step+1)-(step+1))*3])/2;
    var py = (this.car.VertexPositionData[4*(step+1)*(step+1)*3+1]+this.car.VertexPositionData[(4*(step+1)*(step+1)-(step+1))*3+1])/2;
    var pz = (this.car.VertexPositionData[4*(step+1)*(step+1)*3+2]+this.car.VertexPositionData[(4*(step+1)*(step+1)-(step+1))*3+2])/2;
    var x = Math.abs(this.car.VertexPositionData[4*(step+1)*(step+1)*3]-this.car.VertexPositionData[(4*(step+1)*(step+1)-(step+1))*3]);
    var y = 1.0, z = 3;
    this.cube = new THREE.Mesh(new THREE.CubeGeometry(x/1.3,z,y),new THREE.MeshLambertMaterial({color: 0x000000}));
    this.cube.position.x = px; this.cube.position.y = py+0.4; this.cube.position.z = pz;
}

model.prototype.update = function(parameter){
    var order = 2;
    var step = 16;
    var body_n = 32;
    var wheel_n = 5;
    var wheel_step = 38;
    var tire_step = 55;
	var m = car_model(parameter, order, step, wheel_n, wheel_step, tire_step);
	this.car.VertexPositionData = m[0];
	this.car.NormalData = carNormal(this.car.VertexPositionData, this.car.IndexData, step, body_n);
	this.car.VertexPositionData = mirrorvertex(this.car.VertexPositionData);
    this.car.NormalData = mirrornormal(this.car.NormalData);
}

function to_three(model, mat, texture, spec_colr, unsel_colr, reflectivity){
	var geom = new THREE.Geometry();
	geom.verticesNeedUpdate = true;
	geom.elementsNeedUpdate = true;
	geom.morphTargetsNeedUpdate = true;
	geom.uvsNeedUpdate = true;
	geom.normalsNeedUpdate = true;
	geom.colorsNeedUpdate = true;
	geom.tangentsNeedUpdate = true;
	geom.dynamic = true;
	
	for (var i = 0;i<model.VertexPositionData.length/3;i++){
		geom.vertices.push(new THREE.Vector3(model.VertexPositionData[i*3],model.VertexPositionData[i*3+1],model.VertexPositionData[i*3+2]));
	}
	for (i = 0;i<model.IndexData.length/3;i++){
		geom.faces.push(new THREE.Face3(model.IndexData[i*3],model.IndexData[i*3+1],model.IndexData[i*3+2]));
		geom.faces[i].vertexNormals = [new THREE.Vector3(model.NormalData[model.IndexData[i*3]*3],
												   model.NormalData[model.IndexData[i*3]*3+1],
												   model.NormalData[model.IndexData[i*3]*3+2]),
		                                   new THREE.Vector3(model.NormalData[model.IndexData[i*3+1]*3],
		                                		   model.NormalData[model.IndexData[i*3+1]*3+1],
		                                		   model.NormalData[model.IndexData[i*3+1]*3+2]),
		                                   new THREE.Vector3(model.NormalData[model.IndexData[i*3+2]*3],
		                                		   model.NormalData[model.IndexData[i*3+2]*3+1],
		                                		   model.NormalData[model.IndexData[i*3+2]*3+2])];
	}
	geom.computeFaceNormals();
//	if (mat==0){
//		var material = carMaterial;
//	}
//	else{ var material = new THREE.MeshLambertMaterial();}
	var material = new THREE.MeshPhongMaterial( { color: 0xdddddd, specular: 0xeeeedd, shininess: 30, side: THREE.DoubleSide });
	var mesh = new THREE.Mesh(geom, material);
	return mesh;
}

function webgl_scene(scene_id,scene_width,scene_height,scene_left,scene_top,model_parameter){
	this.position_down = false;
	this.scene_id = scene_id;
	this.scene_width = scene_width;
	this.scene_height = scene_height;
	this.scene_left = scene_left;
	this.scene_top = scene_top;
	this.model_num = model_parameter.length;
	this.model_parameter = model_parameter;
	this.view_angle = 45;
	this.aspect = scene_width/scene_height;
	this.near = 0.1;
	this.far = 10000;
	
	this.container = $("#"+scene_id);
	this.renderer = new THREE.WebGLRenderer({antialias:true});
	this.renderer.domElement.style = "position:absolute; left:"
		+ scene_left.toString() + "px; top:" + scene_top.toString() + "px";
	this.renderer.shadowMapEnabled = true;
	this.renderer.gammaInput = true;
	this.renderer.gammaOutput = true;
	this.renderer.physicallyBasedShading = true;
	this.renderer.setSize(scene_width,scene_height);
    this.renderer.shadowMapEnabled = true;
    this.renderer.shadowMapSoft = true;
	this.container.append(this.renderer.domElement);
	
	this.camera =   
		new THREE.PerspectiveCamera(
			this.view_angle,
			this.aspect,
			this.near,
			this.far);
	this.scene = new THREE.Scene();


	var planeGeo = new THREE.PlaneGeometry(1000, 1000, 10, 10);
	var planeMat = new THREE.MeshPhongMaterial({color: 0x000000});
	var plane = new THREE.Mesh(planeGeo, planeMat);
//	if (model_purpose > 0){plane.rotation.x = -Math.PI/12*5;}
	plane.position.z = -0.8;
	plane.receiveShadow = true;
	
	var m = new model(model_parameter);
	var carMesh = to_three(m.car,0,this.textureCube,carSpecColor,carUnselectedColor,0.05);
	var bodyCube = m.cube;
	this.rawModel=m;
	
	carMesh.castShadow = true;
	bodyCube.castShadow = true;
	carMesh.selected = false;
	
    this.carModel = new THREE.Object3D();
    this.carModel.add(carMesh);
    this.carModel.add(bodyCube);
    
    var d = this;
    var e = this.carModel;
    var loader = new THREE.ColladaLoader();
	loader.options.convertUpAxis = true;
	loader.load( '/resource/Mustang_GT500_Wheel.dae', function ( collada ) {
		var dae = collada.scene;
		dae.scale.x = dae.scale.y = dae.scale.z = 0.0024;
		dae.updateMatrix();
		var fr = dae.clone();
		fr.rotation.z = Math.PI/2;
		fr.position.x = 0.8 + m.wheel_center[0];
		fr.position.y = 0.30;
		fr.position.z = -3.7;
		e.add(fr);
		var fl = dae.clone();
		fl.rotation.z = -Math.PI/2;
		fl.position.x = -0.8 + m.wheel_center[0];
		fl.position.y = -0.3;
		fl.position.z = -3.7;
		e.add(fl);
		var rr = dae.clone();
		rr.scale.x = rr.scale.y = rr.scale.z = 0.0026;
		rr.rotation.z = Math.PI/2;
		rr.position.x = 1.0 + m.wheelbase+ m.wheel_center[0];
		rr.position.y = 0.1;
		rr.position.z = -4.0;
		e.add(rr);
		var rl = dae.clone();
		rl.scale.x = rl.scale.y = rl.scale.z = 0.0026;
		rl.rotation.z = -Math.PI/2;
		rl.position.x = -0.8 + m.wheelbase+ m.wheel_center[0];
		rl.position.y = -0.1;
		rl.position.z = -4.0;
		e.add(rl);
		d.renderer.render(d.scene,d.camera);
	} );
    
	var light = new THREE.SpotLight(0xFEEED4,0.9);
	light.position.set(-200,100,200);
	light.castShadow = true;
    light.shadowCameraFov = 5;
    light.shadowDarkness = 0.9;
    light.shadowMapWidth = scene_width;
    light.shadowMapHeight = scene_height;
    this.scene.add( light );
	
	directionalLight = new THREE.DirectionalLight( 0xeeeeff, 0.6 );
	directionalLight.position.x = 200;
	directionalLight.position.y = 100;
	directionalLight.position.z = 200;
	directionalLight.position.normalize();
	directionalLight.castShadow = true;
	directionalLight.shadowDarkness = 0.9;
	directionalLight.shadowMapWidth = scene_width;
	directionalLight.shadowMapHeight = scene_height;		
	this.scene.add( directionalLight );
	
	this.scene.add(this.camera);
	this.scene.add(plane);
	this.scene.add(this.carModel);
	this.camera.rotation.x=Math.PI/3;
	this.camera.position.x=-0.2;
	this.camera.position.y=-11;
	this.camera.position.z=5.0;
	
	$("#"+scene_id).click(function(e){switch(e.which){case 1:if(d.click){d.prehandleEvent(e);d.click(e)}break;
            case 2:if(d.middleclick){d.prehandleEvent(e);d.middleclick(e)}break;
            case 3:if(d.rightclick){d.prehandleEvent(e);d.rightclick(e)}break}});
    $("#"+scene_id).dblclick(function(e){if(d.dblclick){d.prehandleEvent(e);d.dblclick(e)}});
    $("#"+scene_id).mousedown(function(e){switch(e.which){case 1:canvas_dragging=d;if(d.mousedown){d.prehandleEvent(e);d.mousedown(e)}break;
        case 2:if(d.middlemousedown){d.prehandleEvent(e);d.middlemousedown(e)}break;
        case 3:if(d.rightmousedown){d.prehandleEvent(e);d.rightmousedown(e)}break}});
    $("#"+scene_id).mousemove(function(e){if(canvas_dragging==null&&d.mousemove){d.prehandleEvent(e);d.mousemove(e)}});
    $("#"+scene_id).mouseout(function(e){canvas_over=null;if(d.mouseout){d.prehandleEvent(e);d.mouseout(e)}});
    $("#"+scene_id).mouseover(function(e){canvas_over=d;if(d.mouseover){d.prehandleEvent(e);d.mouseover(e)}});
    $("#"+scene_id).mouseup(function(e){switch(e.which){case 1:if(d.mouseup){d.prehandleEvent(e);d.mouseup(e)}break;
        case 2:if(d.middlemouseup){d.prehandleEvent(e);d.middlemouseup(e)}break;
        case 3:if(d.rightmouseup){d.prehandleEvent(e);d.rightmouseup(e)}break}});
    $("#"+scene_id).mousewheel(function(e,f){if(d.mousewheel){d.prehandleEvent(e);d.mousewheel(e,f)}});
}

webgl_scene.prototype.update=function(model_parameter){
	this.rawModel.update(model_parameter);
	var geom = this.carModel.children[0].geometry;
	var model = this.rawModel.car;
	for (var i = 0;i<model.VertexPositionData.length/3;i++){
		geom.vertices[i].set(model.VertexPositionData[i*3],model.VertexPositionData[i*3+1],model.VertexPositionData[i*3+2]);
	}
	for (i = 0;i<model.IndexData.length/3;i++){
		geom.faces[i].vertexNormals[0].set(model.NormalData[model.IndexData[i*3]*3],
												   model.NormalData[model.IndexData[i*3]*3+1],
												   model.NormalData[model.IndexData[i*3]*3+2]);
		geom.faces[i].vertexNormals[1].set(model.NormalData[model.IndexData[i*3+1]*3],
		                                		   model.NormalData[model.IndexData[i*3+1]*3+1],
		                                		   model.NormalData[model.IndexData[i*3+1]*3+2]);
		geom.faces[i].vertexNormals[2].set(model.NormalData[model.IndexData[i*3+2]*3],
		                                		   model.NormalData[model.IndexData[i*3+2]*3+1],
		                                		   model.NormalData[model.IndexData[i*3+2]*3+2]);
	}
	geom.computeFaceNormals();
	geom.verticesNeedUpdate = true;
	geom.elementsNeedUpdate = true;
	geom.morphTargetsNeedUpdate = true;
	geom.uvsNeedUpdate = true;
	geom.normalsNeedUpdate = true;
	geom.colorsNeedUpdate = true;
	geom.tangentsNeedUpdate = true;
	geom.dynamic = true;
}

webgl_scene.prototype.prehandleEvent=function(a){
    a.preventDefault();
    var b=$("#"+this.scene_id).offset();
    a.x=a.pageX-b.left;
    a.y=a.pageY-b.top;
};

webgl_scene.prototype.mousedown=function(a){
	this.x=a.x,this.y=a.y;
};

webgl_scene.prototype.rightmousedown=function(a){
};

webgl_scene.prototype.mouseup=function(a){
}

webgl_scene.prototype.drag=function(a){
	if (!$("sync_model").attr("checked")){
    	this.carModel.rotation.z+=(a.x-this.x)*Math.PI/480;
    	this.renderer.render(this.scene,this.camera);
	    this.x=a.x,this.y=a.y;
	}
};

webgl_scene.prototype.mousewheel=function(a,b){
};

webgl_scene.prototype.dblclick=function(a){
}

webgl_scene.prototype.keydown=function(a){
//	if (a.keyCode==38){
//		if(this.position_down){
//			camera_init(this.camera.position.z,this.camera.position.y,this.camera.rotation.x,
//					this.camera.position.z-5,this.camera.position.y+10,this.camera.rotation.x-0.5,this);
//			tween_animate();
//			this.position_down=false;
//		}
//	}
//	else if(a.keyCode==40){
//		if(!this.position_down){
//			camera_init(this.camera.position.z,this.camera.position.y,this.camera.rotation.x,
//					this.camera.position.z+5,this.camera.position.y-10,this.camera.rotation.x+0.5,this);
//			tween_animate();
//			this.position_down=true;
//		}
//	}
//	
}

jQuery(document).ready(function(){
    $(document).mousemove(function(a){
        if(canvas_dragging!=null)
            if(canvas_dragging.drag){
            	canvas_dragging.prehandleEvent(a);canvas_dragging.drag(a)
            }
    });
    $(document).mouseup(function(a){
        if(canvas_dragging!=null){
            if(canvas_dragging.mouseup){
            	canvas_dragging.prehandleEvent(a);
            	canvas_dragging.mouseup(a)
            }
            canvas_dragging=null
        }
    });
    $(document).keydown(function(a){
        id=a.which;
        var b=canvas_over;
        if(canvas_dragging!=null)b=canvas_dragging;
        if(b!=null)if(b.keydown){
            b.prehandleEvent(a);
            b.keydown(a)
        }
    });
    $(document).keypress(function(a){
        var b=canvas_over;
        if(canvas_dragging!=null)b=canvas_dragging;
        if(b!=null)if(b.keypress){
            b.prehandleEvent(a);
            b.keypress(a)
        }
    });
    $(document).keyup(function(a){
        shift=a.shiftKey;
        alt=a.altKey;
        var b=canvas_over;
        if(canvas_dragging!=null)b=canvas_dragging;
        if(b!=null)if(b.keyup){
            b.prehandleEvent(a);
            b.keyup(a)
        }
    })
});