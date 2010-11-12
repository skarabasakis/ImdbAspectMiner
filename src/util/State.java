// The MIT License
//
// Copyright (c) 2010 Stelios Karabasakis
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.
//
package util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.logging.Level;
import config.Paths;


/**
 * TODO Description missing
 * @author Stelios Karabasakis
 */
public class State<T> {
	
	private T					obj;
	private String				obj_name;
	private File				state_file;
	private File				backup_state_file;
	private ObjectOutputStream	state_out	= null;
	private ObjectInputStream	state_in	= null;
	
	public State(String obj_name, T obj)
	{
		this.obj = obj;
		this.obj_name = obj_name;
		backup_state_file = new File(Paths.indexerState + "/" + obj_name + ".ser.bak");
		state_file = new File(Paths.indexerState + "/" + obj_name + ".ser");
	}
	
	/**
	 * @return the obj
	 */
	public T getObj()
	{
		return obj;
	}
	
	private File backupAndRecreate()
	{
		if (state_file.exists()) {
			backup_state_file.delete();
			if (!state_file.renameTo(backup_state_file)) {
				AppLogger.error.log(Level.WARNING, "Could not save backup file for " + obj_name + ".ser");
			}
		}
		else {
			try {
				state_file.createNewFile();
			} catch ( IOException e ) {
				state_file = null;
				AppLogger.error.log(Level.SEVERE, "Creation of state file " + obj_name + ".ser failed!");
			}
		}
		
		return state_file;
	}
	
	public void saveState()
	{
		try {
			if (state_out == null) {
				state_out = new ObjectOutputStream(new FileOutputStream(backupAndRecreate()));
			}
			else {
				state_out.reset();
			}
			
			state_out.writeObject(obj);
		} catch ( IOException e ) {
			AppLogger.error.log(Level.SEVERE, "Cannot write to state file for " + this.obj_name);
		}
	}
	
	@SuppressWarnings("unchecked")
	public T restoreState() throws IOException
	{
		try {
			if (state_in != null) {
				state_in.close();
			}
			state_in = new ObjectInputStream(new FileInputStream(state_file));
			
			return (T)state_in.readUnshared();
		} catch ( IOException e ) {
			AppLogger.error.log(Level.SEVERE, "Cannot restore state file for " + this.obj_name);
			throw e;
		} catch ( ClassNotFoundException e ) {
			AppLogger.error.log(Level.SEVERE, "Class of serialized object " + this.obj_name + " cannot be found.");
		}
		
		return null;
	}
	
	public static void backupDirectoryTree(File src, File dest)
	{
		if (src.isDirectory()) {
			if (!dest.exists()) {
				dest.mkdir();
			}
			
			String[] children = src.list();
			for (int i = 0 ; i < children.length ; i++) {
				backupDirectoryTree(new File(src, children[i]), new File(dest, children[i]));
			}
		}
		else {
			
			if (dest.exists()) {
				dest.delete();
			}
			try {
				dest.createNewFile();
				
				FileInputStream stream_src = new FileInputStream(src);
				FileOutputStream stream_dest = new FileOutputStream(dest);
				
				// Copy the bits from instream to outstream
				byte[] buf = new byte[512 * 1024];
				int len;
				while ( (len = stream_src.read(buf)) > 0 ) {
					stream_dest.write(buf, 0, len);
				}
				stream_src.close();
				stream_dest.close();
			} catch ( FileNotFoundException e ) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch ( IOException e ) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
