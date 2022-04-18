package client.flopbox;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.Before;
import org.junit.Test;

import client.flopbox.model.User;

public class UserTest {
	
	 User  user;
    @Before 
    public void init() {
      user = User.getInstance();
    
    
    }

	@Test
	public void testnameOki() {
		assertEquals(user.getUserName(),"hatim");
		
	}
	@Test
	public void testpassOki() {
		assertEquals(user.getPassword(),"12345");
	
	}
	@Test
	public void testlocalPathNotOki() {
		assertNotSame(user.getRacinePath(),"D:/Bureau/synchr");
		
	}
	@Test
	public void testgetracineNotoki() {
		File file = new File("D:/Bureau/synchr");
		assertNotSame(user.getRacine(),file);
		
	}

}

