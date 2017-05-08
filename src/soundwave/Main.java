package soundwave;

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.LineUnavailableException;

public class Main {
	
	public static void main(String args[]) throws IOException {
		/*Sound[] scale = new Sound[8];
		for (int i=0; i<scale.length; i++)
			scale[i] = Sound.note((char)(i%7+'A'), 'n', i/7, 0.25);
		Sound scalePlay = Sound.concatenate(scale); */
		
		/* Sound sounds = Sound.concatenate(Sound.note('D', 'b', -1, 0.5),
				Sound.note('D', 'n', -1, 0.5),
				Sound.note('D', '#', -1, 0.5)); */
		
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
