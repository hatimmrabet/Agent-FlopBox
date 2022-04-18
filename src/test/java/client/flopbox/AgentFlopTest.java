package client.flopbox;

import static org.junit.Assert.*;

import java.io.IOException;

import org.apache.http.client.ClientProtocolException;
import org.junit.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import client.flopbox.model.User;
import client.flopbox.util.AgentFlopBox;

public class AgentFlopTest {
	User user;
	JsonElement je;
	void init() throws ClientProtocolException, IOException{
		 user = User.getInstance();
		je = AgentFlopBox.infoRemoteFiles( user.getServeur("lille"), "D:/Bureau/synchro/test.txt");
		 
	}

	@Test
	public void getserveurfromflopOk() throws ClientProtocolException, IOException {
		JsonObject jsonObject = AgentFlopBox.getServeursFromFlopBox().getAsJsonObject();
	     int port = jsonObject.get("lille").getAsJsonObject().get("port").getAsInt();
	    String url = jsonObject.get("lille").getAsJsonObject().get("url").getAsString();
	    String url1 = "webtp.fil.univ-lille1.fr";
	    assertEquals(url,url1);
		assertSame(port,21);
	}
	@Test
	public void infoRemoteFilesWhenFileNotExists()  {
	   assertSame(je,null);
		
	}
	

}
