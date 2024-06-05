package org.jpaste;

import org.jpaste.exceptions.PasteException;


 
public abstract class AbstractPaste<P extends AbstractPasteLink> {
	private String contents;

	
	public AbstractPaste(String contents) {
		this.contents = contents;
	}

	
	public String getContents() {
		return contents;
	}

	
	public void setContents(String contents) {
		this.contents = contents;
	}

	
	public abstract P paste() throws PasteException;

}
