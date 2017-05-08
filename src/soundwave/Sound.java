package soundwave;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

/**
 * Making simple sound waves in Java.
 */
public class Sound {
	
	private double[] buffer;
	private double sampleRate;
	
	public static final double DEFAULT_SAMPLE_RATE = 44100;
	public static final double MAX_AMPLITUDE = 128;
	
	/**
	 * Create a new sound from a buffer array
	 * @param b buffer
	 * @param sr sample rate
	 */
	private Sound(double[] b, double sr) {
		buffer = b;
		sampleRate = sr;
	}
	
	/**
	 * Pure sine wave sound.
	 * @param length number of samples
	 * @param sampleRate samples per second
	 * @param frequency waves per second
	 * @param amplitude height of waves (recommended 128)
	 * @param phase phase shift (recommended 0)
	 */
	public static Sound sineWave(int length, double sampleRate, 
			double frequency, double amplitude, double phase) {
		double[] b = new double[length];
		double angularFreq = 2.0 * Math.PI * frequency;
		for (int i=0; i<b.length; i++)
			b[i] = amplitude * Math.sin(angularFreq * i/sampleRate + phase);
		return new Sound(b, sampleRate);
	}
	
	/**
	 * Pure sine wave sound with sample rate 44.1 KHz, amplitude 128,
	 * and phase 0.
	 * @param time number of seconds
	 * @param frequency waves per second
	 */
	public static Sound sineWave(double time, double frequency) {
		double sampRate = DEFAULT_SAMPLE_RATE;
		double amp = MAX_AMPLITUDE;
		double phase = 0;
		int length = (int)(sampRate * time);
		return Sound.sineWave(length, sampRate, frequency, amp, phase);
	}
	
	/**
	 * Create a sine wave using musical note notation.
	 * @param whiteKey A, B, C, etc
	 * @param accidental 'b' for flat, '#' for sharp, else natural
	 * @param octave 0 for the A-G octave that includes middle C
	 * @param time number of seconds to play note
	 * @return
	 */
	public static Sound note(char whiteKey, char accidental, int octave, 
			double time) {
		// assert valid white keys
		char[] validWhiteKeys = {'A', 'B', 'C', 'D', 'E', 'F', 'G'};
		boolean valid = false;
		for (char vwk : validWhiteKeys) valid = valid || whiteKey == vwk;
		assert valid;
		
		// find frequencjy from white key
		double frequency = 440;
		switch(whiteKey) {
			case 'A': break;
			case 'B': frequency *= Math.pow(2, 2.0/12.0); break;
			case 'C': frequency *= Math.pow(2, 3.0/12.0); break;
			case 'D': frequency *= Math.pow(2, 5.0/12.0); break;
			case 'E': frequency *= Math.pow(2, 7.0/12.0); break;
			case 'F': frequency *= Math.pow(2, 8.0/12.0); break;
			case 'G': frequency *= Math.pow(2, 10.0/12.0); break;
		}
		
		// modify with accidental
		if (accidental == 'b') frequency *= Math.pow(2, -1.0/12.0);
		else if (accidental == '#') frequency *= Math.pow(2, 1.0/12.0);
		
		// change octave
		frequency *= Math.pow(2, octave);
		
		return Sound.sineWave(time, frequency);
	}
	
	/**
	 * Generate music from CSV.
	 * @param csv CSV file to read from
	 * @throws IOException 
	 */
	public static Sound fromCSV(File csv) throws IOException {
		ArrayList<Sound> notes = new ArrayList<>();
		
		BufferedReader reader = new BufferedReader(new FileReader(csv));
		String line;
		while ((line = reader.readLine()) != null) {
			String[] elements = line.split(",");
			if (elements.length != 4) continue;
			char whiteKey = elements[0].charAt(0);
			char accidental = elements[1].charAt(0);
			int octave = Integer.parseInt(elements[2]);
			double time = Double.parseDouble(elements[3]);
			notes.add(Sound.note(whiteKey, accidental, octave, time));
		}
		reader.close();
		
		Sound[] soundArray = new Sound[notes.size()];
		soundArray = notes.toArray(soundArray);
		return Sound.concatenate(soundArray);
	}
	
	/**
	 * Combine a sequence of sounds, heard one after the next.
	 * @param sounds list of sounds to combine
	 * @precondition all sounds must have the same sample rate
	 */
	public static Sound concatenate(Sound ...sounds) {
		// make sure all sample rates are the same
		assert sounds.length > 0;
		double sampRate = sounds[0].sampleRate;
		for (Sound s : sounds) assert s.sampleRate == sampRate;
		
		// find total length of sounds
		int blen = 0;
		for (Sound s : sounds) blen += s.buffer.length;
		
		// combine into one buffer
		double[] b = new double[blen];
		int bIndex = 0;
		for (int soundIndex=0; soundIndex<sounds.length; soundIndex++) {
			for (int i=0; i<sounds[soundIndex].buffer.length; i++) {
				b[bIndex] = sounds[soundIndex].buffer[i];
				bIndex += 1;
			}
		}
		
		return new Sound(b, sampRate);
	}
	
	/**
	 * Play sound recorded in buffer.
	 * @throws LineUnavailableException 
	 */
	public void play() throws LineUnavailableException {
		AudioFormat af = new AudioFormat(
				(float)sampleRate, // samples per second
				8, // number of bits per sample
				1, // 1 = mono, 2 = stereo
				true, // true means it can be negative (signed)
				false); // true = big endian, false = little endian
		// big endian = most significant byte at smallest memory address
		// little endian = least significant byte at smallest memory address
		
		// convert buffer to bytes
		byte[] bbuffer = byteBuffer();
		
		SourceDataLine sdl = AudioSystem.getSourceDataLine(af);
		sdl.open();
		sdl.start();
		
		sdl.write(bbuffer, 0, buffer.length);
		
		sdl.drain();
		sdl.close();
	}
	
	/**
	 * Convert the double buffer array into bytes and squash into
	 * the appropriate range.
	 */
	private byte[] byteBuffer() {
		// find smallest and lowest values in buffer array
		double minS = Double.MAX_VALUE, maxS = -Double.MAX_VALUE;
		for (Double samp : buffer) {
			if (samp < minS) minS = samp;
			if (samp > maxS) maxS = samp;
		}
		double range = maxS - minS;
		
		// convert to bytes
		byte[] bbuffer = new byte[buffer.length];
		double newRange = MAX_AMPLITUDE * 2 - 1;
		double newMinS = -MAX_AMPLITUDE;
		double scale = newRange / range;
		for (int i=0; i<buffer.length; i++)
			bbuffer[i] = (byte) ((buffer[i] - minS) * scale + newMinS);
		return bbuffer;
	}

}
