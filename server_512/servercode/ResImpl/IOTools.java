package ResImpl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class IOTools {
	public static void saveToDisk(Object obj, String filepath) {
		try {
			FileOutputStream fileOut = new FileOutputStream(filepath);
			ObjectOutputStream out = new ObjectOutputStream(fileOut);
			out.writeObject(obj);
			out.close();
			fileOut.close();
		} catch (IOException i) {
			i.printStackTrace();
		}
	}
	
	public static Object loadFromDisk(String filepath) {
		try {
			FileInputStream fileIn = new FileInputStream(filepath);
			ObjectInputStream in= new ObjectInputStream(fileIn);
			Object obj = in.readObject();
			in.close();
			fileIn.close();
			return obj;
		} catch (IOException | ClassNotFoundException i) {
			return null;
		}
	}
	
	public static void deleteFile(String filepath) {
		File file = new File(filepath);
		file.delete();
	}
}