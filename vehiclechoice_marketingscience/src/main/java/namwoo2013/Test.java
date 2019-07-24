package namwoo2013;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;

@Entity
public class Test
{
	// Objectify auto-generates Long IDs just like JDO / JPA
	@Id Long id;
//	String styleModel;
//	String utilityModel;
//	String validateSet;
//	String userY;
	String data;
	String model;
	@Transient String doNotPersist;
	
	public Test(){}
	
	public Test(String data, String model){
//		this.styleModel = styleModel;
//		this.utilityModel = utilityModel;
//		this.validateSet = validateSet;
//		this.userY = userY;
		this.data = data;
		this.model = model;
	}
}
