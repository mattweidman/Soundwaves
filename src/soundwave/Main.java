package soundwave;

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.LineUnavailableException;

public class Main {
	
	public static void main(String args[]) throws IOException {
		Sound sounds = Sound.fromCSV(new File("data/alma.csv"));
		
		Thread t = new Thread(() -> {
			try {
				sounds.play();
			} catch (LineUnavailableException e) {
				System.err.println("Source data line unavailable");
			}
		});
		t.start();
	}

}
