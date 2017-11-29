package MidImpl;

import java.io.File;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class IOTools {
	public static void saveToDisk(Object obj, String filepath) {
		try {
			FileOutputStream file = new FileOutputStream(filepath);
			ObjectOutputStream out = new ObjectOutputStream(file);
			out.writeObject(obj);
			out.close();
			file.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static Object loadFromDisk(String filepath) {
		try {
			FileInputStream file = new FileInputStream(filepath);
			ObjectInputStream in= new ObjectInputStream(file);
			Object obj = in.readObject();
			in.close();
			file.close();
			return obj;
		} catch (IOException | ClassNotFoundException e) {
			return null;
		}
	}
	
	public static void deleteFile(String filepath) {
		File file = new File(filepath);
		file.delete();
	}
}