var canvas_dragging=null,canvas_over=null,alt=false,shift=false;

function load_scene(scene_id,scene_width,scene_height,scene_left,scene_top,model_parameter){
	this.position_down = false;
	this.scene_id = scene_id;
	this.scene_width = scene_width;
	this.scene_height = scene_height;
	this.scene_left = scene_left;
	this.scene_top = scene_top;
	this.model_num = 1;
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

//	var planeGeo = new THREE.PlaneGeometry(1000, 1000, 10, 10);
//	var planeMat = new THREE.MeshPhongMaterial();
//	planeMat.specular.setHex(0x000000);
//	planeMat.combine = THREE.MixOperation;
//	planeMat.reflectivity = 0.0;
//	var plane = new THREE.Mesh(planeGeo, planeMat);
//	plane.position.y = -0.80;
//	plane.receiveShadow = true;
//	this.plane = plane;
	
	var size = 14, step = 1;
	var geometry = new THREE.Geometry();
	var material = new THREE.LineBasicMaterial( { color: 0x303030 } );
	for ( var i = - size; i <= size; i += step ) {
		geometry.vertices.push( new THREE.Vector3( - size, - 0.04, i ) );
		geometry.vertices.push( new THREE.Vector3(   size, - 0.04, i ) );
		geometry.vertices.push( new THREE.Vector3( i, - 0.04, - size ) );
		geometry.vertices.push( new THREE.Vector3( i, - 0.04,   size ) );
	}
	this.line = new THREE.Line( geometry, material, THREE.LinePieces );
	
	this.light = new THREE.SpotLight(0xFEEED4,1);
	this.light.position.set(0,0,200);
	this.light.castShadow = true;
    this.light.shadowCameraFov = 5;
    this.light.shadowDarkness = 0.8;
    this.light.shadowMapWidth = scene_width;
    this.light.shadowMapHeight = scene_height;
    
	var loader = new THREE.ColladaLoader();
	loader.options.convertUpAxis = true;
	var d = this;
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
	
	for (var i=0;i<this.model_num;i++){
		loader.load(model_parameter[i], function ( collada ){
			callback( collada, d );
			});
	}
}

function callback(collada, d){
	var dae = collada.scene;
	var skin = collada.skins[ 0 ];
	dae.scale.x = dae.scale.y = dae.scale.z = 0.5;
	dae.position.y +=1;
	dae.updateMatrix();
	
//	var particleLight = new THREE.Mesh( new THREE.SphereGeometry( 4, 8, 8 ), new THREE.MeshBasicMaterial( { color: 0xffffff } ) );
//	d.scene.add( particleLight );
	
//	d.scene.add( new THREE.AmbientLight( 0xcccccc ) );
//	d.scene.add(d.light);
	var directionalLight = new THREE.DirectionalLight(/*Math.random() * 0xffffff*/0xeeeeee );
	directionalLight.position.x = 5;
	directionalLight.position.y = 5;
	directionalLight.position.z = 5;
	directionalLight.position.normalize();
	d.scene.add( directionalLight );
	
//	pointLight = new THREE.PointLight( 0xffffff, 0.0 );
//	pointLight.position = particleLight.position;
//	d.scene.add( pointLight );

	d.scene.add(d.camera);
	d.scene.add(d.line);
	d.scene.add(dae);
	d.renderer.render(d.scene,d.camera);
}

load_scene.prototype.update=function(model_parameter,i){
}

load_scene.prototype.prehandleEvent=function(a){
    a.preventDefault();
    var b=$("#"+this.scene_id).offset();
    a.x=a.pageX-b.left;
    a.y=a.pageY-b.top;
};

load_scene.prototype.mousedown=function(a){
	this.x=a.x,this.y=a.y;
};

load_scene.prototype.rightmousedown=function(a){
};

load_scene.prototype.mouseup=function(a){
}

load_scene.prototype.drag=function(a){
	var l = this.scene.children.length;
	this.scene.children[l-1].rotation.y+=(a.x-this.x)*Math.PI/480;
	this.renderer.render(this.scene,this.camera);
    this.x=a.x,this.y=a.y;
};

load_scene.prototype.mousewheel=function(a,b){
};

load_scene.prototype.dblclick=function(a){
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