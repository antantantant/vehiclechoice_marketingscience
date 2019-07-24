package namwoo2013;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;

@Entity
public class TestDOE
{
	// Objectify auto-generates Long IDs just like JDO / JPA
	@Id Long id;
	String data;
	String name;
	@Transient String doNotPersist;
	
	public TestDOE(){}
	
	public TestDOE(String data, String name){
		this.data = data;
		this.name = name;
	}
}
