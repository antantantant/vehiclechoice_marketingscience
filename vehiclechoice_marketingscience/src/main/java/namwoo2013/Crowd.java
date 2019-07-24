package namwoo2013;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;

@Entity
public class Crowd
{
	// Objectify auto-generates Long IDs just like JDO / JPA
	@Id Long id;
	String data;
	String name;
	@Transient String doNotPersist;
	
	public Crowd(){}
	
	public Crowd(String data, String name){
//		this.styleModel = styleModel;
//		this.utilityModel = utilityModel;
//		this.validateSet = validateSet;
//		this.userY = userY;
		this.data = data;
		this.name = name;
	}
}
