import org.jpaste.exceptions.PasteException;
import org.jpaste.pastebin.Pastebin;


public class SimplePasteExample {
	
	public static void main(String[] args) throws PasteException {
		String developerKey = "INSERT DEVELOPER KEY HERE";
		String title = "My first jPastebin paste!"; // insert your own title
		String contents = "Hello world"; // insert your own paste contents
		
		
		System.out.println(Pastebin.pastePaste(developerKey, contents, title));
	}

}
