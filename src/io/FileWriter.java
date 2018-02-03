package io;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

public class FileWriter {
	private File file;
	
	public FileWriter(String path) {
		file = new File(path);
	}
	
	public void write(LineWriter writer, int lines) {
		try(PrintWriter pw = new PrintWriter(new FileOutputStream(file))){
			for(int i = 0; i < lines; i++) {
				pw.println(writer.line(i));
			}
		}catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	public void write(TableWriter writer, int rows, int columns) {
		write(writer, rows, columns, "\t");
	}
	
	public void write(TableWriter writer, int rows, int columns, String delim) {
		try(PrintWriter pw = new PrintWriter(new FileOutputStream(file))){
			for(int r = 0; r < rows; r++) {
				for(int c = 0; c < columns; c++) {
					pw.print(writer.tableEntry(r, c) + (c < columns - 1 ? delim : ""));
				}
				pw.println();
			}
		}catch(IOException e) {
			e.printStackTrace();
		}
	}
}
